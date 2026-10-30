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

package org.finos.legend.engine.language.pure.grammar.integration.store.graphql;

import org.finos.legend.engine.language.graphQL.grammar.from.GraphQLGrammarParser;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.store.graphql.GraphQLStoreParserGrammar;
import org.finos.legend.engine.protocol.graphQLStore.metamodel.pure.GraphQLStore;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.DefaultCodeSection;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.function.Consumer;

public class GraphQLStoreParseTreeWalker
{
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final Consumer<PackageableElement> elementConsumer;
    private final DefaultCodeSection section;

    public GraphQLStoreParseTreeWalker(ParseTreeWalkerSourceInformation walkerSourceInformation, Consumer<PackageableElement> elementConsumer, DefaultCodeSection section)
    {
        this.walkerSourceInformation = walkerSourceInformation;
        this.elementConsumer = elementConsumer;
        this.section = section;
    }

    public void visit(GraphQLStoreParserGrammar.DefinitionContext ctx)
    {
        ctx.graphQLStore().stream().map(this::visitGraphQLStore)
                .peek(e -> this.section.elements.add(e.getPath())).forEach(this.elementConsumer);
    }

    private GraphQLStore visitGraphQLStore(GraphQLStoreParserGrammar.GraphQLStoreContext ctx)
    {
        GraphQLStore graphQLStore = new GraphQLStore();
        graphQLStore.name = PureGrammarParserUtility.fromIdentifier(ctx.qualifiedName().identifier());
        graphQLStore._package = ctx.qualifiedName().packagePath() == null ? "" : PureGrammarParserUtility.fromPath(ctx.qualifiedName().packagePath().identifier());
        graphQLStore.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        if (ctx.schemaContent() != null)
        {
            StringBuilder schemaTextBuilder = new StringBuilder();
            for (GraphQLStoreParserGrammar.SchemaIslandFragmentContext fragment : ctx.schemaContent().schemaIslandFragment())
            {
                schemaTextBuilder.append(fragment.getText());
            }
            String schemaText = schemaTextBuilder.length() > 0 ? schemaTextBuilder.substring(0, schemaTextBuilder.length() - 1) : schemaTextBuilder.toString();
            try
            {
                graphQLStore.schema = GraphQLGrammarParser.newInstance().parseDocument(schemaText);
            }
            catch (Exception e)
            {
                throw new EngineException("Failed to parse tracked schema for store '" + graphQLStore.getPath() + "': " + e.getMessage(), this.walkerSourceInformation.getSourceInformation(ctx.schemaContent()), EngineErrorType.PARSER);
            }
        }

        return graphQLStore;
    }
}
