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
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.generation.transformers.PlanTransformer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.ConnectionVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.data.DataElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.Root_meta_pure_runtime_Connection;
import org.finos.legend.pure.generated.Root_meta_pure_test_TestSuite;

import java.util.List;

public class MappingTestRunnerContext
{
    final PureModel pureModel;
    final PureModelContextData pureModelContextData;
    final MutableList<PlanTransformer> executionPlanTransformers;
    final ConnectionVisitor<Root_meta_pure_runtime_Connection> connectionVisitor;
    final Root_meta_pure_test_TestSuite metamodelTestSuite;
    final RichIterable<? extends Root_meta_pure_extension_Extension> routerExtensions;
    final org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping mapping;
    final PlanExecutor.ExecuteArgsBuilder executeBuilder;
    final List<DataElement> dataElements;

    public MappingTestRunnerContext(Root_meta_pure_test_TestSuite metamodelTestSuite, org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping mapping, PureModel pureModel, PureModelContextData pureModelContextData, MutableList<PlanTransformer> executionPlanTransformers,
                                    ConnectionVisitor<Root_meta_pure_runtime_Connection> connectionVisitor, RichIterable<? extends Root_meta_pure_extension_Extension> routerExtensions)
    {
        this.pureModel = pureModel;
        this.pureModelContextData = pureModelContextData;
        this.dataElements = pureModelContextData.getElementsOfType(DataElement.class);
        this.mapping = mapping;
        this.executionPlanTransformers = executionPlanTransformers;
        this.connectionVisitor = connectionVisitor;
        this.metamodelTestSuite = metamodelTestSuite;
        this.routerExtensions = routerExtensions;
        this.executeBuilder = PlanExecutor.withArgs();
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


}
