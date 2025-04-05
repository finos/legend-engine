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
        check("SELECT * FROM myTable");
        check("SELECT myTable.* FROM myTable");
    }

    @Test
    public void testPatternMatching()
    {
        check("SELECT * FROM myTable where 'abc' ~ 'def'");
        check("SELECT * FROM myTable where 'abc' ~* 'def'");
        check("SELECT * FROM myTable where 'abc' !~ 'def'");
        check("SELECT * FROM myTable where 'abc' !~* 'def'");
        check("SELECT * FROM myTable where 'abc' ~~ 'def'");
        check("SELECT * FROM myTable where 'abc' ~~* 'def'");
        check("SELECT * FROM myTable where 'abc' !~~ 'def'");
        check("SELECT * FROM myTable where 'abc' !~~* 'def'");
    }

    @Test
    public void testParameters()
    {
        check("SELECT * FROM myTable where name = $1");
        check("SELECT * FROM myTable where name = ?");
    }

    @Test
    public void testSelectStarFromNoParamTableFunc()
    {
        check("SELECT * FROM myTable()");
    }

    @Test
    public void testSelectStarFromTableFuncWithParams()
    {
        check("SELECT * FROM myTable(1, a => 1, b => [1, 2, 3])");
    }

    @Test
    public void testSelectColumns()
    {
        check("SELECT col1, col2 FROM myTable");
    }

    @Test
    public void testSelectWithAliases()
    {
        check("SELECT col1 AS COL FROM myTable AS myTable1");
    }

    @Test
    public void testSelectQualified()
    {
        check("SELECT col1, myTable.col2, \"col 3\" FROM myTable");
    }

    @Test
    public void testSelectQualifiedWithAlias()
    {
        check("SELECT col1, myTable1.col2 FROM myTable AS myTable1");
    }

    @Test
    public void testDistinct()
    {
        check("SELECT DISTINCT * FROM myTable");
    }

    @Test
    public void testLimit()
    {
        check("SELECT * FROM myTable LIMIT 1");
    }

    @Test
    public void testOffset()
    {
        check("SELECT * FROM myTable OFFSET 1");
    }

    @Test
    public void testOrderBy()
    {
        check("SELECT * FROM myTable ORDER BY col1 DESC, col2 ASC");
    }

    @Test
    public void testOrderByDefaultASC()
    {
        check("SELECT * FROM myTable ORDER BY col1 DESC, col2", "SELECT * FROM myTable ORDER BY col1 DESC, col2 ASC");
    }

    @Test
    public void testOrderByAlias()
    {
        check("SELECT * FROM myTable AS myTable1 ORDER BY myTable1.col1 DESC, col2 ASC");
    }

    @Test
    public void testOrderByQualified()
    {
        check("SELECT * FROM myTable ORDER BY myTable.col1 DESC, col2 ASC");
    }

    @Test
    public void testOrderByQualifiedWithAlias()
    {
        check("SELECT * FROM myTable AS myTable1 ORDER BY myTable1.col1 DESC, col2 ASC");
    }

    @Test
    public void testWhere()
    {
        check("SELECT * FROM myTable WHERE col1 = 1");
    }

    @Test
    public void testWhereExpression()
    {
        checkExpression("col1 = 1");
    }

    @Test
    public void testCompositeWhere()
    {
        check("SELECT * FROM myTable WHERE col1 = 1 AND col2 = 1");
    }

    @Test
    public void testCompositeWhereExpression()
    {
        checkExpression("col1 = 1 AND col2 = 1");
    }

    @Test
    public void testWhereQualified()
    {
        check("SELECT * FROM myTable WHERE myTable.col1 = 1");
    }

    @Test
    public void testWhereQualifiedExpression()
    {
        checkExpression("myTable.col1 = 1");
    }

    @Test
    public void testCompositeWhereQualifiedWithAlias()
    {
        check("SELECT * FROM myTable AS myTable1 WHERE col = 1 AND myTable1.col = 1");
    }

    @Test
    public void testCompositeWhereOperators()
    {
        check("SELECT * FROM myTable WHERE col = 1 AND col > 1 AND col < 1 " +
                "AND col >= 1 AND col <= 1 AND col IN (1, 2, 3) AND col IS NULL AND " +
                "col IS NOT NULL AND col IS DISTINCT FROM 1 AND col IS NOT DISTINCT FROM 1 AND " +
                "col BETWEEN 0 AND 1");
    }

    @Test
    public void testCompositeWhereOperatorsExpression()
    {
        checkExpression("col = 1 AND col > 1 AND col < 1 " +
                "AND col >= 1 AND col <= 1 AND col IN (1, 2, 3) AND col IS NULL AND " +
                "col IS NOT NULL AND col IS DISTINCT FROM 1 AND col IS NOT DISTINCT FROM 1 AND " +
                "col BETWEEN 0 AND 1");
    }

    @Test
    public void testGroupBy()
    {
        check("SELECT COUNT(col) FROM myTable GROUP BY col1");
    }

    @Test
    public void testGroupByQualified()
    {
        check("SELECT COUNT(col) FROM myTable GROUP BY myTable.col1");
    }

    @Test
    public void testGroupByQualifiedWithAlias()
    {
        check("SELECT COUNT(col) FROM myTable AS myTable1 GROUP BY myTable1.col1");
    }

    @Test
    public void testHaving()
    {
        check("SELECT COUNT(col) FROM myTable AS myTable1 GROUP BY col1 HAVING COUNT(col) > 1");
    }

    @Test
    public void testJoinUsing()
    {
        check("SELECT * FROM myTable LEFT OUTER JOIN myTable2 USING (col, col2)");
    }

    @Test
    public void testJoinUsingQualified()
    {
        check("SELECT myTable.col, myTableb.col FROM myTable LEFT OUTER JOIN myTableb USING (col, col2)");
    }

    @Test
    public void testJoinUsingQualifiedAlias()
    {
        check("SELECT myTable1.col, myTableb.col FROM myTable AS myTable1 LEFT OUTER JOIN myTableb USING (col, col2)");
    }

    @Test
    public void testJoinOn()
    {
        check("SELECT * FROM myTable LEFT OUTER JOIN myTableb ON (myTable.col = myTableb.col)");
    }

    @Test
    public void testJoinOnQualified()
    {
        check("SELECT myTable.col, myTableb.col FROM myTable LEFT OUTER JOIN myTableb ON (myTable.col = myTableb.col)");
    }

    @Test
    public void testJoinOnQualifiedAlias()
    {
        check("SELECT myTable1.col, myTable2.col FROM myTable AS myTable1 LEFT OUTER JOIN myTableb AS myTable2 ON (myTable1.col = myTable2.col)");
    }

    @Test
    public void testNaturalJoin()
    {
        check("SELECT * FROM myTable NATURAL LEFT OUTER JOIN myTable2");
    }

    @Test
    public void testCrossJoin()
    {
        check("SELECT * FROM myTable CROSS JOIN myTable1");
    }

    @Test
    public void testUnionAll()
    {
        check("SELECT * FROM myTable UNION ALL SELECT * FROM myTable");
    }

    @Test
    public void testUnion()
    {
        check("SELECT * FROM myTable UNION SELECT * FROM myTable");
    }

    @Test
    public void testArithmetic()
    {
        check("SELECT (1 + 1) AS plus, (1 - 1) AS minus, (1 / 1) AS divide, " +
                "(1 * 1) AS multiply, (1 % 2) AS MOD, (1 ^ 2) AS POW FROM myTable");
    }

    @Test
    public void testCurrentTIme()
    {
        check("SELECT CURRENT_TIME, CURRENT_TIMESTAMP, CURRENT_DATE FROM myTable");
    }

    @Test
    public void testWindowFunc()
    {
        check("SELECT *, ROW_NUMBER() OVER (PARTITION BY abc ORDER BY price ASC) FROM myTable");
    }

    @Test
    public void testCast()
    {
        check("SELECT CAST(1 AS VARCHAR), CAST(1 AS VARCHAR(1)), CAST(1 AS NUMERIC(1, 2)) FROM myTable");
    }

    @Test
    public void testExtract()
    {
        check("SELECT EXTRACT(DOW FROM Date) FROM myTable");
    }

    @Test
    public void testInterval()
    {
        check("SELECT INTERVAL '1 YEAR 2 MONTHS 3 WEEKS 4 DAYS 5 HOURS 6 MINUTES 7 SECONDS' FROM myTable");
    }

    @Test
    public void testLiterals()
    {
        check("SELECT 1, 'abc', true, 1.0, null FROM myTable");
    }

    @Test
    public void testWithinGroup()
    {
        check("SELECT percentile_cont(0.1) WITHIN GROUP (ORDER BY a ASC) FROM myTable");
    }

    @Test
    public void testFunctionCallWithOrder()
    {
        check("SELECT string_agg(Col1, ', ' ORDER BY Col2 ASC, Col3 DESC) FROM myTable");
    }

    @Test
    public void testTrim()
    {
        check("SELECT trim(BOTH ' ' FROM 'abc') FROM myTable");
        check("SELECT trim(BOTH FROM 'abc') FROM myTable");
    }

    @Test
    public void testNested()
    {
        check("SELECT * from (select col from myTable)");
    }

    @Test
    public void testCommonTableExpressionSingle()
    {
        check("WITH cte1 (col) AS (SELECT col from myTable) SELECT col from cte1");
    }

    @Test
    public void testCommonTableExpressionMultiple()
    {
        check("WITH cte1 (col1, col2) AS (SELECT col1, col2 FROM myTable1 AS t1 INNER JOIN myTable2 AS t2 ON (t1.col1 = t2.col2)), cte2 AS (SELECT SUM(), col FROM cte1 GROUP BY col) SELECT col FROM cte1");
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
        check(sql.toLowerCase(), sql);
    }

    private void check(String sql, String expected)
    {
        SQLGrammarParser parser = SQLGrammarParser.newInstance();
        Node node = parser.parseStatement(sql);
        SQLGrammarComposer composer = SQLGrammarComposer.newInstance();
        String result = composer.renderNode(node);
        MatcherAssert.assertThat(result.trim(), IsEqualIgnoringCase.equalToIgnoringCase(expected));
    }

    private void checkExpression(String expression)
    {
        checkExpression(expression, expression);
        checkExpression(expression.toLowerCase(), expression);
    }

    private void checkExpression(String expression, String expected)
    {
        SQLGrammarParser parser = SQLGrammarParser.newInstance();
        Node node = parser.parseExpression(expression);
        SQLGrammarComposer composer = SQLGrammarComposer.newInstance();
        String result = composer.renderNode(node);
        MatcherAssert.assertThat(result.trim(), IsEqualIgnoringCase.equalToIgnoringCase(expected));
    }
}
