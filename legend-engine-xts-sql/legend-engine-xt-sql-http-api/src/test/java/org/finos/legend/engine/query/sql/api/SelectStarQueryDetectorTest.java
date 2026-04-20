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

    // ==================== NON-OPTIMIZABLE QUERIES ====================
    @Test
    public void testWithWhere()
    {
        // WHERE clause requires transformation to apply filtering logic
        assertIsNotSelectStar("SELECT * FROM service('/myService') WHERE age > 30");
        assertIsNotSelectStar("SELECT * FROM (SELECT * FROM service('/myService') WHERE age > 30)");
        assertIsNotSelectStar("SELECT * FROM (SELECT * FROM service('/myService')) WHERE age > 30");
    }

    @Test
    public void testWithSpecificColumns()
    {
        // Selecting specific columns requires transformation to project only those columns
        assertIsNotSelectStar("SELECT name FROM service('/myService')");
        assertIsNotSelectStar("SELECT name, age FROM service('/myService')");
        
        // Inner query with specific columns disqualifies the entire query
        assertIsNotSelectStar("SELECT * FROM (SELECT name FROM service('/myService'))");
    }

    @Test
    public void testWithOrderBy()
    {
        // ORDER BY requires transformation to apply sorting logic
        assertIsNotSelectStar("SELECT * FROM service('/myService') ORDER BY name");
        assertIsNotSelectStar("SELECT * FROM (SELECT * FROM service('/myService')) ORDER BY name");
    }

    @Test
    public void testWithLimitOffset()
    {
        // LIMIT/OFFSET requires transformation to apply pagination
        assertIsNotSelectStar("SELECT * FROM service('/myService') LIMIT 10");
        assertIsNotSelectStar("SELECT * FROM service('/myService') OFFSET 5");
    }

    @Test
    public void testWithGroupByHaving()
    {
        assertIsNotSelectStar("SELECT * FROM service('/myService') GROUP BY name");
        assertIsNotSelectStar("SELECT name, count(*) FROM service('/myService') GROUP BY name HAVING count(*) > 1");
    }

    @Test
    public void testWithDistinct()
    {
        assertIsNotSelectStar("SELECT DISTINCT * FROM service('/myService')");
    }

    @Test
    public void testWithMultipleSources()
    {
        assertIsNotSelectStar("SELECT * FROM service('/myService') JOIN service('/otherService') ON 1=1");
        assertIsNotSelectStar("SELECT * FROM service('/myService'), service('/otherService')");
    }

    @Test
    public void testWithServiceParameters()
    {
        // Parameterized service calls require the standard path for version 1
        assertIsNotSelectStar("SELECT * FROM service('/myService', businessDate => '2015-01-01')");
        assertIsNotSelectStar("SELECT * FROM service('/myService', date => '2023-08-24', type => 'Type1')");
        assertIsNotSelectStar("SELECT * FROM service('/myService', names => ARRAY['Alice', 'Bob'])");
    }

    @Test
    public void testNullQuery()
    {
        assertFalse(SelectStarQueryDetector.isSelectStar(null));
    }
}