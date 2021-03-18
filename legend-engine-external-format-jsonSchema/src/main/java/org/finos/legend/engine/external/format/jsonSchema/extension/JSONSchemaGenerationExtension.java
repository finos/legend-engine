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

package org.finos.legend.engine.external.format.jsonSchema.extension;

import org.eclipse.collections.api.RichIterable;
import org.finos.legend.engine.external.format.jsonSchema.schema.generations.JSONSchemaConfig;
import org.finos.legend.engine.external.format.jsonSchema.schema.generations.JSONSchemaGenerationService;
import org.finos.legend.engine.external.shared.format.extension.GenerationExtension;
import org.finos.legend.engine.external.shared.format.extension.GenerationMode;
import org.finos.legend.engine.external.shared.format.generations.description.FileGenerationDescription;
import org.finos.legend.engine.external.shared.format.generations.description.GenerationConfigurationDescription;
import org.finos.legend.engine.external.shared.format.generations.description.GenerationProperty;
import org.finos.legend.engine.external.shared.format.imports.description.ImportConfigurationDescription;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.fileGeneration.FileGenerationSpecification;
import org.finos.legend.pure.generated.Root_meta_pure_generation_metamodel_GenerationConfiguration;
import org.finos.legend.pure.generated.Root_meta_pure_generation_metamodel_GenerationOutput;
import org.finos.legend.pure.generated.core_json_jsonSchema;

import java.util.ArrayList;
import java.util.List;

public class JSONSchemaGenerationExtension implements GenerationExtension
{
    @Override
    public String getLabel()
    {
        return "JSON Schema";
    }

    @Override
    public String getKey()
    {
        return "jsonSchema";
    }

    @Override
    public GenerationMode getMode()
    {
        return GenerationMode.Schema;
    }

    @Override
    public GenerationConfigurationDescription getGenerationDescription()
    {
        return new GenerationConfigurationDescription()
        {
            @Override
            public String getLabel()
            {
                return JSONSchemaGenerationExtension.this.getLabel();
            }

            @Override
            public String getKey()
            {
                return JSONSchemaGenerationExtension.this.getKey();
            }

            @Override
            public List<GenerationProperty> getProperties(PureModel pureModel)
            {
                return FileGenerationDescription.extractGenerationProperties(core_json_jsonSchema
                        .Root_meta_json_schema_generation_describeConfiguration__GenerationParameter_MANY_(pureModel.getExecutionSupport()));
            }
        };
    }

    @Override
    public ImportConfigurationDescription getImportDescription()
    {
        return new ImportConfigurationDescription()
        {
            @Override
            public String getKey()
            {
                return JSONSchemaGenerationExtension.this.getKey();
            }

            @Override
            public String getLabel()
            {
                return JSONSchemaGenerationExtension.this.getLabel();
            }
        };
    }

    @Override
    public Root_meta_pure_generation_metamodel_GenerationConfiguration defaultConfig(CompileContext compileContext)
    {
        return core_json_jsonSchema.Root_meta_json_schema_generation_defaultConfig__JSONSchemaConfig_1_(compileContext.pureModel.getExecutionSupport());
    }

    @Override
    public Object getService(ModelManager modelManager)
    {
        return new JSONSchemaGenerationService(modelManager);
    }

    @Override
    public List<Root_meta_pure_generation_metamodel_GenerationOutput> generateFromElement(PackageableElement packageableElement, CompileContext compileContext)
    {
        if (packageableElement instanceof FileGenerationSpecification)
        {
            FileGenerationSpecification fileGenerationSpecification = (FileGenerationSpecification) packageableElement;
            JSONSchemaConfig jsonSchemaConfig = JSONSchemaGenerationConfigBuilder.build(fileGenerationSpecification);
            RichIterable<? extends Root_meta_pure_generation_metamodel_GenerationOutput> outputs = core_json_jsonSchema.Root_meta_json_schema_generation_generateJsonSchemaFromPureWithScope_JSONSchemaConfig_1__JSONSchemaOutput_MANY_(jsonSchemaConfig.process(compileContext.pureModel), compileContext.pureModel.getExecutionSupport());
            return new ArrayList<>(outputs.toList());
        }
        return null;
    }

}
