package org.finos.legend.engine.query.graphQL.api.test.dynamic.filters.operators;

import org.finos.legend.engine.query.graphQL.api.test.dynamic.filters.TestGraphQLDynamicFilters;
import org.junit.Test;

public class TestLtOperator extends TestGraphQLDynamicFilters
{
    @Test
    public void test_Int_lt() throws Exception
    {
        String query = "query Query {\n" +
                "  allEmployees(where: { age: { _lt: 35 } }) {\n" +
                "      fullName," +
                "      age" +
                "    }\n" +
                "  }";
        String expected = "{\"data\":{\"allEmployees\":[{\"fullName()\":\"Peter Smith\",\"age\":25},{\"fullName()\":\"John Hill\",\"age\":30}]}}";
        runTest(query, expected);
    }

    @Test
    public void test_Float_lt() throws Exception
    {
        String query = "query Query {\n" +
                "  allEmployees(where: { bankBalance: { _lt: 3000.0 } }) {\n" +
                "      fullName," +
                "      bankBalance" +
                "    }\n" +
                "  }";
        String expected = "{\"data\":{\"allEmployees\":{\"fullName()\":\"Peter Smith\",\"bankBalance\":2500.0}}}";
        runTest(query, expected);
    }

    @Test
    public void test_StrictDate_lt() throws Exception
    {
        String query = "query Query {\n" +
                "  allEmployees(where: { dateOfBirth: { _lt: \"1974-01-28\" } }) {\n" +
                "      fullName," +
                "      dateOfBirth" +
                "    }\n" +
                "  }";
        String expected = "{\"data\":{\"allEmployees\":[{\"fullName()\":\"Oliver Hill\",\"dateOfBirth\":\"1964-01-28\"},{\"fullName()\":\"David Harris\",\"dateOfBirth\":\"1969-01-28\"}]}}";
        runTest(query, expected);
    }

    @Test
    public void test_DateTime_lt() throws Exception
    {
        String query = "query Query {\n" +
                "  allEmployees(where: { modifiedAt: { _lt: \"2024-01-25 12:00:00\" } }) {\n" +
                "      fullName," +
                "      modifiedAt" +
                "    }\n" +
                "  }";
        String expected = "{\"data\":{\"allEmployees\":{\"fullName()\":\"David Harris\",\"modifiedAt\":\"2024-01-25T00:00:00.000000000\"}}}";
        runTest(query, expected);
    }
}
