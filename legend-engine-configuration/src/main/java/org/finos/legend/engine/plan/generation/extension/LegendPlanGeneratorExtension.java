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

import static org.finos.legend.pure.generated.core_external_format_flatdata_externalFormatContract.Root_meta_external_format_flatdata_extension_flatDataFormatExtension__Extension_1_;
import static org.finos.legend.pure.generated.core_external_format_json_externalFormatContract.Root_meta_external_format_json_extension_jsonSchemaFormatExtension__Extension_1_;
import static org.finos.legend.pure.generated.core_external_format_xml_externalFormatContract.Root_meta_external_format_xml_extension_xsdFormatExtension__Extension_1_;
import static org.finos.legend.pure.generated.core_pure_binding_extension.Root_meta_external_shared_format_externalFormatExtension__Extension_1_;
import static org.finos.legend.pure.generated.core_relational_relational_extensions_extension.Root_meta_relational_extension_relationalExtensions__Extension_MANY_;
import static org.finos.legend.pure.generated.core_servicestore_extensions_extension.Root_meta_external_store_service_extension_serviceStoreExtensions__Extension_1_;

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

        pureExtensions.add(Root_meta_external_shared_format_externalFormatExtension__Extension_1_(pureModel.getExecutionSupport()));
        pureExtensions.add(Root_meta_external_format_flatdata_extension_flatDataFormatExtension__Extension_1_(pureModel.getExecutionSupport()));
        pureExtensions.add(Root_meta_external_format_json_extension_jsonSchemaFormatExtension__Extension_1_(pureModel.getExecutionSupport()));
        pureExtensions.add(Root_meta_external_format_xml_extension_xsdFormatExtension__Extension_1_(pureModel.getExecutionSupport()));

        pureExtensions.add(Root_meta_external_store_service_extension_serviceStoreExtensions__Extension_1_(pureModel.getExecutionSupport()));
        pureExtensions.addAll(Root_meta_relational_extension_relationalExtensions__Extension_MANY_(pureModel.getExecutionSupport()).toList());

        return pureExtensions.toImmutable();
    }

}
