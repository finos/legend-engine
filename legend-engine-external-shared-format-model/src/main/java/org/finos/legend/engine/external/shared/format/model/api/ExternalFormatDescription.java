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

package org.finos.legend.engine.external.shared.format.model.api;

import org.eclipse.collections.api.RichIterable;
import org.finos.legend.engine.external.shared.format.generations.description.GenerationParameter;
import org.finos.legend.engine.external.shared.format.generations.description.GenerationProperty;
import org.finos.legend.engine.external.shared.format.model.ExternalFormatExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.pure.generated.Root_meta_pure_generation_metamodel_GenerationParameter;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ExternalFormatDescription
{
    public String name;

    public List<String> contentTypes;

    public boolean supportsSchemaGeneration;
    public List<GenerationProperty> schemaGenerationProperties;

    public boolean supportsModelGeneration;
    public List<GenerationProperty> modelGenerationProperties;

    public static ExternalFormatDescription newDescription(ExternalFormatExtension extension, PureModel pureModel)
    {
        ExternalFormatDescription result = new ExternalFormatDescription();
        result.name = extension.getFormat();
        result.contentTypes = Collections.unmodifiableList(extension.getContentTypes());
        result.supportsModelGeneration = extension.supportsModelGeneration();
        result.modelGenerationProperties = toGenerationProperties(extension.getModelGenerationProperties(pureModel));
        result.supportsSchemaGeneration = extension.supportsSchemaGeneration();
        result.schemaGenerationProperties = toGenerationProperties(extension.getSchemaGenerationProperties(pureModel));
        return result;
    }

    public static List<GenerationProperty> toGenerationProperties(RichIterable<? extends Root_meta_pure_generation_metamodel_GenerationParameter> props)
    {
        return props == null ? Collections.emptyList() : Collections.unmodifiableList(props.toList().stream().map(GenerationParameter::new).collect(Collectors.toList()));
    }
}
