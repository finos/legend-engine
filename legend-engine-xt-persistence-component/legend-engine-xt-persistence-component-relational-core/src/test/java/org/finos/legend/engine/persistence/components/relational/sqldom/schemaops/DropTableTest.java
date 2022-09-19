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
import org.finos.legend.engine.persistence.components.relational.sqldom.constraints.table.CascadeTableConstraint;
import org.finos.legend.engine.persistence.components.relational.sqldom.modifiers.IfExistsTableModifier;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.expresssions.table.Table;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.statements.DropTable;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DropTableTest
{

    @Test
    void testDropQueryWithModifiersAndConstraints()
    {
        Table table = new Table("mydb", null, "mytable", null, BaseTest.QUOTE_IDENTIFIER);
        DropTable drop = new DropTable(
            table,
            Collections.singletonList(new IfExistsTableModifier()),
            Collections.singletonList(new CascadeTableConstraint()));

        String sql1 = BaseTest.genSqlIgnoringErrors(drop);

        String expected = "DROP TABLE IF EXISTS \"mydb\".\"mytable\" CASCADE";
        assertEquals(expected, sql1);
    }

    @Test
    void testDropTableWithMissingTables()
    {
        DropTable drop = new DropTable();
        try
        {
            BaseTest.genSql(drop);
        }
        catch (SqlDomException e)
        {
            String expected = "Table is mandatory for Drop Table Command";
            assertEquals(expected, e.getMessage());
        }
    }

    @Test
    void testDropTableWithMissingTableName()
    {
        Table table = new Table("mydb", null, null, null, BaseTest.QUOTE_IDENTIFIER);
        DropTable drop = new DropTable(table, Collections.emptyList(), Collections.emptyList());
        try
        {
            BaseTest.genSql(drop);
        }
        catch (SqlDomException e)
        {
            String expected = "Table name is mandatory";
            assertEquals(expected, e.getMessage());
        }
    }
}
