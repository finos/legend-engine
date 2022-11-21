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

package org.finos.legend.engine.plan.generation.extension;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.plan.generation.transformers.LegendPlanTransformers;
import org.finos.legend.engine.plan.generation.transformers.PlanTransformer;
import org.finos.legend.engine.plan.generation.transformers.VersionPlanTransformer;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.core_configuration_coreExtensions;

public class LegendPlanGeneratorExtension implements PlanGeneratorExtension
{

    @Override
    public MutableList<PlanTransformer> getExtraPlanTransformers()
    {
        return Lists.mutable.withAll(LegendPlanTransformers.transformers).with(new VersionPlanTransformer());
    }

    @Override
    public RichIterable<? extends Root_meta_pure_extension_Extension> getExtraExtensions(PureModel pureModel)
    {
        MutableList<Root_meta_pure_extension_Extension> pureExtensions = Lists.mutable.empty();

        pureExtensions.addAll(core_configuration_coreExtensions.Root_meta_pure_extension_configuration_coreExtensions__Extension_MANY_(pureModel.getExecutionSupport()).toList());

        return pureExtensions.toImmutable();
    }

}
