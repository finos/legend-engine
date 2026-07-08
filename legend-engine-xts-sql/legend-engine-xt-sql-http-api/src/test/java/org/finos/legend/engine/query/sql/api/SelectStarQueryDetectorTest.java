// Copyright 2026 Goldman Sachs
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
import org.finos.legend.engine.protocol.sql.metamodel.Query;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for SelectStarQueryDetector.
 * A SELECT * query is one with no modifications (no WHERE, ORDER BY, etc.)
 * has a single service source (no JOINs or multiple tables)
 * that can use a pre-generated execution plan directly without SQL-to-Pure transformation.
 */
public class SelectStarQueryDetectorTest
{
    private static final SQLGrammarParser PARSER = SQLGrammarParser.newInstance();
    
    private void assertIsSelectStar(String sql)
    {
        Query query = (Query) PARSER.parseStatement(sql);
        assertTrue("Query should qualify for SELECT * optimization (can use pre-generated plan): " + sql,
                SelectStarQueryDetector.isSelectStar(query));
    }
    
    private void assertIsNotSelectStar(String sql)
    {
        Query query = (Query) PARSER.parseStatement(sql);
        assertFalse("Query should NOT qualify for SELECT * optimization (requires SQL-to-Pure transformation): " + sql,
                SelectStarQueryDetector.isSelectStar(query));
    }


    private void assertIsNotSelectStarOrNotYetSupported(String featureName, String sql)
    {
        try
        {
            Query query = (Query) PARSER.parseStatement(sql);
            // Parser supports this - verify detector rejects it
            assertFalse(featureName + " should NOT qualify for SELECT * optimization: " + sql,
                    SelectStarQueryDetector.isSelectStar(query));
        }
        catch (RuntimeException e)
        {
            if (e.getMessage() != null && e.getMessage().contains("Not supported yet"))
            {
                // Parser doesn't support this yet - When support is added, this will automatically start testing the detector
                System.out.println("INFO: " + featureName + " not yet supported by SQL parser. " +
                        "SelectStarQueryDetector already handles this (returns false) for when support is added.");
            }
            else
            {
                throw new AssertionError(featureName + " parsing failed unexpectedly: " + e.getMessage(), e);
            }
        }
    }

    // ==================== OPTIMIZABLE QUERIES ====================
    @Test
    public void testSelectStar()
    {
        assertIsSelectStar("SELECT * FROM service('/myService')");
        assertIsSelectStar("SELECT * FROM service('/myService') AS t");
        assertIsSelectStar("SELECT t.* FROM service('/myService') AS t");
        assertIsSelectStar("SELECT * FROM (SELECT * FROM service('/myService'))");
        assertIsSelectStar("SELECT * FROM (SELECT * FROM (SELECT * FROM service('/myService')))");
        assertIsSelectStar("SELECT * FROM (SELECT * FROM service('/myService') AS inner_t) AS outer_t");
    }

    @Test
    public void testSelectStarWithServiceParameters()
    {
        // Parameterized service calls with literal values ARE optimizable
        assertIsSelectStar("SELECT * FROM service('/myService', businessDate => '2015-01-01')");
        assertIsSelectStar("SELECT * FROM service('/myService', date => '2023-08-24', type => 'Type1')");
        assertIsSelectStar("SELECT * FROM service('/myService', names => ARRAY['Alice', 'Bob'])");
        assertIsSelectStar("SELECT * FROM service('/myService', id => 42)");
        assertIsSelectStar("SELECT * FROM service('/myService', active => true)");
        assertIsSelectStar("SELECT * FROM service('/myService', rating => 9.5)");

        // Nested with parameters
        assertIsSelectStar("SELECT * FROM (SELECT * FROM service('/myService', businessDate => '2015-01-01'))");
        assertIsSelectStar("SELECT * FROM (SELECT * FROM service('/myService', businessDate => '2015-01-01')) AS t");
    }

    @Test
    public void testSelectStarWithNonLiteralParameters()
    {
        assertIsNotSelectStar("SELECT * FROM service('/myService', businessDate => CURRENT_DATE)");
        assertIsNotSelectStar("SELECT * FROM service('/myService', ts => CURRENT_TIMESTAMP)");
        assertIsNotSelectStar("SELECT * FROM service('/myService', businessDate => now())");
        assertIsNotSelectStar("SELECT * FROM service('/myService', id => 1 + 2)");
        assertIsNotSelectStarOrNotYetSupported("subquery param", "SELECT * FROM service('/myService', id => (SELECT max(id) FROM service('/other')))");
    }

    // ==================== NON-OPTIMIZABLE QUERIES ====================
    @Test
    public void testWithWhere()
    {
        // WHERE clause requires transformation to apply filtering logic
        assertIsNotSelectStar("SELECT * FROM service('/myService') WHERE age > 30");
        assertIsNotSelectStar("SELECT * FROM (SELECT * FROM service('/myService') WHERE age > 30)");
        assertIsNotSelectStar("SELECT * FROM (SELECT * FROM service('/myService')) WHERE age > 30");
        assertIsNotSelectStar("SELECT * FROM service('/myService', businessDate => '2015-01-01') WHERE age > 30");
    }

    @Test
    public void testWithSpecificColumns()
    {
        // Selecting specific columns requires transformation to project only those columns
        assertIsNotSelectStar("SELECT name FROM service('/myService')");
        assertIsNotSelectStar("SELECT name, age FROM service('/myService')");
        assertIsNotSelectStar("SELECT * FROM (SELECT name FROM service('/myService'))");
        assertIsNotSelectStar("SELECT name FROM service('/myService', businessDate => '2015-01-01')");
        assertIsNotSelectStar("SELECT name, age FROM service('/myService', date => '2023-08-24', type => 'Type1')");
    }

    @Test
    public void testWithOrderBy()
    {
        // ORDER BY requires transformation to apply sorting logic
        assertIsNotSelectStar("SELECT * FROM service('/myService') ORDER BY name");
        assertIsNotSelectStar("SELECT * FROM (SELECT * FROM service('/myService')) ORDER BY name");
        assertIsNotSelectStar("SELECT * FROM service('/myService', businessDate => '2015-01-01') ORDER BY name");
    }

    @Test
    public void testWithLimitOffset()
    {
        // LIMIT/OFFSET requires transformation to apply pagination
        assertIsNotSelectStar("SELECT * FROM service('/myService') LIMIT 10");
        assertIsNotSelectStar("SELECT * FROM service('/myService') OFFSET 5");
        assertIsNotSelectStar("SELECT * FROM service('/myService', businessDate => '2015-01-01') LIMIT 10");
    }

    @Test
    public void testWithGroupByHaving()
    {
        assertIsNotSelectStar("SELECT * FROM service('/myService') GROUP BY name");
        assertIsNotSelectStar("SELECT name, count(*) FROM service('/myService') GROUP BY name HAVING count(*) > 1");
        assertIsNotSelectStar("SELECT * FROM service('/myService', businessDate => '2015-01-01') GROUP BY name");
    }

    @Test
    public void testWithDistinct()
    {
        assertIsNotSelectStar("SELECT DISTINCT * FROM service('/myService')");
        assertIsNotSelectStar("SELECT DISTINCT * FROM service('/myService', businessDate => '2015-01-01')");
    }

    @Test
    public void testWithExpressions()
    {
        // Aggregate functions in SELECT
        assertIsNotSelectStar("SELECT count(*) FROM service('/myService')");
        assertIsNotSelectStar("SELECT sum(salary) FROM service('/myService')");
        assertIsNotSelectStar("SELECT name AS n FROM service('/myService')");
    }

    @Test
    public void testNonStarInnerSubquery()
    {
        assertIsNotSelectStar("SELECT * FROM (SELECT name, age FROM service('/myService'))");
        assertIsNotSelectStar("SELECT * FROM (SELECT name FROM service('/myService', businessDate => '2015-01-01'))");
    }

    @Test
    public void testWithMultipleSources()
    {
        assertIsNotSelectStar("SELECT * FROM service('/myService') JOIN service('/otherService') ON 1=1");
        assertIsNotSelectStar("SELECT * FROM service('/myService'), service('/otherService')");
    }

    @Test
    public void testWithSetOperations()
    {
        // UNION is supported - test directly
        assertIsNotSelectStar("SELECT * FROM service('/myService') UNION SELECT * FROM service('/otherService')");
        assertIsNotSelectStar("SELECT * FROM service('/myService') UNION ALL SELECT * FROM service('/otherService')");

        // INTERSECT and EXCEPT may not be supported yet - use graceful helper
        assertIsNotSelectStarOrNotYetSupported("INTERSECT", "SELECT * FROM service('/myService') INTERSECT SELECT * FROM service('/otherService')");
        assertIsNotSelectStarOrNotYetSupported("EXCEPT", "SELECT * FROM service('/myService') EXCEPT SELECT * FROM service('/otherService')");
    }

    @Test
    public void testNullQuery()
    {
        assertFalse(SelectStarQueryDetector.isSelectStar(null));
    }
}