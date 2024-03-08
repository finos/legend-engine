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
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.common.FileFormatType;
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
import org.finos.legend.engine.persistence.components.relational.api.ErrorCategory;
import org.finos.legend.engine.persistence.components.relational.api.IngestStatus;
import org.finos.legend.engine.persistence.components.relational.api.IngestorResult;
import org.finos.legend.engine.persistence.components.relational.api.RelationalConnection;
import org.finos.legend.engine.persistence.components.relational.executor.RelationalExecutor;
import org.finos.legend.engine.persistence.components.relational.jdbc.JdbcConnection;
import org.finos.legend.engine.persistence.components.relational.jdbc.JdbcHelper;
import org.finos.legend.engine.persistence.components.relational.snowflake.logicalplan.datasets.SnowflakeStagedFilesDatasetProperties;
import org.finos.legend.engine.persistence.components.relational.snowflake.logicalplan.datasets.StandardFileFormat;
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

import java.io.IOException;
import java.io.StringReader;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.finos.legend.engine.persistence.components.relational.api.RelationalIngestorAbstract.BATCH_ID_PATTERN;
import static org.finos.legend.engine.persistence.components.relational.api.RelationalIngestorAbstract.BATCH_START_TS_PATTERN;
import static org.finos.legend.engine.persistence.components.util.ValidationCategory.NULL_VALUE;
import static org.finos.legend.engine.persistence.components.util.ValidationCategory.TYPE_CONVERSION;

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
    protected static final String FILE_WITH_ERROR = "FILE";
    protected static final String ROW_NUMBER = "ROW_NUMBER";
    private static final String LINE = "LINE";
    private static final String CHARACTER = "CHARACTER";
    private static final String CATEGORY = "CATEGORY";
    private static final String COLUMN_NAME = "COLUMN_NAME";
    private static final String REJECTED_RECORD = "REJECTED_RECORD";
    private static final String FIELD_DELIMITER = "FIELD_DELIMITER";
    private static final String ESCAPE = "ESCAPE";
    private static final String FIELD_OPTIONALLY_ENCLOSED_BY = "FIELD_OPTIONALLY_ENCLOSED_BY";
    private static final String CATEGORY_CONVERSION = "conversion";
    private static final String CATEGORY_CHECK_CONSTRAINT = "check_constraint";

    static
    {
        Set<Capability> capabilities = new HashSet<>();
        capabilities.add(Capability.MERGE);
        capabilities.add(Capability.ADD_COLUMN);
        capabilities.add(Capability.IMPLICIT_DATA_TYPE_CONVERSION);
        capabilities.add(Capability.DATA_TYPE_LENGTH_CHANGE);
        capabilities.add(Capability.TRANSFORM_WHILE_COPY);
        capabilities.add(Capability.DRY_RUN);
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

    public List<DataError> performDryRun(Datasets datasets, Transformer<SqlGen, SqlPlan> transformer, Executor<SqlGen, TabularData, SqlPlan> executor,
                                         SqlPlan dryRunSqlPlan, Map<ValidationCategory, List<Pair<Set<FieldValue>, SqlPlan>>> dryRunValidationSqlPlan,
                                         int sampleRowCount, CaseConversion caseConversion)
    {
        try
        {
            if (dryRunValidationSqlPlan == null || dryRunValidationSqlPlan.isEmpty())
            {
                return performDryRunWithValidationMode(datasets, executor, dryRunSqlPlan, sampleRowCount);
            }
            else
            {
                return performDryRunWithValidationQueries(datasets, executor, dryRunSqlPlan, dryRunValidationSqlPlan, sampleRowCount, caseConversion);
            }
        }
        catch (Exception e)
        {
            return parseSnowflakeExceptions(e);
        }
    }

    private List<DataError> parseSnowflakeExceptions(Exception e)
    {
        String errorMessage = e.getMessage();

        if (errorMessage.contains("Error parsing"))
        {
            return Collections.singletonList(DataError.builder().errorCategory(ErrorCategory.PARSING_ERROR).errorMessage(errorMessage).build());
        }

        if (errorMessage.contains("file") && errorMessage.contains("was not found"))
        {
            Optional<String> fileName = Optional.empty();
            Matcher matcher = Pattern.compile("file '(.*)' was not found").matcher(errorMessage);
            if (matcher.find())
            {
                fileName = Optional.of(matcher.group(1));
            }
            Map<String, Object> errorDetails = buildErrorDetails(fileName, Optional.empty(), Optional.empty());

            return Collections.singletonList(DataError.builder().errorCategory(ErrorCategory.FILE_NOT_FOUND).errorMessage(errorMessage).putAllErrorDetails(errorDetails).build());
        }

        return Collections.singletonList(DataError.builder().errorCategory(ErrorCategory.UNKNOWN).errorMessage(errorMessage).build());
    }

    private List<DataError> performDryRunWithValidationMode(Datasets datasets, Executor<SqlGen, TabularData, SqlPlan> executor, SqlPlan dryRunSqlPlan, int sampleRowCount)
    {
        List<TabularData> results = executor.executePhysicalPlanAndGetResults(dryRunSqlPlan, sampleRowCount);
        List<DataError> dataErrors = new ArrayList<>();

        if (!results.isEmpty())
        {
            List<Map<String, Object>> resultSets = results.get(0).getData();
            for (Map<String, Object> row : resultSets)
            {
                Map<String, Object> errorDetails = buildErrorDetails(getString(row, FILE_WITH_ERROR), getString(row, COLUMN_NAME), getLong(row, ROW_NUMBER));
                getLong(row, LINE).ifPresent(line -> errorDetails.put(DataError.LINE_NUMBER, line));
                getLong(row, CHARACTER).ifPresent(characterPos -> errorDetails.put(DataError.CHARACTER_POSITION, characterPos));

                DataError dataError = DataError.builder()
                    .errorMessage(getString(row, ERROR).orElseThrow(IllegalStateException::new))
                    .errorCategory(parseSnowflakeErrorCategory(row))
                    .putAllErrorDetails(errorDetails)
                    .errorRecord(getString(row, REJECTED_RECORD).map(rejectedRecord ->
                    {
                        try
                        {
                            return parseSnowflakeRejectedRecord(datasets, rejectedRecord);
                        }
                        catch (IOException e)
                        {
                            LOGGER.warn("Exception in parsing the record");
                            return String.format("{\"%s\" : \"%s\"}", "unparsed_row", rejectedRecord);
                        }
                    }))
                    .build();
                dataErrors.add(dataError);
            }
        }
        return dataErrors;
    }

    private ErrorCategory parseSnowflakeErrorCategory(Map<String, Object> row)
    {
        String snowflakeErrorCategory = getString(row, CATEGORY).orElseThrow(IllegalStateException::new);
        String errorMessage = getString(row, ERROR).orElseThrow(IllegalStateException::new);

        if (snowflakeErrorCategory.equals(CATEGORY_CONVERSION))
        {
            return ErrorCategory.TYPE_CONVERSION;
        }
        else if (snowflakeErrorCategory.equals(CATEGORY_CHECK_CONSTRAINT))
        {
            if (errorMessage.contains("NULL result in a non-nullable column"))
            {
                return ErrorCategory.CHECK_NULL_CONSTRAINT;
            }
            else
            {
                return ErrorCategory.CHECK_OTHER_CONSTRAINT;
            }
        }
        else
        {
            return ErrorCategory.UNKNOWN;
        }
    }

    public String parseSnowflakeRejectedRecord(Datasets datasets, String rejectedRecord) throws IOException
    {
        Map<String, Object> formatOptions = getFormatOptions(datasets);
        CSVFormat csvFormat = CSVFormat.DEFAULT.withQuote(null).withEscape(null);
        if (formatOptions.containsKey(FIELD_DELIMITER))
        {
            csvFormat = csvFormat.withDelimiter(getChar(formatOptions, FIELD_DELIMITER).orElseThrow(IllegalStateException::new));
        }
        if (formatOptions.containsKey(ESCAPE))
        {
            csvFormat = csvFormat.withEscape(getChar(formatOptions, ESCAPE).orElseThrow(IllegalStateException::new));
        }
        if (formatOptions.containsKey(FIELD_OPTIONALLY_ENCLOSED_BY))
        {
            csvFormat = csvFormat.withQuote(getChar(formatOptions, FIELD_OPTIONALLY_ENCLOSED_BY).orElseThrow(IllegalStateException::new));
        }

        List<String> allFields = datasets.stagingDataset().schemaReference().fieldValues().stream().map(FieldValue::fieldName).collect(Collectors.toList());
        Map<String, Object> errorRecordMap = new HashMap<>();

        List<CSVRecord> records = csvFormat.parse(new StringReader(rejectedRecord)).getRecords();
        for (CSVRecord csvRecord : records)
        {
            for (int i = 0; i < csvRecord.size(); i++)
            {
                errorRecordMap.put(allFields.get(i), csvRecord.get(i));
            }
        }

        return new ObjectMapper().writeValueAsString(errorRecordMap);
    }

    private Map<String, Object> getFormatOptions(Datasets datasets)
    {
        if (!(datasets.stagingDataset() instanceof StagedFilesDataset))
        {
            throw new IllegalStateException("StagedFilesDataset expected");
        }
        StagedFilesDataset stagedFilesDataset = (StagedFilesDataset) datasets.stagingDataset();
        if (!(stagedFilesDataset.stagedFilesDatasetProperties() instanceof SnowflakeStagedFilesDatasetProperties))
        {
            throw new IllegalStateException("SnowflakeStagedFilesDatasetProperties expected");
        }
        SnowflakeStagedFilesDatasetProperties snowflakeStagedFilesDatasetProperties = (SnowflakeStagedFilesDatasetProperties) stagedFilesDataset.stagedFilesDatasetProperties();
        if (!snowflakeStagedFilesDatasetProperties.fileFormat().isPresent() || !(snowflakeStagedFilesDatasetProperties.fileFormat().get() instanceof StandardFileFormat))
        {
            throw new IllegalStateException("StandardFileFormat expected");
        }
        StandardFileFormat standardFileFormat = (StandardFileFormat) snowflakeStagedFilesDatasetProperties.fileFormat().get();
        if (!standardFileFormat.formatType().equals(FileFormatType.CSV))
        {
            throw new IllegalStateException("CSV format expected");
        }
        return standardFileFormat.formatOptions();
    }

    private List<DataError> performDryRunWithValidationQueries(Datasets datasets, Executor<SqlGen, TabularData, SqlPlan> executor, SqlPlan dryRunSqlPlan, Map<ValidationCategory, List<Pair<Set<FieldValue>, SqlPlan>>> dryRunValidationSqlPlan, int sampleRowCount, CaseConversion caseConversion)
    {
        executor.executePhysicalPlan(dryRunSqlPlan);

        int dataErrorsTotalCount = 0;
        Map<ValidationCategory, Queue<DataError>> dataErrorsByCategory = new HashMap<>();
        for (ValidationCategory validationCategory : ValidationCategory.values())
        {
            dataErrorsByCategory.put(validationCategory, new LinkedList<>());
        }

        List<String> allFields = datasets.stagingDataset().schemaReference().fieldValues().stream().map(FieldValue::fieldName).collect(Collectors.toList());

        List<Pair<Set<FieldValue>, SqlPlan>> queriesForNull = dryRunValidationSqlPlan.getOrDefault(NULL_VALUE, new ArrayList<>());
        List<Pair<Set<FieldValue>, SqlPlan>> queriesForDatatype = dryRunValidationSqlPlan.getOrDefault(TYPE_CONVERSION, new ArrayList<>());

        // Execute queries for null values
        dataErrorsTotalCount += findNullValuesDataErrors(executor, queriesForNull, dataErrorsByCategory, allFields, caseConversion);
        // Execute queries for datatype conversion
        for (Pair<Set<FieldValue>, SqlPlan> pair : queriesForDatatype)
        {
            List<TabularData> results = executor.executePhysicalPlanAndGetResults(pair.getTwo());
            if (!results.isEmpty())
            {
                List<Map<String, Object>> resultSets = results.get(0).getData();
                for (Map<String, Object> row : resultSets)
                {
                    // This loop will only be executed once as there is always only one element in the set
                    for (String column : pair.getOne().stream().map(FieldValue::fieldName).collect(Collectors.toSet()))
                    {
                        DataError dataError = constructDataError(allFields, row, TYPE_CONVERSION, column, caseConversion);
                        dataErrorsByCategory.get(TYPE_CONVERSION).add(dataError);
                        dataErrorsTotalCount++;
                    }
                }
            }
        }

        return getDataErrorsWithFairDistributionAcrossCategories(sampleRowCount, dataErrorsTotalCount, dataErrorsByCategory);
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
