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

package org.finos.legend.engine.testable.function.extension;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.generation.transformers.PlanTransformer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.ConnectionVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.data.DataElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.function.FunctionTestSuite;
import org.finos.legend.pure.generated.Root_meta_core_runtime_Connection;
import org.finos.legend.pure.generated.Root_meta_legend_function_metamodel_FunctionTestSuite;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;

import java.util.Map;

public class FunctionTestRunnerContext
{
    private final Pair<PureModelContextData, PureModel> models;
    private final Pair<FunctionTestSuite, Root_meta_legend_function_metamodel_FunctionTestSuite> suites;
    private final Map<String, DataElement> dataElementIndex;

    // execution
    private final MutableList<PlanTransformer> executionPlanTransformers;
    private final ConnectionVisitor<Root_meta_core_runtime_Connection> connectionVisitor;
    private final RichIterable<? extends Root_meta_pure_extension_Extension> routerExtensions;
    private final PlanExecutor.ExecuteArgsBuilder executeBuilder;

    public FunctionTestRunnerContext(Pair<PureModelContextData, PureModel> models, Pair<FunctionTestSuite, Root_meta_legend_function_metamodel_FunctionTestSuite> suites,
                                     MutableList<PlanTransformer> executionPlanTransformers,
                                     ConnectionVisitor<Root_meta_core_runtime_Connection> connectionVisitor, RichIterable<? extends Root_meta_pure_extension_Extension> routerExtensions
    )
    {
        this.models = models;
        this.suites = suites;
        this.executionPlanTransformers = executionPlanTransformers;
        this.connectionVisitor = connectionVisitor;
        this.routerExtensions = routerExtensions;
        this.executeBuilder = PlanExecutor.withArgs();
        this.dataElementIndex = this.buildDataElementIndex(models.getOne());

    }

    private Map<String, DataElement> buildDataElementIndex(PureModelContextData pureModelContextData)
    {
        Map<String, DataElement> result = Maps.mutable.empty();
        pureModelContextData.getElementsOfType(DataElement.class).forEach(d -> result.put(d.getPath(), d));
        return result;
    }

    public PureModel getPureModel()
    {
        return models.getTwo();
    }

    public PureModelContextData getPureModelContextData()
    {
        return models.getOne();
    }

    public Root_meta_legend_function_metamodel_FunctionTestSuite getTestSuite()
    {
        return this.suites.getTwo();
    }

    public FunctionTestSuite getProtocolSuite()
    {
        return this.suites.getOne();
    }

    public Map<String, DataElement> getDataElementIndex()
    {
        return dataElementIndex;
    }


    public PlanExecutor.ExecuteArgsBuilder getExecuteBuilder()
    {
        return executeBuilder;
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

}
