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

import org.finos.legend.pure.m3.pct.shared.PCTManifestLoader;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositorySet;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

public class TestPCTManifestRepositorySelection
{
    @Test
    public void classpathSelectionDiscoversManifestOwnersAndTheirDependencies(@TempDir Path resources) throws Exception
    {
        CodeRepositorySet available = CodeRepositorySet.newBuilder().withCodeRepositories(
                GenericCodeRepository.build("platform", ".*"),
                GenericCodeRepository.build("core", ".*", "platform"),
                GenericCodeRepository.build("pure_ide", ".*", "core"),
                GenericCodeRepository.build("core_adapter_dependency", ".*", "core"),
                GenericCodeRepository.build("core_new_adapter", ".*", "core_adapter_dependency"),
                GenericCodeRepository.build("core_new_functions", ".*", "core"),
                GenericCodeRepository.build("unrelated", ".*", "platform")).build();
        String adapterManifest = "/core_new_adapter/Functions_manifest.json";
        String coreManifest = "/core_new_functions/Functions_manifest.json";
        for (String path : new String[]{adapterManifest, coreManifest})
        {
            Path file = resources.resolve(path.substring(1));
            Files.createDirectories(file.getParent());
            Files.writeString(file, "{\"adapter\":\"test::adapter_Function_1__X_o_\",\"exclusions\":[]}");
        }

        try (URLClassLoader loader = new URLClassLoader(new URL[]{resources.toUri().toURL()}, null))
        {
            ClassLoaderCodeStorage storage = new ClassLoaderCodeStorage(loader, available.getRepositories());
            CodeRepositorySet selected = PCTManifestRepositories.selectRepositories(available, storage);
            Assertions.assertEquals(6, selected.size());
            Assertions.assertTrue(selected.hasRepository("core_new_adapter"));
            Assertions.assertTrue(selected.hasRepository("core_adapter_dependency"));
            Assertions.assertTrue(selected.hasRepository("core_new_functions"));
            Assertions.assertFalse(selected.hasRepository("unrelated"));
            ClassLoaderCodeStorage ideStorage = new ClassLoaderCodeStorage(loader, selected.getRepositories());
            for (String path : new String[]{adapterManifest, coreManifest})
            {
                try (InputStream content = ideStorage.getContent(path))
                {
                    Assertions.assertEquals("test::adapter_Function_1__X_o_", PCTManifestLoader.loadFromStream(content, path).adapter);
                }
            }
        }
    }

    @Test
    public void classpathSelectionWorksWithoutOptionalAdapters(@TempDir Path resources) throws Exception
    {
        CodeRepositorySet available = CodeRepositorySet.newBuilder().withCodeRepositories(
                GenericCodeRepository.build("platform", ".*"),
                GenericCodeRepository.build("core", ".*", "platform"),
                GenericCodeRepository.build("pure_ide", ".*", "core")).build();
        try (URLClassLoader loader = new URLClassLoader(new URL[]{resources.toUri().toURL()}, null))
        {
            ClassLoaderCodeStorage storage = new ClassLoaderCodeStorage(loader, available.getRepositories());
            Assertions.assertEquals(available, PCTManifestRepositories.selectRepositories(available, storage));
        }
    }

}
