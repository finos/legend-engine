// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.language.sql.grammar.test.roundtrip;

import org.finos.legend.engine.language.sql.grammar.from.SQLGrammarParser;
import org.finos.legend.engine.language.sql.grammar.from.SQLParserException;
import org.finos.legend.engine.language.sql.grammar.to.SQLGrammarComposer;
import org.finos.legend.engine.protocol.sql.metamodel.Node;
import org.hamcrest.MatcherAssert;
import org.hamcrest.text.IsEqualIgnoringCase;
import org.junit.Assert;
import org.junit.Test;

public class TestSQLRoundTrip
{
    @Test
    public void testEmptyStatement()
    {
        fail("", 1, 1, "Unexpected token");
    }

    @Test
    public void testSelectStar()
    {
        check("SELECT * FROM table");
    }

    @Test
    public void testSelectStarFromNoParamTableFunc()
    {
        check("SELECT * FROM table()");
    }

    @Test
    public void testSelectStarFromTableFuncWithParams()
    {
        check("SELECT * FROM table(1, a => 1, b => [1, 2, 3])");
    }

    @Test
    public void testSelectColumns()
    {
        check("SELECT col1, col2 FROM table");
    }

    @Test
    public void testSelectWithAliases()
    {
        check("SELECT col1 AS COL FROM table AS table1");
    }

    @Test
    public void testSelectQualified()
    {
        check("SELECT col1, table.col2 FROM table");
    }

    @Test
    public void testSelectQualifiedWithAlias()
    {
        check("SELECT col1, table1.col2 FROM table AS table1");
    }

    @Test
    public void testDistinct()
    {
        check("SELECT DISTINCT * FROM table");
    }

    @Test
    public void testLimit()
    {
        check("SELECT * FROM table LIMIT 1");
    }

    @Test
    public void testOrderBy()
    {
        check("SELECT * FROM table ORDER BY col1 DESC, col2 ASC");
    }

    @Test
    public void testOrderByDefaultASC()
    {
        check("SELECT * FROM table ORDER BY col1 DESC, col2", "SELECT * FROM table ORDER BY col1 DESC, col2 ASC");
    }

    @Test
    public void testOrderByAlias()
    {
        check("SELECT * FROM table AS table1 ORDER BY table1.col1 DESC, col2 ASC");
    }

    @Test
    public void testOrderByQualified()
    {
        check("SELECT * FROM table ORDER BY table.col1 DESC, col2 ASC");
    }

    @Test
    public void testOrderByQualifiedWithAlias()
    {
        check("SELECT * FROM table AS table1 ORDER BY table1.col1 DESC, col2 ASC");
    }

    @Test
    public void testWhere()
    {
        check("SELECT * FROM table WHERE col1 = 1");
    }

    @Test
    public void testCompositeWhere()
    {
        check("SELECT * FROM table WHERE col1 = 1 AND col2 = 1");
    }

    @Test
    public void testWhereQualified()
    {
        check("SELECT * FROM table WHERE table.col1 = 1");
    }

    @Test
    public void testCompositeWhereQualifiedWithAlias()
    {
        check("SELECT * FROM table AS table1 WHERE col = 1 AND table1.col = 1");
    }

    @Test
    public void testCompositeWhereOperators()
    {
        check("SELECT * FROM table WHERE col = 1 AND col > 1 AND col < 1 " +
                "AND col >= 1 AND col <= 1 AND col IN (1, 2, 3) AND col IS NULL AND col IS NOT NULL");
    }

    @Test
    public void testGroupBy()
    {
        check("SELECT COUNT(col) FROM table GROUP BY col1");
    }

    @Test
    public void testGroupByQualified()
    {
        check("SELECT COUNT(col) FROM table GROUP BY table.col1");
    }

    @Test
    public void testGroupByQualifiedWithAlias()
    {
        check("SELECT COUNT(col) FROM table AS table1 GROUP BY table1.col1");
    }

    @Test
    public void testHaving()
    {
        check("SELECT COUNT(col) FROM table AS table1 GROUP BY col1 HAVING COUNT(col) > 1");
    }

    @Test
    public void testJoinUsing()
    {
        check("SELECT * FROM table LEFT OUTER JOIN table2 USING (col, col2)");
    }

    @Test
    public void testJoinUsingQualified()
    {
        check("SELECT table.col, tableb.col FROM table LEFT OUTER JOIN tableb USING (col, col2)");
    }

    @Test
    public void testJoinUsingQualifiedAlias()
    {
        check("SELECT table1.col, tableb.col FROM table AS table1 LEFT OUTER JOIN tableb USING (col, col2)");
    }

    @Test
    public void testJoinOn()
    {
        check("SELECT * FROM table LEFT OUTER JOIN tableb ON (table.col = tableb.col)");
    }

    @Test
    public void testJoinOnQualified()
    {
        check("SELECT table.col, tableb.col FROM table LEFT OUTER JOIN tableb ON (table.col = tableb.col)");
    }

    @Test
    public void testJoinOnQualifiedAlias()
    {
        check("SELECT table1.col, table2.col FROM table AS table1 LEFT OUTER JOIN tableb AS table2 ON (table1.col = table2.col)");
    }

    @Test
    public void testUnionAll()
    {
        check("SELECT * FROM table UNION ALL SELECT * FROM table");
    }

    @Test
    public void testUnion()
    {
        check("SELECT * FROM table UNION SELECT * FROM table");
    }

    @Test
    public void testArithmetic()
    {
        check("SELECT (1 + 1) AS plus, (1 - 1) AS minus, (1 / 1) AS divide, (1 * 1) AS multiple FROM table");
    }

    @Test
    public void testCurrentTIme()
    {
        check("SELECT CURRENT_TIME, CURRENT_TIMESTAMP, CURRENT_DATE FROM table");
    }

    @Test
    public void testWindowFunc()
    {
        check("SELECT *, ROW_NUMBER() OVER (PARTITION BY group ORDER BY price ASC) FROM table");
    }

    private void fail(String sql, int start, int end, String message)
    {
        try
        {
            SQLGrammarParser parser = SQLGrammarParser.newInstance();
            parser.parseStatement(sql);
            Assert.fail();
        }
        catch (SQLParserException e)
        {
            Assert.assertEquals(start, e.getSourceInformation().startColumn);
            Assert.assertEquals(end, e.getSourceInformation().startLine);
            Assert.assertEquals(message, e.getMessage());
        }
    }

    private void check(String sql)
    {
        check(sql, sql);
    }

    private void check(String sql, String expected)
    {
        SQLGrammarParser parser = SQLGrammarParser.newInstance();
        Node node = parser.parseStatement(sql);
        SQLGrammarComposer composer = SQLGrammarComposer.newInstance();
        String result = composer.renderNode(node);
        MatcherAssert.assertThat(result.trim(), IsEqualIgnoringCase.equalToIgnoringCase(expected));
    }
}
