// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.language.pure.grammar.integration.store.graphql.connection;

import org.finos.legend.engine.language.pure.dsl.authentication.grammar.from.IAuthenticationGrammarParserExtension;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.store.graphql.connection.GraphQLConnectionParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtensions;
import org.finos.legend.engine.protocol.graphQLStore.metamodel.pure.GraphQLConnection;

import java.util.Collections;

public class GraphQLConnectionParseTreeWalker
{
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final PureGrammarParserExtensions extension;

    public GraphQLConnectionParseTreeWalker(ParseTreeWalkerSourceInformation walkerSourceInformation, PureGrammarParserExtensions extension)
    {
        this.walkerSourceInformation = walkerSourceInformation;
        this.extension = extension;
    }

    public void visitGraphQLConnectionValue(GraphQLConnectionParserGrammar.DefinitionContext ctx, GraphQLConnection connectionValue, boolean isEmbedded)
    {
        // store (optional if the store is provided by embedding context, if not provided, it is required)
        GraphQLConnectionParserGrammar.ConnectionStoreContext storeContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.connectionStore(), "store", connectionValue.sourceInformation);
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

        GraphQLConnectionParserGrammar.ConnectionUrlContext urlContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.connectionUrl(), "url", connectionValue.sourceInformation);
        connectionValue.url = PureGrammarParserUtility.fromIdentifier(urlContext.identifier());

        GraphQLConnectionParserGrammar.AuthenticationContext authenticationContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.authentication(), "authentication", connectionValue.sourceInformation);
        if (authenticationContext != null)
        {
            connectionValue.authenticationSpecification = IAuthenticationGrammarParserExtension.parseAuthentication(authenticationContext.islandDefinition(), this.walkerSourceInformation, this.extension);
        }
    }
}
