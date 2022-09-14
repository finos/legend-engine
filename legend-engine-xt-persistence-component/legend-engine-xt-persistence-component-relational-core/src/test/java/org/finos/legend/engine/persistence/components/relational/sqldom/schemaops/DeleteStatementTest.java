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

import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.conditions.Condition;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.conditions.comparison.EqualityCondition;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.conditions.comparison.NotEqualCondition;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.conditions.logical.AndCondition;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.expresssions.table.Table;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.statements.DeleteStatement;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Field;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.ObjectValue;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Value;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeleteStatementTest
{

    @Test
    void testDelete()
    {
        Table table = new Table("mydb", null, "mytable", "alias", BaseTest.QUOTE_IDENTIFIER);

        Value item1 = new Field(null, "item1", BaseTest.QUOTE_IDENTIFIER, null);
        Value item2 = new Field(null, "item2", BaseTest.QUOTE_IDENTIFIER, "my_item");

        Value value1 = new ObjectValue(100);
        Value value2 = new ObjectValue(50);
        Condition condition = new AndCondition(Arrays.asList(new EqualityCondition(item1, value1), new NotEqualCondition(item2, value2)));

        DeleteStatement query = new DeleteStatement(table, condition);

        String sql1 = BaseTest.genSqlIgnoringErrors(query);
        String expected = "DELETE FROM \"mydb\".\"mytable\" as alias WHERE (\"item1\" = 100) AND (\"item2\" <> 50)";
        assertEquals(expected, sql1);
    }

    @Test
    void testDeleteWithoutTable()
    {
        DeleteStatement query = new DeleteStatement();

        try
        {
            BaseTest.genSql(query);
        }
        catch (Exception e)
        {
            String expected = "Table is mandatory for Delete Table Command";
            assertEquals(expected, e.getMessage());
        }
    }
}
