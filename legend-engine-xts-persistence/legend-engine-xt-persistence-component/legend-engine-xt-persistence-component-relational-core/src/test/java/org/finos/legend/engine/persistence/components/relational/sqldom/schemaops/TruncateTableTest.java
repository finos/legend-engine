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

package org.finos.legend.engine.persistence.components.relational.sqldom.schemaops;

import org.finos.legend.engine.persistence.components.relational.sqldom.SqlDomException;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.expresssions.table.Table;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.statements.TruncateTable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TruncateTableTest
{

    @Test
    void testTruncateQuery()
    {
        Table table = new Table("mydb", null, "mytable", null, BaseTest.QUOTE_IDENTIFIER);
        TruncateTable truncateTable = new TruncateTable(table);

        String sql = BaseTest.genSqlIgnoringErrors(truncateTable);

        String expected = "TRUNCATE TABLE \"mydb\".\"mytable\"";
        assertEquals(expected, sql);
    }

    @Test
    void testTruncateTableWithMissingTables()
    {
        TruncateTable truncateTable = new TruncateTable();
        try
        {
            BaseTest.genSql(truncateTable);
        }
        catch (SqlDomException e)
        {
            String expected = "Table is mandatory for Truncate Table Command";
            assertEquals(expected, e.getMessage());
        }
    }
}