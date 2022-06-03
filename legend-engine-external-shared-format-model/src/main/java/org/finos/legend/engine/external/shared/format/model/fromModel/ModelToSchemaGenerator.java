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

package org.finos.legend.engine.external.shared.format.model.fromModel;

import org.finos.legend.engine.external.shared.format.model.Generator;
import org.finos.legend.engine.external.shared.format.model.ExternalFormatExtension;
import org.finos.legend.engine.external.shared.format.model.ExternalFormatExtensionLoader;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.external.shared.ExternalFormatSchema;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.external.shared.ExternalFormatSchemaSet;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_binding_Binding;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_metamodel_Schema;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_metamodel_SchemaSet;

import java.lang.reflect.ParameterizedType;
import java.util.Map;

public class ModelToSchemaGenerator extends Generator
{
    private final Map<String, ExternalFormatExtension> schemaExtensions = ExternalFormatExtensionLoader.extensions();

    public ModelToSchemaGenerator(PureModel pureModel)
    {
        super(pureModel);
    }

    public PureModelContextData generate(ModelToSchemaConfiguration configuration)
    {
        ExternalFormatExtension schemaExtension = schemaExtensions.get(configuration.format);
        if (schemaExtension == null)
        {
            throw new IllegalArgumentException("Unknown schema format: " + configuration.format);
        }

        Class<?> configClass = (Class<?>) ((ParameterizedType)schemaExtension.getClass().getGenericInterfaces()[0]).getActualTypeArguments()[2];
        if (!configClass.isInstance(configuration))
        {
            throw new IllegalArgumentException("Invalid configuration for " + configuration.format + " model generation: " + configuration.getClass().getSimpleName());
        }

        Root_meta_external_shared_format_binding_Binding binding = schemaExtension.generateSchema(configuration, pureModel);

        PureModelContextData.Builder builder = PureModelContextData.newBuilder();
        builder.addElement(transformSchemaSet(binding._schemaSet(), schemaExtension));
        builder.addElement(transformBinding(binding));
        return builder.build();
    }

    private ExternalFormatSchemaSet transformSchemaSet(Root_meta_external_shared_format_metamodel_SchemaSet schemaSet, ExternalFormatExtension schemaExtension)
    {
        ExternalFormatSchemaSet result = new ExternalFormatSchemaSet();
        result.name = schemaSet._name();
        result._package = elementToPath(schemaSet._package());
        result.format = schemaSet._format();
        for (Root_meta_external_shared_format_metamodel_Schema schema: schemaSet._schemas())
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
