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

package org.finos.legend.engine.external.shared.format.model;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.plan.generation.extension.PlanGeneratorExtension;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_ExternalFormatExtension;
import org.finos.legend.pure.generated.Root_meta_pure_router_extension_RouterExtension;
import org.finos.legend.pure.generated.core_external_shared_extension;

public class ExternalFormatPlanGeneratorExtension implements PlanGeneratorExtension {
    @Override
    public RichIterable<? extends Root_meta_pure_router_extension_RouterExtension> getExtraRouterExtensions(PureModel model) {
        RichIterable<Root_meta_external_shared_format_ExternalFormatExtension> planGenerationExtensions = LazyIterate.collect(ExternalFormatPlanGenerationExtensionLoader.extensions().values(), ext -> ext.getPureExtension(model.getExecutionSupport()));
        return core_external_shared_extension.Root_meta_external_shared_format_routerExtensions_String_1__ExternalFormatExtension_MANY__RouterExtension_MANY_("externalFormat", planGenerationExtensions, model.getExecutionSupport());
    }
}
