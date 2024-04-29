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

package org.finos.legend.engine.plan.execution.stores.relational.test.data;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.plan.execution.stores.relational.test.HelperRelationalCSVBuilder;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.data.RelationalCSVData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.data.RelationalCSVTable;
import org.junit.Assert;
import org.junit.Test;

public class TestHelperRelationalCSVBuilder
{

    @Test
    public void testMultiTableCSVGeneration()
    {
        String expectedSingle =
                "default\n" +
                        "PersonTable\n" +
                        "id,firm_id,firstName,lastName\n" +
                        "1,1,John,Doe\n" +
                        "2,1,Nicole,Smith\n" +
                        "3,2,Tim,Smith\n";

        String expected =
                        expectedSingle +
                        "----\n" +
                        "default\n" +
                        "FirmTable\n" +
                        "id,legal_Name\n" +
                        "1,Finos\n" +
                        "2,Apple\n";

        RelationalCSVData relationalData = new RelationalCSVData();
        relationalData.tables = Lists.mutable.empty();
        RelationalCSVTable table1 = new RelationalCSVTable();
        table1.table = "PersonTable";
        table1.schema = "default";
        table1.values = "id,firm_id,firstName,lastName\n1,1,John,Doe\n2,1,Nicole,Smith\n3,2,Tim,Smith\n";
        relationalData.tables.add(table1);
        Assert.assertEquals(expectedSingle, new HelperRelationalCSVBuilder(relationalData).build());

        RelationalCSVTable table2 = new RelationalCSVTable();
        table2.table = "FirmTable";
        table2.schema = "default";
        table2.values = "id,legal_Name\n1,Finos\n2,Apple\n";
        relationalData.tables.add(table2);
        Assert.assertEquals(expected, new HelperRelationalCSVBuilder(relationalData).build());
    }

}
