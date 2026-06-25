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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TestEmitCatalogBuilder
{
    @Test
    public void findYamlsDiscoversEveryEmitModelsYaml(@TempDir Path repo) throws IOException
    {
        writeYaml(repo, "moduleA/src/test/resources/emit-models/a.emit.yaml");
        writeYaml(repo, "moduleA/src/test/resources/emit-models/nested/sub.emit.yaml");
        writeYaml(repo, "moduleB/src/test/resources/emit-models/b.emit.yaml");

        List<String> found = relativise(repo, EmitCatalogBuilder.findYamls(repo, "src/test/resources/emit-models"));

        Assertions.assertEquals(
                Arrays.asList(
                        "moduleA/src/test/resources/emit-models/a.emit.yaml",
                        "moduleA/src/test/resources/emit-models/nested/sub.emit.yaml",
                        "moduleB/src/test/resources/emit-models/b.emit.yaml"),
                found,
                "Walker must return every emit-models yaml under any module, sorted by repo-relative path.");
    }

    @Test
    public void findYamlsIgnoresYamlsOutsideEmitModels(@TempDir Path repo) throws IOException
    {
        writeYaml(repo, "moduleA/src/test/resources/other/config.emit.yaml");
        writeYaml(repo, "moduleB/src/main/resources/something.emit.yaml");
        writeYaml(repo, "moduleC/src/test/resources/emit-models/real.emit.yaml");

        List<String> found = relativise(repo, EmitCatalogBuilder.findYamls(repo, "src/test/resources/emit-models"));

        Assertions.assertEquals(
                java.util.Collections.singletonList("moduleC/src/test/resources/emit-models/real.emit.yaml"),
                found,
                "Only yamls whose path contains the emit-models segment count.");
    }

    @Test
    public void findYamlsSkipsTargetAndOtherBuildOutputDirs(@TempDir Path repo) throws IOException
    {
        writeYaml(repo, "moduleA/src/test/resources/emit-models/source.emit.yaml");
        writeYaml(repo, "consumer/target/classes/META-INF/emit-catalog/moduleA/src/test/resources/emit-models/source.emit.yaml");
        writeYaml(repo, "frontend/node_modules/pkg/src/test/resources/emit-models/junk.emit.yaml");
        writeYaml(repo, ".git/tmp/src/test/resources/emit-models/junk.emit.yaml");
        writeYaml(repo, ".idea/cache/src/test/resources/emit-models/junk.emit.yaml");
        writeYaml(repo, "moduleX/build/tmp/src/test/resources/emit-models/junk.emit.yaml");

        List<String> found = relativise(repo, EmitCatalogBuilder.findYamls(repo, "src/test/resources/emit-models"));

        Assertions.assertEquals(
                java.util.Collections.singletonList("moduleA/src/test/resources/emit-models/source.emit.yaml"),
                found,
                "target/, build/, node_modules/, .git/, .idea/ paths must be excluded so prior-build copies aren't re-found.");
    }

    @Test
    public void findYamlsSkipsLegendEngineEmitJunitModule(@TempDir Path repo) throws IOException
    {
        writeYaml(repo, "legend-engine-config/legend-engine-emit-tests/src/test/resources/emit-models/basic/class-simple.emit.yaml");
        writeYaml(repo, "legend-engine-core/legend-engine-core-emit/legend-engine-emit-junit/src/test/resources/emit-models/basic/class-simple.emit.yaml");
        writeYaml(repo, "legend-engine-core/legend-engine-core-emit/legend-engine-emit-junit/src/test/resources/emit-models/basic/file-generation.emit.yaml");

        List<String> found = relativise(repo, EmitCatalogBuilder.findYamls(repo, "src/test/resources/emit-models"));

        Assertions.assertEquals(
                java.util.Collections.singletonList(
                        "legend-engine-config/legend-engine-emit-tests/src/test/resources/emit-models/basic/class-simple.emit.yaml"),
                found,
                "Yamls under legend-engine-emit-junit must be skipped — they are JUnit-harness fixtures, not catalog entries.");
    }

    @Test
    public void writeCatalogCopiesEveryYamlAndWritesSortedIndex(@TempDir Path tmp) throws IOException
    {
        Path repo = Files.createDirectories(tmp.resolve("repo"));
        Path out = tmp.resolve("out");

        writeYaml(repo, "moduleA/src/test/resources/emit-models/a.emit.yaml", "name: a\n");
        writeYaml(repo, "moduleB/src/test/resources/emit-models/b.emit.yaml", "name: b\n");

        List<Path> yamls = EmitCatalogBuilder.findYamls(repo, "src/test/resources/emit-models");
        List<String> entries = EmitCatalogBuilder.writeCatalog(yamls, repo, out, "index.txt");

        Assertions.assertEquals(
                Arrays.asList(
                        "moduleA/src/test/resources/emit-models/a.emit.yaml",
                        "moduleB/src/test/resources/emit-models/b.emit.yaml"),
                entries);

        String indexFileContent = new String(Files.readAllBytes(out.resolve("index.txt")), StandardCharsets.UTF_8);
        Assertions.assertEquals(
                "moduleA/src/test/resources/emit-models/a.emit.yaml\n"
                        + "moduleB/src/test/resources/emit-models/b.emit.yaml\n",
                indexFileContent);

        Assertions.assertTrue(Files.exists(out.resolve("moduleA/src/test/resources/emit-models/a.emit.yaml")));
        Assertions.assertTrue(Files.exists(out.resolve("moduleB/src/test/resources/emit-models/b.emit.yaml")));
        Assertions.assertEquals("name: a\n",
                new String(Files.readAllBytes(out.resolve("moduleA/src/test/resources/emit-models/a.emit.yaml")), StandardCharsets.UTF_8));
    }

    @Test
    public void writeCatalogIsRerunnableOverExistingOutput(@TempDir Path tmp) throws IOException
    {
        Path repo = Files.createDirectories(tmp.resolve("repo"));
        Path out = tmp.resolve("out");

        writeYaml(repo, "moduleA/src/test/resources/emit-models/a.emit.yaml", "v1\n");
        EmitCatalogBuilder.writeCatalog(
                EmitCatalogBuilder.findYamls(repo, "src/test/resources/emit-models"),
                repo, out, "index.txt");

        writeYaml(repo, "moduleA/src/test/resources/emit-models/a.emit.yaml", "v2\n");
        EmitCatalogBuilder.writeCatalog(
                EmitCatalogBuilder.findYamls(repo, "src/test/resources/emit-models"),
                repo, out, "index.txt");

        Assertions.assertEquals("v2\n",
                new String(Files.readAllBytes(out.resolve("moduleA/src/test/resources/emit-models/a.emit.yaml")), StandardCharsets.UTF_8),
                "Second run must overwrite the prior copy.");
    }

    @Test
    public void findYamlsThrowsForMissingRepoRoot(@TempDir Path tmp)
    {
        Path missing = tmp.resolve("does-not-exist");
        Assertions.assertThrows(IOException.class,
                () -> EmitCatalogBuilder.findYamls(missing, "src/test/resources/emit-models"));
    }

    private static void writeYaml(Path repo, String relativePath) throws IOException
    {
        writeYaml(repo, relativePath, "name: placeholder\n");
    }

    private static void writeYaml(Path repo, String relativePath, String content) throws IOException
    {
        Path target = repo.resolve(relativePath);
        Files.createDirectories(target.getParent());
        Files.write(target, content.getBytes(StandardCharsets.UTF_8));
    }

    private static List<String> relativise(Path repo, List<Path> paths)
    {
        return paths.stream()
                .map(p -> repo.relativize(p).toString().replace(java.io.File.separatorChar, '/'))
                .collect(Collectors.toList());
    }
}

