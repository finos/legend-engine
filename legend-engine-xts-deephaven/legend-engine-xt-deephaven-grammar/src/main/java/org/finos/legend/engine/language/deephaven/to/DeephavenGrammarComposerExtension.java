// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.language.deephaven.to;

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.deephaven.from.DeephavenGrammarParserExtension;
import org.finos.legend.engine.language.pure.dsl.authentication.grammar.to.IAuthenticationGrammarComposerExtension;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility;
import org.finos.legend.engine.language.pure.grammar.to.extension.PureGrammarComposerExtension;
import org.finos.legend.engine.protocol.deephaven.metamodel.runtime.DeephavenConnection;
import org.finos.legend.engine.protocol.deephaven.metamodel.store.Column;
import org.finos.legend.engine.protocol.deephaven.metamodel.store.DeephavenStore;
import org.finos.legend.engine.protocol.deephaven.metamodel.store.Table;
import org.finos.legend.engine.protocol.deephaven.metamodel.type.FloatType;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.deephaven.metamodel.type.BooleanType;
import org.finos.legend.engine.protocol.deephaven.metamodel.type.IntType;
import org.finos.legend.engine.protocol.deephaven.metamodel.type.StringType;
import org.finos.legend.engine.protocol.deephaven.metamodel.type.DateTimeType;

import java.util.List;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposer.buildSectionComposer;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabString;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.unsupported;

public class DeephavenGrammarComposerExtension implements PureGrammarComposerExtension
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Store", "Deephaven");
    }

    private MutableList<Function2<PackageableElement, PureGrammarComposerContext, String>> renderers = Lists.mutable.with((element, context) ->
    {
        if (element instanceof DeephavenStore)
        {
            return renderDeephavenStore((DeephavenStore) element, context);
        }
        return null;
    });

    private static String renderDeephavenStore(DeephavenStore element, PureGrammarComposerContext context)
    {
        int baseIndentation = 1;
        StringBuilder builder = new StringBuilder();
        builder.append("Deephaven ").append(PureGrammarComposerUtility.convertPath(element.getPath())).append("\n");
        builder.append("(\n");
        if (!element.tables.isEmpty())
        {
            builder.append(LazyIterate.collect(element.tables, table -> renderDeephavenDatabaseTable(table, baseIndentation + 1, context)).makeString("\n"));
            builder.append("\n");
        }
        builder.append(")");
        return builder.toString();
    }

    private static Object renderDeephavenDatabaseTable(Table table, int i, PureGrammarComposerContext context)
    {
        int baseIndentation = 2;
        StringBuilder builder = new StringBuilder();
        builder.append(getTabString(baseIndentation)).append("Table ").append(table.name).append("\n");
        builder.append(getTabString(baseIndentation)).append("(\n");
        boolean nonEmpty = false;
        if (!table.columns.isEmpty())
        {
            builder.append(nonEmpty ? "\n" : "");
            builder.append(LazyIterate.collect(table.columns, column -> renderDeephavenTableColumn(column, table, baseIndentation + 1)).makeString(",\n"));
            builder.append("\n");
        }
        builder.append(getTabString(baseIndentation)).append(")");
        return builder.toString();
    }

    private static Object renderDeephavenTableColumn(Column column, Table table, int i)
    {
        int baseIndentation = 3;
        StringBuilder builder = new StringBuilder();
        builder.append(getTabString(baseIndentation)).append(column.name).append(": ");
        if (column.type instanceof StringType)
        {
            builder.append("String");
        }
        else if (column.type instanceof IntType)
        {
            builder.append("Integer");
        }
        else if (column.type instanceof BooleanType)
        {
            builder.append("Boolean");
        }
        else if (column.type instanceof DateTimeType)
        {
            builder.append("DateTime");
        }
        else if (column.type instanceof FloatType)
        {
            builder.append("Float");
        }
        else
        {
            builder.append(unsupported(column.type.getClass(), "database table column type"));
        }
        return builder.toString();
    }

    @Override
    public MutableList<Function2<PackageableElement, PureGrammarComposerContext, String>> getExtraPackageableElementComposers()
    {
        return renderers;
    }

    @Override
    public List<Function3<List<PackageableElement>, PureGrammarComposerContext, String, String>> getExtraSectionComposers()
    {
        return Lists.mutable.with(buildSectionComposer(DeephavenGrammarParserExtension.NAME, renderers));
    }

    @Override
    public List<Function3<List<PackageableElement>, PureGrammarComposerContext, List<String>, PureFreeSectionGrammarComposerResult>> getExtraFreeSectionComposers()
    {
        return Lists.fixedSize.with((elements, context, composedSections) ->
        {
            MutableList<DeephavenStore> composableElements = ListIterate.selectInstancesOf(elements, DeephavenStore.class);
            return composableElements.isEmpty() ? null : new PureFreeSectionGrammarComposerResult(composableElements.collectWith(DeephavenGrammarComposerExtension::renderDeephavenStore, context).makeString("###" + DeephavenGrammarParserExtension.NAME + "\n", "\n\n", ""), composableElements);
        });
    }

    @Override
    public List<Function2<Connection, PureGrammarComposerContext, Pair<String, String>>> getExtraConnectionValueComposers()
    {
        return Lists.fixedSize.with((connectionValue, context) ->
        {
            if (connectionValue instanceof DeephavenConnection)
            {
                return DeephavenGrammarComposerExtension.render((DeephavenConnection) connectionValue, context);
            }
            return null;
        });
    }

    private static Pair<String, String> render(DeephavenConnection connectionValue, PureGrammarComposerContext context)
    {
        return Tuples.pair(DeephavenGrammarParserExtension.DEEPHAVEN_CONNECTION_TYPE,
                context.getIndentationString() + "{\n" +
                        context.getIndentationString() + getTabString() + "store: " + PureGrammarComposerUtility.convertPath(connectionValue.element) + ";\n" +
                        context.getIndentationString() + getTabString() + "serverUrl: '" + connectionValue.sourceSpec.url + "'\n" +
                        context.getIndentationString() + getTabString() + "authentication: " + IAuthenticationGrammarComposerExtension.renderAuthentication(connectionValue.authSpec, 1, context) + ";\n" +
                        context.getIndentationString() + "}");
    }
}
