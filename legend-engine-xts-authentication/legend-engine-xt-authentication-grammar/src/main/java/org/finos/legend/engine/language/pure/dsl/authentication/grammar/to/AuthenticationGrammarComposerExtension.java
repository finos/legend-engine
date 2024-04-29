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

import java.util.List;
import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.AuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.CredentialVaultSecret;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.EnvironmentCredentialVaultSecret;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.PropertiesFileSecret;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.SystemPropertiesSecret;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabString;

public class AuthenticationGrammarComposerExtension implements IAuthenticationGrammarComposerExtension
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("PackageableElement", "Authentication");
    }

    @Override
    public List<Function3<AuthenticationSpecification, Integer, PureGrammarComposerContext, String>> getExtraAuthenticationSpecificationComposers()
    {
        return Lists.fixedSize.with((authenticationSpec, indentLevel, context) ->
                authenticationSpec.accept(new AuthenticationSpecificationComposer(indentLevel, context)));
    }

    @Override
    public List<Function3<CredentialVaultSecret, Integer, PureGrammarComposerContext, String>> getExtraCredentialVaultSecretComposers()
    {
        return Lists.fixedSize.with((credentialVaultSecret, indentLevel, context) ->
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
}