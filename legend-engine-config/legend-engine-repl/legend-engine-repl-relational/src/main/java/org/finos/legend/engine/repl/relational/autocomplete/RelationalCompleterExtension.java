// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.repl.relational.autocomplete;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperRelationalBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.relation.RelationStoreAccessor;
import org.finos.legend.engine.repl.autocomplete.CompleterExtension;
import org.finos.legend.engine.repl.autocomplete.CompletionItem;
import org.finos.legend.engine.repl.autocomplete.CompletionResult;
import org.finos.legend.engine.repl.autocomplete.parser.ParserFixer;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Schema;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.Table;

public class RelationalCompleterExtension implements CompleterExtension
{
    @Override
    public CompletionResult extraClassInstanceProcessor(Object islandExpr, PureModel pureModel)
    {
        if (islandExpr instanceof RelationStoreAccessor)
        {
            RelationStoreAccessor relationStoreAccessor = (RelationStoreAccessor) islandExpr;
            MutableList<String> path = Lists.adapt(relationStoreAccessor.path);

            if (path.anySatisfy(x -> x.isEmpty() || x.contains(ParserFixer.magicToken)))
            {
                String writtenPath = path.get(0).replace(ParserFixer.magicToken, "");
                MutableList<Store> elements = pureModel.getAllStores().select(c -> nameMatch(c, writtenPath)).toList();

                MutableList<CompletionItem> completionItems = Lists.mutable.empty();

                if (elements.size() == 1 && path.size() > 1)
                {
                    org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database db = (org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database) elements.get(0);

                    if (path.size() < 3)
                    {
                        String tableOrSchema = path.get(1).replace(ParserFixer.magicToken, "").replace("::", "");
                        completionItems.addAll(getTableSuggestions(db, "default", tableOrSchema));
                        completionItems.addAll(getSchemaSuggestions(db, tableOrSchema));
                    }
                    else
                    {
                        String schema = path.get(1);
                        String tableName = path.get(2).replace(ParserFixer.magicToken, "").replace("::", "");
                        completionItems.addAll(getTableSuggestions(db, schema, tableName));
                    }
                }
                else
                {
                    ListIterate.collect(elements, c -> new CompletionItem(org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.getUserPathForPackageableElement(c), ">{" + org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.getUserPathForPackageableElement(c) + '.'), completionItems);
                }

                return new CompletionResult(completionItems);
            }
        }
        return null;
    }

    private static MutableList<CompletionItem> getSchemaSuggestions(Database db, String schemaName)
    {
        MutableList<? extends Schema> foundSchemas = db._schemas().select(schema -> !schema._name().equals("default") && schema._name().startsWith(schemaName)).toSortedListBy(Schema::_name);
        return foundSchemas.collect(c -> new CompletionItem(c._name(), c._name() + "."));
    }

    private static MutableList<CompletionItem> getTableSuggestions(Database db, String schemaName, String tableName)
    {
        SetIterable<Table> tables = HelperRelationalBuilder.getAllTablesInSchema(db, schemaName, SourceInformation.getUnknownSourceInformation());
        MutableList<? extends Table> foundTables = tables.select(c -> c._name().startsWith(tableName)).toSortedListBy(Table::_name);
        return foundTables.collect(c -> new CompletionItem(c._name(), c._name() + "}#"));
    }

    private static boolean nameMatch(PackageableElement c, String writtenPath)
    {
        String path = org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.getUserPathForPackageableElement(c);
        if (path.isEmpty()) // NOTE: handle an edge case where stub store is added to the graph
        {
            return false;
        }
        if (path.length() > writtenPath.length())
        {
            return path.startsWith(writtenPath);
        }
        else
        {
            return writtenPath.startsWith(path);
        }
    }
}
