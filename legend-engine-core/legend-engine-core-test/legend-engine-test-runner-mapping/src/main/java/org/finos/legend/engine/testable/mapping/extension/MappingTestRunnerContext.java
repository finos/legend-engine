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

package org.finos.legend.engine.testable.mapping.extension;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.generation.transformers.PlanTransformer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.ConnectionVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.data.DataElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_metamodel_MappingTestSuite;
import org.finos.legend.pure.generated.Root_meta_core_runtime_Connection;

import java.util.List;
import java.util.Map;

class MappingTestRunnerContext
{
    private final PureModel pureModel;
    private final PureModelContextData pureModelContextData;
    private final MutableList<PlanTransformer> executionPlanTransformers;
    private final ConnectionVisitor<Root_meta_core_runtime_Connection> connectionVisitor;
    private final Root_meta_pure_mapping_metamodel_MappingTestSuite metamodelTestSuite;
    private final RichIterable<? extends Root_meta_pure_extension_Extension> routerExtensions;
    private final org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping mapping;
    private final PlanExecutor.ExecuteArgsBuilder executeBuilder;
    private final Map<String, DataElement> dataElementIndex;
    private SingleExecutionPlan plan;
    private List<Connection> connections;

    public MappingTestRunnerContext(Root_meta_pure_mapping_metamodel_MappingTestSuite metamodelTestSuite, org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping mapping, PureModel pureModel, PureModelContextData pureModelContextData, MutableList<PlanTransformer> executionPlanTransformers,
                                    ConnectionVisitor<Root_meta_core_runtime_Connection> connectionVisitor, RichIterable<? extends Root_meta_pure_extension_Extension> routerExtensions)
    {
        this.pureModel = pureModel;
        this.pureModelContextData = pureModelContextData;
        this.dataElementIndex = this.buildDataElementIndex(pureModelContextData);
        this.mapping = mapping;
        this.executionPlanTransformers = executionPlanTransformers;
        this.connectionVisitor = connectionVisitor;
        this.metamodelTestSuite = metamodelTestSuite;
        this.routerExtensions = routerExtensions;
        this.executeBuilder = PlanExecutor.withArgs();
    }

    private Map<String, DataElement> buildDataElementIndex(PureModelContextData pureModelContextData)
    {
        Map<String, DataElement> result = Maps.mutable.empty();
        pureModelContextData.getElementsOfType(DataElement.class).forEach(d -> result.put(d.getPath(), d));
        return result;
    }

    public void withConnections(List<Connection> connections)
    {
        this.connections = connections;
    }

    public List<Connection> getConnections()
    {
        return connections;
    }

    public PureModel getPureModel()
    {
        return pureModel;
    }

    public Mapping getMapping()
    {
        return mapping;
    }

    public PureModelContextData getPureModelContextData()
    {
        return pureModelContextData;
    }

    public PlanExecutor.ExecuteArgsBuilder getExecuteBuilder()
    {
        return executeBuilder;
    }

    public Map<String, DataElement> getDataElementIndex()
    {
        return dataElementIndex;
    }

    public SingleExecutionPlan getPlan()
    {
        return plan;
    }

    public void withPlan(SingleExecutionPlan plan)
    {
        this.executeBuilder.withPlan(plan);
        this.plan = plan;
    }

    public ConnectionVisitor<Root_meta_core_runtime_Connection> getConnectionVisitor()
    {
        return connectionVisitor;
    }

    public RichIterable<? extends Root_meta_pure_extension_Extension> getRouterExtensions()
    {
        return routerExtensions;
    }

    public MutableList<PlanTransformer> getExecutionPlanTransformers()
    {
        return executionPlanTransformers;
    }

    public Root_meta_pure_mapping_metamodel_MappingTestSuite getMetamodelTestSuite()
    {
        return metamodelTestSuite;
    }
}
