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

package org.finos.legend.engine.plan.execution.stores.relational.test;

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.data.RelationalCSVData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.data.RelationalCSVTable;

public class HelperRelationalCSVBuilder
{
    final RelationalCSVData relationalData;

    public HelperRelationalCSVBuilder(RelationalCSVData relationalData)
    {
        this.relationalData = relationalData;
    }

    public String build()
    {
        return ListIterate.collect(this.relationalData.tables, this::generateTableCSV).makeString("----\n");
    }

    private String generateTableCSV(RelationalCSVTable table)
    {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(table.schema);
        stringBuilder.append("\n");
        stringBuilder.append(table.table == null ? "" : table.table);
        stringBuilder.append("\n");
        stringBuilder.append(table.values);
        return stringBuilder.toString();
    }

}
