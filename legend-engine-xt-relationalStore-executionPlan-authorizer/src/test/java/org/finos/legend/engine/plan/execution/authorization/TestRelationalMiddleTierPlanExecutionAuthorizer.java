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

package org.finos.legend.engine.plan.execution.authorization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.middletier.MiddleTierUserPasswordCredential;
import org.finos.legend.engine.shared.core.vault.TestVaultImplementation;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.finos.legend.engine.plan.execution.authorization.PlanExecutionAuthorizerInput.ExecutionMode.INTERACTIVE_EXECUTION;
import static org.finos.legend.engine.plan.execution.authorization.PlanExecutionAuthorizerInput.ExecutionMode.SERVICE_EXECUTION;
import static org.junit.Assert.assertEquals;

public class TestRelationalMiddleTierPlanExecutionAuthorizer
{
    private TestVaultImplementation testVaultImplementation;
    private Identity alice = new Identity("alice");

    @Before
    public void setup() throws Exception
    {
        this.testVaultImplementation = new TestVaultImplementation();
        Vault.INSTANCE.registerImplementation(testVaultImplementation);

        String credentialAsString = new ObjectMapper().writeValueAsString(new MiddleTierUserPasswordCredential("x", "y", "policy1"));
        this.testVaultImplementation.setValue("reference1", credentialAsString);
    }

    @After
    public void shutdown()
    {
        if (this.testVaultImplementation != null)
        {
            Vault.INSTANCE.unregisterImplementation(this.testVaultImplementation);
        }
    }

    @Test
    public void planWithSingleMiddleTierNode_ServiceExecutionDenied() throws Exception
    {
        RootMiddleTierPlanExecutionAuthorizer planExecutionAuthorizer = new RootMiddleTierPlanExecutionAuthorizer(
                Lists.immutable.of(
                        new RelationalMiddleTierPlanExecutionAuthorizer(new SometimesDenyRelationalMiddleTierConnectionCredentialAuthorizer())
                )
        );

        SingleExecutionPlan planWithMiddleTierAuth = this.loadPlanFromFile("/plans/planWithSingleMiddleTierAuthNode.json");

        PlanExecutionAuthorizerInput authorizationInput = PlanExecutionAuthorizerInput.with(SERVICE_EXECUTION)
                .withResourceContext("legend.servicePath", "/api/foobar")
                .withResourceContext("legend.serviceUniqueId", "v1:1234")
                .build();

        PlanExecutionAuthorizerOutput authorizationResult = planExecutionAuthorizer.evaluate(alice, planWithMiddleTierAuth, authorizationInput);
        this.compare(authorizationResult, "/plans/planWithSingleMiddleTierAuthNode_expected_authz_deny_service_execution.json");
    }

    @Test
    public void planWithSingleMiddleTierNode_ServiceExecutionAllowed() throws Exception
    {
        RootMiddleTierPlanExecutionAuthorizer planExecutionAuthorizer = new RootMiddleTierPlanExecutionAuthorizer(
                Lists.immutable.of(new RelationalMiddleTierPlanExecutionAuthorizer(new AlwaysAllowRelationalMiddleTierConnectionCredentialAuthorizer()))
        );

        SingleExecutionPlan planWithMiddleTierAuth = this.loadPlanFromFile("/plans/planWithSingleMiddleTierAuthNode.json");

        PlanExecutionAuthorizerInput authorizationInput = PlanExecutionAuthorizerInput.with(SERVICE_EXECUTION)
                .withResourceContext("legend.servicePath", "/api/foobar")
                .withResourceContext("legend.serviceUniqueId", "v1:1234")
                .build();

        PlanExecutionAuthorizerOutput authorizationResult = planExecutionAuthorizer.evaluate(alice, planWithMiddleTierAuth, authorizationInput);
        this.compare(authorizationResult, "/plans/planWithSingleMiddleTierAuthNode_expected_authz_allow_service_execution.json");
    }

    @Test
    public void planWithMultipleMiddleTierNode_InteractiveExecutionDenied() throws Exception
    {
        RootMiddleTierPlanExecutionAuthorizer planExecutionAuthorizer = new RootMiddleTierPlanExecutionAuthorizer(
                Lists.immutable.of(new RelationalMiddleTierPlanExecutionAuthorizer(new SometimesDenyRelationalMiddleTierConnectionCredentialAuthorizer("DB@name1@host1.com@1234", "DB@name2@host2.com@1235")))
        );

        SingleExecutionPlan planWithMiddleTierAuth = this.loadPlanFromFile("/plans/planWithMultipleMiddleTierAuthNodes.json");

        PlanExecutionAuthorizerInput authorizationInput = PlanExecutionAuthorizerInput.with(INTERACTIVE_EXECUTION).build();

        PlanExecutionAuthorizerOutput authorizationResult = planExecutionAuthorizer.evaluate(alice, planWithMiddleTierAuth, authorizationInput);
        this.compare(authorizationResult, "/plans/planWithMultipleMiddleTierAuthNode_expected_authz_deny_service_execution.json");
    }

    @Test
    public void planWithMultipleMiddleTierNode_InteractiveExecutionAllowed() throws Exception
    {
        RootMiddleTierPlanExecutionAuthorizer planExecutionAuthorizer = new RootMiddleTierPlanExecutionAuthorizer(
                Lists.immutable.of(new RelationalMiddleTierPlanExecutionAuthorizer(new AlwaysAllowRelationalMiddleTierConnectionCredentialAuthorizer()))
        );

        SingleExecutionPlan planWithMiddleTierAuth = this.loadPlanFromFile("/plans/planWithMultipleMiddleTierAuthNodes.json");

        PlanExecutionAuthorizerInput authorizationInput = PlanExecutionAuthorizerInput.with(INTERACTIVE_EXECUTION).build();

        PlanExecutionAuthorizerOutput authorizationResult = planExecutionAuthorizer.evaluate(alice, planWithMiddleTierAuth, authorizationInput);
        this.compare(authorizationResult, "/plans/planWithMultipleMiddleTierAuthNode_expected_authz_allow_service_execution.json");
    }

    private void compare(PlanExecutionAuthorizerOutput authorization, String expectedResultResourceName) throws Exception
    {
        ObjectMapper objectMapper = new ObjectMapper();
        String actualResultJSON = authorization.toPrettyJSON();
        String expectedResultJSON = this.loadFromFile(expectedResultResourceName);

        JsonNode expectedTree = objectMapper.readTree(expectedResultJSON);
        JsonNode actualTree = objectMapper.readTree(actualResultJSON);
        System.out.println(actualResultJSON);;
        assertEquals(expectedTree, actualTree);
    }

    private SingleExecutionPlan loadPlanFromFile(String planResourceName) throws IOException, URISyntaxException
    {
        String planJson = this.loadFromFile(planResourceName);
        SingleExecutionPlan plan = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().readValue(planJson, SingleExecutionPlan.class);
        return plan;
    }

    private String loadFromFile(String resourceName) throws IOException, URISyntaxException
    {
        URL resource = TestRelationalMiddleTierPlanExecutionAuthorizer.class.getResource(resourceName);
        return new String(Files.readAllBytes(Paths.get(resource.toURI())), StandardCharsets.UTF_8).trim();
    }

    static class SometimesDenyRelationalMiddleTierConnectionCredentialAuthorizer implements RelationalMiddleTierConnectionCredentialAuthorizer
    {
        private ImmutableList<String> resourcesToDeny;

        public SometimesDenyRelationalMiddleTierConnectionCredentialAuthorizer(String... resourcesToDeny)
        {
            this.resourcesToDeny = Lists.immutable.of(resourcesToDeny);
        }

        @Override
        public CredentialAuthorization evaluate(Identity currentUser, String credentialVaultReference, PlanExecutionAuthorizerInput.ExecutionMode usageContext, String resourceContext, String policyContext) throws Exception
        {
            ImmutableMap<String, String> details = Maps.immutable.of("foo", "foo1", "bar", "bar1");

            if (this.resourcesToDeny.isEmpty() || this.resourcesToDeny.contains(resourceContext))
            {
                return CredentialAuthorization.deny(currentUser.getName(), credentialVaultReference, Lists.immutable.with(details));
            }
            else
            {
                return CredentialAuthorization.allow(currentUser.getName(), credentialVaultReference, Lists.immutable.with(details));
            }
        }
    }

    static class AlwaysAllowRelationalMiddleTierConnectionCredentialAuthorizer implements RelationalMiddleTierConnectionCredentialAuthorizer
    {
        @Override
        public CredentialAuthorization evaluate(Identity currentUser, String credentialVaultReference, PlanExecutionAuthorizerInput.ExecutionMode usageContext, String resourceContext, String policyContext) throws Exception
        {
            ImmutableMap<String, String> details = Maps.immutable.of("foo", "foo1", "bar", "bar1");

            return CredentialAuthorization.allow(currentUser.getName(), credentialVaultReference, Lists.immutable.with(details));
        }
    }
}