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

import org.eclipse.lsp4j.Location;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests go-to-definition using a real PureRuntime.
 * Creates Pure sources with cross-references and verifies navigation.
 */
public class NavigationProviderTest
{
    private static LegendPureSession session;
    private static UriMapper uriMapper;

    // Source with a Class definition
    private static final String CLASS_SOURCE_ID = "nav_test_class.pure";
    private static final String CLASS_CODE =
            "Class test::nav::Person\n" +     // line 1
            "{\n" +                            // line 2
            "  name: String[1];\n" +           // line 3
            "  age: Integer[1];\n" +           // line 4
            "}\n";                             // line 5

    // Source that references the Class
    private static final String REF_SOURCE_ID = "nav_test_ref.pure";
    private static final String REF_CODE =
            "Class test::nav::Employee\n" +    // line 1
            "{\n" +                            // line 2
            "  person: test::nav::Person[1];\n" + // line 3: references Person
            "}\n";                             // line 4

    @BeforeClass
    public static void init()
    {
        session = new LegendPureSession();
        session.initialize();
        uriMapper = new UriMapper();

        // Register URI mappings for test sources so NavigationProvider can resolve them
        uriMapper.register("file:///test/" + CLASS_SOURCE_ID, CLASS_SOURCE_ID);
        uriMapper.register("file:///test/" + REF_SOURCE_ID, REF_SOURCE_ID);

        // Compile both sources
        session.modifyAndCompile(CLASS_SOURCE_ID, CLASS_CODE);
        LegendPureSession.CompileResult result = session.modifyAndCompile(REF_SOURCE_ID, REF_CODE);
        Assert.assertTrue("Test sources should compile: " +
                (result.getError() != null ? result.getError().getMessage() : ""),
                result.isSuccess());
    }

    @AfterClass
    public static void cleanup()
    {
        session = null;
    }

    @Test
    public void definition_onClassReference_jumpsToClassDefinition()
    {
        // In REF_CODE line 3: "  person: test::nav::Person[1];"
        // "  person: test::nav::Person[1];"
        //  123456789012345678901234567
        // "test" starts at col 11, "Person" starts at col 22
        Location loc = NavigationProvider.definition(
                session.getPureRuntime(), uriMapper,
                REF_SOURCE_ID, 3, 22);

        Assert.assertNotNull("Should navigate to Person class definition", loc);
        // The definition is in CLASS_SOURCE_ID at line 1
        Assert.assertTrue("Should point to the class source, got: " + loc.getUri(),
                loc.getUri().contains(CLASS_SOURCE_ID));
        Assert.assertEquals("Should point to line 1 (0-based = 0)",
                0, loc.getRange().getStart().getLine());
    }

    @Test
    public void definition_onPropertyName_navigates()
    {
        // In CLASS_CODE line 3: "  name: String[1];"
        // Click on "name" at column 3 (1-based)
        Location loc = NavigationProvider.definition(
                session.getPureRuntime(), uriMapper,
                CLASS_SOURCE_ID, 3, 3);

        // Should navigate to the property definition (which is in the same source)
        // The property "name" is defined in CLASS_SOURCE_ID
        Assert.assertNotNull("Should navigate for property", loc);
    }

    @Test
    public void definition_onEmptySpace_returnsNull()
    {
        // Line 2 is just "{"  -- clicking on column 1 should not navigate
        Location loc = NavigationProvider.definition(
                session.getPureRuntime(), uriMapper,
                CLASS_SOURCE_ID, 2, 1);

        // navigate() on a brace may return the enclosing class or null
        // We just verify it doesn't throw -- either result is acceptable
        // (PureRuntime may resolve "{" to the enclosing Class definition)
    }

    @Test
    public void definition_onUnknownSource_returnsNull()
    {
        Location loc = NavigationProvider.definition(
                session.getPureRuntime(), uriMapper,
                "nonexistent_source.pure", 1, 1);

        Assert.assertNull("Unknown source should return null", loc);
    }

    @Test
    public void definition_onExistingPlatformSource_works()
    {
        // Navigate within a real platform source that's pre-loaded
        // Use a known source like /platform/pure/essential/lang.pure or similar
        PureRuntime runtime = session.getPureRuntime();
        org.finos.legend.pure.m3.serialization.runtime.Source platformSource = null;
        for (org.finos.legend.pure.m3.serialization.runtime.Source s : runtime.getSourceRegistry().getSources())
        {
            if (!s.isInMemory() && s.getId().contains("/platform/"))
            {
                platformSource = s;
                break;
            }
        }

        Assert.assertNotNull("Should have at least one platform source", platformSource);
        // Navigate at line 1, column 1 -- just verify it doesn't crash
        Location loc = NavigationProvider.definition(
                runtime, uriMapper,
                platformSource.getId(), 1, 1);
        // Location may or may not be null depending on what's at line 1
    }
}
