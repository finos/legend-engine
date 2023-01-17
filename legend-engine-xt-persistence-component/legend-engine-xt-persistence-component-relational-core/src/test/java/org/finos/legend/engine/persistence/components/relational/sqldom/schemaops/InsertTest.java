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

import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.expresssions.select.SelectExpression;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.expresssions.select.ValuesExpression;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.expresssions.table.Table;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.statements.InsertStatement;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.statements.SelectStatement;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Field;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.ObjectValue;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.StringValue;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Value;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InsertTest
{

    @Test
    void testInsertWithValues()
    {

        List<Field> columns = Arrays.asList(
            new Field("col1", BaseTest.QUOTE_IDENTIFIER),
            new Field("col2", BaseTest.QUOTE_IDENTIFIER),
            new Field("col3", BaseTest.QUOTE_IDENTIFIER),
            new Field("col4", BaseTest.QUOTE_IDENTIFIER)
        );
        Table table = new Table("mydb", null, "mytable", null, BaseTest.QUOTE_IDENTIFIER);
        List<List<Value>> values = new ArrayList<>();
        values.add(Arrays.asList(new ObjectValue(1, BaseTest.QUOTE_IDENTIFIER), new StringValue("2", BaseTest.QUOTE_IDENTIFIER), new ObjectValue(3.05, BaseTest.QUOTE_IDENTIFIER), new ObjectValue(4L, BaseTest.QUOTE_IDENTIFIER)));
        values.add(Arrays.asList(new ObjectValue(11, BaseTest.QUOTE_IDENTIFIER), new StringValue("22", BaseTest.QUOTE_IDENTIFIER), new ObjectValue(33.05, BaseTest.QUOTE_IDENTIFIER), new ObjectValue(44L, BaseTest.QUOTE_IDENTIFIER)));

        ValuesExpression valuesExpression = new ValuesExpression(values);

        InsertStatement insertStatement = new InsertStatement(table, columns, valuesExpression);

        String sql = BaseTest.genSqlIgnoringErrors(insertStatement);

        assertEquals("INSERT INTO \"mydb\".\"mytable\" (\"col1\", \"col2\", \"col3\", \"col4\") (VALUES (1,'2',3.05,4),(11,'22',33.05,44))", sql);
    }

    @Test
    void testInsertWithSelect()
    {
        List<Field> columns = Arrays.asList(
            new Field("convolve_digest", BaseTest.QUOTE_IDENTIFIER),
            new Field("column_smallint", BaseTest.QUOTE_IDENTIFIER),
            new Field("column_tinyint", BaseTest.QUOTE_IDENTIFIER),
            new Field("column_bigint", BaseTest.QUOTE_IDENTIFIER),
            new Field("column_varchar", BaseTest.QUOTE_IDENTIFIER),
            new Field("column_char", BaseTest.QUOTE_IDENTIFIER),
            new Field("column_timestamp", BaseTest.QUOTE_IDENTIFIER),
            new Field("column_date", BaseTest.QUOTE_IDENTIFIER),
            new Field("column_float", BaseTest.QUOTE_IDENTIFIER),
            new Field("column_real", BaseTest.QUOTE_IDENTIFIER),
            new Field("column_decimal", BaseTest.QUOTE_IDENTIFIER),
            new Field("column_double", BaseTest.QUOTE_IDENTIFIER)
        );
        Table tableToInsert = new Table("CONVOLVE_TEST_SCHEMA", null, "CONVOLVE_TEST_TABLE", null, BaseTest.QUOTE_IDENTIFIER);
        Table tableToSelect = new Table("CONVOLVE_TEST_SCHEMA", null, "CONVOLVE_TEST_TABLE__stage", "stage_left", BaseTest.QUOTE_IDENTIFIER);

        Value item1 = new Field(tableToSelect.getAlias(), "convolve_digest", BaseTest.QUOTE_IDENTIFIER, "convolve_digest");
        Value item2 = new Field(tableToSelect.getAlias(), "column_smallint", BaseTest.QUOTE_IDENTIFIER, "column_smallint");
        Value item3 = new Field(tableToSelect.getAlias(), "column_tinyint", BaseTest.QUOTE_IDENTIFIER, "column_tinyint");
        Value item4 = new Field(tableToSelect.getAlias(), "column_bigint", BaseTest.QUOTE_IDENTIFIER, "column_bigint");
        Value item5 = new Field(tableToSelect.getAlias(), "column_varchar", BaseTest.QUOTE_IDENTIFIER, "column_varchar");
        Value item6 = new Field(tableToSelect.getAlias(), "column_char", BaseTest.QUOTE_IDENTIFIER, "column_char");
        Value item7 = new Field(tableToSelect.getAlias(), "column_timestamp", BaseTest.QUOTE_IDENTIFIER, "column_timestamp");
        Value item8 = new Field(tableToSelect.getAlias(), "column_date", BaseTest.QUOTE_IDENTIFIER, "column_date");
        Value item9 = new Field(tableToSelect.getAlias(), "column_float", BaseTest.QUOTE_IDENTIFIER, "column_float");
        Value item10 = new Field(tableToSelect.getAlias(), "column_real", BaseTest.QUOTE_IDENTIFIER, "column_real");
        Value item11 = new Field(tableToSelect.getAlias(), "column_decimal", BaseTest.QUOTE_IDENTIFIER, "column_decimal");
        Value item12 = new Field(tableToSelect.getAlias(), "column_double", BaseTest.QUOTE_IDENTIFIER, "column_double");

        SelectExpression selectExpression = new SelectStatement(
            null,
            Arrays.asList(item1, item2, item3, item4, item5, item6, item7, item8, item9, item10, item11, item12),
            Collections.singletonList(tableToSelect),
            null,
            Collections.emptyList());

        InsertStatement insertStatement = new InsertStatement(tableToInsert, columns, selectExpression);

        String sql = BaseTest.genSqlIgnoringErrors(insertStatement);

        String expected = "INSERT INTO \"CONVOLVE_TEST_SCHEMA\".\"CONVOLVE_TEST_TABLE\" (\"convolve_digest\", \"column_smallint\", \"column_tinyint\", \"column_bigint\", \"column_varchar\", \"column_char\", \"column_timestamp\", \"column_date\", \"column_float\", \"column_real\", \"column_decimal\", \"column_double\") (SELECT stage_left.\"convolve_digest\" as \"convolve_digest\",stage_left.\"column_smallint\" as \"column_smallint\",stage_left.\"column_tinyint\" as \"column_tinyint\",stage_left.\"column_bigint\" as \"column_bigint\",stage_left.\"column_varchar\" as \"column_varchar\",stage_left.\"column_char\" as \"column_char\",stage_left.\"column_timestamp\" as \"column_timestamp\",stage_left.\"column_date\" as \"column_date\",stage_left.\"column_float\" as \"column_float\",stage_left.\"column_real\" as \"column_real\",stage_left.\"column_decimal\" as \"column_decimal\",stage_left.\"column_double\" as \"column_double\" FROM \"CONVOLVE_TEST_SCHEMA\".\"CONVOLVE_TEST_TABLE__stage\" as stage_left)";
        assertEquals(expected, sql);
    }

    @Test
    void testInsertTableMissing()
    {
        List<Field> columns = Arrays.asList(
            new Field("col1", BaseTest.QUOTE_IDENTIFIER),
            new Field("col2", BaseTest.QUOTE_IDENTIFIER),
            new Field("col3", BaseTest.QUOTE_IDENTIFIER),
            new Field("col4", BaseTest.QUOTE_IDENTIFIER));
        List<List<Value>> values = new ArrayList<>();
        values.add(Arrays.asList(new ObjectValue(1, BaseTest.QUOTE_IDENTIFIER), new StringValue("2", BaseTest.QUOTE_IDENTIFIER), new ObjectValue(3.05, BaseTest.QUOTE_IDENTIFIER), new ObjectValue(4L, BaseTest.QUOTE_IDENTIFIER)));
        values.add(Arrays.asList(new ObjectValue(11, BaseTest.QUOTE_IDENTIFIER), new StringValue("22", BaseTest.QUOTE_IDENTIFIER), new ObjectValue(33.05, BaseTest.QUOTE_IDENTIFIER), new ObjectValue(44L, BaseTest.QUOTE_IDENTIFIER)));

        ValuesExpression valuesExpression = new ValuesExpression(values);

        InsertStatement insertStatement = new InsertStatement(null, columns, valuesExpression);

        try
        {
            BaseTest.genSql(insertStatement);
        }
        catch (Exception e)
        {
            String expected = "table is mandatory for Create Table Command";
            assertEquals(expected, e.getMessage());
        }
    }

    @Test
    void testInsertValuesMissing()
    {
        List<Field> columns = Arrays.asList(
            new Field("col1", BaseTest.QUOTE_IDENTIFIER),
            new Field("col2", BaseTest.QUOTE_IDENTIFIER),
            new Field("col3", BaseTest.QUOTE_IDENTIFIER),
            new Field("col4", BaseTest.QUOTE_IDENTIFIER));

        Table table = new Table("mydb", null, "mytable", null, BaseTest.QUOTE_IDENTIFIER);

        InsertStatement insertStatement = new InsertStatement(table, columns, null);

        try
        {
            BaseTest.genSql(insertStatement);
        }
        catch (Exception e)
        {
            String expected = "selectExpression is mandatory for Select Statement";
            assertEquals(expected, e.getMessage());
        }
    }
}
