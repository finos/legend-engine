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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TestEMITCoverageReportGenerationMojo
{
    private static final String LEGEND_ENGINE_GROUP_ID = "org.finos.legend.engine";
    private static final String LEGEND_ENGINE_ARTIFACT_ID = "legend-engine";

    @Test
    public void findLegendEngineRootReturnsProjectWhenItIsTheLegendEngineRoot() throws MojoExecutionException
    {
        MavenProject root = projectOf(LEGEND_ENGINE_GROUP_ID, LEGEND_ENGINE_ARTIFACT_ID);

        MavenProject found = EMITCoverageReportGenerationMojo.findLegendEngineRoot(root);

        Assertions.assertSame(root, found,
                "A project that IS the legend-engine root must be returned as-is");
    }

    @Test
    public void findLegendEngineRootWalksParentChainToFindRoot() throws MojoExecutionException
    {
        MavenProject root = projectOf(LEGEND_ENGINE_GROUP_ID, LEGEND_ENGINE_ARTIFACT_ID);
        MavenProject child = projectOf(LEGEND_ENGINE_GROUP_ID, "legend-engine-emit-maven-plugin");
        Mockito.when(child.getParent()).thenReturn(root);

        MavenProject found = EMITCoverageReportGenerationMojo.findLegendEngineRoot(child);

        Assertions.assertSame(root, found,
                "findLegendEngineRoot must walk getParent() from the consuming module up to the legend-engine root");
    }

    @Test
    public void findLegendEngineRootWalksMultipleAncestors() throws MojoExecutionException
    {
        MavenProject root = projectOf(LEGEND_ENGINE_GROUP_ID, LEGEND_ENGINE_ARTIFACT_ID);
        MavenProject parent = projectOf(LEGEND_ENGINE_GROUP_ID, "legend-engine-core-emit");
        MavenProject grandchild = projectOf(LEGEND_ENGINE_GROUP_ID, "legend-engine-emit-maven-plugin");
        Mockito.when(parent.getParent()).thenReturn(root);
        Mockito.when(grandchild.getParent()).thenReturn(parent);

        MavenProject found = EMITCoverageReportGenerationMojo.findLegendEngineRoot(grandchild);

        Assertions.assertSame(root, found,
                "findLegendEngineRoot must traverse the full parent chain, not just one level");
    }

    @Test
    public void findLegendEngineRootThrowsWhenNoLegendEngineInChain()
    {
        MavenProject parent = projectOf("com.example.fork", "some-parent");
        MavenProject standalone = projectOf("com.example.fork", "some-module");
        Mockito.when(standalone.getParent()).thenReturn(parent);

        MojoExecutionException ex = Assertions.assertThrows(MojoExecutionException.class,
                () -> EMITCoverageReportGenerationMojo.findLegendEngineRoot(standalone));

        Assertions.assertTrue(ex.getMessage().contains("com.example.fork:some-module"),
                "Error message should identify the starting project. Was: " + ex.getMessage());
        Assertions.assertTrue(ex.getMessage().contains(LEGEND_ENGINE_GROUP_ID + ":" + LEGEND_ENGINE_ARTIFACT_ID),
                "Error message should mention the expected legend-engine coordinates. Was: " + ex.getMessage());
    }

    @Test
    public void findLegendEngineRootRequiresBothGroupAndArtifactIdToMatch()
    {
        MavenProject foreign = projectOf("com.example.fork", LEGEND_ENGINE_ARTIFACT_ID);

        MojoExecutionException ex = Assertions.assertThrows(MojoExecutionException.class,
                () -> EMITCoverageReportGenerationMojo.findLegendEngineRoot(foreign));

        Assertions.assertTrue(ex.getMessage().contains("com.example.fork"),
                "Error message should mention the actual groupId. Was: " + ex.getMessage());
    }

    @Test
    public void findLegendEngineRootThrowsOnNull()
    {
        MojoExecutionException ex = Assertions.assertThrows(MojoExecutionException.class,
                () -> EMITCoverageReportGenerationMojo.findLegendEngineRoot(null));

        Assertions.assertTrue(ex.getMessage().contains("<null"),
                "Error message should call out the null starting project. Was: " + ex.getMessage());
    }

    @Test
    public void collectEmitModelsDirsWalksTopLevelModulesAndFindsTheirEmitModels(@TempDir Path repo) throws Exception
    {
        writePom(repo, "legend-engine", Arrays.asList("moduleA", "moduleB"));
        writePom(repo.resolve("moduleA"), "moduleA-art", Collections.emptyList());
        writePom(repo.resolve("moduleB"), "moduleB-art", Collections.emptyList());
        Path emitA = Files.createDirectories(repo.resolve("moduleA/src/test/resources/emit-models"));
        Path emitB = Files.createDirectories(repo.resolve("moduleB/src/test/resources/emit-models"));

        List<Path> dirs = EMITCoverageReportGenerationMojo.collectEmitModelsDirs(repo);

        Assertions.assertEquals(2, dirs.size(),
                "Each declared module's emit-models/ should be discovered. Got " + dirs);
        Assertions.assertTrue(dirs.contains(emitA));
        Assertions.assertTrue(dirs.contains(emitB));
    }

    @Test
    public void collectEmitModelsDirsRecursesIntoNestedModuleHierarchies(@TempDir Path repo) throws Exception
    {
        writePom(repo, "legend-engine", Collections.singletonList("level1"));
        writePom(repo.resolve("level1"), "level1-art", Collections.singletonList("level2"));
        writePom(repo.resolve("level1/level2"), "level2-art", Collections.emptyList());
        Path emit = Files.createDirectories(repo.resolve("level1/level2/src/test/resources/emit-models"));

        List<Path> dirs = EMITCoverageReportGenerationMojo.collectEmitModelsDirs(repo);

        Assertions.assertEquals(Collections.singletonList(emit), dirs,
                "Discovery must descend through multi-level <modules> trees");
    }

    @Test
    public void collectEmitModelsDirsSkipsExcludedModuleArtifactIds(@TempDir Path repo) throws Exception
    {
        writePom(repo, "legend-engine", Arrays.asList("real", "legend-engine-emit-junit"));
        writePom(repo.resolve("real"), "real-art", Collections.emptyList());
        writePom(repo.resolve("legend-engine-emit-junit"), "legend-engine-emit-junit", Collections.emptyList());
        Path realEmit = Files.createDirectories(repo.resolve("real/src/test/resources/emit-models"));
        Files.createDirectories(repo.resolve("legend-engine-emit-junit/src/test/resources/emit-models"));

        List<Path> dirs = EMITCoverageReportGenerationMojo.collectEmitModelsDirs(repo);

        Assertions.assertEquals(Collections.singletonList(realEmit), dirs,
                "legend-engine-emit-junit must be skipped even though its emit-models/ exists.");
    }

    @Test
    public void collectEmitModelsDirsSilentlySkipsModulesWithCustomTestResources(@TempDir Path repo) throws Exception
    {
        writePom(repo, "legend-engine", Arrays.asList("real", "custom"));
        writePom(repo.resolve("real"), "real-art", Collections.emptyList());
        writePomWithTestResources(repo.resolve("custom"), "custom-art",
                Collections.singletonList("custom/path"));
        Path realEmit = Files.createDirectories(repo.resolve("real/src/test/resources/emit-models"));
        Files.createDirectories(repo.resolve("custom/src/test/resources/emit-models"));
        Files.createDirectories(repo.resolve("custom/custom/path/emit-models"));

        List<Path> dirs = EMITCoverageReportGenerationMojo.collectEmitModelsDirs(repo);

        Assertions.assertEquals(Collections.singletonList(realEmit), dirs,
                "Only the conventional module should be reported; the module with custom <testResources> is silently skipped");
    }

    @Test
    public void collectEmitModelsDirsReturnsEmptyListWhenNoModuleHasEmitModels(@TempDir Path repo) throws Exception
    {
        writePom(repo, "legend-engine", Arrays.asList("a", "b"));
        writePom(repo.resolve("a"), "a-art", Collections.emptyList());
        writePom(repo.resolve("b"), "b-art", Collections.emptyList());

        List<Path> dirs = EMITCoverageReportGenerationMojo.collectEmitModelsDirs(repo);

        Assertions.assertTrue(dirs.isEmpty(),
                "No emit-models/ anywhere → empty result, not an error. Got " + dirs);
    }

    @Test
    public void collectEmitModelsDirsSilentlySkipsDeclaredModulesWithoutAPom(@TempDir Path repo) throws Exception
    {
        writePom(repo, "legend-engine", Arrays.asList("real", "phantom"));
        writePom(repo.resolve("real"), "real-art", Collections.emptyList());
        Files.createDirectories(repo.resolve("phantom"));
        Path realEmit = Files.createDirectories(repo.resolve("real/src/test/resources/emit-models"));

        List<Path> dirs = EMITCoverageReportGenerationMojo.collectEmitModelsDirs(repo);

        Assertions.assertEquals(Collections.singletonList(realEmit), dirs,
                "Modules without an on-disk pom.xml must be skipped silently");
    }

    @Test
    public void collectEmitModelsDirsReturnsEmptyWhenRepoRootHasNoPom(@TempDir Path repo) throws Exception
    {
        List<Path> dirs = EMITCoverageReportGenerationMojo.collectEmitModelsDirs(repo);

        Assertions.assertTrue(dirs.isEmpty(), "Repo root with no pom.xml must produce an empty result");
    }

    private static MavenProject projectOf(String groupId, String artifactId)
    {
        MavenProject p = Mockito.mock(MavenProject.class);
        Mockito.when(p.getGroupId()).thenReturn(groupId);
        Mockito.when(p.getArtifactId()).thenReturn(artifactId);
        return p;
    }

    private static void writePom(Path moduleDir, String artifactId, List<String> submodules) throws IOException
    {
        Files.createDirectories(moduleDir);
        StringBuilder sb = new StringBuilder();
        sb.append("<project xmlns=\"http://maven.apache.org/POM/4.0.0\">\n");
        sb.append("  <modelVersion>4.0.0</modelVersion>\n");
        sb.append("  <groupId>test</groupId>\n");
        sb.append("  <artifactId>").append(artifactId).append("</artifactId>\n");
        sb.append("  <version>1.0</version>\n");
        if (!submodules.isEmpty())
        {
            sb.append("  <packaging>pom</packaging>\n");
            sb.append("  <modules>\n");
            for (String m : submodules)
            {
                sb.append("    <module>").append(m).append("</module>\n");
            }
            sb.append("  </modules>\n");
        }
        sb.append("</project>\n");
        Files.write(moduleDir.resolve("pom.xml"), sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static void writePomWithTestResources(Path moduleDir, String artifactId, List<String> testResourceDirs) throws IOException
    {
        Files.createDirectories(moduleDir);
        StringBuilder sb = new StringBuilder();
        sb.append("<project xmlns=\"http://maven.apache.org/POM/4.0.0\">\n");
        sb.append("  <modelVersion>4.0.0</modelVersion>\n");
        sb.append("  <groupId>test</groupId>\n");
        sb.append("  <artifactId>").append(artifactId).append("</artifactId>\n");
        sb.append("  <version>1.0</version>\n");
        sb.append("  <build>\n");
        sb.append("    <testResources>\n");
        for (String dir : testResourceDirs)
        {
            sb.append("      <testResource><directory>").append(dir).append("</directory></testResource>\n");
        }
        sb.append("    </testResources>\n");
        sb.append("  </build>\n");
        sb.append("</project>\n");
        Files.write(moduleDir.resolve("pom.xml"), sb.toString().getBytes(StandardCharsets.UTF_8));
    }
}
