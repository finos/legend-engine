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

package org.finos.legend.engine.query.graphQL.api.execute.directives;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.ImmutableList;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.generation.transformers.PlanTransformer;
import org.finos.legend.engine.protocol.graphQL.metamodel.Directive;
import org.finos.legend.engine.protocol.graphQL.metamodel.Document;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.ExecutionPlan;
import org.finos.legend.engine.shared.core.extension.LegendModuleSpecificExtension;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.pure.generated.Root_meta_core_runtime_Runtime;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;

import java.util.Map;

public interface IGraphQLDirectiveExtension extends LegendModuleSpecificExtension
{
    ImmutableList<String> getSupportedDirectives();

    ExecutionPlan planDirective(
            Document document,
            PureModel pureModel,
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> _class,
            Mapping mapping,
            Root_meta_core_runtime_Runtime runtime,
            RichIterable<? extends Root_meta_pure_extension_Extension> _extensions,
            Iterable<? extends PlanTransformer> transformers);

    Object executeDirective(Directive directive, ExecutionPlan executionPlan, PlanExecutor planExecutor, Map<String, Result> parameterMap, Identity identity);
}
