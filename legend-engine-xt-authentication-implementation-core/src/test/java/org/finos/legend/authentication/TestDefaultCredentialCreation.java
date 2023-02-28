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

package org.finos.legend.authentication;

import org.finos.legend.authentication.credentialprovider.impl.ApikeyCredentialProvider;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.ApiKeyAuthenticationSpecification;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.ApiTokenCredential;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestDefaultCredentialCreation
{
    private Identity identity;

    @Before
    public void setup()
    {
        this.identity = new Identity("alice", new ApiTokenCredential("value1"));
    }

    @Test
    public void testProviderWithDefaultIncomingCredential() throws Exception
    {
        ApikeyCredentialProvider credentialProvider = new ApikeyCredentialProvider();
        ApiTokenCredential credential = credentialProvider.makeCredential(new ApiKeyAuthenticationSpecification(), identity);

        assertTrue(credential instanceof ApiTokenCredential);
        assertEquals("value1", credential.getApiToken());
    }
}
