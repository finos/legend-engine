// Copyright 2026 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.postgres.e2e;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.compiler.api.Compile;
import org.finos.legend.engine.language.pure.grammar.api.grammarToJson.GrammarToJson;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.generation.extension.PlanGeneratorExtension;
import org.finos.legend.engine.protocol.pure.v1.PureProtocolObjectMapperFactory;
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.engine.query.pure.api.Execute;
import org.finos.legend.engine.query.sql.api.CatchAllExceptionMapper;
import org.finos.legend.engine.query.sql.api.MockPac4jFeature;
import org.finos.legend.engine.query.sql.api.schema.SqlSchema;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.servlet.ServletRegistration;
import org.glassfish.grizzly.servlet.WebappContext;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ServiceLoader;

public class E2eEngineHttpServer
{
    private final HttpServer httpServer;
    private final int port;

    public E2eEngineHttpServer(ModelManager modelManager)
    {
        MutableList<PlanGeneratorExtension> generatorExtensions = Lists.mutable.withAll(ServiceLoader.load(PlanGeneratorExtension.class));
        Execute execute = new Execute(
                modelManager,
                PlanExecutor.newPlanExecutorWithAvailableStoreExecutors(),
                pm -> (RichIterable) PureCoreExtensionLoader.extensions().flatCollect(g -> g.extraPureCoreExtensions(pm.getExecutionSupport())),
                generatorExtensions.flatCollect(PlanGeneratorExtension::getExtraPlanTransformers));
        ResourceConfig resources = new ResourceConfig()
                .register(new JacksonJsonProvider(PureProtocolObjectMapperFactory.getNewObjectMapper()))
                .register(new GrammarToJson())
                .register(new Compile(modelManager))
                .register(execute)
                .register(new SqlSchema(modelManager))
                .register(new MockPac4jFeature())
                .register(new CatchAllExceptionMapper());
        this.port = freePort();
        this.httpServer = HttpServer.createSimpleServer(null, "127.0.0.1", this.port);
        WebappContext context = new WebappContext("e2e-engine", "/api");
        ServletRegistration registration = context.addServlet("jersey", new ServletContainer(resources));
        registration.addMapping("/*");
        context.deploy(this.httpServer);
        try
        {
            this.httpServer.start();
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Could not start the embedded engine HTTP server", e);
        }
    }

    public int getPort()
    {
        return this.port;
    }

    private static int freePort()
    {
        try (ServerSocket socket = new ServerSocket(0))
        {
            return socket.getLocalPort();
        }
        catch (IOException e)
        {
            throw new IllegalStateException("No free port for the embedded engine HTTP server", e);
        }
    }

    public void shutDown()
    {
        this.httpServer.shutdownNow();
    }
}
