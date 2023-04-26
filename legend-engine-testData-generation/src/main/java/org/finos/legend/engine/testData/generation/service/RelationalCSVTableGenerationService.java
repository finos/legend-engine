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

package org.finos.legend.engine.testData.generation.service;

import org.eclipse.collections.api.RichIterable;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.data.RelationalCSVTable;
import org.finos.legend.pure.generated.core_relational_store_entitlement_utility_relationalTableAnalyzer;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.Table;

import java.util.List;

public class RelationalCSVTableGenerationService
{
    public List<RelationalCSVTable> generateRelationalCSVTable(Mapping mapping, PureModel pureModel)
    {
        RichIterable<? extends Table> tables =  core_relational_store_entitlement_utility_relationalTableAnalyzer.Root_meta_analytics_store_entitlements_getTablesFromMapping_Mapping_1__Table_MANY_(mapping, pureModel.getExecutionSupport());
        return tables.collect(table ->
        {
           RelationalCSVTable relationalCSVTable = new RelationalCSVTable();
           relationalCSVTable.schema = table._schema()._name();
           relationalCSVTable.table = table._name();
           relationalCSVTable.values = "";
           return relationalCSVTable;
        }).toList();
    }
}
