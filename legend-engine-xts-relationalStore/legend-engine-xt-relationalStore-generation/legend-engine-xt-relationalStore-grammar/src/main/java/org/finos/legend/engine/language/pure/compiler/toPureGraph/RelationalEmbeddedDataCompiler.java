// Copyright 2022 Goldman Sachs
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

import java.util.List;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.data.RelationalCSVData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.data.RelationalCSVTable;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_pure_data_EmbeddedData;
import org.finos.legend.pure.generated.Root_meta_relational_metamodel_data_RelationalCSVData;
import org.finos.legend.pure.generated.Root_meta_relational_metamodel_data_RelationalCSVData_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_metamodel_data_RelationalCSVTable;
import org.finos.legend.pure.generated.Root_meta_relational_metamodel_data_RelationalCSVTable_Impl;

public class RelationalEmbeddedDataCompiler
{

    public static Root_meta_pure_data_EmbeddedData compileRelationalEmbeddedDataCompiler(EmbeddedData embeddedData, CompileContext context, ProcessingContext processingContext)
    {
        if (embeddedData instanceof RelationalCSVData)
        {
            RelationalCSVData relationalData = (RelationalCSVData) embeddedData;
            Root_meta_relational_metamodel_data_RelationalCSVData data_relationalData = new Root_meta_relational_metamodel_data_RelationalCSVData_Impl("", null, context.pureModel.getClass("meta::relational::metamodel::data::RelationalCSVData"));
            MutableMap<String, List<String>> schemaTableMap = Maps.mutable.empty();
            data_relationalData._tables(ListIterate.collect(relationalData.tables, t -> compileTable(t, schemaTableMap, context)));
            return data_relationalData;
        }
        return null;
    }

    public static Root_meta_relational_metamodel_data_RelationalCSVTable compileTable(RelationalCSVTable element, MutableMap<String, List<String>> schemaTableMap, CompileContext context)
    {
        if (schemaTableMap.get(element.schema) != null)
        {
            List<String> tables = schemaTableMap.get(element.schema);
            if (tables.contains(element.table))
            {
                throw new EngineException("Duplicated table name: '" + element.schema + "." + element.table + "'", element.sourceInformation, EngineErrorType.COMPILATION);
            }
            tables.add(element.table);
        }
        else
        {
            schemaTableMap.put(element.schema, Lists.mutable.with(element.table));
        }
        Root_meta_relational_metamodel_data_RelationalCSVTable table = new Root_meta_relational_metamodel_data_RelationalCSVTable_Impl("", null, context.pureModel.getClass("meta::relational::metamodel::data::RelationalCSVTable"));
        table._table(element.table);
        table._schema(element.schema);
        table._values(element.values);
        return table;
    }

}