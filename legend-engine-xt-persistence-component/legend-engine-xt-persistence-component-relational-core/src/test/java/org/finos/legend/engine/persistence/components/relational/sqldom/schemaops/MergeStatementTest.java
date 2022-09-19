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

import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.expresssions.table.Table;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.statements.MergeStatement;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Field;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.ObjectValue;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.StringValue;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Value;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MergeStatementTest
{

    @Test
    void genSqlForSimpleMerge()
    {
        Table sourceTable = new Table("mydb", null, "mytable", "main", BaseTest.QUOTE_IDENTIFIER);
        Table targetTable = new Table("mydb", null, "stagetable", "staging", BaseTest.QUOTE_IDENTIFIER);
        List<Pair<Field, Value>> setPairs = Arrays.asList(
            new Pair<>(new Field("col1", BaseTest.QUOTE_IDENTIFIER), new ObjectValue(1)),
            new Pair<>(new Field("col2", BaseTest.QUOTE_IDENTIFIER), new StringValue("one")),
            new Pair<>(new Field("col3", BaseTest.QUOTE_IDENTIFIER), new ObjectValue(3.1)),
            new Pair<>(new Field("col4", BaseTest.QUOTE_IDENTIFIER), new ObjectValue(4L)));

        MergeStatement query = new MergeStatement(
            sourceTable,
            targetTable,
            (long) setPairs.size(),
            setPairs,
            setPairs,
            null,
            null);

        String sql1 = BaseTest.genSqlIgnoringErrors(query);
        String expected = "MERGE INTO \"mydb\".\"mytable\" as main " +
            "USING \"mydb\".\"stagetable\" as staging " +
            "WHEN MATCHED THEN UPDATE SET \"col1\" = 1,\"col2\" = 'one',\"col3\" = 3.1,\"col4\" = 4 " +
            "WHEN NOT MATCHED THEN INSERT (\"col1\", \"col2\", \"col3\", \"col4\") VALUES (1,'one',3.1,4)";
        assertEquals(expected, sql1);
    }
}
