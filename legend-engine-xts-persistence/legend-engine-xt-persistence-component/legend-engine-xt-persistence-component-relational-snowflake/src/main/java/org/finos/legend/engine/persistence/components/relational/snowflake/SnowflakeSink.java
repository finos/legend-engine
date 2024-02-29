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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.finos.legend.engine.persistence.components.logicalplan.values.FieldValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.MetadataFileNameField;
import org.finos.legend.engine.persistence.components.logicalplan.values.MetadataRowNumberField;
import org.finos.legend.engine.persistence.components.logicalplan.values.StagedFilesFieldValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.TryCastFunction;
import org.finos.legend.engine.persistence.components.optimizer.Optimizer;
import org.finos.legend.engine.persistence.components.relational.CaseConversion;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.ansi.AnsiSqlSink;
import org.finos.legend.engine.persistence.components.relational.api.DataError;
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
import org.finos.legend.engine.persistence.components.relational.snowflake.sql.visitor.MetadataFileNameFieldVisitor;
import org.finos.legend.engine.persistence.components.relational.snowflake.sql.visitor.MetadataRowNumberFieldVisitor;
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
import org.finos.legend.engine.persistence.components.relational.snowflake.sql.visitor.TryCastFunctionVisitor;
import org.finos.legend.engine.persistence.components.relational.sql.TabularData;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlGen;
import org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils;
import org.finos.legend.engine.persistence.components.relational.transformer.RelationalTransformer;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.transformer.Transformer;
import org.finos.legend.engine.persistence.components.util.Capability;
import org.finos.legend.engine.persistence.components.util.PlaceholderValue;
import org.finos.legend.engine.persistence.components.util.ValidationCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.ArrayList;
import java.util.stream.Collectors;

import static org.finos.legend.engine.persistence.components.relational.api.RelationalIngestorAbstract.BATCH_ID_PATTERN;
import static org.finos.legend.engine.persistence.components.relational.api.RelationalIngestorAbstract.BATCH_START_TS_PATTERN;

public class SnowflakeSink extends AnsiSqlSink
{
    private static final Logger LOGGER = LoggerFactory.getLogger(SnowflakeSink.class);
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
    private static final String FIRST_ERROR = "first_error";
    private static final String FIRST_ERROR_COLUMN_NAME = "first_error_column_name";

    private static final String ERROR = "ERROR";
    private static final String FILE_WITH_ERROR = "FILE";
    private static final String LINE = "LINE";
    private static final String CHARACTER = "CHARACTER";
    private static final String BYTE_OFFSET = "BYTE_OFFSET";
    private static final String CATEGORY = "CATEGORY";
    private static final String COLUMN_NAME = "COLUMN_NAME";
    private static final String ROW_NUMBER = "ROW_NUMBER";
    private static final String ROW_START_LINE = "ROW_START_LINE";

    private static final String REJECTED_RECORD = "REJECTED_RECORD";

    static
    {
        Set<Capability> capabilities = new HashSet<>();
        capabilities.add(Capability.MERGE);
        capabilities.add(Capability.ADD_COLUMN);
        capabilities.add(Capability.IMPLICIT_DATA_TYPE_CONVERSION);
        capabilities.add(Capability.DATA_TYPE_LENGTH_CHANGE);
        capabilities.add(Capability.TRANSFORM_WHILE_COPY);
        capabilities.add(Capability.DRY_RUN);
        capabilities.add(Capability.TRY_CAST);
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
        logicalPlanVisitorByClass.put(TryCastFunction.class, new TryCastFunctionVisitor());
        logicalPlanVisitorByClass.put(MetadataFileNameField.class, new MetadataFileNameFieldVisitor());
        logicalPlanVisitorByClass.put(MetadataRowNumberField.class, new MetadataRowNumberFieldVisitor());

        LOGICAL_PLAN_VISITOR_BY_CLASS = Collections.unmodifiableMap(logicalPlanVisitorByClass);

        Map<DataType, Set<DataType>> implicitDataTypeMapping = new HashMap<>();
        implicitDataTypeMapping.put(DataType.DECIMAL, new HashSet<>(Arrays.asList(DataType.TINYINT, DataType.SMALLINT, DataType.INTEGER, DataType.INT, DataType.BIGINT, DataType.FLOAT, DataType.DOUBLE, DataType.REAL, DataType.NUMERIC)));
        implicitDataTypeMapping.put(DataType.DOUBLE, new HashSet<>(Arrays.asList(DataType.TINYINT, DataType.SMALLINT, DataType.INTEGER, DataType.INT, DataType.FLOAT, DataType.REAL)));
        implicitDataTypeMapping.put(DataType.BIGINT, new HashSet<>(Arrays.asList(DataType.TINYINT, DataType.SMALLINT, DataType.INTEGER, DataType.INT)));
        implicitDataTypeMapping.put(DataType.VARCHAR, new HashSet<>(Arrays.asList(DataType.CHAR, DataType.STRING, DataType.TEXT)));
        implicitDataTypeMapping.put(DataType.TIMESTAMP, Collections.singleton(DataType.DATETIME));
        implicitDataTypeMapping.put(DataType.JSON, Collections.singleton(DataType.VARIANT));
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

    public List<DataError> performDryRun(Transformer<SqlGen, SqlPlan> transformer, Executor<SqlGen, TabularData, SqlPlan> executor, SqlPlan dryRunSqlPlan, Map<ValidationCategory, Map<Set<FieldValue>, SqlPlan>> dryRunValidationSqlPlan, int sampleRowCount)
    {
        if (dryRunValidationSqlPlan == null || dryRunValidationSqlPlan.isEmpty())
        {
            return performDryRunWithValidationMode(executor, dryRunSqlPlan, sampleRowCount);
        }
        else
        {
            return performDryRunWithValidationQueries(executor, dryRunSqlPlan, dryRunValidationSqlPlan, sampleRowCount);
        }
    }

    private List<DataError> performDryRunWithValidationMode(Executor<SqlGen, TabularData, SqlPlan> executor, SqlPlan dryRunSqlPlan, int sampleRowCount)
    {
        List<TabularData> results = executor.executePhysicalPlanAndGetResults(dryRunSqlPlan, sampleRowCount);
        List<DataError> dataErrors = new ArrayList<>();

        if (!results.isEmpty())
        {
            List<Map<String, Object>> resultSets = results.get(0).getData();
            for (Map<String, Object> row : resultSets)
            {
                DataError dataError = DataError.builder()
                    .errorMessage(getString(row, ERROR).orElseThrow(IllegalStateException::new))
                    .file(getString(row, FILE_WITH_ERROR).orElseThrow(IllegalStateException::new))
                    .errorCategory(getString(row, CATEGORY).orElseThrow(IllegalStateException::new))
                    .columnName(getString(row, COLUMN_NAME))
                    .lineNumber(getLong(row, LINE))
                    .characterPosition(getLong(row, CHARACTER))
                    .rowNumber(getLong(row, ROW_NUMBER))
                    .rowStartLineNumber(getLong(row, ROW_START_LINE))
                    .rejectedRecord(getString(row, REJECTED_RECORD))
                    .build();
                dataErrors.add(dataError);
            }
        }
        return dataErrors;
    }

    private List<DataError> performDryRunWithValidationQueries(Executor<SqlGen, TabularData, SqlPlan> executor, SqlPlan dryRunSqlPlan, Map<ValidationCategory, Map<Set<FieldValue>, SqlPlan>> dryRunValidationSqlPlan, int sampleRowCount)
    {
        executor.executePhysicalPlan(dryRunSqlPlan);

        List<DataError> dataErrors = new ArrayList<>();
        Map<ValidationCategory, Queue<DataError>> dataErrorsByCategory = new HashMap<>();
        for (ValidationCategory validationCategory : ValidationCategory.values())
        {
            dataErrorsByCategory.put(validationCategory, new LinkedList<>());
        }

        for (ValidationCategory validationCategory : dryRunValidationSqlPlan.keySet())
        {
            for (Set<FieldValue> validatedColumns : dryRunValidationSqlPlan.get(validationCategory).keySet())
            {
                List<TabularData> results = executor.executePhysicalPlanAndGetResults(dryRunValidationSqlPlan.get(validationCategory).get(validatedColumns));
                if (!results.isEmpty())
                {
                    List<Map<String, Object>> resultSets = results.get(0).getData();
                    for (Map<String, Object> row : resultSets)
                    {
                        for (String column : validatedColumns.stream().map(FieldValue::fieldName).collect(Collectors.toSet()))
                        {
                            if (row.get(column) == null)
                            {
                                DataError dataError = constructDataError(row, FILE_WITH_ERROR, ROW_NUMBER, validationCategory, column);
                                dataErrors.add(dataError);
                                dataErrorsByCategory.get(validationCategory).add(dataError);
                            }
                        }
                    }
                }
            }
        }

        if (dataErrors.size() <= sampleRowCount)
        {
            return dataErrors;
        }
        else
        {
            return getDataErrorsWithEqualDistributionAcrossCategories(sampleRowCount, dataErrorsByCategory);
        }
    }

    @Override
    public IngestorResult performBulkLoad(Datasets datasets, Executor<SqlGen, TabularData, SqlPlan> executor, SqlPlan ingestSqlPlan, Map<StatisticName, SqlPlan> statisticsSqlPlan, Map<String, PlaceholderValue> placeHolderKeyValues)
    {
        List<TabularData> results = executor.executePhysicalPlanAndGetResults(ingestSqlPlan, placeHolderKeyValues);
        List<Map<String, Object>> resultSets = results.get(0).getData();
        List<String> dataFilePathsWithErrors = new ArrayList<>();

        long totalFilesLoaded = 0;
        long totalRowsLoaded = 0;
        long totalRowsWithError = 0;
        List<String> errorMessages = new ArrayList<>();

        for (Map<String, Object> row: resultSets)
        {
            Optional<String> bulkLoadStatus = getString(row, BULK_LOAD_STATUS);
            Optional<String> filePath = getString(row, FILE);
            if (bulkLoadStatus.isPresent() && filePath.isPresent())
            {
                if (bulkLoadStatus.get().equals(LOADED))
                {
                    totalFilesLoaded++;
                }
                else
                {
                    // if partially loaded or load failed
                    dataFilePathsWithErrors.add(filePath.get());
                    errorMessages.add(getErrorMessage(row));
                }
            }

            Optional<Long> rowsWithError = getLong(row, ERRORS_SEEN);
            if (rowsWithError.isPresent())
            {
                totalRowsWithError += rowsWithError.get();
            }

            Optional<Long> rowsLoaded = getLong(row, ROWS_LOADED);
            if (rowsLoaded.isPresent())
            {
                totalRowsLoaded += rowsLoaded.get();
            }
        }

        Map<StatisticName, Object> stats = new HashMap<>();
        stats.put(StatisticName.ROWS_INSERTED, totalRowsLoaded);
        stats.put(StatisticName.ROWS_WITH_ERRORS, totalRowsWithError);
        stats.put(StatisticName.FILES_LOADED, totalFilesLoaded);

        IngestorResult.Builder resultBuilder = IngestorResult.builder()
            .updatedDatasets(datasets)
            .putAllStatisticByName(stats)
            .ingestionTimestampUTC(placeHolderKeyValues.get(BATCH_START_TS_PATTERN).value())
            .batchId(Optional.ofNullable(placeHolderKeyValues.containsKey(BATCH_ID_PATTERN) ? Integer.valueOf(placeHolderKeyValues.get(BATCH_ID_PATTERN).value()) : null));
        IngestorResult result;

        if (dataFilePathsWithErrors.isEmpty())
        {
            result = resultBuilder
                .status(IngestStatus.SUCCEEDED)
                .build();
        }
        else
        {
            String errorMessage = "Errors encountered: " + String.join(",", errorMessages);
            LOGGER.error(errorMessage);
            result = resultBuilder
                .status(IngestStatus.FAILED)
                .message(errorMessage)
                .build();
        }
        return result;
    }

    private String getErrorMessage(Map<String, Object> row)
    {
        Map<String, Object> errorInfoMap = new HashMap<>();
        errorInfoMap.put(FILE, row.get(FILE));
        errorInfoMap.put(BULK_LOAD_STATUS, row.get(BULK_LOAD_STATUS));
        errorInfoMap.put(ERRORS_SEEN, row.get(ERRORS_SEEN));
        errorInfoMap.put(FIRST_ERROR, row.get(FIRST_ERROR));
        errorInfoMap.put(FIRST_ERROR_COLUMN_NAME, row.get(FIRST_ERROR_COLUMN_NAME));

        ObjectMapper objectMapper = new ObjectMapper();
        try
        {
            return objectMapper.writeValueAsString(errorInfoMap);
        }
        catch (JsonProcessingException e)
        {
            throw new RuntimeException(e);
        }
    }
}
