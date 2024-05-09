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
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.collector.Collectors2;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.finos.legend.engine.ide.api.*;
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
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.RepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.fs.MutableFSCodeStorage;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public abstract class PureIDEServer extends Application<ServerConfiguration>
{
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

        this.pureSession = new PureSession(configuration.sourceLocationConfiguration, this.getRepositories(configuration.sourceLocationConfiguration, configuration.requiredRepositories));

        environment.jersey().register(new Concept(pureSession));
        environment.jersey().register(new RenameConcept(pureSession));
        environment.jersey().register(new MovePackageableElements(pureSession));

        environment.jersey().register(new Execute(pureSession));
        environment.jersey().register(new ExecuteGo(pureSession));
        environment.jersey().register(new ExecuteTests(pureSession));

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

        if (requiredRepositories != null && !requiredRepositories.isEmpty())
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

    protected MutableList<RepositoryCodeStorage> buildRepositories(SourceLocationConfiguration sourceLocationConfiguration)
    {
        MutableMap<CodeRepository, Path> codeRepositoryPathMap = Maps.mutable.empty();
        List<String> directoriesToSearch = sourceLocationConfiguration.directories != null && !sourceLocationConfiguration.directories.isEmpty()
                                            ? sourceLocationConfiguration.directories
                                            : Lists.mutable.of(".");

        for (String path : directoriesToSearch)
        {
            Map<CodeRepository, Path> p = findCodeRepositoriesAndMapToPath(path, sourceLocationConfiguration.pathPatternsToExclude);
            codeRepositoryPathMap.putAll(p);
        }

        MutableList<RepositoryCodeStorage> result = Lists.mutable.empty();
        codeRepositoryPathMap.forEachKeyValue((r, repoDefFilePath) ->
        {
            if (r instanceof GenericCodeRepository) //TODO is this correct/needed?
            {
                String dirName = r.getName();
                Path parentDirectory = repoDefFilePath.getParent();
                result.add(new MutableFSCodeStorage(r, parentDirectory.resolve(dirName)));
            }
        });

        return result;
    }

    //TODO: This should probably be moved to CodeRepositoryProviderHelper in legend-pure
    protected Map<CodeRepository, Path> findCodeRepositoriesAndMapToPath(String path, List<String> patternsToExclude)
    {
        Map<CodeRepository, Path> result = Maps.mutable.empty();
        FileSystem f = FileSystems.getDefault();
        MutableList<PathMatcher> excludeMatchers = patternsToExclude.stream().map(p -> f.getPathMatcher("glob:" + p)).collect(Collectors2.toList());

        try
        {
            Files.walkFileTree(Paths.get(path), new SimpleFileVisitor<Path>()
            {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, java.nio.file.attribute.BasicFileAttributes attrs)
                {
                    return excludeMatchers.anySatisfy(m -> m.matches(dir)) ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                {
                    if (file.toString().endsWith(".definition.json"))
                    {
                        result.put(GenericCodeRepository.build(file), file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
        return result;
    }
}
