// Copyright 2023 Goldman Sachs
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

import static org.finos.legend.engine.generation.SearchDocumentArtifactGenerationExtension.FILE_NAME;

import java.net.URL;
import java.util.List;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.dsl.generation.extension.Artifact;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.Protocol;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.model.context.AlloySDLC;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextPointer;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.deployment.DeploymentStateAndVersions;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.*;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_Service;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.junit.Assert;
import org.junit.Test;


public class TestSearchDocumentArtifactGenerationExtension
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
    public void testArtifactGenerationWithStereotypeInProgress()
    {
        String pureModelString = getResourceAsString("models/Model.pure");
        PureModelContextData pureModelContextData = PureGrammarParser.newInstance().parseModel(pureModelString);
        PureModel pureModel = Compiler.compile(pureModelContextData, DeploymentMode.TEST, IdentityFactoryProvider.getInstance().getAnonymousIdentity().getName());
        SearchDocumentArtifactGenerationExtension extension = new SearchDocumentArtifactGenerationExtension();
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement packageableElement = pureModel.getPackageableElement("test::Service");
        Root_meta_legend_service_metamodel_Service metamodelClass = (Root_meta_legend_service_metamodel_Service) packageableElement;
        Assert.assertFalse(extension.canGenerate(metamodelClass));
    }

    @Test
    public void testSearchDocumentArtifactGenerationExtensionWithUnknownMavenCoordinates()
    {
        String pureModelString = getResourceAsString("models/Model.pure");
        PureModelContextData pureModelContextData = PureGrammarParser.newInstance().parseModel(pureModelString);
        PureModel pureModel = Compiler.compile(pureModelContextData, DeploymentMode.TEST, IdentityFactoryProvider.getInstance().getAnonymousIdentity().getName());
        SearchDocumentArtifactGenerationExtension extension = new SearchDocumentArtifactGenerationExtension();
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement packageableElement = pureModel.getPackageableElement("model::Person");
        Assert.assertTrue(packageableElement instanceof Class);
        Class<?> metamodelClass = (Class<?>) packageableElement;
        Assert.assertTrue(extension.canGenerate(metamodelClass));

        List<Artifact> outputs = extension.generate(packageableElement, pureModel, pureModelContextData, PureClientVersions.production);
        Assert.assertEquals(1, outputs.size());
        Artifact searchDocumentResult = outputs.get(0);
        Assert.assertEquals("json", searchDocumentResult.format);
        Assert.assertEquals(FILE_NAME, searchDocumentResult.path);
        Assert.assertNotNull(searchDocumentResult.content);
        Assert.assertEquals("{\"taggedValues\":{},\"package\":\"model\",\"name\":\"Person\",\"description\":\"\",\"projectCoordinates\":{\"versionId\":\"UNKNOWN\",\"groupId\":\"UNKNOWN\",\"artifactId\":\"UNKNOWN\"},\"id\":\"model::Person\",\"type\":\"Class\",\"properties\":[{\"taggedValues\":{},\"name\":\"firstName\"},{\"taggedValues\":{},\"name\":\"lastName\"}]}", searchDocumentResult.content);
    }

    @Test
    public void testSearchDocumentArtifactGenerationExtensionWithKnownMavenCoordinates()
    {
        String pureModelString = getResourceAsString("models/Model.pure");
        PureModelContextData pureModelContextData = PureGrammarParser.newInstance().parseModel(pureModelString);
        PureModelContextPointer origin = new PureModelContextPointer();
        AlloySDLC sdlcInfo = new AlloySDLC();
        sdlcInfo.groupId = "org.finos.test";
        sdlcInfo.artifactId = "test-project";
        sdlcInfo.version = "0.0.1-SNAPSHOT";
        origin.sdlcInfo = sdlcInfo;
        origin.serializer = new Protocol("pure", PureClientVersions.production);
        PureModelContextData pureModelContextDataWithOrigin = PureModelContextData.newBuilder().withPureModelContextData(pureModelContextData).withOrigin(origin).build();

        PureModel pureModel = Compiler.compile(pureModelContextDataWithOrigin, DeploymentMode.TEST, IdentityFactoryProvider.getInstance().getAnonymousIdentity().getName());
        SearchDocumentArtifactGenerationExtension extension = new SearchDocumentArtifactGenerationExtension();
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement packageableElement = pureModel.getPackageableElement("model::Person");
        Assert.assertTrue(packageableElement instanceof Class);
        Class<?> metamodelClass = (Class<?>) packageableElement;
        Assert.assertTrue(extension.canGenerate(metamodelClass));

        List<Artifact> outputs = extension.generate(packageableElement, pureModel, pureModelContextDataWithOrigin, PureClientVersions.production);
        Assert.assertEquals(1, outputs.size());
        Artifact searchDocumentResult = outputs.get(0);
        Assert.assertEquals("json", searchDocumentResult.format);
        Assert.assertEquals(FILE_NAME, searchDocumentResult.path);
        Assert.assertNotNull(searchDocumentResult.content);
        Assert.assertEquals("{\"taggedValues\":{},\"package\":\"model\",\"name\":\"Person\",\"description\":\"\",\"projectCoordinates\":{\"versionId\":\"0.0.1-SNAPSHOT\",\"groupId\":\"org.finos.test\",\"artifactId\":\"test-project\"},\"id\":\"model::Person\",\"type\":\"Class\",\"properties\":[{\"taggedValues\":{},\"name\":\"firstName\"},{\"taggedValues\":{},\"name\":\"lastName\"}]}",searchDocumentResult.content);
    }
}
