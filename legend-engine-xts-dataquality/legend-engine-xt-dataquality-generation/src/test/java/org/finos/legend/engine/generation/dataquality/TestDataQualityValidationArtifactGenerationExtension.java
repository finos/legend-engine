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

package org.finos.legend.engine.generation.dataquality;

import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtensionLoader;
import org.finos.legend.engine.language.pure.dsl.generation.extension.Artifact;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestDataQualityValidationArtifactGenerationExtension
{
    @Test
    public void testDataQualityValidationArtifact() throws IOException
    {
        URL url = Objects.requireNonNull(getClass().getClassLoader().getResource("inputs/modelWithDataQualityValidation.json"));
        PureModelContextData pureModelContextData = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().readValue(url, PureModelContextData.class);
        CompilerExtensionLoader.logExtensionList();

        PureModel model = Compiler.compile(pureModelContextData, DeploymentMode.TEST_IGNORE_FUNCTION_MATCH, Identity.getAnonymousIdentity().getName());
        DataQualityValidationArtifactGenerationExtension extension = new DataQualityValidationArtifactGenerationExtension();
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement packageableElement = model.getPackageableElement("meta::dataquality::PersonDataQualityValidation");
        assertTrue(extension.canGenerate(packageableElement));
        List<Artifact> outputs = extension.generate(packageableElement, model, pureModelContextData, "vX_X_X");
        assertEquals(2, outputs.size());
        Optional<Artifact> artifactOptional = outputs.stream().filter(artifact -> "dataQualityRulesMetadata.json".equalsIgnoreCase(artifact.path)).findAny();
        assertTrue(artifactOptional.isPresent());
        Artifact dataQualityMetaData = artifactOptional.get();
        assertEquals("{\"dqRules\":[{\"constraintGrammar\":\"($this.age >= 18)\",\"constraintName\":\"mustBeOfLegalAge\",\"constraintType\":\"Alloy_Constraint_Validation\",\"propertyPath\":\"Person\"},{\"constraintGrammar\":\"Class\",\"constraintName\":\"Person\",\"constraintType\":\"Alloy_Class_Validation\",\"propertyPath\":\"Person\"},{\"constraintGrammar\":\"1\",\"constraintName\":\"name\",\"constraintType\":\"Alloy_Structural_Validation\",\"propertyPath\":\"Person::name\"},{\"constraintGrammar\":\"1\",\"constraintName\":\"age\",\"constraintType\":\"Alloy_Structural_Validation\",\"propertyPath\":\"Person::age\"}]}", dataQualityMetaData.content);

        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement packageableElement2 = model.getPackageableElement("meta::dataquality::Person");
        assertFalse(extension.canGenerate(packageableElement2));

    }
}
