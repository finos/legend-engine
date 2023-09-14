// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.language.pure.dsl.mastery.grammar.to;

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.authentication.AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.authentication.CredentialSecret;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.authentication.NTLMAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.authentication.TokenAuthenticationStrategy;

import java.util.List;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.convertString;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabString;

public class HelperAuthenticationComposer
{

    public static String renderAuthentication(AuthenticationStrategy authenticationStrategy, int indentLevel, PureGrammarComposerContext context)
    {

        if (authenticationStrategy instanceof NTLMAuthenticationStrategy)
        {
            return renderNTLMAuthentication((NTLMAuthenticationStrategy) authenticationStrategy, indentLevel, context);
        }

        if (authenticationStrategy instanceof TokenAuthenticationStrategy)
        {
            return renderTokenAuthentication((TokenAuthenticationStrategy) authenticationStrategy, indentLevel, context);
        }
        return null;
    }

    private static String renderNTLMAuthentication(NTLMAuthenticationStrategy authenticationStrategy, int indentLevel, PureGrammarComposerContext context)
    {
       return "NTLM #{ \n"
               + renderCredentialSecret("credential", authenticationStrategy.credential, indentLevel + 1, context)
               + getTabString(indentLevel + 1) + "}#;\n";
    }

    private static String renderTokenAuthentication(TokenAuthenticationStrategy authenticationStrategy, int indentLevel, PureGrammarComposerContext context)
    {
        return "Token #{ \n"
                + renderCredentialSecret("credential", authenticationStrategy.credential, indentLevel + 1, context)
                + getTabString(indentLevel + 1) + "tokenUrl: " + convertString(authenticationStrategy.tokenUrl, true) + ";\n"
                +  getTabString(indentLevel + 1) + "}#;\n";
    }

    public static String renderCredentialSecret(String field, CredentialSecret credentialSecret, int indentLevel, PureGrammarComposerContext context)
    {
        if (credentialSecret == null)
        {
            return "";
        }
        List<IMasteryComposerExtension> extensions = IMasteryComposerExtension.getExtensions(context);
        String text = IMasteryComposerExtension.process(credentialSecret, ListIterate.flatCollect(extensions, IMasteryComposerExtension::getExtraSecretComposers), indentLevel, context);
        return getTabString(indentLevel) + field + ": " + text;
    }
}
