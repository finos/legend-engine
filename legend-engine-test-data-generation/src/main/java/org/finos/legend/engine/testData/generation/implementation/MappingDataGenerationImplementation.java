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

package org.finos.legend.engine.testData.generation.implementation;

import java.util.Collections;
import java.util.List;
import org.eclipse.collections.api.RichIterable;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.data.RelationalCSVData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.data.RelationalCSVTable;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.testData.generation.model.MappingDataGenerationRequest;
import org.finos.legend.pure.generated.core_relational_store_entitlement_utility_relationalTableAnalyzer;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.Table;

public class MappingDataGenerationImplementation
{

    public static List<EmbeddedData> buildData(MappingDataGenerationRequest request, PureModel model, PureModelContextData data)
    {
        if (request.query != null)
        {
            throw new EngineException("Mapping Data Generation does not currently support query");
        }
        if (request.connection != null)
        {
            throw new EngineException("Mapping Data Generation does not currently support connection");
        }
        List<EmbeddedData> mockData = attemptToGenerateRelationalTables(model.getMapping(request.mapping), model, data);
        if (mockData == null)
        {
            return Collections.emptyList();
        }
        return mockData;
    }

    public static List<EmbeddedData> attemptToGenerateRelationalTables(Mapping mapping, PureModel pureModel, PureModelContextData pureModelContextData)
    {
        RichIterable<? extends Table> tables = core_relational_store_entitlement_utility_relationalTableAnalyzer.Root_meta_analytics_store_entitlements_getTablesFromMapping_Mapping_1__Table_MANY_(
            mapping, pureModel.getExecutionSupport());
        if (tables.isEmpty())
        {
            return null;
        }
        List<RelationalCSVTable> relationalCSVTables = tables.collect(table ->
        {
            RelationalCSVTable relationalCSVTable = new RelationalCSVTable();
            relationalCSVTable.schema = table._schema()._name();
            relationalCSVTable.table = table._name();
            relationalCSVTable.values = "";
            return relationalCSVTable;
        }).toList();

        RelationalCSVData data = new RelationalCSVData();
        data.tables = relationalCSVTables;
        return Collections.singletonList(data);
    }
}
