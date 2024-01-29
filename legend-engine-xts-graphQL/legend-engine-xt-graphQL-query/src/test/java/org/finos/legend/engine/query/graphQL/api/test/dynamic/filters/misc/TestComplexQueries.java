package org.finos.legend.engine.query.graphQL.api.test.dynamic.filters.misc;

import org.finos.legend.engine.query.graphQL.api.cache.*;
import org.finos.legend.engine.query.graphQL.api.test.dynamic.filters.*;
import org.junit.*;

public class TestComplexQueries extends TestGraphQLDynamicFilters
{
    @Test
    public void testSimplePaginated() throws Exception
    {
        String query =
                "query Query {\n" +
                        "   allEmployeesPaginated(pageNumber: 1, where: {" +
                        "       firstName: { _contains: \"Jo\" }" +
                        "   }) " +
                        "   {\n" +
                        "       fullName\n" +
                        "   }\n" +
                        "}";
        String expected = "{\"data\":{\"allEmployeesPaginated\":{\"fullName()\":\"John Hill\"}}}";
        runTest(query, expected);
    }

    @Test
    public void testSimpleSliced() throws Exception
    {
        String query =
                "query Query {\n" +
                        "   allEmployeesSliced(limit: 1, offset: 1, where: {" +
                        "       firstName: { _eqIgnoreCase: \"john\" }" +
                        "   }) " +
                        "   {\n" +
                        "       fullName\n" +
                        "   }\n" +
                        "}";
        String expected = "{\"data\":{\"allEmployeesSliced\":{\"fullName()\":\"John Johnson\"}}}";
        runTest(query, expected);
    }

    @Test
    public void testInWithExists() throws Exception
    {
        String query =
                "query Query {\n" +
                        "   firms(where: {" +
                        "       employees: { _exists: { firstName: { _in: %s } }}" +
                        "   }) " +
                        "   {\n" +
                        "       legalName\n" +
                        "   }\n" +
                        "}";
        GraphQLPlanCache cache = new GraphQLPlanCache(getExecutionCacheInstance());

        String expected = "{\"data\":{\"firms\":[{\"legalName\":\"Firm X\"},{\"legalName\":\"Firm B\"}]}}";
        runTest(String.format(query, "[\"John\", \"David\"]"), expected, cache);
        Assert.assertEquals(0, cache.getCache().stats().hitCount(), 0);
        Assert.assertEquals(1, cache.getCache().stats().missCount(), 0);

        String expected2 = "{\"data\":{\"firms\":[]}}";
        runTest(String.format(query, "[\"Name Doesn't Exist\"]"), expected2, cache);
        Assert.assertEquals(1, cache.getCache().stats().hitCount(), 0);
        Assert.assertEquals(1, cache.getCache().stats().missCount(), 0);

        String expected3 = "{\"data\":{\"firms\":{\"legalName\":\"Firm A\"}}}";
        runTest(String.format(query, "[\"Fabrice\"]"), expected3, cache);
        Assert.assertEquals(2, cache.getCache().stats().hitCount(), 0);
        Assert.assertEquals(1, cache.getCache().stats().missCount(), 0);
    }
}
