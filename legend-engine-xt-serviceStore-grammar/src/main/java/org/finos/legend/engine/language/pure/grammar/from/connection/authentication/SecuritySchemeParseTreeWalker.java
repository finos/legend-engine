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
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.authentication.SecuritySchemeParserGrammar;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model.ApiKeySecurityScheme;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model.SimpleHttpSecurityScheme;

public class SecuritySchemeParseTreeWalker
{

    public SimpleHttpSecurityScheme visitSimpleHttpSecurityScheme(SecuritySchemeSourceCode code, SecuritySchemeParserGrammar.HttpSecuritySchemeContext securitySchemeCtx)
    {
        SimpleHttpSecurityScheme securityScheme = new SimpleHttpSecurityScheme();
        securityScheme.sourceInformation = code.getSourceInformation();
        SecuritySchemeParserGrammar.SchemeContext schemeContext = PureGrammarParserUtility.validateAndExtractRequiredField(securitySchemeCtx.scheme(), "scheme", securityScheme.sourceInformation);
        securityScheme.scheme = PureGrammarParserUtility.fromGrammarString(schemeContext.STRING().getText(), true);

        return securityScheme;
    }

    public ApiKeySecurityScheme visitApiKeySecurityScheme(SecuritySchemeSourceCode code, SecuritySchemeParserGrammar.ApiKeySecuritySchemeContext securitySchemeCtx)
    {
        ApiKeySecurityScheme securityScheme = new ApiKeySecurityScheme();
        securityScheme.sourceInformation = code.getSourceInformation();
        SecuritySchemeParserGrammar.LocationContext locationContext = PureGrammarParserUtility.validateAndExtractRequiredField(securitySchemeCtx.location(), "location", securityScheme.sourceInformation);
        securityScheme.location = PureGrammarParserUtility.fromGrammarString(locationContext.STRING().getText(), true);

        SecuritySchemeParserGrammar.KeynameContext keynameContext = PureGrammarParserUtility.validateAndExtractRequiredField(securitySchemeCtx.keyname(), "keyNamme", securityScheme.sourceInformation);
        securityScheme.keyName = PureGrammarParserUtility.fromGrammarString(keynameContext.STRING().getText(), true);

        return securityScheme;
    }
}
