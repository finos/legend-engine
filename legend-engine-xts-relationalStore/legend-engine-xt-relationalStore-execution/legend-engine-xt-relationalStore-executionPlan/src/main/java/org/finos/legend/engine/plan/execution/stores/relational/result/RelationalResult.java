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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.opentracing.Span;

import java.sql.Connection;
import java.sql.JDBCType;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.TimeZone;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.plan.dependencies.domain.date.PureDate;
import org.finos.legend.engine.plan.dependencies.store.relational.IRelationalResult;
import org.finos.legend.engine.plan.execution.nodes.helpers.ExecutionNodeClassResultHelper;
import org.finos.legend.engine.plan.execution.nodes.helpers.ExecutionNodePartialClassResultHelper;
import org.finos.legend.engine.plan.execution.nodes.helpers.ExecutionNodeTDSResultHelper;
import org.finos.legend.engine.plan.execution.result.*;
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
import org.slf4j.Logger;

public class RelationalResult extends StreamingResult implements IRelationalResult, StoreExecutable
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(RelationalResult.class);
    private static final ImmutableList<String> TEMPORAL_DATE_ALIASES = Lists.immutable.of("k_businessDate", "k_processingDate");

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
                if (sqlExecutionNode.isResultColumnsDynamic != null && sqlExecutionNode.isResultColumnsDynamic)
                {
                    tdsResultType.tdsColumns = Lists.mutable.empty();
                    for (int columnIndex = 1; columnIndex <= this.columnCount; columnIndex++)
                    {
                        TDSColumn c = new TDSColumn(this.resultColumns.get(columnIndex - 1).label, this.resultColumns.get(columnIndex - 1).labelTypePair().getTwo());
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
            SetImplTransformers setImpl = new SetImplTransformers();
            for (int i = 1; i <= this.columnCount; i++)
            {
                setImpl.transformers.add(SetImplTransformers.TEMPORARY_DATATYPE_TRANSFORMER);
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
        else if (resultDBColumnsMetaData.isVariantColumn(columnIndex))
        {
            result = resultSet.getString(columnIndex);
        }
        else
        {
            result = resultSet.getObject(columnIndex);
        }
        return result;
    }

    public Object getTransformedValue(int columnIndex) throws SQLException
    {
        Object result = null;
        switch (resultSetMetaData.getColumnType(columnIndex))
        {
            case Types.DATE:
            {
                java.sql.Date date = this.resultSet.getDate(columnIndex);
                if (date != null)
                {
                    result = PureDate.fromSQLDate(date);
                }
                break;
            }
            case Types.TIMESTAMP:
            {
                java.sql.Timestamp timestamp = this.resultSet.getTimestamp(
                        columnIndex,
                        getRelationalDatabaseTimeZone() == null ?
                                new GregorianCalendar(TimeZone.getTimeZone("GMT")) :
                                new GregorianCalendar(TimeZone.getTimeZone(getRelationalDatabaseTimeZone()))
                );
                if (timestamp != null)
                {
                    result = PureDate.fromSQLTimestamp(timestamp);
                }
                break;
            }
            case Types.TINYINT:
            case Types.SMALLINT:
            case Types.INTEGER:
            case Types.BIGINT:
            {
                long num = this.resultSet.getLong(columnIndex);
                if (!this.resultSet.wasNull())
                {
                    result = Long.valueOf(num);
                }
                break;
            }
            case Types.REAL:
            case Types.FLOAT:
            case Types.DOUBLE:
            {
                double num = this.resultSet.getDouble(columnIndex);
                if (!this.resultSet.wasNull())
                {
                    result = Double.valueOf(num);
                }
                break;
            }
            case Types.DECIMAL:
            case Types.NUMERIC:
            {
                result = this.resultSet.getBigDecimal(columnIndex);
                break;
            }
            case Types.CHAR:
            case Types.VARCHAR:
            case Types.LONGVARCHAR:
            case Types.NCHAR:
            case Types.NVARCHAR:
            case Types.LONGNVARCHAR:
            case Types.OTHER:
            {
                result = this.resultSet.getString(columnIndex);
                break;
            }
            case Types.BIT:
            case Types.BOOLEAN:
            {
                boolean bool = this.resultSet.getBoolean(columnIndex);
                if (!this.resultSet.wasNull())
                {
                    result = Boolean.valueOf(bool);
                }
                break;  // TODO: check this
            }
            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:
            {
                byte[] bytes = this.resultSet.getBytes(columnIndex);
                if (bytes != null)
                {
                    result = BinaryUtils.encodeHex(bytes);
                }
                break;
            }
            case Types.NULL:
            {
                // do nothing: value is already assigned to null
                break;
            }
            default:
            {
                result = this.resultSet.getObject(columnIndex);
            }
        }
        return result;
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
        TimeZone timeZone = (timeZoneId != null) ? TimeZone.getTimeZone(timeZoneId) : TimeZone.getTimeZone("GMT");
        if (calendar == null)
        {
            //TODO, throw exception, TZ should always be specified
            //Till then, default to PURE default which is "GMT"
            calendar = new GregorianCalendar(timeZone);
        }
        else
        {
            calendar.clear();
            calendar.setTimeZone(timeZone);
        }
        return calendar;
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
            List<Column> columns = new ArrayList<>(this.resultSetMetaData.getColumnCount());
            Function<String, String> unquote = s -> s.startsWith("\"") && s.endsWith("\"") ? s.substring(1, s.length() - 1) : s;
            for (int i = 1; i <= this.resultSetMetaData.getColumnCount(); i++)
            {
                String columnType = JDBCType.valueOf(this.resultSetMetaData.getColumnType(i)).getName();
                String updatedColumnType = columnType.equals("TIMESTAMP_WITH_TIMEZONE") ? "TIMESTAMP WITH TIME ZONE" :
                        columnType.equals("TIME_WITH_TIMEZONE") ? "TIME WITH TIME ZONE" : columnType;
                columns.add(new Column(unquote.valueOf(this.resultSetMetaData.getColumnLabel(i)), updatedColumnType));
            }
            return columns;
        }
        catch (SQLException e)
        {
            this.close();
            throw new RuntimeException(e);
        }
    }
}
