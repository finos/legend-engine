// Copyright 2022 Databricks
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

package org.finos.legend.engine.authentication.flows;

import org.finos.legend.engine.authentication.vaults.InMemoryVaultForTesting;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.ApiTokenAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatabricksDatasourceSpecification;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.ApiTokenCredential;
import org.finos.legend.engine.shared.core.identity.factory.IdentityFactoryProvider;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestDatabricksWithApiTokenFlow
{
    private InMemoryVaultForTesting inMemoryVault = new InMemoryVaultForTesting();
    private Identity identity = IdentityFactoryProvider.getInstance().makeIdentityForTesting("identity1");

    @Before
    public void setup()
    {
        Vault.INSTANCE.registerImplementation(inMemoryVault);
    }

    @Test
    public void testFlow() throws Exception
    {
        inMemoryVault.setValue("apiToken", "123456");

        DatabricksDatasourceSpecification datasourceSpec = new DatabricksDatasourceSpecification();
        ApiTokenAuthenticationStrategy authSpec = new ApiTokenAuthenticationStrategy();
        authSpec.apiToken = "apiToken";

        DatabricksWithApiTokenFlow flow = new DatabricksWithApiTokenFlow();
        ApiTokenCredential credential = (ApiTokenCredential) flow.makeCredential(identity, datasourceSpec, authSpec);
        assertEquals("123456", credential.getApiToken());
    }

}