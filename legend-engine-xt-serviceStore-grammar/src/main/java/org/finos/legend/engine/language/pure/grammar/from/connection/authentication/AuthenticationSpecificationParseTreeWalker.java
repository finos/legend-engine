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

package org.finos.legend.engine.language.pure.grammar.from.connection.authentication;

import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.authentication.AuthSpecificationParserGrammar;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model.ApiKeyAuthentication;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model.OAuthAuthentication;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model.UsernamePasswordAuthentication;

public class AuthenticationSpecificationParseTreeWalker
{

    public UsernamePasswordAuthentication visitUsernamePasswordAuthentication(AuthenticationSpecificationSourceCode code, AuthSpecificationParserGrammar.BasicAuthenticationContext ctx)
    {
        UsernamePasswordAuthentication u = new UsernamePasswordAuthentication();
        u.sourceInformation = code.getSourceInformation();

        AuthSpecificationParserGrammar.UsernameContext usernameContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.username(),"username", u.sourceInformation);
        u.username = PureGrammarParserUtility.fromGrammarString(usernameContext.STRING().getText(), true);

        AuthSpecificationParserGrammar.PasswordContext passwordContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.password(), "password", u.sourceInformation);
        u.password = PureGrammarParserUtility.fromGrammarString(passwordContext.STRING().getText(), true);

        return u;
    }

    public ApiKeyAuthentication visitApiKeyAuthentication(AuthenticationSpecificationSourceCode code, AuthSpecificationParserGrammar.ApiKeyAuthenticationContext ctx)
    {
        ApiKeyAuthentication u = new ApiKeyAuthentication();
        u.sourceInformation = code.getSourceInformation();

        AuthSpecificationParserGrammar.ValueContext valueContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.value(),"value", u.sourceInformation);
        u.value = PureGrammarParserUtility.fromGrammarString(valueContext.STRING().getText(), true);

        return u;
    }

    public OAuthAuthentication visitOAuthAuthentication(AuthenticationSpecificationSourceCode code, AuthSpecificationParserGrammar.OauthAuthenticationContext ctx)
    {
        OAuthAuthentication o = new OAuthAuthentication();
        o.sourceInformation = code.getSourceInformation();

        AuthSpecificationParserGrammar.GrantTypeContext grantTypeContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.grantType(), "grantType", o.sourceInformation);
        //o.grantType = OauthGrantType.valueOf(PureGrammarParserUtility.fromGrammarString(grantTypeContext.STRING().getText(), true));
        o.grantType = PureGrammarParserUtility.fromGrammarString(grantTypeContext.STRING().getText(), true);

        AuthSpecificationParserGrammar.ClientIdContext clientIdContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.clientId(), "clientId", o.sourceInformation);
        o.clientId = PureGrammarParserUtility.fromGrammarString(clientIdContext.STRING().getText(), true);

        AuthSpecificationParserGrammar.ClientSecretContext clientSecretContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.clientSecret(), "clientSecret", o.sourceInformation);
        if (clientSecretContext != null)
        {
            o.clientSecretVaultReference = PureGrammarParserUtility.fromGrammarString(clientSecretContext.STRING().getText(), true);
        }

        AuthSpecificationParserGrammar.AuthServerUrlContext authServerUrlContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.authServerUrl(), "authServerUrl", o.sourceInformation);
        o.authServerUrl = PureGrammarParserUtility.fromGrammarString(authServerUrlContext.STRING().getText(), true);

        return o;
    }
}
