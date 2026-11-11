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

package org.finos.legend.engine.ide;

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.lsp.RepositoryScanner;
import org.finos.legend.pure.m3.pct.shared.PCTManifestLoader;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.RepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.fs.MutableFSCodeStorage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class TestPCTManifestRepositories
{
    @Test
    public void everyManifestIsBrowsableAndLoadableFromIDEAndLSP() throws Exception
    {
        Path root = findRepositoryRoot();
        MutableList<RepositoryCodeStorage> ideStorages = new PureIDELight()
        {
            @Override
            protected MutableFSCodeStorage build(String path, String module, boolean core)
            {
                return super.build(root.resolve(path).toString(), module, core);
            }
        }.buildRepositories(null);
        RepositoryScanner scanner = new RepositoryScanner();
        scanner.scanRoot(root);
        MutableList<RepositoryCodeStorage> lspStorages = scanner.buildWorkspaceStorages();

        List<Path> manifests = findManifests(root);
        Assertions.assertFalse(manifests.isEmpty());
        for (Path manifest : manifests)
        {
            String path = manifest.toString().replace('\\', '/');
            path = path.substring(path.indexOf("/src/main/resources/") + "/src/main/resources".length());
            String repository = path.substring(1, path.indexOf('/', 1));
            Assertions.assertEquals(manifest, scanner.resolve(path), path);
            assertManifest(ideStorages, repository, path, manifest);
            assertManifest(lspStorages, repository, path, manifest);
        }
    }

    private static void assertManifest(MutableList<RepositoryCodeStorage> storages, String repository, String path, Path manifest) throws Exception
    {
        RepositoryCodeStorage storage = storages.detect(s -> s.getAllRepositories().anySatisfy(r -> repository.equals(r.getName())));
        Assertions.assertNotNull(storage, "Missing repository for " + path);
        Assertions.assertTrue(storage.isFile(path), path);
        Assertions.assertTrue(storage.getFiles(path.substring(0, path.lastIndexOf('/'))).anySatisfy(n -> path.equals(n.getPath())), path);
        Assertions.assertEquals(Files.readString(manifest), storage.getContentAsText(path), path);
        try (InputStream content = storage.getContent(path))
        {
            Assertions.assertNotNull(PCTManifestLoader.loadFromStream(content, path).adapter, path);
        }
    }

    private static Path findRepositoryRoot()
    {
        for (Path path = Paths.get("").toAbsolutePath(); path != null; path = path.getParent())
        {
            if (Files.isDirectory(path.resolve("legend-engine-core")))
            {
                return path;
            }
        }
        throw new IllegalStateException("Cannot locate the legend-engine checkout");
    }

    private static List<Path> findManifests(Path root) throws Exception
    {
        List<Path> manifests = new ArrayList<>();
        Files.walkFileTree(root, new SimpleFileVisitor<Path>()
        {
            @Override
            public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes)
            {
                String name = directory.getFileName().toString();
                return name.startsWith(".") || "target".equals(name) || "node_modules".equals(name) ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes)
            {
                if (file.getFileName().toString().endsWith("_manifest.json") && file.toString().replace('\\', '/').contains("/src/main/resources/"))
                {
                    manifests.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return manifests;
    }
}
