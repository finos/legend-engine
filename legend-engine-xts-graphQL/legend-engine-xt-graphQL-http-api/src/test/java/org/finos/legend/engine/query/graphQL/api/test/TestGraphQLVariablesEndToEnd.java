// Copyright 2026 Goldman Sachs
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

import net.javacrumbs.jsonunit.JsonAssert;
import org.finos.legend.engine.query.graphQL.api.execute.GraphQLExecute;
import org.finos.legend.engine.query.graphQL.api.execute.model.Query;
import org.junit.Test;
import org.mockito.Mockito;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

public class TestGraphQLVariablesEndToEnd extends TestGraphQLApiAbstract
{
    private static final String JOHN_HILL = "{\"firstName\":\"John\",\"lastName\":\"Hill\"}";
    private static final String JOHN_JOHNSON = "{\"firstName\":\"John\",\"lastName\":\"Johnson\"}";
    private static final String PETER_SMITH = "{\"firstName\":\"Peter\",\"lastName\":\"Smith\"}";

    private static String expected(String field, String... rows)
    {
        return "{\"data\":{\"" + field + "\":[" + String.join(",", rows) + "]}}";
    }

    private static String expectedSingle(String field, String row)
    {
        return "{\"data\":{\"" + field + "\":" + row + "}}";
    }

    private String run(String queryText, String variables) throws IOException
    {
        GraphQLExecute graphQLExecute = getGraphQLExecute();
        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mockRequest.getCookies()).thenReturn(new Cookie[0]);
        Query query = new Query();
        query.query = queryText;
        query.variables = variables;
        return responseAsString(graphQLExecute.executeDev(mockRequest, "Project6", "Workspace1", "simple::model::Query", "simple::mapping::Map", "simple::runtime::Runtime", query, null));
    }

    @Test
    public void listArgumentLiteral() throws Exception
    {
        JsonAssert.assertJsonEquals(expected("employeesByFirstNames", JOHN_HILL, JOHN_JOHNSON, PETER_SMITH),
                run("query { employeesByFirstNames(names: [\"John\", \"Peter\"]) { firstName lastName } }", null));
    }

    @Test
    public void listArgumentAsWholeVariable() throws Exception
    {
        JsonAssert.assertJsonEquals(expected("employeesByFirstNames", JOHN_HILL, JOHN_JOHNSON, PETER_SMITH),
                run("query($names: [String!]!) { employeesByFirstNames(names: $names) { firstName lastName } }", "{\"names\": [\"John\", \"Peter\"]}"));
    }

    @Test
    public void listArgumentWithVariableElement() throws Exception
    {
        JsonAssert.assertJsonEquals(expected("employeesByFirstNames", JOHN_HILL, JOHN_JOHNSON, PETER_SMITH),
                run("query($second: String!) { employeesByFirstNames(names: [\"John\", $second]) { firstName lastName } }", "{\"second\": \"Peter\"}"));
    }

    @Test
    public void objectArgumentLiteral() throws Exception
    {
        JsonAssert.assertJsonEquals(expectedSingle("employeesByNameFilter", PETER_SMITH),
                run("query { employeesByNameFilter(filter: {firstName: \"Peter\"}) { firstName lastName } }", null));
    }

    @Test
    public void objectArgumentWithVariableField() throws Exception
    {
        JsonAssert.assertJsonEquals(expectedSingle("employeesByNameFilter", PETER_SMITH),
                run("query($n: String!) { employeesByNameFilter(filter: {firstName: $n}) { firstName lastName } }", "{\"n\": \"Peter\"}"));
    }

    @Test
    public void objectArgumentAsWholeVariable() throws Exception
    {
        JsonAssert.assertJsonEquals(expectedSingle("employeesByNameFilter", PETER_SMITH),
                run("query($f: NameFilter!) { employeesByNameFilter(filter: $f) { firstName lastName } }", "{\"f\": {\"firstName\": \"Peter\"}}"));
    }


}
