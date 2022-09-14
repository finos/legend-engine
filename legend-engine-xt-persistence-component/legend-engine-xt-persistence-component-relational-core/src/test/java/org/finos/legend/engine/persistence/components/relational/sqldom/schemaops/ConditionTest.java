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

import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.conditions.BetweenCondition;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.conditions.Condition;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.conditions.ExistsCondition;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.conditions.InCondition;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.conditions.IsNotNullCondition;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.conditions.IsNullCondition;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.conditions.LikeCondition;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.conditions.comparison.EqualityCondition;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.conditions.comparison.GreaterThanCondition;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.conditions.comparison.LessThanCondition;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.conditions.comparison.LessThanEqualToCondition;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.conditions.comparison.NotEqualCondition;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.conditions.logical.AndCondition;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.conditions.logical.NotCondition;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.conditions.logical.OrCondition;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.expresssions.select.ArrayExpression;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.expresssions.select.SelectExpression;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.expresssions.table.Table;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.statements.SelectStatement;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Field;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.ObjectValue;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.StringValue;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Value;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConditionTest
{

    @Test
    void testAndCondition()
    {
        Table tableA = new Table("mydb", null, "mytable1", "sink", BaseTest.QUOTE_IDENTIFIER);
        Table tableB = new Table("mydb", null, "mytable2", "stage", BaseTest.QUOTE_IDENTIFIER);

        Value field1 = new Field(tableA, "column_varchar", BaseTest.QUOTE_IDENTIFIER, null);
        Value field2 = new Field(tableB, "column_varchar", BaseTest.QUOTE_IDENTIFIER, null);

        Value field3 = new Field(tableA, "column_timestamp", BaseTest.QUOTE_IDENTIFIER, null);
        Value field4 = new Field(tableB, "column_timestamp", BaseTest.QUOTE_IDENTIFIER, null);
        String expected = "(sink.\"column_varchar\" = stage.\"column_varchar\") AND (sink.\"column_timestamp\" <> stage.\"column_timestamp\")";

        Condition condition = new AndCondition(Arrays.asList(new EqualityCondition(field1, field2), new NotEqualCondition(field3, field4)));
        String sql = BaseTest.genSqlIgnoringErrors(condition);
        assertEquals(expected, sql);
    }

    @Test
    void testOrCondition()
    {
        Table tableA = new Table("mydb", null, "mytable1", "sink", BaseTest.QUOTE_IDENTIFIER);
        Table tableB = new Table("mydb", null, "mytable2", "stage", BaseTest.QUOTE_IDENTIFIER);

        Value field1 = new Field(tableA, "column_varchar", BaseTest.QUOTE_IDENTIFIER, null);
        Value field2 = new Field(tableB, "column_varchar", BaseTest.QUOTE_IDENTIFIER, null);
        Value field3 = new Field(tableA, "column_timestamp", BaseTest.QUOTE_IDENTIFIER, null);
        Value field4 = new Field(tableB, "column_timestamp", BaseTest.QUOTE_IDENTIFIER, null);

        String expected = "(sink.\"column_varchar\" < stage.\"column_varchar\") OR (sink.\"column_timestamp\" <= stage.\"column_timestamp\")";

        Condition condition = new OrCondition(Arrays.asList(new LessThanCondition(field1, field2), new LessThanEqualToCondition(field3, field4)));
        String sql = BaseTest.genSqlIgnoringErrors(condition);
        assertEquals(expected, sql);
    }

    @Test
    void testNotCondition()
    {
        Table tableA = new Table("mydb", null, "mytable1", "sink", BaseTest.QUOTE_IDENTIFIER);
        Table tableB = new Table("mydb", null, "mytable2", "stage", BaseTest.QUOTE_IDENTIFIER);

        Value field1 = new Field(tableA, "column_varchar", BaseTest.QUOTE_IDENTIFIER, null);
        Value field2 = new Field(tableB, "column_varchar", BaseTest.QUOTE_IDENTIFIER, null);

        String expected = "NOT (sink.\"column_varchar\" > stage.\"column_varchar\")";

        Condition condition = new NotCondition(new GreaterThanCondition(field1, field2));
        String sql = BaseTest.genSqlIgnoringErrors(condition);
        assertEquals(expected, sql);
    }

    @Test
    void testBetweenCondition()
    {
        Table tableA = new Table("mydb", null, "mytable1", "sink", BaseTest.QUOTE_IDENTIFIER);
        Value field1 = new Field(tableA, "column_varchar", BaseTest.QUOTE_IDENTIFIER, null);
        ObjectValue value1 = new ObjectValue(1);
        ObjectValue value2 = new ObjectValue(10);

        String expected = "sink.\"column_varchar\" BETWEEN 1 AND 10";
        Condition condition = new BetweenCondition(field1, value1, value2);
        String sql = BaseTest.genSqlIgnoringErrors(condition);
        assertEquals(expected, sql);
    }

    @Test
    void testExistsCondition()
    {
        Table table = new Table("mydb", null, "mytable", null, BaseTest.QUOTE_IDENTIFIER);
        Value item = new Field(null, "item1", BaseTest.QUOTE_IDENTIFIER, null);
        ObjectValue value = new ObjectValue(1);
        Condition condition = new EqualityCondition(item, value);

        SelectExpression selectExpression = new SelectStatement(
            null,
            Collections.singletonList(item),
            Collections.singletonList(table),
            condition,
            Collections.emptyList());

        Condition existsCondition = new ExistsCondition(selectExpression);
        String expected = "EXISTS (SELECT \"item1\" FROM \"mydb\".\"mytable\" WHERE \"item1\" = 1)";
        String sql = BaseTest.genSqlIgnoringErrors(existsCondition);

        assertEquals(expected, sql);
    }

    @Test
    void testInArrayCondition()
    {
        Table tableA = new Table("mydb", null, "mytable1", "sink", BaseTest.QUOTE_IDENTIFIER);
        Value field1 = new Field(tableA, "column_varchar", BaseTest.QUOTE_IDENTIFIER, null);
        ArrayExpression expression = new ArrayExpression(Arrays.asList(new Value[]{new StringValue("IN"), new StringValue("SG"), new StringValue("ID")}));

        String expected = "sink.\"column_varchar\" IN ('IN','SG','ID')";
        Condition condition = new InCondition(field1, expression);
        String sql = BaseTest.genSqlIgnoringErrors(condition);
        assertEquals(expected, sql);
    }

    @Test
    void testInSelectCondition()
    {
        Table tableA = new Table("mydb", null, "mytable1", "sink", BaseTest.QUOTE_IDENTIFIER);
        Table tableB = new Table("mydb", null, "mytable2", "stage", BaseTest.QUOTE_IDENTIFIER);

        Value field1 = new Field(tableA, "col1", BaseTest.QUOTE_IDENTIFIER, null);
        Value field2 = new Field(tableB, "col2", BaseTest.QUOTE_IDENTIFIER, null);

        SelectExpression selectExpression = new SelectStatement(
            null,
            Collections.singletonList(field2),
            Collections.singletonList(tableB),
            null,
            Collections.emptyList());

        String expected = "sink.\"col1\" IN (SELECT stage.\"col2\" FROM \"mydb\".\"mytable2\" as stage)";
        Condition condition = new InCondition(field1, selectExpression);
        String sql = BaseTest.genSqlIgnoringErrors(condition);
        assertEquals(expected, sql);
    }

    @Test
    void testIsNullCondition()
    {
        Table tableA = new Table("mydb", null, "mytable1", "sink", BaseTest.QUOTE_IDENTIFIER);
        Value field1 = new Field(tableA, "column_varchar", BaseTest.QUOTE_IDENTIFIER, null);

        String expected = "sink.\"column_varchar\" IS NULL";
        Condition condition = new IsNullCondition(field1);
        String sql = BaseTest.genSqlIgnoringErrors(condition);
        assertEquals(expected, sql);
    }

    @Test
    void testIsNotNullCondition()
    {
        Table tableA = new Table("mydb", null, "mytable1", "sink", BaseTest.QUOTE_IDENTIFIER);
        Value field1 = new Field(tableA, "column_varchar", BaseTest.QUOTE_IDENTIFIER, null);

        String expected = "sink.\"column_varchar\" IS NOT NULL";
        Condition condition = new IsNotNullCondition(field1);
        String sql = BaseTest.genSqlIgnoringErrors(condition);
        assertEquals(expected, sql);
    }

    @Test
    void testLikeCondition()
    {
        Table tableA = new Table("mydb", null, "mytable1", "sink", BaseTest.QUOTE_IDENTIFIER);
        Value field = new Field(tableA, "column_varchar", BaseTest.QUOTE_IDENTIFIER, null);
        StringValue value = new StringValue("%value");

        String expected = "sink.\"column_varchar\" LIKE '%value'";
        Condition condition = new LikeCondition(field, value);
        String sql = BaseTest.genSqlIgnoringErrors(condition);
        assertEquals(expected, sql);
    }
}
