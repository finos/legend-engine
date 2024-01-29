package org.finos.legend.engine.query.graphQL.api.test.dynamic.filters.misc;

import org.finos.legend.engine.query.graphQL.api.cache.*;
import org.finos.legend.engine.query.graphQL.api.test.dynamic.filters.*;
import org.junit.*;

import java.io.*;

public class TestExceptions extends TestGraphQLDynamicFilters
{
    private void test(String where, String expectedException) throws IOException {
        String query = String.format("query Query {\n" +
                "  allEmployees( %s ) {\n" +
                "      fullName" +
                "    }\n" +
                "  }", where);
        String result = runQuery(query, null);
        Assert.assertTrue(String.format("%s doesn't contain %s", result, expectedException), result.contains(expectedException));
    }
    @Test
    public void testExceptions() throws Exception
    {
        test(
            "where: { _or : [ { firstName: { _eq: \"John\" } } ] }",
            "_or should contain atleast two expressions"
        );

        test(
            "where: { _or :  { firstName: { _eq: \"John\" } } }",
            "_or should have a list value"
        );

        test(
            "where: { _and : [ { firstName: { _eq: \"John\" } } ] }",
            "_and should contain atleast two expressions"
        );

        test(
            "where: { _and :  { firstName: { _eq: \"John\" } } }",
            "_and should have a list value"
        );

        test(
            "where: { _and : [ { firstName: { _eq: \"John\" } }, { lastName: { _eq: 1 } } ] }",
            "Incorrect type of value provided for \\\"lastName\\\".Expected: String, Actual: Integer"
        );

        test(
            "where: { firstName : { _and: [ { _eq: \"John\" }, { _eq: \"David\" } ] } }",
            "Value for field firstName should only be an BooleanOperator or ColumnExpression but is AndExpression"
        );

        test(
            "where: { firstName : { _or: [ { _eq: \"John\" }, { _eq: \"David\" } ] } }",
            "Value for field firstName should only be an BooleanOperator or ColumnExpression but is OrExpression"
        );
    }
}
