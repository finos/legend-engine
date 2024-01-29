package org.finos.legend.engine.query.graphQL.api.test.dynamic.filters.operators;

import org.finos.legend.engine.query.graphQL.api.test.dynamic.filters.TestGraphQLDynamicFilters;
import org.junit.Test;

public class TestGtOperator extends TestGraphQLDynamicFilters
{
    @Test
    public void test_Int_gt() throws Exception
    {
        String query = "query Query {\n" +
                "  allEmployees(where: { age: { _gt: 50 } }) {\n" +
                "      fullName," +
                "      age" +
                "    }\n" +
                "  }";
        String expected = "{\"data\":{\"allEmployees\":[{\"fullName()\":\"Oliver Hill\",\"age\":60},{\"fullName()\":\"David Harris\",\"age\":55}]}}";
        runTest(query, expected);
    }

    @Test
    public void test_Float_gt() throws Exception
    {
        String query = "query Query {\n" +
                "  allEmployees(where: { bankBalance: { _gt: 5500.0 } }) {\n" +
                "      fullName," +
                "      bankBalance" +
                "    }\n" +
                "  }";
        String expected = "{\"data\":{\"allEmployees\":{\"fullName()\":\"Oliver Hill\",\"bankBalance\":6000.0}}}";
        runTest(query, expected);
    }

    @Test
    public void test_StrictDate_gt() throws Exception
    {
        String query = "query Query {\n" +
                "  allEmployees(where: { dateOfBirth: { _gt: \"1989-01-28\" } }) {\n" +
                "      fullName," +
                "      dateOfBirth" +
                "    }\n" +
                "  }";
        String expected = "{\"data\":{\"allEmployees\":[{\"fullName()\":\"Peter Smith\",\"dateOfBirth\":\"1999-01-28\"},{\"fullName()\":\"John Hill\",\"dateOfBirth\":\"1994-01-28\"}]}}";
        runTest(query, expected);
    }

    @Test
    public void test_DateTime_gt() throws Exception
    {
        String query = "query Query {\n" +
                "  allEmployees(where: { modifiedAt: { _gt: \"2024-01-28 00:00:00\" } }) {\n" +
                "      fullName," +
                "      modifiedAt" +
                "    }\n" +
                "  }";
        String expected = "{\"data\":{\"allEmployees\":{\"fullName()\":\"Peter Smith\",\"modifiedAt\":\"2024-01-29T00:00:00.000000000\"}}}";
        runTest(query, expected);
    }
}
