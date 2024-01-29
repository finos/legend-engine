package org.finos.legend.engine.query.graphQL.api.test.dynamic.filters.operators;

import org.finos.legend.engine.query.graphQL.api.cache.GraphQLPlanCache;
import org.finos.legend.engine.query.graphQL.api.test.dynamic.filters.TestGraphQLDynamicFilters;
import org.junit.Test;

public class TestExistsOperator extends TestGraphQLDynamicFilters
{
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

        String expected = "{\"data\":{\"firms\":{\"legalName\":\"Firm X\"}}}";
        runTest(query, expected);
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

        String expected = "{\"data\":{\"firms\":{\"legalName\":\"Firm X\"}}}";
        runTest(query, expected);
    }
}
