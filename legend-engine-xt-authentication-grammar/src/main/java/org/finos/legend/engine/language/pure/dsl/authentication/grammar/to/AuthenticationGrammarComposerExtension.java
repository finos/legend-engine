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

package org.finos.legend.engine.language.pure.dsl.authentication.grammar.to;

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.ApiKeyAuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.AuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.UserPasswordAuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.CredentialVaultSecret;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.EnvironmentCredentialVaultSecret;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.PropertiesFileSecret;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.SystemPropertiesSecret;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.List;
import java.util.Objects;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabString;

public class AuthenticationGrammarComposerExtension implements IAuthenticationGrammarComposerExtension
{
    @Override
    public List<Function2<AuthenticationSpecification, Integer, String>> getExtraAuthenticationSpecificationComposers()
    {
        return Lists.mutable.with((authenticationSpec, indentLevel) ->
        {
            if (authenticationSpec instanceof ApiKeyAuthenticationSpecification)
            {
                ApiKeyAuthenticationSpecification apiKeyAuthenticationSpecification = (ApiKeyAuthenticationSpecification) authenticationSpec;
                return "ApiKey\n" +
                        getTabString(indentLevel) + "{\n" +
                        getTabString(indentLevel + 1) + "location: '" + apiKeyAuthenticationSpecification.location.name().toLowerCase() + "';\n" +
                        getTabString(indentLevel + 1) + "keyName: '" + apiKeyAuthenticationSpecification.keyName + "';\n" +
                        getTabString(indentLevel + 1) + "value: " + renderCredentialVaultSecret(apiKeyAuthenticationSpecification.value, indentLevel + 1) + "\n" +
                        getTabString(indentLevel) + "}";
            }
            else if (authenticationSpec instanceof UserPasswordAuthenticationSpecification)
            {
                UserPasswordAuthenticationSpecification userPasswordAuthenticationSpecification = (UserPasswordAuthenticationSpecification) authenticationSpec;
                return "UserPassword\n" +
                        getTabString(indentLevel) + "{\n" +
                        getTabString(indentLevel + 1) + "username: '" + userPasswordAuthenticationSpecification.username + "';\n" +
                        getTabString(indentLevel + 1) + "password: " + renderCredentialVaultSecret(userPasswordAuthenticationSpecification.password, indentLevel + 1) + "\n" +
                        getTabString(indentLevel) + "}";
            }
            return null;
        });
    }

    @Override
    public List<Function2<CredentialVaultSecret, Integer, String>> getExtraCredentialVaultSecretComposers()
    {
        return Lists.mutable.with((credentialVaultSecret, indentLevel) ->
        {
            if (credentialVaultSecret instanceof PropertiesFileSecret)
            {
                PropertiesFileSecret propertiesFileSecret = (PropertiesFileSecret) credentialVaultSecret;
                return "PropertiesFileSecret\n" +
                        getTabString(indentLevel) + "{\n" +
                        getTabString(indentLevel + 1) + "propertyName: '" + propertiesFileSecret.propertyName + "';\n" +
                        getTabString(indentLevel) + "}";
            }
            else if (credentialVaultSecret instanceof SystemPropertiesSecret)
            {
                SystemPropertiesSecret systemPropertiesSecret = (SystemPropertiesSecret) credentialVaultSecret;
                return "SystemPropertiesSecret\n" +
                        getTabString(indentLevel) + "{\n" +
                        getTabString(indentLevel + 1) + "systemPropertyName: '" + systemPropertiesSecret.systemPropertyName + "';\n" +
                        getTabString(indentLevel) + "}";
            }
            else if (credentialVaultSecret instanceof EnvironmentCredentialVaultSecret)
            {
                EnvironmentCredentialVaultSecret environmentCredentialVaultSecret = (EnvironmentCredentialVaultSecret) credentialVaultSecret;
                return "EnvironmentSecret\n" +
                        getTabString(indentLevel) + "{\n" +
                        getTabString(indentLevel + 1) + "envVariableName: '" + environmentCredentialVaultSecret.envVariableName + "';\n" +
                        getTabString(indentLevel) + "}";
            }
            return null;
        });
    }

    private String renderCredentialVaultSecret(CredentialVaultSecret credentialVaultSecret, int indentLevel)
    {
        List<Function2<CredentialVaultSecret, Integer, String>> processors = ListIterate.flatCollect(IAuthenticationGrammarComposerExtension.getExtensions(), ext -> ext.getExtraCredentialVaultSecretComposers());

        return ListIterate.collect(processors, processor -> processor.value(credentialVaultSecret, indentLevel))
                .select(Objects::nonNull)
                .getFirstOptional()
                .orElseThrow(() -> new EngineException("Unsupported credential vault Secret - " + credentialVaultSecret.getClass(), credentialVaultSecret.sourceInformation, EngineErrorType.PARSER));

    }
}