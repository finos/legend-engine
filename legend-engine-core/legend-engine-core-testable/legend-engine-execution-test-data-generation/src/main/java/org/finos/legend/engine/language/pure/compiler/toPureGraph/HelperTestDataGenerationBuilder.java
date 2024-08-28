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

package org.finos.legend.engine.language.pure.compiler.toPureGraph;

import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.testDataGeneration.ColumnValuePair;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.testDataGeneration.RowIdentifier;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.testDataGeneration.TableRowIdentifiers;
import org.finos.legend.pure.generated.Root_meta_pure_functions_collection_Pair_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_testDataGeneration_RowIdentifier;
import org.finos.legend.pure.generated.Root_meta_relational_testDataGeneration_RowIdentifier_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_testDataGeneration_TableRowIdentifiers;
import org.finos.legend.pure.generated.Root_meta_relational_testDataGeneration_TableRowIdentifiers_Impl;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.Table;

import static org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperRelationalBuilder.resolveDatabase;

public class HelperTestDataGenerationBuilder
{
    public static Root_meta_relational_testDataGeneration_TableRowIdentifiers processTestDataGenerationTableRowIdentifiers(TableRowIdentifiers identifiers, CompileContext context)
    {
        Root_meta_relational_testDataGeneration_TableRowIdentifiers tableRowIdentifiers = new Root_meta_relational_testDataGeneration_TableRowIdentifiers_Impl("");
        Table table = (Table) HelperRelationalBuilder.getRelation(resolveDatabase(identifiers.table.database, identifiers.table.sourceInformation, context), identifiers.table.schema, identifiers.table.table);
        tableRowIdentifiers._table(table);
        if (identifiers.rowIdentifiers == null)
        {
            identifiers.rowIdentifiers = Lists.mutable.empty();
        }
        identifiers.rowIdentifiers.forEach(x ->
                tableRowIdentifiers._rowIdentifiersAdd(processTestDataGenerationTableRowIdentifier(x, context))
        );
        return tableRowIdentifiers;
    }

    private static Root_meta_relational_testDataGeneration_RowIdentifier processTestDataGenerationTableRowIdentifier(RowIdentifier identifier, CompileContext context)
    {
        Root_meta_relational_testDataGeneration_RowIdentifier rowIdentifier = new Root_meta_relational_testDataGeneration_RowIdentifier_Impl("");
        if (identifier.columnValuePairs == null)
        {
            identifier.columnValuePairs = Lists.mutable.empty();
        }
        identifier.columnValuePairs.forEach(x ->
                rowIdentifier._columnValuePairsAdd(processTestDataGenerationTableColumnValuePair(x))
        );
        return rowIdentifier;
    }

    private static Pair processTestDataGenerationTableColumnValuePair(ColumnValuePair valuePair)
    {
        Pair<String, Object> columnValuePair = new Root_meta_pure_functions_collection_Pair_Impl<>("");
        columnValuePair._first(valuePair.name);
        columnValuePair._second(PrimitiveProcessor.process(valuePair.value));
        return columnValuePair;
    }
}