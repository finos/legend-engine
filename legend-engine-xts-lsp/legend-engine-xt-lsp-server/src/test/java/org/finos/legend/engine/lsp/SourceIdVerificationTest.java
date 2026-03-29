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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Verifies our assumptions about PureRuntime source IDs.
 * These tests check the ACTUAL source IDs created by PureRuntime
 * to ensure our UriMapper logic is correct.
 */
public class SourceIdVerificationTest
{
    private static LegendPureSession session;

    @BeforeClass
    public static void initSession()
    {
        session = new LegendPureSession();
        session.initialize();
    }

    @AfterClass
    public static void cleanup()
    {
        session = null;
    }

    @Test
    public void sourceIds_startWithSlash()
    {
        // Verify that all storage-backed source IDs start with /
        PureRuntime runtime = session.getPureRuntime();
        MutableList<String> nonSlashIds = Lists.mutable.empty();
        for (Source source : runtime.getSourceRegistry().getSources())
        {
            if (!source.isInMemory() && !source.getId().startsWith("/"))
            {
                nonSlashIds.add(source.getId());
            }
        }
        Assert.assertTrue(
                "Expected all storage source IDs to start with /, but found: " + nonSlashIds.makeString(", "),
                nonSlashIds.isEmpty()
        );
    }

    @Test
    public void sourceIds_matchExpectedPattern()
    {
        // Verify source IDs look like /repoName/path/to/file.pure
        // This is the pattern UriMapper.deriveSourceId produces
        PureRuntime runtime = session.getPureRuntime();
        MutableList<String> sampleIds = Lists.mutable.empty();
        int count = 0;
        for (Source source : runtime.getSourceRegistry().getSources())
        {
            if (!source.isInMemory() && count < 10)
            {
                sampleIds.add(source.getId());
                count++;
            }
        }
        Assert.assertFalse("Should have some sources loaded", sampleIds.isEmpty());

        // Print sample IDs for manual inspection
        System.out.println("Sample PureRuntime source IDs:");
        for (String id : sampleIds)
        {
            System.out.println("  " + id);
        }

        // All should match /repoName/...
        for (String id : sampleIds)
        {
            Assert.assertTrue("Source ID should start with /: " + id, id.startsWith("/"));
            Assert.assertTrue("Source ID should end with .pure: " + id, id.endsWith(".pure"));
        }
    }

    @Test
    public void modifyExistingSource_byId_works()
    {
        // Pick a real source from the registry and verify we can modify + compile it
        PureRuntime runtime = session.getPureRuntime();
        Source firstSource = null;
        for (Source source : runtime.getSourceRegistry().getSources())
        {
            if (!source.isInMemory() && !source.isImmutable())
            {
                firstSource = source;
                break;
            }
        }

        if (firstSource == null)
        {
            // All sources are immutable (platform). Try with an immutable one -- modify should still work in-memory.
            for (Source source : runtime.getSourceRegistry().getSources())
            {
                if (!source.isInMemory())
                {
                    firstSource = source;
                    break;
                }
            }
        }

        Assert.assertNotNull("Should have at least one source", firstSource);
        String sourceId = firstSource.getId();
        String originalContent = firstSource.getContent();
        Assert.assertNotNull("Source should have content: " + sourceId, originalContent);

        System.out.println("Modifying source: " + sourceId + " (length=" + originalContent.length() + ")");

        // Modify with original content (no-op change) -- should compile successfully
        LegendPureSession.CompileResult result = session.modifyAndCompile(sourceId, originalContent);
        Assert.assertTrue("Modifying existing source should succeed: " + sourceId +
                        (result.getError() != null ? " error: " + result.getError().getMessage() : ""),
                result.isSuccess());
    }

    @Test
    public void uriMapper_derivedIds_matchRealSourceIds()
    {
        // Simulate what UriMapper does: given a path like
        // file:///home/.../src/main/resources/core/pure/corefunctions/lang.pure
        // it strips to /core/pure/corefunctions/lang.pure
        // Verify this matches a real source ID in the registry

        PureRuntime runtime = session.getPureRuntime();
        boolean foundMatch = false;
        for (Source source : runtime.getSourceRegistry().getSources())
        {
            if (!source.isInMemory())
            {
                String id = source.getId();
                // Simulate the reverse: the filesystem path would be
                // .../src/main/resources{id}
                // UriMapper.deriveSourceId("file:///x/src/main/resources" + id) should return "/" + id.substring(1)
                String fakeUri = "file:///workspace/src/main/resources" + id;
                String derived = new UriMapper().deriveSourceId(fakeUri);
                Assert.assertEquals("UriMapper should derive the correct source ID for " + id,
                        id, derived);
                foundMatch = true;
            }
        }
        Assert.assertTrue("Should have verified at least one source", foundMatch);
    }

    @Test
    public void sourceCount_isReasonable()
    {
        // Sanity check: PureRuntime should have loaded a meaningful number of sources
        PureRuntime runtime = session.getPureRuntime();
        int count = 0;
        for (Source source : runtime.getSourceRegistry().getSources())
        {
            count++;
        }
        System.out.println("Total sources loaded: " + count);
        Assert.assertTrue("Should have loaded many sources (got " + count + ")", count > 100);
    }
}
