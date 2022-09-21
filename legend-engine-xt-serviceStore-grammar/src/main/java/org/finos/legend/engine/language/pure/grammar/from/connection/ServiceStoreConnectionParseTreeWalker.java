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
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.ServiceStoreConnectionParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.connection.authentication.AuthenticationSpecificationSourceCode;
import org.finos.legend.engine.language.pure.grammar.from.extensions.IServiceStoreGrammarParserExtension;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.connection.ServiceStoreConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model.AuthenticationSpecification;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

public class ServiceStoreConnectionParseTreeWalker
{
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;

    public ServiceStoreConnectionParseTreeWalker(ParseTreeWalkerSourceInformation walkerSourceInformation)
    {
        this.walkerSourceInformation = walkerSourceInformation;
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

        ServiceStoreConnectionParserGrammar.AuthenticationContext authContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.authentication(), "authentication", connectionValue.sourceInformation);
        connectionValue.authSpecs = ListIterate.collect(authContext.authSpecificationObject(), this::visitAuthSpecification).stream().collect(Collectors.toMap(Pair::getOne,Pair::getTwo, (u,v) -> u, LinkedHashMap::new));

    }

    private Pair<String, AuthenticationSpecification> visitAuthSpecification(ServiceStoreConnectionParserGrammar.AuthSpecificationObjectContext ctx)
    {
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        ServiceStoreConnectionParserGrammar.SingleAuthSpecificationContext specContext = ctx.singleAuthSpecification();
        AuthenticationSpecificationSourceCode code = new AuthenticationSpecificationSourceCode(
                specContext.getText(),
                specContext.authSpecificationType().getText(),
                sourceInformation,
                ParseTreeWalkerSourceInformation.offset(walkerSourceInformation, specContext.getStart())
        );


        List<IServiceStoreGrammarParserExtension> extensions = IServiceStoreGrammarParserExtension.getExtensions();
        AuthenticationSpecification spec = IServiceStoreGrammarParserExtension.process(code, ListIterate.flatCollect(extensions, IServiceStoreGrammarParserExtension::getExtraAuthenticationGenerationSpecificationParsers));

        if (spec == null)
        {
            throw new EngineException("Unsupported syntax", this.walkerSourceInformation.getSourceInformation(ctx), EngineErrorType.PARSER);
        }

        String securitySchemeId = PureGrammarParserUtility.fromIdentifier(ctx.qualifiedName().identifier());
        return Tuples.pair(securitySchemeId,spec);
    }


    private void validateUrl(String url, SourceInformation sourceInformation)
    {
        if (url.endsWith("/"))
        {
            throw new EngineException("baseUrl should not end with '/'", sourceInformation, EngineErrorType.PARSER);
        }
    }
}
