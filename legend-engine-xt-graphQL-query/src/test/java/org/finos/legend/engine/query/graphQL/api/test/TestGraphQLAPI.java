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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.language.pure.grammar.to.DEPRECATED_PureGrammarComposerCore;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.language.pure.modelManager.sdlc.configuration.MetaDataServerConfiguration;
import org.finos.legend.engine.language.pure.modelManager.sdlc.configuration.ServerConnectionConfiguration;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.generation.extension.PlanGeneratorExtension;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.RelationalRootQueryTempTableGraphFetchExecutionNode;
import org.finos.legend.engine.query.graphQL.api.debug.GraphQLDebug;
import org.finos.legend.engine.query.graphQL.api.debug.model.GraphFetchResult;
import org.finos.legend.engine.query.graphQL.api.execute.GraphQLExecute;
import org.finos.legend.engine.query.graphQL.api.execute.model.PlansResult;
import org.finos.legend.engine.query.graphQL.api.execute.model.Query;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.api.grammar.RenderStyle;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.port.DynamicPortGenerator;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

public class TestGraphQLAPI
{
    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
    private static MetaDataServerConfiguration metaDataServerConfiguration;
    private static Server server;

    @BeforeClass
    public static void beforeClass() throws Exception
    {
        int serverPort = DynamicPortGenerator.generatePort();
        server = new Server(serverPort);
        HandlerCollection handlerCollection = new HandlerCollection();
        handlerCollection.setHandlers(new Handler[] {
                buildPMCDMetadataHandler("/api/projects/Project1/workspaces/Workspace1/pureModelContextData", "/org/finos/legend/engine/query/graphQL/api/test/Project1_Workspace1.pure"),
                buildPMCDMetadataHandler("/api/projects/Project2/workspaces/Workspace1/pureModelContextData", "/org/finos/legend/engine/query/graphQL/api/test/Project2_Workspace1.pure")
        });
        server.setHandler(handlerCollection);
        server.start();
        metaDataServerConfiguration = new MetaDataServerConfiguration(null, null, new ServerConnectionConfiguration("127.0.0.1", serverPort));
    }

    @AfterClass
    public static void afterClass() throws Exception
    {
        server.stop();
    }

    @Test
    public void testGraphQLExecuteDevAPI_Relational() throws Exception
    {
        ModelManager modelManager = new ModelManager(DeploymentMode.TEST);
        PlanExecutor executor = PlanExecutor.newPlanExecutorWithAvailableStoreExecutors();
        MutableList<PlanGeneratorExtension> generatorExtensions = Lists.mutable.withAll(ServiceLoader.load(PlanGeneratorExtension.class));
        GraphQLExecute graphQLExecute = new GraphQLExecute(modelManager, executor, metaDataServerConfiguration, (pm) -> generatorExtensions.flatCollect(g -> g.getExtraExtensions(pm)), generatorExtensions.flatCollect(PlanGeneratorExtension::getExtraPlanTransformers));
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
    public void testGraphQLExecuteDevAPI_InMemory() throws Exception
    {
        ModelManager modelManager = new ModelManager(DeploymentMode.TEST);
        PlanExecutor executor = PlanExecutor.newPlanExecutorWithAvailableStoreExecutors();
        MutableList<PlanGeneratorExtension> generatorExtensions = Lists.mutable.withAll(ServiceLoader.load(PlanGeneratorExtension.class));
        GraphQLExecute graphQLExecute = new GraphQLExecute(modelManager, executor, metaDataServerConfiguration, (pm) -> generatorExtensions.flatCollect(g -> g.getExtraExtensions(pm)), generatorExtensions.flatCollect(PlanGeneratorExtension::getExtraPlanTransformers));
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
        ModelManager modelManager = new ModelManager(DeploymentMode.TEST);
        PlanExecutor executor = PlanExecutor.newPlanExecutorWithAvailableStoreExecutors();
        MutableList<PlanGeneratorExtension> generatorExtensions = Lists.mutable.withAll(ServiceLoader.load(PlanGeneratorExtension.class));
        GraphQLExecute graphQLExecute = new GraphQLExecute(modelManager, executor, metaDataServerConfiguration, (pm) -> generatorExtensions.flatCollect(g -> g.getExtraExtensions(pm)), generatorExtensions.flatCollect(PlanGeneratorExtension::getExtraPlanTransformers));
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
        ModelManager modelManager = new ModelManager(DeploymentMode.TEST);
        MutableList<PlanGeneratorExtension> generatorExtensions = Lists.mutable.withAll(ServiceLoader.load(PlanGeneratorExtension.class));
        GraphQLDebug graphQLDebug = new GraphQLDebug(modelManager, metaDataServerConfiguration, (pm) -> generatorExtensions.flatCollect(g -> g.getExtraExtensions(pm)));
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

    private static Handler buildPMCDMetadataHandler(String path, String resourcePath) throws Exception
    {
        ContextHandler contextHandler = new ContextHandler(path);
        PureModelContextData pureModelContextData = PureGrammarParser.newInstance().parseModel(readModelContentFromResource(resourcePath));
        byte[] bytes = OBJECT_MAPPER.writeValueAsBytes(pureModelContextData);

        AbstractHandler handler = new AbstractHandler()
        {
            @Override
            public void handle(String s, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException
            {
                OutputStream stream = httpServletResponse.getOutputStream();
                stream.write(bytes);
                stream.flush();
            }
        };
        contextHandler.setHandler(handler);
        return contextHandler;
    }

    private static String readModelContentFromResource(String resourcePath)
    {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(Objects.requireNonNull(TestGraphQLAPI.class.getResourceAsStream(resourcePath)))))
        {
            return buffer.lines().collect(Collectors.joining("\n"));
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private static String responseAsString(Response response) throws IOException
    {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        StreamingOutput output = (StreamingOutput) response.getEntity();
        output.write(byteArrayOutputStream);
        return byteArrayOutputStream.toString("UTF-8");
    }

    private static List<ExecutionNode> allChildNodes(ExecutionNode node)
    {
        return Lists.mutable.of(node).withAll(node.childNodes().stream().flatMap(c -> allChildNodes(c).stream()).collect(Collectors.toList()));
    }
}
