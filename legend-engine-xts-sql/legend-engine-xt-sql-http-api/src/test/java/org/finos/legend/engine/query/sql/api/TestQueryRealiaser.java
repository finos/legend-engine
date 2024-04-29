// Copyright 2023 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package org.finos.legend.engine.query.sql.api;

import org.finos.legend.engine.language.sql.grammar.from.SQLGrammarParser;
import org.finos.legend.engine.language.sql.grammar.to.SQLGrammarComposer;
import org.finos.legend.engine.protocol.sql.metamodel.Query;
import org.junit.Assert;
import org.junit.Test;

public class TestQueryRealiaser
{

    @Test
    public void testSelect()
    {
        test("select a from myTable as t1");

        test("select t1.a from myTable as t1");

        test("select myTable.a from myTable",
                "select t1.a from myTable as t1");

        test("select a from myTable",
                "select a from myTable as t1");
    }

    @Test
    public void testUnion()
    {
        test("select a from myTable as t1 union select a from myTable as t2");
        test("select a from myTable as t1 union select a from myTable as t2");
        test("select a from myTable union select a from myTable", "select a from myTable as t1 union select a from myTable as t2");

        test("select * from (select a from myTable as t1 union select a from myTable as t2) t1",
                "select * from (select a from myTable as t1_1 union select a from myTable as t2_2) as t1");

        test("select * from (select t1.a from myTable as t1 union select t2.a from myTable as t2) t1",
                "select * from (select t1_1.a from myTable as t1_1 union select t2_2.a from myTable as t2_2) as t1");
    }

    @Test
    public void testJoin()
    {
        test("select t1.a, t2.b from myTable as t1 left outer join myTable2 as t2 on (t1.a = t2.b)");

        test("select t1.a, t2.b from myTable as t1 left outer join (select t1.* from myTable as t1) as t2 on (t1.a = t2.b)",
                "select t1.a, t2.b from myTable as t1 left outer join (select t1_1.* from myTable as t1_1) as t2 on (t1.a = t2.b)");
    }

    @Test
    public void testSubQuery()
    {
        test("select * from (select a from (select * from myTable left outer join myTable2 on myTable.a = myTable2.b))",
                "select * from (select a from (select * from myTable as t1 left outer join myTable2 as t2 on (t1.a = t2.b)))");

        test("select * from (select a from (select * from myTable left outer join myTable2 on myTable.a = myTable2.b) as myTable) as myTable",
                "select * from (select a from (select * from myTable as t1 left outer join myTable2 as t2 on (t1.a = t2.b)) as myTable_3) as myTable");
    }

    private void test(String input)
    {
        test(input, input);
    }

    private void test(String input, String expected)
    {
        Query query = (Query) SQLGrammarParser.newInstance().parseStatement(input);

        Query output = QueryRealiaser.realias(query);

        String grammar = SQLGrammarComposer.newInstance().renderNode(output);

        Assert.assertEquals(expected.toLowerCase(), grammar.toLowerCase());
    }
}