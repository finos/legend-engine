// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.language.pure.dsl.service.generation.extension;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.plan.generation.transformers.PlanTransformer;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.ExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Execution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.ServiceTest_Legacy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.TestContainer;
import org.finos.legend.engine.shared.core.extension.LegendModuleSpecificExtension;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;

import java.util.List;
import java.util.Optional;

public interface ServiceExecutionExtension extends LegendModuleSpecificExtension
{
    @Deprecated
    default Optional<Pair<ExecutionPlan, RichIterable<? extends String>>> tryToBuildTestExecutorContext(Execution execution, String testData, ObjectMapper objectMapper, PureModel pureModel, RichIterable<? extends Root_meta_pure_extension_Extension> extensions, MutableList<PlanTransformer> transformers, String pureVersion)
    {
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    default Optional<Pair<ExecutionPlan, RichIterable<? extends String>>> tryToBuildTestExecutorContext(Execution execution, String testData, ObjectMapper objectMapper, PureModel pureModel, RichIterable<? extends Root_meta_pure_extension_Extension> extensions, Iterable<? extends PlanTransformer> transformers, String pureVersion)
    {
        // For backward compatibility
        return tryToBuildTestExecutorContext(execution, testData, objectMapper, pureModel, extensions, (transformers instanceof MutableList) ? (MutableList<PlanTransformer>) transformers : Lists.mutable.withAll(transformers), pureVersion);
    }

    default Optional<List<TestContainer>> tryToBuildTestAsserts(ServiceTest_Legacy test, ObjectMapper objectMapper, PureModel pureModel)
    {
        return Optional.empty();
    }
}
