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

import org.finos.legend.engine.external.language.morphir.extension.MorphirGenerationConfigFromFileGenerationSpecificationBuilder;
import org.finos.legend.engine.external.language.morphir.model.MorphirGenerationConfig;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.fileGeneration.FileGenerationSpecification;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.*;
import org.finos.legend.pure.generated.Root_meta_external_language_morphir_generation_MorphirConfig;
import org.finos.legend.pure.generated.Root_meta_pure_generation_metamodel_GenerationOutput;
import org.finos.legend.pure.generated.core_external_language_morphir_transformation_integration;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;

public class TestMorphirFileGeneration
{
    @Test
    public void testGenerateMorphirRentalExample() throws IOException
    {
        PureModelContextData pureModelContextData = getProtocol("org/finos/legend/engine/external/language/morphir/tests/simpleFileGeneration.json");
        PureModel pureModel = new PureModel(pureModelContextData, IdentityFactoryProvider.getInstance().getAnonymousIdentity().getName(), DeploymentMode.PROD);
        FileGenerationSpecification fileGeneration = pureModelContextData.getElementsOfType(FileGenerationSpecification.class).get(0);
        MorphirGenerationConfig morphirConfig = MorphirGenerationConfigFromFileGenerationSpecificationBuilder.build(fileGeneration);
        Root_meta_external_language_morphir_generation_MorphirConfig metaModelConfig = morphirConfig.process(pureModel);
        List<? extends Root_meta_pure_generation_metamodel_GenerationOutput> outputs = core_external_language_morphir_transformation_integration.Root_meta_external_language_morphir_generation_generateMorphirIRFromPureWithScope_MorphirConfig_1__GenerationOutput_MANY_(metaModelConfig, pureModel.getExecutionSupport()).toList();
        Assert.assertEquals(outputs.size(), 1);
    }

    private PureModelContextData getProtocol(String fileName) throws IOException
    {
        try (InputStream stream = Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName)))
        {
            return ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().readValue(stream, PureModelContextData.class);
        }
    }
}
