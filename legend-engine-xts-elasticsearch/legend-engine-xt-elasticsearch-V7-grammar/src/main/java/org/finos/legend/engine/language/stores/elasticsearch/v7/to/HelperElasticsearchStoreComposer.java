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
//

package org.finos.legend.engine.language.stores.elasticsearch.v7.to;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.language.pure.dsl.authentication.grammar.to.IAuthenticationGrammarComposerExtension;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility;
import org.finos.legend.engine.language.stores.elasticsearch.v7.from.ElasticsearchGrammarParserExtension;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.runtime.Elasticsearch7StoreConnection;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.runtime.Elasticsearch7StoreURLSourceSpecification;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.store.Elasticsearch7Store;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.store.Elasticsearch7StoreIndex;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.store.Elasticsearch7StoreIndexProperty;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.mapping.PropertyBase;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabString;

public class HelperElasticsearchStoreComposer
{
    public static String render(Elasticsearch7Store store, PureGrammarComposerContext context)
    {
        StringWriter stringWriter = new StringWriter();
        PrintWriter builder = new PrintWriter(stringWriter)
        {
            @Override
            public void println()
            {
                this.print('\n');
            }
        };
        builder.append(context.getIndentationString()).append("Elasticsearch7Cluster ").println(PureGrammarComposerUtility.convertPath(store.getPath()));
        builder.append(context.getIndentationString()).println("{");

        builder.append(context.getIndentationString()).append(getTabString()).println("indices: [");
        Iterator<Elasticsearch7StoreIndex> storeIndexIterator = store.indices.iterator();
        while (storeIndexIterator.hasNext())
        {
            render(builder, storeIndexIterator.next(), context);
            if (storeIndexIterator.hasNext())
            {
                builder.println(',');
            }
            else
            {
                builder.println();
            }
        }
        builder.append(context.getIndentationString()).append(getTabString()).println("];");

        builder.append(context.getIndentationString()).println("}");

        return stringWriter.toString();
    }

    private static void render(PrintWriter builder, Elasticsearch7StoreIndex index, PureGrammarComposerContext context)
    {
        builder.append(context.getIndentationString()).append(getTabString(2)).append(PureGrammarComposerUtility.convertIdentifier(index.indexName)).println(": {");
        builder.append(context.getIndentationString()).append(getTabString(3)).println("properties: [");

        HelperElasticsearchPropertyComposer propertyComposer = new HelperElasticsearchPropertyComposer(builder, 4, context);
        Iterator<Elasticsearch7StoreIndexProperty> indexPropertyIterator = index.properties.iterator();
        while (indexPropertyIterator.hasNext())
        {
            Elasticsearch7StoreIndexProperty property = indexPropertyIterator.next();
            builder.append(context.getIndentationString()).append(getTabString(4)).append(PureGrammarComposerUtility.convertIdentifier(property.propertyName)).append(": ");

            ((PropertyBase) property.property.unionValue())
                    .accept(propertyComposer);

            if (indexPropertyIterator.hasNext())
            {
                builder.println(',');
            }
            else
            {
                builder.println();
            }
        }

        builder.append(context.getIndentationString()).append(getTabString(3)).println("];");
        builder.append(context.getIndentationString()).append(getTabString(2)).print("}");

    }

    public static Pair<String, String> render(Elasticsearch7StoreConnection connectionValue, PureGrammarComposerContext context)
    {
        return Tuples.pair(ElasticsearchGrammarParserExtension.V7_CONNECTION_TYPE_NAME,
                context.getIndentationString() + "{\n" +
                        context.getIndentationString() + getTabString() + "store: " + PureGrammarComposerUtility.convertPath(connectionValue.element) + ";\n" +
                        context.getIndentationString() + getTabString() + "clusterDetails: " + render(connectionValue.sourceSpec, context) + ";\n" +
                        context.getIndentationString() + getTabString() + "authentication: " + IAuthenticationGrammarComposerExtension.renderAuthentication(connectionValue.authSpec, 1, context) + ";\n" +
                        context.getIndentationString() + "}");
    }

    private static String render(Elasticsearch7StoreURLSourceSpecification sourceSpec, PureGrammarComposerContext context)
    {
        return String.format("# URL { %s }#", sourceSpec.url);
    }
}
