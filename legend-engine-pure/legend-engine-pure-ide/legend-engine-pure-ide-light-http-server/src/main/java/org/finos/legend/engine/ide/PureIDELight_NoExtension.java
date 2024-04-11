//  Copyright 2023 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.ide;

import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import io.federecio.dropwizard.swagger.SwaggerResource;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.finos.legend.engine.pure.runtime.compiler.interpreted.natives.LegendCompileMixedProcessorSupport;
import org.finos.legend.engine.ide.api.Activities;
import org.finos.legend.engine.ide.api.FileManagement;
import org.finos.legend.engine.ide.api.LifeCycle;
import org.finos.legend.engine.ide.api.Service;
import org.finos.legend.engine.ide.api.Suggestion;
import org.finos.legend.engine.ide.api.concept.Concept;
import org.finos.legend.engine.ide.api.concept.MovePackageableElements;
import org.finos.legend.engine.ide.api.concept.RenameConcept;
import org.finos.legend.engine.ide.api.execution.function.Execute;
import org.finos.legend.engine.ide.api.execution.go.ExecuteGo;
import org.finos.legend.engine.ide.api.execution.test.ExecuteTests;
import org.finos.legend.engine.ide.api.find.FindInSources;
import org.finos.legend.engine.ide.api.find.FindPureFile;
import org.finos.legend.engine.ide.api.find.FindTextPreview;
import org.finos.legend.engine.ide.api.source.UpdateSource;
import org.finos.legend.engine.ide.session.PureSession;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositorySet;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.RepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import java.util.EnumSet;

public class PureIDELight_NoExtension extends Application<ServerConfiguration>
{
    private PureSession pureSession;

    public PureIDELight_NoExtension()
    {
    }

    public static void main(String[] args) throws Exception
    {
        new PureIDELight_NoExtension().run(args.length == 0 ? new String[]{"server", "legend-engine-pure-ide-light/src/main/resources/ideLightConfig.json"} : args);
    }

    public void initialize(Bootstrap<ServerConfiguration> bootstrap)
    {
        bootstrap.addBundle(new SwaggerBundle<ServerConfiguration>()
        {
            protected SwaggerBundleConfiguration getSwaggerBundleConfiguration(ServerConfiguration configuration)
            {
                return configuration.swagger;
            }
        });
        bootstrap.addBundle(new AssetsBundle("/web/ide", "/ide", "index.html", "static"));

        // Enable variable substitution with environment variables
        bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(true)));
    }

    public void run(ServerConfiguration configuration, Environment environment) throws Exception
    {
        environment.jersey().setUrlPattern("/*");
        environment.jersey().register(new SwaggerResource("", configuration.swagger.getSwaggerViewConfiguration(), configuration.swagger.getSwaggerOAuth2Configuration(), configuration.swagger.getContextRoot() + (configuration.swagger.getContextRoot().endsWith("/") ? "" : "/") + "api"));
        this.pureSession = new PureSession(configuration.sourceLocationConfiguration, this.getRepositories(configuration.sourceLocationConfiguration));
        environment.jersey().register(new Concept(this.pureSession));
        environment.jersey().register(new RenameConcept(this.pureSession));
        environment.jersey().register(new MovePackageableElements(this.pureSession));
        environment.jersey().register(new Execute(this.pureSession));
        environment.jersey().register(new ExecuteGo(this.pureSession));
        environment.jersey().register(new ExecuteTests(this.pureSession));
        environment.jersey().register(new FindInSources(this.pureSession));
        environment.jersey().register(new FindPureFile(this.pureSession));
        environment.jersey().register(new FindTextPreview(this.pureSession));
        environment.jersey().register(new UpdateSource(this.pureSession));
        environment.jersey().register(new Activities(this.pureSession));
        environment.jersey().register(new FileManagement(this.pureSession));
        environment.jersey().register(new LifeCycle(this.pureSession));
        environment.jersey().register(new Suggestion(this.pureSession));
        environment.jersey().register(new Service(this.pureSession));
        this.enableCors(environment);
        this.postInit();
    }

    protected void postInit()
    {
        FunctionExecutionInterpreted fe = (FunctionExecutionInterpreted) this.getPureSession().getFunctionExecution();
        fe.setProcessorSupport(new LegendCompileMixedProcessorSupport(fe.getRuntime().getContext(), fe.getRuntime().getModelRepository(), fe.getProcessorSupport()));
    }

    private void enableCors(Environment environment)
    {
        FilterRegistration.Dynamic corsFilter = environment.servlets().addFilter("CORS", CrossOriginFilter.class);
        corsFilter.setInitParameter("allowedMethods", "GET,PUT,POST,DELETE,OPTIONS");
        corsFilter.setInitParameter("allowedOrigins", "*");
        corsFilter.setInitParameter("allowedTimingOrigins", "*");
        corsFilter.setInitParameter("allowedHeaders", "X-Requested-With,Content-Type,Accept,Origin,Access-Control-Allow-Credentials,x-b3-parentspanid,x-b3-sampled,x-b3-spanid,x-b3-traceid");
        corsFilter.setInitParameter("chainPreflight", "false");
        corsFilter.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, "*");
    }

    private MutableList<RepositoryCodeStorage> getRepositories(SourceLocationConfiguration sourceLocationConfiguration)
    {
        MutableList<String> coreRepositories = CodeRepositorySet.newBuilder().withCodeRepositories(CodeRepositoryProviderHelper.findCodeRepositories()).build().subset("core", "pure_ide").getRepositories().collect(CodeRepository::getName).toList();
        // NOTE: here we make the IDE start with a very simple set of modules, we could make this richer, by including everything in the class paths
        // so that we have capabilities to do more advanced execution in Pure IDE, but even then, we should consider filter out some repos which are
        // not necessary but for some reasons, included
        ClassLoaderCodeStorage classPathCodeStorage = new ClassLoaderCodeStorage(CodeRepositoryProviderHelper.findCodeRepositories().toList().select((x) ->
                coreRepositories.contains(x.getName())));
        return Lists.mutable.with(classPathCodeStorage);
    }

    public PureSession getPureSession()
    {
        return this.pureSession;
    }
}
