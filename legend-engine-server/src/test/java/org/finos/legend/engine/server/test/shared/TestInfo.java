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

public class TestInfo {

    @Test
    public void testExtraInformationValidJson()
    {
        DeploymentConfiguration config = new DeploymentConfiguration();
        config.mode = DeploymentMode.TEST;
        MutableMap<String, DeploymentVersionInfo> addedInfo = Maps.mutable.empty();
        DeploymentVersionInfo v = new DeploymentVersionInfo();
        v.branch = "testBranch"; v.buildTag="testTag";
        DeploymentVersionInfo v2 = new DeploymentVersionInfo();
        v2.branch = "mainBranch"; v2.commitId="jhsah676jhh";
        addedInfo.put("server1", v);
        addedInfo.put("server2", v2);
        Info info = new Info(config, new OpenTracingConfiguration(), addedInfo);
        Assert.assertTrue("Json not valid", info.getMessage().contains("\"server1\":{\"git.branch\":\"testBranch\",\"git.closest.tag.name\":\"testTag\"},\"server2\":{\"git.branch\":\"mainBranch\",\"git.commit.id\":\"jhsah676jhh\"}")
                || info.getMessage().contains(",\"server2\":{\"git.branch\":\"mainBranch\",\"git.commit.id\":\"jhsah676jhh\"},\"server1\":{\"git.branch\":\"testBranch\",\"git.closest.tag.name\":\"testTag\"}"));
    }
}
