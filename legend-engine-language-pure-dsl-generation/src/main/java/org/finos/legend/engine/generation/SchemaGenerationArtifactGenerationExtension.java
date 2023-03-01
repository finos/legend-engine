// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.generation;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.finos.legend.engine.external.shared.format.model.ExternalFormatExtension;
import org.finos.legend.engine.external.shared.format.model.ExternalFormatExtensionLoader;
import org.finos.legend.engine.external.shared.format.model.transformation.fromModel.ModelToSchemaGenerator;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.dsl.generation.extension.Artifact;
import org.finos.legend.engine.language.pure.dsl.generation.extension.ArtifactGenerationExtension;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.externalFormat.ExternalFormatSchemaSet;
import org.finos.legend.pure.generated.Root_meta_pure_generation_metamodel_SchemaGenerationSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;

public class SchemaGenerationArtifactGenerationExtension implements ArtifactGenerationExtension
{

    static final Map<String, ExternalFormatExtension<?>> extensions = ExternalFormatExtensionLoader.extensions();

    public final String ROOT_PATH = "schema-generation";

    @Override
    public String getKey()
    {
        return ROOT_PATH;
    }

    @Override
    public boolean canGenerate(PackageableElement element)
    {
        return element instanceof Root_meta_pure_generation_metamodel_SchemaGenerationSpecification;
    }

    @Override
    public List<Artifact> generate(PackageableElement element, PureModel pureModel, PureModelContextData data, String clientVersion)
    {
        Root_meta_pure_generation_metamodel_SchemaGenerationSpecification generationElement = (Root_meta_pure_generation_metamodel_SchemaGenerationSpecification) element;
        ModelToSchemaGenerator generator = new ModelToSchemaGenerator(pureModel, extensions);
        ExternalFormatSchemaSet schemaSet = generator.generate(generationElement._modelToCodeConfiguration(), generationElement._modelUnit(), false, null);
        return schemaSet.schemas.stream().map(schema -> new Artifact(schema.content, schema.location, schemaSet.format)).collect(Collectors.toList());
    }

}
