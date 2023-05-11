// Copyright 2023 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.language.pure.grammar.integration.plan;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.plan.generation.extension.PlanGeneratorExtension;
import org.finos.legend.engine.plan.generation.transformers.PlanTransformer;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.core_nonrelational_mongodb_java_platform_binding_mongodbStoreLegendJavaPlatformBindingExtension;

public class MongoDBPlanGeneratorExtension implements PlanGeneratorExtension
{
    @Override
    public MutableList<PlanTransformer> getExtraPlanTransformers()
    {
        return PlanGeneratorExtension.super.getExtraPlanTransformers();
    }

    @Override
    public RichIterable<? extends Root_meta_pure_extension_Extension> getExtraExtensions(PureModel pureModel)
    {
        return core_nonrelational_mongodb_java_platform_binding_mongodbStoreLegendJavaPlatformBindingExtension.Root_meta_external_store_mongodb_executionPlan_platformBinding_legendJava_mongoDBLegendJavaPlatformBindingExtensions__Extension_MANY_(pureModel.getExecutionSupport());
    }
}
