package org.finos.legend.engine.query.graphQL.api.test.dynamic.filters.operators;

import org.finos.legend.engine.query.graphQL.api.test.dynamic.filters.TestGraphQLDynamicFilters;
import org.junit.Test;

public class TestEqIgnoreCaseOperator extends TestGraphQLDynamicFilters
{
    @Test
    public void test_String_eqIgnoreCase() throws Exception
    {
        String query = "query Query {\n" +
                "  allEmployees(where: { firstName: { _eqIgnoreCase: \"joHN\" } }) {\n" +
                "      fullName" +
                "    }\n" +
                "  }";
        String expected = "{\"data\":{\"allEmployees\":[{\"fullName()\":\"John Johnson\"},{\"fullName()\":\"John Hill\"}]}}";
        runTest(query, expected);
    }
}
