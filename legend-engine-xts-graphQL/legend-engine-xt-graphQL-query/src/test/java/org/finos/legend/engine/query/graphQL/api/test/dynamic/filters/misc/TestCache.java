package org.finos.legend.engine.query.graphQL.api.test.dynamic.filters.misc;

import org.finos.legend.engine.query.graphQL.api.cache.*;
import org.finos.legend.engine.query.graphQL.api.test.dynamic.filters.*;
import org.junit.*;

public class TestCache extends TestGraphQLDynamicFilters
{
    @Test
    public void testCache() throws Exception
    {
        String query = "query Query {\n" +
                "  allEmployees(where: { firstName: { _eq: \"%s\" } }) {\n" +
                "      fullName" +
                "    }\n" +
                "  }";
        GraphQLPlanCache cache = new GraphQLPlanCache(getExecutionCacheInstance());

        String expected = "{\"data\":{\"allEmployees\":[{\"fullName()\":\"John Johnson\"},{\"fullName()\":\"John Hill\"}]}}";
        runTest(String.format(query, "John"), expected, cache);
        Assert.assertEquals(0, cache.getCache().stats().hitCount(), 0);
        Assert.assertEquals(1, cache.getCache().stats().missCount(), 0);

        String expected2 = "{\"data\":{\"allEmployees\":{\"fullName()\":\"David Harris\"}}}";
        runTest(String.format(query, "David"), expected2, cache);
        Assert.assertEquals(1, cache.getCache().stats().hitCount(), 0);
        Assert.assertEquals(1, cache.getCache().stats().missCount(), 0);

        runTest(String.format(query, "John"), expected, cache);
        Assert.assertEquals(2, cache.getCache().stats().hitCount(), 0);
        Assert.assertEquals(1, cache.getCache().stats().missCount(), 0);

        runTest(String.format(query, "David"), expected2, cache);
        Assert.assertEquals(3, cache.getCache().stats().hitCount(), 0);
        Assert.assertEquals(1, cache.getCache().stats().missCount(), 0);
    }

    @Test
    public void testCacheInComplexQuery() throws Exception
    {
        String query =
                "query Query {\n" +
                        "   allEmployees(where: {" +
                        "      _or: [" +
                        "               { firstName: { _eq: \"%s\" } }," +
                        "               { " +
                        "                   _and: [" +
                        "                       {firstName: { _eq: \"John\"}}," +
                        "                       {isAFullTimeEmployee: { _eq: %s }}" +
                        "                       ] " +
                        "               }," +
                        "               { age: { _eq: %s }}" +
                        "           ]" +
                        "   }) " +
                        "   {\n" +
                        "       fullName\n" +
                        "       age\n" +
                        "   }\n" +
                        "}";
        GraphQLPlanCache cache = new GraphQLPlanCache(getExecutionCacheInstance());

        String expected = "{\"data\":{\"allEmployees\":[{\"fullName()\":\"Peter Smith\",\"age\":25},{\"fullName()\":\"John Johnson\",\"age\":35},{\"fullName()\":\"David Harris\",\"age\":55}]}}";
        runTest(String.format(query, "David", false, 25), expected, cache);
        Assert.assertEquals(0, cache.getCache().stats().hitCount(), 0);
        Assert.assertEquals(1, cache.getCache().stats().missCount(), 0);

        String expected2 = "{\"data\":{\"allEmployees\":[{\"fullName()\":\"John Hill\",\"age\":30},{\"fullName()\":\"Anthony Allen\",\"age\":40},{\"fullName()\":\"David Harris\",\"age\":55}]}}";
        runTest(String.format(query, "David", true, 40), expected2, cache);
        Assert.assertEquals(1, cache.getCache().stats().hitCount(), 0);
        Assert.assertEquals(1, cache.getCache().stats().missCount(), 0);

        runTest(String.format(query, "David", false, 25), expected, cache);
        Assert.assertEquals(2, cache.getCache().stats().hitCount(), 0);
        Assert.assertEquals(1, cache.getCache().stats().missCount(), 0);

        runTest(String.format(query, "David", true, 40), expected2, cache);
        Assert.assertEquals(3, cache.getCache().stats().hitCount(), 0);
        Assert.assertEquals(1, cache.getCache().stats().missCount(), 0);
    }

    @Test
    public void testCacheWithOrderChange() throws Exception
    {
        String query =
                "query Query {\n" +
                        "   allEmployees(where: {" +
                        "      _or: [" +
                        "               %s," +
                        "               %s" +
                        "           ]" +
                        "   }) " +
                        "   {\n" +
                        "       fullName\n" +
                        "       age\n" +
                        "   }\n" +
                        "}";
        GraphQLPlanCache cache = new GraphQLPlanCache(getExecutionCacheInstance());

        String expected = "{\"data\":{\"allEmployees\":[{\"fullName()\":\"Peter Smith\",\"age\":25},{\"fullName()\":\"David Harris\",\"age\":55}]}}";
        runTest(String.format(query, "{ firstName: { _eq: \"David\" } }", "{ age: { _eq: 25 }}"), expected, cache);
        Assert.assertEquals(0, cache.getCache().stats().hitCount(), 0);
        Assert.assertEquals(1, cache.getCache().stats().missCount(), 0);

        runTest(String.format(query, "{ age: { _eq: 25 }}", "{ firstName: { _eq: \"David\" } }"), expected, cache);
        Assert.assertEquals(0, cache.getCache().stats().hitCount(), 0);
        Assert.assertEquals(2, cache.getCache().stats().missCount(), 0);

        runTest(String.format(query, "{ firstName: { _eq: \"David\" } }", "{ age: { _eq: 25 }}"), expected, cache);
        Assert.assertEquals(1, cache.getCache().stats().hitCount(), 0);
        Assert.assertEquals(2, cache.getCache().stats().missCount(), 0);

        runTest(String.format(query, "{ age: { _eq: 25 }}", "{ firstName: { _eq: \"David\" } }"), expected, cache);
        Assert.assertEquals(2, cache.getCache().stats().hitCount(), 0);
        Assert.assertEquals(2, cache.getCache().stats().missCount(), 0);
    }
}
