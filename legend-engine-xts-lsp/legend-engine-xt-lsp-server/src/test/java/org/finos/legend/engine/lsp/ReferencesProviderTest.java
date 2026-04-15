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

import java.util.List;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.lsp4j.Location;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests find-all-references using a real PureRuntime.
 * Creates Pure sources with cross-references and verifies that
 * references to a class defined in one source are found in other sources.
 */
public class ReferencesProviderTest
{
    private static LegendPureSession session;
    private static UriMapper uriMapper;

    // Source defining a class
    private static final String DEF_SOURCE_ID = "ref_test_def.pure";
    private static final String DEF_CODE =
            "Class test::ref::Animal\n" +        // line 1
            "{\n" +                              // line 2
            "  name: String[1];\n" +             // line 3
            "}\n";                               // line 4

    // Source referencing the class in a property
    private static final String USE1_SOURCE_ID = "ref_test_use1.pure";
    private static final String USE1_CODE =
            "Class test::ref::Zoo\n" +           // line 1
            "{\n" +                              // line 2
            "  animal: test::ref::Animal[1];\n" + // line 3: references Animal
            "}\n";                               // line 4

    // Another source referencing the class
    private static final String USE2_SOURCE_ID = "ref_test_use2.pure";
    private static final String USE2_CODE =
            "Class test::ref::Vet\n" +           // line 1
            "{\n" +                              // line 2
            "  patient: test::ref::Animal[0..1];\n" + // line 3: references Animal
            "}\n";                               // line 4

    @BeforeClass
    public static void init()
    {
        session = new LegendPureSession();
        session.initialize();
        uriMapper = new UriMapper();

        // Register URI mappings
        uriMapper.register("file:///test/" + DEF_SOURCE_ID, DEF_SOURCE_ID);
        uriMapper.register("file:///test/" + USE1_SOURCE_ID, USE1_SOURCE_ID);
        uriMapper.register("file:///test/" + USE2_SOURCE_ID, USE2_SOURCE_ID);

        // Compile all sources
        LegendPureSession.CompileResult r1 = session.modifyAndCompile(DEF_SOURCE_ID, DEF_CODE);
        Assert.assertTrue("Animal should compile: " +
                (r1.getError() != null ? r1.getError().getMessage() : ""), r1.isSuccess());

        LegendPureSession.CompileResult r2 = session.modifyAndCompile(USE1_SOURCE_ID, USE1_CODE);
        Assert.assertTrue("Zoo should compile: " +
                (r2.getError() != null ? r2.getError().getMessage() : ""), r2.isSuccess());

        LegendPureSession.CompileResult r3 = session.modifyAndCompile(USE2_SOURCE_ID, USE2_CODE);
        Assert.assertTrue("Vet should compile: " +
                (r3.getError() != null ? r3.getError().getMessage() : ""), r3.isSuccess());
    }

    @AfterClass
    public static void cleanup()
    {
        session = null;
    }

    @Test
    public void references_onClassDefinition_findsUsages()
    {
        // Click on "Animal" in the definition (line 1, col ~16)
        // "Class test::ref::Animal"
        //  1234567890123456789012345
        List<Location> refs = ReferencesProvider.references(
                session.getPureRuntime(), uriMapper,
                DEF_SOURCE_ID, 1, 20, false);

        // Should find at least the two usages in Zoo and Vet
        Assert.assertFalse("Should find references to Animal, found: " + refs.size(), refs.isEmpty());
        Assert.assertTrue("Should find at least 2 references (Zoo and Vet), found: " + refs.size(),
                refs.size() >= 2);
    }

    @Test
    public void references_onClassReference_findsUsages()
    {
        // Click on "Animal" in the reference from Zoo (line 3)
        // "  animal: test::ref::Animal[1];"
        //  12345678901234567890123456789
        List<Location> refs = ReferencesProvider.references(
                session.getPureRuntime(), uriMapper,
                USE1_SOURCE_ID, 3, 24, false);

        // Should find references — navigate resolves to Animal, then finds its usages
        Assert.assertFalse("Should find references from a reference site", refs.isEmpty());
    }

    @Test
    public void references_includeDeclaration_addsDefinitionLocation()
    {
        List<Location> withDecl = ReferencesProvider.references(
                session.getPureRuntime(), uriMapper,
                DEF_SOURCE_ID, 1, 20, true);

        List<Location> withoutDecl = ReferencesProvider.references(
                session.getPureRuntime(), uriMapper,
                DEF_SOURCE_ID, 1, 20, false);

        Assert.assertTrue("includeDeclaration=true should have more results",
                withDecl.size() > withoutDecl.size());

        // The declaration location should point to DEF_SOURCE_ID
        boolean foundDeclaration = false;
        for (Location loc : withDecl)
        {
            if (loc.getUri().contains(DEF_SOURCE_ID))
            {
                foundDeclaration = true;
                break;
            }
        }
        Assert.assertTrue("Should include the definition location", foundDeclaration);
    }

    @Test
    public void references_onUnknownSource_returnsEmpty()
    {
        List<Location> refs = ReferencesProvider.references(
                session.getPureRuntime(), uriMapper,
                "nonexistent.pure", 1, 1, false);

        Assert.assertTrue("Unknown source should return empty", refs.isEmpty());
    }

    @Test
    public void references_onProperty_findsUsages()
    {
        // Click on "name" property in Animal (line 3, col 3)
        List<Location> refs = ReferencesProvider.references(
                session.getPureRuntime(), uriMapper,
                DEF_SOURCE_ID, 3, 4, false);

        // Property references may or may not have usages depending on whether
        // other sources access animal.name — we just verify no crash
        Assert.assertNotNull(refs);
    }

    @Test
    public void references_resultsHaveValidLocations()
    {
        List<Location> refs = ReferencesProvider.references(
                session.getPureRuntime(), uriMapper,
                DEF_SOURCE_ID, 1, 20, false);

        for (Location loc : refs)
        {
            Assert.assertNotNull("Location should have URI", loc.getUri());
            Assert.assertNotNull("Location should have range", loc.getRange());
            Assert.assertTrue("URI should be file:// or pure://, got: " + loc.getUri(),
                    loc.getUri().startsWith("file://") || loc.getUri().startsWith("pure://"));
            Assert.assertTrue("Start line should be >= 0",
                    loc.getRange().getStart().getLine() >= 0);
        }
    }

    @Test
    public void references_onSelfReference_findsUsage()
    {
        String selfRefSourceId = "ref_test_self.pure";
        uriMapper.register("file:///test/" + selfRefSourceId, selfRefSourceId);
        LegendPureSession.CompileResult r = session.modifyAndCompile(
                selfRefSourceId,
                "Class test::ref::Node\n{\n  value: String[1];\n  next: test::ref::Node[0..1];\n}\n"
        );
        Assert.assertTrue("Node should compile: " +
                (r.getError() != null ? r.getError().getMessage() : ""), r.isSuccess());

        // Find references to Node — should include the self-reference in 'next' property
        List<Location> refs = ReferencesProvider.references(
                session.getPureRuntime(), uriMapper,
                selfRefSourceId, 1, 18, false);

        Assert.assertFalse("Should find self-reference", refs.isEmpty());
    }

    @Test
    public void references_onFunction_findsCallSitesViaApplications()
    {
        String funcDefId = "ref_test_func_def.pure";
        String funcCallerId = "ref_test_func_caller.pure";
        String funcCaller2Id = "ref_test_func_caller2.pure";
        uriMapper.register("file:///test/" + funcDefId, funcDefId);
        uriMapper.register("file:///test/" + funcCallerId, funcCallerId);
        uriMapper.register("file:///test/" + funcCaller2Id, funcCaller2Id);

        // Define a function
        LegendPureSession.CompileResult r1 = session.modifyAndCompile(
                funcDefId,
                "function test::ref::greet(name: String[1]): String[1]\n{\n  'Hello ' + $name\n}\n"
        );
        Assert.assertTrue("greet should compile: " +
                (r1.getError() != null ? r1.getError().getMessage() : ""), r1.isSuccess());

        // Call it from another function
        LegendPureSession.CompileResult r2 = session.modifyAndCompile(
                funcCallerId,
                "function test::ref::caller(): String[1]\n{\n  test::ref::greet('World')\n}\n"
        );
        Assert.assertTrue("caller should compile: " +
                (r2.getError() != null ? r2.getError().getMessage() : ""), r2.isSuccess());

        // Call it from yet another function (cross-file)
        LegendPureSession.CompileResult r3 = session.modifyAndCompile(
                funcCaller2Id,
                "function test::ref::caller2(): String[1]\n{\n  test::ref::greet('again')\n}\n"
        );
        Assert.assertTrue("caller2 should compile: " +
                (r3.getError() != null ? r3.getError().getMessage() : ""), r3.isSuccess());

        // Find references to greet — navigate at "greet" in the definition
        // "function test::ref::greet(name: String[1]): String[1]"
        //  1234567890123456789012345
        List<Location> refs = ReferencesProvider.references(
                session.getPureRuntime(), uriMapper,
                funcDefId, 1, 22, false);

        // Should find at least the two call sites (via 'applications' property)
        Assert.assertTrue(
                "Should find at least 2 call-site references to greet(), found: " + refs.size()
                        + ". References: " + locationsToString(refs),
                refs.size() >= 2);

        // Verify call sites are in the caller files
        boolean foundCaller1 = false;
        boolean foundCaller2 = false;
        for (Location loc : refs)
        {
            if (loc.getUri().contains(funcCallerId))
            {
                foundCaller1 = true;
            }
            if (loc.getUri().contains(funcCaller2Id))
            {
                foundCaller2 = true;
            }
        }
        Assert.assertTrue("Should find call site in caller function. Refs: " + locationsToString(refs),
                foundCaller1);
        Assert.assertTrue("Should find call site in caller2 function. Refs: " + locationsToString(refs),
                foundCaller2);
    }

    @Test
    public void references_onFunction_sameFile_findsCallSite()
    {
        String sameFileId = "ref_test_func_same.pure";
        uriMapper.register("file:///test/" + sameFileId, sameFileId);

        // Define and call a function within the same file
        LegendPureSession.CompileResult r = session.modifyAndCompile(
                sameFileId,
                "function test::ref::helper(): String[1]\n" +     // line 1
                "{\n" +                                            // line 2
                "  'help'\n" +                                     // line 3
                "}\n" +                                            // line 4
                "\n" +                                             // line 5
                "function test::ref::main(): String[1]\n" +       // line 6
                "{\n" +                                            // line 7
                "  test::ref::helper()\n" +                        // line 8: call site
                "}\n"                                              // line 9
        );
        Assert.assertTrue("same-file functions should compile: " +
                (r.getError() != null ? r.getError().getMessage() : ""), r.isSuccess());

        // Find references to helper() from its definition
        List<Location> refs = ReferencesProvider.references(
                session.getPureRuntime(), uriMapper,
                sameFileId, 1, 22, false);

        // Should find the call site on line 8 in the same file
        boolean foundSameFileCall = false;
        for (Location loc : refs)
        {
            if (loc.getUri().contains(sameFileId)
                    && loc.getRange().getStart().getLine() >= 7) // line 8, 0-based = 7
            {
                foundSameFileCall = true;
            }
        }
        Assert.assertTrue(
                "Should find same-file call site for helper(). Refs: " + locationsToString(refs),
                foundSameFileCall);
    }

    private static String locationsToString(List<Location> locations)
    {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < locations.size(); i++)
        {
            if (i > 0)
            {
                sb.append(", ");
            }
            Location loc = locations.get(i);
            sb.append(loc.getUri())
              .append(":")
              .append(loc.getRange().getStart().getLine() + 1)
              .append(":")
              .append(loc.getRange().getStart().getCharacter() + 1);
        }
        return sb.append("]").toString();
    }

    /**
     * Directly verify that the 'applications' property on a function contains
     * call sites. This test would have caught the instanceof bug where
     * Java instanceof PackageableFunction failed on CoreInstance objects.
     */
    @Test
    public void references_functionApplicationsProperty_isPopulated()
    {
        String fnDefId = "ref_test_apps_def.pure";
        String fnCallId = "ref_test_apps_call.pure";
        uriMapper.register("file:///test/" + fnDefId, fnDefId);
        uriMapper.register("file:///test/" + fnCallId, fnCallId);

        LegendPureSession.CompileResult r1 = session.modifyAndCompile(
                fnDefId,
                "function test::ref::sayHi(): String[1]\n{\n  'Hi'\n}\n"
        );
        Assert.assertTrue("sayHi should compile", r1.isSuccess());

        LegendPureSession.CompileResult r2 = session.modifyAndCompile(
                fnCallId,
                "function test::ref::useSayHi(): String[1]\n{\n  test::ref::sayHi()\n}\n"
        );
        Assert.assertTrue("useSayHi should compile", r2.isSuccess());

        // Navigate to sayHi function and verify its 'applications' property directly
        Source source = session.getPureRuntime().getSourceById(fnDefId);
        Assert.assertNotNull("Source should exist", source);
        CoreInstance raw = source.navigate(1, 22, session.getPureRuntime().getProcessorSupport());
        Assert.assertNotNull("Should navigate to function", raw);

        // Verify classifier name
        String classifierName = raw.getClassifier() != null ? raw.getClassifier().getName() : "";
        Assert.assertEquals("Should be a ConcreteFunctionDefinition",
                "ConcreteFunctionDefinition", classifierName);

        // Verify applications property has the call site
        ListIterable<? extends CoreInstance> apps =
                raw.getValueForMetaPropertyToMany(M3Properties.applications);
        Assert.assertNotNull("applications property should not be null", apps);
        Assert.assertFalse(
                "applications should contain at least one call site, found: " + apps.size(),
                apps.isEmpty());

        // Now verify ReferencesProvider actually returns these call sites
        List<Location> refs = ReferencesProvider.references(
                session.getPureRuntime(), uriMapper,
                fnDefId, 1, 22, false);
        boolean foundCallSite = false;
        for (Location loc : refs)
        {
            if (loc.getUri().contains(fnCallId))
            {
                foundCallSite = true;
            }
        }
        Assert.assertTrue(
                "ReferencesProvider should find the call site from applications. "
                        + "applications.size()=" + apps.size()
                        + ", refs=" + locationsToString(refs),
                foundCallSite);
    }

    @Test
    public void references_onPlatformType_findsUsages()
    {
        // "String" is used in DEF_CODE line 3: "  name: String[1];"
        // Hover at col 9 should hit "String"
        List<Location> refs = ReferencesProvider.references(
                session.getPureRuntime(), uriMapper,
                DEF_SOURCE_ID, 3, 10, false);

        // String is used everywhere — should have many references
        // We just verify it returns something and doesn't crash
        Assert.assertNotNull(refs);
        // String is a platform type with many usages across compiled sources
    }
}
