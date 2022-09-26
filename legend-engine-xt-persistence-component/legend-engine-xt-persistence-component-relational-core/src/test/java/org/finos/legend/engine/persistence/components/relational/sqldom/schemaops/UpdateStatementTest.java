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
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.conditions.ExistsCondition;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.conditions.comparison.EqualityCondition;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.expresssions.table.Table;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.statements.SelectStatement;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.statements.UpdateStatement;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.All;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Field;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.NumericalValue;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.ObjectValue;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.SelectValue;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.StringValue;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Value;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UpdateStatementTest
{

    @Test
    void genSqlForSimpleUpdate()
    {
        Table table = new Table("mydb", null, "mytable", "alias", BaseTest.QUOTE_IDENTIFIER);
        List<Pair<Field, Value>> setPairs = Arrays.asList(
            new Pair<>(new Field("col1", BaseTest.QUOTE_IDENTIFIER), new ObjectValue(1)),
            new Pair<>(new Field("col2", BaseTest.QUOTE_IDENTIFIER), new StringValue("one")),
            new Pair<>(new Field("col3", BaseTest.QUOTE_IDENTIFIER), new ObjectValue(3.1)),
            new Pair<>(new Field("col4", BaseTest.QUOTE_IDENTIFIER), new ObjectValue(4L))
        );

        UpdateStatement query = new UpdateStatement(table, setPairs, null);

        String sql1 = BaseTest.genSqlIgnoringErrors(query);
        String expected = "UPDATE \"mydb\".\"mytable\" as alias " +
            "SET \"col1\" = 1," +
            "\"col2\" = 'one'," +
            "\"col3\" = 3.1," +
            "\"col4\" = 4";
        assertEquals(expected, sql1);
    }

    @Test
    void genSqlForSimpleUpdateWithWhereClause()
    {
        Table table = new Table("mydb", null, "mytable", "alias", BaseTest.QUOTE_IDENTIFIER);
        List<Pair<Field, Value>> setPairs = Arrays.asList(
            new Pair<>(new Field("col1", BaseTest.QUOTE_IDENTIFIER), new ObjectValue(1)),
            new Pair<>(new Field("col2", BaseTest.QUOTE_IDENTIFIER), new StringValue("one")),
            new Pair<>(new Field("col3", BaseTest.QUOTE_IDENTIFIER), new ObjectValue(3.1)),
            new Pair<>(new Field("col4", BaseTest.QUOTE_IDENTIFIER), new ObjectValue(4L))
        );
        Condition condition = new EqualityCondition(
            new Field(table.getAlias(), "id", BaseTest.QUOTE_IDENTIFIER, null),
            new NumericalValue(1L));

        UpdateStatement query = new UpdateStatement(table, setPairs, condition);

        String sql1 = BaseTest.genSqlIgnoringErrors(query);
        String expected = "UPDATE \"mydb\".\"mytable\" as alias " +
            "SET \"col1\" = 1," +
            "\"col2\" = 'one'," +
            "\"col3\" = 3.1," +
            "\"col4\" = 4 " +
            "WHERE alias.\"id\" = 1";
        assertEquals(expected, sql1);
    }

    @Test
    void genSqlForUpdateWithJoin()
    {
        Table table = new Table("my_db", null, "tableA", "A", BaseTest.QUOTE_IDENTIFIER);
        Table joinTable = new Table("my_db", null, "tableB", "B", BaseTest.QUOTE_IDENTIFIER);

        Condition pkMatchCondition = new EqualityCondition(
            new Field(table.getAlias(), "id", BaseTest.QUOTE_IDENTIFIER, null),
            new Field(joinTable.getAlias(), "a_id", BaseTest.QUOTE_IDENTIFIER, null));

        List<Pair<Field, Value>> setPairs = Arrays.asList(
            new Pair<>(new Field("col1", BaseTest.QUOTE_IDENTIFIER), new SelectValue(
                new SelectStatement(
                    null,
                    Collections.singletonList(new Field(joinTable.getAlias(), "col1", BaseTest.QUOTE_IDENTIFIER, null)),
                    Collections.singletonList(joinTable),
                    pkMatchCondition,
                    Collections.emptyList()))),
            new Pair<>(new Field("col2", BaseTest.QUOTE_IDENTIFIER), new SelectValue(
                new SelectStatement(
                    null,
                    Collections.singletonList(new Field(joinTable.getAlias(), "col2", BaseTest.QUOTE_IDENTIFIER, null)),
                    Collections.singletonList(joinTable),
                    pkMatchCondition,
                    Collections.emptyList()))));

        Condition whereCondition = new ExistsCondition(
            new SelectStatement(
                null,
                Collections.singletonList(new All()),
                Collections.singletonList(joinTable),
                pkMatchCondition,
                Collections.emptyList()));

        UpdateStatement query = new UpdateStatement(table, setPairs, whereCondition);

        String sql1 = BaseTest.genSqlIgnoringErrors(query);
        String expected = "UPDATE \"my_db\".\"tableA\" as A " +
            "SET \"col1\" = (SELECT B.\"col1\" FROM \"my_db\".\"tableB\" as B WHERE A.\"id\" = B.\"a_id\")," +
            "\"col2\" = (SELECT B.\"col2\" FROM \"my_db\".\"tableB\" as B WHERE A.\"id\" = B.\"a_id\") " +
            "WHERE EXISTS (SELECT * FROM \"my_db\".\"tableB\" as B WHERE A.\"id\" = B.\"a_id\")";
        assertEquals(expected, sql1);
    }
}
