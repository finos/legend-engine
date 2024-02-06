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

package org.finos.legend.engine.persistence.components.relational;

import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.common.StatisticName;
import org.finos.legend.engine.persistence.components.executor.Executor;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanNode;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.optimizer.Optimizer;
import org.finos.legend.engine.persistence.components.relational.api.IngestorResult;
import org.finos.legend.engine.persistence.components.relational.api.RelationalConnection;
import org.finos.legend.engine.persistence.components.executor.RelationalExecutionHelper;
import org.finos.legend.engine.persistence.components.relational.sql.TabularData;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlGen;
import org.finos.legend.engine.persistence.components.sink.Sink;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.util.Capability;
import org.finos.legend.engine.persistence.components.util.PlaceholderValue;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public abstract class RelationalSink implements Sink
{
    private final Set<Capability> capabilities;
    private final Map<DataType, Set<DataType>> implicitDataTypeMapping;
    private final Map<DataType, Set<DataType>> explicitDataTypeMapping;
    private final String quoteIdentifier;
    private final Map<Class<?>, LogicalPlanVisitor<?>> logicalPlanVisitorByClass;

    private final DatasetExists datasetExists;
    private final ValidateMainDatasetSchema validateMainDatasetSchema;
    private final ConstructDatasetFromDatabase constructDatasetFromDatabase;

    protected RelationalSink(Set<Capability> capabilities,
                             Map<DataType, Set<DataType>> implicitDataTypeMapping,
                             Map<DataType, Set<DataType>> explicitDataTypeMapping,
                             String quoteIdentifier,
                             Map<Class<?>, LogicalPlanVisitor<?>> logicalPlanVisitorByClass,
                             DatasetExists datasetExists,
                             ValidateMainDatasetSchema validateMainDatasetSchema,
                             ConstructDatasetFromDatabase constructDatasetFromDatabase)
    {
        this.capabilities = capabilities;
        this.implicitDataTypeMapping = implicitDataTypeMapping;
        this.explicitDataTypeMapping = explicitDataTypeMapping;
        this.quoteIdentifier = quoteIdentifier;
        this.logicalPlanVisitorByClass = logicalPlanVisitorByClass;
        this.datasetExists = datasetExists;
        this.validateMainDatasetSchema = validateMainDatasetSchema;
        this.constructDatasetFromDatabase = constructDatasetFromDatabase;
    }

    @Override
    public Set<Capability> capabilities()
    {
        return capabilities;
    }

    @Override
    public boolean supportsImplicitMapping(DataType source, DataType target)
    {
        return implicitDataTypeMapping.getOrDefault(source, Collections.emptySet()).contains(target);
    }

    @Override
    public boolean supportsExplicitMapping(DataType source, DataType target)
    {
        return explicitDataTypeMapping.getOrDefault(source, Collections.emptySet()).contains(target);
    }

    @Override
    public String quoteIdentifier()
    {
        return quoteIdentifier;
    }

    @Override
    public <L extends LogicalPlanNode> LogicalPlanVisitor<L> visitorForClass(Class<?> clazz)
    {
        final LogicalPlanVisitor<?> visitor = logicalPlanVisitorByClass.get(clazz);
        if (visitor == null)
        {
            throw new IllegalArgumentException("Unable to find logical plan visitor for class: " + clazz.toString());
        }
        return (LogicalPlanVisitor<L>) visitor;
    }

    public DatasetExists datasetExistsFn()
    {
        return datasetExists;
    }

    public ValidateMainDatasetSchema validateMainDatasetSchemaFn()
    {
        return validateMainDatasetSchema;
    }

    public ConstructDatasetFromDatabase constructDatasetFromDatabaseFn()
    {
        return constructDatasetFromDatabase;
    }

    public abstract Optional<Optimizer> optimizerForCaseConversion(CaseConversion caseConversion);

    public abstract Executor<SqlGen, TabularData, SqlPlan> getRelationalExecutor(RelationalConnection connection);

    //evolve to = field to replace main column (datatype)
    //evolve from = reference field to compare sizing/nullability requirements
    @Override
    public Field evolveFieldLength(Field evolveFrom, Field evolveTo)
    {
        Optional<Integer> length = evolveTo.type().length();
        Optional<Integer> scale = evolveTo.type().scale();

        //If the oldField and newField have a length associated, pick the greater length
        if (evolveFrom.type().length().isPresent() && evolveTo.type().length().isPresent())
        {
            length = evolveTo.type().length().get() >= evolveFrom.type().length().get()
                    ? evolveTo.type().length()
                    : evolveFrom.type().length();
        }
        //Allow length evolution from unspecified length only when data types are same. This is to avoid evolution like SMALLINT(6) -> INT(6) or INT -> DOUBLE(6) and allow for DATETIME -> DATETIME(6)
        else if (evolveFrom.type().dataType().equals(evolveTo.type().dataType())
                && evolveFrom.type().length().isPresent() && !evolveTo.type().length().isPresent())
        {
            length = evolveFrom.type().length();
        }

        //If the oldField and newField have a scale associated, pick the greater scale
        if (evolveFrom.type().scale().isPresent() && evolveTo.type().scale().isPresent())
        {
            scale = evolveTo.type().scale().get() >= evolveFrom.type().scale().get()
                    ? evolveTo.type().scale()
                    : evolveFrom.type().scale();
        }
        //Allow scale evolution from unspecified scale only when data types are same. This is to avoid evolution like SMALLINT(6) -> INT(6) or INT -> DOUBLE(6) and allow for DATETIME -> DATETIME(6)
        else if (evolveFrom.type().dataType().equals(evolveTo.type().dataType())
                && evolveFrom.type().scale().isPresent() && !evolveTo.type().scale().isPresent())
        {
            scale = evolveFrom.type().scale();
        }
        return createNewField(evolveTo, evolveFrom, length, scale);
    }


    @Override
    public Field createNewField(Field evolveTo, Field evolveFrom, Optional<Integer> length, Optional<Integer> scale)
    {
        FieldType modifiedFieldType = FieldType.of(evolveTo.type().dataType(), length, scale);
        boolean nullability = evolveTo.nullable() || evolveFrom.nullable();

        //todo : how to handle default value, identity, uniqueness ?
        return Field.builder().name(evolveTo.name()).primaryKey(evolveTo.primaryKey())
                .fieldAlias(evolveTo.fieldAlias()).nullable(nullability)
                .identity(evolveTo.identity()).unique(evolveTo.unique())
                .defaultValue(evolveTo.defaultValue()).type(modifiedFieldType).build();
    }

    public interface DatasetExists
    {
        boolean apply(Executor<SqlGen, TabularData, SqlPlan> executor, RelationalExecutionHelper sink, Dataset dataset);
    }

    public interface ValidateMainDatasetSchema
    {
        void execute(Executor<SqlGen, TabularData, SqlPlan> executor, RelationalExecutionHelper sink, Dataset dataset);
    }

    public interface ConstructDatasetFromDatabase
    {
        Dataset execute(Executor<SqlGen, TabularData, SqlPlan> executor, RelationalExecutionHelper sink, Dataset dataset);
    }

    public abstract IngestorResult performBulkLoad(Datasets datasets, Executor<SqlGen, TabularData, SqlPlan> executor, SqlPlan ingestSqlPlan, Map<StatisticName, SqlPlan> statisticsSqlPlan, Map<String, PlaceholderValue> placeHolderKeyValues);
}
