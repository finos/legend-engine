// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.server;

import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.forms.MultiPartBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import io.prometheus.client.CollectorRegistry;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.finos.legend.engine.language.pure.compiler.api.Compile;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.api.grammarToJson.TransformGrammarToJson;
import org.finos.legend.engine.language.pure.grammar.api.jsonToGrammar.TransformJsonToGrammar;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.language.pure.modelManager.sdlc.SDLCLoader;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.api.ExecutePlan;
import org.finos.legend.engine.plan.execution.stores.inMemory.plugin.InMemory;
import org.finos.legend.engine.plan.generation.transformers.LegendPlanTransformers;
import org.finos.legend.engine.protocol.pure.v1.PureProtocolObjectMapperFactory;
import org.finos.legend.engine.server.core.ServerShared;
import org.finos.legend.engine.server.core.api.CurrentUser;
import org.finos.legend.engine.server.core.api.Info;
import org.finos.legend.engine.server.core.api.Memory;
import org.finos.legend.engine.server.core.exceptionMappers.CatchAllExceptionMapper;
import org.finos.legend.engine.server.core.exceptionMappers.JsonInformationExceptionMapper;
import org.finos.legend.engine.server.core.session.SessionAttributeBundle;
import org.finos.legend.engine.server.core.session.SessionTracker;
import org.finos.legend.engine.server.core.session.api.SessionInfo;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentStateAndVersions;
import org.finos.legend.engine.shared.core.operational.http.InflateInterceptor;
import org.finos.legend.engine.shared.core.url.EngineUrlStreamHandlerFactory;
import org.finos.legend.server.pac4j.LegendPac4jBundle;
import org.finos.legend.server.shared.bundles.ChainFixingFilterHandler;
import org.finos.legend.server.shared.bundles.HostnameHeaderBundle;
import org.finos.legend.server.shared.bundles.OpenTracingBundle;
import org.finos.legend.engine.query.pure.api.Execute;
import org.slf4j.Logger;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.ws.rs.container.DynamicFeature;
import java.util.EnumSet;

public class Server extends Application<ServerConfiguration>
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");

    private Environment environment;

    public static void main(String[] args) throws Exception
    {
        EngineUrlStreamHandlerFactory.initialize();
        new Server().run(args);
    }

    @Override
    public void initialize(Bootstrap<ServerConfiguration> bootstrap)
    {
        bootstrap.addBundle(new AssetsBundle("/web", "/", "legend_index.html"));
        bootstrap.addBundle(new SwaggerBundle<ServerConfiguration>()
        {
            @Override
            protected SwaggerBundleConfiguration getSwaggerBundleConfiguration(
                    ServerConfiguration configuration)
            {
                return configuration.swagger;
            }
        });
        bootstrap.addBundle(new OpenTracingBundle(Lists.mutable.empty(), Lists.mutable.of("/api/server/v1/info")));
        bootstrap.addBundle(new HostnameHeaderBundle());
        bootstrap.addBundle(new LegendPac4jBundle<>(serverConfiguration -> serverConfiguration.pac4j));
        bootstrap.addBundle(new SessionAttributeBundle());
        bootstrap.addBundle(new MultiPartBundle());
        PureProtocolObjectMapperFactory.withPureProtocolExtensions(bootstrap.getObjectMapper());
        ObjectMapperFactory.withStandardConfigurations(bootstrap.getObjectMapper());
    }

    @Override
    public void run(ServerConfiguration serverConfiguration, Environment environment)
    {
        this.environment = environment;
        DeploymentStateAndVersions.DEPLOYMENT_MODE = serverConfiguration.deployment.mode;

        SDLCLoader sdlcLoader = new SDLCLoader(serverConfiguration.metadataserver, null);
        ModelManager modelManager = new ModelManager(serverConfiguration.deployment.mode, sdlcLoader);

        ChainFixingFilterHandler.apply(environment.getApplicationContext(), serverConfiguration.filterPriorities);

        PlanExecutor planExecutor = PlanExecutor.newPlanExecutor(InMemory.build());

        // Session Management
        SessionTracker sessionTracker = new SessionTracker();
        environment.servlets().setSessionHandler(new SessionHandler());
        environment.servlets().addServletListeners(sessionTracker);
        environment.jersey().register(new SessionInfo(sessionTracker));

        // API & Swagger
        environment.jersey().setUrlPattern("/api/*");
        ServerShared.registerSwagger(environment, serverConfiguration.swagger);

        // Server
        environment.jersey().register(new Info(serverConfiguration.deployment, serverConfiguration.opentracing));
        environment.jersey().register(new CurrentUser());
        environment.jersey().register(new Memory());

        // Grammar
        environment.jersey().register(new TransformGrammarToJson());
        environment.jersey().register(new TransformJsonToGrammar());

        // Compilation
        environment.jersey().register((DynamicFeature) (resourceInfo, context) -> context.register(new InflateInterceptor()));
        environment.jersey().register(new Compile(modelManager));

        // Generation and Import
        // WIP: These APIs are placeholders until we open-source external format supports
        environment.jersey().register(new GenerationAndImportApi());

        // Execution
        // Should use: core_pure_extensions_extension.Root_meta_pure_router_extension_defaultExtensions__RouterExtension_MANY_(modelManager.)
        environment.jersey().register(new Execute(modelManager, planExecutor, (PureModel pureModel) -> Lists.mutable.empty(), LegendPlanTransformers.transformers));
        environment.jersey().register(new ExecutePlan(planExecutor));

        // Global
        environment.jersey().register(new JsonInformationExceptionMapper());
        environment.jersey().register(new CatchAllExceptionMapper());

        enableCors(environment);
    }

    public void shutDown() throws Exception
    {
        this.environment.getApplicationContext().getServer().stop();
        CollectorRegistry.defaultRegistry.clear();
    }

    private void enableCors(Environment environment)
    {
        FilterRegistration.Dynamic corsFilter = environment.servlets().addFilter("CORS", CrossOriginFilter.class);
        corsFilter.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "GET,PUT,POST,DELETE,OPTIONS");
        corsFilter.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*");
        corsFilter.setInitParameter(CrossOriginFilter.ALLOWED_TIMING_ORIGINS_PARAM, "*");
        corsFilter.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, "X-Requested-With,Content-Type,Accept,Origin,Access-Control-Allow-Credentials,x-b3-parentspanid,x-b3-sampled,x-b3-spanid,x-b3-traceid");
        corsFilter.setInitParameter(CrossOriginFilter.CHAIN_PREFLIGHT_PARAM, "false");
        corsFilter.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, "*");
    }
}