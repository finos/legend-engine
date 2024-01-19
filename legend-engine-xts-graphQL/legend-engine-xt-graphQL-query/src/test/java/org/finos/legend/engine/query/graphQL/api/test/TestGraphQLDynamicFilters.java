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

package org.finos.legend.engine.query.graphQL.api.test;

import org.finos.legend.engine.query.graphQL.api.cache.GraphQLPlanCache;
import org.finos.legend.engine.query.graphQL.api.execute.GraphQLExecute;
import org.finos.legend.engine.query.graphQL.api.execute.model.Query;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Suite;
import org.mockito.Mockito;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

public class TestGraphQLDynamicFilters extends TestGraphQLApiAbstract
{
    private void runTest(String queryString, String expectedResponse) throws Exception
    {
        runTest(queryString, expectedResponse, null);
    }

    private void runTest(String queryString, String expectedResponse, GraphQLPlanCache planCache) throws Exception
    {
        GraphQLExecute graphQLExecute = getGraphQLExecuteWithCache(planCache);
        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mockRequest.getCookies()).thenReturn(new Cookie[0]);
        Query query = new Query();
        query.query = queryString;
        Response response = graphQLExecute.executeDev(mockRequest, "Project5", "Workspace1", "simple::model::Query", "simple::mapping::Map", "simple::runtime::Runtime", query, null);
        Assert.assertEquals(expectedResponse, responseAsString(response));
    }

    @Test
    public void testSimpleString() throws Exception
    {
        String query = "query Query {\n" +
                "  allEmployees(where: { firstName: { _eq: \"John\" } }) {\n" +
                "      fullName" +
                "    }\n" +
                "  }";
        String expected = "{\"data\":{\"allEmployees\":[{\"fullName()\":\"John Johnson\"},{\"fullName()\":\"John Hill\"}]}}";
        runTest(query, expected);
    }

    @Test
    public void testSimpleInt() throws Exception
    {
        String query = "query Query {\n" +
                "  allEmployees(where: { age: { _eq: 25 } }) {\n" +
                "      fullName," +
                "      age" +
                "    }\n" +
                "  }";
        String expected = "{\"data\":{\"allEmployees\":{\"fullName()\":\"Peter Smith\",\"age\":25}}}";
        runTest(query, expected);
    }

    @Test
    public void testSimpleFloat() throws Exception
    {
        String query = "query Query {\n" +
                "  allEmployees(where: { bankBalance: { _eq: 5000.0 } }) {\n" +
                "      fullName," +
                "      bankBalance" +
                "    }\n" +
                "  }";
        String expected = "{\"data\":{\"allEmployees\":{\"fullName()\":\"Fabrice Roberts\",\"bankBalance\":5000.0}}}";
        runTest(query, expected);
    }

    @Test
    public void testSimpleBoolean() throws Exception
    {
        String query = "query Query {\n" +
                "  allEmployees(where: { isAFullTimeEmployee: { _eq: true } }) {\n" +
                "      fullName," +
                "      isAFullTimeEmployee" +
                "    }\n" +
                "  }";
        String expected = "{\"data\":{\"allEmployees\":[{\"fullName()\":\"Peter Smith\",\"isAFullTimeEmployee\":true},{\"fullName()\":\"John Hill\",\"isAFullTimeEmployee\":true},{\"fullName()\":\"Fabrice Roberts\",\"isAFullTimeEmployee\":true},{\"fullName()\":\"David Harris\",\"isAFullTimeEmployee\":true}]}}";
        runTest(query, expected);
    }

    @Test
    public void testSimpleAnd() throws Exception
    {
        String query = "query Query {\n" +
                "  allEmployees(where: { _and : [ { firstName: { _eq: \"John\" } }, { lastName: { _eq: \"Hill\" } } ] }) {\n" +
                "      fullName" +
                "    }\n" +
                "  }";
        String expected = "{\"data\":{\"allEmployees\":{\"fullName()\":\"John Hill\"}}}";
        runTest(query, expected);
    }

    @Test
    public void testSimpleOr() throws Exception
    {
        String query = "query Query {\n" +
                "  allEmployees(where: { _or : [ { firstName: { _eq: \"John\" } }, { lastName: { _eq: \"Hill\" } } ] }) {\n" +
                "      fullName" +
                "    }\n" +
                "  }";
        String expected = "{\"data\":{\"allEmployees\":[{\"fullName()\":\"John Johnson\"},{\"fullName()\":\"John Hill\"},{\"fullName()\":\"Oliver Hill\"}]}}";
        runTest(query, expected);
    }

    @Test
    public void testSimpleExists() throws Exception
    {
        String query =
                "query Query {\n" +
                        "   firms(where: {" +
                        "       employees: { _exists : { firstName: { _eq: \"Peter\" } }}" +
                        "   }) " +
                        "   {\n" +
                        "       legalName\n" +
                        "   }\n" +
                        "}";
        GraphQLPlanCache cache = new GraphQLPlanCache(getExecutionCacheInstance());

        String expected = "{\"data\":{\"firms\":{\"legalName\":\"Firm X\"}}}";
        runTest(query, expected, cache);
    }

    @Test
    public void testSimpleIn() throws Exception
    {
        String query =
                "query Query {\n" +
                        "   firms(where: {" +
                        "       legalName: { _in: %s}" +
                        "   }) " +
                        "   {\n" +
                        "       legalName\n" +
                        "   }\n" +
                        "}";
        GraphQLPlanCache cache = new GraphQLPlanCache(getExecutionCacheInstance());

        String expected = "{\"data\":{\"firms\":[{\"legalName\":\"Firm X\"},{\"legalName\":\"Firm A\"}]}}";
        runTest(String.format(query, "[\"Firm X\", \"Firm A\"]"), expected, cache);
        Assert.assertEquals(0, cache.getCache().stats().hitCount(), 0);
        Assert.assertEquals(1, cache.getCache().stats().missCount(), 0);

        String expected2 = "{\"data\":{\"firms\":[{\"legalName\":\"Firm X\"},{\"legalName\":\"Firm B\"}]}}";
        runTest(String.format(query, "[\"Firm X\", \"Firm B\"]"), expected2, cache);
        Assert.assertEquals(1, cache.getCache().stats().hitCount(), 0);
        Assert.assertEquals(1, cache.getCache().stats().missCount(), 0);
    }

    @Test
    public void testSimpleContains() throws Exception
    {
        String query =
                "query Query {\n" +
                        "   allEmployees(where: {" +
                        "       firstName: { _contains: %s}" +
                        "   }) " +
                        "   {\n" +
                        "       firstName\n" +
                        "   }\n" +
                        "}";
        GraphQLPlanCache cache = new GraphQLPlanCache(getExecutionCacheInstance());

        String expected = "{\"data\":{\"allEmployees\":[{\"firstName\":\"John\"},{\"firstName\":\"John\"}]}}";
        runTest(String.format(query, "\"Jo\""), expected, cache);
        Assert.assertEquals(0, cache.getCache().stats().hitCount(), 0);
        Assert.assertEquals(1, cache.getCache().stats().missCount(), 0);

        String expected2 = "{\"data\":{\"allEmployees\":[{\"firstName\":\"Fabrice\"},{\"firstName\":\"David\"}]}}";
        runTest(String.format(query, "\"a\""), expected2, cache);
        Assert.assertEquals(1, cache.getCache().stats().hitCount(), 0);
        Assert.assertEquals(1, cache.getCache().stats().missCount(), 0);
    }

    @Test
    public void testSimplePaginated() throws Exception
    {
        String query =
                "query Query {\n" +
                        "   allEmployeesPaginated(pageNumber: %d, where: {" +
                        "       firstName: { _contains: %s }" +
                        "   }) " +
                        "   {\n" +
                        "       lastName\n" +
                        "   }\n" +
                        "}";
        GraphQLPlanCache cache = new GraphQLPlanCache(getExecutionCacheInstance());

        String expected = "{\"data\":{\"allEmployeesPaginated\":{\"lastName\":\"Hill\"}}}";
        runTest(String.format(query, 1, "\"Jo\""), expected, cache);
        Assert.assertEquals(0, cache.getCache().stats().hitCount(), 0);
        Assert.assertEquals(1, cache.getCache().stats().missCount(), 0);

        String expected2 = "{\"data\":{\"allEmployeesPaginated\":{\"lastName\":\"Johnson\"}}}";
        runTest(String.format(query, 2, "\"Jo\""), expected2, cache);
        Assert.assertEquals(1, cache.getCache().stats().hitCount(), 0);
        Assert.assertEquals(1, cache.getCache().stats().missCount(), 0);
    }

    @Test
    public void testNestedExists() throws Exception
    {
        String query =
                "query Query {\n" +
                        "   firms(where: {" +
                        "       employees: { _exists : { employer: { legalName: { _eq: \"Firm X\" } } }}" +
                        "   }) " +
                        "   {\n" +
                        "       legalName\n" +
                        "   }\n" +
                        "}";
        GraphQLPlanCache cache = new GraphQLPlanCache(getExecutionCacheInstance());

        String expected = "{\"data\":{\"firms\":{\"legalName\":\"Firm X\"}}}";
        runTest(query, expected, cache);
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

    @Test
    public void testExceptions() throws Exception
    {
        String query = "query Query {\n" +
                "  allEmployees(where: { _or : [ { firstName: { _eq: \"John\" } } ] }) {\n" +
                "      fullName" +
                "    }\n" +
                "  }";
        String expected = "{\"errors\":[{\"location\":[],\"message\":\"Assert failure at (resource:/platform/pure/basics/tests/assert.pure line:26 column:5), \\\"_or should contain atleast two expressions\\\"\",\"path\":[]}]}";
        runTest(query, expected);

        query = "query Query {\n" +
                "  allEmployees(where: { _and : [ { firstName: { _eq: \"John\" } } ] }) {\n" +
                "      fullName" +
                "    }\n" +
                "  }";
        expected = "{\"errors\":[{\"location\":[],\"message\":\"Assert failure at (resource:/platform/pure/basics/tests/assert.pure line:26 column:5), \\\"_and should contain atleast two expressions\\\"\",\"path\":[]}]}";
        runTest(query, expected);

        query = "query Query {\n" +
                "  allEmployees(where: { _and : [ { firstName: { _eq: \"John\" } }, { lastName: { _eq: 1 } } ] }) {\n" +
                "      fullName" +
                "    }\n" +
                "  }";
        expected = "{\"errors\":[{\"location\":[],\"message\":\"Assert failure at (resource:/platform/pure/basics/tests/assert.pure line:26 column:5), \\\"Incorrect type of value provided for \\\"lastName\\\".Expected: String, Actual: Integer\\\"\",\"path\":[]}]}";
        runTest(query, expected);

        query = "query Query {\n" +
                "  allEmployees(where: { firstName : { _and: [ { _eq: \"John\" }, { _eq: \"David\" } ] } }) {\n" +
                "      fullName" +
                "    }\n" +
                "  }";
        expected = "{\"errors\":[{\"location\":[],\"message\":\"Assert failure at (resource:/platform/pure/basics/tests/assert.pure line:26 column:5), \\\"Value for field firstName should only be an BooleanOperator or ColumnExpression but is AndExpression\\\"\",\"path\":[]}]}";
        runTest(query, expected);

        query = "query Query {\n" +
                "  allEmployees(where: { firstName : { _or: [ { _eq: \"John\" }, { _eq: \"David\" } ] } }) {\n" +
                "      fullName" +
                "    }\n" +
                "  }";
        expected = "{\"errors\":[{\"location\":[],\"message\":\"Assert failure at (resource:/platform/pure/basics/tests/assert.pure line:26 column:5), \\\"Value for field firstName should only be an BooleanOperator or ColumnExpression but is OrExpression\\\"\",\"path\":[]}]}";
        runTest(query, expected);
    }

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
