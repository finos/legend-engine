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

import org.finos.legend.engine.persistence.components.executor.Executor;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanNode;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.optimizer.Optimizer;
import org.finos.legend.engine.persistence.components.relational.jdbc.JdbcHelper;
import org.finos.legend.engine.persistence.components.relational.sql.TabularData;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlGen;
import org.finos.legend.engine.persistence.components.sink.Sink;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.util.Capability;

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

    protected RelationalSink(Set<Capability> capabilities,
                             Map<DataType, Set<DataType>> implicitDataTypeMapping,
                             Map<DataType, Set<DataType>> explicitDataTypeMapping,
                             String quoteIdentifier,
                             Map<Class<?>, LogicalPlanVisitor<?>> logicalPlanVisitorByClass,
                             DatasetExists datasetExists,
                             ValidateMainDatasetSchema validateMainDatasetSchema)
    {
        this.capabilities = capabilities;
        this.implicitDataTypeMapping = implicitDataTypeMapping;
        this.explicitDataTypeMapping = explicitDataTypeMapping;
        this.quoteIdentifier = quoteIdentifier;
        this.logicalPlanVisitorByClass = logicalPlanVisitorByClass;
        this.datasetExists = datasetExists;
        this.validateMainDatasetSchema = validateMainDatasetSchema;
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

    public abstract Optional<Optimizer> optimizerForCaseConversion(CaseConversion caseConversion);

    public interface DatasetExists
    {
        boolean apply(Executor<SqlGen, TabularData, SqlPlan> executor, JdbcHelper sink, Dataset dataset);
    }

    public interface ValidateMainDatasetSchema
    {
        void execute(Executor<SqlGen, TabularData, SqlPlan> executor, JdbcHelper sink, Dataset dataset);
    }
}
