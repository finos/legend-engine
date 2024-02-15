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
import com.google.common.cache.CacheBuilder;
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
import org.finos.legend.engine.language.pure.modelManager.sdlc.SDLCLoader;
import org.finos.legend.engine.language.pure.modelManager.sdlc.configuration.MetaDataServerConfiguration;
import org.finos.legend.engine.language.pure.modelManager.sdlc.configuration.ServerConnectionConfiguration;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.cache.ExecutionCache;
import org.finos.legend.engine.plan.execution.cache.ExecutionCacheBuilder;
import org.finos.legend.engine.plan.generation.extension.PlanGeneratorExtension;
import org.finos.legend.engine.protocol.Protocol;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextPointer;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.engine.query.graphQL.api.cache.GraphQLCacheKey;
import org.finos.legend.engine.query.graphQL.api.cache.GraphQLPlanCache;
import org.finos.legend.engine.query.graphQL.api.execute.GraphQLExecute;
import org.finos.legend.engine.query.graphQL.api.execute.SerializedNamedPlans;
import org.finos.legend.engine.query.graphQL.api.execute.model.error.GraphQLErrorMain;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.port.DynamicPortGenerator;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.TimeZone;

public abstract class TestGraphQLApiAbstract
{
    protected static final ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
    protected static MetaDataServerConfiguration metaDataServerConfiguration;
    protected static Server server;

    @BeforeClass
    public static void beforeClass() throws Exception
    {
        int serverPort = DynamicPortGenerator.generatePort();
        server = new Server(serverPort);
        HandlerCollection handlerCollection = new HandlerCollection();
        handlerCollection.setHandlers(new Handler[] {
                buildPMCDMetadataHandler("/api/projects/Project1/workspaces/Workspace1/pureModelContextData", "/org/finos/legend/engine/query/graphQL/api/test/Project1_Workspace1.pure"),
                buildJsonHandler("/api/projects/Project1/workspaces/Workspace1/revisions/HEAD/upstreamProjects", "[]"),
                buildPMCDMetadataHandler("/projects/org.finos.legend.graphql/model.one/versions/1.0.0/pureModelContextData","/org/finos/legend/engine/query/graphQL/api/test/Project1_Workspace1.pure",new Protocol("pure", PureClientVersions.production),new PureModelContextPointer()),

                buildPMCDMetadataHandler("/api/projects/Project1/workspaces/Workspace2/pureModelContextData", "/org/finos/legend/engine/query/graphQL/api/test/Project1_Workspace2.pure"),
                buildJsonHandler("/api/projects/Project1/workspaces/Workspace2/revisions/HEAD/upstreamProjects", "[]"),

                buildPMCDMetadataHandler("/api/projects/Project2/workspaces/Workspace1/pureModelContextData", "/org/finos/legend/engine/query/graphQL/api/test/Project2_Workspace1.pure"),
                buildJsonHandler("/api/projects/Project2/workspaces/Workspace1/revisions/HEAD/upstreamProjects", "[]]"),

                buildPMCDMetadataHandler("/api/projects/Project3/workspaces/Workspace1/pureModelContextData", "/org/finos/legend/engine/query/graphQL/api/test/Project3_Workspace1.pure"),
                buildJsonHandler("/api/projects/Project3/workspaces/Workspace1/revisions/HEAD/upstreamProjects",  readModelContentFromResource("/org/finos/legend/engine/query/graphQL/api/test/Project3_upstreamProjects.json")),
                buildPMCDMetadataHandler("/projects/org.finos.legend.graphql/models/versions/2.0.1/pureModelContextData","/org/finos/legend/engine/query/graphQL/api/test/Project4_Version_2.0.1.pure",new Protocol("pure", PureClientVersions.production),new PureModelContextPointer()),

                buildPMCDMetadataHandler("/api/projects/Project5/workspaces/Workspace1/pureModelContextData", "/org/finos/legend/engine/query/graphQL/api/test/Project5_Workspace1.pure"),
                buildJsonHandler("/api/projects/Project5/workspaces/Workspace1/revisions/HEAD/upstreamProjects", "[]]"),

                buildJsonHandler("/api/projects/p3/workspaces/ws1/pureModelContextData", readModelContentFromResource("/org/finos/legend/engine/query/graphQL/api/test/pmcd_p3_ws1.json")),
                buildJsonHandler("/api/projects/p3/workspaces/ws1/revisions/HEAD/upstreamProjects", readModelContentFromResource("/org/finos/legend/engine/query/graphQL/api/test/p3_ws1_upstream_projects.json")),
                buildJsonHandler("/projects/org.finos.legend.abhishoya/first-project/versions/1.0.2/pureModelContextData", readModelContentFromResource("/org/finos/legend/engine/query/graphQL/api/test/pmcd_p1_1.0.2.json")),
                buildJsonHandler("/projects/org.finos.legend.abhishoya/second-project/versions/1.0.1/pureModelContextData", readModelContentFromResource("/org/finos/legend/engine/query/graphQL/api/test/pmcd_p2_1.0.1.json"))

        });
        server.setHandler(handlerCollection);
        server.start();
        metaDataServerConfiguration = new MetaDataServerConfiguration(null, new ServerConnectionConfiguration("127.0.0.1", serverPort), new ServerConnectionConfiguration("127.0.0.1", serverPort));
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @AfterClass
    public static void afterClass() throws Exception
    {
        server.stop();
        TimeZone.setDefault(TimeZone.getDefault());
    }

    protected ExecutionCache<GraphQLCacheKey, List<SerializedNamedPlans>> getExecutionCacheInstance()
    {
        return ExecutionCacheBuilder.buildExecutionCacheFromGuavaCache(CacheBuilder.newBuilder().recordStats().build());
    }

    protected GraphQLExecute getGraphQLExecute()
    {
        return getGraphQLExecuteWithCache(null);
    }

    protected GraphQLExecute getGraphQLExecuteWithCache(GraphQLPlanCache cache)
    {
        ModelManager modelManager = createModelManager();
        PlanExecutor executor = PlanExecutor.newPlanExecutorWithAvailableStoreExecutors();
        MutableList<PlanGeneratorExtension> generatorExtensions = Lists.mutable.withAll(ServiceLoader.load(PlanGeneratorExtension.class));
        return new GraphQLExecute(modelManager, executor, metaDataServerConfiguration, (pm) -> PureCoreExtensionLoader.extensions().flatCollect(g -> g.extraPureCoreExtensions(pm.getExecutionSupport())), generatorExtensions.flatCollect(PlanGeneratorExtension::getExtraPlanTransformers), cache);
    }

    protected static ModelManager createModelManager()
    {
        return new ModelManager(DeploymentMode.TEST, new SDLCLoader(metaDataServerConfiguration, null));
    }

    protected static Handler buildPMCDMetadataHandler(String path, String resourcePath) throws Exception
    {
        return buildPMCDMetadataHandler(path, resourcePath, new Protocol("pure", PureClientVersions.production), new PureModelContextPointer());
    }

    protected static Handler buildPMCDMetadataHandler(String path, String resourcePath, Protocol serializer, PureModelContextPointer pointer) throws Exception
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

    protected static Handler buildJsonHandler(String path, String json)
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

    protected static String readModelContentFromResource(String resourcePath)
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

    protected static String responseAsString(Response response) throws IOException
    {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Object entity = response.getEntity();
        if (entity instanceof StreamingOutput)
        {
            StreamingOutput output = (StreamingOutput) response.getEntity();
            output.write(byteArrayOutputStream);
            return byteArrayOutputStream.toString("UTF-8");
        }
        else if (entity instanceof GraphQLErrorMain)
        {
            return OBJECT_MAPPER.writeValueAsString(entity);
        }
        throw new RuntimeException("Unhandled exception");
    }

    protected static List<ExecutionNode> allChildNodes(ExecutionNode node)
    {
        return Lists.mutable.of(node).withAll(node.childNodes().stream().flatMap(c -> allChildNodes(c).stream()).collect(Collectors.toList()));
    }
}
