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

import org.finos.legend.engine.persistence.components.relational.sqldom.common.FunctionName;
import org.finos.legend.engine.persistence.components.relational.sqldom.common.Operator;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.expresssions.table.Table;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Expression;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Field;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Function;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.ObjectValue;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.StringValue;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Value;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ValueTest
{

    @Test
    void testFieldValue()
    {
        Table tableA = new Table(null, null, "mytable", "A", BaseTest.QUOTE_IDENTIFIER);
        Table tableB = new Table(null, null, "mytable", "B", BaseTest.QUOTE_IDENTIFIER);

        Value item1 = new Field(null, "id", BaseTest.QUOTE_IDENTIFIER, null);
        Value item2 = new Field(tableA.getAlias(), "item2", BaseTest.QUOTE_IDENTIFIER, "my_item");
        Value item3 = new Field(tableB.getAlias(), "item3", BaseTest.QUOTE_IDENTIFIER, "my_item");

        String sql1 = BaseTest.genSqlIgnoringErrors(item1);
        String sql2 = BaseTest.genSqlIgnoringErrors(item2);
        String sql3 = BaseTest.genSqlIgnoringErrors(item3);

        assertEquals("\"id\"", sql1);
        assertEquals("A.\"item2\" as my_item", sql2);
        assertEquals("B.\"item3\" as my_item", sql3);
    }

    @Test
    void testValueValue()
    {
        Value item1 = new ObjectValue(4, "val");
        Value item2 = new StringValue("four", "val");
        Value item3 = new ObjectValue(3.7, "val");

        String sql1 = BaseTest.genSqlIgnoringErrors(item1);
        String sql2 = BaseTest.genSqlIgnoringErrors(item2);
        String sql3 = BaseTest.genSqlIgnoringErrors(item3);

        assertEquals("4 as val", sql1);
        assertEquals("'four' as val", sql2);
        assertEquals("3.7 as val", sql3);
    }

    @Test
    void testSimpleSelectExpression()
    {
        Value item1 = new Field(null, "id", BaseTest.QUOTE_IDENTIFIER, null);
        Value item2 = new ObjectValue(4);
        Value item = new Expression(item1, item2, Operator.PLUS, "temp");

        String sql1 = BaseTest.genSqlIgnoringErrors(item);
        assertEquals("\"id\"+4 as temp", sql1);
    }

    @Test
    void testSelectExpressionWithFunction()
    {
        Value id = new Field(null, "id", BaseTest.QUOTE_IDENTIFIER, null);
        Value item = new ObjectValue(4);
        Function function = new Function(FunctionName.SUM, Arrays.asList(id));
        Value exp = new Expression(function, item, Operator.PLUS, "temp");
        String sql1 = BaseTest.genSqlIgnoringErrors(exp);
        assertEquals("SUM(\"id\")+4 as temp", sql1);
    }
}
