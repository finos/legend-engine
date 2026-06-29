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

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.finos.legend.engine.test.emit.EMIT_to_HTML;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Mojo(
        name = "generate-EMIT-coverage-report",
        defaultPhase = LifecyclePhase.GENERATE_RESOURCES,
        threadSafe = true
)
public class EMITCoverageReportGenerationMojo extends AbstractMojo
{
    private static final String LEGEND_ENGINE_GROUP_ID = "org.finos.legend.engine";
    private static final String LEGEND_ENGINE_ARTIFACT_ID = "legend-engine";

    private static final Set<String> EXCLUDED_MODULE_ARTIFACT_IDS = Collections.singleton("legend-engine-emit-junit");

    private static final String EMIT_MODELS_SUBPATH = "emit-models";

    private static final String DEFAULT_TEST_RESOURCES_DIR = "src/test/resources";

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "${project.build.outputDirectory}/emit/emit-coverage.html", required = true)
    private Path outputFilePath;

    @Override
    public void execute() throws MojoExecutionException
    {
        MavenProject root = findLegendEngineRoot(project);
        Path repoRoot = root.getBasedir().toPath();
        List<Path> emitModelsDirs;
        try
        {
            emitModelsDirs = collectEmitModelsDirs(repoRoot);
        }
        catch (IOException | XmlPullParserException e)
        {
            throw new MojoExecutionException("Failed to walk module hierarchy under " + repoRoot, e);
        }
        getLog().info("EMIT coverage report: " + emitModelsDirs.size() + " emit-models directories discovered across the legend-engine multi-module tree (root " + repoRoot + ")");
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

    static List<Path> collectEmitModelsDirs(Path repoRoot) throws IOException, XmlPullParserException
    {
        List<Path> result = new ArrayList<>();
        collectEmitModelsDirsRecursive(repoRoot, result);
        return result;
    }

    private static void collectEmitModelsDirsRecursive(Path moduleDir, List<Path> result) throws IOException, XmlPullParserException
    {
        Path pomXml = moduleDir.resolve("pom.xml");
        if (!Files.isRegularFile(pomXml))
        {
            return;
        }
        Model model;
        try (Reader reader = Files.newBufferedReader(pomXml, StandardCharsets.UTF_8))
        {
            model = new MavenXpp3Reader().read(reader);
        }
        if (!EXCLUDED_MODULE_ARTIFACT_IDS.contains(model.getArtifactId()))
        {
            for (Path testResourceDir : effectiveTestResourceDirs(moduleDir, model))
            {
                Path emitModelsDir = testResourceDir.resolve(EMIT_MODELS_SUBPATH);
                if (Files.isDirectory(emitModelsDir))
                {
                    result.add(emitModelsDir);
                }
            }
        }
        for (String submodule : model.getModules())
        {
            collectEmitModelsDirsRecursive(moduleDir.resolve(submodule), result);
        }
    }

    private static List<Path> effectiveTestResourceDirs(Path moduleDir, Model model)
    {
        Build build = model.getBuild();
        if (build == null || build.getTestResources() == null || build.getTestResources().isEmpty())
        {
            return Collections.singletonList(moduleDir.resolve(DEFAULT_TEST_RESOURCES_DIR));
        }
        return Collections.emptyList();
    }

    static MavenProject findLegendEngineRoot(MavenProject project) throws MojoExecutionException
    {
        for (MavenProject p = project; p != null; p = p.getParent())
        {
            if (LEGEND_ENGINE_GROUP_ID.equals(p.getGroupId()) && LEGEND_ENGINE_ARTIFACT_ID.equals(p.getArtifactId()))
            {
                return p;
            }
        }
        throw new MojoExecutionException("Could not find " + LEGEND_ENGINE_GROUP_ID + ":" + LEGEND_ENGINE_ARTIFACT_ID + " in the parent chain of " + (project == null ? "<null project>" : (project.getGroupId() + ":" + project.getArtifactId())));
    }
}
