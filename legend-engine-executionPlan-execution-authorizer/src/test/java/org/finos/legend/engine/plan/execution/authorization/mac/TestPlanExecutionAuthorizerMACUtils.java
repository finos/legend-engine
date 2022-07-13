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

package org.finos.legend.engine.plan.execution.authorization.mac;

import org.finos.legend.engine.shared.core.vault.TestVaultImplementation;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestPlanExecutionAuthorizerMACUtils
{

    private TestVaultImplementation vaultImplementation;

    @Before
    public void setup()
    {
        this.vaultImplementation = new TestVaultImplementation();
        Vault.INSTANCE.registerImplementation(vaultImplementation);
    }

    @After
    public void shutdown()
    {
        if (this.vaultImplementation != null)
        {
            Vault.INSTANCE.unregisterImplementation(this.vaultImplementation);
        }
    }

    @Test
    public void happyPath() throws Exception
    {
        this.vaultImplementation.setValue("keyref1", this.generateKey());

        String generatedMAC = new PlanExecutionAuthorizerMACUtils().generateMAC("hello world", "keyref1");
        PlanExecutionAuthorizerMACUtils.MACValidationResult result = new PlanExecutionAuthorizerMACUtils().isValidMAC("hello world", generatedMAC, "keyref1");
        assertTrue("Mac failed verification", result.isValidMAC());
    }

    @Test
    public void tamperedMessage() throws Exception
    {
        this.vaultImplementation.setValue("keyref1", this.generateKey());

        String generatedMac = new PlanExecutionAuthorizerMACUtils().generateMAC("hello world", "keyref1");
        PlanExecutionAuthorizerMACUtils.MACValidationResult result = new PlanExecutionAuthorizerMACUtils().isValidMAC("hello world tampered", generatedMac, "keyref1");
        assertFalse("Invalid Mac failed verification", result.isValidMAC());
    }


    @Test
    public void invalidKey() throws Exception
    {
        this.vaultImplementation.setValue("keyref1", this.generateKey());
        this.vaultImplementation.setValue("keyref2", this.generateKey());

        String generatedMac = new PlanExecutionAuthorizerMACUtils().generateMAC("hello world", "keyref1");
        PlanExecutionAuthorizerMACUtils.MACValidationResult result = new PlanExecutionAuthorizerMACUtils().isValidMAC("hello world", generatedMac, "keyref2");
        assertFalse("Invalid Mac failed verification", result.isValidMAC());
    }

    private String generateKey() throws Exception
    {
        Path pathToKeyFile = Files.createTempDirectory("tempmac").resolve("key");
        new PlanExecutionAuthorizerMACKeyGenerator().generateKey(pathToKeyFile);
        return new String(Files.readAllBytes(pathToKeyFile), StandardCharsets.UTF_8);
    }
}