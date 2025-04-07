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

package org.finos.legend.engine.ide;

import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import io.federecio.dropwizard.swagger.SwaggerResource;
import java.util.concurrent.TimeUnit;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.finos.legend.engine.ide.api.*;
import org.finos.legend.engine.ide.api.concept.Concept;
import org.finos.legend.engine.ide.api.concept.MovePackageableElements;
import org.finos.legend.engine.ide.api.concept.RenameConcept;
import org.finos.legend.engine.ide.api.debug.Debugging;
import org.finos.legend.engine.ide.api.execution.function.Execute;
import org.finos.legend.engine.ide.api.execution.go.ExecuteGo;
import org.finos.legend.engine.ide.api.execution.test.ExecuteTests;
import org.finos.legend.engine.ide.api.find.FindInSources;
import org.finos.legend.engine.ide.api.find.FindPureFile;
import org.finos.legend.engine.ide.api.find.FindTextPreview;
import org.finos.legend.engine.ide.api.source.UpdateSource;
import org.finos.legend.engine.ide.session.PureSession;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.finos.legend.engine.shared.core.vault.aws.AWSVaultImplementation;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositorySet;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.RepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

public abstract class PureIDEServer extends Application<ServerConfiguration>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PureIDEServer.class);

    private PureSession pureSession;

    @Override
    public void initialize(Bootstrap<ServerConfiguration> bootstrap)
    {
        bootstrap.addBundle(new SwaggerBundle<ServerConfiguration>()
        {
            @Override
            protected SwaggerBundleConfiguration getSwaggerBundleConfiguration(
                    ServerConfiguration configuration)
            {
                return configuration.swagger;
            }
        });
        bootstrap.addBundle(new AssetsBundle("/web/ide", "/ide", "index.html", "static"));
    }

    @Override
    public void run(ServerConfiguration configuration, Environment environment) throws Exception
    {
        environment.jersey().setUrlPattern("/*");
        environment.jersey().register(new SwaggerResource(
                "",
                configuration.swagger.getSwaggerViewConfiguration(),
                configuration.swagger.getSwaggerOAuth2Configuration(),
                configuration.swagger.getContextRoot() +
                        (configuration.swagger.getContextRoot().endsWith("/") ? "" : "/") + "api")
        );

        this.pureSession = new PureSession(configuration.sourceLocationConfiguration, configuration.debugMode != null && configuration.debugMode, this.getRepositories(configuration.sourceLocationConfiguration, configuration.requiredRepositories));

        environment.jersey().register(new Concept(pureSession));
        environment.jersey().register(new RenameConcept(pureSession));
        environment.jersey().register(new MovePackageableElements(pureSession));

        environment.jersey().register(new Execute(pureSession));
        environment.jersey().register(new ExecuteGo(pureSession));
        environment.jersey().register(new ExecuteTests(pureSession));
        environment.jersey().register(new Debugging(pureSession));

        environment.jersey().register(new FindInSources(pureSession));
        environment.jersey().register(new FindPureFile(pureSession));
        environment.jersey().register(new FindTextPreview((pureSession)));

        environment.jersey().register(new UpdateSource(pureSession));

        environment.jersey().register(new Activities(pureSession));
        environment.jersey().register(new FileManagement(pureSession));
        environment.jersey().register(new LifeCycle(pureSession));
        environment.jersey().register(new PureRuntimeOptions(pureSession));

        environment.jersey().register(new Suggestion(pureSession));

        environment.jersey().register(new Service(pureSession));

        enableCors(environment);

        postInit();

        if (configuration.initPureRuntimeBeforeStart == null || configuration.initPureRuntimeBeforeStart)
        {
            long ct = System.currentTimeMillis();
            LOGGER.info("**************************************\n" +
                    "**** Initializing Pure runtime... ****\n" +
                    "**************************************"
            );
            try
            {
                this.pureSession.getPureRuntime().initialize();
                LOGGER.info("Initialization of Pure runtime completed in {}s.", TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - ct));
            }
            catch (Exception e)
            {
                LOGGER.warn("Initialization of Pure runtime failed in {}s.", TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - ct), e);
            }
        }
    }

    protected void postInit()
    {
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

    private MutableList<RepositoryCodeStorage> getRepositories(org.finos.legend.engine.ide.SourceLocationConfiguration sourceLocationConfiguration, List<String> requiredRepositories)
    {
        Map<CodeRepository, RepositoryCodeStorage> repoToCodeStorageMap = Maps.mutable.empty();
        repoToCodeStorageMap.putAll(CodeRepositoryProviderHelper.findCodeRepositories().toMap(r -> r, ClassLoaderCodeStorage::new));
        repoToCodeStorageMap.putAll(this.buildRepositories(sourceLocationConfiguration).toMap(cs -> cs.getAllRepositories().getOnly(), cs -> cs));

        CodeRepositorySet codeRepositorySet = CodeRepositorySet.newBuilder().withCodeRepositories(repoToCodeStorageMap.keySet()).build();

        if (requiredRepositories != null)
        {
            MutableSet<String> requiredSet = Sets.mutable.withAll(requiredRepositories);
            if (codeRepositorySet.hasRepository("pure_ide"))
            {
                requiredSet.add("pure_ide");
            }
            codeRepositorySet = codeRepositorySet.subset(requiredSet);
        }

        return codeRepositorySet.getRepositories().collect(repoToCodeStorageMap::get, Lists.mutable.ofInitialCapacity(codeRepositorySet.size()));
    }

    public PureSession getPureSession()
    {
        return pureSession;
    }

    protected abstract MutableList<RepositoryCodeStorage> buildRepositories(SourceLocationConfiguration sourceLocationConfiguration);
}
