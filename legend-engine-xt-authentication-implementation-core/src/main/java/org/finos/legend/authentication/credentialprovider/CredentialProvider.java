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
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.authentication.intermediationrule.IntermediationRule;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.AuthenticationSpecification;
import org.finos.legend.engine.shared.core.identity.Credential;
import org.finos.legend.engine.shared.core.identity.Identity;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;

/*
    A CredentialProvider provides a Credential.

    It accepts as inputs
    - AuthenticationSpecification that provides configuration data that can be used during credential creation.
    - Identity of the current runtime user. The user's credentials can be used to perform further authentication with other systems if needed.

    A CredentialProvider is organized as a collection of IntermediationRules.
    An IntermediationRule is the construct that actually acquires a Credential.

    The IntermediationRule construct allows us to customize the Credential acquisition behavior.
    For e.g an OAuthCredential can be acquired in different ways by injecting different rules.
 */

public abstract class CredentialProvider<SPEC extends AuthenticationSpecification, CRED extends Credential>
{
    private final Type[] types;
    protected FastList<IntermediationRule> intermediationRules = FastList.newList();

    public CredentialProvider()
    {
        Type genericSuperClass = this.getClass().getGenericSuperclass();
        ParameterizedType parameterizedType = (ParameterizedType) genericSuperClass;
        this.types = parameterizedType.getActualTypeArguments();
    }

    public CredentialProvider(List<IntermediationRule> intermediationRules)
    {
        this();
        this.intermediationRules.addAll(intermediationRules);
    }

    /*
        spec - AuthenticationSpecification that provides configuration to be used for creating the credential
        identity - Identity of the runtime user
     */
    public abstract CRED makeCredential(SPEC specification, Identity identity) throws Exception;

    public CredentialProvider configureWithRules(List<IntermediationRule> intermediationRules)
    {
        this.intermediationRules.addAll(intermediationRules);
        return this;
    }

    public Class<? extends AuthenticationSpecification> getAuthenticationSpecificationType()
    {
        return (Class<? extends AuthenticationSpecification>) actualTypeArguments()[0];
    }

    public Class<? extends Credential> getOutputCredentialType()
    {
        return (Class<? extends Credential>) actualTypeArguments()[1];
    }

    private Type[] actualTypeArguments()
    {
        return this.types;
    }

    private boolean consumesAuthenticationSpecification(Class<? extends AuthenticationSpecification> authenticationSpecificiationType)
    {
        return this.getAuthenticationSpecificationType().equals(authenticationSpecificiationType);
    }

    private boolean producesOutputCredential(Class<? extends Credential> outputCredential)
    {
        return this.getOutputCredentialType().equals(outputCredential);
    }

    private boolean hasRuleThatConsumesInputCredential(Class<? extends Credential> inputCredential)
    {
        Predicate<IntermediationRule> predicate = rule -> rule.consumesInputCredential(inputCredential);
        return !this.intermediationRules.select(predicate).isEmpty();
    }

    public boolean accepts(Class<? extends AuthenticationSpecification> authenticationSpecificationType, ImmutableSet<? extends Class<? extends Credential>> inputCredentialTypes)
    {
        return this.consumesAuthenticationSpecification(authenticationSpecificationType)
                && inputCredentialTypes.anySatisfy(x -> this.producesOutputCredential(x) || this.hasRuleThatConsumesInputCredential(x));
    }

    /*
        Find a rule that
        - can be configured with the specification AND
        - consumes one of the input credential types AND
        - produces a credential of the output credential type
     */
    protected Optional<IntermediationRule> findMatchingRule(AuthenticationSpecification specification, ImmutableSet<? extends Class<? extends Credential>> inputCredentialTypes, Class<? extends Credential> outputCredentialType)
    {
        for (IntermediationRule intermediationRule : intermediationRules)
        {
            boolean match =
                    intermediationRule.consumesAuthenticationSpecification(specification.getClass()) &&
                    intermediationRule.consumesInputCredentials(inputCredentialTypes) &&
                    intermediationRule.producesOutputCredential(outputCredentialType);
            if (match)
            {
                return Optional.of(intermediationRule);
            }
        }
        return Optional.empty();
    }

    protected CRED makeCredential(AuthenticationSpecification specification, Identity identity, Class<CRED> outputCredentialType) throws Exception
    {
        Optional<CRED> shortCircuitCredential = identity.getCredential(outputCredentialType);

        if (shortCircuitCredential.isPresent())
        {
           return shortCircuitCredential.get();
        }

        if (this.intermediationRules.isEmpty())
        {
            String message = String.format("Cannot make credential for configuration of type '%s'. No intermediation rules have been configured", specification.getClass());
            throw new UnsupportedOperationException(message);
        }

        ImmutableSet<? extends Class<? extends Credential>> identityCredentialTypes = identity.getCredentials().asLazy().collect(Credential::getClass).toSet().toImmutable();

        Optional<IntermediationRule> matchingRuleHolder = this.findMatchingRule(specification, identityCredentialTypes, outputCredentialType);
        if (!matchingRuleHolder.isPresent())
        {
            String message = String.format("Cannot make credential. No intermediation rule that matches configuration type '%s' and one of these input credential types : [%s]",
                    specification.getClass(),
                    identityCredentialTypes.makeString(","));
            throw new UnsupportedOperationException(message);
        }

        IntermediationRule intermediationRule = matchingRuleHolder.get();
        Credential credential = (Credential) identity.getCredential(intermediationRule.getInputCredentialType()).get();
        return outputCredentialType.cast(intermediationRule.makeCredential(specification, credential, identity));
    }

    public FastList<IntermediationRule> getIntermediationRules()
    {
        return intermediationRules;
    }
}
