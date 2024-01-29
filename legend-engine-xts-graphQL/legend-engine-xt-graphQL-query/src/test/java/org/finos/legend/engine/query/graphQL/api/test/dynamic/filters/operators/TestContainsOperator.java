package org.finos.legend.engine.query.graphQL.api.test.dynamic.filters.operators;

import org.finos.legend.engine.query.graphQL.api.test.dynamic.filters.TestGraphQLDynamicFilters;
import org.junit.Test;

public class TestContainsOperator extends TestGraphQLDynamicFilters
{
    @Test
    public void test_String_eqIgnoreCase() throws Exception
    {
        String query = "query Query {\n" +
                "  allEmployees(where: { firstName: { _contains: \"Ant\" } }) {\n" +
                "      fullName" +
                "    }\n" +
                "  }";
        String expected = "{\"data\":{\"allEmployees\":{\"fullName()\":\"Anthony Allen\"}}}";
        runTest(query, expected);
    }
}
