// Copyright 2023 Goldman Sachs
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
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.language.pure.modelManager.sdlc.configuration.MetaDataServerConfiguration;
import org.finos.legend.engine.language.pure.modelManager.sdlc.configuration.ServerConnectionConfiguration;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.generation.extension.PlanGeneratorExtension;
import org.finos.legend.engine.protocol.Protocol;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextPointer;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.engine.query.graphQL.api.execute.GraphQLExecute;
import org.finos.legend.engine.query.graphQL.api.execute.model.Query;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionError;
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
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

public class TestTotalCountDirective
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
                buildPMCDMetadataHandler("/api/projects/Project1/workspaces/Workspace1/pureModelContextData", "/Project1_Workspace1.pure"),
                buildJsonHandler("/api/projects/Project1/workspaces/Workspace1/revisions/HEAD/upstreamProjects", "[]"),
        });
        server.setHandler(handlerCollection);
        server.start();
        metaDataServerConfiguration = new MetaDataServerConfiguration(null, new ServerConnectionConfiguration("127.0.0.1", serverPort), new ServerConnectionConfiguration("127.0.0.1", serverPort));
    }

    @AfterClass
    public static void afterClass() throws Exception
    {
        server.stop();
    }

    private GraphQLExecute getGraphQLExecute()
    {
        ModelManager modelManager = new ModelManager(DeploymentMode.TEST);
        PlanExecutor executor = PlanExecutor.newPlanExecutorWithAvailableStoreExecutors();
        MutableList<PlanGeneratorExtension> generatorExtensions = Lists.mutable.withAll(ServiceLoader.load(PlanGeneratorExtension.class));
        GraphQLExecute graphQLExecute = new GraphQLExecute(modelManager, executor, metaDataServerConfiguration, (pm) -> PureCoreExtensionLoader.extensions().flatCollect(g -> g.extraPureCoreExtensions(pm.getExecutionSupport())), generatorExtensions.flatCollect(PlanGeneratorExtension::getExtraPlanTransformers));
        return graphQLExecute;
    }

    @Test
    public void testGraphQLExecuteDevAPI_TotalCountDirective() throws Exception
    {
        GraphQLExecute graphQLExecute = getGraphQLExecute();
        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mockRequest.getCookies()).thenReturn(new Cookie[0]);
        Query query = new Query();
        query.query = "query Query {\n" +
                "  firms: allFirms @totalCount {\n" +
                "      legalName\n" +
                "    }\n" +
                "  }";
        Response response = graphQLExecute.executeDev(mockRequest, "Project1", "Workspace1", "simple::model::Query", "simple::mapping::Map", "simple::runtime::Runtime", query, null);

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
                "\"totalCount\":3" +
                "}" +
                "}" +
                "}";

        Assert.assertEquals(expected, responseAsString(response));
    }

    @Test
    public void testGraphQLExecuteDevAPI_TotalCountDirective_WithALimitingFunction() throws Exception
    {
        GraphQLExecute graphQLExecute = getGraphQLExecute();
        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mockRequest.getCookies()).thenReturn(new Cookie[0]);
        Query query = new Query();
        query.query = "query Query {\n" +
                "  selectEmployees(offset: 1,limit: 2) @totalCount {\n" +
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
                "}," +
                "\"extensions\":{" +
                "\"selectEmployees\":{" +
                "\"totalCount\":7" +
                "}" +
                "}" +
                "}";
        Assert.assertEquals(expected, responseAsString(response));
    }

    private static Handler buildPMCDMetadataHandler(String path, String resourcePath) throws Exception
    {
        return buildPMCDMetadataHandler(path, resourcePath, null, null);
    }

    private static Handler buildPMCDMetadataHandler(String path, String resourcePath, Protocol serializer, PureModelContextPointer pointer) throws Exception
    {
        ContextHandler contextHandler = new ContextHandler(path);
        PureModelContextData pureModelContextData = PureModelContextData.newBuilder().withOrigin(pointer).withSerializer(serializer).withPureModelContextData(PureGrammarParser.newInstance().parseModel(readModelContentFromResource(resourcePath))).build();
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

    private static Handler buildJsonHandler(String path, String json)
    {
        ContextHandler contextHandler = new ContextHandler(path);
        AbstractHandler handler = new AbstractHandler()
        {
            @Override
            public void handle(String s, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException
            {
                OutputStream stream = httpServletResponse.getOutputStream();
                stream.write(json.getBytes(StandardCharsets.UTF_8));
                stream.flush();
            }
        };
        contextHandler.setHandler(handler);
        return contextHandler;
    }

    private static String readModelContentFromResource(String resourcePath)
    {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(Objects.requireNonNull(TestTotalCountDirective.class.getResourceAsStream(resourcePath)))))
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
        Object entity = response.getEntity();
        if (entity instanceof StreamingOutput)
        {
            StreamingOutput output = (StreamingOutput) response.getEntity();
            output.write(byteArrayOutputStream);
            return byteArrayOutputStream.toString("UTF-8");
        }
        throw new RuntimeException(((ExceptionError) entity).getMessage());
    }

    private static List<ExecutionNode> allChildNodes(ExecutionNode node)
    {
        return Lists.mutable.of(node).withAll(node.childNodes().stream().flatMap(c -> allChildNodes(c).stream()).collect(Collectors.toList()));
    }
}
