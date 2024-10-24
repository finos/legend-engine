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

package org.finos.legend.engine.persistence.components.relational.ansi;

import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.common.StatisticName;
import org.finos.legend.engine.persistence.components.executor.Executor;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.And;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Equals;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Exists;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.GreaterThan;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.GreaterThanEqualTo;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.In;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.IsNull;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.LessThan;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.LessThanEqualTo;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Not;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.NotEquals;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.NotIn;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Or;
import org.finos.legend.engine.persistence.components.logicalplan.constraints.CascadeTableConstraint;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetAdditionalProperties;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetReference;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetReferenceImpl;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DerivedDataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FilteredDataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Join;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.JsonExternalDatasetReference;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaReference;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Selection;
import org.finos.legend.engine.persistence.components.logicalplan.modifiers.IfExistsTableModifier;
import org.finos.legend.engine.persistence.components.logicalplan.modifiers.IfNotExistsTableModifier;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Alter;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Create;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Delete;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Drop;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Insert;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Merge;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Show;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Truncate;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Update;
import org.finos.legend.engine.persistence.components.logicalplan.quantifiers.AllQuantifier;
import org.finos.legend.engine.persistence.components.logicalplan.quantifiers.DistinctQuantifier;
import org.finos.legend.engine.persistence.components.logicalplan.values.All;
import org.finos.legend.engine.persistence.components.logicalplan.values.Array;
import org.finos.legend.engine.persistence.components.logicalplan.values.BatchEndTimestamp;
import org.finos.legend.engine.persistence.components.logicalplan.values.BatchIdValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.BatchStartTimestamp;
import org.finos.legend.engine.persistence.components.logicalplan.values.BulkLoadBatchStatusValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.Case;
import org.finos.legend.engine.persistence.components.logicalplan.values.DatetimeValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.DiffBinaryValueOperator;
import org.finos.legend.engine.persistence.components.logicalplan.values.FieldValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.FunctionImpl;
import org.finos.legend.engine.persistence.components.logicalplan.values.HashFunction;
import org.finos.legend.engine.persistence.components.logicalplan.values.InfiniteBatchIdValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.ModuloBinaryValueOperator;
import org.finos.legend.engine.persistence.components.logicalplan.values.NumericalValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.ObjectValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.OrderedField;
import org.finos.legend.engine.persistence.components.logicalplan.values.Pair;
import org.finos.legend.engine.persistence.components.logicalplan.values.ParseJsonFunction;
import org.finos.legend.engine.persistence.components.logicalplan.values.SelectValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.StringValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.SumBinaryValueOperator;
import org.finos.legend.engine.persistence.components.logicalplan.values.TabularValues;
import org.finos.legend.engine.persistence.components.logicalplan.values.Udf;
import org.finos.legend.engine.persistence.components.logicalplan.values.WindowFunction;
import org.finos.legend.engine.persistence.components.logicalplan.values.ApproxCountDistinct;
import org.finos.legend.engine.persistence.components.optimizer.Optimizer;
import org.finos.legend.engine.persistence.components.relational.CaseConversion;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.ansi.optimizer.LowerCaseOptimizer;
import org.finos.legend.engine.persistence.components.relational.ansi.optimizer.UpperCaseOptimizer;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.AllQuantifierVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.AllVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.AlterVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.AndVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.ArrayVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.BatchEndTimestampVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.BatchIdValueVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.BatchStartTimestampVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.BulkLoadBatchStatusValueVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.CaseVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.DatasetAdditionalPropertiesVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.DatasetDefinitionVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.DatasetReferenceVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.DatetimeValueVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.DeleteVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.DerivedDatasetVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.DiffBinaryValueOperatorVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.DistinctQuantifierVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.EqualsVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.ExistsConditionVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.FieldValueVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.FieldVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.FilteredDatasetVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.FunctionVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.GreaterThanEqualToVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.GreaterThanVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.HashFunctionVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.InVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.InfiniteBatchIdValueVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.InsertVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.IsNullVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.JoinOperationVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.LessThanEqualToVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.LessThanVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.ModuloBinaryValueOperatorVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.NotEqualsVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.NotInVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.NotVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.NumericalValueVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.ObjectValueVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.OrVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.OrderedFieldVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.PairVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.ParseJsonFunctionVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.SQLCreateVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.SQLDropVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.SQLMergeVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.SQLUpdateVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.SchemaDefinitionVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.SchemaReferenceVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.SelectValueVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.SelectionVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.ShowVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.StringValueVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.SumBinaryValueOperatorVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.TableConstraintVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.TableModifierVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.TabularValuesVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.TruncateVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.UdfVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.WindowFunctionVisitor;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.ApproxCountDistinctVisitor;
import org.finos.legend.engine.persistence.components.relational.api.DataError;
import org.finos.legend.engine.persistence.components.relational.api.utils.ApiUtils;
import org.finos.legend.engine.persistence.components.relational.api.ErrorCategory;
import org.finos.legend.engine.persistence.components.relational.api.IngestorResult;
import org.finos.legend.engine.persistence.components.relational.api.RelationalConnection;
import org.finos.legend.engine.persistence.components.executor.TabularData;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlGen;
import org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.transformer.Transformer;
import org.finos.legend.engine.persistence.components.util.Capability;
import org.finos.legend.engine.persistence.components.util.PlaceholderValue;
import org.finos.legend.engine.persistence.components.util.ValidationCategory;

import java.time.Clock;
import java.util.*;
import java.util.stream.Collectors;

import static org.finos.legend.engine.persistence.components.relational.api.utils.IngestionUtils.buildErrorRecord;
import static org.finos.legend.engine.persistence.components.util.ValidationCategory.NULL_VALUE;

public class AnsiSqlSink extends RelationalSink
{
    private static final RelationalSink INSTANCE;
    private static final Set<Capability> CAPABILITIES;
    protected static final Map<Class<?>, LogicalPlanVisitor<?>> LOGICAL_PLAN_VISITOR_BY_CLASS;

    private static final String FILE = "legend_persistence_file";
    private static final String ROW_NUMBER = "legend_persistence_row_number";

    static
    {
        Set<Capability> capabilities = new HashSet<>();
        capabilities.add(Capability.ALIAS_IN_HAVING);
        CAPABILITIES = Collections.unmodifiableSet(capabilities);

        Map<Class<?>, LogicalPlanVisitor<?>> logicalPlanVisitorByClass = new HashMap<>();
        logicalPlanVisitorByClass.put(Drop.class, new SQLDropVisitor());
        logicalPlanVisitorByClass.put(Truncate.class, new TruncateVisitor());
        logicalPlanVisitorByClass.put(Selection.class, new SelectionVisitor());
        logicalPlanVisitorByClass.put(Create.class, new SQLCreateVisitor());
        logicalPlanVisitorByClass.put(Insert.class, new InsertVisitor());
        logicalPlanVisitorByClass.put(Merge.class, new SQLMergeVisitor());
        logicalPlanVisitorByClass.put(Alter.class, new AlterVisitor());
        logicalPlanVisitorByClass.put(Delete.class, new DeleteVisitor());

        logicalPlanVisitorByClass.put(Field.class, new FieldVisitor());
        logicalPlanVisitorByClass.put(FieldValue.class, new FieldValueVisitor());
        logicalPlanVisitorByClass.put(OrderedField.class, new OrderedFieldVisitor());
        logicalPlanVisitorByClass.put(SchemaReference.class, new SchemaReferenceVisitor());
        logicalPlanVisitorByClass.put(SchemaDefinition.class, new SchemaDefinitionVisitor());
        logicalPlanVisitorByClass.put(DatasetReference.class, new DatasetReferenceVisitor());
        logicalPlanVisitorByClass.put(DatasetReferenceImpl.class, new DatasetReferenceVisitor());
        logicalPlanVisitorByClass.put(DatasetDefinition.class, new DatasetDefinitionVisitor());
        logicalPlanVisitorByClass.put(DerivedDataset.class, new DerivedDatasetVisitor());
        logicalPlanVisitorByClass.put(FilteredDataset.class, new FilteredDatasetVisitor());
        logicalPlanVisitorByClass.put(JsonExternalDatasetReference.class, new DatasetReferenceVisitor());
        logicalPlanVisitorByClass.put(DatasetAdditionalProperties.class, new DatasetAdditionalPropertiesVisitor());

        logicalPlanVisitorByClass.put(Not.class, new NotVisitor());
        logicalPlanVisitorByClass.put(And.class, new AndVisitor());
        logicalPlanVisitorByClass.put(Or.class, new OrVisitor());

        logicalPlanVisitorByClass.put(Exists.class, new ExistsConditionVisitor());
        logicalPlanVisitorByClass.put(In.class, new InVisitor());
        logicalPlanVisitorByClass.put(NotIn.class, new NotInVisitor());
        logicalPlanVisitorByClass.put(Equals.class, new EqualsVisitor());
        logicalPlanVisitorByClass.put(NotEquals.class, new NotEqualsVisitor());
        logicalPlanVisitorByClass.put(GreaterThanEqualTo.class, new GreaterThanEqualToVisitor());
        logicalPlanVisitorByClass.put(GreaterThan.class, new GreaterThanVisitor());
        logicalPlanVisitorByClass.put(LessThanEqualTo.class, new LessThanEqualToVisitor());
        logicalPlanVisitorByClass.put(LessThan.class, new LessThanVisitor());
        logicalPlanVisitorByClass.put(IsNull.class, new IsNullVisitor());
        logicalPlanVisitorByClass.put(Join.class, new JoinOperationVisitor());

        logicalPlanVisitorByClass.put(Update.class, new SQLUpdateVisitor());
        logicalPlanVisitorByClass.put(FunctionImpl.class, new FunctionVisitor());
        logicalPlanVisitorByClass.put(Udf.class, new UdfVisitor());
        logicalPlanVisitorByClass.put(HashFunction.class, new HashFunctionVisitor());
        logicalPlanVisitorByClass.put(WindowFunction.class, new WindowFunctionVisitor());
        logicalPlanVisitorByClass.put(ParseJsonFunction.class, new ParseJsonFunctionVisitor());
        logicalPlanVisitorByClass.put(NumericalValue.class, new NumericalValueVisitor());
        logicalPlanVisitorByClass.put(ObjectValue.class, new ObjectValueVisitor());
        logicalPlanVisitorByClass.put(Case.class, new CaseVisitor());

        logicalPlanVisitorByClass.put(All.class, new AllVisitor());
        logicalPlanVisitorByClass.put(Pair.class, new PairVisitor());
        logicalPlanVisitorByClass.put(Array.class, new ArrayVisitor());
        logicalPlanVisitorByClass.put(ApproxCountDistinct.class, new ApproxCountDistinctVisitor());
        logicalPlanVisitorByClass.put(TabularValues.class, new TabularValuesVisitor());
        logicalPlanVisitorByClass.put(StringValue.class, new StringValueVisitor());
        logicalPlanVisitorByClass.put(DatetimeValue.class, new DatetimeValueVisitor());
        logicalPlanVisitorByClass.put(BatchStartTimestamp.class, new BatchStartTimestampVisitor());
        logicalPlanVisitorByClass.put(BatchEndTimestamp.class, new BatchEndTimestampVisitor());
        logicalPlanVisitorByClass.put(AllQuantifier.class, new AllQuantifierVisitor());
        logicalPlanVisitorByClass.put(DistinctQuantifier.class, new DistinctQuantifierVisitor());
        logicalPlanVisitorByClass.put(IfExistsTableModifier.class, new TableModifierVisitor());
        logicalPlanVisitorByClass.put(IfNotExistsTableModifier.class, new TableModifierVisitor());
        logicalPlanVisitorByClass.put(CascadeTableConstraint.class, new TableConstraintVisitor());
        logicalPlanVisitorByClass.put(SelectValue.class, new SelectValueVisitor());
        logicalPlanVisitorByClass.put(SumBinaryValueOperator.class, new SumBinaryValueOperatorVisitor());
        logicalPlanVisitorByClass.put(DiffBinaryValueOperator.class, new DiffBinaryValueOperatorVisitor());
        logicalPlanVisitorByClass.put(ModuloBinaryValueOperator.class, new ModuloBinaryValueOperatorVisitor());
        logicalPlanVisitorByClass.put(Show.class, new ShowVisitor());
        logicalPlanVisitorByClass.put(BatchIdValue.class, new BatchIdValueVisitor());
        logicalPlanVisitorByClass.put(InfiniteBatchIdValue.class, new InfiniteBatchIdValueVisitor());
        logicalPlanVisitorByClass.put(BulkLoadBatchStatusValue.class, new BulkLoadBatchStatusValueVisitor());

        LOGICAL_PLAN_VISITOR_BY_CLASS = Collections.unmodifiableMap(logicalPlanVisitorByClass);

        INSTANCE = new AnsiSqlSink();
    }

    public static RelationalSink get()
    {
        return INSTANCE;
    }

    private AnsiSqlSink()
    {
        super(
            CAPABILITIES,
            Collections.emptyMap(),
            Collections.emptyMap(),
            SqlGenUtils.QUOTE_IDENTIFIER,
            LOGICAL_PLAN_VISITOR_BY_CLASS,
            (x, y, z) ->
            {
                throw new UnsupportedOperationException();
            },
            (x, y, z) ->
            {
                throw new UnsupportedOperationException();
            },
            (x, y, z) ->
            {
                throw new UnsupportedOperationException();
            });
    }

    protected AnsiSqlSink(
        Set<Capability> capabilities,
        Map<DataType, Set<DataType>> implicitDataTypeMapping,
        Map<DataType, Set<DataType>> nonBreakingDataTypeMapping,
        String quoteIdentifier,
        Map<Class<?>, LogicalPlanVisitor<?>> logicalPlanVisitorByClass,
        DatasetExists datasetExists,
        ValidateMainDatasetSchema validateMainDatasetSchema,
        ConstructDatasetFromDatabase constructDatasetFromDatabase)
    {
        super(
            capabilities,
            implicitDataTypeMapping,
            nonBreakingDataTypeMapping,
            quoteIdentifier,
            rightBiasedUnion(LOGICAL_PLAN_VISITOR_BY_CLASS, logicalPlanVisitorByClass),
            datasetExists,
            validateMainDatasetSchema,
            constructDatasetFromDatabase);
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
    public Executor<SqlGen, TabularData, SqlPlan> getRelationalExecutor(RelationalConnection connection)
    {
        throw new UnsupportedOperationException("No executor supported for AnsiSql Sink");
    }

    // utility methods

    private static Map<Class<?>, LogicalPlanVisitor<?>> rightBiasedUnion(Map<Class<?>, LogicalPlanVisitor<?>> map1, Map<Class<?>, LogicalPlanVisitor<?>> map2)
    {
        Map<Class<?>, LogicalPlanVisitor<?>> union = new HashMap<>();
        union.putAll(map1);
        union.putAll(map2);
        return union;
    }

    @Override
    public IngestorResult performBulkLoad(Datasets datasets, Executor<SqlGen, TabularData, SqlPlan> executor, SqlPlan ingestSqlPlan, Map<StatisticName, SqlPlan> statisticsSqlPlan, Map<String, PlaceholderValue> placeHolderKeyValues, Clock executionTimestampClock)
    {
        throw new UnsupportedOperationException("Bulk Load not supported!");
    }

    public List<DataError> performDryRun(Datasets datasets, Transformer<SqlGen, SqlPlan> transformer, Executor<SqlGen, TabularData, SqlPlan> executor, SqlPlan dryRunSqlPlan, Map<ValidationCategory, List<org.eclipse.collections.api.tuple.Pair<Set<FieldValue>, SqlPlan>>> dryRunValidationSqlPlan, int sampleRowCount, CaseConversion caseConversion)
    {
        throw new UnsupportedOperationException("DryRun not supported!");
    }

    protected Optional<String> getString(Map<String, Object> row, String key)
    {
        Object value = row.get(key);
        String strValue = value == null ? null : (String) value;
        return Optional.ofNullable(strValue);
    }

    protected Optional<Long> getLong(Map<String, Object> row, String key)
    {
        Object value = row.get(key);
        Long longValue = value == null ? null : (Long) value;
        return Optional.ofNullable(longValue);
    }

    protected Optional<Character> getChar(Map<String, Object> row, String key)
    {
        Object value = row.get(key);
        if (value instanceof Character)
        {
            Character charValue = value == null ? null : (Character) value;
            return Optional.ofNullable(charValue);
        }
        if (value instanceof String)
        {
            Optional<String> stringValue = getString(row, key);
            return stringValue.map(s -> s.charAt(0));
        }
        return Optional.empty();
    }

    protected int findNullValuesDataErrors(Executor<SqlGen, TabularData, SqlPlan> executor, List<org.eclipse.collections.api.tuple.Pair<Set<FieldValue>, SqlPlan>> queriesForNull, Map<ValidationCategory, Queue<DataError>> dataErrorsByCategory, List<String> allFields, CaseConversion caseConversion)
    {
        int errorsCount = 0;
        for (org.eclipse.collections.api.tuple.Pair<Set<FieldValue>, SqlPlan> pair : queriesForNull)
        {
            List<TabularData> results = executor.executePhysicalPlanAndGetResults(pair.getTwo());
            if (!results.isEmpty())
            {
                List<Map<String, Object>> resultSets = results.get(0).data();
                for (Map<String, Object> row : resultSets)
                {
                    for (String column : pair.getOne().stream().map(FieldValue::fieldName).collect(Collectors.toSet()))
                    {
                        if (row.get(column) == null)
                        {
                            DataError dataError = constructDataError(allFields, row, NULL_VALUE, column, caseConversion);
                            dataErrorsByCategory.get(NULL_VALUE).add(dataError);
                            errorsCount++;
                        }
                    }
                }
            }
        }
        return errorsCount;
    }

    protected DataError constructDataError(List<String> allColumns, Map<String, Object> row, ValidationCategory validationCategory, String validatedColumnName, CaseConversion caseConversion)
    {
        ErrorCategory errorCategory = getValidationFailedErrorCategory(validationCategory);
        String fileColumnName = ApiUtils.convertCase(caseConversion, FILE);
        String rowNumberColumnName = ApiUtils.convertCase(caseConversion, ROW_NUMBER);
        Map<String, Object> errorDetails = buildErrorDetails(getString(row, fileColumnName), Optional.of(validatedColumnName), getLong(row, rowNumberColumnName));

        return DataError.builder()
            .errorMessage(errorCategory.getDefaultErrorMessage())
            .errorCategory(errorCategory)
            .putAllErrorDetails(errorDetails)
            .errorRecord(buildErrorRecord(allColumns, row))
            .build();
    }

    protected Map<String, Object> buildErrorDetails(Optional<String> fileName, Optional<String> columnName, Optional<Long> recordNumber)
    {
        Map<String, Object> errorDetails = new HashMap<>();
        fileName.ifPresent(file -> errorDetails.put(DataError.FILE_NAME, file));
        columnName.ifPresent(col -> errorDetails.put(DataError.COLUMN_NAME, col));
        recordNumber.ifPresent(rowNum -> errorDetails.put(DataError.RECORD_NUMBER, rowNum));
        return errorDetails;
    }

    private ErrorCategory getValidationFailedErrorCategory(ValidationCategory validationCategory)
    {
        switch (validationCategory)
        {
            case NULL_VALUE:
                return ErrorCategory.CHECK_NULL_CONSTRAINT;
            case TYPE_CONVERSION:
                return ErrorCategory.TYPE_CONVERSION;
            default:
                throw new IllegalStateException("Unsupported validation category");
        }
    }

    public List<DataError> getDataErrorsWithFairDistributionAcrossCategories(int sampleRowCount, int dataErrorsTotalCount, Map<ValidationCategory, Queue<DataError>> dataErrorsByCategory)
    {
        if (dataErrorsTotalCount <= sampleRowCount)
        {
            return dataErrorsByCategory.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
        }

        List<DataError> fairlyDistributedDataErrors = new ArrayList<>();
        List<ValidationCategory> eligibleCategories = new ArrayList<>(Arrays.asList(ValidationCategory.values()));

        while (fairlyDistributedDataErrors.size() < sampleRowCount && !eligibleCategories.isEmpty())
        {
            for (ValidationCategory validationCategory : eligibleCategories)
            {
                if (!dataErrorsByCategory.get(validationCategory).isEmpty())
                {
                    if (fairlyDistributedDataErrors.size() < sampleRowCount)
                    {
                        fairlyDistributedDataErrors.add(dataErrorsByCategory.get(validationCategory).poll());
                    }
                }
                else
                {
                    eligibleCategories.remove(validationCategory);
                }
            }
        }

        return fairlyDistributedDataErrors;
    }
}
