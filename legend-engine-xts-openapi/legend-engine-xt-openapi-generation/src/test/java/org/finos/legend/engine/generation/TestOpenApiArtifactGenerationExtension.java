//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.generation;

import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.dsl.generation.extension.Artifact;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.deployment.DeploymentStateAndVersions;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.*;
import org.junit.Test;

import java.net.URL;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestOpenApiArtifactGenerationExtension
{

    private String getResourceAsString(String path)
    {
        try
        {
            URL infoURL = DeploymentStateAndVersions.class.getClassLoader().getResource(path);
            if (infoURL != null)
            {
                java.util.Scanner scanner = new java.util.Scanner(infoURL.openStream()).useDelimiter("\\A");
                return scanner.hasNext() ? scanner.next() : null;
            }
            return null;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testOpenApiServiceSpecArtifactGenerationForOpenApiProfileLegendService()
    {
        String pureServiceString = getResourceAsString("testService.pure");
        PureModelContextData contextData = PureGrammarParser.newInstance().parseModel(pureServiceString);
        PureModel model = Compiler.compile(contextData, DeploymentMode.TEST, IdentityFactoryProvider.getInstance().getAnonymousIdentity().getName());
        OpenApiArtifactGenerationExtension extension = new OpenApiArtifactGenerationExtension();
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement packageableElement = model.getPackageableElement("test::TestService");
        assertTrue(extension.canGenerate(packageableElement));

        List<Artifact> outputs = extension.generate(packageableElement, model, contextData, PureClientVersions.production);
        assertEquals(1, outputs.size());
        Artifact openapiGenerationResult = outputs.get(0);
        assertTrue(openapiGenerationResult.content.contains("/service/test"));
    }

    @Test
    public void testArtifactGenerationShouldSkipForNonOpenApiProfileLegendService()
    {
        String pureServiceString = getResourceAsString("TestServiceNoProfile.pure");
        PureModelContextData contextData = PureGrammarParser.newInstance().parseModel(pureServiceString);
        PureModel model = Compiler.compile(contextData, DeploymentMode.TEST, IdentityFactoryProvider.getInstance().getAnonymousIdentity().getName());
        OpenApiArtifactGenerationExtension extension = new OpenApiArtifactGenerationExtension();
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement packageableElement = model.getPackageableElement("test::TestServiceNoProfile");
        assertFalse(extension.canGenerate(packageableElement));
    }
}
