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

package org.finos.legend.engine.external.shared.format.model.transformation.fromModel;

import org.finos.legend.engine.external.shared.format.model.ExternalFormatExtension;
import org.finos.legend.engine.external.shared.format.model.transformation.Generator;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.ModelUnit;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.externalFormat.ExternalFormatSchema;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.externalFormat.ExternalFormatSchemaSet;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_metamodel_Schema;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_metamodel_SchemaSet;

import java.util.Map;

public class ModelToSchemaGenerator extends Generator
{
    private final Map<String, ExternalFormatExtension<?>> schemaExtensions;

    public ModelToSchemaGenerator(PureModel pureModel, Map<String, ExternalFormatExtension<?>> schemaExtensions)
    {
        super(pureModel);
        this.schemaExtensions = schemaExtensions;
    }

    public PureModelContextData generate(ModelToSchemaConfiguration configuration, ModelUnit sourceModelUnit, boolean generateBinding, String targetBindingPath)
    {
        if (generateBinding)
        {
            if (targetBindingPath == null || !targetBindingPath.matches("[A-Za-z0-9_]+(::[A-Za-z0-9_]+)+"))
            {
                throw new IllegalArgumentException("Invalid path provided for target binding");
            }
        }

        ExternalFormatExtension<?> schemaExtension = schemaExtensions.get(configuration.format);
        if (schemaExtension == null)
        {
            throw new IllegalArgumentException("Unknown schema format: " + configuration.format);
        }
        if (!(schemaExtension instanceof ExternalFormatSchemaGenerationExtension))
        {
            throw new UnsupportedOperationException("Schema generation not supported for " + schemaExtension.getFormat());
        }

        Root_meta_external_shared_format_metamodel_SchemaSet schemaSet = ((ExternalFormatSchemaGenerationExtension) schemaExtension).generateSchema(configuration, sourceModelUnit, pureModel);
        ExternalFormatSchemaSet externalFormatSchemaSet = transformSchemaSet(schemaSet, schemaExtension);

        PureModelContextData.Builder builder = PureModelContextData.newBuilder();
        builder.addElement(externalFormatSchemaSet);
        if (generateBinding)
        {
            builder.addElement(generateBinding(targetBindingPath, externalFormatSchemaSet.getPath(), null, schemaExtension.getContentTypes().get(0), sourceModelUnit));
        }

        return builder.build();
    }

    private ExternalFormatSchemaSet transformSchemaSet(Root_meta_external_shared_format_metamodel_SchemaSet schemaSet, ExternalFormatExtension schemaExtension)
    {
        ExternalFormatSchemaSet result = new ExternalFormatSchemaSet();
        result.name = schemaSet._name();
        result._package = elementToPath(schemaSet._package());
        result.format = schemaSet._format();
        for (Root_meta_external_shared_format_metamodel_Schema schema : schemaSet._schemas())
        {
            ExternalFormatSchema externalFormatSchema = new ExternalFormatSchema();
            externalFormatSchema.id = schema._id();
            externalFormatSchema.location = schema._location();
            externalFormatSchema.content = schemaExtension.metamodelToText(schema._detail(), pureModel);
            result.schemas.add(externalFormatSchema);
        }
        return result;
    }
}
