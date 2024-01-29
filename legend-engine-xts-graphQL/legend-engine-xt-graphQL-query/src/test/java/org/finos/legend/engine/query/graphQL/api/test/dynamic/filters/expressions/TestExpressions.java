package org.finos.legend.engine.query.graphQL.api.test.dynamic.filters.expressions;

import org.finos.legend.engine.query.graphQL.api.test.dynamic.filters.*;
import org.junit.*;

public class TestExpressions extends TestGraphQLDynamicFilters
{
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
}
