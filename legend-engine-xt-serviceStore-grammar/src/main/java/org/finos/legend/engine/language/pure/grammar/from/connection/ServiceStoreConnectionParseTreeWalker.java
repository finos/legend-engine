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

package org.finos.legend.engine.language.pure.grammar.from.connection;

import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.dsl.authentication.grammar.from.IAuthenticationGrammarParserExtension;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.ServiceStoreConnectionParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtensions;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.AuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.connection.ServiceStoreConnection;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.Collections;
import java.util.HashMap;
import java.util.stream.Collectors;

public class ServiceStoreConnectionParseTreeWalker
{
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final PureGrammarParserExtensions extensions;

    public ServiceStoreConnectionParseTreeWalker(ParseTreeWalkerSourceInformation walkerSourceInformation, PureGrammarParserExtensions extensions)
    {
        this.walkerSourceInformation = walkerSourceInformation;
        this.extensions = extensions;
    }

    public void visitServiceStoreConnectionValue(ServiceStoreConnectionParserGrammar.DefinitionContext ctx, ServiceStoreConnection connectionValue, boolean isEmbedded)
    {
        // store (optional if the store is provided by embedding context, if not provided, it is required)
        ServiceStoreConnectionParserGrammar.ConnectionStoreContext storeContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.connectionStore(), "store", connectionValue.sourceInformation);
        if (storeContext != null)
        {
            connectionValue.element = PureGrammarParserUtility.fromQualifiedName(storeContext.qualifiedName().packagePath() == null ? Collections.emptyList() : storeContext.qualifiedName().packagePath().identifier(), storeContext.qualifiedName().identifier());
            connectionValue.elementSourceInformation = this.walkerSourceInformation.getSourceInformation(storeContext.qualifiedName());
        }
        else if (!isEmbedded)
        {
            // non-embedded connection requires store
            PureGrammarParserUtility.validateAndExtractRequiredField(ctx.connectionStore(), "store", connectionValue.sourceInformation);
        }
        // database type
        ServiceStoreConnectionParserGrammar.BaseUrlContext baseUrlCtx = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.baseUrl(), "baseUrl", connectionValue.sourceInformation);
        connectionValue.baseUrl = PureGrammarParserUtility.fromIdentifier(baseUrlCtx.identifier());

        validateUrl(connectionValue.baseUrl, this.walkerSourceInformation.getSourceInformation(baseUrlCtx));

        ServiceStoreConnectionParserGrammar.AuthenticationSpecContext authContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.authenticationSpec(), "authentication", connectionValue.sourceInformation);
        if (authContext != null)
        {
            connectionValue.authenticationSpecifications = ListIterate.collect(authContext.authSpecificationObject(), this::visitAuthentication).stream().collect(Collectors.toMap(Pair::getOne, Pair::getTwo));
        }
        else
        {
            connectionValue.authenticationSpecifications = new HashMap<>();
        }
    }

    private Pair<String, AuthenticationSpecification> visitAuthentication(ServiceStoreConnectionParserGrammar.AuthSpecificationObjectContext authSpecificationObjectContext)
    {
        AuthenticationSpecification authenticationSpec = IAuthenticationGrammarParserExtension.parseAuthentication(authSpecificationObjectContext.islandDefinition(), walkerSourceInformation, extensions);
        String securitySchemeId = PureGrammarParserUtility.fromIdentifier(authSpecificationObjectContext.identifier());
        return Tuples.pair(securitySchemeId, authenticationSpec);
    }

    private void validateUrl(String url, SourceInformation sourceInformation)
    {
        if (url.endsWith("/"))
        {
            throw new EngineException("baseUrl should not end with '/'", sourceInformation, EngineErrorType.PARSER);
        }
    }
}
