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

import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TestEMITCoverageReportGenerationMojo
{
    private static final Set<String> DEFAULT_EXCLUDED_NAMES = setOf("target", "legend-engine-emit-junit");
    private static final Set<String> DEFAULT_EXCLUDED_PREFIXES = setOf(".");
    private static final Set<String> DEFAULT_EXCLUDED_SUBPATHS = setOf("src/main");
    private static final Set<String> DEFAULT_INCLUDED_SUBPATHS = setOf("src/test/resources/emit-models");

    @Test
    public void findRepoRootReturnsProjectWhenItHasNoParent()
    {
        MavenProject project = projectOnDisk();

        MavenProject found = EMITCoverageReportGenerationMojo.findRepoRoot(project);

        Assertions.assertSame(project, found,
                "A project with no parent is itself the repo root");
    }

    @Test
    public void findRepoRootWalksParentChainToTopmostOnDiskProject()
    {
        MavenProject root = projectOnDisk();
        MavenProject parent = projectOnDisk();
        MavenProject child = projectOnDisk();
        Mockito.when(parent.getParent()).thenReturn(root);
        Mockito.when(child.getParent()).thenReturn(parent);

        MavenProject found = EMITCoverageReportGenerationMojo.findRepoRoot(child);

        Assertions.assertSame(root, found,
                "findRepoRoot must walk all the way to the topmost on-disk ancestor");
    }

    @Test
    public void findRepoRootStopsAtTheFirstParentWithoutBasedir()
    {
        MavenProject externalParent = projectWithoutBasedir();
        MavenProject root = projectOnDisk();
        Mockito.when(root.getParent()).thenReturn(externalParent);

        MavenProject found = EMITCoverageReportGenerationMojo.findRepoRoot(root);

        Assertions.assertSame(root, found,
                "Walking must stop at the first parent that has no basedir");
    }

    @Test
    public void findRepoRootDoesNotCrossExternalParentInDeeperChain()
    {
        MavenProject externalParent = projectWithoutBasedir();
        MavenProject onDiskRoot = projectOnDisk();
        MavenProject onDiskChild = projectOnDisk();
        Mockito.when(onDiskRoot.getParent()).thenReturn(externalParent);
        Mockito.when(onDiskChild.getParent()).thenReturn(onDiskRoot);

        MavenProject found = EMITCoverageReportGenerationMojo.findRepoRoot(onDiskChild);

        Assertions.assertSame(onDiskRoot, found,
                "Even with several on-disk descendants, the walk must still stop at the on-disk root");
    }

    @Test
    public void findRepoRootThrowsOnNull()
    {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> EMITCoverageReportGenerationMojo.findRepoRoot(null));
    }

    @Test
    public void walksTopLevelModulesAndReturnsTheirEmitModelsDirs(@TempDir Path repo) throws IOException
    {
        Path moduleA = touchYaml(repo, "legend-engine-xt-a/src/test/resources/emit-models/a.emit.yaml");
        Path moduleB = touchYaml(repo, "legend-engine-xt-b/src/test/resources/emit-models/b.emit.yaml");

        List<Path> dirs = collectWithDefaults(repo);

        Assertions.assertEquals(2, dirs.size(), "Both modules' emit-models dirs should be returned. Got " + dirs);
        Assertions.assertTrue(dirs.contains(moduleA.getParent()));
        Assertions.assertTrue(dirs.contains(moduleB.getParent()));
    }

    @Test
    public void deduplicatesMultipleYamlsInTheSameEmitModelsDir(@TempDir Path repo) throws IOException
    {
        Path emitModels = touchYaml(repo, "m/src/test/resources/emit-models/a.emit.yaml").getParent();
        Files.write(emitModels.resolve("b.emit.yaml"), new byte[0]);
        Files.createDirectories(emitModels.resolve("nested"));
        Files.write(emitModels.resolve("nested/c.emit.yaml"), new byte[0]);

        List<Path> dirs = collectWithDefaults(repo);

        Assertions.assertEquals(Collections.singletonList(emitModels), dirs,
                "The emit-models directory is recorded once (SKIP_SUBTREE prevents descent), regardless of how many yamls it contains or how deeply they are nested");
    }

    @Test
    public void prunesTargetSubtrees(@TempDir Path repo) throws IOException
    {
        Path good = touchYaml(repo, "m/src/test/resources/emit-models/good.emit.yaml").getParent();
        touchYaml(repo, "m/target/leftover/src/test/resources/emit-models/leaked.emit.yaml");

        List<Path> dirs = collectWithDefaults(repo);

        Assertions.assertEquals(Collections.singletonList(good), dirs,
                "target/ subtrees are pruned by preVisitDirectory SKIP_SUBTREE");
    }

    @Test
    public void prunesHiddenDirectoriesByNamePrefix(@TempDir Path repo) throws IOException
    {
        Path good = touchYaml(repo, "m/src/test/resources/emit-models/good.emit.yaml").getParent();
        touchYaml(repo, ".git/objects/src/test/resources/emit-models/leaked.emit.yaml");
        touchYaml(repo, ".idea/cache/src/test/resources/emit-models/leaked2.emit.yaml");

        List<Path> dirs = collectWithDefaults(repo);

        Assertions.assertEquals(Collections.singletonList(good), dirs,
                "Any directory whose simple name starts with '.' is pruned");
    }

    @Test
    public void prunesSrcMainSubtrees(@TempDir Path repo) throws IOException
    {
        Path good = touchYaml(repo, "m/src/test/resources/emit-models/good.emit.yaml").getParent();
        touchYaml(repo, "m/src/main/resources/emit-models/leaked.emit.yaml");

        List<Path> dirs = collectWithDefaults(repo);

        Assertions.assertEquals(Collections.singletonList(good), dirs,
                "Yamls under src/main must not leak into the report");
    }

    @Test
    public void prunesExcludedModuleArtifactIdDirectories(@TempDir Path repo) throws IOException
    {
        Path good = touchYaml(repo, "real-module/src/test/resources/emit-models/good.emit.yaml").getParent();
        touchYaml(repo, "legend-engine-emit-junit/src/test/resources/emit-models/leaked.emit.yaml");

        List<Path> dirs = collectWithDefaults(repo);

        Assertions.assertEquals(Collections.singletonList(good), dirs,
                "Modules whose dir name matches an excluded artifactId must be skipped");
    }

    @Test
    public void requiresYamlsToLiveUnderTheIncludedSubpath(@TempDir Path repo) throws IOException
    {
        Path good = touchYaml(repo, "m/src/test/resources/emit-models/good.emit.yaml").getParent();
        touchYaml(repo, "m/src/test/resources/other/nope.emit.yaml");
        touchYaml(repo, "m/random-place/nope.emit.yaml");

        List<Path> dirs = collectWithDefaults(repo);

        Assertions.assertEquals(Collections.singletonList(good), dirs,
                "preVisitDirectory only records dirs whose relative path matches the inclusion subpath; non-matching dirs are not recorded, even if they contain *.emit.yaml files");
    }

    @Test
    public void parameterizedInclusionSubpathsChangeWhereYamlsAreLookedFor(@TempDir Path repo) throws IOException
    {
        Path customDir = touchYaml(repo, "m/some/custom/place/foo.emit.yaml").getParent();

        Assertions.assertTrue(collectWithDefaults(repo).isEmpty(),
                "Default inclusion is src/test/resources/emit-models — custom location must be ignored");

        List<Path> dirs = EMITCoverageReportGenerationMojo.collectEmitModelsDirs(
                repo, setOf("some/custom/place"), DEFAULT_EXCLUDED_NAMES,
                DEFAULT_EXCLUDED_PREFIXES, DEFAULT_EXCLUDED_SUBPATHS);

        Assertions.assertEquals(Collections.singletonList(customDir), dirs,
                "A custom inclusion subpath should pick up yamls under it");
    }

    @Test
    public void matchesDirsAgainstAnyOfTheConfiguredInclusionSubpaths(@TempDir Path repo) throws IOException
    {
        Path unit = touchYaml(repo, "m/src/test/resources/emit-models/a.emit.yaml").getParent();
        Path integration = touchYaml(repo, "m/src/it/resources/emit-models/b.emit.yaml").getParent();

        List<Path> dirs = EMITCoverageReportGenerationMojo.collectEmitModelsDirs(
                repo,
                setOf("src/test/resources/emit-models", "src/it/resources/emit-models"),
                DEFAULT_EXCLUDED_NAMES, DEFAULT_EXCLUDED_PREFIXES, DEFAULT_EXCLUDED_SUBPATHS);

        Assertions.assertEquals(2, dirs.size(),
                "Directories matching any of the configured inclusion subpaths must all be recorded. Got " + dirs);
        Assertions.assertTrue(dirs.contains(unit));
        Assertions.assertTrue(dirs.contains(integration));
    }

    @Test
    public void parameterizedExclusionsCanBeRelaxedToIncludeOtherwiseHiddenSubtrees(@TempDir Path repo) throws IOException
    {
        Path under_target = touchYaml(repo, "target/foo/src/test/resources/emit-models/a.emit.yaml").getParent();

        List<Path> dirs = EMITCoverageReportGenerationMojo.collectEmitModelsDirs(
                repo, DEFAULT_INCLUDED_SUBPATHS, Collections.emptySet(),
                DEFAULT_EXCLUDED_PREFIXES, DEFAULT_EXCLUDED_SUBPATHS);

        Assertions.assertEquals(Collections.singletonList(under_target), dirs,
                "Clearing excludedDirectoryNames should allow target/ subtrees to surface");
    }

    @Test
    public void returnsSortedListOfEmitModelsDirs(@TempDir Path repo) throws IOException
    {
        Path z = touchYaml(repo, "zzz-module/src/test/resources/emit-models/z.emit.yaml").getParent();
        Path a = touchYaml(repo, "aaa-module/src/test/resources/emit-models/a.emit.yaml").getParent();
        Path m = touchYaml(repo, "mmm-module/src/test/resources/emit-models/m.emit.yaml").getParent();

        List<Path> dirs = collectWithDefaults(repo);

        Assertions.assertEquals(Arrays.asList(a, m, z), dirs,
                "Result must be in natural Path order");
    }

    @Test
    public void rejectsNullRepoRoot()
    {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> EMITCoverageReportGenerationMojo.collectEmitModelsDirs(
                        null, DEFAULT_INCLUDED_SUBPATHS, DEFAULT_EXCLUDED_NAMES,
                        DEFAULT_EXCLUDED_PREFIXES, DEFAULT_EXCLUDED_SUBPATHS));
    }

    @Test
    public void rejectsNullOrEmptyInclusionSubpaths(@TempDir Path repo)
    {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> EMITCoverageReportGenerationMojo.collectEmitModelsDirs(
                        repo, null, DEFAULT_EXCLUDED_NAMES,
                        DEFAULT_EXCLUDED_PREFIXES, DEFAULT_EXCLUDED_SUBPATHS),
                "null inclusion set must be rejected");
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> EMITCoverageReportGenerationMojo.collectEmitModelsDirs(
                        repo, Collections.emptySet(), DEFAULT_EXCLUDED_NAMES,
                        DEFAULT_EXCLUDED_PREFIXES, DEFAULT_EXCLUDED_SUBPATHS),
                "empty inclusion set must be rejected (nothing could ever match)");
    }

    @Test
    public void tolerantOfNullExclusionSets(@TempDir Path repo) throws IOException
    {
        Path good = touchYaml(repo, "m/src/test/resources/emit-models/g.emit.yaml").getParent();

        List<Path> dirs = EMITCoverageReportGenerationMojo.collectEmitModelsDirs(
                repo, DEFAULT_INCLUDED_SUBPATHS, null, null, null);

        Assertions.assertEquals(Collections.singletonList(good), dirs,
                "Null exclusion sets are treated as empty (no pruning beyond the inclusion filter)");
    }

    private static MavenProject projectOnDisk()
    {
        MavenProject p = Mockito.mock(MavenProject.class);
        Mockito.when(p.getBasedir()).thenReturn(new File("/tmp"));
        return p;
    }

    private static MavenProject projectWithoutBasedir()
    {
        return Mockito.mock(MavenProject.class);
    }

    private static List<Path> collectWithDefaults(Path repo) throws IOException
    {
        return EMITCoverageReportGenerationMojo.collectEmitModelsDirs(
                repo, DEFAULT_INCLUDED_SUBPATHS, DEFAULT_EXCLUDED_NAMES,
                DEFAULT_EXCLUDED_PREFIXES, DEFAULT_EXCLUDED_SUBPATHS);
    }

    private static Path touchYaml(Path repo, String relativePath) throws IOException
    {
        Path target = repo.resolve(relativePath);
        Files.createDirectories(target.getParent());
        Files.write(target, new byte[0]);
        return target;
    }

    private static Set<String> setOf(String... values)
    {
        return new HashSet<>(Arrays.asList(values));
    }
}
