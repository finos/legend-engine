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

package org.finos.legend.engine.query.graphQL.api.test.dynamic.filters;

import org.finos.legend.engine.query.graphQL.api.cache.GraphQLPlanCache;
import org.finos.legend.engine.query.graphQL.api.execute.GraphQLExecute;
import org.finos.legend.engine.query.graphQL.api.execute.model.Query;
import org.finos.legend.engine.query.graphQL.api.test.TestGraphQLApiAbstract;
import org.junit.Assert;
import org.mockito.Mockito;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.io.*;

public abstract class TestGraphQLDynamicFilters extends TestGraphQLApiAbstract
{
    protected void runTest(String queryString, String expectedResponse) throws Exception
    {
        runTest(queryString, expectedResponse, null);
    }

    protected void runTest(String queryString, String expectedResponse, GraphQLPlanCache planCache) throws Exception
    {
        Assert.assertEquals(expectedResponse, runQuery(queryString, planCache));
    }

    protected String runQuery(String queryString, GraphQLPlanCache planCache) throws IOException
    {
        GraphQLExecute graphQLExecute = getGraphQLExecuteWithCache(planCache);
        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mockRequest.getCookies()).thenReturn(new Cookie[0]);
        Query query = new Query();
        query.query = queryString;
        Response response = graphQLExecute.executeDev(mockRequest, "Project5", "Workspace1", "simple::model::Query", "simple::mapping::Map", "simple::runtime::Runtime", query, null);
        return responseAsString(response);
    }
}
