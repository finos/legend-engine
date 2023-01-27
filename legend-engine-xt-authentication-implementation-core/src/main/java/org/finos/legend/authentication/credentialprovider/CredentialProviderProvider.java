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

package org.finos.legend.authentication.credentialprovider;

import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.authentication.intermediationrule.IntermediationRule;
import org.finos.legend.authentication.intermediationrule.IntermediationRuleProvider;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.AuthenticationSpecification;
import org.finos.legend.engine.shared.core.identity.Credential;

import java.util.List;
import java.util.Optional;

/*
    A CredentialProviderProvider provides CredentialProviders.
    It loads CredentialProviders from the classpath.
    It loads IntermediationRules from an IntermediationRuleProvider.
    Finally, it configures CredentialProviders with matching IntermediationRules.
 */
public class CredentialProviderProvider
{
    private FastList<CredentialProvider> credentialProviders = FastList.newList();
    private IntermediationRuleProvider intermediationRuleProvider;

    public CredentialProviderProvider(IntermediationRuleProvider intermediationRuleProvider)
    {
        // TODO - Load credential providers from the classpath
        throw new UnsupportedOperationException("Load credential providers from the classpath");
    }

    public CredentialProviderProvider(List<CredentialProvider> credentialProviders, IntermediationRuleProvider intermediationRuleProvider)
    {
        this.intermediationRuleProvider = intermediationRuleProvider;
        this.configureCredentialProvidersWithRules(credentialProviders);
    }

    private void configureCredentialProvidersWithRules(List<CredentialProvider> credentialProviders)
    {
        for (CredentialProvider credentialProvider : credentialProviders)
        {
            this.configureProvider(credentialProvider);
        }
    }

    private void configureProvider(CredentialProvider credentialProvider)
    {
        if (this.intermediationRuleProvider != null)
        {
            this.configureProviderWithExternalRules(credentialProvider);
        }
        else
        {
            this.credentialProviders.add(credentialProvider);
        }
    }

    private void configureProviderWithExternalRules(CredentialProvider credentialProvider)
    {
        FastList<IntermediationRule> rules = this.intermediationRuleProvider.getRules();
        Class<? extends AuthenticationSpecification> authenticationSpecificationType = credentialProvider.getAuthenticationSpecificationType();
        Class<? extends Credential> outputCredentialType = credentialProvider.getOutputCredentialType();

        Predicate<IntermediationRule> predicate = rule -> rule.consumesAuthenticationSpecification(authenticationSpecificationType) && rule.producesOutputCredential(outputCredentialType);
        FastList<IntermediationRule> matchingRules = rules.select(predicate);
        if (!matchingRules.isEmpty())
        {
            credentialProvider.configureWithRules(matchingRules);
            this.credentialProviders.add(credentialProvider);
        }
    }

    public Optional<CredentialProvider> findMatchingCredentialProvider(Class<? extends AuthenticationSpecification> AuthenticationSpecificationType, ImmutableSet<? extends Class<? extends Credential>> inputCredentialTypes)
    {
        Predicate<CredentialProvider> predicate =
                        credentialProvider ->
                                credentialProvider.consumesAuthenticationSpecification(AuthenticationSpecificationType) &&
                                credentialProvider.hasRuleThatConsumesInputCredentials(inputCredentialTypes);

        FastList<CredentialProvider> matchingProviders = this.credentialProviders.select(predicate);
        if (matchingProviders.isEmpty())
        {
            return Optional.empty();
        }
        return Optional.of(matchingProviders.get(0));
    }

    public FastList<CredentialProvider> getConfiguredCredentialProviders()
    {
        return credentialProviders;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static class Builder
    {
        private IntermediationRuleProvider intermediationRuleProvider = null;

        private MutableList<CredentialProvider> credentialProviders = Lists.mutable.empty();

        public Builder with(IntermediationRuleProvider intermediationRuleProvider)
        {
            this.intermediationRuleProvider = intermediationRuleProvider;
            return this;
        }

        public Builder with(List<CredentialProvider> credentialProviders)
        {
            this.credentialProviders.addAll(credentialProviders);
            return this;
        }

        public Builder with(CredentialProvider credentialProvider)
        {
            this.credentialProviders.add(credentialProvider);
            return this;
        }

        public CredentialProviderProvider build()
        {
            return new CredentialProviderProvider(credentialProviders, intermediationRuleProvider);
        }
    }

    /*
        Generates a human readable 'explanation' of the configured providers and their rules.
        Note : This is an internal/debug API.
     */
    public String explain()
    {
        StringBuilder stringBuilder = new StringBuilder();
        for (CredentialProvider credentialProvider : this.credentialProviders)
        {
            stringBuilder.append("CredentialProvider : " + credentialProvider.getClass().getCanonicalName() + "\n");
            FastList<IntermediationRule> intermediationRules = credentialProvider.getIntermediationRules();
            for (IntermediationRule rule : intermediationRules)
            {
                stringBuilder.append("\tRule   : " + rule.getClass().getCanonicalName() + "\n");
                stringBuilder.append("\t\tSpec   : " + rule.getAuthenticationSpecificationType().getCanonicalName() + "\n");
                stringBuilder.append("\t\tInput  : " + rule.getInputCredentialType().getCanonicalName() + "\n");
                stringBuilder.append("\t\tOutput : " + rule.getOutputCredentialType().getCanonicalName() + "\n");
            }
        }
        return stringBuilder.toString();
    }

}
