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

package org.finos.legend.authentication;

import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.authentication.credentialprovider.impl.ApikeyCredentialProvider;
import org.finos.legend.authentication.credentialprovider.impl.UserPasswordCredentialProvider;
import org.finos.legend.authentication.testrules.CannedUserPasswordRuleForTesting;
import org.finos.legend.authentication.testrules.CannedUserPasswordRuleWithKerberosForTesting;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.ApiKeyAuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.UserPasswordAuthenticationSpecification;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.AnonymousCredential;
import org.finos.legend.engine.shared.core.identity.credential.PlaintextUserPasswordCredential;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TestCredentialProviderEdgeCases
{
    private Identity identity;

    @Before
    public void setup()
    {
        this.identity = new Identity("alice", new AnonymousCredential());
    }

    @Test
    public void testProviderWithoutAnyRules() throws Exception
    {
        ApikeyCredentialProvider credentialProvider = new ApikeyCredentialProvider();
        try
        {
            credentialProvider.makeCredential(new ApiKeyAuthenticationSpecification(), identity);
            fail("failed to produce exception");
        }
        catch (UnsupportedOperationException e)
        {
            String expected = "Cannot make credential for configuration of type 'class org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.ApiKeyAuthenticationSpecification'. No intermediation rules have been configured";
            assertEquals(expected, e.getMessage());
        }
    }

    @Test
    public void testProviderWithRuleThatDoesNotMatchSpecificationType() throws Exception
    {
        ApikeyCredentialProvider credentialProvider = new ApikeyCredentialProvider();
        credentialProvider.configureWithRules(FastList.newListWith(new CannedUserPasswordRuleForTesting(null)));
        try
        {
            credentialProvider.makeCredential(new ApiKeyAuthenticationSpecification(), identity);
            fail("failed to produce exception");
        }
        catch (UnsupportedOperationException e)
        {
            String expected = "Cannot make credential. No intermediation rule that matches configuration type 'class org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.ApiKeyAuthenticationSpecification' and one of these input credential types : [class org.finos.legend.engine.shared.core.identity.credential.AnonymousCredential]";
            assertEquals(expected, e.getMessage());
        }
    }

    @Test
    public void testProviderWithRuleThatDoesNotMatchInputCredentialType() throws Exception
    {
        UserPasswordCredentialProvider credentialProvider = new UserPasswordCredentialProvider();
        credentialProvider.configureWithRules(FastList.newListWith(new CannedUserPasswordRuleWithKerberosForTesting(null)));
        try
        {
            credentialProvider.makeCredential(new UserPasswordAuthenticationSpecification(), identity);
            fail("failed to produce exception");
        }
        catch (UnsupportedOperationException e)
        {
            String expected = "Cannot make credential. No intermediation rule that matches configuration type 'class org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.UserPasswordAuthenticationSpecification' and one of these input credential types : [class org.finos.legend.engine.shared.core.identity.credential.AnonymousCredential]";
            assertEquals(expected, e.getMessage());
        }
    }

    @Test
    public void testRulesProcessedInOrder() throws Exception
    {
        UserPasswordCredentialProvider credentialProvider = new UserPasswordCredentialProvider();
        CannedUserPasswordRuleForTesting rule1 = new CannedUserPasswordRuleForTesting("user1", "password1");
        CannedUserPasswordRuleForTesting rule2 = new CannedUserPasswordRuleForTesting("user2", "password2");
        credentialProvider.configureWithRules(FastList.newListWith(rule1, rule2));

        PlaintextUserPasswordCredential credential = credentialProvider.makeCredential(new UserPasswordAuthenticationSpecification(), identity);

        assertEquals("user1", credential.getUser());
        assertEquals("password1", credential.getPassword());
    }
}
