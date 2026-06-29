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

import org.finos.legend.engine.test.emit.EMIT_to_HTML;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

public class TestEMITCoverageReportGeneration
{
    @Test
    public void onlyEmitModelsYamlsAreIncluded(@TempDir Path repo) throws IOException
    {
        writeYaml(repo, "moduleA/src/test/resources/emit-models/included-a.emit.yaml", "included-a");
        writeYaml(repo, "moduleB/src/test/resources/emit-models/nested/included-b.emit.yaml", "included-b");
        writeYaml(repo, "moduleC/src/test/resources/other-fixtures/not-included.emit.yaml", "not-included-other");
        writeYaml(repo, "moduleD/src/main/resources/emit-models/not-included.emit.yaml", "not-included-main");

        String html = EMIT_to_HTML.generateFromRepoRoot(repo);

        Assertions.assertTrue(html.contains(">included-a<"), "included-a must appear in the report");
        Assertions.assertTrue(html.contains(">included-b<"), "included-b must appear in the report");
        Assertions.assertFalse(html.contains("not-included-other"), "yaml outside emit-models/ must be excluded");
        Assertions.assertFalse(html.contains("not-included-main"), "yaml outside src/test/resources/ must be excluded");
    }

    @Test
    public void buildOutputCopiesAreSkipped(@TempDir Path repo) throws IOException
    {
        writeYaml(repo, "moduleA/src/test/resources/emit-models/source.emit.yaml", "source-only");
        writeYaml(repo, "consumer/target/classes/legend-engine/moduleA/src/test/resources/emit-models/source.emit.yaml", "target-copy");
        writeYaml(repo, "frontend/node_modules/pkg/src/test/resources/emit-models/source.emit.yaml", "node-modules-copy");
        writeYaml(repo, ".git/tmp/src/test/resources/emit-models/source.emit.yaml", "git-copy");
        writeYaml(repo, ".idea/cache/src/test/resources/emit-models/source.emit.yaml", "idea-copy");
        writeYaml(repo, "moduleX/build/tmp/src/test/resources/emit-models/source.emit.yaml", "build-copy");

        String html = EMIT_to_HTML.generateFromRepoRoot(repo);

        Assertions.assertTrue(html.contains(">source-only<"), "source-only must appear");
        Assertions.assertFalse(html.contains("target-copy"), "target/ paths must be skipped");
        Assertions.assertFalse(html.contains("node-modules-copy"), "node_modules/ paths must be skipped");
        Assertions.assertFalse(html.contains("git-copy"), ".git/ paths must be skipped");
        Assertions.assertFalse(html.contains("idea-copy"), ".idea/ paths must be skipped");
        Assertions.assertFalse(html.contains("build-copy"), "build/ paths must be skipped");
    }

    @Test
    public void legendEngineEmitJunitFixturesAreSkipped(@TempDir Path repo) throws IOException
    {
        writeYaml(repo, "legend-engine-config/legend-engine-emit-tests/src/test/resources/emit-models/basic/class-simple.emit.yaml", "real-class-simple");
        writeYaml(repo, "legend-engine-core/legend-engine-core-emit/legend-engine-emit-junit/src/test/resources/emit-models/basic/class-simple.emit.yaml", "junit-fixture-class-simple");
        writeYaml(repo, "legend-engine-core/legend-engine-core-emit/legend-engine-emit-junit/src/test/resources/emit-models/basic/m2m-passing.emit.yaml", "junit-fixture-m2m");

        String html = EMIT_to_HTML.generateFromRepoRoot(repo);

        Assertions.assertTrue(html.contains(">real-class-simple<"), "yaml from a real module must appear");
        Assertions.assertFalse(html.contains("junit-fixture-class-simple"), "legend-engine-emit-junit fixtures must be skipped");
        Assertions.assertFalse(html.contains("junit-fixture-m2m"), "legend-engine-emit-junit fixtures must be skipped");
    }

    @Test
    public void emptyRepoProducesValidReportWithZeroModels(@TempDir Path repo) throws IOException
    {
        String html = EMIT_to_HTML.generateFromRepoRoot(repo);

        Assertions.assertTrue(html.contains("EMIT Coverage Report"), "header must always render");
        Assertions.assertTrue(html.contains(">0</span><span class=\"stat-label\">Models<"),
                "zero-models stat-bar must render");
    }

    @Test
    public void renderingToFileCreatesParentDirectories(@TempDir Path tmp) throws IOException
    {
        Path repo = Files.createDirectories(tmp.resolve("repo"));
        writeYaml(repo, "moduleA/src/test/resources/emit-models/x.emit.yaml", "x");
        Path output = tmp.resolve("out/sub/dir/emit-coverage.html");

        EMIT_to_HTML.generateFromRepoRoot(repo, output);

        Assertions.assertTrue(Files.exists(output), "output file must be created");
        String written = new String(Files.readAllBytes(output), StandardCharsets.UTF_8);
        Assertions.assertTrue(written.contains(">x<"), "rendered file must contain the model");
    }

    @Test
    public void nonExistentRepoRootIsRejected(@TempDir Path tmp)
    {
        Path missing = tmp.resolve("does-not-exist");
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> EMIT_to_HTML.generateFromRepoRoot(missing));
    }

    @Test
    public void generateFromEmitModelsDirsCollectsFromEveryListedDir(@TempDir Path repo) throws IOException
    {
        Path moduleA = repo.resolve("moduleA/src/test/resources/emit-models");
        Path moduleB = repo.resolve("moduleB/src/test/resources/emit-models");
        Files.createDirectories(moduleA);
        Files.createDirectories(moduleB);
        writeYaml(repo, "moduleA/src/test/resources/emit-models/a.emit.yaml", "from-a");
        writeYaml(repo, "moduleA/src/test/resources/emit-models/nested/a-nested.emit.yaml", "from-a-nested");
        writeYaml(repo, "moduleB/src/test/resources/emit-models/b.emit.yaml", "from-b");

        String html = EMIT_to_HTML.generateFromEmitModelsDirs(repo, Arrays.asList(moduleA, moduleB));

        Assertions.assertTrue(html.contains(">from-a<"), "from-a should be included");
        Assertions.assertTrue(html.contains(">from-a-nested<"), "from-a-nested should be included (recursive walk)");
        Assertions.assertTrue(html.contains(">from-b<"), "from-b should be included");
    }

    @Test
    public void generateFromEmitModelsDirsDoesNotApplyExclusionPatterns(@TempDir Path repo) throws IOException
    {
        Path moduleADir = repo.resolve("moduleA/src/test/resources/emit-models");
        Files.createDirectories(moduleADir);
        writeYaml(repo, "moduleA/src/test/resources/emit-models/regular.emit.yaml", "regular");
        writeYaml(repo, "moduleA/src/test/resources/emit-models/target/leaked.emit.yaml", "leaked-under-target");

        String html = EMIT_to_HTML.generateFromEmitModelsDirs(repo, Collections.singletonList(moduleADir));

        Assertions.assertTrue(html.contains(">regular<"), "regular yaml must be included");
        Assertions.assertTrue(html.contains(">leaked-under-target<"),
                "yaml under a target/ subdirectory of the supplied dir IS included; "
                        + "filtering is the caller's responsibility, not generateFromEmitModelsDirs's.");
    }

    @Test
    public void generateFromEmitModelsDirsSilentlySkipsMissingDirs(@TempDir Path repo) throws IOException
    {
        Path real = repo.resolve("real/src/test/resources/emit-models");
        Files.createDirectories(real);
        writeYaml(repo, "real/src/test/resources/emit-models/r.emit.yaml", "real-r");
        Path missing = repo.resolve("missing/src/test/resources/emit-models");

        String html = EMIT_to_HTML.generateFromEmitModelsDirs(repo, Arrays.asList(real, missing));

        Assertions.assertTrue(html.contains(">real-r<"), "real yaml must be included");
        Assertions.assertTrue(html.contains("EMIT Coverage Report"), "report must render without throwing");
    }

    @Test
    public void generateFromEmitModelsDirsEmitsResourcePathAndModuleForRenderColumns(@TempDir Path repo) throws IOException
    {
        Path moduleADir = repo.resolve("moduleA/src/test/resources/emit-models");
        Files.createDirectories(moduleADir);
        writeYaml(repo, "moduleA/src/test/resources/emit-models/nested/sub.emit.yaml", "sub");

        String html = EMIT_to_HTML.generateFromEmitModelsDirs(repo, Collections.singletonList(moduleADir));

        Assertions.assertTrue(html.contains("moduleA/src/test/resources/emit-models/nested/sub.emit.yaml"),
                "Resource path (repo-relative) must appear in the report. HTML excerpt: "
                        + html.substring(Math.max(0, html.indexOf(">sub<") - 200),
                                Math.min(html.length(), html.indexOf(">sub<") + 500)));
        Assertions.assertTrue(html.contains(">moduleA<"),
                "Derived module name must appear in the Module column.");
    }

    @Test
    public void generateFromEmitModelsDirsWritesToFile(@TempDir Path tmp) throws IOException
    {
        Path repo = Files.createDirectories(tmp.resolve("repo"));
        Path moduleADir = repo.resolve("moduleA/src/test/resources/emit-models");
        Files.createDirectories(moduleADir);
        writeYaml(repo, "moduleA/src/test/resources/emit-models/x.emit.yaml", "x");
        Path output = tmp.resolve("out/sub/dir/emit-coverage.html");

        EMIT_to_HTML.generateFromEmitModelsDirs(repo, Collections.singletonList(moduleADir), output);

        Assertions.assertTrue(Files.exists(output), "output file must be created");
        String written = new String(Files.readAllBytes(output), StandardCharsets.UTF_8);
        Assertions.assertTrue(written.contains(">x<"), "rendered file must contain the model");
    }

    @Test
    public void generateFromEmitModelsDirsRejectsNullArgs(@TempDir Path repo)
    {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> EMIT_to_HTML.generateFromEmitModelsDirs(null, Collections.emptyList()),
                "null repoRoot must be rejected");
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> EMIT_to_HTML.generateFromEmitModelsDirs(repo, null),
                "null emitModelsDirs must be rejected");
    }

    private static void writeYaml(Path repo, String relativePath, String uniqueMarker) throws IOException
    {
        Path target = repo.resolve(relativePath);
        Files.createDirectories(target.getParent());
        String yaml = "name: " + uniqueMarker + "\n"
                + "title: " + uniqueMarker + "\n"
                + "description: " + uniqueMarker + "\n"
                + "features: []\n"
                + "stores: []\n"
                + "complexity: basic\n"
                + "tags: []\n"
                + "modelSources:\n"
                + "  model:\n"
                + "    root: .\n"
                + "    files: []\n";
        Files.write(target, yaml.getBytes(StandardCharsets.UTF_8));
    }
}
