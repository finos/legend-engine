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

import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.authentication.credentialprovider.CredentialProvider;
import org.finos.legend.authentication.credentialprovider.CredentialProviderProvider;
import org.finos.legend.authentication.credentialprovider.impl.ApiTokenCredentialProvider;
import org.finos.legend.authentication.credentialprovider.impl.UserPasswordCredentialProvider;
import org.finos.legend.authentication.intermediationrule.IntermediationRule;
import org.finos.legend.authentication.intermediationrule.IntermediationRuleProvider;
import org.finos.legend.authentication.testrules.CannedApiKeyRuleForTesting;
import org.finos.legend.authentication.testrules.CannedUserPasswordRuleForTesting;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.ApiTokenAuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.UserPasswordAuthenticationSpecification;
import org.finos.legend.engine.shared.core.identity.credential.AnonymousCredential;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestCredentialProviderProvider
{
    /*
    Setup
         CredentialProviderProvider is given two unconfigured providers :  ApiTokenCredentialProvider, UserPasswordCredentialProvider
         IntermediationRuleProvider provides a rule that matches only ApiTokenCredentialProvider
    Behavior
         CredentialProviderProvider is configured only with the ApiTokenCredentialProvider that has a matching rule
     */
    @Test
    public void providerWiring_testcase1()
    {
        ApiTokenCredentialProvider apiTokenCredentialProvider = new ApiTokenCredentialProvider();
        UserPasswordCredentialProvider userPasswordCredentialProvider = new UserPasswordCredentialProvider();
        FastList<CredentialProvider> credentialProviders = FastList.newListWith(apiTokenCredentialProvider, userPasswordCredentialProvider);

        CannedApiKeyRuleForTesting apiKeyRule = new CannedApiKeyRuleForTesting(null);
        IntermediationRuleProvider intermediationRuleProvider = new IntermediationRuleProvider(FastList.newListWith(apiKeyRule));

        CredentialProviderProvider credentialProviderProvider = new CredentialProviderProvider(credentialProviders, intermediationRuleProvider);

        FastList<CredentialProvider> configuredCredentialProviders = credentialProviderProvider.getConfiguredCredentialProviders();
        assertEquals(1, configuredCredentialProviders.size());
        assertTrue(configuredCredentialProviders.contains(apiTokenCredentialProvider));
        assertTrue(apiTokenCredentialProvider.getIntermediationRules().contains(apiKeyRule));
    }

    /*
    Setup
         CredentialProviderProvider is given two unconfigured providers :  ApiTokenCredentialProvider, UserPasswordCredentialProvider
         IntermediationRuleProvider provides a rule that matches  ApiTokenCredentialProvider and UserPasswordCredentialProvider
    Behavior
         CredentialProviderProvider is configured with both the providers and the appropriate matching rules
     */
    @Test
    public void providerWiring_testcase2()
    {
        ApiTokenCredentialProvider apiTokenCredentialProvider = new ApiTokenCredentialProvider();
        UserPasswordCredentialProvider userPasswordCredentialProvider = new UserPasswordCredentialProvider();
        FastList<CredentialProvider> credentialProviders = FastList.newListWith(apiTokenCredentialProvider, userPasswordCredentialProvider);

        CannedApiKeyRuleForTesting apiKeyRule = new CannedApiKeyRuleForTesting(null);
        CannedUserPasswordRuleForTesting userPasswordRule = new CannedUserPasswordRuleForTesting(null);
        IntermediationRuleProvider intermediationRuleProvider = new IntermediationRuleProvider(FastList.newListWith(apiKeyRule, userPasswordRule));

        CredentialProviderProvider credentialProviderProvider = new CredentialProviderProvider(credentialProviders, intermediationRuleProvider);

        FastList<CredentialProvider> configuredCredentialProviders = credentialProviderProvider.getConfiguredCredentialProviders();
        assertEquals(2, configuredCredentialProviders.size());

        assertTrue(configuredCredentialProviders.contains(apiTokenCredentialProvider));
        assertTrue(apiTokenCredentialProvider.getIntermediationRules().contains(apiKeyRule));

        assertTrue(configuredCredentialProviders.contains(userPasswordCredentialProvider));
        assertTrue(userPasswordCredentialProvider.getIntermediationRules().contains(userPasswordRule));
    }

    @Test
    public void explain()
    {
        ApiTokenCredentialProvider apiTokenCredentialProvider = new ApiTokenCredentialProvider();
        UserPasswordCredentialProvider userPasswordCredentialProvider = new UserPasswordCredentialProvider();
        FastList<CredentialProvider> credentialProviders = FastList.newListWith(apiTokenCredentialProvider, userPasswordCredentialProvider);

        CannedApiKeyRuleForTesting apiKeyRule = new CannedApiKeyRuleForTesting(null);
        CannedUserPasswordRuleForTesting userPasswordRule = new CannedUserPasswordRuleForTesting(null);
        IntermediationRuleProvider intermediationRuleProvider = new IntermediationRuleProvider(FastList.newListWith(apiKeyRule, userPasswordRule));

        CredentialProviderProvider credentialProviderProvider = new CredentialProviderProvider(credentialProviders, intermediationRuleProvider);
        String explain = credentialProviderProvider.explain();

        String expected =
                "CredentialProvider : org.finos.legend.authentication.credentialprovider.impl.ApiTokenCredentialProvider\n" +
                "\tRule   : org.finos.legend.authentication.testrules.CannedApiKeyRuleForTesting\n" +
                "\t\tSpec   : org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.ApiTokenAuthenticationSpecification\n" +
                "\t\tInput  : org.finos.legend.engine.shared.core.identity.credential.AnonymousCredential\n" +
                "\t\tOutput : org.finos.legend.engine.shared.core.identity.credential.ApiTokenCredential\n" +
                "CredentialProvider : org.finos.legend.authentication.credentialprovider.impl.UserPasswordCredentialProvider\n" +
                "\tRule   : org.finos.legend.authentication.testrules.CannedUserPasswordRuleForTesting\n" +
                "\t\tSpec   : org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.UserPasswordAuthenticationSpecification\n" +
                "\t\tInput  : org.finos.legend.engine.shared.core.identity.credential.AnonymousCredential\n" +
                "\t\tOutput : org.finos.legend.engine.shared.core.identity.credential.PlaintextUserPasswordCredential\n";

        assertEquals(expected, explain);
    }

    @Test
    public void testMatching()
    {
        ApiTokenCredentialProvider apikeyCredentialProvider = new ApiTokenCredentialProvider();
        UserPasswordCredentialProvider userPasswordCredentialProvider = new UserPasswordCredentialProvider();
        FastList<CredentialProvider> credentialProviders = FastList.newListWith(apikeyCredentialProvider, userPasswordCredentialProvider);

        CannedApiKeyRuleForTesting apiKeyRule = new CannedApiKeyRuleForTesting(null);
        CannedUserPasswordRuleForTesting userPasswordRule = new CannedUserPasswordRuleForTesting(null);
        IntermediationRuleProvider intermediationRuleProvider = new IntermediationRuleProvider(FastList.newListWith(apiKeyRule, userPasswordRule));

        CredentialProviderProvider credentialProviderProvider = new CredentialProviderProvider(credentialProviders, intermediationRuleProvider);

        Optional<CredentialProvider> matchingApiKeyCredentialProvider = credentialProviderProvider.findMatchingCredentialProvider(ApiTokenAuthenticationSpecification.class, Sets.immutable.of(AnonymousCredential.class));
        assertTrue(apikeyCredentialProvider.equals(matchingApiKeyCredentialProvider.get()));

        Optional<CredentialProvider> matchingUserPasswordCredentialProvider = credentialProviderProvider.findMatchingCredentialProvider(UserPasswordAuthenticationSpecification.class, Sets.immutable.of(AnonymousCredential.class));
        assertTrue(userPasswordCredentialProvider.equals(matchingUserPasswordCredentialProvider.get()));
    }

    @Test
    public void testWithoutIntermediationRuleProvider()
    {
        ApiTokenCredentialProvider apiTokenCredentialProvider = new ApiTokenCredentialProvider(
                Lists.immutable.<IntermediationRule>of(
                        new CannedApiKeyRuleForTesting(null)
                ).castToList());

        UserPasswordCredentialProvider userPasswordCredentialProvider = new UserPasswordCredentialProvider(
                Lists.immutable.<IntermediationRule>of(
                        new CannedUserPasswordRuleForTesting(null)
                ).castToList());

        CredentialProviderProvider credentialProviderProvider = CredentialProviderProvider.builder()
                .with(userPasswordCredentialProvider)
                .with(apiTokenCredentialProvider)
                .build();

        String explain = credentialProviderProvider.explain();
        String expected = "CredentialProvider : org.finos.legend.authentication.credentialprovider.impl.UserPasswordCredentialProvider\n" +
                "\tRule   : org.finos.legend.authentication.testrules.CannedUserPasswordRuleForTesting\n" +
                "\t\tSpec   : org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.UserPasswordAuthenticationSpecification\n" +
                "\t\tInput  : org.finos.legend.engine.shared.core.identity.credential.AnonymousCredential\n" +
                "\t\tOutput : org.finos.legend.engine.shared.core.identity.credential.PlaintextUserPasswordCredential\n" +
                "CredentialProvider : org.finos.legend.authentication.credentialprovider.impl.ApiTokenCredentialProvider\n" +
                "\tRule   : org.finos.legend.authentication.testrules.CannedApiKeyRuleForTesting\n" +
                "\t\tSpec   : org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.ApiTokenAuthenticationSpecification\n" +
                "\t\tInput  : org.finos.legend.engine.shared.core.identity.credential.AnonymousCredential\n" +
                "\t\tOutput : org.finos.legend.engine.shared.core.identity.credential.ApiTokenCredential\n";
        assertEquals(expected, explain);

        Optional<CredentialProvider> matchingApiKeyCredentialProvider = credentialProviderProvider.findMatchingCredentialProvider(ApiTokenAuthenticationSpecification.class, Sets.immutable.of(AnonymousCredential.class));
        assertTrue(apiTokenCredentialProvider.equals(matchingApiKeyCredentialProvider.get()));

        Optional<CredentialProvider> matchingUserPasswordCredentialProvider = credentialProviderProvider.findMatchingCredentialProvider(UserPasswordAuthenticationSpecification.class, Sets.immutable.of(AnonymousCredential.class));
        assertTrue(userPasswordCredentialProvider.equals(matchingUserPasswordCredentialProvider.get()));
    }
}
