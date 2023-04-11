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


package org.finos.legend.engine.language.pure.grammar.from.securityScheme;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.SpecificationSourceCode;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.securityScheme.SecuritySchemeParserGrammar;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model.ApiKeySecurityScheme;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model.HttpSecurityScheme;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model.HttpSecurityScheme.Scheme;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model.ApiKeySecurityScheme.Location;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.List;

public class SecuritySchemeParseTreeWalker
{

    public HttpSecurityScheme visitHttpSecurityScheme(SpecificationSourceCode code, SecuritySchemeParserGrammar.HttpSecuritySchemeContext securitySchemeCtx)
    {
        HttpSecurityScheme securityScheme = new HttpSecurityScheme();
        securityScheme.sourceInformation = code.getSourceInformation();

        SecuritySchemeParserGrammar.SchemeContext schemeContext = PureGrammarParserUtility.validateAndExtractRequiredField(securitySchemeCtx.scheme(), "scheme", securityScheme.sourceInformation);
        String providedScheme = PureGrammarParserUtility.fromIdentifier(schemeContext.identifier());
        List<String> supportedSchemes = ListIterate.collect(Lists.mutable.with(Scheme.values()), Scheme::toString);
        if (!supportedSchemes.contains(providedScheme.toUpperCase()))
        {
            throw new EngineException("Unsupported Scheme - " + providedScheme + ". Supported schemes are - " + String.join(",", ListIterate.collect(supportedSchemes, String::toLowerCase)), code.getWalkerSourceInformation().getSourceInformation(schemeContext), EngineErrorType.PARSER);
        }
        securityScheme.scheme = Scheme.valueOf(providedScheme.toUpperCase());

        SecuritySchemeParserGrammar.BearerFormatContext bearerFormatContext = PureGrammarParserUtility.validateAndExtractOptionalField(securitySchemeCtx.bearerFormat(),"bearerFormat",securityScheme.sourceInformation);
        if (bearerFormatContext != null)
        {
            securityScheme.bearerFormat = PureGrammarParserUtility.fromGrammarString(bearerFormatContext.STRING().getText(), true);
        }

        return securityScheme;
    }

    public ApiKeySecurityScheme visitApiKeySecurityScheme(SpecificationSourceCode code, SecuritySchemeParserGrammar.ApiKeySecuritySchemeContext securitySchemeCtx)
    {
        ApiKeySecurityScheme securityScheme = new ApiKeySecurityScheme();
        securityScheme.sourceInformation = code.getSourceInformation();

        SecuritySchemeParserGrammar.LocationContext locationContext = PureGrammarParserUtility.validateAndExtractRequiredField(securitySchemeCtx.location(), "location", securityScheme.sourceInformation);
        String providedLocation = PureGrammarParserUtility.fromIdentifier(locationContext.identifier());
        List<String> supportedLocations = ListIterate.collect(Lists.mutable.with(Location.values()), Location::toString);
        if (!supportedLocations.contains(providedLocation.toUpperCase()))
        {
            throw new EngineException("Unsupported Api Key location - " + providedLocation + ". Supported locations are - " + String.join(",", ListIterate.collect(supportedLocations, String::toLowerCase)), code.getWalkerSourceInformation().getSourceInformation(locationContext), EngineErrorType.PARSER);
        }
        securityScheme.location = Location.valueOf(providedLocation.toUpperCase());

        SecuritySchemeParserGrammar.KeynameContext keynameContext = PureGrammarParserUtility.validateAndExtractRequiredField(securitySchemeCtx.keyname(), "keyNamme", securityScheme.sourceInformation);
        securityScheme.keyName = PureGrammarParserUtility.fromGrammarString(keynameContext.STRING().getText(), true);

        return securityScheme;
    }
}
