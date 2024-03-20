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
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.relation.RelationStoreAccessor;
import org.finos.legend.engine.repl.autocomplete.CompleterExtension;
import org.finos.legend.engine.repl.autocomplete.CompletionItem;
import org.finos.legend.engine.repl.autocomplete.CompletionResult;
import org.finos.legend.engine.repl.autocomplete.parser.ParserFixer;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store;

public class RelationalCompleterExtension implements CompleterExtension
{
    @Override
    public CompletionResult extraClassInstanceProcessor(Object islandExpr, PureModel pureModel)
    {
        if (islandExpr instanceof RelationStoreAccessor)
        {
            MutableList<String> path = Lists.mutable.withAll(((RelationStoreAccessor) islandExpr).path);
            String writtenPath = path.makeString("::").replace(ParserFixer.magicToken, "");
            MutableList<Store> elements = pureModel.getAllStores().select(c -> nameMatch(c, writtenPath)).toList();
            if (elements.size() == 1 &&
                    writtenPath.startsWith(org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.getUserPathForPackageableElement(elements.get(0))) &&
                    !writtenPath.equals(org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.getUserPathForPackageableElement(elements.get(0)))
            )
            {
                org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database db = (org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database) elements.get(0);
                String writtenTableName = writtenPath.replace(org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.getUserPathForPackageableElement(db), "").replace("::", "");
                MutableList<? extends org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.Table> tables = db._schemas().isEmpty() ? Lists.mutable.empty() : db._schemas().getFirst()._tables().toList();
                MutableList<? extends org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.Table> foundTables = tables.select(c -> c._name().startsWith(writtenTableName));
                if ((foundTables.size() == 1 && foundTables.get(0)._name().equals(path.getLast())))
                {
                    return new CompletionResult(Lists.mutable.empty());
                }
                else
                {
                    return new CompletionResult(foundTables.collect(c -> new CompletionItem(c._name(), c._name() + "}")));
                }
            }
            return new CompletionResult(ListIterate.collect(elements, c -> new CompletionItem(org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.getUserPathForPackageableElement(c), ">{" + org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.getUserPathForPackageableElement(c))).toList());
        }
        return null;
    }

    private static boolean nameMatch(PackageableElement c, String writtenPath)
    {
        String path = org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.getUserPathForPackageableElement(c);
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
