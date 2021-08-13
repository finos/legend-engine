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

import org.finos.legend.engine.plan.execution.stores.relational.activity.RelationalExecutionActivity;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.result.builder.relation.RelationBuilder;
import org.finos.legend.engine.plan.execution.stores.relational.serialization.RelationalResultToCSVSerializer;
import org.finos.legend.engine.plan.execution.stores.relational.serialization.RelationalResultToJsonDefaultSerializer;
import org.finos.legend.engine.plan.execution.stores.relational.serialization.RelationalResultToPureTDSSerializer;
import org.finos.legend.engine.plan.execution.stores.relational.serialization.RelationalResultToPureTDSToObjectSerializer;

import io.opentracing.Span;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.plan.dependencies.domain.date.PureDate;
import org.finos.legend.engine.plan.dependencies.store.relational.IRelationalResult;
import org.finos.legend.engine.plan.execution.nodes.helpers.ExecutionNodeClassResultHelper;
import org.finos.legend.engine.plan.execution.nodes.helpers.ExecutionNodePartialClassResultHelper;
import org.finos.legend.engine.plan.execution.nodes.helpers.ExecutionNodeTDSResultHelper;
import org.finos.legend.engine.plan.execution.result.ErrorResult;
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
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.execution.result.serialization.Serializer;
import org.finos.legend.engine.plan.execution.result.transformer.SetImplTransformers;
import org.finos.legend.engine.plan.execution.result.transformer.TransformerInput;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.RelationalExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.RelationalInstantiationExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.result.TDSColumn;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.result.SQLResultColumn;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.pac4j.core.profile.CommonProfile;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.function.Consumer;

public class RelationalResult extends StreamingResult implements IRelationalResult
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");
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

    private final boolean statusEIB;
    private final SQLResultDBColumnsMetaData resultDBColumnsMetaData;

    public MutableList<SetImplTransformers> setTransformers = Lists.mutable.empty();

    public Builder builder;

    public RelationalResult(MutableList<ExecutionActivity> activities, RelationalExecutionNode node, List<SQLResultColumn> sqlResultColumns, String databaseType, String databaseTimeZone, Connection connection, MutableList<CommonProfile> profiles, List<String> temporaryTables, Span topSpan)
    {
        super(activities);
        this.databaseType = databaseType;
        this.databaseTimeZone = databaseTimeZone;
        this.temporaryTables = temporaryTables;
        this.topSpan = topSpan;

        try
        {
            this.connection = connection;
            this.statement = connection.createStatement();
            long start = System.currentTimeMillis();
            String sql = ((RelationalExecutionActivity) activities.getLast()).sql;
            LOGGER.info(new LogInfo(profiles, LoggingEventType.EXECUTION_RELATIONAL_START, sql).toString());
            this.resultSet = this.statement.executeQuery(sql);
            this.executedSQl = sql;
            LOGGER.info(new LogInfo(profiles, LoggingEventType.EXECUTION_RELATIONAL_STOP, (double)System.currentTimeMillis() - start).toString());
            this.resultSetMetaData = resultSet.getMetaData();
            this.columnCount = this.resultSetMetaData.getColumnCount();
            this.resultColumns = sqlResultColumns;

            this.statusEIB = true;
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

        try
        {
            System.out.println("NORMAL ROUTE: relationalresult, sqlexecution result");

            this.connection = sqlExecutionResult.getConnection();

            this.statement = connection.createStatement();
            this.resultSet = sqlExecutionResult.getResultSet();

            this.executedSQl = sqlExecutionResult.getExecutedSql();
            this.statusEIB = sqlExecutionResult.getstatusEIB();
            this.resultSetMetaData = sqlExecutionResult.getResultSetMetaData();
            this.columnCount = sqlExecutionResult.getColumnCount();
            this.sqlColumns = sqlExecutionResult.getColumnNames();
            this.columnListForSerializer = this.sqlColumns;
            this.resultColumns = sqlExecutionResult.getSqlResultColumns();
            this.resultDBColumnsMetaData = new SQLResultDBColumnsMetaData(this.resultColumns, this.resultSetMetaData);

            if (sqlExecutionResult.getstatusEIB() == true) {
               //Skipping building tansformers from result since this is ExecuteInDatabase
            }
            else {
                this.buildTransformersAndBuilder(node, sqlExecutionResult.getSQLExecutionNode().connection);
            }
        }
        catch (Throwable e)
        {
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
            List<TransformerInput<Integer>> transformerInputs = Lists.mutable.empty();
            for (int columnIndex = 1; columnIndex <= this.columnCount; columnIndex++)
            {
                TDSColumn c = ExecutionNodeTDSResultHelper.getTDSColumn(node, this.resultSetMetaData.getColumnLabel(columnIndex), isDatabaseIdentifiersCaseSensitive);
                transformerInputs.add(new TransformerInput<>(
                        columnIndex,
                        c.type,
                        (index) -> {
                            try
                            {
                                return ExecutionNodeTDSResultHelper.isTDSColumnEnum(node, this.resultSetMetaData.getColumnLabel(index), isDatabaseIdentifiersCaseSensitive);
                            }
                            catch (Exception e)
                            {
                                throw new RuntimeException(e);
                            }
                        },
                        (index) -> {
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
                            (colNameX) -> {
                                try
                                {
                                    return !TEMPORAL_DATE_ALIASES.contains(colNameX) && ExecutionNodeClassResultHelper.isClassPropertyEnum(node, classMappingInfo.setImplementationId, colNameX);
                                }
                                catch (Exception e)
                                {
                                    throw new RuntimeException(e);
                                }
                            },
                            (colNameX) -> {
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
            temporaryTables.forEach((Consumer<? super String>) table -> {
                try
                {
                    DatabaseManager databaseManager = DatabaseManager.fromString(this.databaseType);
                    statement.execute(databaseManager.relationalDatabaseSupport().dropTempTable(table));
                }
                catch (Exception ignore)
                {
                }
            });
        }
        if (resultSet != null)
        {
            try
            {
                resultSet.close();
            }
            catch (Exception e)
            {
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
            if (getRelationalDatabaseTimeZone() != null)
            {
                ts = resultSet.getTimestamp(columnIndex, new GregorianCalendar(TimeZone.getTimeZone(getRelationalDatabaseTimeZone())));
            }
            else
            {
                //TODO, throw exception, TZ should always be specified
                //Till then, default to PURE default which is "GMT"
                ts = resultSet.getTimestamp(columnIndex, new GregorianCalendar(TimeZone.getTimeZone("GMT")));
            }
            result = ts;
        }
        else if (resultDBColumnsMetaData.isDateColumn(columnIndex))
        {
            result = resultSet.getDate(columnIndex);
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
            return new ErrorResult(-1, "Error realizing the relational result in memory : " + e.getMessage());
        }
    }

    @Override
    public Serializer getSerializer(SerializationFormat format)
    {
        switch (format)
        {
            case PURE:
                return new RelationalResultToPureTDSSerializer(this);
            case PURE_TDSOBJECT:
                return new RelationalResultToPureTDSToObjectSerializer(this);
            case CSV:
                return new RelationalResultToCSVSerializer(this, true);
            case DEFAULT:
                return new RelationalResultToJsonDefaultSerializer(this);
            default:
                this.close();
                throw new RuntimeException(format.toString() + " format not currently supported with RelationalResult");
        }
    }
}