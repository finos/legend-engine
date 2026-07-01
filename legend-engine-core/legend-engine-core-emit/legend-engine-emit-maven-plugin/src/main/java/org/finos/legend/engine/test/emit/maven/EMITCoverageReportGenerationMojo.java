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
import org.eclipse.collections.api.factory.Sets;
import org.finos.legend.engine.test.emit.EMIT_to_HTML;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
    private Path outputFilePath;

    @Parameter
    private Set<String> includedRelativeSubpaths = Sets.fixedSize.with("src/test/resources/emit-models");

    @Parameter
    private Set<String> excludedDirectoryNames = Sets.fixedSize.with("target", "legend-engine-emit-junit");

    @Parameter
    private Set<String> excludedDirectoryNamePrefixes = Sets.fixedSize.with(".");

    @Parameter
    private Set<String> excludedRelativeSubpaths = Sets.fixedSize.with("src/main");

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
        try
        {
            EMIT_to_HTML.generateFromEmitModelsDirs(repoRoot, emitModelsDirs, outputFilePath);
        }
        catch (IOException e)
        {
            throw new MojoExecutionException("Failed to generate EMIT coverage report", e);
        }
        getLog().info("EMIT coverage report: written to " + outputFilePath);
    }

    static List<Path> collectEmitModelsDirs(
            Path repoRoot,
            Set<String> includedRelativeSubpaths,
            Set<String> excludedDirectoryNames,
            Set<String> excludedDirectoryNamePrefixes,
            Set<String> excludedRelativeSubpaths) throws IOException
    {
        if (repoRoot == null)
        {
            throw new IllegalArgumentException("repoRoot is required");
        }
        if (includedRelativeSubpaths == null || includedRelativeSubpaths.isEmpty())
        {
            throw new IllegalArgumentException("includedRelativeSubpaths must be non-null and non-empty");
        }
        Set<String> dirNames = (excludedDirectoryNames == null) ? Collections.emptySet() : excludedDirectoryNames;
        Set<String> dirNamePrefixes = (excludedDirectoryNamePrefixes == null) ? Collections.emptySet() : excludedDirectoryNamePrefixes;
        Set<String> relativeSubpaths = (excludedRelativeSubpaths == null) ? Collections.emptySet() : excludedRelativeSubpaths;

        Set<Path> emitModelsDirs = new LinkedHashSet<>();
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
                for (String prefix : dirNamePrefixes)
                {
                    if (!prefix.isEmpty() && name.startsWith(prefix))
                    {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                }
                if (dirNames.contains(name))
                {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                String rel = relativize(repoRoot, dir);
                for (String subpath : relativeSubpaths)
                {
                    if (rel.equals(subpath) || rel.endsWith("/" + subpath))
                    {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                }
                for (String subpath : includedRelativeSubpaths)
                {
                    if (rel.equals(subpath) || rel.endsWith("/" + subpath))
                    {
                        emitModelsDirs.add(dir);
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
        List<Path> sorted = new ArrayList<>(emitModelsDirs);
        sorted.sort(null);
        return sorted;
    }

    private static String relativize(Path repoRoot, Path dir)
    {
        return repoRoot.relativize(dir).toString().replace(File.separatorChar, '/');
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
