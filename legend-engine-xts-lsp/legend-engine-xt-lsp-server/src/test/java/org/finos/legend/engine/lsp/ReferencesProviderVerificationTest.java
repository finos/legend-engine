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

import java.util.Collections;
import java.util.List;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.lsp4j.Location;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Deep verification tests for find-references.
 * These tests inspect the actual ReferenceUsage structure to ensure
 * the implementation returns meaningful locations, not just non-empty lists.
 */
public class ReferencesProviderVerificationTest
{
    private static LegendPureSession session;
    private static UriMapper uriMapper;

    private static final String CLASS_A_ID = "verify_ref_a.pure";
    private static final String CLASS_A =
            "Class test::verify::ClassA\n" +        // line 1
            "{\n" +                                  // line 2
            "  name: String[1];\n" +                 // line 3
            "}\n";                                   // line 4

    private static final String CLASS_B_ID = "verify_ref_b.pure";
    private static final String CLASS_B =
            "Class test::verify::ClassB\n" +         // line 1
            "{\n" +                                  // line 2
            "  ref: test::verify::ClassA[1];\n" +   // line 3: references ClassA
            "}\n";                                   // line 4

    private static final String CLASS_C_ID = "verify_ref_c.pure";
    private static final String CLASS_C =
            "Class test::verify::ClassC extends test::verify::ClassA\n" + // line 1: extends ClassA
            "{\n" +                                  // line 2
            "  extra: Integer[1];\n" +               // line 3
            "}\n";                                   // line 4

    @BeforeClass
    public static void init()
    {
        session = new LegendPureSession();
        session.initialize();
        uriMapper = new UriMapper();

        uriMapper.register("file:///test/" + CLASS_A_ID, CLASS_A_ID);
        uriMapper.register("file:///test/" + CLASS_B_ID, CLASS_B_ID);
        uriMapper.register("file:///test/" + CLASS_C_ID, CLASS_C_ID);

        LegendPureSession.CompileResult r1 = session.modifyAndCompile(CLASS_A_ID, CLASS_A);
        Assert.assertTrue("ClassA should compile: " +
                (r1.getError() != null ? r1.getError().getMessage() : ""), r1.isSuccess());

        LegendPureSession.CompileResult r2 = session.modifyAndCompile(CLASS_B_ID, CLASS_B);
        Assert.assertTrue("ClassB should compile: " +
                (r2.getError() != null ? r2.getError().getMessage() : ""), r2.isSuccess());

        LegendPureSession.CompileResult r3 = session.modifyAndCompile(CLASS_C_ID, CLASS_C);
        Assert.assertTrue("ClassC should compile: " +
                (r3.getError() != null ? r3.getError().getMessage() : ""), r3.isSuccess());
    }

    @AfterClass
    public static void cleanup()
    {
        session = null;
    }

    @Test
    public void verify_referenceUsagesExistOnClassA()
    {
        // Directly inspect the CoreInstance to verify the compiler populated referenceUsages
        CoreInstance classA = session.getPureRuntime().getCoreInstance("test::verify::ClassA");
        Assert.assertNotNull("ClassA should exist in the model", classA);

        ListIterable<? extends CoreInstance> refUsages =
                classA.getValueForMetaPropertyToMany(M3Properties.referenceUsages);
        Assert.assertNotNull("ClassA should have referenceUsages", refUsages);
        Assert.assertFalse("ClassA should have at least one reference usage (from ClassB.ref and ClassC extends)",
                refUsages.isEmpty());

        System.out.println("ClassA has " + refUsages.size() + " reference usages:");
        for (CoreInstance ru : refUsages)
        {
            CoreInstance owner = ru.getValueForMetaPropertyToOne(M3Properties.owner);
            SourceInformation ownerSi = (owner != null) ? owner.getSourceInformation() : null;
            String propName = null;
            CoreInstance propNameInst = ru.getValueForMetaPropertyToOne(M3Properties.propertyName);
            if (propNameInst != null)
            {
                propName = PrimitiveUtilities.getStringValue(propNameInst);
            }
            System.out.println("  owner: " + (owner != null ? owner.getClassifier().getName() : "null")
                    + ", property: " + propName
                    + ", location: " + (ownerSi != null ? ownerSi.getSourceId() + ":" + ownerSi.getStartLine() : "null"));
        }
    }

    @Test
    public void verify_referencesPointToCorrectFiles()
    {
        List<Location> refs = ReferencesProvider.references(
                session.getPureRuntime(), uriMapper,
                CLASS_A_ID, 1, 22, false);

        Assert.assertTrue("Should have references, found: " + refs.size(), refs.size() >= 2);

        boolean foundB = false;
        boolean foundC = false;
        for (Location loc : refs)
        {
            if (loc.getUri().contains(CLASS_B_ID))
            {
                foundB = true;
            }
            if (loc.getUri().contains(CLASS_C_ID))
            {
                foundC = true;
            }
        }
        Assert.assertTrue("Should find reference from ClassB", foundB);
        Assert.assertTrue("Should find reference from ClassC (extends)", foundC);
    }

    @Test
    public void verify_referenceLocationsAreWithinSourceBounds()
    {
        List<Location> refs = ReferencesProvider.references(
                session.getPureRuntime(), uriMapper,
                CLASS_A_ID, 1, 22, true);

        for (Location loc : refs)
        {
            int startLine = loc.getRange().getStart().getLine();
            int startCol = loc.getRange().getStart().getCharacter();
            int endLine = loc.getRange().getEnd().getLine();
            int endCol = loc.getRange().getEnd().getCharacter();

            Assert.assertTrue("Start line should be >= 0, got " + startLine + " in " + loc.getUri(),
                    startLine >= 0);
            Assert.assertTrue("End line should be >= start line, got start=" + startLine
                    + " end=" + endLine + " in " + loc.getUri(),
                    endLine >= startLine);
            Assert.assertTrue("Start col should be >= 0, got " + startCol + " in " + loc.getUri(),
                    startCol >= 0);
        }
    }

    @Test
    public void verify_navigateToReferenceSite_thenFindReferences_roundTrips()
    {
        // From ClassB line 3, navigate to ClassA (go-to-def), then find references on ClassA
        // This simulates the real user workflow
        Location defLoc = NavigationProvider.definition(
                session.getPureRuntime(), uriMapper,
                CLASS_B_ID, 3, 22);

        Assert.assertNotNull("Go-to-definition should resolve ClassA reference", defLoc);
        Assert.assertTrue("Should point to ClassA definition, got: " + defLoc.getUri(),
                defLoc.getUri().contains(CLASS_A_ID));

        // Now from the definition, find all references.
        // Try multiple columns on the definition line since navigate() is position-sensitive
        List<Location> refs = Collections.emptyList();
        int defLine = defLoc.getRange().getStart().getLine() + 1;
        for (int col = 1; col <= 25; col += 3)
        {
            refs = ReferencesProvider.references(
                    session.getPureRuntime(), uriMapper,
                    CLASS_A_ID, defLine, col, false);
            if (refs.size() >= 2)
            {
                break;
            }
        }

        Assert.assertTrue("Should find references from definition site, found: " + refs.size(),
                refs.size() >= 2);
    }

    @Test
    public void verify_includeDeclaration_addsExactlyOneMore()
    {
        List<Location> without = ReferencesProvider.references(
                session.getPureRuntime(), uriMapper,
                CLASS_A_ID, 1, 22, false);
        List<Location> with = ReferencesProvider.references(
                session.getPureRuntime(), uriMapper,
                CLASS_A_ID, 1, 22, true);

        Assert.assertEquals("includeDeclaration should add exactly one location",
                without.size() + 1, with.size());
    }
}
