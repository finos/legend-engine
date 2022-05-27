// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.external.format.protobuf.schema.generations;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.external.shared.format.generations.GenerationConfiguration;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.pure.generated.core_external_format_protobuf_integration;
import org.finos.legend.pure.generated.core_pure_corefunctions_metaExtension;
import org.finos.legend.pure.generated.Root_meta_external_format_protobuf_generation_ProtobufConfig;
import org.finos.legend.pure.generated.Root_meta_external_format_protobuf_generation_ProtobufOption_Impl;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;

import java.util.List;

public class ProtobufGenerationConfig extends GenerationConfiguration
{
    public Options options;

    public Root_meta_external_format_protobuf_generation_ProtobufConfig transformToPure(PureModel pureModel)
    {
        Root_meta_external_format_protobuf_generation_ProtobufConfig generationConfiguration = core_external_format_protobuf_integration.Root_meta_external_format_protobuf_generation_defaultConfig__ProtobufConfig_1_(pureModel.getExecutionSupport());
        List<PackageableElement> scopeElements = ListIterate.collect(this.generationScope(), e -> core_pure_corefunctions_metaExtension.Root_meta_pure_functions_meta_pathToElement_String_1__PackageableElement_1_(e, pureModel.getExecutionSupport()));
        if (options != null)
        {
            generationConfiguration
                    ._javaPackage(options.javaPackage)
                    ._javaOuterClassname(options.javaOuterClassname)
                    ._javaMultipleFiles(options.javaMultipleFiles)
                    ._customOptions(Lists.mutable.withAll(Iterate.collect(options.customOptions, o -> new Root_meta_external_format_protobuf_generation_ProtobufOption_Impl(""))))
                    ._optimizeFor(options.optimizeFor == null ? null : pureModel.getEnumValue("meta::protocols::pure::vX_X_X::metamodel::invocation::generation::protobuf::OptimizeMode", options.optimizeFor.name())
                    );
        }

        return generationConfiguration._scopeElements((RichIterable<? extends PackageableElement>) scopeElements);
    }
}
