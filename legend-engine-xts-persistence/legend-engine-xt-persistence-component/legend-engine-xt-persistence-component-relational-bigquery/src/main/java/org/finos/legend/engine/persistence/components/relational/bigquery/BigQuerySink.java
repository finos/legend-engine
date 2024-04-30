// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.persistence.components.relational.bigquery;

import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.common.StatisticName;
import org.finos.legend.engine.persistence.components.executor.Executor;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.ClusterKey;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.ExternalDataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.PartitionKey;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.StagedFilesDataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.StagedFilesDatasetReference;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.StagedFilesSelection;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Alter;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Copy;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Create;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Delete;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Drop;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Truncate;
import org.finos.legend.engine.persistence.components.logicalplan.values.BatchEndTimestamp;
import org.finos.legend.engine.persistence.components.logicalplan.values.BatchStartTimestamp;
import org.finos.legend.engine.persistence.components.logicalplan.values.DatetimeValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.DigestUdf;
import org.finos.legend.engine.persistence.components.logicalplan.values.StagedFilesFieldValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.ToArrayFunction;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.ansi.AnsiSqlSink;
import org.finos.legend.engine.persistence.components.relational.api.IngestStatus;
import org.finos.legend.engine.persistence.components.relational.api.IngestorResult;
import org.finos.legend.engine.persistence.components.relational.api.RelationalConnection;
import org.finos.legend.engine.persistence.components.relational.bigquery.executor.BigQueryConnection;
import org.finos.legend.engine.persistence.components.relational.bigquery.executor.BigQueryExecutor;
import org.finos.legend.engine.persistence.components.relational.bigquery.executor.BigQueryHelper;
import org.finos.legend.engine.persistence.components.relational.bigquery.sql.BigQueryDataTypeMapping;
import org.finos.legend.engine.persistence.components.relational.bigquery.sql.BigQueryDataTypeToLogicalDataTypeMapping;
import org.finos.legend.engine.persistence.components.relational.bigquery.sql.visitor.AlterVisitor;
import org.finos.legend.engine.persistence.components.relational.bigquery.sql.visitor.BatchEndTimestampVisitor;
import org.finos.legend.engine.persistence.components.relational.bigquery.sql.visitor.BatchStartTimestampVisitor;
import org.finos.legend.engine.persistence.components.relational.bigquery.sql.visitor.ClusterKeyVisitor;
import org.finos.legend.engine.persistence.components.relational.bigquery.sql.visitor.CopyVisitor;
import org.finos.legend.engine.persistence.components.relational.bigquery.sql.visitor.DatetimeValueVisitor;
import org.finos.legend.engine.persistence.components.relational.bigquery.sql.visitor.DeleteVisitor;
import org.finos.legend.engine.persistence.components.relational.bigquery.sql.visitor.DigestUdfVisitor;
import org.finos.legend.engine.persistence.components.relational.bigquery.sql.visitor.ExternalDatasetVisitor;
import org.finos.legend.engine.persistence.components.relational.bigquery.sql.visitor.FieldVisitor;
import org.finos.legend.engine.persistence.components.relational.bigquery.sql.visitor.PartitionKeyVisitor;
import org.finos.legend.engine.persistence.components.relational.bigquery.sql.visitor.SQLCreateVisitor;
import org.finos.legend.engine.persistence.components.relational.bigquery.sql.visitor.SQLDropVisitor;
import org.finos.legend.engine.persistence.components.relational.bigquery.sql.visitor.SchemaDefinitionVisitor;
import org.finos.legend.engine.persistence.components.relational.bigquery.sql.visitor.StagedFilesDatasetReferenceVisitor;
import org.finos.legend.engine.persistence.components.relational.bigquery.sql.visitor.StagedFilesDatasetVisitor;
import org.finos.legend.engine.persistence.components.relational.bigquery.sql.visitor.StagedFilesFieldValueVisitor;
import org.finos.legend.engine.persistence.components.relational.bigquery.sql.visitor.StagedFilesSelectionVisitor;
import org.finos.legend.engine.persistence.components.relational.bigquery.sql.visitor.ToArrayFunctionVisitor;
import org.finos.legend.engine.persistence.components.relational.bigquery.sql.visitor.TruncateVisitor;
import org.finos.legend.engine.persistence.components.relational.sql.TabularData;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlGen;
import org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.util.Capability;
import org.finos.legend.engine.persistence.components.util.PlaceholderValue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.finos.legend.engine.persistence.components.relational.api.RelationalIngestorAbstract.BATCH_ID_PATTERN;
import static org.finos.legend.engine.persistence.components.relational.api.RelationalIngestorAbstract.BATCH_START_TS_PATTERN;

public class BigQuerySink extends AnsiSqlSink
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
        CAPABILITIES = Collections.unmodifiableSet(capabilities);

        Map<Class<?>, LogicalPlanVisitor<?>> logicalPlanVisitorByClass = new HashMap<>();
        logicalPlanVisitorByClass.put(SchemaDefinition.class, new SchemaDefinitionVisitor());
        logicalPlanVisitorByClass.put(Create.class, new SQLCreateVisitor());
        logicalPlanVisitorByClass.put(Drop.class, new SQLDropVisitor());
        logicalPlanVisitorByClass.put(ClusterKey.class, new ClusterKeyVisitor());
        logicalPlanVisitorByClass.put(PartitionKey.class, new PartitionKeyVisitor());
        logicalPlanVisitorByClass.put(Alter.class, new AlterVisitor());
        logicalPlanVisitorByClass.put(Delete.class, new DeleteVisitor());
        logicalPlanVisitorByClass.put(Field.class, new FieldVisitor());
        logicalPlanVisitorByClass.put(Truncate.class, new TruncateVisitor());
        logicalPlanVisitorByClass.put(DatetimeValue.class, new DatetimeValueVisitor());
        logicalPlanVisitorByClass.put(BatchEndTimestamp.class, new BatchEndTimestampVisitor());
        logicalPlanVisitorByClass.put(BatchStartTimestamp.class, new BatchStartTimestampVisitor());
        logicalPlanVisitorByClass.put(Copy.class, new CopyVisitor());
        logicalPlanVisitorByClass.put(ExternalDataset.class, new ExternalDatasetVisitor());
        logicalPlanVisitorByClass.put(StagedFilesFieldValue.class, new StagedFilesFieldValueVisitor());
        logicalPlanVisitorByClass.put(StagedFilesDataset.class, new StagedFilesDatasetVisitor());
        logicalPlanVisitorByClass.put(StagedFilesSelection.class, new StagedFilesSelectionVisitor());
        logicalPlanVisitorByClass.put(StagedFilesDatasetReference.class, new StagedFilesDatasetReferenceVisitor());
        logicalPlanVisitorByClass.put(DigestUdf.class, new DigestUdfVisitor());
        logicalPlanVisitorByClass.put(ToArrayFunction.class, new ToArrayFunctionVisitor());
        LOGICAL_PLAN_VISITOR_BY_CLASS = Collections.unmodifiableMap(logicalPlanVisitorByClass);

        Map<DataType, Set<DataType>> implicitDataTypeMapping = new HashMap<>();
        implicitDataTypeMapping.put(DataType.INTEGER, getUnmodifiableDataTypesSet(DataType.INT, DataType.BIGINT, DataType.TINYINT, DataType.SMALLINT));
        implicitDataTypeMapping.put(DataType.NUMERIC, getUnmodifiableDataTypesSet(DataType.NUMERIC, DataType.DECIMAL, DataType.INT, DataType.INTEGER, DataType.BIGINT, DataType.TINYINT, DataType.SMALLINT));
        implicitDataTypeMapping.put(DataType.FLOAT, getUnmodifiableDataTypesSet(DataType.REAL, DataType.DOUBLE, DataType.INT, DataType.INTEGER, DataType.BIGINT, DataType.TINYINT, DataType.SMALLINT, DataType.NUMERIC, DataType.DECIMAL));
        implicitDataTypeMapping.put(DataType.STRING, getUnmodifiableDataTypesSet(DataType.STRING, DataType.CHAR, DataType.VARCHAR, DataType.LONGTEXT, DataType.TEXT));
        implicitDataTypeMapping.put(DataType.DATETIME, Collections.singleton(DataType.DATE));
        IMPLICIT_DATA_TYPE_MAPPING = Collections.unmodifiableMap(implicitDataTypeMapping);

        Map<DataType, Set<DataType>> explicitDataTypeMapping = new HashMap<>();
        explicitDataTypeMapping.put(DataType.INT, getUnmodifiableDataTypesSet(DataType.NUMERIC, DataType.DECIMAL, DataType.REAL, DataType.FLOAT, DataType.DOUBLE));
        explicitDataTypeMapping.put(DataType.INTEGER, getUnmodifiableDataTypesSet(DataType.NUMERIC, DataType.DECIMAL, DataType.REAL, DataType.FLOAT, DataType.DOUBLE));
        explicitDataTypeMapping.put(DataType.BIGINT, getUnmodifiableDataTypesSet(DataType.NUMERIC, DataType.DECIMAL, DataType.REAL, DataType.FLOAT, DataType.DOUBLE));
        explicitDataTypeMapping.put(DataType.TINYINT, getUnmodifiableDataTypesSet(DataType.NUMERIC, DataType.DECIMAL, DataType.REAL, DataType.FLOAT, DataType.DOUBLE));
        explicitDataTypeMapping.put(DataType.SMALLINT, getUnmodifiableDataTypesSet(DataType.NUMERIC, DataType.DECIMAL, DataType.REAL, DataType.FLOAT, DataType.DOUBLE));
        explicitDataTypeMapping.put(DataType.NUMERIC, getUnmodifiableDataTypesSet(DataType.REAL, DataType.FLOAT, DataType.DOUBLE));
        explicitDataTypeMapping.put(DataType.DECIMAL, getUnmodifiableDataTypesSet(DataType.REAL, DataType.FLOAT, DataType.DOUBLE));
        EXPLICIT_DATA_TYPE_MAPPING = Collections.unmodifiableMap(explicitDataTypeMapping);
        INSTANCE = new BigQuerySink();
    }

    private static Set getUnmodifiableDataTypesSet(DataType... dataTypes)
    {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(dataTypes)));
    }

    public static RelationalSink get()
    {
        return INSTANCE;
    }

    private BigQuerySink()
    {
        super(
                CAPABILITIES,
                IMPLICIT_DATA_TYPE_MAPPING,
                EXPLICIT_DATA_TYPE_MAPPING,
                SqlGenUtils.BACK_QUOTE_IDENTIFIER,
                LOGICAL_PLAN_VISITOR_BY_CLASS,
                (executor, sink, dataset) -> sink.doesTableExist(dataset),
                (executor, sink, dataset) -> sink.validateDatasetSchema(dataset, new BigQueryDataTypeMapping()),
                (executor, sink, dataset) -> sink.constructDatasetFromDatabase(dataset, new BigQueryDataTypeToLogicalDataTypeMapping(), false));
    }

    @Override
    public Executor<SqlGen, TabularData, SqlPlan> getRelationalExecutor(RelationalConnection relationalConnection)
    {
        if (relationalConnection instanceof BigQueryConnection)
        {
            BigQueryConnection bigQueryConnection = (BigQueryConnection) relationalConnection;
            return new BigQueryExecutor(this, BigQueryHelper.of(bigQueryConnection.bigQuery()));
        }
        else
        {
            throw new UnsupportedOperationException("Only BigQueryConnection is supported for BigQuery Sink");
        }
    }

    //evolve to = field to replace main column (datatype)
    //evolve from = reference field to compare sizing/nullability requirements
    @Override
    public Field evolveFieldLength(Field evolveFrom, Field evolveTo)
    {
        Optional<Integer> oldScale = evolveFrom.type().scale();
        Optional<Integer> newScale = evolveTo.type().scale();
        Optional<Integer> scale = getMaximumValue(oldScale, newScale, true);

        Optional<Integer> oldLength = evolveFrom.type().length();
        Optional<Integer> newLength = evolveTo.type().length();
        Optional<Integer> length;

        if (scale.isPresent())
        {
            //if scale is present, by design, precision is also present
            Optional<Integer> oldIntegralLength = oldScale.map(scaleValue -> Optional.of(oldLength.get() - scaleValue)).orElse(oldLength);
            Optional<Integer> newIntegralLength = newScale.map(scaleValue -> Optional.of(newLength.get() - scaleValue)).orElse(newLength);
            Optional<Integer> integralLength = getMaximumValue(oldIntegralLength, newIntegralLength, false);
            length = integralLength.map(integralLengthValue -> integralLengthValue + scale.get());
        }
        else
        {
            length = getMaximumValue(oldLength, newLength, false);
        }

        return this.createNewField(evolveTo, evolveFrom, length, scale);
    }

    private static Optional<Integer> getMaximumValue(Optional<Integer> oldValue, Optional<Integer> newValue, boolean classifyEmptyValueAsZero)
    {
        Optional<Integer> value = newValue;
        if (oldValue.isPresent() && newValue.isPresent())
        {
            if (newValue.get() <= oldValue.get())
            {
                value = oldValue;
            }
        }
        else if (!classifyEmptyValueAsZero && !oldValue.isPresent() || classifyEmptyValueAsZero && oldValue.isPresent())
        {
            value = oldValue;
        }
        return value;
    }

    @Override
    public Field createNewField(Field evolveTo, Field evolveFrom, Optional<Integer> length, Optional<Integer> scale)
    {
        FieldType modifiedFieldType = length.isPresent() ? FieldType.of(evolveTo.type().dataType(), length, scale) : FieldType.of(evolveTo.type().dataType(), Optional.empty(), Optional.empty());
        boolean nullability = evolveTo.nullable() || evolveFrom.nullable();

        return Field.builder().name(evolveTo.name()).primaryKey(evolveTo.primaryKey())
                .fieldAlias(evolveTo.fieldAlias()).nullable(nullability)
                .identity(evolveTo.identity()).unique(evolveTo.unique())
                .defaultValue(evolveTo.defaultValue()).type(modifiedFieldType).build();
    }

    @Override
    public IngestorResult performBulkLoad(Datasets datasets, Executor<SqlGen, TabularData, SqlPlan> executor, SqlPlan ingestSqlPlan, Map<StatisticName, SqlPlan> statisticsSqlPlan, Map<String, PlaceholderValue> placeHolderKeyValues)
    {
        BigQueryExecutor bigQueryExecutor = (BigQueryExecutor) executor;
        Map<StatisticName, Object> stats = bigQueryExecutor.executeLoadPhysicalPlanAndGetStats(ingestSqlPlan, placeHolderKeyValues);

        IngestorResult.Builder resultBuilder = IngestorResult.builder()
            .updatedDatasets(datasets)
            .putAllStatisticByName(stats)
            .ingestionTimestampUTC(placeHolderKeyValues.get(BATCH_START_TS_PATTERN).value())
            .batchId(Optional.ofNullable(placeHolderKeyValues.containsKey(BATCH_ID_PATTERN) ? Integer.valueOf(placeHolderKeyValues.get(BATCH_ID_PATTERN).value()) : null));
        IngestorResult result;

        if ((long) stats.get(StatisticName.ROWS_WITH_ERRORS) == 0)
        {
            result = resultBuilder
                .status(IngestStatus.SUCCEEDED)
                .build();
        }
        else
        {
            result = resultBuilder
                .status(IngestStatus.FAILED)
                .build();
        }
        return result;
    }
}
