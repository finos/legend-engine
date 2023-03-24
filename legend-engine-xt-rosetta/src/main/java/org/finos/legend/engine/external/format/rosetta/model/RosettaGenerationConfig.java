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

package org.finos.legend.engine.external.format.rosetta.model;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.external.shared.format.generations.GenerationConfiguration;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.pure.generated.Root_meta_external_format_rosetta_generation_RosettaConfig;
import org.finos.legend.pure.generated.core_external_format_rosetta_transformation_integration;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;

import java.util.List;

import static org.finos.legend.pure.generated.platform_pure_basics_meta_pathToElement.Root_meta_pure_functions_meta_pathToElement_String_1__PackageableElement_1_;

public class RosettaGenerationConfig extends GenerationConfiguration
{

    public Root_meta_external_format_rosetta_generation_RosettaConfig process(PureModel pureModel)
    {
        Root_meta_external_format_rosetta_generation_RosettaConfig generationCdmConfig = core_external_format_rosetta_transformation_integration.Root_meta_external_format_rosetta_generation_defaultConfig__RosettaConfig_1_(pureModel.getExecutionSupport());
        List<PackageableElement> scopeElements = ListIterate.collect(this.generationScope(), e -> Root_meta_pure_functions_meta_pathToElement_String_1__PackageableElement_1_(e, pureModel.getExecutionSupport()));
        return generationCdmConfig._scopeElements((RichIterable<? extends PackageableElement>) scopeElements);
    }
}
