package org.finos.legend.engine.plan.execution.authorization;

import org.apache.hadoop.conf.Configuration;
import org.apache.ranger.authorization.hadoop.config.RangerPluginConfig;
import org.apache.ranger.plugin.policyengine.RangerAccessRequest;
import org.apache.ranger.plugin.policyengine.RangerAccessRequestImpl;
import org.apache.ranger.plugin.policyengine.RangerAccessResourceImpl;
import org.apache.ranger.plugin.policyengine.RangerAccessResult;
import org.apache.ranger.plugin.policyengine.RangerPolicyEngineOptions;
import org.apache.ranger.plugin.service.RangerBasePlugin;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestRangerPluginApi
{
    private final RangerBasePlugin rangerBasePlugin;

    public TestRangerPluginApi()
    {
        RangerPolicyEngineOptions policyEngineOptions = new RangerPolicyEngineOptions();
        policyEngineOptions.disablePolicyRefresher = false;

        RangerPluginConfig rangerPluginConfig = new RangerPluginConfig(
                "legend",
                "legend",
                "someAppId",
                "someCluster",
                "someClusterType",
                policyEngineOptions
        );

        this.rangerBasePlugin = new RangerBasePlugin(rangerPluginConfig);
        RangerAdminClientImpl adminClient = new RangerAdminClientImpl();
        adminClient.init("legend", "app", "unused", new Configuration());
        this.rangerBasePlugin.getPluginContext().setAdminClient(adminClient);
        this.rangerBasePlugin.init();
    }

    @Test
    public void testApi()
    {
        RangerAccessResourceImpl request1Resource = new RangerAccessResourceImpl();
        request1Resource.setValue("service-guid", "legend-service-path1");
        RangerAccessRequest request1 = new RangerAccessRequestImpl(request1Resource, "execute_service", "alice", Collections.emptySet(), Collections.emptySet());
        RangerAccessResult result1 = rangerBasePlugin.isAccessAllowed(request1);
        assertTrue(result1.getIsAllowed());

        // happy paths allowed by policy 1
        this.evalPolicy("alice", "legend-service-path1", "execute_service", true,1L);
        this.evalPolicy("bob", "legend-service-path1", "execute_service", true,1L);
        this.evalPolicy("dave", "legend-service-path1", "execute_service", true,1L);

        // happy paths allowed by policy 2
        this.evalPolicy("fred", "legend-service-path2", "execute_service", true,2L);

        // paths not allowed by either policy 1 or policy 2

        this.evalPolicy("someuser", "someresource", "execute_service", false,-1L);

    }

    public void evalPolicy(String user, String resource, String accessType, boolean allowed, Long policyId)
    {
        RangerAccessResourceImpl requestResource = new RangerAccessResourceImpl();
        requestResource.setValue("service-guid", resource);
        RangerAccessRequest request = new RangerAccessRequestImpl(requestResource, accessType, user, Collections.emptySet(), Collections.emptySet());
        RangerAccessResult result = rangerBasePlugin.isAccessAllowed(request);
        assertEquals("mismatch in authz result", allowed, result.getIsAllowed());
        assertEquals("mismatch in policy id", policyId, (Long)result.getPolicyId());
    }
}
