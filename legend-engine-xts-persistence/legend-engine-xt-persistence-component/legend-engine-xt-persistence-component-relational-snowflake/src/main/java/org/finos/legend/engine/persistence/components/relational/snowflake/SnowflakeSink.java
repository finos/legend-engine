// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.persistence.components.relational.snowflake;

import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.common.StatisticName;
import org.finos.legend.engine.persistence.components.executor.Executor;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanFactory;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.ClusterKey;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetAdditionalProperties;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.StagedFilesDataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.StagedFilesDatasetReference;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.StagedFilesSelection;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Alter;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Copy;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Create;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Show;
import org.finos.legend.engine.persistence.components.logicalplan.values.BatchEndTimestamp;
import org.finos.legend.engine.persistence.components.logicalplan.values.DigestUdf;
import org.finos.legend.engine.persistence.components.logicalplan.values.StagedFilesFieldValue;
import org.finos.legend.engine.persistence.components.optimizer.Optimizer;
import org.finos.legend.engine.persistence.components.relational.CaseConversion;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.ansi.AnsiSqlSink;
import org.finos.legend.engine.persistence.components.relational.api.IngestStatus;
import org.finos.legend.engine.persistence.components.relational.api.IngestorResult;
import org.finos.legend.engine.persistence.components.relational.api.RelationalConnection;
import org.finos.legend.engine.persistence.components.relational.executor.RelationalExecutor;
import org.finos.legend.engine.persistence.components.relational.jdbc.JdbcConnection;
import org.finos.legend.engine.persistence.components.relational.jdbc.JdbcHelper;
import org.finos.legend.engine.persistence.components.relational.snowflake.optmizer.LowerCaseOptimizer;
import org.finos.legend.engine.persistence.components.relational.snowflake.optmizer.UpperCaseOptimizer;
import org.finos.legend.engine.persistence.components.relational.snowflake.sql.SnowflakeDataTypeMapping;
import org.finos.legend.engine.persistence.components.relational.snowflake.sql.SnowflakeJdbcPropertiesToLogicalDataTypeMapping;
import org.finos.legend.engine.persistence.components.relational.snowflake.sql.visitor.AlterVisitor;
import org.finos.legend.engine.persistence.components.relational.snowflake.sql.visitor.BatchEndTimestampVisitor;
import org.finos.legend.engine.persistence.components.relational.snowflake.sql.visitor.ClusterKeyVisitor;
import org.finos.legend.engine.persistence.components.relational.snowflake.sql.visitor.SQLCreateVisitor;
import org.finos.legend.engine.persistence.components.relational.snowflake.sql.visitor.SchemaDefinitionVisitor;
import org.finos.legend.engine.persistence.components.relational.snowflake.sql.visitor.FieldVisitor;
import org.finos.legend.engine.persistence.components.relational.snowflake.sql.visitor.ShowVisitor;
import org.finos.legend.engine.persistence.components.relational.snowflake.sql.visitor.DatasetAdditionalPropertiesVisitor;
import org.finos.legend.engine.persistence.components.relational.snowflake.sql.visitor.CopyVisitor;
import org.finos.legend.engine.persistence.components.relational.snowflake.sql.visitor.StagedFilesDatasetReferenceVisitor;
import org.finos.legend.engine.persistence.components.relational.snowflake.sql.visitor.StagedFilesDatasetVisitor;
import org.finos.legend.engine.persistence.components.relational.snowflake.sql.visitor.StagedFilesFieldValueVisitor;
import org.finos.legend.engine.persistence.components.relational.snowflake.sql.visitor.DigestUdfVisitor;
import org.finos.legend.engine.persistence.components.relational.snowflake.sql.visitor.StagedFilesSelectionVisitor;
import org.finos.legend.engine.persistence.components.relational.sql.TabularData;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlGen;
import org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils;
import org.finos.legend.engine.persistence.components.relational.transformer.RelationalTransformer;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.util.Capability;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.Objects;
import java.util.ArrayList;

import static org.finos.legend.engine.persistence.components.relational.api.RelationalIngestorAbstract.BATCH_ID_PATTERN;
import static org.finos.legend.engine.persistence.components.relational.api.RelationalIngestorAbstract.BATCH_START_TS_PATTERN;

public class SnowflakeSink extends AnsiSqlSink
{
    private static final RelationalSink INSTANCE;

    private static final Set<Capability> CAPABILITIES;
    private static final Map<Class<?>, LogicalPlanVisitor<?>> LOGICAL_PLAN_VISITOR_BY_CLASS;
    private static final Map<DataType, Set<DataType>> IMPLICIT_DATA_TYPE_MAPPING;
    private static final Map<DataType, Set<DataType>> EXPLICIT_DATA_TYPE_MAPPING;

    private static final String LOADED = "LOADED";
    private static final String FILE = "file";
    private static final String BULK_LOAD_STATUS = "status";
    private static final String ROWS_LOADED = "rows_loaded";
    private static final String ERRORS_SEEN = "errors_seen";

    static
    {
        Set<Capability> capabilities = new HashSet<>();
        capabilities.add(Capability.MERGE);
        capabilities.add(Capability.ADD_COLUMN);
        capabilities.add(Capability.IMPLICIT_DATA_TYPE_CONVERSION);
        capabilities.add(Capability.DATA_TYPE_LENGTH_CHANGE);
        capabilities.add(Capability.TRANSFORM_WHILE_COPY);
        CAPABILITIES = Collections.unmodifiableSet(capabilities);

        Map<Class<?>, LogicalPlanVisitor<?>> logicalPlanVisitorByClass = new HashMap<>();
        logicalPlanVisitorByClass.put(SchemaDefinition.class, new SchemaDefinitionVisitor());
        logicalPlanVisitorByClass.put(Create.class, new SQLCreateVisitor());
        logicalPlanVisitorByClass.put(ClusterKey.class, new ClusterKeyVisitor());
        logicalPlanVisitorByClass.put(Alter.class, new AlterVisitor());
        logicalPlanVisitorByClass.put(Show.class, new ShowVisitor());
        logicalPlanVisitorByClass.put(BatchEndTimestamp.class, new BatchEndTimestampVisitor());
        logicalPlanVisitorByClass.put(Field.class, new FieldVisitor());
        logicalPlanVisitorByClass.put(DatasetAdditionalProperties.class, new DatasetAdditionalPropertiesVisitor());
        logicalPlanVisitorByClass.put(Copy.class, new CopyVisitor());
        logicalPlanVisitorByClass.put(StagedFilesDatasetReference.class, new StagedFilesDatasetReferenceVisitor());
        logicalPlanVisitorByClass.put(StagedFilesDataset.class, new StagedFilesDatasetVisitor());
        logicalPlanVisitorByClass.put(StagedFilesFieldValue.class, new StagedFilesFieldValueVisitor());
        logicalPlanVisitorByClass.put(StagedFilesSelection.class, new StagedFilesSelectionVisitor());
        logicalPlanVisitorByClass.put(DigestUdf.class, new DigestUdfVisitor());

        LOGICAL_PLAN_VISITOR_BY_CLASS = Collections.unmodifiableMap(logicalPlanVisitorByClass);

        Map<DataType, Set<DataType>> implicitDataTypeMapping = new HashMap<>();
        implicitDataTypeMapping.put(DataType.DECIMAL, new HashSet<>(Arrays.asList(DataType.TINYINT, DataType.SMALLINT, DataType.INTEGER, DataType.INT, DataType.BIGINT, DataType.FLOAT, DataType.DOUBLE, DataType.REAL, DataType.NUMERIC)));
        implicitDataTypeMapping.put(DataType.DOUBLE, new HashSet<>(Arrays.asList(DataType.TINYINT, DataType.SMALLINT, DataType.INTEGER, DataType.INT, DataType.FLOAT, DataType.REAL)));
        implicitDataTypeMapping.put(DataType.BIGINT, new HashSet<>(Arrays.asList(DataType.TINYINT, DataType.SMALLINT, DataType.INTEGER, DataType.INT)));
        implicitDataTypeMapping.put(DataType.VARCHAR, new HashSet<>(Arrays.asList(DataType.CHAR, DataType.STRING, DataType.TEXT)));
        implicitDataTypeMapping.put(DataType.TIMESTAMP, Collections.singleton(DataType.DATETIME));
        IMPLICIT_DATA_TYPE_MAPPING = Collections.unmodifiableMap(implicitDataTypeMapping);

        EXPLICIT_DATA_TYPE_MAPPING = Collections.unmodifiableMap(new HashMap<>());

        INSTANCE = new SnowflakeSink();
    }

    public static RelationalSink get()
    {
        return INSTANCE;
    }

    public static Connection createConnection(String user,
                                              String pwd,
                                              String jdbcUrl,
                                              String account,
                                              String db,
                                              String schema)
    {
        Properties properties = new Properties();
        properties.put("user", user);
        properties.put("password", pwd);
        properties.put("account", account);
        properties.put("db", db);
        properties.put("schema", schema);

        try
        {
            return DriverManager.getConnection(jdbcUrl, properties);
        }
        catch (SQLException e)
        {
            throw new IllegalArgumentException(e);
        }
    }

    private SnowflakeSink()
    {
        super(
            CAPABILITIES,
            IMPLICIT_DATA_TYPE_MAPPING,
            EXPLICIT_DATA_TYPE_MAPPING,
            SqlGenUtils.QUOTE_IDENTIFIER,
            LOGICAL_PLAN_VISITOR_BY_CLASS,
            (executor, sink, dataset) ->
            {
                //TODO: pass transformer as an argument
                RelationalTransformer transformer = new RelationalTransformer(SnowflakeSink.get());
                LogicalPlan datasetExistLogicalPlan = LogicalPlanFactory.getLogicalPlanForDoesDatasetExist(dataset);
                SqlPlan physicalPlanForDoesDatasetExist = transformer.generatePhysicalPlan(datasetExistLogicalPlan);
                List<TabularData> results = executor.executePhysicalPlanAndGetResults(physicalPlanForDoesDatasetExist);
                return results.size() > 0;
            },
            (executor, sink, dataset) -> sink.validateDatasetSchema(dataset, new SnowflakeDataTypeMapping()),
            (executor, sink, dataset) -> sink.constructDatasetFromDatabase(dataset, new SnowflakeJdbcPropertiesToLogicalDataTypeMapping()));
    }

    @Override
    public Executor<SqlGen, TabularData, SqlPlan> getRelationalExecutor(RelationalConnection relationalConnection)
    {
        if (relationalConnection instanceof JdbcConnection)
        {
            JdbcConnection jdbcConnection = (JdbcConnection) relationalConnection;
            return new RelationalExecutor(this, JdbcHelper.of(jdbcConnection.connection()));
        }
        else
        {
            throw new UnsupportedOperationException("Only JdbcConnection is supported for Snowflake Sink");
        }
    }

    @Override
    public Optional<Optimizer> optimizerForCaseConversion(CaseConversion caseConversion)
    {
        switch (caseConversion)
        {
            case TO_LOWER:
                return Optional.of(new LowerCaseOptimizer());
            case TO_UPPER:
                return Optional.of(new UpperCaseOptimizer());
            case NONE:
                return Optional.empty();
            default:
                throw new IllegalArgumentException("Unrecognized case conversion: " + caseConversion);
        }
    }

    @Override
    public IngestorResult performBulkLoad(Datasets datasets, Executor<SqlGen, TabularData, SqlPlan> executor, SqlPlan ingestSqlPlan, Map<StatisticName, SqlPlan> statisticsSqlPlan, Map<String, String> placeHolderKeyValues)
    {
        List<TabularData> results = executor.executePhysicalPlanAndGetResults(ingestSqlPlan, placeHolderKeyValues);
        List<Map<String, Object>> resultSets = results.get(0).getData();
        List<String> dataFilePathsWithFailedBulkLoad = new ArrayList<>();

        long totalFilesLoaded = 0;
        long totalRowsLoaded = 0;
        long totalRowsWithError = 0;

        for (Map<String, Object> row: resultSets)
        {
            Object bulkLoadStatus = row.get(BULK_LOAD_STATUS);
            Object filePath = row.get(FILE);
            if (Objects.nonNull(bulkLoadStatus) && bulkLoadStatus.equals(LOADED))
            {
                totalFilesLoaded++;
                totalRowsLoaded += (Long) row.get(ROWS_LOADED);
            }

            if (Objects.nonNull(bulkLoadStatus) && !bulkLoadStatus.equals(LOADED) && Objects.nonNull(filePath))
            {
                dataFilePathsWithFailedBulkLoad.add(filePath.toString());
            }

            Object rowsWithError = row.get(ERRORS_SEEN);
            if (Objects.nonNull(rowsWithError))
            {
                totalRowsWithError += (Long) row.get(ERRORS_SEEN);
            }
        }

        Map<StatisticName, Object> stats = new HashMap<>();
        stats.put(StatisticName.ROWS_INSERTED, totalRowsLoaded);
        stats.put(StatisticName.ROWS_WITH_ERRORS, totalRowsWithError);
        stats.put(StatisticName.FILES_LOADED, totalFilesLoaded);

        IngestorResult.Builder resultBuilder = IngestorResult.builder()
            .updatedDatasets(datasets)
            .putAllStatisticByName(stats)
            .ingestionTimestampUTC(placeHolderKeyValues.get(BATCH_START_TS_PATTERN))
            .batchId(Optional.ofNullable(placeHolderKeyValues.containsKey(BATCH_ID_PATTERN) ? Integer.valueOf(placeHolderKeyValues.get(BATCH_ID_PATTERN)) : null));
        IngestorResult result;

        if (dataFilePathsWithFailedBulkLoad.isEmpty())
        {
            result = resultBuilder
                .status(IngestStatus.SUCCEEDED)
                .build();
        }
        else
        {
            String errorMessage = String.format("Unable to bulk load these files: %s", String.join(",", dataFilePathsWithFailedBulkLoad));
            result = resultBuilder
                .status(IngestStatus.FAILED)
                .message(errorMessage)
                .build();
        }
        return result;
    }

}
