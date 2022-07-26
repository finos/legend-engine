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

package org.finos.legend.engine.authentication.flows.middletier;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.factory.Maps;
import org.finos.legend.engine.plan.execution.authorization.mac.PlanExecutionAuthorizerMACKeyGenerator;
import org.finos.legend.engine.plan.execution.authorization.mac.PlanExecutionAuthorizerMACUtils;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.MiddleTierUserNamePasswordAuthenticationStrategy;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.middletier.MiddleTierUserPasswordCredential;
import org.finos.legend.engine.shared.core.vault.TestVaultImplementation;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.finos.legend.engine.authentication.DatabaseAuthenticationFlow.RuntimeContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TestMiddleTierUserNamePasswordFlow
{
    private TestVaultImplementation vault;
    private MemSqlStaticWithMiddletierUserNamePasswordAuthenticationFlow flow;

    @Before
    public void setup() throws Exception
    {
        this.vault = new TestVaultImplementation();
        Vault.INSTANCE.registerImplementation(vault);

        Path keyFile = Files.createTempDirectory("temp").resolve("mackey");
        new PlanExecutionAuthorizerMACKeyGenerator().generateKey(keyFile);
        String keyValue = new String(Files.readAllBytes(keyFile), StandardCharsets.UTF_8);

        this.vault.setValue("macKeyReference", keyValue);

        flow = new MemSqlStaticWithMiddletierUserNamePasswordAuthenticationFlow("macKeyReference");
    }

    @After
    public void cleanup()
    {
        Vault.INSTANCE.unregisterImplementation(vault);
    }

    @Test
    public void testUseCredentialWithoutUsageContext() throws Exception
    {
        MiddleTierUserNamePasswordAuthenticationStrategy authenticationStrategy = new MiddleTierUserNamePasswordAuthenticationStrategy("reference1");
        try
        {
            RuntimeContext context = RuntimeContext.newWith(Maps.immutable.with(
                    "legend.resourceContext",
                    "resource1")
            );
            flow.makeCredential(null, null, authenticationStrategy, context);
            fail("failed to throw");
        }
        catch (Exception e)
        {
            assertEquals("Credential acquisition context does not contain a parameter named 'legend.usageContext'. Supplied context values={legend.resourceContext=resource1}", e.getMessage());
        }
    }

    @Test
    public void testUseCredentialWithoutResourceContext() throws Exception
    {
        MiddleTierUserNamePasswordAuthenticationStrategy authenticationStrategy = new MiddleTierUserNamePasswordAuthenticationStrategy("reference1");
        try
        {
            RuntimeContext context = RuntimeContext.newWith(Maps.immutable.with(
                    "legend.usageContext",
                    "SERVICE_EXECUTION")
            );
            flow.makeCredential(null, null, authenticationStrategy, context);
            fail("failed to throw");
        }
        catch (Exception e)
        {
            assertEquals("Credential acquisition context does not contain a parameter named 'legend.resourceContext'. Supplied context values={legend.usageContext=SERVICE_EXECUTION}", e.getMessage());
        }
    }

    @Test
    public void testUseCredentialWithoutMACContext() throws Exception
    {
        MiddleTierUserNamePasswordAuthenticationStrategy authenticationStrategy = new MiddleTierUserNamePasswordAuthenticationStrategy("reference1");
        try
        {
            RuntimeContext context = RuntimeContext.newWith(Maps.immutable.with(
                    "legend.resourceContext",
                    "resource1",
                    "legend.usageContext",
                    "SERVICE_EXECUTION")
            );
            flow.makeCredential(null, null, authenticationStrategy, context);
            fail("failed to throw");
        }
        catch (Exception e)
        {
            assertEquals("Credential acquisition context does not contain a parameter named 'legend.macContext'. Supplied context values={legend.resourceContext=resource1, legend.usageContext=SERVICE_EXECUTION}", e.getMessage());
        }
    }

    @Test
    public void testValidCredentialAcquisition() throws Exception
    {
        this.vault.setValue("reference1", toJSON(new MiddleTierUserPasswordCredential("user", "password", "policy1")));

        MiddleTierUserNamePasswordAuthenticationStrategy authenticationStrategy = new MiddleTierUserNamePasswordAuthenticationStrategy("reference1");

        RuntimeContext context = RuntimeContext.newWith(Maps.immutable
                .with("legend.resourceContext", "resource1",
                        "legend.usageContext", "SERVICE_EXECUTION",
                        "legend.macContext", new PlanExecutionAuthorizerMACUtils().generateMAC("Plan execution authorization completed", "macKeyReference")
                )
        );
        MiddleTierUserPasswordCredential credential = (MiddleTierUserPasswordCredential) flow.makeCredential(new Identity("alice"), null, authenticationStrategy, context);

        assertEquals("user", credential.getUser());
        assertEquals("password", credential.getPassword());
        assertEquals("policy1", credential.getUsagePolicyContext());
    }

    private String toJSON(MiddleTierUserPasswordCredential credential) throws JsonProcessingException
    {
        return new ObjectMapper().writeValueAsString(credential);
    }
}
