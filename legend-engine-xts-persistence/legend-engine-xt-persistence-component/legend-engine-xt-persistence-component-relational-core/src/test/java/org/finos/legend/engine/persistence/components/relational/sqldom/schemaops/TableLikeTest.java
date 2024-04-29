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

import org.finos.legend.engine.persistence.components.relational.sqldom.common.Join;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.conditions.Condition;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.conditions.comparison.EqualityCondition;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.expresssions.table.JoinOperation;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.expresssions.table.Table;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.expresssions.table.TableLike;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Field;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Value;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TableLikeTest
{

    @Test
    void genSqlForTableNameExp()
    {
        Table table1 = new Table("mydb", null, "mytable", null, BaseTest.QUOTE_IDENTIFIER);
        Table table2 = new Table("mydb", null, "mytable", "A", BaseTest.QUOTE_IDENTIFIER);
        String sql1 = BaseTest.genSqlIgnoringErrors(table1);
        String sql2 = BaseTest.genSqlIgnoringErrors(table2);
        assertEquals("\"mydb\".\"mytable\"", sql1);
        assertEquals("\"mydb\".\"mytable\" as A", sql2);
    }

    @Test
    void genSqlForSubQueries()
    {
        Table table = new Table("mydb", null, "mytable", null, BaseTest.QUOTE_IDENTIFIER);
    }

    @Test
    void genSqlForJoins()
    {
        Table leftTable = new Table("mydb", null, "left", "A", BaseTest.QUOTE_IDENTIFIER);
        Table rightTable = new Table("mydb", null, "right", "B", BaseTest.QUOTE_IDENTIFIER);

        Value field1 = new Field(leftTable.getAlias(), "id", BaseTest.QUOTE_IDENTIFIER, null);
        Value field2 = new Field(rightTable.getAlias(), "id", BaseTest.QUOTE_IDENTIFIER, null);

        Condition joinCondition = new EqualityCondition(field1, field2);
        TableLike exp = new JoinOperation(leftTable, rightTable, Join.INNER_JOIN, joinCondition);
        String sql = BaseTest.genSqlIgnoringErrors(exp);
        assertEquals("\"mydb\".\"left\" as A INNER JOIN \"mydb\".\"right\" as B ON A.\"id\" = B.\"id\"", sql);
    }
}
