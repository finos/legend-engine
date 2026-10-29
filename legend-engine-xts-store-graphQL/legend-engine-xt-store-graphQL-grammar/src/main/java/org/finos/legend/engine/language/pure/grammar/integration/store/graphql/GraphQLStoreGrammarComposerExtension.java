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

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.dsl.authentication.grammar.to.IAuthenticationGrammarComposerExtension;
import org.finos.legend.engine.language.pure.grammar.integration.store.graphql.util.GraphQLMappingComposer;
import org.finos.legend.engine.language.pure.grammar.integration.store.graphql.util.GraphQLStoreComposer;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility;
import org.finos.legend.engine.language.pure.grammar.to.extension.PureGrammarComposerExtension;
import org.finos.legend.engine.protocol.graphQLStore.metamodel.pure.GraphQLClassMapping;
import org.finos.legend.engine.protocol.graphQLStore.metamodel.pure.GraphQLConnection;
import org.finos.legend.engine.protocol.graphQLStore.metamodel.pure.GraphQLStore;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.ClassMapping;

import java.util.List;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposer.buildSectionComposer;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabString;

public class GraphQLStoreGrammarComposerExtension implements PureGrammarComposerExtension
{
    @Override
    public MutableList<String> group()
    {
        return Lists.mutable.with("Store", "GraphQL");
    }

    private final MutableList<Function2<PackageableElement, PureGrammarComposerContext, String>> renderers = Lists.mutable.with((element, context) ->
    {
        if (element instanceof GraphQLStore)
        {
            return GraphQLStoreComposer.renderGraphQLStore((GraphQLStore) element);
        }
        return null;
    });

    @Override
    public MutableList<Function2<PackageableElement, PureGrammarComposerContext, String>> getExtraPackageableElementComposers()
    {
        return renderers;
    }

    @Override
    public List<Function3<List<PackageableElement>, PureGrammarComposerContext, String, String>> getExtraSectionComposers()
    {
        return Lists.mutable.with(buildSectionComposer(GraphQLStoreGrammarParserExtension.NAME, renderers));
    }

    @Override
    public List<Function3<List<PackageableElement>, PureGrammarComposerContext, List<String>, PureFreeSectionGrammarComposerResult>> getExtraFreeSectionComposers()
    {
        return Lists.fixedSize.with((elements, context, composedSections) ->
        {
            MutableList<PackageableElement> composableElements = ListIterate.select(elements, e -> e instanceof GraphQLStore);
            return composableElements.isEmpty() ? null : new PureFreeSectionGrammarComposerResult(composableElements.collect(e -> renderers.get(0).value(e, context)).makeString("###" + GraphQLStoreGrammarParserExtension.NAME + "\n", "\n\n", ""), composableElements);
        });
    }

    @Override
    public List<Function2<Connection, PureGrammarComposerContext, Pair<String, String>>> getExtraConnectionValueComposers()
    {
        return Lists.mutable.with((connectionValue, context) ->
        {
            if (connectionValue instanceof GraphQLConnection)
            {
                GraphQLConnection graphQLConnection = (GraphQLConnection) connectionValue;
                StringBuilder builder = new StringBuilder();
                builder.append(context.getIndentationString()).append("{\n");
                if (graphQLConnection.element != null)
                {
                    builder.append(context.getIndentationString()).append(getTabString()).append("store: ").append(graphQLConnection.element).append(";\n");
                }
                builder.append(context.getIndentationString()).append(getTabString()).append("url: ").append(PureGrammarComposerUtility.convertString(graphQLConnection.url, true)).append(";\n");
                if (graphQLConnection.authenticationSpecification != null)
                {
                    builder.append(context.getIndentationString()).append(getTabString()).append("authentication: ").append(IAuthenticationGrammarComposerExtension.renderAuthentication(graphQLConnection.authenticationSpecification, 1, context)).append(";\n");
                }
                builder.append(context.getIndentationString()).append("}");
                return Tuples.pair(GraphQLStoreGrammarParserExtension.GRAPHQL_CONNECTION_TYPE, builder.toString());
            }
            return null;
        });
    }

    @Override
    public List<Function2<ClassMapping, PureGrammarComposerContext, String>> getExtraClassMappingComposers()
    {
        return Lists.mutable.with((classMapping, context) ->
        {
            if (classMapping instanceof GraphQLClassMapping)
            {
                GraphQLClassMapping graphQLClassMapping = (GraphQLClassMapping) classMapping;
                StringBuilder builder = new StringBuilder();
                builder.append(": ").append(GraphQLStoreGrammarParserExtension.GRAPHQL_STORE_MAPPING_ELEMENT_TYPE).append("\n");
                builder.append(getTabString()).append("{\n");
                builder.append(GraphQLMappingComposer.renderGraphQLClassMapping(graphQLClassMapping));
                builder.append(getTabString()).append("}");
                return builder.toString();
            }
            return null;
        });
    }
}
