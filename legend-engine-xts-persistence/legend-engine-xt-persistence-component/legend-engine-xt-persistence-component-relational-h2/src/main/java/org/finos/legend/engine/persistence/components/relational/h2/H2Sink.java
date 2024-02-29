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
import org.finos.legend.engine.persistence.components.logicalplan.values.CastFunction;
import org.finos.legend.engine.persistence.components.logicalplan.values.DigestUdf;
import org.finos.legend.engine.persistence.components.logicalplan.values.FieldValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.HashFunction;
import org.finos.legend.engine.persistence.components.logicalplan.values.MetadataFileNameField;
import org.finos.legend.engine.persistence.components.logicalplan.values.MetadataRowNumberField;
import org.finos.legend.engine.persistence.components.logicalplan.values.ParseJsonFunction;
import org.finos.legend.engine.persistence.components.logicalplan.values.StagedFilesFieldValue;
import org.finos.legend.engine.persistence.components.optimizer.Optimizer;
import org.finos.legend.engine.persistence.components.relational.CaseConversion;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.ansi.AnsiSqlSink;
import org.finos.legend.engine.persistence.components.relational.ansi.optimizer.LowerCaseOptimizer;
import org.finos.legend.engine.persistence.components.relational.ansi.optimizer.UpperCaseOptimizer;
import org.finos.legend.engine.persistence.components.relational.api.DataError;
import org.finos.legend.engine.persistence.components.relational.api.IngestStatus;
import org.finos.legend.engine.persistence.components.relational.api.IngestorResult;
import org.finos.legend.engine.persistence.components.relational.h2.logicalplan.values.ToArrayFunction;
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
import org.finos.legend.engine.persistence.components.relational.h2.sql.visitor.CastFunctionVisitor;
import org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.transformer.Transformer;
import org.finos.legend.engine.persistence.components.util.Capability;
import org.finos.legend.engine.persistence.components.relational.jdbc.JdbcConnection;
import org.finos.legend.engine.persistence.components.relational.jdbc.JdbcHelper;
import org.finos.legend.engine.persistence.components.relational.sql.TabularData;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlGen;
import org.finos.legend.engine.persistence.components.relational.api.RelationalConnection;
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
import static org.finos.legend.engine.persistence.components.util.ValidationCategory.DATATYPE_CONVERSION;
import static org.finos.legend.engine.persistence.components.util.ValidationCategory.NULL_VALUES;

public class H2Sink extends AnsiSqlSink
{
    private static final RelationalSink INSTANCE;

    private static final Set<Capability> CAPABILITIES;
    private static final Map<Class<?>, LogicalPlanVisitor<?>> LOGICAL_PLAN_VISITOR_BY_CLASS;
    private static final Map<DataType, Set<DataType>> IMPLICIT_DATA_TYPE_MAPPING;
    private static final Map<DataType, Set<DataType>> EXPLICIT_DATA_TYPE_MAPPING;

    private static final String FILE = "FILE";
    private static final String ROW_NUMBER = "ROW_NUMBER";

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
        logicalPlanVisitorByClass.put(CastFunction.class, new CastFunctionVisitor());
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
            (executor, sink, dataset) -> sink.constructDatasetFromDatabase(dataset, new H2JdbcPropertiesToLogicalDataTypeMapping()));
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

    public List<DataError> performDryRun(Transformer<SqlGen, SqlPlan> transformer, Executor<SqlGen, TabularData, SqlPlan> executor, SqlPlan dryRunSqlPlan, Map<ValidationCategory, Map<Set<FieldValue>, SqlPlan>> dryRunValidationSqlPlan, int sampleRowCount)
    {
        executor.executePhysicalPlan(dryRunSqlPlan);

        List<DataError> dataErrors = new ArrayList<>();
        Map<ValidationCategory, Queue<DataError>> dataErrorsByCategory = new HashMap<>();
        for (ValidationCategory validationCategory : ValidationCategory.values())
        {
            dataErrorsByCategory.put(validationCategory, new LinkedList<>());
        }

        Map<Set<FieldValue>, SqlPlan> queriesForNull =  dryRunValidationSqlPlan.getOrDefault(NULL_VALUES, new HashMap<>());
        Map<Set<FieldValue>, SqlPlan> queriesForDatatype = dryRunValidationSqlPlan.getOrDefault(DATATYPE_CONVERSION, new HashMap<>());

        // Execute queries for null values
        for (Set<FieldValue> validatedColumns : queriesForNull.keySet())
        {
            List<TabularData> results = executor.executePhysicalPlanAndGetResults(queriesForNull.get(validatedColumns));
            if (!results.isEmpty())
            {
                List<Map<String, Object>> resultSets = results.get(0).getData();
                for (Map<String, Object> row : resultSets)
                {
                    for (String column : validatedColumns.stream().map(FieldValue::fieldName).collect(Collectors.toSet()))
                    {
                        if (row.get(column) == null)
                        {
                            DataError dataError = constructDataError(row, FILE, ROW_NUMBER, NULL_VALUES, column);
                            dataErrors.add(dataError);
                            dataErrorsByCategory.get(NULL_VALUES).add(dataError);
                        }
                    }
                }
            }
        }

        // Execute queries for datatype conversion
        for (Set<FieldValue> validatedColumns : queriesForDatatype.keySet())
        {
            try
            {
                executor.executePhysicalPlanAndGetResults(queriesForDatatype.get(validatedColumns));
            }
            catch (RuntimeException e)
            {
                String errorMessage = e.getCause().getMessage();
                String problematicValue = errorMessage.substring(0, errorMessage.indexOf("; SQL statement"));
                problematicValue = problematicValue.replace("org.h2.jdbc.JdbcSQLDataException: Data conversion error converting ", "");
                problematicValue = problematicValue.replace("org.h2.jdbc.JdbcSQLDataException: Cannot parse \"TIMESTAMP\" constant ", "");
                problematicValue = problematicValue.replaceAll("\"", "");

                // This loop will only be executed once as there is always only one element in the set
                for (FieldValue validatedColumn : validatedColumns)
                {
                    List<TabularData> results = executor.executePhysicalPlanAndGetResults(transformer.generatePhysicalPlan(LogicalPlanFactory.getLogicalPlanForSelectAllFieldsWithStringFieldEquals(validatedColumn, problematicValue)), sampleRowCount);
                    if (!results.isEmpty())
                    {
                        List<Map<String, Object>> resultSets = results.get(0).getData();
                        for (Map<String, Object> row : resultSets)
                        {
                            DataError dataError = constructDataError(row, FILE, ROW_NUMBER, DATATYPE_CONVERSION, validatedColumn.fieldName());
                            dataErrors.add(dataError);
                            dataErrorsByCategory.get(DATATYPE_CONVERSION).add(dataError);
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
}
