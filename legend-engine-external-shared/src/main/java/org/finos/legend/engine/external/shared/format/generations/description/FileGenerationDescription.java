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

package org.finos.legend.engine.external.shared.format.generations.description;

import org.eclipse.collections.api.RichIterable;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.pure.generated.Root_meta_pure_generation_metamodel_GenerationParameter;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class FileGenerationDescription
{
    public String key;

    public String label;

    public List<GenerationProperty> properties;

    FileGenerationDescription(String key, String label, List<GenerationProperty> properties)
    {
        this.key = key;
        this.label = label;
        this.properties = properties;
    }

    public static FileGenerationDescription newDescription(GenerationConfigurationDescription generationConfigurationDescription, PureModel pureModel)
    {
        return new FileGenerationDescription(generationConfigurationDescription.getKey(), generationConfigurationDescription.getLabel(), generationConfigurationDescription.getProperties(pureModel));
    }

    public static List<GenerationProperty> extractGenerationProperties(RichIterable<? extends Root_meta_pure_generation_metamodel_GenerationParameter> props)
    {
        return props == null ? Collections.emptyList() : props.toList().stream().map(GenerationParameter::new).collect(Collectors.toList());
    }
}
