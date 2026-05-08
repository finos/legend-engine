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

package org.finos.legend.engine.test.emit;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestEMITModelLoader
{
    @Test
    void diamondDependencyLoadsSharedFileOnce()
    {
        Path emitYaml = resource("emit-models/diamond/diamond.emit.yaml");

        EMITSourceSet sourceSet = loadSilently(emitYaml);

        // Primary scope: only the diamond's own model file.
        MutableList<String> modelVirtualPaths = ListIterate.collect(sourceSet.getModelFiles(), EMITSourceFile::getVirtualPath);
        Assertions.assertEquals(Lists.fixedSize.with("model/main.pure"), modelVirtualPaths,
                "Primary model scope should contain only the diamond's own file");

        // Dependency scope: left.pure, right.pure, and the shared file exactly once
        // (despite being reachable via both left-dep and right-dep).
        MutableList<String> depVirtualPaths = ListIterate.collect(sourceSet.getDependencyFiles(), EMITSourceFile::getVirtualPath).sortThis();
        Assertions.assertEquals(
                Lists.fixedSize.with("model/left.pure", "model/right.pure", "model/shared.pure"),
                depVirtualPaths,
                "Diamond dependency must load shared.pure exactly once");

        int sharedCount = ListIterate.count(sourceSet.getDependencyFiles(), f -> "model/shared.pure".equals(f.getVirtualPath()));
        Assertions.assertEquals(1, sharedCount, "shared.pure must appear exactly once in the dependency list");
    }

    @Test
    void clashingVirtualPathsAcrossDependenciesAreReported()
    {
        Path emitYaml = resource("emit-models/diamond/clash-test.emit.yaml");

        EMITModelLoader loader = new EMITModelLoader();
        IllegalStateException error = Assertions.assertThrows(IllegalStateException.class, () -> loader.load(emitYaml));
        Assertions.assertEquals(
                "Source file virtual-path clashes: model/shared.pure",
                error.getMessage(),
                () -> "Expected clash error to name the offending virtual path; got: " + error.getMessage());
    }

    private static EMITSourceSet loadSilently(Path emitYaml)
    {
        try
        {
            return new EMITModelLoader().load(emitYaml);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    private static Path resource(String name)
    {
        URL url = Thread.currentThread().getContextClassLoader().getResource(name);
        Assertions.assertNotNull(url, "test resource not found: " + name);
        try
        {
            return Paths.get(url.toURI());
        }
        catch (URISyntaxException e)
        {
            throw new RuntimeException(e);
        }
    }
}
