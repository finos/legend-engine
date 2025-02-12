// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.deployment.manager;

import org.finos.legend.engine.deployment.model.*;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;

import org.junit.Test;
import org.junit.Assert;

import java.util.List;
import java.util.stream.Collectors;


public class TestDeploymentExtension
{

    @Test
    public void deploymentManagerTest()
    {
        String models = "Class test::A {\n" +
                "  prop1: String[1];\n" +
                "}\n" +
                "Class test::B {}\n";

        PureModelContextData modelData = PureGrammarParser.newInstance().parseModel(models);
        List<DeploymentExtension> extensions = DeploymentExtensionLoader.extensions();
        // any extension should not be added to this core module
        Assert.assertEquals(1, extensions.size());
        DeploymentExtension deploymentExtension = extensions.stream().filter(e -> e.getKey().equals("classDeploymentTest")).findFirst().orElse(null);
        Assert.assertNotNull(deploymentExtension);
        DeploymentManager manager = new DeploymentManager(modelData, modelData.getElements());
        Assert.assertEquals(2, manager.deploy().size());
        Assert.assertEquals(2, manager.validate().size());
        DeploymentResponse validateResponse = manager.validateElement("test::A");
        DeploymentResponse deployResponse = manager.validateElement("test::A");
        Assert.assertEquals(validateResponse.status, DeploymentStatus.SUCCESS);
        Assert.assertEquals(deployResponse.status, DeploymentStatus.SUCCESS);

        DeploymentManager manager2 = new DeploymentManager(modelData, modelData.getElements().stream().filter(e -> e.getPath().equals("test::A")).collect(Collectors.toList()));
        Assert.assertEquals(1, manager2.deploy().size());
        Assert.assertEquals(1, manager2.validate().size());

    }


    @Test
    public void testMetadata()
    {
        List<DeploymentExtensionMetadata> metadata = DeploymentExtensionLoader.getExtensionsMetadata();
        Assert.assertEquals(1, metadata.size());
        DeploymentExtensionMetadata classExtension = metadata.get(0);
        Assert.assertEquals("classDeploymentTest", classExtension.key);
        Assert.assertEquals(1, classExtension.classifierPaths.size());
        Assert.assertEquals("meta::pure::metamodel::type::Class", classExtension.classifierPaths.get(0));
        Assert.assertEquals(1, DeploymentExtensionLoader.getAllSupportedClassifierPaths().size());
        Assert.assertEquals("meta::pure::metamodel::type::Class", DeploymentExtensionLoader.getAllSupportedClassifierPaths().stream().findFirst().get());

    }

}
