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

package org.finos.legend.engine.external.format.avro.extension;

import org.eclipse.collections.api.RichIterable;
import org.finos.legend.engine.external.format.avro.schema.generations.AvroGenerationConfig;
import org.finos.legend.engine.external.format.avro.schema.generations.AvroGenerationService;
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
import org.finos.legend.pure.generated.core_external_format_avro_transformation_integration;
import org.finos.legend.pure.generated.core_external_format_avro_transformation_transformation_avroSchemaGenerator;

import java.util.ArrayList;
import java.util.List;

public class AvroGenerationExtension implements GenerationExtension
{
    @Override
    public String getLabel()
    {
        return "Avro";
    }

    @Override
    public String getKey()
    {
        return "avro";
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
                return AvroGenerationExtension.this.getLabel();
            }

            @Override
            public String getKey()
            {
                return AvroGenerationExtension.this.getKey();
            }

            @Override
            public List<GenerationProperty> getProperties(PureModel pureModel)
            {
                return FileGenerationDescription.extractGenerationProperties(core_external_format_avro_transformation_integration.Root_meta_external_format_avro_generation_describeConfiguration__GenerationParameter_MANY_(pureModel.getExecutionSupport()));
            }
        };
    }

    @Override
    public ImportConfigurationDescription getImportDescription()
    {
        return null;
    }

    @Override
    public Object getService(ModelManager modelManager)
    {
        return new AvroGenerationService(modelManager);
    }

    @Override
    public List<Root_meta_pure_generation_metamodel_GenerationOutput> generateFromElement(PackageableElement element, CompileContext compileContext)
    {
        if (element instanceof FileGenerationSpecification)
        {
            FileGenerationSpecification specification = (FileGenerationSpecification) element;
            AvroGenerationConfig avroGenerationConfig = AvroGenerationConfigFromFileGenerationSpecificationBuilder.build(specification);
            RichIterable<? extends Root_meta_pure_generation_metamodel_GenerationOutput> output =
                    core_external_format_avro_transformation_transformation_avroSchemaGenerator.Root_meta_external_format_avro_generation_generateAvroFromPureWithScope_AvroConfig_1__AvroOutput_MANY_(avroGenerationConfig.process(compileContext.pureModel), compileContext.pureModel.getExecutionSupport());
            return new ArrayList<>(output.toList());
        }
        return null;
    }

    @Override
    public Root_meta_pure_generation_metamodel_GenerationConfiguration defaultConfig(CompileContext context)
    {
        return core_external_format_avro_transformation_integration.Root_meta_external_format_avro_generation_defaultConfig__AvroConfig_1_(context.pureModel.getExecutionSupport());
    }
}
