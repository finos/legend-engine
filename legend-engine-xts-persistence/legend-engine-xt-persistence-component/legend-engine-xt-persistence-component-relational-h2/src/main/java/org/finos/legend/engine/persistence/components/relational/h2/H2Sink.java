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

package org.finos.legend.engine.persistence.components.relational.h2;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.common.StatisticName;
import org.finos.legend.engine.persistence.components.executor.Executor;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanFactory;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.CsvExternalDatasetReference;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.StagedFilesDataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.StagedFilesDatasetReference;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.StagedFilesSelection;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Copy;
import org.finos.legend.engine.persistence.components.logicalplan.operations.LoadCsv;
import org.finos.legend.engine.persistence.components.logicalplan.values.DigestUdf;
import org.finos.legend.engine.persistence.components.logicalplan.values.FieldValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.HashFunction;
import org.finos.legend.engine.persistence.components.logicalplan.values.MetadataFileNameField;
import org.finos.legend.engine.persistence.components.logicalplan.values.MetadataRowNumberField;
import org.finos.legend.engine.persistence.components.logicalplan.values.ParseJsonFunction;
import org.finos.legend.engine.persistence.components.logicalplan.values.StagedFilesFieldValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.ToArrayFunction;
import org.finos.legend.engine.persistence.components.logicalplan.values.TryCastFunction;
import org.finos.legend.engine.persistence.components.relational.CaseConversion;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.ansi.AnsiSqlSink;
import org.finos.legend.engine.persistence.components.relational.api.DataError;
import org.finos.legend.engine.persistence.components.relational.api.RelationalConnection;
import org.finos.legend.engine.persistence.components.relational.api.ApiUtils;
import org.finos.legend.engine.persistence.components.relational.api.ErrorCategory;
import org.finos.legend.engine.persistence.components.relational.api.IngestStatus;
import org.finos.legend.engine.persistence.components.relational.api.IngestorResult;
import org.finos.legend.engine.persistence.components.relational.h2.sql.H2DataTypeMapping;
import org.finos.legend.engine.persistence.components.relational.h2.sql.H2JdbcPropertiesToLogicalDataTypeMapping;
import org.finos.legend.engine.persistence.components.relational.h2.sql.visitor.CopyVisitor;
import org.finos.legend.engine.persistence.components.relational.h2.sql.visitor.CsvExternalDatasetReferenceVisitor;
import org.finos.legend.engine.persistence.components.relational.h2.sql.visitor.DigestUdfVisitor;
import org.finos.legend.engine.persistence.components.relational.h2.sql.visitor.HashFunctionVisitor;
import org.finos.legend.engine.persistence.components.relational.h2.sql.visitor.LoadCsvVisitor;
import org.finos.legend.engine.persistence.components.relational.h2.sql.visitor.MetadataFileNameFieldVisitor;
import org.finos.legend.engine.persistence.components.relational.h2.sql.visitor.MetadataRowNumberFieldVisitor;
import org.finos.legend.engine.persistence.components.relational.h2.sql.visitor.SchemaDefinitionVisitor;
import org.finos.legend.engine.persistence.components.relational.h2.sql.visitor.ParseJsonFunctionVisitor;
import org.finos.legend.engine.persistence.components.relational.h2.sql.visitor.FieldVisitor;
import org.finos.legend.engine.persistence.components.relational.h2.sql.visitor.StagedFilesDatasetReferenceVisitor;
import org.finos.legend.engine.persistence.components.relational.h2.sql.visitor.StagedFilesDatasetVisitor;
import org.finos.legend.engine.persistence.components.relational.h2.sql.visitor.StagedFilesFieldValueVisitor;
import org.finos.legend.engine.persistence.components.relational.h2.sql.visitor.StagedFilesSelectionVisitor;
import org.finos.legend.engine.persistence.components.relational.h2.sql.visitor.ToArrayFunctionVisitor;
import org.finos.legend.engine.persistence.components.relational.h2.sql.visitor.TryCastFunctionVisitor;
import org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.transformer.Transformer;
import org.finos.legend.engine.persistence.components.util.Capability;
import org.finos.legend.engine.persistence.components.relational.jdbc.JdbcConnection;
import org.finos.legend.engine.persistence.components.relational.jdbc.JdbcHelper;
import org.finos.legend.engine.persistence.components.relational.sql.TabularData;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlGen;
import org.finos.legend.engine.persistence.components.relational.executor.RelationalExecutor;
import org.finos.legend.engine.persistence.components.util.PlaceholderValue;
import org.finos.legend.engine.persistence.components.util.ValidationCategory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import static org.finos.legend.engine.persistence.components.relational.api.RelationalIngestorAbstract.BATCH_ID_PATTERN;
import static org.finos.legend.engine.persistence.components.relational.api.RelationalIngestorAbstract.BATCH_START_TS_PATTERN;
import static org.finos.legend.engine.persistence.components.util.ValidationCategory.TYPE_CONVERSION;
import static org.finos.legend.engine.persistence.components.util.ValidationCategory.NULL_VALUE;

public class H2Sink extends AnsiSqlSink
{
    private static final RelationalSink INSTANCE;

    private static final Set<Capability> CAPABILITIES;
    private static final Map<Class<?>, LogicalPlanVisitor<?>> LOGICAL_PLAN_VISITOR_BY_CLASS;
    private static final Map<DataType, Set<DataType>> IMPLICIT_DATA_TYPE_MAPPING;
    private static final Map<DataType, Set<DataType>> EXPLICIT_DATA_TYPE_MAPPING;

    static
    {
        Set<Capability> capabilities = new HashSet<>();
        capabilities.add(Capability.MERGE);
        capabilities.add(Capability.ADD_COLUMN);
        capabilities.add(Capability.IMPLICIT_DATA_TYPE_CONVERSION);
        capabilities.add(Capability.EXPLICIT_DATA_TYPE_CONVERSION);
        capabilities.add(Capability.DATA_TYPE_LENGTH_CHANGE);
        capabilities.add(Capability.DATA_TYPE_SCALE_CHANGE);
        capabilities.add(Capability.TRANSFORM_WHILE_COPY);
        capabilities.add(Capability.DRY_RUN);
        CAPABILITIES = Collections.unmodifiableSet(capabilities);

        Map<Class<?>, LogicalPlanVisitor<?>> logicalPlanVisitorByClass = new HashMap<>();
        logicalPlanVisitorByClass.put(SchemaDefinition.class, new SchemaDefinitionVisitor());
        logicalPlanVisitorByClass.put(HashFunction.class, new HashFunctionVisitor());
        logicalPlanVisitorByClass.put(ParseJsonFunction.class, new ParseJsonFunctionVisitor());
        logicalPlanVisitorByClass.put(LoadCsv.class, new LoadCsvVisitor());
        logicalPlanVisitorByClass.put(CsvExternalDatasetReference.class, new CsvExternalDatasetReferenceVisitor());
        logicalPlanVisitorByClass.put(Field.class, new FieldVisitor());
        logicalPlanVisitorByClass.put(Copy.class, new CopyVisitor());
        logicalPlanVisitorByClass.put(StagedFilesDataset.class, new StagedFilesDatasetVisitor());
        logicalPlanVisitorByClass.put(StagedFilesSelection.class, new StagedFilesSelectionVisitor());
        logicalPlanVisitorByClass.put(StagedFilesDatasetReference.class, new StagedFilesDatasetReferenceVisitor());
        logicalPlanVisitorByClass.put(StagedFilesFieldValue.class, new StagedFilesFieldValueVisitor());
        logicalPlanVisitorByClass.put(DigestUdf.class, new DigestUdfVisitor());
        logicalPlanVisitorByClass.put(ToArrayFunction.class, new ToArrayFunctionVisitor());
        logicalPlanVisitorByClass.put(TryCastFunction.class, new TryCastFunctionVisitor());
        logicalPlanVisitorByClass.put(MetadataFileNameField.class, new MetadataFileNameFieldVisitor());
        logicalPlanVisitorByClass.put(MetadataRowNumberField.class, new MetadataRowNumberFieldVisitor());
        LOGICAL_PLAN_VISITOR_BY_CLASS = Collections.unmodifiableMap(logicalPlanVisitorByClass);

        Map<DataType, Set<DataType>> implicitDataTypeMapping = new HashMap<>();
        implicitDataTypeMapping.put(DataType.DECIMAL, new HashSet<>(Arrays.asList(DataType.TINYINT, DataType.SMALLINT, DataType.INTEGER, DataType.INT, DataType.BIGINT, DataType.FLOAT, DataType.DOUBLE, DataType.REAL, DataType.NUMERIC)));
        implicitDataTypeMapping.put(DataType.DOUBLE, new HashSet<>(Arrays.asList(DataType.TINYINT, DataType.SMALLINT, DataType.INTEGER, DataType.INT, DataType.FLOAT, DataType.REAL)));
        implicitDataTypeMapping.put(DataType.REAL, new HashSet<>(Arrays.asList(DataType.TINYINT, DataType.SMALLINT, DataType.INTEGER, DataType.INT, DataType.FLOAT, DataType.DOUBLE)));
        implicitDataTypeMapping.put(DataType.BIGINT, new HashSet<>(Arrays.asList(DataType.TINYINT, DataType.SMALLINT, DataType.INTEGER, DataType.INT)));
        implicitDataTypeMapping.put(DataType.INTEGER, new HashSet<>(Arrays.asList(DataType.INT, DataType.TINYINT, DataType.SMALLINT)));
        implicitDataTypeMapping.put(DataType.SMALLINT, Collections.singleton(DataType.TINYINT));
        implicitDataTypeMapping.put(DataType.VARCHAR, new HashSet<>(Arrays.asList(DataType.CHAR, DataType.STRING)));
        implicitDataTypeMapping.put(DataType.TIMESTAMP, Collections.singleton(DataType.DATETIME));
        IMPLICIT_DATA_TYPE_MAPPING = Collections.unmodifiableMap(implicitDataTypeMapping);

        Map<DataType, Set<DataType>> explicitDataTypeMapping = new HashMap<>();
        explicitDataTypeMapping.put(DataType.TINYINT, new HashSet<>(Arrays.asList(DataType.SMALLINT, DataType.INTEGER, DataType.INT, DataType.BIGINT, DataType.FLOAT, DataType.DOUBLE, DataType.DECIMAL, DataType.NUMERIC)));
        explicitDataTypeMapping.put(DataType.SMALLINT, new HashSet<>(Arrays.asList(DataType.INTEGER, DataType.INT, DataType.BIGINT, DataType.FLOAT, DataType.DOUBLE, DataType.DECIMAL, DataType.NUMERIC)));
        explicitDataTypeMapping.put(DataType.INTEGER, new HashSet<>(Arrays.asList(DataType.BIGINT, DataType.FLOAT, DataType.DOUBLE, DataType.DECIMAL, DataType.NUMERIC)));
        explicitDataTypeMapping.put(DataType.BIGINT, new HashSet<>(Arrays.asList(DataType.DECIMAL, DataType.NUMERIC)));
        explicitDataTypeMapping.put(DataType.REAL, new HashSet<>(Arrays.asList(DataType.DOUBLE, DataType.DECIMAL, DataType.NUMERIC)));
        explicitDataTypeMapping.put(DataType.DOUBLE, new HashSet<>(Arrays.asList(DataType.DECIMAL, DataType.NUMERIC)));
        explicitDataTypeMapping.put(DataType.CHAR, new HashSet<>(Arrays.asList(DataType.VARCHAR, DataType.LONGTEXT, DataType.STRING)));
        explicitDataTypeMapping.put(DataType.VARCHAR, Collections.singleton(DataType.LONGTEXT));
        EXPLICIT_DATA_TYPE_MAPPING = Collections.unmodifiableMap(explicitDataTypeMapping);

        INSTANCE = new H2Sink();
    }

    public static RelationalSink get()
    {
        return INSTANCE;
    }

    public static Connection createConnection(String user, String pwd, String jdbcUrl)
    {
        try
        {
            return DriverManager.getConnection(jdbcUrl, user, pwd);
        }
        catch (SQLException e)
        {
            throw new IllegalArgumentException(e);
        }
    }

    private H2Sink()
    {
        super(
            CAPABILITIES,
            IMPLICIT_DATA_TYPE_MAPPING,
            EXPLICIT_DATA_TYPE_MAPPING,
            SqlGenUtils.QUOTE_IDENTIFIER,
            LOGICAL_PLAN_VISITOR_BY_CLASS,
            (executor, sink, dataset) -> sink.doesTableExist(dataset),
            (executor, sink, dataset) -> sink.validateDatasetSchema(dataset, new H2DataTypeMapping()),
            (executor, sink, dataset) -> sink.constructDatasetFromDatabase(dataset, new H2JdbcPropertiesToLogicalDataTypeMapping(), false));
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
            throw new UnsupportedOperationException("Only JdbcConnection is supported for H2 Sink");
        }
    }

    @Override
    public IngestorResult performBulkLoad(Datasets datasets, Executor<SqlGen, TabularData, SqlPlan> executor, SqlPlan ingestSqlPlan, Map<StatisticName, SqlPlan> statisticsSqlPlan, Map<String, PlaceholderValue> placeHolderKeyValues)
    {
        executor.executePhysicalPlan(ingestSqlPlan, placeHolderKeyValues);

        Map<StatisticName, Object> stats = new HashMap<>();
        stats.put(StatisticName.FILES_LOADED, 1);
        stats.put(StatisticName.ROWS_WITH_ERRORS, 0);

        SqlPlan rowsInsertedSqlPlan = statisticsSqlPlan.get(StatisticName.ROWS_INSERTED);
        if (rowsInsertedSqlPlan != null)
        {
            stats.put(StatisticName.ROWS_INSERTED, executor.executePhysicalPlanAndGetResults(rowsInsertedSqlPlan, placeHolderKeyValues)
                .stream()
                .findFirst()
                .map(TabularData::getData)
                .flatMap(t -> t.stream().findFirst())
                .map(Map::values)
                .flatMap(t -> t.stream().findFirst())
                .orElseThrow(IllegalStateException::new));
        }

        IngestorResult result;
        result = IngestorResult.builder()
            .status(IngestStatus.SUCCEEDED)
            .batchId(Optional.ofNullable(placeHolderKeyValues.containsKey(BATCH_ID_PATTERN) ? Integer.valueOf(placeHolderKeyValues.get(BATCH_ID_PATTERN).value()) : null))
            .updatedDatasets(datasets)
            .putAllStatisticByName(stats)
            .ingestionTimestampUTC(placeHolderKeyValues.get(BATCH_START_TS_PATTERN).value())
            .build();

        return result;
    }

    public List<DataError> performDryRun(Datasets datasets, Transformer<SqlGen, SqlPlan> transformer, Executor<SqlGen, TabularData, SqlPlan> executor, SqlPlan dryRunSqlPlan, Map<ValidationCategory, List<Pair<Set<FieldValue>, SqlPlan>>> dryRunValidationSqlPlan, int sampleRowCount, CaseConversion caseConversion)
    {
        try
        {
            return performDryRunWithValidationQueries(datasets, transformer, executor, dryRunSqlPlan, dryRunValidationSqlPlan, sampleRowCount, caseConversion);
        }
        catch (Exception e)
        {
            return parseH2Exceptions(e);
        }
    }

    private List<DataError> parseH2Exceptions(Exception e)
    {
        String errorMessage = e.getMessage();
        String errorMessageWithoutLineBreak = ApiUtils.removeLineBreaks(errorMessage);

        if (errorMessage.contains("IO Exception"))
        {
            Optional<String> fileName = extractProblematicValueFromErrorMessage(errorMessage);
            Map<String, Object> errorDetails = buildErrorDetails(fileName, Optional.empty(), Optional.empty());
            return Collections.singletonList(DataError.builder().errorCategory(ErrorCategory.FILE_NOT_FOUND).errorMessage(ErrorCategory.FILE_NOT_FOUND.getDefaultErrorMessage()).putAllErrorDetails(errorDetails).build());
        }

        return Collections.singletonList(DataError.builder().errorCategory(ErrorCategory.UNKNOWN).errorMessage(errorMessageWithoutLineBreak).build());
    }

    public List<DataError> performDryRunWithValidationQueries(Datasets datasets, Transformer<SqlGen, SqlPlan> transformer, Executor<SqlGen, TabularData, SqlPlan> executor, SqlPlan dryRunSqlPlan, Map<ValidationCategory, List<Pair<Set<FieldValue>, SqlPlan>>> dryRunValidationSqlPlan, int sampleRowCount, CaseConversion caseConversion)
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
            try
            {
                executor.executePhysicalPlanAndGetResults(pair.getTwo());
            }
            catch (RuntimeException e)
            {
                Optional<String> problematicValue = extractProblematicValueFromErrorMessage(e.getCause().getMessage());
                if (problematicValue.isPresent())
                {
                    // This loop will only be executed once as there is always only one element in the set
                    for (FieldValue validatedColumn : pair.getOne())
                    {
                        List<TabularData> results = executor.executePhysicalPlanAndGetResults(transformer.generatePhysicalPlan(LogicalPlanFactory.getLogicalPlanForSelectAllFieldsWithStringFieldEquals(validatedColumn, problematicValue.get())), sampleRowCount);
                        if (!results.isEmpty())
                        {
                            List<Map<String, Object>> resultSets = results.get(0).getData();
                            for (Map<String, Object> row : resultSets)
                            {
                                DataError dataError = constructDataError(allFields, row, TYPE_CONVERSION, validatedColumn.fieldName(), caseConversion);
                                dataErrorsByCategory.get(TYPE_CONVERSION).add(dataError);
                                dataErrorsTotalCount++;
                            }
                        }
                    }
                }

            }
        }

        return getDataErrorsWithFairDistributionAcrossCategories(sampleRowCount, dataErrorsTotalCount, dataErrorsByCategory);
    }

    private Optional<String> extractProblematicValueFromErrorMessage(String errorMessage)
    {
        errorMessage = errorMessage.substring(0, errorMessage.indexOf("; SQL statement"));
        Optional<String> value = Optional.empty();
        if (errorMessage.contains("Data conversion error"))
        {
            value = ApiUtils.findToken(errorMessage, "Data conversion error converting \"(.*)\"", 1);
        }
        else if (errorMessage.contains("Cannot parse"))
        {
            value = ApiUtils.findToken(errorMessage, "Cannot parse \"(.*)\" constant \"(.*)\"", 2);
        }
        else if (errorMessage.contains("IO Exception"))
        {
            value = ApiUtils.findToken(errorMessage, "IO Exception: \"IOException reading (.*)\"", 1);
        }
        return value;
    }
}
