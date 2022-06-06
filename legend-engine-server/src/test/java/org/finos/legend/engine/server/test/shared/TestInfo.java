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

package org.finos.legend.engine.server.test.shared;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.engine.server.core.api.Info;
import org.finos.legend.engine.server.core.configuration.DeploymentConfiguration;
import org.finos.legend.engine.server.core.configuration.OpenTracingConfiguration;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.deployment.DeploymentVersionInfo;
import org.junit.Assert;
import org.junit.Test;

public class TestInfo
{

    @Test
    public void testExtraInformationValidJson()
    {
        DeploymentConfiguration config = new DeploymentConfiguration();
        config.mode = DeploymentMode.TEST;
        MutableMap<String, DeploymentVersionInfo> addedInfo = Maps.mutable.empty();
        DeploymentVersionInfo v = new DeploymentVersionInfo();
        v.branch = "testBranch";
        v.buildTag = "testTag";
        DeploymentVersionInfo v2 = new DeploymentVersionInfo();
        v2.branch = "mainBranch";
        v2.commitId = "jhsah676jhh";
        addedInfo.put("server1", v);
        addedInfo.put("server2", v2);
        Info info = new Info(config, new OpenTracingConfiguration(), addedInfo);
        String message = info.executePureGet().getEntity().toString();
        Assert.assertTrue("Json not valid", message.contains(",\"server1\":{\"git.branch\":\"testBranch\",\"git.closest.tag.name\":\"testTag\"},\"server2\":{\"git.branch\":\"mainBranch\",\"git.commit.id\":\"jhsah676jhh\"}")
                || message.contains(",\"server2\":{\"git.branch\":\"mainBranch\",\"git.commit.id\":\"jhsah676jhh\"},\"server1\":{\"git.branch\":\"testBranch\",\"git.closest.tag.name\":\"testTag\"}"));
    }
}
