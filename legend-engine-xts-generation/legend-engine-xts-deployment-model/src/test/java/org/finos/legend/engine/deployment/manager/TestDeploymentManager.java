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

import org.finos.legend.engine.deployment.model.DeploymentResponse;
import org.finos.legend.engine.deployment.model.DeploymentStatus;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.identity.Identity;

import org.junit.Test;
import org.junit.Assert;

import java.util.List;


public class TestDeploymentManager
{

    @Test
    public void simpleTest()
    {
        String models = "Class test::A {\n" +
                "  prop1: String[1];\n" +
                "}\n" +
                "\n";

        PureModelContextData modelData = PureGrammarParser.newInstance().parseModel(models);
        PureModel pureModel = org.finos.legend.engine.language.pure.compiler.Compiler.compile(modelData, DeploymentMode.TEST, Identity.getAnonymousIdentity().getName());
        Assert.assertEquals(1, DeploymentManager.extensions().size());
        DeploymentManager manager = new DeploymentManager(modelData, pureModel, modelData.getElements());
        List<DeploymentResponse> validateResponses = manager.validate();
        List<DeploymentResponse> deployResponses = manager.deploy();
        Assert.assertEquals(1, validateResponses.size());
        Assert.assertEquals(1, deployResponses.size());

        DeploymentResponse validateResponse = validateResponses.get(0);
        Assert.assertEquals(validateResponse.status, DeploymentStatus.SUCCESS);

        DeploymentResponse deploymentResponse = deployResponses.get(0);
        Assert.assertEquals(deploymentResponse.status, DeploymentStatus.ERROR);
    }

}
