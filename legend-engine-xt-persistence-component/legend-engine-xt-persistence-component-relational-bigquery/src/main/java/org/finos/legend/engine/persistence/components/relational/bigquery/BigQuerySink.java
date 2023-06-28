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

import org.finos.legend.engine.persistence.components.executor.Executor;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.ClusterKey;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.PartitionKey;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Alter;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Create;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Delete;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Truncate;
import org.finos.legend.engine.persistence.components.logicalplan.values.BatchEndTimestamp;
import org.finos.legend.engine.persistence.components.logicalplan.values.BatchStartTimestamp;
import org.finos.legend.engine.persistence.components.optimizer.Optimizer;
import org.finos.legend.engine.persistence.components.relational.CaseConversion;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.ansi.AnsiSqlSink;
import org.finos.legend.engine.persistence.components.relational.api.RelationalConnection;
import org.finos.legend.engine.persistence.components.relational.bigquery.executor.BigQueryConnection;
import org.finos.legend.engine.persistence.components.relational.bigquery.executor.BigQueryExecutor;
import org.finos.legend.engine.persistence.components.relational.bigquery.executor.BigQueryHelper;
import org.finos.legend.engine.persistence.components.relational.bigquery.optmizer.LowerCaseOptimizer;
import org.finos.legend.engine.persistence.components.relational.bigquery.optmizer.UpperCaseOptimizer;
import org.finos.legend.engine.persistence.components.relational.bigquery.sql.BigQueryDataTypeMapping;
import org.finos.legend.engine.persistence.components.relational.bigquery.sql.BigQueryDataTypeToLogicalDataTypeMapping;
import org.finos.legend.engine.persistence.components.relational.bigquery.sql.visitor.AlterVisitor;
import org.finos.legend.engine.persistence.components.relational.bigquery.sql.visitor.BatchEndTimestampVisitor;
import org.finos.legend.engine.persistence.components.relational.bigquery.sql.visitor.BatchStartTimestampVisitor;
import org.finos.legend.engine.persistence.components.relational.bigquery.sql.visitor.ClusterKeyVisitor;
import org.finos.legend.engine.persistence.components.relational.bigquery.sql.visitor.DeleteVisitor;
import org.finos.legend.engine.persistence.components.relational.bigquery.sql.visitor.FieldVisitor;
import org.finos.legend.engine.persistence.components.relational.bigquery.sql.visitor.PartitionKeyVisitor;
import org.finos.legend.engine.persistence.components.relational.bigquery.sql.visitor.SQLCreateVisitor;
import org.finos.legend.engine.persistence.components.relational.bigquery.sql.visitor.SchemaDefinitionVisitor;
import org.finos.legend.engine.persistence.components.relational.bigquery.sql.visitor.TruncateVisitor;
import org.finos.legend.engine.persistence.components.relational.sql.TabularData;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlGen;
import org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.util.Capability;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
        logicalPlanVisitorByClass.put(ClusterKey.class, new ClusterKeyVisitor());
        logicalPlanVisitorByClass.put(PartitionKey.class, new PartitionKeyVisitor());
        logicalPlanVisitorByClass.put(Alter.class, new AlterVisitor());
        logicalPlanVisitorByClass.put(Field.class, new FieldVisitor());
        logicalPlanVisitorByClass.put(Delete.class, new DeleteVisitor());
        logicalPlanVisitorByClass.put(Truncate.class, new TruncateVisitor());
        logicalPlanVisitorByClass.put(BatchEndTimestamp.class, new BatchEndTimestampVisitor());
        logicalPlanVisitorByClass.put(BatchStartTimestamp.class, new BatchStartTimestampVisitor());
        LOGICAL_PLAN_VISITOR_BY_CLASS = Collections.unmodifiableMap(logicalPlanVisitorByClass);

        // TODO: To review the capabilities for Schema Evolution
        Map<DataType, Set<DataType>> implicitDataTypeMapping = new HashMap<>();
        implicitDataTypeMapping.put(DataType.INT, new HashSet<>(Arrays.asList(DataType.INTEGER, DataType.BIGINT, DataType.TINYINT, DataType.SMALLINT, DataType.INT64)));
        implicitDataTypeMapping.put(DataType.INTEGER, new HashSet<>(Arrays.asList(DataType.INT, DataType.BIGINT, DataType.TINYINT, DataType.SMALLINT, DataType.INT64)));
        implicitDataTypeMapping.put(DataType.BIGINT, new HashSet<>(Arrays.asList(DataType.INT, DataType.INTEGER, DataType.TINYINT, DataType.SMALLINT, DataType.INT64)));
        implicitDataTypeMapping.put(DataType.TINYINT, new HashSet<>(Arrays.asList(DataType.INT, DataType.INTEGER, DataType.BIGINT, DataType.SMALLINT, DataType.INT64)));
        implicitDataTypeMapping.put(DataType.SMALLINT, new HashSet<>(Arrays.asList(DataType.INT, DataType.INTEGER, DataType.BIGINT, DataType.TINYINT, DataType.INT64)));
        implicitDataTypeMapping.put(DataType.INT64, new HashSet<>(Arrays.asList(DataType.INT, DataType.INTEGER, DataType.BIGINT, DataType.TINYINT, DataType.SMALLINT)));
        implicitDataTypeMapping.put(DataType.NUMBER, new HashSet<>(Arrays.asList(DataType.NUMBER, DataType.NUMERIC, DataType.DECIMAL, DataType.INT, DataType.INTEGER, DataType.BIGINT, DataType.TINYINT, DataType.SMALLINT, DataType.INT64)));
        implicitDataTypeMapping.put(DataType.NUMERIC, new HashSet<>(Arrays.asList(DataType.NUMERIC, DataType.NUMBER, DataType.DECIMAL, DataType.INT, DataType.INTEGER, DataType.BIGINT, DataType.TINYINT, DataType.SMALLINT, DataType.INT64)));
        implicitDataTypeMapping.put(DataType.DECIMAL, new HashSet<>(Arrays.asList(DataType.DECIMAL, DataType.NUMBER, DataType.NUMERIC, DataType.INT, DataType.INTEGER, DataType.BIGINT, DataType.TINYINT, DataType.SMALLINT, DataType.INT64)));
        implicitDataTypeMapping.put(DataType.REAL, new HashSet<>(Arrays.asList(DataType.FLOAT, DataType.DOUBLE, DataType.FLOAT64, DataType.INT, DataType.INTEGER, DataType.BIGINT, DataType.TINYINT, DataType.SMALLINT, DataType.INT64, DataType.NUMBER, DataType.NUMERIC, DataType.DECIMAL)));
        implicitDataTypeMapping.put(DataType.FLOAT, new HashSet<>(Arrays.asList(DataType.REAL, DataType.DOUBLE, DataType.FLOAT64, DataType.INT, DataType.INTEGER, DataType.BIGINT, DataType.TINYINT, DataType.SMALLINT, DataType.INT64, DataType.NUMBER, DataType.NUMERIC, DataType.DECIMAL)));
        implicitDataTypeMapping.put(DataType.DOUBLE, new HashSet<>(Arrays.asList(DataType.REAL, DataType.FLOAT, DataType.FLOAT64, DataType.INT, DataType.INTEGER, DataType.BIGINT, DataType.TINYINT, DataType.SMALLINT, DataType.INT64, DataType.NUMBER, DataType.NUMERIC, DataType.DECIMAL)));
        implicitDataTypeMapping.put(DataType.FLOAT64, new HashSet<>(Arrays.asList(DataType.REAL, DataType.FLOAT, DataType.DOUBLE, DataType.INT, DataType.INTEGER, DataType.BIGINT, DataType.TINYINT, DataType.SMALLINT, DataType.INT64, DataType.NUMBER, DataType.NUMERIC, DataType.DECIMAL)));
        implicitDataTypeMapping.put(DataType.CHAR, new HashSet<>(Arrays.asList(DataType.CHAR, DataType.CHARACTER, DataType.VARCHAR, DataType.LONGNVARCHAR, DataType.LONGTEXT, DataType.TEXT, DataType.STRING)));
        implicitDataTypeMapping.put(DataType.CHARACTER, new HashSet<>(Arrays.asList(DataType.CHARACTER, DataType.CHAR, DataType.VARCHAR, DataType.LONGNVARCHAR, DataType.LONGTEXT, DataType.TEXT, DataType.STRING)));
        implicitDataTypeMapping.put(DataType.VARCHAR, new HashSet<>(Arrays.asList(DataType.VARCHAR, DataType.CHAR, DataType.CHARACTER, DataType.LONGNVARCHAR, DataType.LONGTEXT, DataType.TEXT, DataType.STRING)));
        implicitDataTypeMapping.put(DataType.LONGNVARCHAR, new HashSet<>(Arrays.asList(DataType.LONGNVARCHAR, DataType.CHAR, DataType.CHARACTER, DataType.VARCHAR, DataType.LONGTEXT, DataType.TEXT, DataType.STRING)));
        implicitDataTypeMapping.put(DataType.LONGTEXT, new HashSet<>(Arrays.asList(DataType.LONGTEXT, DataType.CHAR, DataType.CHARACTER, DataType.VARCHAR, DataType.LONGNVARCHAR, DataType.TEXT, DataType.STRING)));
        implicitDataTypeMapping.put(DataType.TEXT, new HashSet<>(Arrays.asList(DataType.TEXT, DataType.CHAR, DataType.CHARACTER, DataType.VARCHAR, DataType.LONGNVARCHAR, DataType.LONGTEXT, DataType.STRING)));
        implicitDataTypeMapping.put(DataType.STRING, new HashSet<>(Arrays.asList(DataType.STRING, DataType.CHAR, DataType.CHARACTER, DataType.VARCHAR, DataType.LONGNVARCHAR, DataType.LONGTEXT, DataType.TEXT)));
        implicitDataTypeMapping.put(DataType.DATETIME, Collections.singleton(DataType.DATE));
        implicitDataTypeMapping.put(DataType.BOOL, Collections.singleton(DataType.BOOLEAN));
        IMPLICIT_DATA_TYPE_MAPPING = Collections.unmodifiableMap(implicitDataTypeMapping);

        Map<DataType, Set<DataType>> explicitDataTypeMapping = new HashMap<>();
        explicitDataTypeMapping.put(DataType.INT, new HashSet<>(Arrays.asList(DataType.NUMBER, DataType.NUMERIC, DataType.DECIMAL, DataType.REAL, DataType.FLOAT, DataType.DOUBLE, DataType.FLOAT64)));
        explicitDataTypeMapping.put(DataType.INTEGER, new HashSet<>(Arrays.asList(DataType.NUMBER, DataType.NUMERIC, DataType.DECIMAL, DataType.REAL, DataType.FLOAT, DataType.DOUBLE, DataType.FLOAT64)));
        explicitDataTypeMapping.put(DataType.BIGINT, new HashSet<>(Arrays.asList(DataType.NUMBER, DataType.NUMERIC, DataType.DECIMAL, DataType.REAL, DataType.FLOAT, DataType.DOUBLE, DataType.FLOAT64)));
        explicitDataTypeMapping.put(DataType.TINYINT, new HashSet<>(Arrays.asList(DataType.NUMBER, DataType.NUMERIC, DataType.DECIMAL, DataType.REAL, DataType.FLOAT, DataType.DOUBLE, DataType.FLOAT64)));
        explicitDataTypeMapping.put(DataType.SMALLINT, new HashSet<>(Arrays.asList(DataType.NUMBER, DataType.NUMERIC, DataType.DECIMAL, DataType.REAL, DataType.FLOAT, DataType.DOUBLE, DataType.FLOAT64)));
        explicitDataTypeMapping.put(DataType.INT64, new HashSet<>(Arrays.asList(DataType.NUMBER, DataType.NUMERIC, DataType.DECIMAL, DataType.REAL, DataType.FLOAT, DataType.DOUBLE, DataType.FLOAT64)));
        explicitDataTypeMapping.put(DataType.NUMBER, new HashSet<>(Arrays.asList(DataType.REAL, DataType.FLOAT, DataType.DOUBLE, DataType.FLOAT64)));
        explicitDataTypeMapping.put(DataType.NUMERIC, new HashSet<>(Arrays.asList(DataType.REAL, DataType.FLOAT, DataType.DOUBLE, DataType.FLOAT64)));
        explicitDataTypeMapping.put(DataType.DECIMAL, new HashSet<>(Arrays.asList(DataType.REAL, DataType.FLOAT, DataType.DOUBLE, DataType.FLOAT64)));
        EXPLICIT_DATA_TYPE_MAPPING = Collections.unmodifiableMap(explicitDataTypeMapping);
        INSTANCE = new BigQuerySink();
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
                (executor, sink, tableName, schemaName, databaseName) -> sink.constructDatasetFromDatabase(tableName, schemaName, databaseName, new BigQueryDataTypeToLogicalDataTypeMapping()));
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

    @Override
    public Field evolveFieldLength(Field oldField, Field newField)
    {
        Optional<Integer> oldScale = oldField.type().scale();
        Optional<Integer> newScale = newField.type().scale();
        Optional<Integer> scale = getMaximumValue(oldScale, newScale, true);

        Optional<Integer> oldLength = oldField.type().length();
        Optional<Integer> newLength = newField.type().length();
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

        return this.createNewField(newField, oldField, length, scale);
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
    public Field createNewField(Field newField, Field oldField, Optional<Integer> length, Optional<Integer> scale)
    {
        FieldType modifiedFieldType = length.isPresent() ? FieldType.of(newField.type().dataType(), length, scale) : FieldType.of(newField.type().dataType(), Optional.empty(), Optional.empty());
        boolean nullability = newField.nullable() || oldField.nullable();

        //todo : how to handle default value, identity, uniqueness ?
        return Field.builder().name(newField.name()).primaryKey(newField.primaryKey())
                .fieldAlias(newField.fieldAlias()).nullable(nullability)
                .identity(newField.identity()).unique(newField.unique())
                .defaultValue(newField.defaultValue()).type(modifiedFieldType).build();
    }
}
