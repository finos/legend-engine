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

import com.fasterxml.jackson.databind.jsontype.NamedType;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.forms.MultiPartBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import io.prometheus.client.CollectorRegistry;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.finos.legend.engine.application.query.api.ApplicationQuery;
import org.finos.legend.engine.application.query.configuration.ApplicationQueryConfiguration;
import org.finos.legend.engine.authentication.LegendDefaultDatabaseAuthenticationFlowProviderConfiguration;
import org.finos.legend.engine.external.shared.format.extension.GenerationExtension;
import org.finos.legend.engine.external.shared.format.extension.GenerationMode;
import org.finos.legend.engine.external.shared.format.generations.loaders.CodeGenerators;
import org.finos.legend.engine.external.shared.format.generations.loaders.SchemaGenerators;
import org.finos.legend.engine.external.shared.format.imports.loaders.CodeImports;
import org.finos.legend.engine.external.shared.format.imports.loaders.SchemaImports;
import org.finos.legend.engine.external.shared.format.model.api.ExternalFormats;
import org.finos.legend.engine.language.pure.compiler.api.Compile;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.api.grammarToJson.GrammarToJson;
import org.finos.legend.engine.language.pure.grammar.api.grammarToJson.TransformGrammarToJson;
import org.finos.legend.engine.language.pure.grammar.api.jsonToGrammar.JsonToGrammar;
import org.finos.legend.engine.language.pure.grammar.api.jsonToGrammar.TransformJsonToGrammar;
import org.finos.legend.engine.language.pure.grammar.api.relationalOperationElement.RelationalOperationElementGrammarToJson;
import org.finos.legend.engine.language.pure.grammar.api.relationalOperationElement.RelationalOperationElementJsonToGrammar;
import org.finos.legend.engine.language.pure.grammar.api.relationalOperationElement.TransformRelationalOperationElementGrammarToJson;
import org.finos.legend.engine.language.pure.grammar.api.relationalOperationElement.TransformRelationalOperationElementJsonToGrammar;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.language.pure.modelManager.sdlc.SDLCLoader;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.api.ExecutePlanLegacy;
import org.finos.legend.engine.plan.execution.api.ExecutePlanStrategic;
import org.finos.legend.engine.plan.execution.service.api.ServiceModelingApi;
import org.finos.legend.engine.plan.execution.stores.inMemory.plugin.InMemory;
import org.finos.legend.engine.plan.execution.stores.relational.api.RelationalExecutorInformation;
import org.finos.legend.engine.plan.execution.stores.relational.connection.api.schema.SchemaExplorationApi;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.Relational;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.RelationalStoreExecutor;
import org.finos.legend.engine.plan.execution.stores.service.plugin.ServiceStore;
import org.finos.legend.engine.plan.generation.extension.PlanGeneratorExtension;
import org.finos.legend.engine.plan.generation.transformers.LegendPlanTransformers;
import org.finos.legend.engine.protocol.pure.v1.PureProtocolObjectMapperFactory;
import org.finos.legend.engine.query.graphQL.api.debug.GraphQLDebug;
import org.finos.legend.engine.query.graphQL.api.execute.GraphQLExecute;
import org.finos.legend.engine.query.graphQL.api.grammar.GraphQLGrammar;
import org.finos.legend.engine.query.pure.api.Execute;
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
import org.finos.legend.engine.shared.core.vault.Vault;
import org.finos.legend.engine.shared.core.vault.VaultConfiguration;
import org.finos.legend.engine.shared.core.vault.VaultFactory;
import org.finos.legend.pure.generated.Root_meta_pure_router_extension_RouterExtension;
import org.finos.legend.server.pac4j.LegendPac4jBundle;
import org.finos.legend.server.shared.bundles.ChainFixingFilterHandler;
import org.finos.legend.server.shared.bundles.HostnameHeaderBundle;
import org.slf4j.Logger;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.ws.rs.container.DynamicFeature;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;

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
        bootstrap.addBundle(new HostnameHeaderBundle());
        bootstrap.addBundle(new LegendPac4jBundle<>(serverConfiguration -> serverConfiguration.pac4j));
        bootstrap.addBundle(new SessionAttributeBundle());
        bootstrap.addBundle(new MultiPartBundle());
        PureProtocolObjectMapperFactory.withPureProtocolExtensions(bootstrap.getObjectMapper());
        VaultFactory.withVaultConfigurationExtensions(bootstrap.getObjectMapper());
        ObjectMapperFactory.withStandardConfigurations(bootstrap.getObjectMapper());

        bootstrap.getObjectMapper().registerSubtypes(new NamedType(LegendDefaultDatabaseAuthenticationFlowProviderConfiguration.class, "legendDefault"));
    }

    @Override
    public void run(ServerConfiguration serverConfiguration, Environment environment)
    {
        loadVaults(serverConfiguration.vaults);

        this.environment = environment;
        DeploymentStateAndVersions.DEPLOYMENT_MODE = serverConfiguration.deployment.mode;

        SDLCLoader sdlcLoader = new SDLCLoader(serverConfiguration.metadataserver, null);
        ModelManager modelManager = new ModelManager(serverConfiguration.deployment.mode, sdlcLoader);

        ChainFixingFilterHandler.apply(environment.getApplicationContext(), serverConfiguration.filterPriorities);

        RelationalStoreExecutor relationalStoreExecutor = (RelationalStoreExecutor) Relational.build(serverConfiguration.relationalexecution);
        PlanExecutor planExecutor = PlanExecutor.newPlanExecutor(relationalStoreExecutor, ServiceStore.build(), InMemory.build());

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
        environment.jersey().register(new RelationalExecutorInformation());

        // Grammar
        environment.jersey().register(new GrammarToJson());
        environment.jersey().register(new JsonToGrammar());
        environment.jersey().register(new RelationalOperationElementGrammarToJson());
        environment.jersey().register(new RelationalOperationElementJsonToGrammar());
        environment.jersey().register(new TransformGrammarToJson());
        environment.jersey().register(new TransformJsonToGrammar());
        environment.jersey().register(new TransformRelationalOperationElementGrammarToJson());
        environment.jersey().register(new TransformRelationalOperationElementJsonToGrammar());

        // Relational
        environment.jersey().register(new SchemaExplorationApi(modelManager, relationalStoreExecutor));

        // Compilation
        environment.jersey().register((DynamicFeature) (resourceInfo, context) -> context.register(new InflateInterceptor()));
        environment.jersey().register(new Compile(modelManager));

        // Generation and Import
        MutableList<GenerationExtension> genExtensions = Iterate.addAllTo(ServiceLoader.load(GenerationExtension.class), Lists.mutable.empty());
        environment.jersey().register(new CodeGenerators(modelManager, genExtensions.select(p -> p.getMode() == GenerationMode.Code).collect(GenerationExtension::getGenerationDescription).select(Objects::nonNull)));
        environment.jersey().register(new CodeImports(modelManager, genExtensions.select(p -> p.getMode() == GenerationMode.Code).collect(GenerationExtension::getImportDescription).select(Objects::nonNull)));
        environment.jersey().register(new SchemaGenerators(modelManager, genExtensions.select(p -> p.getMode() == GenerationMode.Schema).collect(GenerationExtension::getGenerationDescription).select(Objects::nonNull)));
        environment.jersey().register(new SchemaImports(modelManager, genExtensions.select(p -> p.getMode() == GenerationMode.Schema).collect(GenerationExtension::getImportDescription).select(Objects::nonNull)));
        genExtensions.forEach(p -> environment.jersey().register(p.getService(modelManager)));

        // Execution
        MutableList<PlanGeneratorExtension> generatorExtensions = Lists.mutable.withAll(ServiceLoader.load(PlanGeneratorExtension.class));
        Function<PureModel, RichIterable<? extends Root_meta_pure_router_extension_RouterExtension>> routerExtensions = (PureModel pureModel) -> generatorExtensions.flatCollect(e -> e.getExtraRouterExtensions(pureModel));
        environment.jersey().register(new Execute(modelManager, planExecutor, routerExtensions, LegendPlanTransformers.transformers));
        environment.jersey().register(new ExecutePlanStrategic(planExecutor));
        environment.jersey().register(new ExecutePlanLegacy(planExecutor));

        // GraphQL
        environment.jersey().register(new GraphQLGrammar());
        environment.jersey().register(new GraphQLExecute(modelManager, planExecutor));
        environment.jersey().register(new GraphQLDebug(modelManager));

        // Service
        environment.jersey().register(new ServiceModelingApi(modelManager, serverConfiguration.deployment.mode));

        // Query
        environment.jersey().register(new ApplicationQuery(ApplicationQueryConfiguration.getMongoClient()));

        // Global
        environment.jersey().register(new JsonInformationExceptionMapper());
        environment.jersey().register(new CatchAllExceptionMapper());

        // External Format
        environment.jersey().register(new ExternalFormats(modelManager));

        enableCors(environment);
    }

    private void loadVaults(List<VaultConfiguration> vaultConfigurations)
    {
        if (vaultConfigurations != null)
        {
            ListIterate.forEach(vaultConfigurations, v -> Vault.INSTANCE.registerImplementation(VaultFactory.generateVaultImplementationFromConfiguration(v)));
        }
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
