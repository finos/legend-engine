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
import org.finos.legend.engine.persistence.components.relational.sqldom.common.Join;
import org.finos.legend.engine.persistence.components.relational.sqldom.quantifiers.DistinctQuantifier;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.conditions.Condition;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.conditions.comparison.EqualityCondition;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.conditions.comparison.NotEqualCondition;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.conditions.logical.AndCondition;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.expresssions.select.SelectExpression;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.expresssions.table.JoinOperation;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.expresssions.table.Table;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.expresssions.table.TableLike;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.statements.SelectStatement;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.All;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Field;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Function;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.NumericalValue;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.ObjectValue;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Value;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SelectExpressionTest
{

    @Test
    void genSqlForSimpleSelect()
    {
        Table table = new Table("mydb", null, "mytable", null, BaseTest.QUOTE_IDENTIFIER);

        Value item1 = new Field(null, "item1", BaseTest.QUOTE_IDENTIFIER, null);
        Value item2 = new Field(null, "item2", BaseTest.QUOTE_IDENTIFIER, "my_item");

        SelectExpression selectExpression =
            new SelectStatement(
                new DistinctQuantifier(),
                Arrays.asList(item1, item2),
                Collections.singletonList(table),
                null,
                Collections.emptyList());

        String sql1 = BaseTest.genSqlIgnoringErrors(selectExpression);
        String expected = "SELECT DISTINCT \"item1\",\"item2\" as my_item FROM \"mydb\".\"mytable\"";
        assertEquals(expected, sql1);
    }

    @Test
    void genSqlForSimpleSelectStar()
    {
        Table table = new Table("mydb", null, "mytable", null, BaseTest.QUOTE_IDENTIFIER);

        SelectExpression selectExpression =
            new SelectStatement(
                null,
                Collections.singletonList(new All()),
                Collections.singletonList(table),
                null,
                Collections.emptyList());

        String sql1 = BaseTest.genSqlIgnoringErrors(selectExpression);
        String expected = "SELECT * FROM \"mydb\".\"mytable\"";
        assertEquals(expected, sql1);
    }

    @Test
    void genSqlForCondtionalSelect()
    {
        Table table = new Table("mydb", null, "mytable", null, BaseTest.QUOTE_IDENTIFIER);

        Value item1 = new Field(null, "item1", BaseTest.QUOTE_IDENTIFIER, null);
        Value item2 = new Field(null, "item2", BaseTest.QUOTE_IDENTIFIER, "my_item");

        Value value1 = new ObjectValue(100);
        Value value2 = new ObjectValue(50);
        Condition condition = new AndCondition(Arrays.asList(new EqualityCondition(item1, value1), new NotEqualCondition(item2, value2)));

        SelectExpression selectExpression =
            new SelectStatement(
                null,
                Arrays.asList(item1, item2),
                Collections.singletonList(table),
                condition,
                Collections.emptyList());

        String sql1 = BaseTest.genSqlIgnoringErrors(selectExpression);
        String expected = "SELECT \"item1\",\"item2\" as my_item FROM \"mydb\".\"mytable\" WHERE (\"item1\" = 100) AND (\"item2\" <> 50)";
        assertEquals(expected, sql1);
    }

    @Test
    void genSqlForInnerJoin()
    {
        Table leftTable = new Table("mydb", null, "left", "A", BaseTest.QUOTE_IDENTIFIER);
        Table rightTable = new Table("mydb", null, "right", "B", BaseTest.QUOTE_IDENTIFIER);

        Value field1 = new Field(leftTable, "id", BaseTest.QUOTE_IDENTIFIER, null);
        Value field2 = new Field(rightTable, "id", BaseTest.QUOTE_IDENTIFIER, null);
        Condition joinCondition = new EqualityCondition(field1, field2);
        TableLike table = new JoinOperation(leftTable, rightTable, Join.INNER_JOIN, joinCondition);

        Value item1 = new Field(leftTable, "id", BaseTest.QUOTE_IDENTIFIER, null);
        Value item2 = new Field(leftTable, "item2", BaseTest.QUOTE_IDENTIFIER, null);
        Value item3 = new Field(rightTable, "item3", BaseTest.QUOTE_IDENTIFIER, "my_item");

        SelectExpression selectExpression =
            new SelectStatement(
                new DistinctQuantifier(),
                Arrays.asList(item1, item2, item3),
                Collections.singletonList(table),
                null,
                Collections.emptyList());

        String sql1 = BaseTest.genSqlIgnoringErrors(selectExpression);
        String expected = "SELECT DISTINCT A.\"id\",A.\"item2\",B.\"item3\" as my_item FROM \"mydb\".\"left\" as A INNER JOIN \"mydb\".\"right\" as B ON A.\"id\" = B.\"id\"";
        assertEquals(expected, sql1);
    }

    @Test
    void genSqlForSelectSelectItemsMissing()
    {
        Table table = new Table("mydb", null, "mytable", null, BaseTest.QUOTE_IDENTIFIER);
        SelectExpression selectExpression =
            new SelectStatement(
                new DistinctQuantifier(),
                Collections.emptyList(),
                Collections.singletonList(table),
                null,
                Collections.emptyList());
        try
        {
            BaseTest.genSql(selectExpression);
        }
        catch (Exception e)
        {
            String expected = "selectItems is mandatory for Select Statement";
            assertEquals(expected, e.getMessage());
        }
    }

    @Test
    void genSqlForSelectWithConditionAndGroupBy()
    {
        Table table = new Table("mydb", null, "mytable", null, BaseTest.QUOTE_IDENTIFIER);

        Field item1 = new Field(null, "item1", BaseTest.QUOTE_IDENTIFIER, null);
        Field item2 = new Field(null, "item2", BaseTest.QUOTE_IDENTIFIER, "my_item");
        Condition condition = new NotEqualCondition(item2, new NumericalValue(50L));
        Function countFunction = new Function(FunctionName.COUNT, Collections.singletonList(item1));

        SelectExpression selectExpression =
            new SelectStatement(
                null,
                Arrays.asList(countFunction, item2),
                Collections.singletonList(table),
                condition,
                Collections.singletonList(item2));

        String sql = BaseTest.genSqlIgnoringErrors(selectExpression);
        String expected = "SELECT COUNT(\"item1\"),\"item2\" as my_item FROM \"mydb\".\"mytable\" WHERE \"item2\" <> 50 GROUP BY \"item2\"";
        assertEquals(expected, sql);
    }
}
