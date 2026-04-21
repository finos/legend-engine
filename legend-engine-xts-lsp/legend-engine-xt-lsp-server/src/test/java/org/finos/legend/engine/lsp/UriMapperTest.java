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

package org.finos.legend.engine.lsp;

import org.junit.Assert;
import org.junit.Test;

public class UriMapperTest
{
    @Test
    public void deriveSourceId_stripsResourcesPrefix()
    {
        UriMapper mapper = new UriMapper();
        String uri = "file:///home/user/legend-engine/legend-engine-xts-relationalStore/src/main/resources/core_relational/tests/model.pure";
        Assert.assertEquals("/core_relational/tests/model.pure", mapper.deriveSourceId(uri));
    }

    @Test
    public void deriveSourceId_handlesNestedResourcesPath()
    {
        UriMapper mapper = new UriMapper();
        String uri = "file:///home/user/legend-engine/legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core/src/main/resources/core/pure/corefunctions/lang.pure";
        Assert.assertEquals("/core/pure/corefunctions/lang.pure", mapper.deriveSourceId(uri));
    }

    @Test
    public void deriveSourceId_fallsBackToFilename_whenNoResourcesMarker()
    {
        UriMapper mapper = new UriMapper();
        String uri = "file:///home/user/project/model.pure";
        Assert.assertEquals("model.pure", mapper.deriveSourceId(uri));
    }

    @Test
    public void deriveSourceId_handlesPureScheme()
    {
        UriMapper mapper = new UriMapper();
        Assert.assertEquals("/core/pure/extensions/extension.pure",
                mapper.deriveSourceId("pure:///core/pure/extensions/extension.pure"));
    }

    @Test
    public void deriveSourceId_pureScheme_roundTrips()
    {
        UriMapper mapper = new UriMapper();
        // toUri generates pure:// for platform sources
        String pureUri = mapper.toUri("/platform/pure/essential/lang.pure");
        Assert.assertNotNull(pureUri);
        Assert.assertTrue("Should be pure:// URI", pureUri.startsWith("pure://"));

        // deriveSourceId should extract the source ID back
        String sourceId = mapper.deriveSourceId(pureUri);
        Assert.assertEquals("/platform/pure/essential/lang.pure", sourceId);
    }

    @Test
    public void toSourceId_pureScheme_worksForLspFeatures()
    {
        UriMapper mapper = new UriMapper();
        // When a pure:// document sends an LSP request, toSourceId must handle it
        String sourceId = mapper.toSourceId("pure:///core/pure/extensions/extension.pure");
        Assert.assertEquals("/core/pure/extensions/extension.pure", sourceId);
    }

    @Test
    public void register_overridesDerived()
    {
        UriMapper mapper = new UriMapper();
        String uri = "file:///a/b/c.pure";
        mapper.register(uri, "/custom/c.pure");

        Assert.assertEquals("/custom/c.pure", mapper.toSourceId(uri));
        Assert.assertEquals(uri, mapper.toUri("/custom/c.pure"));
    }

    @Test
    public void toSourceId_cachesDerivedResult()
    {
        UriMapper mapper = new UriMapper();
        String uri = "file:///x/src/main/resources/core/test.pure";

        String first = mapper.toSourceId(uri);
        String second = mapper.toSourceId(uri);

        Assert.assertEquals("/core/test.pure", first);
        Assert.assertSame(first, second);
    }

    @Test
    public void toSourceId_populatesReverseMap()
    {
        UriMapper mapper = new UriMapper();
        String uri = "file:///x/src/main/resources/core/test.pure";
        mapper.toSourceId(uri);

        Assert.assertEquals(uri, mapper.toUri("/core/test.pure"));
    }

    @Test
    public void deriveSourceId_usesRepoScanner_whenInsideKnownRepo() throws Exception
    {
        // Set up a temp dir simulating a repo resources root
        java.nio.file.Path tempDir = java.nio.file.Files.createTempDirectory("lsp_test");
        java.nio.file.Path resourcesDir = tempDir.resolve("mod/src/main/resources");
        java.nio.file.Files.createDirectories(resourcesDir);
        java.nio.file.Files.write(resourcesDir.resolve("my_repo.definition.json"),
                "{\"name\": \"my_repo\"}".getBytes());
        java.nio.file.Path pureFile = resourcesDir.resolve("my_repo/sub/file.pure");
        java.nio.file.Files.createDirectories(pureFile.getParent());
        java.nio.file.Files.write(pureFile, "Class X {}".getBytes());

        RepositoryScanner scanner = new RepositoryScanner();
        scanner.scan(java.util.Collections.singletonList(tempDir));

        UriMapper mapper = new UriMapper();
        mapper.setRepositoryScanner(scanner);

        // File inside known repo but URI doesn't have src/main/resources marker in the standard position
        String uri = pureFile.toUri().toString();
        String sourceId = mapper.deriveSourceId(uri);
        Assert.assertEquals("/my_repo/sub/file.pure", sourceId);

        // Cleanup
        java.nio.file.Files.walk(tempDir).sorted(java.util.Comparator.reverseOrder())
                .forEach(p ->
                {
                    try
                    {
                        java.nio.file.Files.delete(p);
                    }
                    catch (Exception ignored)
                    {
                        // Best-effort cleanup for the temporary test workspace.
                    }
                });
    }

    @Test
    public void toUri_returnsPureScheme_forUnknownStorageSourceId()
    {
        UriMapper mapper = new UriMapper();
        // Storage sources (start with /) should fall back to pure:// URIs
        String uri = mapper.toUri("/core/pure/extensions/extension.pure");
        Assert.assertNotNull("Should return pure:// URI for unknown storage source", uri);
        Assert.assertEquals("pure:///core/pure/extensions/extension.pure", uri);
    }

    @Test
    public void toUri_returnsNull_forUnknownInMemorySourceId()
    {
        UriMapper mapper = new UriMapper();
        // In-memory sources (no leading /) have no fallback
        Assert.assertNull(mapper.toUri("some_test.pure"));
    }

    @Test
    public void toUri_slashAgnostic_findsAlternateForm()
    {
        UriMapper mapper = new UriMapper();
        mapper.register("file:///test/foo.pure", "/foo.pure");

        // Looking up without leading slash should find the /foo.pure entry
        Assert.assertEquals("file:///test/foo.pure", mapper.toUri("foo.pure"));
    }

    @Test
    public void clear_removesCachedMappings()
    {
        UriMapper mapper = new UriMapper();
        mapper.register("file:///a.pure", "/a.pure");
        Assert.assertEquals("file:///a.pure", mapper.toUri("/a.pure"));

        mapper.clear();

        // After clear, the registered file:// mapping is gone.
        // For storage sources (leading /), the pure:// fallback kicks in.
        String uri = mapper.toUri("/a.pure");
        Assert.assertEquals("pure:///a.pure", uri);
    }
}
