// Copyright 2021 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.plan.execution.stores.relational.result;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.opentracing.Span;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.plan.dependencies.domain.date.PureDate;
import org.finos.legend.engine.plan.dependencies.store.relational.IRelationalResult;
import org.finos.legend.engine.plan.execution.nodes.helpers.ExecutionNodeClassResultHelper;
import org.finos.legend.engine.plan.execution.nodes.helpers.ExecutionNodePartialClassResultHelper;
import org.finos.legend.engine.plan.execution.nodes.helpers.ExecutionNodeTDSResultHelper;
import org.finos.legend.engine.plan.execution.result.ExecutionActivity;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.ResultVisitor;
import org.finos.legend.engine.plan.execution.result.StreamingResult;
import org.finos.legend.engine.plan.execution.result.builder.Builder;
import org.finos.legend.engine.plan.execution.result.builder._class.ClassBuilder;
import org.finos.legend.engine.plan.execution.result.builder._class.ClassMappingInfo;
import org.finos.legend.engine.plan.execution.result.builder._class.PartialClassBuilder;
import org.finos.legend.engine.plan.execution.result.builder._class.PropertyInfo;
import org.finos.legend.engine.plan.execution.result.builder.datatype.DataTypeBuilder;
import org.finos.legend.engine.plan.execution.result.builder.tds.TDSBuilder;
import org.finos.legend.engine.plan.execution.result.serialization.ExecutionResultObjectMapperFactory;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.execution.result.serialization.Serializer;
import org.finos.legend.engine.plan.execution.result.transformer.SetImplTransformers;
import org.finos.legend.engine.plan.execution.result.transformer.TransformerInput;
import org.finos.legend.engine.plan.execution.stores.StoreExecutable;
import org.finos.legend.engine.plan.execution.stores.StoreExecutableManager;
import org.finos.legend.engine.plan.execution.stores.relational.activity.RelationalExecutionActivity;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.Column;
import org.finos.legend.engine.plan.execution.stores.relational.result.builder.relation.RelationBuilder;
import org.finos.legend.engine.plan.execution.stores.relational.serialization.RelationalResultToArrowIPCSerializer;
import org.finos.legend.engine.plan.execution.stores.relational.serialization.RelationalResultToCSVSerializer;
import org.finos.legend.engine.plan.execution.stores.relational.serialization.RelationalResultToCSVSerializerWithTransformersApplied;
import org.finos.legend.engine.plan.execution.stores.relational.serialization.RelationalResultToJsonDefaultSerializer;
import org.finos.legend.engine.plan.execution.stores.relational.serialization.RelationalResultToPureTDSSerializer;
import org.finos.legend.engine.plan.execution.stores.relational.serialization.RelationalResultToPureTDSToObjectSerializer;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.RelationalExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.RelationalInstantiationExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.SQLExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.result.TDSColumn;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.result.TDSResultType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.result.SQLResultColumn;
import org.finos.legend.engine.shared.core.api.request.RequestContext;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m4.tools.time.TimeZones;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.Array;
import java.sql.Connection;
import java.sql.JDBCType;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class RelationalResult extends StreamingResult implements IRelationalResult, StoreExecutable
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(RelationalResult.class);
    private static final ImmutableList<String> TEMPORAL_DATE_ALIASES = Lists.immutable.of("k_businessDate", "k_processingDate");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public final List<String> sqlColumns;
    private final List<String> temporaryTables;
    private final List<SQLResultColumn> resultColumns;
    private List<String> columnListForSerializer;

    private final Connection connection;
    private final Statement statement;
    public ResultSet resultSet;
    public ResultSetMetaData resultSetMetaData;
    public String executedSQl;
    public int columnCount;

    private final String databaseType;
    private final String databaseTimeZone;

    public Span topSpan;

    private final SQLResultDBColumnsMetaData resultDBColumnsMetaData;
    private final RequestContext requestContext;
    public MutableList<SetImplTransformers> setTransformers = Lists.mutable.empty();

    public Builder builder;
    private Calendar calendar;
    private boolean readsLocalDate = true;
    private boolean readsLocalDateTime = true;

    public RelationalResult(MutableList<ExecutionActivity> activities, RelationalExecutionNode node, List<SQLResultColumn> sqlResultColumns, String databaseType, String databaseTimeZone, Connection connection, Identity identity, List<String> temporaryTables, Span topSpan)
    {
        this(activities, node, sqlResultColumns, databaseType, databaseTimeZone, connection, identity, temporaryTables, topSpan, new RequestContext());
    }

    public RelationalResult(MutableList<ExecutionActivity> activities, RelationalExecutionNode node, List<SQLResultColumn> sqlResultColumns, String databaseType, String databaseTimeZone, Connection connection,Identity identity, List<String> temporaryTables, Span topSpan, RequestContext requestContext)
    {
        this(activities, node, sqlResultColumns, databaseType, databaseTimeZone, connection, identity, temporaryTables, topSpan, requestContext, true);
    }

    public RelationalResult(MutableList<ExecutionActivity> activities, RelationalExecutionNode node, List<SQLResultColumn> sqlResultColumns, String databaseType, String databaseTimeZone, Connection connection, Identity identity, List<String> temporaryTables, Span topSpan, RequestContext requestContext, boolean logSQLWithParamValues)
    {
        super(activities);
        this.databaseType = databaseType;
        this.databaseTimeZone = databaseTimeZone;
        this.temporaryTables = temporaryTables;
        this.topSpan = topSpan;
        this.requestContext = requestContext;
        try
        {
            this.connection = connection;
            this.statement = connection.createStatement();
            if (DatabaseType.MemSQL.name().equals(databaseType))
            {
                this.statement.setFetchSize(100);
            }

            if (requestContext != null)
            {
                StoreExecutableManager.INSTANCE.addExecutable(requestContext, this);
            }

            long start = System.currentTimeMillis();
            RelationalExecutionActivity activity = ((RelationalExecutionActivity) activities.getLast());
            String sql = activity.comment != null ? activity.comment.concat("\n").concat(activity.sql) : activity.sql;
            String logMessage = logSQLWithParamValues ? sql : node.sqlQuery();
            LOGGER.info(new LogInfo(identity.getName(), LoggingEventType.EXECUTION_RELATIONAL_START, logMessage).toString());
            this.resultSet = this.statement.executeQuery(sql);
            this.executedSQl = sql;
            LOGGER.info(new LogInfo(identity.getName(), LoggingEventType.EXECUTION_RELATIONAL_STOP, (double) System.currentTimeMillis() - start).toString());
            this.resultSetMetaData = resultSet.getMetaData();
            this.columnCount = this.resultSetMetaData.getColumnCount();
            this.resultColumns = sqlResultColumns;
            this.resultDBColumnsMetaData = new SQLResultDBColumnsMetaData(this.resultColumns, this.resultSetMetaData);

            this.sqlColumns = Lists.mutable.ofInitialCapacity(this.columnCount);
            for (int i = 1; i <= this.columnCount; i++)
            {
                this.sqlColumns.add(this.resultSetMetaData.getColumnLabel(i));
            }
            this.columnListForSerializer = this.sqlColumns;
            this.buildTransformersAndBuilder(node, node.connection);
        }
        catch (Throwable e)
        {

            LOGGER.error("error initialising RelationalResult", e);
            this.close();
            if (e instanceof Error)
            {
                throw (Error) e;
            }
            if (e instanceof RuntimeException)
            {
                throw (RuntimeException) e;
            }
            if (e instanceof SQLException)
            {
                // A plain RuntimeException(e) would set getMessage() to e.toString(), burying the driver's own
                // message behind its exception class name and hiding it from PureException.findPureException's
                // instanceof check. PureExecutionException carries the driver's message forward untouched and
                // is itself a PureException, so it survives the compiled dispatcher's generic-wrapper fallback.
                // cleanErrorMessage lets a dialect strip its own driver-specific wrapping (e.g. Databricks'
                // Hive-Thrift response envelope); dialects with no such wrapping return the message unchanged.
                String message = DatabaseManager.fromString(databaseType).cleanErrorMessage(e.getMessage());
                throw new PureExecutionException(message, e);
            }
            throw new RuntimeException(e);
        }
    }

    public RelationalResult(SQLExecutionResult sqlExecutionResult, RelationalInstantiationExecutionNode node)
    {
        super(sqlExecutionResult.activities);
        this.databaseType = sqlExecutionResult.getDatabaseType();
        this.databaseTimeZone = sqlExecutionResult.getDatabaseTimeZone();
        this.temporaryTables = sqlExecutionResult.getTemporaryTables();
        this.topSpan = sqlExecutionResult.getTopSpan();
        this.requestContext = sqlExecutionResult.getRequestContext();
        try
        {
            this.connection = sqlExecutionResult.getConnection();
            this.statement = sqlExecutionResult.getStatement();
            this.resultSet = sqlExecutionResult.getResultSet();
            this.executedSQl = sqlExecutionResult.getExecutedSql();
            this.resultSetMetaData = sqlExecutionResult.getResultSetMetaData();
            this.columnCount = sqlExecutionResult.getColumnCount();
            this.sqlColumns = sqlExecutionResult.getColumnNames();
            this.columnListForSerializer = this.sqlColumns;
            this.resultColumns = sqlExecutionResult.getSqlResultColumns();
            this.resultDBColumnsMetaData = new SQLResultDBColumnsMetaData(this.resultColumns, this.resultSetMetaData);
            this.buildTransformersAndBuilder(node, sqlExecutionResult.getSQLExecutionNode().connection);
            if (this.requestContext != null)
            {
                StoreExecutableManager.INSTANCE.addExecutable(requestContext, this);
            }
        }
        catch (Throwable e)
        {
            LOGGER.error("error initialising RelationalResult", e);
            this.close();
            if (e instanceof Error)
            {
                throw (Error) e;
            }
            if (e instanceof RuntimeException)
            {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e);
        }
    }

    private void buildTransformersAndBuilder(ExecutionNode node, DatabaseConnection databaseConnection) throws SQLException
    {
        boolean isDatabaseIdentifiersCaseSensitive = databaseConnection.accept(new DatabaseIdentifiersCaseSensitiveVisitor());
        if (ExecutionNodeTDSResultHelper.isResultTDS(node))
        {
            // dynamically update the TDS result columns
            if (!node.executionNodes.isEmpty() && node.executionNodes.get(0) instanceof SQLExecutionNode)
            {
                TDSResultType tdsResultType = (TDSResultType) node.resultType;
                SQLExecutionNode sqlExecutionNode = (SQLExecutionNode) node.executionNodes.get(0);
                if (tdsResultType.tdsColumns == null || sqlExecutionNode.isResultColumnsDynamic)
                {
                    tdsResultType.tdsColumns = Lists.mutable.empty();
                    for (int columnIndex = 1; columnIndex <= this.columnCount; columnIndex++)
                    {
                        TDSColumn c = new TDSColumn(this.sqlColumns.get(columnIndex - 1), this.resultColumns.get(columnIndex - 1).labelTypePair().getTwo());
                        tdsResultType.tdsColumns.add(c);
                    }
                }
            }

            List<TransformerInput<Integer>> transformerInputs = Lists.mutable.empty();
            for (int columnIndex = 1; columnIndex <= this.columnCount; columnIndex++)
            {
                TDSColumn c = ExecutionNodeTDSResultHelper.getTDSColumn(node, this.resultSetMetaData.getColumnLabel(columnIndex), isDatabaseIdentifiersCaseSensitive);
                transformerInputs.add(new TransformerInput<>(
                        columnIndex,
                        c.type,
                        (index) ->
                        {
                            try
                            {
                                return ExecutionNodeTDSResultHelper.isTDSColumnEnum(node, this.resultSetMetaData.getColumnLabel(index), isDatabaseIdentifiersCaseSensitive);
                            }
                            catch (Exception e)
                            {
                                throw new RuntimeException(e);
                            }
                        },
                        (index) ->
                        {
                            try
                            {
                                return ExecutionNodeTDSResultHelper.getTDSEnumTransformer(node, this.resultSetMetaData.getColumnLabel(index), isDatabaseIdentifiersCaseSensitive);
                            }
                            catch (Exception e)
                            {
                                throw new RuntimeException(e);
                            }
                        })
                );
            }
            setTransformers.add(new SetImplTransformers(transformerInputs));
            this.builder = new TDSBuilder(node, this.sqlColumns, isDatabaseIdentifiersCaseSensitive);
            this.columnListForSerializer = ListIterate.collect(((TDSBuilder) this.builder).columns, col -> col.name);
        }
        else if (ExecutionNodeClassResultHelper.isClassResult(node))
        {
            List<? extends ClassMappingInfo> classMappings = ExecutionNodeClassResultHelper.getClassMappingInfoFromClassResult(node);
            for (ClassMappingInfo classMappingInfo : classMappings)
            {
                List<TransformerInput<String>> transformerInputs = Lists.mutable.empty();
                for (int i = 1; i <= this.columnCount; i++)
                {
                    final String colName = this.resultSetMetaData.getColumnLabel(i);
                    PropertyInfo profiles = ListIterate.select(classMappingInfo.properties, p -> isDatabaseIdentifiersCaseSensitive ? p.property.equals(colName) : p.property.equalsIgnoreCase(colName)).getFirst();
                    transformerInputs.add(new TransformerInput<>(
                            profiles != null ? profiles.property : colName,
                            resolveType(profiles, colName),
                            (colNameX) ->
                            {
                                try
                                {
                                    return !TEMPORAL_DATE_ALIASES.contains(colNameX) && ExecutionNodeClassResultHelper.isClassPropertyEnum(node, classMappingInfo.setImplementationId, colNameX);
                                }
                                catch (Exception e)
                                {
                                    throw new RuntimeException(e);
                                }
                            },
                            (colNameX) ->
                            {
                                try
                                {
                                    return ExecutionNodeClassResultHelper.getClassEnumTransformer(node, classMappingInfo.setImplementationId, colNameX);
                                }
                                catch (Exception e)
                                {
                                    throw new RuntimeException(e);
                                }
                            })
                    );
                }
                setTransformers.add(new SetImplTransformers(transformerInputs));

                if (ExecutionNodePartialClassResultHelper.isPartialClassResult(node))
                {
                    this.builder = new PartialClassBuilder(node);
                }
                else
                {
                    this.builder = new ClassBuilder(node);
                }
            }
        }
        else if (ExecutionNodeRelationalResultHelper.isRelationResult(node))
        {
            SetImplTransformers setImpl = new SetImplTransformers();
            for (int columnIndex = 1; columnIndex <= this.columnCount; columnIndex++)
            {
                setImpl.transformers.add(SetImplTransformers.TEMPORARY_DATATYPE_TRANSFORMER);
            }
            setTransformers.add(setImpl);
            this.builder = new RelationBuilder(node);
        }
        else
        {
            Function<Object, Object> transformer = "Boolean".equals(node.getDataTypeResultType())
                    ? SetImplTransformers::toBoolean
                    : SetImplTransformers.TEMPORARY_DATATYPE_TRANSFORMER;
            SetImplTransformers setImpl = new SetImplTransformers();
            for (int i = 1; i <= this.columnCount; i++)
            {
                setImpl.transformers.add(transformer);
            }
            setTransformers.add(setImpl);
            this.builder = new DataTypeBuilder(node);
        }
    }

    @Override
    public <T> T accept(ResultVisitor<T> resultVisitor)
    {
        return ((RelationalResultVisitor<T>) resultVisitor).visit(this);
    }

    private static String resolveType(PropertyInfo profiles, String colIndentifier)
    {
        return profiles == null ? (TEMPORAL_DATE_ALIASES.contains(colIndentifier) ? "Date" : null) : profiles.type;
    }

    public String getRelationalDatabaseTimeZone()
    {
        return this.databaseTimeZone;
    }

    @Override
    public void close()
    {
        if (temporaryTables != null && statement != null)
        {
            temporaryTables.forEach((Consumer<? super String>) table ->
            {
                try
                {
                    DatabaseManager databaseManager = DatabaseManager.fromString(this.databaseType);
                    statement.execute(databaseManager.relationalDatabaseSupport().dropTempTable(table));
                }
                catch (Exception ignored)
                {
                }
            });
        }

        if (requestContext != null)
        {
            StoreExecutableManager.INSTANCE.removeExecutable(requestContext, this);
        }

        if (resultSet != null)
        {
            try
            {
                resultSet.close();
            }
            catch (Exception e)
            {
                LOGGER.error("error closing result set", e);
            }
        }
        if (statement != null)
        {
            try
            {
                statement.close();
            }
            catch (Exception e)
            {
                LOGGER.error("error closing statement", e);
            }
        }
        if (connection != null)
        {
            try
            {
                connection.close();
            }
            catch (Exception e)
            {
                LOGGER.error("error closing connection", e);
            }
        }
        super.close();
    }

    public List<TDSColumn> getTdsColumns()
    {
        if (this.builder instanceof TDSBuilder)
        {
            return ((TDSBuilder) this.builder).columns;
        }
        else
        {
            throw new RuntimeException("Current result is not a tds result");
        }
    }

    public List<SQLResultColumn> getSQLResultColumns()
    {
        return this.resultColumns;
    }

    public List<String> getColumnListForSerializer()
    {
        return this.columnListForSerializer;
    }

    public MutableList<Function<Object, Object>> getTransformers() throws SQLException
    {
        return this.setTransformers.size() == 1 ? this.setTransformers.get(0).transformers : this.setTransformers.get(this.resultSet.getInt("u_type")).transformers;
    }

    public Object getValue(int columnIndex) throws SQLException
    {
        Object result;
        if (resultDBColumnsMetaData.isTimestampColumn(columnIndex))
        {
            Timestamp ts;
            ts = resultSet.getTimestamp(columnIndex, getCalendar());
            result = ts;
        }
        else if (resultDBColumnsMetaData.isDateColumn(columnIndex))
        {
            result = resultSet.getDate(columnIndex);
        }
        else if (resultDBColumnsMetaData.isArrayColumn(columnIndex))
        {
            Array array = resultSet.getArray(columnIndex);
            if (array == null)
            {
                result =  null;
            }
            else
            {
                try
                {
                    result = arrayValueToJson(array.getArray());
                }
                catch (RuntimeException e)
                {
                    throw new UncheckedIOException(String.format("Unable to process variant result as JSON from column '%s' with value: %s", this.resultSetMetaData.getColumnLabel(columnIndex), array), new IOException(e));
                }
            }
        }
        else if (resultDBColumnsMetaData.isVariantColumn(columnIndex))
        {
            Object object = resultSet.getObject(columnIndex);
            if (object == null)
            {
                result =  null;
            }
            else
            {
                try
                {
                    if (object instanceof Map)
                    {
                        result = OBJECT_MAPPER.writeValueAsString(object);
                    }
                    else
                    {
                        //json object...
                        result = OBJECT_MAPPER.readTree(object.toString()).toString();
                    }

                }
                catch (JsonProcessingException e)
                {
                    throw new UncheckedIOException(String.format("Unable to process variant result as JSON from column '%s' with value: %s", this.resultSetMetaData.getColumnLabel(columnIndex), object), e);
                }
            }
        }
        else if (resultDBColumnsMetaData.isBinaryColumn(columnIndex))
        {
            byte[] bytes = resultSet.getBytes(columnIndex);
            if (bytes != null)
            {
                result = BinaryUtils.encodeHex(bytes);
            }
            else
            {
                result = null;
            }
        }
        else
        {
            result = resultSet.getObject(columnIndex);
        }
        return result;
    }

    /**
     * Renders a JDBC array value (from {@link Array#getArray()}) as JSON text, without going
     * through Jackson's default bean-reflection serialization. That matters for an array whose
     * element type is itself semi-structured/JSON (e.g. DuckDB's {@code JSON[]}): those elements
     * come back as opaque, driver-specific wrapper objects (e.g. {@code org.duckdb.JsonNode})
     * that Jackson has no serializer for, so its default reflection over the wrapper's own bean
     * getters produces garbage (e.g. {@code {"array":false,"null":false,"number":true,...}})
     * instead of the value. Every JDBC driver's JSON-column wrapper is expected to render correct
     * JSON text from its own toString() (this is the same assumption the isVariantColumn branch
     * just below already relies on for a scalar semi-structured value).
     *
     * Not every driver wraps a semi-structured element in a rich object, though -- confirmed live
     * on Databricks, whose JDBC driver returns each element of an {@code ARRAY<VARIANT>} column as
     * a plain Java String that is already valid JSON text (e.g. "1", not a quoted "\"1\""). A plain
     * (non-JSON) typed array (e.g. VARCHAR[]) also returns String elements, but there the string
     * IS the raw value and needs proper JSON quoting/escaping -- the two cases are indistinguishable
     * by Java type alone. Disambiguate by content: if the string already parses as JSON, trust it
     * verbatim (matching the isVariantColumn branch's identical assumption for a scalar value);
     * only fall back to escaping it as a JSON string when it doesn't. This can misfire only for a
     * plain string array element whose entire content happens to already look like a JSON token
     * (e.g. a VARCHAR[] value of "123" or "true") -- a narrow, pre-existing ambiguity in what the
     * JDBC layer hands back, not something a per-element check can fully resolve either way.
     */
    private static String arrayValueToJson(Object arrayValue)
    {
        if (arrayValue == null)
        {
            return "null";
        }
        int length = java.lang.reflect.Array.getLength(arrayValue);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < length; i++)
        {
            if (i > 0)
            {
                sb.append(",");
            }
            Object element = java.lang.reflect.Array.get(arrayValue, i);
            sb.append(arrayElementToJson(element));
        }
        sb.append("]");
        return sb.toString();
    }

    private static String arrayElementToJson(Object element)
    {
        if (element != null && element.getClass().isArray())
        {
            return arrayValueToJson(element);
        }
        if (element instanceof String)
        {
            // readTree() alone only parses a leading JSON value and silently ignores anything
            // after it, so a genuine raw string like "9876 Hello World Street" would otherwise be
            // misdetected as already-JSON (a bare number "9876") and truncated. Only trust it as
            // already-JSON if parsing consumes the entire string -- confirmed via parser.nextToken()
            // returning null (Jackson skips insignificant trailing whitespace on its own).
            try (com.fasterxml.jackson.core.JsonParser parser = OBJECT_MAPPER.getFactory().createParser((String) element))
            {
                JsonNode node = OBJECT_MAPPER.readTree(parser);
                if (parser.nextToken() == null)
                {
                    return node.toString();
                }
            }
            catch (IOException e)
            {
                // Not parseable as JSON at all -- a genuine raw string value, fall through to be escaped.
            }
        }
        return ExecutionResultObjectMapperFactory.getPurePrimitiveToJsonConverter().valueOf(element);
    }

    public Object getTransformedValue(int columnIndex) throws SQLException
    {
        switch (this.resultSetMetaData.getColumnType(columnIndex))
        {
            case Types.DATE:
            {
                return readDay(columnIndex);
            }
            case Types.TIMESTAMP:
            {
                return readMoment(columnIndex);
            }
            case Types.TINYINT:
            case Types.SMALLINT:
            case Types.INTEGER:
            case Types.BIGINT:
            {
                long num = this.resultSet.getLong(columnIndex);
                return this.resultSet.wasNull() ? null : num;
            }
            case Types.REAL:
            case Types.FLOAT:
            case Types.DOUBLE:
            {
                double num = this.resultSet.getDouble(columnIndex);
                return this.resultSet.wasNull() ? null : num;
            }
            case Types.DECIMAL:
            case Types.NUMERIC:
            {
                return this.resultSet.getBigDecimal(columnIndex);
            }
            case Types.CHAR:
            case Types.VARCHAR:
            case Types.LONGVARCHAR:
            case Types.NCHAR:
            case Types.NVARCHAR:
            case Types.LONGNVARCHAR:
            case Types.OTHER:
            {
                return this.resultSet.getString(columnIndex);
            }
            case Types.BIT:
            case Types.BOOLEAN:
            {
                boolean bool = this.resultSet.getBoolean(columnIndex);
                return this.resultSet.wasNull() ? null : bool;
            }
            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:
            case Types.BLOB:
            {
                byte[] bytes = this.resultSet.getBytes(columnIndex);
                return (bytes == null) ? null : BinaryUtils.encodeHex(bytes);
            }
            case Types.NULL:
            {
                return null;
            }
            default:
            {
                return this.resultSet.getObject(columnIndex);
            }
        }
    }

    @Override
    public ResultSet getResultSet()
    {
        return this.resultSet;
    }

    @Override
    public Builder getResultBuilder()
    {
        return this.builder;
    }

    @Override
    public Result realizeInMemory()
    {
        try
        {
            return new RealizedRelationalResult(this);
        }
        catch (SQLException e)
        {
            throw new RuntimeException("Failed to realize in memory", e);
        }
    }

    @Override
    public Serializer getSerializer(SerializationFormat format)
    {
        switch (format)
        {
            case PURE:
                return new RelationalResultToPureTDSSerializer(this);
            case RAW:
            case PURE_TDSOBJECT:
                return new RelationalResultToPureTDSToObjectSerializer(this);
            case CSV:
                return new RelationalResultToCSVSerializer(this, true);
            case CSV_TRANSFORMED:
                return new RelationalResultToCSVSerializerWithTransformersApplied(this, true);
            case ARROW_IPC:
                return new RelationalResultToArrowIPCSerializer(this);
            case DEFAULT:
                return new RelationalResultToJsonDefaultSerializer(this);
            default:
                this.close();
                throw new RuntimeException(format.toString() + " format not currently supported with RelationalResult");
        }
    }

    public Stream<ObjectNode> toStream()
    {
        ObjectMapper objectMapper = ExecutionResultObjectMapperFactory.getNewObjectMapper();
        Map<String, Object> row = Maps.mutable.empty();

        Spliterator<ObjectNode> spliterator = new Spliterators.AbstractSpliterator<ObjectNode>(Long.MAX_VALUE, 0)
        {
            @Override
            public boolean tryAdvance(Consumer<? super ObjectNode> action)
            {
                try
                {
                    boolean next = resultSet.next();
                    if (next)
                    {
                        List<Function<Object, Object>> transformers = getTransformers();

                        for (int i = 0; i < resultColumns.size(); i++)
                        {
                            row.put(columnListForSerializer.get(i), transformers.get(i).valueOf(getValue(i + 1)));
                        }
                        action.accept(objectMapper.convertValue(row, ObjectNode.class));
                    }
                    return next;
                }
                catch (SQLException e)
                {
                    throw new RuntimeException(e);
                }
            }
        };

        return StreamSupport.stream(spliterator, false).onClose(this::close);
    }


    private Calendar getCalendar()
    {
        String timeZoneId = getRelationalDatabaseTimeZone();
        if (this.calendar == null)
        {
            //TODO, throw exception, TZ should always be specified
            //Till then, default to PURE default which is "GMT"
            this.calendar = TimeZones.newCalendar((timeZoneId == null) ? "GMT" : timeZoneId);
        }
        return this.calendar;
    }

    @Override
    public void cancel()
    {
        try
        {
            if (!statement.isClosed())
            {
                statement.cancel();
                LOGGER.info(new LogInfo(Identity.getAnonymousIdentity().getName(), LoggingEventType.EXECUTABLE_CANCELLATION, "Successful cancellation of  RelationalResult " + RequestContext.getSessionID(this.requestContext)).toString());

            }
        }
        catch (Exception e)
        {
            LOGGER.error(new LogInfo(Identity.getAnonymousIdentity().getName(), LoggingEventType.EXECUTABLE_CANCELLATION_ERROR, "Unable to cancel  RelationalResult  for session " + RequestContext.getSessionID(this.requestContext) + " " + e.getMessage()).toString());
        }
    }

    public List<Column> getResultSetColumns()
    {
        try
        {
            MutableList<Column> columns = Lists.mutable.ofInitialCapacity(this.resultSetMetaData.getColumnCount());
            for (int i = 1; i <= this.resultSetMetaData.getColumnCount(); i++)
            {
                String columnType = JDBCType.valueOf(this.resultSetMetaData.getColumnType(i)).getName();
                String updatedColumnType = columnType.equals("TIMESTAMP_WITH_TIMEZONE") ? "TIMESTAMP WITH TIME ZONE" :
                        columnType.equals("TIME_WITH_TIMEZONE") ? "TIME WITH TIME ZONE" : columnType;
                columns.add(new Column(unquote(this.resultSetMetaData.getColumnLabel(i)), updatedColumnType));
            }
            return columns;
        }
        catch (SQLException e)
        {
            this.close();
            throw new RuntimeException(e);
        }
    }

    private static String unquote(String s)
    {
        return (s.startsWith("\"") && s.endsWith("\"")) ? s.substring(1, s.length() - 1) : s;
    }

    /**
     * Read a date column.
     *
     * <p>The column carries a day and no zone, and {@link ResultSet#getObject(int, Class)} for a
     * {@link LocalDate} hands that day over as it stands. Reading it as a {@link java.sql.Date}
     * instead gives an instant, which yields a day only once a zone is chosen to read it in, and
     * drivers do not agree on the zone they built that instant in. A driver need not answer the
     * first call, and is asked once rather than once a row.
     */
    private PureDate readDay(int columnIndex) throws SQLException
    {
        if (this.readsLocalDate)
        {
            try
            {
                LocalDate day = this.resultSet.getObject(columnIndex, LocalDate.class);
                return (day == null) ? null : toPureDate(day);
            }
            catch (SQLException | UnsupportedOperationException | AbstractMethodError unsupported)
            {
                this.readsLocalDate = false;
            }
        }
        java.sql.Date date = this.resultSet.getDate(columnIndex);
        return (date == null) ? null : toPureDate(date.toLocalDate());
    }

    /**
     * Read a timestamp column.
     *
     * <p>The column carries a wall clock the database keeps in the zone the connection names, and
     * a Pure date is that moment in UTC, so the wall clock is read and shifted here rather than
     * by the driver. Handing a driver a calendar and asking it to shift does not work for every
     * driver: some ignore the calendar and answer as though the connection named UTC, leaving
     * every timestamp short by the connection zone.
     */
    private PureDate readMoment(int columnIndex) throws SQLException
    {
        Calendar calendar = getCalendar();
        if (this.readsLocalDateTime)
        {
            try
            {
                LocalDateTime wallClock = this.resultSet.getObject(columnIndex, LocalDateTime.class);
                return (wallClock == null) ? null : toPureDate(wallClock.atZone(calendar.getTimeZone().toZoneId()));
            }
            catch (SQLException | UnsupportedOperationException | AbstractMethodError unsupported)
            {
                this.readsLocalDateTime = false;
            }
        }
        Timestamp timestamp = this.resultSet.getTimestamp(columnIndex, calendar);
        return (timestamp == null) ? null : PureDate.fromSQLTimestamp(timestamp);
    }

    private static PureDate toPureDate(LocalDate day)
    {
        return PureDate.newPureDate(day.getYear(), day.getMonthValue(), day.getDayOfMonth());
    }

    private static PureDate toPureDate(ZonedDateTime moment)
    {
        LocalDateTime utc = moment.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        return PureDate.newPureDate(utc.getYear(), utc.getMonthValue(), utc.getDayOfMonth(),
                utc.getHour(), utc.getMinute(), utc.getSecond(), String.format("%09d", utc.getNano()));
    }
}
