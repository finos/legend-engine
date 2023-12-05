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

import org.finos.legend.engine.query.graphQL.api.execute.GraphQLExecute;
import org.finos.legend.engine.query.graphQL.api.execute.model.Query;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

public class TestGraphQLDynamicFilters extends TestGraphQLApiAbstract
{
    private void runTest(String queryString, String expectedResponse) throws Exception
    {
        GraphQLExecute graphQLExecute = getGraphQLExecute();
        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mockRequest.getCookies()).thenReturn(new Cookie[0]);
        Query query = new Query();
        query.query = queryString;
        Response response = graphQLExecute.executeDev(mockRequest, "Project5", "Workspace1", "simple::model::Query", "simple::mapping::Map", "simple::runtime::Runtime", query, null);
        Assert.assertEquals(expectedResponse, responseAsString(response));
    }

    @Test
    public void testGraphQLExecuteDevAPI_DynamicFilter_Simple_FieldEqualsString() throws Exception
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
    public void testGraphQLExecuteDevAPI_DynamicFilter_Simple_FieldEqualsInteger() throws Exception
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
    public void testGraphQLExecuteDevAPI_DynamicFilter_Simple_FieldEqualsFloat() throws Exception
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
    public void testGraphQLExecuteDevAPI_DynamicFilter_Simple_FieldEqualsBoolean() throws Exception
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
    public void testGraphQLExecuteDevAPI_DynamicFilter_Simple_AndExpression() throws Exception
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
    public void testGraphQLExecuteDevAPI_DynamicFilter_Simple_OrExpression() throws Exception
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
    public void testGraphQLExecuteDevAPI_DynamicFilter_Simple_Exceptions() throws Exception
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
    }
}
