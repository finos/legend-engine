// Copyright 2025 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.postgres;

import org.finos.legend.engine.language.sql.grammar.from.SQLGrammarParser;
import org.finos.legend.engine.postgres.protocol.sql.serialization.SQLSerializer;
import org.junit.Assert;
import org.junit.Test;

/**
 * Comprehensive round-trip tests for SQLSerializer.
 * Each test parses SQL and asserts that the serialized output matches the expected form.
 * The serializer lowercases keywords, so expected values use lowercase keywords.
 */
public class TestSQLSerializerRoundtrip
{
    // ===== Basic SELECT =====

    @Test
    public void testSimpleSelect()
    {
        assertRoundtrip("select 1");
    }

    @Test
    public void testSelectWithAlias()
    {
        assertRoundtrip("select 1 as result");
    }

    @Test
    public void testSelectFromTable()
    {
        assertRoundtrip("select name from persons");
    }

    @Test
    public void testSelectMultipleColumns()
    {
        assertRoundtrip("select name, age, salary from persons");
    }

    @Test
    public void testSelectAll()
    {
        assertRoundtrip("select * from persons");
    }

    @Test
    public void testSelectQualifiedAll()
    {
        assertRoundtrip("select p.* from persons p");
    }

    @Test
    public void testSelectDistinct()
    {
        assertRoundtrip("select DISTINCT name from persons");
    }

    // ===== WHERE clause =====

    @Test
    public void testWhereEquals()
    {
        assertRoundtrip("select name from persons where id = 1");
    }

    @Test
    public void testWhereAnd()
    {
        assertRoundtrip("select name from persons where id > 1 AND name = 'test'");
    }

    @Test
    public void testWhereOr()
    {
        assertRoundtrip("select name from persons where id = 1 OR id = 2");
    }

    @Test
    public void testWhereNot()
    {
        assertRoundtrip("select name from persons where not id = 1");
    }

    @Test
    public void testWhereIsNull()
    {
        assertRoundtrip("select name from persons where salary is null");
    }

    @Test
    public void testWhereIsNotNull()
    {
        assertRoundtrip("select name from persons where salary is not null");
    }

    @Test
    public void testWhereBetween()
    {
        assertRoundtrip("select name from persons where salary between 1000 and 5000");
    }

    @Test
    public void testWhereNotBetween()
    {
        assertRoundtrip("select name from persons where salary not between 1000 and 5000");
    }

    @Test
    public void testWhereIn()
    {
        assertRoundtrip("select name from persons where id in (1, 2, 3)");
    }

    @Test
    public void testWhereNotIn()
    {
        assertRoundtrip("select name from persons where id not in (1, 2)");
    }

    @Test
    public void testWhereInSubquery()
    {
        assertRoundtrip("select name from persons where id not in (select id from departments)");
    }

    @Test
    public void testWhereLike()
    {
        assertRoundtrip("select name from persons where name like '%test%'");
    }

    @Test
    public void testWhereLikeEscape()
    {
        assertRoundtrip("select name from persons where name like '%\\%%' escape '\\'");
    }

    @Test
    public void testWhereILike()
    {
        assertRoundtrip("select name from persons where name ilike '%test%'");
    }

    @Test
    public void testWhereDistinctFrom()
    {
        assertRoundtrip("select name from persons where salary is distinct from 0");
    }

    @Test
    public void testWhereNotDistinctFrom()
    {
        assertRoundtrip("select name from persons where salary is not distinct from 0");
    }

    // ===== ORDER BY / LIMIT / OFFSET =====

    @Test
    public void testOrderBy()
    {
        assertRoundtrip("select name from persons order by name");
    }

    @Test
    public void testOrderByDesc()
    {
        assertRoundtrip("select name from persons order by salary DESC");
    }

    @Test
    public void testOrderByMultiple()
    {
        assertRoundtrip("select name from persons order by dept_id, name ASC");
    }

    @Test
    public void testOrderByNulls()
    {
        assertRoundtrip("select name from persons order by salary NULLS LAST");
    }

    @Test
    public void testLimit()
    {
        assertRoundtrip("select name from persons limit 10");
    }

    @Test
    public void testLimitAll()
    {
        assertRoundtrip("select name from persons limit ALL");
    }

    @Test
    public void testOffset()
    {
        assertRoundtrip("select name from persons offset 5");
    }

    @Test
    public void testLimitOffset()
    {
        assertRoundtrip("select name from persons limit 10 offset 5");
    }

    @Test
    public void testFetchFirst()
    {
        assertRoundtrip("select name from persons order by 1 FETCH FIRST 5 ROWS ONLY");
    }

    @Test
    public void testFetchNext()
    {
        assertRoundtrip("select name from persons order by 1 FETCH NEXT 1 ROW ONLY");
    }

    // ===== GROUP BY / HAVING =====

    @Test
    public void testGroupBy()
    {
        assertRoundtrip("select dept_id, count(*) from persons group by dept_id");
    }

    @Test
    public void testGroupByMultiple()
    {
        assertRoundtrip("select dept_id, active, count(*) from persons group by dept_id, active");
    }

    @Test
    public void testHaving()
    {
        assertRoundtrip("select dept_id, count(*) from persons group by dept_id having count(*) > 2");
    }

    // ===== JOINs =====

    @Test
    public void testInnerJoin()
    {
        assertRoundtrip("select p.name from persons p inner join departments d on p.dept_id = d.id");
    }

    @Test
    public void testImplicitJoin()
    {
        assertRoundtrip("select p.name from persons p join departments d on p.dept_id = d.id");
    }

    @Test
    public void testLeftJoin()
    {
        assertRoundtrip("select p.name from persons p left join departments d on p.dept_id = d.id");
    }

    @Test
    public void testLeftOuterJoin()
    {
        assertRoundtrip("select p.name from persons p left outer join departments d on p.dept_id = d.id");
    }

    @Test
    public void testRightJoin()
    {
        assertRoundtrip("select p.name from persons p right join departments d on p.dept_id = d.id");
    }

    @Test
    public void testFullJoin()
    {
        assertRoundtrip("select p.name from persons p full join departments d on p.dept_id = d.id");
    }

    @Test
    public void testFullOuterJoin()
    {
        assertRoundtrip("select p.name from persons p full outer join departments d on p.dept_id = d.id");
    }

    @Test
    public void testCrossJoin()
    {
        assertRoundtrip("select p.name from persons p CROSS join departments d");
    }

    @Test
    public void testJoinUsing()
    {
        assertRoundtrip("select name from persons p join departments d using (dept_id)");
    }

    @Test
    public void testNaturalJoin()
    {
        assertRoundtrip("select name from persons p natural join departments d");
    }

    // ===== Subqueries =====

    @Test
    public void testSubqueryInFrom()
    {
        assertRoundtrip("select sub.name from (select name from persons) sub");
    }

    @Test
    public void testSubqueryInWhere()
    {
        assertRoundtrip("select name from persons where id = (select max(id) from persons)");
    }

    @Test
    public void testExists()
    {
        assertRoundtrip("select name from persons p where exists (select 1 from orders o where o.person_id = p.id)");
    }

    @Test
    public void testScalarSubqueryInSelect()
    {
        assertRoundtrip("select name, (select count(*) from orders o where o.person_id = p.id) as cnt from persons p");
    }

    // ===== CTEs =====

    @Test
    public void testWith()
    {
        assertRoundtrip("with cte as (select name from persons) select name from cte");
    }

    @Test
    public void testWithMultiple()
    {
        assertRoundtrip("with a as (select 1 as x), b as (select 2 as y) select x, y from a, b");
    }

    // ===== UNION / INTERSECT / EXCEPT =====

    @Test
    public void testUnionAll()
    {
        assertRoundtrip("select name from persons UNION ALL select name from departments");
    }

    @Test
    public void testUnion()
    {
        assertRoundtrip("select name from persons UNION select name from departments");
    }

    @Test
    public void testIntersect()
    {
        assertRoundtrip("select name from persons INTERSECT select name from departments");
    }

    @Test
    public void testExcept()
    {
        assertRoundtrip("select name from persons EXCEPT select name from departments");
    }

    // ===== Functions =====

    @Test
    public void testCountStar()
    {
        assertRoundtrip("select count(*) from persons");
    }

    @Test
    public void testCountDistinct()
    {
        assertRoundtrip("select count(DISTINCT dept_id) from persons");
    }

    @Test
    public void testSumDistinct()
    {
        assertRoundtrip("select sum(DISTINCT salary) from persons");
    }

    @Test
    public void testFunctionNoArgs()
    {
        assertRoundtrip("select now()");
    }

    @Test
    public void testFunctionMultipleArgs()
    {
        assertRoundtrip("select coalesce(salary, 0) from persons");
    }

    @Test
    public void testFunctionOrderBy()
    {
        assertRoundtrip("select string_agg(name, ', ' order by name) from persons");
    }

    @Test
    public void testFunctionFilter()
    {
        assertRoundtrip("select count(*) FILTER (where active = true) from persons");
    }

    @Test
    public void testFunctionWithin()
    {
        assertRoundtrip("select percentile_cont(0.5) WITHIN GROUP (ORDER BY salary) from persons");
    }

    // ===== Window Functions =====

    @Test
    public void testWindowOver()
    {
        assertRoundtrip("select name, row_number() over (order by name) from persons");
    }

    @Test
    public void testWindowPartitionBy()
    {
        assertRoundtrip("select name, rank() over (partition by dept_id order by salary DESC) from persons");
    }

    @Test
    public void testWindowFrameRows()
    {
        assertRoundtrip("select name, sum(salary) over (order by name ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) from persons");
    }

    @Test
    public void testWindowFrameRange()
    {
        assertRoundtrip("select name, sum(salary) over (order by salary RANGE BETWEEN 1000 PRECEDING AND 1000 FOLLOWING) from persons");
    }

    @Test
    public void testWindowFrameUnboundedFollowing()
    {
        assertRoundtrip("select name, sum(salary) over (order by name ROWS BETWEEN CURRENT ROW AND UNBOUNDED FOLLOWING) from persons");
    }

    @Test
    public void testNamedWindow()
    {
        assertRoundtrip("select name, sum(salary) over w from persons WINDOW w AS (order by name)");
    }

    @Test
    public void testNamedWindowMultiple()
    {
        assertRoundtrip("select name, sum(salary) over w1, rank() over w2 from persons WINDOW w1 AS (order by name), w2 AS (order by salary DESC)");
    }

    @Test
    public void testNamedWindowWithFrame()
    {
        assertRoundtrip("select name, avg(salary) over w from persons WINDOW w AS (order by name ROWS BETWEEN 1 PRECEDING AND 1 FOLLOWING)");
    }

    @Test
    public void testWindowRef()
    {
        assertRoundtrip("select name, sum(salary) over w from persons");
    }

    // ===== CASE expressions =====

    @Test
    public void testSearchedCase()
    {
        assertRoundtrip("select case when salary > 50000 then 'high' else 'low' end from persons");
    }

    @Test
    public void testSimpleCase()
    {
        assertRoundtrip("select case dept_id when 1 then 'A' when 2 then 'B' else 'C' end from persons");
    }

    @Test
    public void testCaseNoElse()
    {
        assertRoundtrip("select case when salary > 50000 then 'high' end from persons");
    }

    // ===== CAST / Type operations =====

    @Test
    public void testCast()
    {
        assertRoundtrip("select CAST(salary AS INTEGER) from persons");
    }

    @Test
    public void testTryCast()
    {
        assertRoundtrip("select TRY_CAST(name AS INTEGER) from persons");
    }

    @Test
    public void testDoubleColonCast()
    {
        assertRoundtrip("select salary::integer from persons");
    }

    @Test
    public void testFromStringLiteralCast()
    {
        assertRoundtrip("select date '2024-01-01'");
    }

    // ===== Arithmetic =====

    @Test
    public void testArithmeticAdd()
    {
        assertRoundtrip("select salary + 100 from persons");
    }

    @Test
    public void testArithmeticMultiply()
    {
        assertRoundtrip("select salary * 2 from persons");
    }

    @Test
    public void testArithmeticUnaryMinus()
    {
        assertRoundtrip("select -salary from persons");
    }

    @Test
    public void testConcatenation()
    {
        assertRoundtrip("select name || ' - ' || dept_id from persons");
    }

    // ===== Explicit functions =====

    @Test
    public void testExtract()
    {
        assertRoundtrip("select extract(year from hire_date) from persons");
    }

    @Test
    public void testSubstring()
    {
        assertRoundtrip("select SUBSTRING(name FROM 1 FOR 3) from persons");
    }

    @Test
    public void testSubstringNoFor()
    {
        assertRoundtrip("select SUBSTRING(name FROM 2) from persons");
    }

    @Test
    public void testTrim()
    {
        assertRoundtrip("select trim(name) from persons");
    }

    @Test
    public void testTrimLeading()
    {
        assertRoundtrip("select trim(LEADING ' ' from name) from persons");
    }

    @Test
    public void testLeftFunction()
    {
        assertRoundtrip("select LEFT(name, 3) from persons");
    }

    @Test
    public void testRightFunction()
    {
        assertRoundtrip("select RIGHT(name, 3) from persons");
    }

    @Test
    public void testCurrentTimestamp()
    {
        assertRoundtrip("select CURRENT_TIMESTAMP");
    }

    @Test
    public void testCurrentDate()
    {
        assertRoundtrip("select CURRENT_DATE");
    }

    @Test
    public void testCurrentSchema()
    {
        assertRoundtrip("select CURRENT_SCHEMA");
    }

    @Test
    public void testCurrentUser()
    {
        assertRoundtrip("select CURRENT_USER");
    }

    @Test
    public void testSessionUser()
    {
        assertRoundtrip("select SESSION_USER");
    }

    // ===== Literals =====

    @Test
    public void testNullLiteral()
    {
        assertRoundtrip("select null");
    }

    @Test
    public void testBooleanLiterals()
    {
        assertRoundtrip("select true, false");
    }

    @Test
    public void testStringLiteral()
    {
        assertRoundtrip("select 'hello world'");
    }

    @Test
    public void testNumericLiteral()
    {
        assertRoundtrip("select 3.14");
    }

    @Test
    public void testEscapedString()
    {
        assertRoundtrip("select E'hello\\nworld'");
    }

    @Test
    public void testIntervalLiteral()
    {
        assertRoundtrip("select INTERVAL '1' DAY");
    }

    @Test
    public void testIntervalWithTo()
    {
        assertRoundtrip("select INTERVAL '1 2:30' DAY TO MINUTE");
    }

    @Test
    public void testArrayLiteral()
    {
        assertRoundtrip("select ARRAY[1, 2, 3]");
    }

    @Test
    public void testEmptyArray()
    {
        assertRoundtrip("select ARRAY[]");
    }

    @Test
    public void testParameterPlaceholder()
    {
        assertRoundtrip("select name from persons where id = ?");
    }

    @Test
    public void testPositionalParameter()
    {
        assertRoundtrip("select name from persons where id = $1");
    }

    // ===== Subscript / Array operations =====

    @Test
    public void testSubscript()
    {
        assertRoundtrip("select arr[1] from data");
    }

    @Test
    public void testArraySlice()
    {
        assertRoundtrip("select arr[1:3] from data");
    }

    // ===== Nested expressions =====

    @Test
    public void testNestedExpression()
    {
        assertRoundtrip("select (salary + bonus) * 2 from persons");
    }

    @Test
    public void testParenthesizedQuery()
    {
        assertRoundtrip("(select name from persons)");
    }

    // ===== LATERAL =====

    @Test
    public void testLateralSubquery()
    {
        assertRoundtrip("select p.name, sub.cnt from persons p CROSS join LATERAL (select count(*) as cnt from orders o where o.person_id = p.id) sub");
    }

    @Test
    public void testLateralTable()
    {
        assertRoundtrip("select p.name, g.val from persons p CROSS join LATERAL generate_series(1, p.dept_id) as g(val)");
    }

    // ===== JSON operators =====

    @Test
    public void testJsonExtract()
    {
        assertRoundtrip("select data -> 'key' from json_table");
    }

    @Test
    public void testJsonExtractText()
    {
        assertRoundtrip("select data ->> 'key' from json_table");
    }

    @Test
    public void testJsonPathExtract()
    {
        assertRoundtrip("select data #> '{a,b}' from json_table");
    }

    @Test
    public void testJsonPathExtractText()
    {
        assertRoundtrip("select data #>> '{a,b}' from json_table");
    }

    @Test
    public void testJsonbContainsRight()
    {
        assertRoundtrip("select data @> '{\"a\":1}'::jsonb from json_table");
    }

    @Test
    public void testJsonbContainedBy()
    {
        assertRoundtrip("select data <@ '{\"a\":1}'::jsonb from json_table");
    }

    @Test
    public void testJsonbHasKey()
    {
        assertRoundtrip("select data ? 'key' from json_table");
    }

    @Test
    public void testJsonbConcat()
    {
        assertRoundtrip("select '{\"a\":1}'::jsonb || '{\"b\":2}'::jsonb");
    }

    @Test
    public void testJsonbDeleteKey()
    {
        assertRoundtrip("select data - 'key' from json_table");
    }

    @Test
    public void testJsonbDeletePath()
    {
        assertRoundtrip("select data #- '{a,b}' from json_table");
    }

    // ===== Miscellaneous =====

    @Test
    public void testAtTimezone()
    {
        assertRoundtrip("select ts at time zone 'UTC' from events");
    }

    @Test
    public void testRecordSubscript()
    {
        assertRoundtrip("select (composite_col).field_name from data");
    }

    @Test
    public void testDereference()
    {
        assertRoundtrip("select t.schema.col from t");
    }

    @Test
    public void testAliasedRelationWithAs()
    {
        assertRoundtrip("select p.name from persons as p");
    }

    @Test
    public void testAliasedColumns()
    {
        assertRoundtrip("select a, b from (select 1, 2) as sub(a, b)");
    }

    @Test
    public void testValues()
    {
        assertRoundtrip("VALUES (1, 'a'), (2, 'b')");
    }

    @Test
    public void testArraySubquery()
    {
        assertRoundtrip("select ARRAY (select id from persons)");
    }

    @Test
    public void testIfCase()
    {
        assertRoundtrip("select IF(salary > 50000, 'high', 'low') from persons");
    }

    @Test
    public void testQuantifiedComparison()
    {
        assertRoundtrip("select name from persons where salary > any(select salary from persons)");
    }

    @Test
    public void testBitwiseBinary()
    {
        assertRoundtrip("select flags & 1 from data");
    }

    @Test
    public void testLogicalBinaryComplex()
    {
        assertRoundtrip("select name from persons where (id > 1 AND id < 10) OR name = 'admin'");
    }

    // ===== Combined / complex queries =====

    @Test
    public void testComplexQuery()
    {
        assertRoundtrip("with dept_stats as (select dept_id, count(*) as cnt from persons where dept_id is not null group by dept_id) select dept_id, cnt from dept_stats where cnt > 2 order by 1");
    }

    @Test
    public void testJoinWithAggAndWindow()
    {
        assertRoundtrip("select d.name, count(*) as cnt, sum(count(*)) over (order by d.name) as running from persons p inner join departments d on p.dept_id = d.id group by d.name order by 1");
    }

    @Test
    public void testMultipleJoins()
    {
        assertRoundtrip("select p.name, d.name, o.amount from persons p inner join departments d on p.dept_id = d.id inner join orders o on o.person_id = p.id order by 1");
    }

    // ===== Helper =====

    private void assertRoundtrip(String sql)
    {
        String result = SQLGrammarParser.getSqlBaseParser(sql, "query").statement().accept(new SQLSerializer());
        Assert.assertEquals(sql, result);
    }
}

