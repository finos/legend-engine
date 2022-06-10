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

package org.finos.legend.engine.external.language.morphir.tests;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.finos.legend.engine.external.language.morphir.extension.MorphirGenerationConfigFromFileGenerationSpecificationBuilder;
import org.finos.legend.engine.external.language.morphir.model.MorphirGenerationConfig;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.fileGeneration.FileGenerationSpecification;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.pure.generated.Root_meta_external_language_morphir_generation_MorphirConfig;
import org.finos.legend.pure.generated.Root_meta_pure_generation_metamodel_GenerationOutput;
import org.finos.legend.pure.generated.core_external_language_morphir_integration;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Scanner;

public class TestMorphirFileGeneration
{
    @Test
    public void testGenerateMorphirRentalExample()
    {
        try
        {
            PureModelContextData pureModelContextData = getProtocol("simpleFileGeneration.json");
            PureModel pureModel = new PureModel(pureModelContextData, null, DeploymentMode.TEST);
            FileGenerationSpecification fileGeneration = pureModelContextData.getElementsOfType(FileGenerationSpecification.class).get(0);
            MorphirGenerationConfig morphirConfig = MorphirGenerationConfigFromFileGenerationSpecificationBuilder.build(fileGeneration);
            Root_meta_external_language_morphir_generation_MorphirConfig metaModelConfig = morphirConfig.process(pureModel);
            List<? extends Root_meta_pure_generation_metamodel_GenerationOutput> outputs = core_external_language_morphir_integration.Root_meta_external_language_morphir_generation_generateMorphirIRFromPureWithScope_MorphirConfig_1__GenerationOutput_MANY_(metaModelConfig, pureModel.getExecutionSupport()).toList();
            Assert.assertEquals(outputs.size(), 1);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private PureModelContextData getProtocol(String fileName) throws JsonProcessingException
    {
        return ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().readValue(this.getResourceAsString(fileName), PureModelContextData.class);
    }

    private String getResourceAsString(String fileName)
    {
        InputStream inputStream = TestMorphirFileGeneration.class.getResourceAsStream(fileName);
        Scanner scanner = new Scanner(inputStream, "UTF-8").useDelimiter("\\A");
        return scanner.hasNext() ? scanner.next() : "";
    }
}
