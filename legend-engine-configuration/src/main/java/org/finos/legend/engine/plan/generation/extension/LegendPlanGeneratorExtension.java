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
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.engine.external.shared.format.model.ExternalFormatPlanGenerationExtensionLoader;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.plan.generation.transformers.LegendPlanTransformers;
import org.finos.legend.engine.plan.generation.transformers.PlanTransformer;
import org.finos.legend.engine.plan.generation.transformers.VersionPlanTransformer;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_ExternalFormatExtension;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.core_external_shared_extension;
import org.finos.legend.pure.generated.core_servicestore_router_router_extension;

import static org.finos.legend.pure.generated.core_relational_relational_extensions_router_extension.Root_meta_relational_extension_relationalExtensions__Extension_MANY_;

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

        RichIterable<Root_meta_external_shared_format_ExternalFormatExtension> planGenerationExtensions = LazyIterate.collect(ExternalFormatPlanGenerationExtensionLoader.extensions().values(), ext -> ext.getPureExtension(pureModel.getExecutionSupport()));
        pureExtensions.addAll(core_external_shared_extension.Root_meta_external_shared_format_routerExtensions_String_1__ExternalFormatExtension_MANY__Extension_MANY_("externalFormat", planGenerationExtensions, pureModel.getExecutionSupport()).toList());

        pureExtensions.add(core_servicestore_router_router_extension.Root_meta_external_store_service_extension_serviceStoreExtensions__Extension_1_(pureModel.getExecutionSupport()));
        pureExtensions.addAll(Root_meta_relational_extension_relationalExtensions__Extension_MANY_(pureModel.getExecutionSupport()).toList());

        return pureExtensions.toImmutable();
    }

}
