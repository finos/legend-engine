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

import java.util.List;

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.language.pure.grammar.to.extension.PureGrammarComposerExtension;
import org.finos.legend.engine.language.stores.elasticsearch.v7.from.ElasticsearchGrammarParserExtension;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.runtime.Elasticsearch7StoreConnection;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.store.Elasticsearch7Store;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposer.buildSectionComposer;

public class ElasticsearchGrammarComposerExtension implements PureGrammarComposerExtension
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Store", "Elastic");
    }

    private MutableList<Function2<PackageableElement, PureGrammarComposerContext, String>> renderers = Lists.mutable.with((element, context) ->
    {
        if (element instanceof Elasticsearch7Store)
        {
            return HelperElasticsearchStoreComposer.render((Elasticsearch7Store) element, context);
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
        return Lists.mutable.with(buildSectionComposer(ElasticsearchGrammarParserExtension.NAME, renderers));
    }

    @Override
    public List<Function3<List<PackageableElement>, PureGrammarComposerContext, List<String>, PureFreeSectionGrammarComposerResult>> getExtraFreeSectionComposers()
    {
        return Lists.fixedSize.with((elements, context, composedSections) ->
        {
            MutableList<Elasticsearch7Store> composableElements = ListIterate.selectInstancesOf(elements, Elasticsearch7Store.class);
            return composableElements.isEmpty() ? null : new PureFreeSectionGrammarComposerResult(composableElements.collectWith(HelperElasticsearchStoreComposer::render, context).makeString("###" + ElasticsearchGrammarParserExtension.NAME + "\n", "\n\n", ""), composableElements);
        });
    }

    @Override
    public List<Function2<Connection, PureGrammarComposerContext, Pair<String, String>>> getExtraConnectionValueComposers()
    {
        return Lists.fixedSize.with((connectionValue, context) ->
        {
            if (connectionValue instanceof Elasticsearch7StoreConnection)
            {
                return HelperElasticsearchStoreComposer.render((Elasticsearch7StoreConnection) connectionValue, context);
            }

            return null;
        });
    }
}
