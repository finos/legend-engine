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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.grammar.to.DEPRECATED_PureGrammarComposerCore;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.generation.extension.PlanGeneratorExtension;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.RelationalRootQueryTempTableGraphFetchExecutionNode;
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.engine.query.graphQL.api.cache.GraphQLPlanCache;
import org.finos.legend.engine.query.graphQL.api.debug.GraphQLDebug;
import org.finos.legend.engine.query.graphQL.api.debug.model.GraphFetchResult;
import org.finos.legend.engine.query.graphQL.api.execute.GraphQLExecute;
import org.finos.legend.engine.query.graphQL.api.execute.model.PlansResult;
import org.finos.legend.engine.query.graphQL.api.execute.model.Query;
import org.finos.legend.engine.shared.core.api.grammar.RenderStyle;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.util.ServiceLoader;

public class TestGraphQLAPI extends TestGraphQLApiAbstract
{
    @Test
    public void testGraphQLExecuteDevAPI_Relational() throws Exception
    {
        GraphQLExecute graphQLExecute = getGraphQLExecute();
        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mockRequest.getCookies()).thenReturn(new Cookie[0]);
        Query query = new Query();
        query.query = "query Query {\n" +
                "  allFirms {\n" +
                "      legalName,\n" +
                "      employees {\n" +
                "        firstName,\n" +
                "        lastName\n" +
                "      }\n" +
                "    }\n" +
                "  }";
        Response response = graphQLExecute.executeDev(mockRequest, "Project1", "Workspace1", "simple::model::Query", "simple::mapping::Map", "simple::runtime::Runtime", query, null);

        String expected = "{" +
                "\"data\":{" +
                "\"allFirms\":[" +
                "{\"legalName\":\"Firm X\",\"employees\":[{\"firstName\":\"Peter\",\"lastName\":\"Smith\"},{\"firstName\":\"John\",\"lastName\":\"Johnson\"},{\"firstName\":\"John\",\"lastName\":\"Hill\"},{\"firstName\":\"Anthony\",\"lastName\":\"Allen\"}]}," +
                "{\"legalName\":\"Firm A\",\"employees\":[{\"firstName\":\"Fabrice\",\"lastName\":\"Roberts\"}]}," +
                "{\"legalName\":\"Firm B\",\"employees\":[{\"firstName\":\"Oliver\",\"lastName\":\"Hill\"},{\"firstName\":\"David\",\"lastName\":\"Harris\"}]}" +
                "]" +
                "}" +
                "}";
        Assert.assertEquals(expected, responseAsString(response));
    }

    @Test
    public void testGraphQLExecuteProdAPI_Relational_With_Dataspace() throws Exception
    {
        GraphQLExecute graphQLExecute = getGraphQLExecute();
        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mockRequest.getCookies()).thenReturn(new Cookie[0]);
        Query query = new Query();
        query.query = "query Query {\n" +
                "  allFirms {\n" +
                "      legalName,\n" +
                "      employees {\n" +
                "        firstName,\n" +
                "        lastName\n" +
                "      }\n" +
                "    }\n" +
                "  }";
        Response response = graphQLExecute.executeProdWithDataspace(mockRequest, "org.finos.legend.graphql", "model.one", "1.0.0", "simple::dataspace", "defaultExecutionContext", "simple::model::Query", query, null);

        String expected = "{" +
                "\"data\":{" +
                "\"allFirms\":[" +
                "{\"legalName\":\"Firm X\",\"employees\":[{\"firstName\":\"Peter\",\"lastName\":\"Smith\"},{\"firstName\":\"John\",\"lastName\":\"Johnson\"},{\"firstName\":\"John\",\"lastName\":\"Hill\"},{\"firstName\":\"Anthony\",\"lastName\":\"Allen\"}]}," +
                "{\"legalName\":\"Firm A\",\"employees\":[{\"firstName\":\"Fabrice\",\"lastName\":\"Roberts\"}]}," +
                "{\"legalName\":\"Firm B\",\"employees\":[{\"firstName\":\"Oliver\",\"lastName\":\"Hill\"},{\"firstName\":\"David\",\"lastName\":\"Harris\"}]}" +
                "]" +
                "}" +
                "}";
        Assert.assertEquals(expected, responseAsString(response));
    }

    @Test
    public void testGraphQLExecuteProdAPI_Relational_With_Dataspace_With_Caching() throws Exception
    {
        GraphQLPlanCache cache = new GraphQLPlanCache(getExecutionCacheInstance());
        GraphQLExecute graphQLExecute = getGraphQLExecuteWithCache(cache);
        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mockRequest.getCookies()).thenReturn(new Cookie[0]);
        Query query = new Query();
        query.query = "query Query {\n" +
                "  allFirms {\n" +
                "      legalName,\n" +
                "      employees {\n" +
                "        firstName,\n" +
                "        lastName\n" +
                "      }\n" +
                "    }\n" +
                "  }";
        Response response = graphQLExecute.executeProdWithDataspace(mockRequest, "org.finos.legend.graphql", "model.one", "1.0.0", "simple::dataspace", "defaultExecutionContext", "simple::model::Query", query, null);

        String expected = "{" +
                "\"data\":{" +
                "\"allFirms\":[" +
                "{\"legalName\":\"Firm X\",\"employees\":[{\"firstName\":\"Peter\",\"lastName\":\"Smith\"},{\"firstName\":\"John\",\"lastName\":\"Johnson\"},{\"firstName\":\"John\",\"lastName\":\"Hill\"},{\"firstName\":\"Anthony\",\"lastName\":\"Allen\"}]}," +
                "{\"legalName\":\"Firm A\",\"employees\":[{\"firstName\":\"Fabrice\",\"lastName\":\"Roberts\"}]}," +
                "{\"legalName\":\"Firm B\",\"employees\":[{\"firstName\":\"Oliver\",\"lastName\":\"Hill\"},{\"firstName\":\"David\",\"lastName\":\"Harris\"}]}" +
                "]" +
                "}" +
                "}";
        Assert.assertEquals(expected, responseAsString(response));
        Assert.assertEquals(0, cache.getCache().stats().hitCount(), 0);
        Assert.assertEquals(1, cache.getCache().stats().missCount(), 0);

        response = graphQLExecute.executeProdWithDataspace(mockRequest, "org.finos.legend.graphql", "model.one", "1.0.0", "simple::dataspace", "defaultExecutionContext", "simple::model::Query", query, null);
        Assert.assertEquals(expected, responseAsString(response));
        Assert.assertEquals(1, cache.getCache().stats().hitCount(), 0);
        Assert.assertEquals(1, cache.getCache().stats().missCount(), 0);
    }

    @Test
    public void testGraphQLExecuteDevAPI_BiTemporalMilestoning_Root() throws Exception
    {
        GraphQLExecute graphQLExecute = getGraphQLExecute();
        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mockRequest.getCookies()).thenReturn(new Cookie[0]);
        Query query = new Query();
        query.query = "query Query {\n" +
                "  addressesBiTemporal (businessDate: \"2023-02-13\", processingDate: \"2023-02-13\") {\n" +
                "      line1\n" +
                "    }\n" +
                "  }";
        Response response = graphQLExecute.executeDev(mockRequest, "Project1", "Workspace1", "simple::model::Query", "simple::mapping::Map", "simple::runtime::Runtime", query, null);

        String expected = "{\"data\":{\"addressesBiTemporal\":{\"line1\":\"peter address\"}}}";
        Assert.assertEquals(expected, responseAsString(response));
    }

    @Test
    public void testGraphQLExecuteDevAPI_BusinessTemporalMilestoning_Root() throws Exception
    {
        GraphQLExecute graphQLExecute = getGraphQLExecute();
        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mockRequest.getCookies()).thenReturn(new Cookie[0]);
        Query query = new Query();
        query.query = "query Query {\n" +
                "  addressesBusinessTemporal (businessDate: \"2023-02-14\") {\n" +
                "      line1\n" +
                "    }\n" +
                "  }";
        Response response = graphQLExecute.executeDev(mockRequest, "Project1", "Workspace1", "simple::model::Query", "simple::mapping::Map", "simple::runtime::Runtime", query, null);

        String expected = "{\"data\":{\"addressesBusinessTemporal\":[{\"line1\":\"peter address\"},{\"line1\":\"John address\"}]}}";
        Assert.assertEquals(expected, responseAsString(response));
    }

    @Test
    public void testGraphQLExecuteDevAPI_ProcessingTemporalMilestoning_Root() throws Exception
    {
        GraphQLExecute graphQLExecute = getGraphQLExecute();
        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mockRequest.getCookies()).thenReturn(new Cookie[0]);
        Query query = new Query();
        query.query = "query Query {\n" +
                "  addressesProcessingTemporal (processingDate: \"2023-02-15\") {\n" +
                "      line1\n" +
                "    }\n" +
                "  }";
        Response response = graphQLExecute.executeDev(mockRequest, "Project1", "Workspace1", "simple::model::Query", "simple::mapping::Map", "simple::runtime::Runtime", query, null);

        String expected = "{\"data\":{\"addressesProcessingTemporal\":[{\"line1\":\"peter address\"},{\"line1\":\"John address\"},{\"line1\":\"John hill address\"}]}}";
        Assert.assertEquals(expected, responseAsString(response));
    }

    @Test
    public void testGraphQLExecuteDevAPI_Relational_WithDependencies() throws Exception
    {
        ModelManager modelManager = createModelManager();
        PlanExecutor executor = PlanExecutor.newPlanExecutorWithAvailableStoreExecutors();
        MutableList<PlanGeneratorExtension> generatorExtensions = Lists.mutable.withAll(ServiceLoader.load(PlanGeneratorExtension.class));
        GraphQLExecute graphQLExecute = new GraphQLExecute(modelManager, executor, metaDataServerConfiguration, (pm) -> PureCoreExtensionLoader.extensions().flatCollect(g -> g.extraPureCoreExtensions(pm.getExecutionSupport())), generatorExtensions.flatCollect(PlanGeneratorExtension::getExtraPlanTransformers));
        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mockRequest.getCookies()).thenReturn(new Cookie[0]);
        Query query = new Query();
        query.query = "query Query {\n" +
                "  allFirms {\n" +
                "      legalName,\n" +
                "      employees {\n" +
                "        firstName,\n" +
                "        lastName\n" +
                "      }\n" +
                "    }\n" +
                "  }";
        Response response = graphQLExecute.executeDev(mockRequest, "Project3", "Workspace1", "simple::model::Query", "simple::mapping::Map", "simple::runtime::Runtime", query, null);

        String expected = "{" +
                "\"data\":{" +
                "\"allFirms\":[" +
                "{\"legalName\":\"Firm X\",\"employees\":[{\"firstName\":\"Peter\",\"lastName\":\"Smith\"},{\"firstName\":\"John\",\"lastName\":\"Johnson\"},{\"firstName\":\"John\",\"lastName\":\"Hill\"},{\"firstName\":\"Anthony\",\"lastName\":\"Allen\"}]}," +
                "{\"legalName\":\"Firm A\",\"employees\":[{\"firstName\":\"Fabrice\",\"lastName\":\"Roberts\"}]}," +
                "{\"legalName\":\"Firm B\",\"employees\":[{\"firstName\":\"Oliver\",\"lastName\":\"Hill\"},{\"firstName\":\"David\",\"lastName\":\"Harris\"}]}" +
                "]" +
                "}" +
                "}";
        Assert.assertEquals(expected, responseAsString(response));
    }

    @Test
    public void testGraphQLExecuteDevAPI_RelationalWithParameter() throws Exception
    {
        GraphQLExecute graphQLExecute = getGraphQLExecute();
        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mockRequest.getCookies()).thenReturn(new Cookie[0]);
        Query query = new Query();
        query.query = "query Query {\n" +
                "  firmByLegalName(legalName: \"Firm X\") {\n" +
                "      legalName,\n" +
                "      employees {\n" +
                "        firstName,\n" +
                "        lastName\n" +
                "      }\n" +
                "    }\n" +
                "  }";
        Response response = graphQLExecute.executeDev(mockRequest, "Project1", "Workspace1", "simple::model::Query", "simple::mapping::Map", "simple::runtime::Runtime", query, null);

        String expected = "{" +
                "\"data\":{" +
                "\"firmByLegalName\":" +
                "{\"legalName\":\"Firm X\",\"employees\":[{\"firstName\":\"Peter\",\"lastName\":\"Smith\"},{\"firstName\":\"John\",\"lastName\":\"Johnson\"},{\"firstName\":\"John\",\"lastName\":\"Hill\"},{\"firstName\":\"Anthony\",\"lastName\":\"Allen\"}]}" +
                "}" +
                "}";
        Assert.assertEquals(expected, responseAsString(response));
        query.query = "query Query {\n" +
                "  firmByEmployees(firstName: \"Peter\",lastName: \"Smith\") {\n" +
                "      legalName,\n" +
                "      employees {\n" +
                "        firstName,\n" +
                "        lastName\n" +
                "      }\n" +
                "    }\n" +
                "  }";
        String expected2 = "{" +
                "\"data\":{" +
                "\"firmByEmployees\":" +
                "{\"legalName\":\"Firm X\",\"employees\":[{\"firstName\":\"Peter\",\"lastName\":\"Smith\"},{\"firstName\":\"John\",\"lastName\":\"Johnson\"},{\"firstName\":\"John\",\"lastName\":\"Hill\"},{\"firstName\":\"Anthony\",\"lastName\":\"Allen\"}]}" +
                "}" +
                "}";
        Response response2 = graphQLExecute.executeDev(mockRequest, "Project1", "Workspace1", "simple::model::Query", "simple::mapping::Map", "simple::runtime::Runtime", query, null);
        Assert.assertEquals(expected2, responseAsString(response2));
        query.query = "query Query {\n" +
                "  personsByLastNames(lastNames: [\"Smith\"]) {\n" +
                "     firstName,\n" +
                "     lastName\n" +
                "  }\n" +
                "}";
        String expected3 = "{" +
                "\"data\":{" +
                    "\"personsByLastNames\":" + "{\"firstName\":\"Peter\",\"lastName\":\"Smith\"}" +
                "}" +
            "}";
        Response response3 = graphQLExecute.executeDev(mockRequest, "Project1", "Workspace1", "simple::model::Query", "simple::mapping::Map", "simple::runtime::Runtime", query, null);
        Assert.assertEquals(expected3, responseAsString(response3));
        query.query = "query Query {\n" +
                "  personsByLastNames(lastNames: []) {\n" +
                "     firstName,\n" +
                "     lastName\n" +
                "  }\n" +
                "}";
        String expected4 = "{" +
                "\"data\":{" +
                "\"personsByLastNames\":" + "[]" +
                "}" +
                "}";
        Response response4 = graphQLExecute.executeDev(mockRequest, "Project1", "Workspace1", "simple::model::Query", "simple::mapping::Map", "simple::runtime::Runtime", query, null);
        Assert.assertEquals(expected4, responseAsString(response4));
        query.query = "query Query {\n" +
                "  personsByLastNames {\n" + // not providing array is treated the same way as providing an empty array
                "     firstName,\n" +
                "     lastName\n" +
                "  }\n" +
                "}";
        String expected5 = "{" +
                "\"data\":{" +
                "\"personsByLastNames\":" + "[]" +
                "}" +
                "}";
        Response response5 = graphQLExecute.executeDev(mockRequest, "Project1", "Workspace1", "simple::model::Query", "simple::mapping::Map", "simple::runtime::Runtime", query, null);
        Assert.assertEquals(expected5, responseAsString(response5));
        query.query = "query Query {\n" +
                "  personsByLastNames(lastNames: null) {\n" + // null is treated the same way as providing an empty array
                "     firstName,\n" +
                "     lastName\n" +
                "  }\n" +
                "}";
        String expected6 = "{" +
                "\"data\":{" +
                "\"personsByLastNames\":" + "[]" +
                "}" +
                "}";
        Response response6 = graphQLExecute.executeDev(mockRequest, "Project1", "Workspace1", "simple::model::Query", "simple::mapping::Map", "simple::runtime::Runtime", query, null);
        Assert.assertEquals(expected6, responseAsString(response6));


    }

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testGraphQLExecuteDevAPI_RelationalWithMissingMandatoryArgument() throws Exception
    {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Missing external parameter(s): legalName:String[1]");
        GraphQLExecute graphQLExecute = getGraphQLExecute();
        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mockRequest.getCookies()).thenReturn(new Cookie[0]);
        Query query = new Query();
        query.query = "query Query {\n" +
                "  firmByLegalName(name: \"Firm X\") {\n" +
                "      legalName,\n" +
                "      employees {\n" +
                "        firstName,\n" +
                "        lastName\n" +
                "      }\n" +
                "    }\n" +
                "  }";
        Response response = graphQLExecute.executeDev(mockRequest, "Project1", "Workspace1", "simple::model::Query", "simple::mapping::Map", "simple::runtime::Runtime", query, null);
        responseAsString(response);
    }

    @Test
    public void testGraphQLExecuteDevAPI_RelationalWithNullParameter() throws Exception
    {
        GraphQLExecute graphQLExecute = getGraphQLExecute();
        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mockRequest.getCookies()).thenReturn(new Cookie[0]);
        Query query = new Query();
        query.query = "query Query {\n" +
                "  personByNames(firstName: \"Peter\") {\n" +
                "        firstName,\n" +
                "        lastName\n" +
                "    }\n" +
                "  }";
        Response response = graphQLExecute.executeDev(mockRequest, "Project1", "Workspace1", "simple::model::Query", "simple::mapping::Map", "simple::runtime::Runtime", query, null);
        Assert.assertEquals("{\"data\":{\"personByNames\":{\"firstName\":\"Peter\",\"lastName\":\"Smith\"}}}", responseAsString(response));
        query.query = "query Query {\n" +
                "  personByNames(lastName: \"Johnson\") {\n" +
                "        firstName,\n" +
                "        lastName\n" +
                "    }\n" +
                "  }";
        Response response2 = graphQLExecute.executeDev(mockRequest, "Project1", "Workspace1", "simple::model::Query", "simple::mapping::Map", "simple::runtime::Runtime", query, null);
        Assert.assertEquals("{\"data\":{\"personByNames\":{\"firstName\":\"John\",\"lastName\":\"Johnson\"}}}", responseAsString(response2));

    }

    @Test
    public void testGraphQLExecuteDevAPIWithLetStatements_Relational() throws Exception
    {
        GraphQLExecute graphQLExecute = getGraphQLExecute();
        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mockRequest.getCookies()).thenReturn(new Cookie[0]);
        Query query = new Query();
        query.query = "query Query {\n" +
                "  selectEmployees(offset: 1,limit: 2) {\n" +
                "    firstName,\n" +
                "    lastName\n" +
                "  }\n" +
            "}";
        Response response = graphQLExecute.executeDev(mockRequest, "Project1", "Workspace1", "simple::model::Query", "simple::mapping::Map", "simple::runtime::Runtime", query, null);

        String expected = "{" +
                "\"data\":{" +
                    "\"selectEmployees\":[" +
                        "{\"firstName\":\"Peter\",\"lastName\":\"Smith\"}," +
                        "{\"firstName\":\"John\",\"lastName\":\"Johnson\"}" +
                    "]" +
                "}" +
            "}";
        Assert.assertEquals(expected, responseAsString(response));
    }

    @Test
    public void testGraphQLExecuteDevAPI_InMemory() throws Exception
    {
        GraphQLExecute graphQLExecute = getGraphQLExecute();
        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mockRequest.getCookies()).thenReturn(new Cookie[0]);
        Query query = new Query();
        query.query = "query Query {\n" +
                "  allFirms {\n" +
                "      legalName,\n" +
                "      employees {\n" +
                "        firstName,\n" +
                "        lastName\n" +
                "      }\n" +
                "    }\n" +
                "  }";
        Response response = graphQLExecute.executeDev(mockRequest, "Project2", "Workspace1", "simple::model::Query", "simple::mapping::Map", "simple::runtime::Runtime", query, null);

        String expected = "{" +
                "\"data\":{" +
                "\"allFirms\":[" +
                "{\"legalName\":\"Firm X\",\"employees\":[{\"firstName\":\"Peter\",\"lastName\":\"Smith\"},{\"firstName\":\"John\",\"lastName\":\"Johnson\"}]}," +
                "{\"legalName\":\"Firm A\",\"employees\":[{\"firstName\":\"Fabrice\",\"lastName\":\"Roberts\"}]}" +
                "]" +
                "}" +
                "}";
        Assert.assertEquals(expected, responseAsString(response));
    }

    @Test
    public void testGraphQLExecuteGeneratePlansDevAPI_Relational()
    {
        GraphQLExecute graphQLExecute = getGraphQLExecute();
        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mockRequest.getCookies()).thenReturn(new Cookie[0]);
        Query query = new Query();
        query.query = "query Query {\n" +
                "  allFirms {\n" +
                "      legalName,\n" +
                "      employees {\n" +
                "        firstName,\n" +
                "        lastName\n" +
                "      }\n" +
                "    }\n" +
                "  }";
        Response response = graphQLExecute.generatePlansDev(mockRequest, "Project1", "Workspace1", "simple::model::Query", "simple::mapping::Map", query, null);
        Assert.assertTrue(response.getEntity() instanceof PlansResult);
        Assert.assertEquals(1, ((PlansResult) response.getEntity()).executionPlansByProperty.size());
        Assert.assertTrue(((PlansResult) response.getEntity()).executionPlansByProperty.get(0).executionPlan instanceof SingleExecutionPlan);
        Assert.assertTrue(allChildNodes(((SingleExecutionPlan) ((PlansResult) response.getEntity()).executionPlansByProperty.get(0).executionPlan).rootExecutionNode).stream().anyMatch(e -> e instanceof RelationalRootQueryTempTableGraphFetchExecutionNode));
    }

    @Test
    public void testGraphQLDebugGenerateGraphFetchDevAPI()
    {
        ModelManager modelManager = createModelManager();
        MutableList<PlanGeneratorExtension> generatorExtensions = Lists.mutable.withAll(ServiceLoader.load(PlanGeneratorExtension.class));
        GraphQLDebug graphQLDebug = new GraphQLDebug(modelManager, metaDataServerConfiguration, (pm) -> PureCoreExtensionLoader.extensions().flatCollect(g -> g.extraPureCoreExtensions(pm.getExecutionSupport())));
        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mockRequest.getCookies()).thenReturn(new Cookie[0]);
        Query query = new Query();
        query.query = "query Query {\n" +
                "  allFirms {\n" +
                "      legalName,\n" +
                "      employees {\n" +
                "        firstName,\n" +
                "        lastName\n" +
                "      }\n" +
                "    }\n" +
                "  }";
        Response response = graphQLDebug.generateGraphFetchDev(mockRequest, "Workspace1", "Project1", "simple::model::Query", query, null);
        Assert.assertTrue(response.getEntity() instanceof GraphFetchResult);
        String expected = "#{\n" +
                "  simple::model::Query{\n" +
                "    allFirms{\n" +
                "      legalName,\n" +
                "      employees{\n" +
                "        firstName,\n" +
                "        lastName\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}#";
        Assert.assertEquals(expected, DEPRECATED_PureGrammarComposerCore.Builder.newInstance().withRenderStyle(RenderStyle.PRETTY).build().processGraphFetchTree(((GraphFetchResult) response.getEntity()).graphFetchTree, 2));
    }


    @Test
    public void testCacheUsed() throws Exception
    {
        GraphQLPlanCache cache = new GraphQLPlanCache(getExecutionCacheInstance());
        GraphQLExecute graphQLExecute = getGraphQLExecuteWithCache(cache);
        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mockRequest.getCookies()).thenReturn(new Cookie[0]);
        Query query = new Query();
        query.query = "query Query {\n" +
                "  firmByLegalName(legalName: \"Firm X\") {\n" +
                "      legalName,\n" +
                "      employees {\n" +
                "        firstName,\n" +
                "        lastName\n" +
                "      }\n" +
                "    }\n" +
                "  }";
        Response response = graphQLExecute.executeDev(mockRequest, "Project1", "Workspace1", "simple::model::Query", "simple::mapping::Map", "simple::runtime::Runtime", query, null);

        String expected = "{" +
                "\"data\":{" +
                "\"firmByLegalName\":" +
                "{\"legalName\":\"Firm X\",\"employees\":[{\"firstName\":\"Peter\",\"lastName\":\"Smith\"},{\"firstName\":\"John\",\"lastName\":\"Johnson\"},{\"firstName\":\"John\",\"lastName\":\"Hill\"},{\"firstName\":\"Anthony\",\"lastName\":\"Allen\"}]}" +
                "}" +
                "}";
        Assert.assertEquals(expected, responseAsString(response));
        Assert.assertEquals(0, cache.getCache().stats().hitCount(), 0);
        Assert.assertEquals(1, cache.getCache().stats().missCount(), 0);

        query.query = "query Query {\n" +
                "  firmByLegalName(legalName: \"Firm A\") {\n" +
                "      legalName,\n" +
                "      employees {\n" +
                "        firstName,\n" +
                "        lastName\n" +
                "      }\n" +
                "    }\n" +
                "  }";

        Response response2 = graphQLExecute.executeDev(mockRequest, "Project1", "Workspace1", "simple::model::Query", "simple::mapping::Map", "simple::runtime::Runtime", query, null);
        Assert.assertEquals(1, cache.getCache().stats().hitCount(), 0);
        Assert.assertEquals("{\"data\":{\"firmByLegalName\":{\"legalName\":\"Firm A\",\"employees\":[{\"firstName\":\"Fabrice\",\"lastName\":\"Roberts\"}]}}}", responseAsString(response2));

        //different query should miss the cache
        query.query = "query Query {\n" +
                "  allFirms {\n" +
                "      legalName,\n" +
                "      employees {\n" +
                "        firstName,\n" +
                "        lastName\n" +
                "      }\n" +
                "    }\n" +
                "  }";
        String expected3 = "{" +
                "\"data\":{" +
                "\"allFirms\":[" +
                "{\"legalName\":\"Firm X\",\"employees\":[{\"firstName\":\"Peter\",\"lastName\":\"Smith\"},{\"firstName\":\"John\",\"lastName\":\"Johnson\"},{\"firstName\":\"John\",\"lastName\":\"Hill\"},{\"firstName\":\"Anthony\",\"lastName\":\"Allen\"}]}," +
                "{\"legalName\":\"Firm A\",\"employees\":[{\"firstName\":\"Fabrice\",\"lastName\":\"Roberts\"}]}," +
                "{\"legalName\":\"Firm B\",\"employees\":[{\"firstName\":\"Oliver\",\"lastName\":\"Hill\"},{\"firstName\":\"David\",\"lastName\":\"Harris\"}]}" +
                "]" +
                "}" +
                "}";
        Response response3 = graphQLExecute.executeDev(mockRequest, "Project1", "Workspace1", "simple::model::Query", "simple::mapping::Map", "simple::runtime::Runtime", query, null);
        Assert.assertEquals(1, cache.getCache().stats().hitCount(), 0);
        Assert.assertEquals(2, cache.getCache().stats().missCount(), 0);
        Assert.assertEquals(expected3, responseAsString(response3));

    }

    @Test
    public void testCachingUsingNestedSelectionSets() throws Exception
    {
        GraphQLPlanCache cache = new GraphQLPlanCache(getExecutionCacheInstance());
        GraphQLExecute graphQLExecute = getGraphQLExecuteWithCache(cache);
        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mockRequest.getCookies()).thenReturn(new Cookie[0]);
        Query query = new Query();
        query.query = "query Query {\n" +
                "  firmByLegalName(legalName: \"Firm X\") {\n" +
                "      legalName,\n" +
                "      employees {\n" +
                "        firstName,\n" +
                "        lastName\n" +
                "      }\n" +
                "    }\n" +
                "  }";
        Response response = graphQLExecute.executeDev(mockRequest, "Project1", "Workspace2", "simple::model::Query", "simple::mapping::Map", "simple::runtime::Runtime", query, null);

        String expected = "{" +
                "\"data\":{" +
                "\"firmByLegalName\":" +
                "{\"legalName\":\"Firm X\",\"employees\":[{\"firstName\":\"Peter\",\"lastName\":\"Smith\"},{\"firstName\":\"John\",\"lastName\":\"Johnson\"},{\"firstName\":\"John\",\"lastName\":\"Hill\"},{\"firstName\":\"Anthony\",\"lastName\":\"Allen\"}]}" +
                "}" +
                "}";
        Assert.assertEquals(expected, responseAsString(response));
        Assert.assertEquals(0, cache.getCache().stats().hitCount(), 0);
        Assert.assertEquals(1, cache.getCache().stats().missCount(), 0);

        query.query = "query Query {\n" +
                "  firmByLegalName(legalName: \"Firm A\") {\n" +
                "      legalName,\n" +
                "      employees {\n" +
                "        firstName,\n" +
                "        lastName,\n" +
                "        address{\n" +
                "           line1,\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }";

        Response response2 = graphQLExecute.executeDev(mockRequest, "Project1", "Workspace2", "simple::model::Query", "simple::mapping::Map", "simple::runtime::Runtime", query, null);
        Assert.assertEquals(0, cache.getCache().stats().hitCount(), 0);
        Assert.assertEquals(2, cache.getCache().stats().missCount(), 0);

        Assert.assertEquals("{\"data\":{\"firmByLegalName\":{\"legalName\":\"Firm A\",\"employees\":[{\"firstName\":\"Fabrice\",\"lastName\":\"Roberts\",\"address\":{\"line1\":\"Fabrice address\"}}]}}}", responseAsString(response2));
    }

    @Test
    public void testGraphQLExecuteDevAPI_EchoDirective() throws Exception
    {
        GraphQLPlanCache graphQLPlanCache = new GraphQLPlanCache(getExecutionCacheInstance());
        GraphQLExecute graphQLExecute = getGraphQLExecuteWithCache(graphQLPlanCache);
        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mockRequest.getCookies()).thenReturn(new Cookie[0]);
        Query query = new Query();
        query.query = "query Query {\n" +
                "  firms: allFirms @echo {\n" +
                "      legalName\n" +
                "    }\n" +
                "  }";
        String expected = "{" +
                "\"data\":{" +
                    "\"allFirms\":[" +
                        "{\"legalName\":\"Firm X\"}," +
                        "{\"legalName\":\"Firm A\"}," +
                        "{\"legalName\":\"Firm B\"}" +
                    "]" +
                "}," +
                "\"extensions\":{" +
                    "\"allFirms\":{" +
                        "\"echo\":true" +
                    "}" +
                "}" +
            "}";

        Response response = graphQLExecute.executeDev(mockRequest, "Project1", "Workspace1", "simple::model::Query", "simple::mapping::Map", "simple::runtime::Runtime", query, null);
        Assert.assertEquals(expected, responseAsString(response));
        Assert.assertEquals(0, graphQLPlanCache.getCache().stats().hitCount(), 0);
        Assert.assertEquals(1, graphQLPlanCache.getCache().stats().missCount(), 0);

        response = graphQLExecute.executeDev(mockRequest, "Project1", "Workspace1", "simple::model::Query", "simple::mapping::Map", "simple::runtime::Runtime", query, null);
        Assert.assertEquals(expected, responseAsString(response));
        Assert.assertEquals(1, graphQLPlanCache.getCache().stats().hitCount(), 0);
        Assert.assertEquals(1, graphQLPlanCache.getCache().stats().missCount(), 0);
    }
}
