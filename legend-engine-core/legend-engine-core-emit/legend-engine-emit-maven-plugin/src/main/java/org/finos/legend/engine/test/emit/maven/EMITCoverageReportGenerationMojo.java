// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.test.emit.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.finos.legend.engine.test.emit.EMITModelDiscovery;
import org.finos.legend.engine.test.emit.EMITModelLoader;
import org.finos.legend.engine.test.emit.catalog.EMITModelDescriptor;
import org.finos.legend.engine.test.emit.report.EMIT_to_HTML;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mojo(
        name = "generate-EMIT-coverage-report",
        defaultPhase = LifecyclePhase.GENERATE_RESOURCES,
        threadSafe = true
)
public class EMITCoverageReportGenerationMojo extends AbstractMojo
{
    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "${project.build.outputDirectory}/emit/emit-coverage.html", required = true)
    private File outputFilePath;

    @Parameter
    private List<String> includedRelativeSubpaths = Lists.fixedSize.with("src/test/resources/emit-models");

    @Parameter
    private Set<String> excludedDirectoryNames = Sets.fixedSize.with("target");

    @Parameter
    private List<String> excludedDirectoryNamePrefixes = Lists.fixedSize.with(".");

    @Parameter
    private List<String> excludedRelativeSubpaths = Lists.fixedSize.with("src/main");

    @Override
    public void execute() throws MojoExecutionException
    {
        Path repoRoot = findRepoRoot(project).getBasedir().toPath();
        List<Path> emitModelsDirs;
        try
        {
            emitModelsDirs = collectEmitModelsDirs(
                    repoRoot, includedRelativeSubpaths, excludedDirectoryNames,
                    excludedDirectoryNamePrefixes, excludedRelativeSubpaths);
        }
        catch (IOException e)
        {
            throw new MojoExecutionException("Failed to walk directory tree under " + repoRoot, e);
        }
        getLog().info("EMIT coverage report: " + emitModelsDirs.size() + " emit-models directories discovered under " + repoRoot);
        List<EMITModelDescriptor> descriptors;
        try
        {
            descriptors = parseDescriptorsUnder(emitModelsDirs);
        }
        catch (IOException e)
        {
            throw new MojoExecutionException("Failed to parse EMIT descriptors under " + repoRoot, e);
        }
        try
        {
            EMIT_to_HTML.writeHTML(descriptors, outputFilePath.toPath(), repoRoot);
        }
        catch (IOException e)
        {
            throw new MojoExecutionException("Failed to generate EMIT coverage report", e);
        }
        getLog().info("EMIT coverage report: " + descriptors.size() + " model(s) rendered to " + outputFilePath);
    }

    static List<Path> collectEmitModelsDirs(
            Path repoRoot,
            List<String> includedRelativeSubpaths,
            Set<String> excludedDirectoryNames,
            List<String> excludedDirectoryNamePrefixes,
            List<String> excludedRelativeSubpaths) throws IOException
    {
        if (repoRoot == null)
        {
            throw new IllegalArgumentException("repoRoot is required");
        }
        if (includedRelativeSubpaths == null || includedRelativeSubpaths.isEmpty())
        {
            throw new IllegalArgumentException("includedRelativeSubpaths must be non-null and non-empty");
        }
        Set<String> directoryNameExclusions = (excludedDirectoryNames == null) ? Collections.emptySet() : excludedDirectoryNames;
        List<String> directoryNamePrefixExclusions = (excludedDirectoryNamePrefixes == null) ? Collections.emptyList() : excludedDirectoryNamePrefixes;
        FileSystem fs = repoRoot.getFileSystem();
        List<Path> relativeSubpathInclusions = includedRelativeSubpaths.stream().map(fs::getPath).collect(Collectors.toList());
        List<Path> relativeSubpathExclusions = (excludedRelativeSubpaths == null) ? Collections.emptyList() : excludedRelativeSubpaths.stream().map(fs::getPath).collect(Collectors.toList());

        List<Path> emitModelsDirs = Lists.mutable.empty();
        Files.walkFileTree(repoRoot, new SimpleFileVisitor<Path>()
        {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
            {
                if (dir.equals(repoRoot))
                {
                    return FileVisitResult.CONTINUE;
                }
                String name = dir.getFileName().toString();
                if (directoryNamePrefixExclusions.stream().anyMatch(prefix -> !prefix.isEmpty() && name.startsWith(prefix)))
                {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                if (directoryNameExclusions.contains(name))
                {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                if (relativeSubpathExclusions.stream().anyMatch(dir::endsWith))
                {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                if (relativeSubpathInclusions.stream().anyMatch(dir::endsWith))
                {
                    emitModelsDirs.add(dir);
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }
        });
        emitModelsDirs.sort(null);
        return emitModelsDirs;
    }

    static List<EMITModelDescriptor> parseDescriptorsUnder(List<Path> emitModelsDirs) throws IOException
    {
        if (emitModelsDirs == null)
        {
            throw new IllegalArgumentException("emitModelsDirs is required");
        }
        List<EMITModelDescriptor> descriptors = Lists.mutable.empty();
        EMITModelLoader loader = new EMITModelLoader();
        for (Path dir : emitModelsDirs)
        {
            if (dir == null || !Files.isDirectory(dir))
            {
                continue;
            }
            for (Path yaml : EMITModelDiscovery.findEmitYamls(dir))
            {
                descriptors.add(loader.parseDescriptor(yaml));
            }
        }
        return descriptors;
    }


    static MavenProject findRepoRoot(MavenProject project)
    {
        if (project == null)
        {
            throw new IllegalArgumentException("project must not be null");
        }
        MavenProject current = project;
        while (current.getParent() != null && current.getParent().getBasedir() != null)
        {
            current = current.getParent();
        }
        return current;
    }
}
