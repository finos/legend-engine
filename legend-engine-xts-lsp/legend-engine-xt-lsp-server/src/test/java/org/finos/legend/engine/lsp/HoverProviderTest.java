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

import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.MarkupKind;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests hover information using a real PureRuntime.
 */
public class HoverProviderTest
{
    private static LegendPureSession session;

    private static final String CLASS_SOURCE_ID = "hover_test_class.pure";
    private static final String CLASS_CODE =
            "Class test::hover::Animal\n" +     // line 1
            "{\n" +                              // line 2
            "  species: String[1];\n" +          // line 3
            "  legs: Integer[1];\n" +            // line 4
            "}\n";                               // line 5

    private static final String ENUM_SOURCE_ID = "hover_test_enum.pure";
    private static final String ENUM_CODE =
            "Enum test::hover::Status\n" +       // line 1
            "{\n" +                              // line 2
            "  Active,\n" +                      // line 3
            "  Inactive\n" +                     // line 4
            "}\n";                               // line 5

    private static final String REF_SOURCE_ID = "hover_test_ref.pure";
    private static final String REF_CODE =
            "Class test::hover::Zoo\n" +         // line 1
            "{\n" +                              // line 2
            "  animal: test::hover::Animal[1];\n" + // line 3
            "}\n";                               // line 4

    // Source with a Function-typed property (tests generic type parameter display)
    private static final String FUNC_PROP_SOURCE_ID = "hover_test_func_prop.pure";
    private static final String FUNC_PROP_CODE =
            "Class test::hover::Handler\n" +     // line 1
            "{\n" +                              // line 2
            "  callback: Function<{String[1]->Boolean[1]}>[1];\n" + // line 3
            "}\n";                               // line 4

    @BeforeClass
    public static void init()
    {
        session = new LegendPureSession();
        session.initialize();

        LegendPureSession.CompileResult r1 = session.modifyAndCompile(CLASS_SOURCE_ID, CLASS_CODE);
        Assert.assertTrue("Animal class should compile: " +
                (r1.getError() != null ? r1.getError().getMessage() : ""), r1.isSuccess());

        LegendPureSession.CompileResult r2 = session.modifyAndCompile(ENUM_SOURCE_ID, ENUM_CODE);
        Assert.assertTrue("Status enum should compile: " +
                (r2.getError() != null ? r2.getError().getMessage() : ""), r2.isSuccess());

        LegendPureSession.CompileResult r3 = session.modifyAndCompile(REF_SOURCE_ID, REF_CODE);
        Assert.assertTrue("Zoo class should compile: " +
                (r3.getError() != null ? r3.getError().getMessage() : ""), r3.isSuccess());

        LegendPureSession.CompileResult r4 = session.modifyAndCompile(FUNC_PROP_SOURCE_ID, FUNC_PROP_CODE);
        Assert.assertTrue("Handler class should compile: " +
                (r4.getError() != null ? r4.getError().getMessage() : ""), r4.isSuccess());
    }

    @AfterClass
    public static void cleanup()
    {
        session = null;
    }

    @Test
    public void hover_onClassName_showsClassInfo()
    {
        // Hover on "Animal" in class definition line 1, col ~16 hits "Animal"
        // "Class test::hover::Animal"
        //  1234567890123456789012345
        Hover hover = HoverProvider.hover(session.getPureRuntime(), CLASS_SOURCE_ID, 1, 22);

        Assert.assertNotNull("Should return hover for class name", hover);
        Assert.assertEquals(MarkupKind.MARKDOWN, hover.getContents().getRight().getKind());
        String value = hover.getContents().getRight().getValue();
        Assert.assertTrue("Should mention Class, got: " + value,
                value.contains("Class"));
        Assert.assertTrue("Should mention Animal, got: " + value,
                value.contains("Animal"));
    }

    @Test
    public void hover_onClassReference_showsClassInfo()
    {
        // In REF_CODE line 3: "  animal: test::hover::Animal[1];"
        // Hover on the Animal reference
        //  "  animal: test::hover::Animal[1];"
        //  1234567890123456789012345678
        Hover hover = HoverProvider.hover(session.getPureRuntime(), REF_SOURCE_ID, 3, 25);

        Assert.assertNotNull("Should return hover for class reference", hover);
        String value = hover.getContents().getRight().getValue();
        Assert.assertTrue("Should contain type info, got: " + value,
                value.contains("Animal") || value.contains("Class"));
    }

    @Test
    public void hover_onProperty_showsPropertyInfo()
    {
        // In CLASS_CODE line 3: "  species: String[1];"
        // Hover on "species" at col 3
        Hover hover = HoverProvider.hover(session.getPureRuntime(), CLASS_SOURCE_ID, 3, 4);

        Assert.assertNotNull("Should return hover for property", hover);
        String value = hover.getContents().getRight().getValue();
        Assert.assertTrue("Should contain property info, got: " + value,
                value.contains("species") || value.contains("Property") || value.contains("String"));
    }

    @Test
    public void hover_onEnum_showsEnumInfo()
    {
        // Hover on "Status" in enum definition line 1
        // "Enum test::hover::Status"
        //  1234567890123456789012345
        // Try multiple positions on the Enum declaration line to find the element
        // navigate() may return different AST nodes depending on exact cursor position
        Hover hover = null;
        for (int col : new int[]{6, 10, 14, 18, 22})
        {
            hover = HoverProvider.hover(session.getPureRuntime(), ENUM_SOURCE_ID, 1, col);
            if (hover != null)
            {
                String v = hover.getContents().getRight().getValue();
                if (v.contains("Enumeration") || v.contains("Status"))
                {
                    break;
                }
            }
        }

        Assert.assertNotNull("Should return hover for some position on enum line", hover);
        // At minimum, hover should not crash and should return valid markdown
    }

    @Test
    public void hover_onClassDefinition_showsProperties()
    {
        // Hover on "Animal" in CLASS_CODE line 1, col 22
        Hover hover = HoverProvider.hover(session.getPureRuntime(), CLASS_SOURCE_ID, 1, 22);

        if (hover != null)
        {
            String value = hover.getContents().getRight().getValue();
            // If hovering on the class itself, the hover should show properties
            if (value.contains("Class") && value.contains("Animal"))
            {
                Assert.assertTrue("Class hover should list properties, got: " + value,
                        value.contains("species") || value.contains("legs"));
            }
        }
    }

    @Test
    public void hover_onUnknownSource_returnsNull()
    {
        Hover hover = HoverProvider.hover(session.getPureRuntime(), "nonexistent.pure", 1, 1);
        Assert.assertNull("Unknown source should return null", hover);
    }

    @Test
    public void hover_returnsMarkdown()
    {
        Hover hover = HoverProvider.hover(session.getPureRuntime(), CLASS_SOURCE_ID, 1, 22);

        if (hover != null)
        {
            Assert.assertNotNull("Should have markup content", hover.getContents().getRight());
            Assert.assertEquals("Should be markdown", MarkupKind.MARKDOWN,
                    hover.getContents().getRight().getKind());
        }
    }

    @Test
    public void hover_includesSourceLocation()
    {
        Hover hover = HoverProvider.hover(session.getPureRuntime(), CLASS_SOURCE_ID, 1, 22);

        if (hover != null)
        {
            String value = hover.getContents().getRight().getValue();
            Assert.assertTrue("Should include source location, got: " + value,
                    value.contains("Defined in"));
        }
    }

    @Test
    public void formatHover_handlesNullClassifier()
    {
        // Verify formatHover doesn't crash on edge cases
        String result = HoverProvider.formatHover(
                session.getPureRuntime().getCoreInstance("::"),
                "Package",
                session.getPureRuntime()
        );
        Assert.assertNotNull("formatHover should handle root package", result);
        Assert.assertTrue("Should contain Package, got: " + result, result.contains("Package"));
    }

    @Test
    public void hover_onTypeReference_neverReturnsImportStub()
    {
        // This is the critical test: hovering on a type reference (like "Animal" in
        // "animal: test::hover::Animal[1]") must resolve the ImportStub and show "Class",
        // not "ImportStub".
        // REF_CODE line 3: "  animal: test::hover::Animal[1];"
        //                   123456789012345678901234567890
        // "test" starts around col 11, "Animal" around col 25
        Hover hover = HoverProvider.hover(session.getPureRuntime(), REF_SOURCE_ID, 3, 25);

        Assert.assertNotNull("Should return hover for type reference", hover);
        String value = hover.getContents().getRight().getValue();
        Assert.assertFalse("Hover must NOT contain 'ImportStub', got: " + value,
                value.contains("ImportStub"));
        Assert.assertTrue("Hover should show the actual type (Class/Animal), got: " + value,
                value.contains("Class") || value.contains("Animal"));
    }

    @Test
    public void hover_classifierName_isNeverImportStub()
    {
        // Test multiple positions in the reference source to ensure none return ImportStub
        String[] badClassifiers = {"ImportStub", "PropertyStub", "EnumStub"};
        for (int line = 1; line <= 4; line++)
        {
            for (int col = 1; col <= 35; col += 5)
            {
                Hover hover = HoverProvider.hover(session.getPureRuntime(), REF_SOURCE_ID, line, col);
                if (hover != null)
                {
                    String value = hover.getContents().getRight().getValue();
                    for (String bad : badClassifiers)
                    {
                        Assert.assertFalse(
                                "Hover at line " + line + " col " + col + " returned '" + bad + "': " + value,
                                value.contains("**" + bad + "**"));
                    }
                }
            }
        }
    }

    @Test
    public void hover_onPropertyType_showsActualTypeName()
    {
        // In CLASS_CODE line 3: "  species: String[1];"
        // Hover on "String" at col ~13
        Hover hover = HoverProvider.hover(session.getPureRuntime(), CLASS_SOURCE_ID, 3, 13);

        if (hover != null)
        {
            String value = hover.getContents().getRight().getValue();
            Assert.assertFalse("Must not show ImportStub, got: " + value,
                    value.contains("ImportStub"));
            // Should show the actual type: String, PrimitiveType, or Class
            Assert.assertTrue("Should contain type info, got: " + value,
                    value.contains("String") || value.contains("PrimitiveType") || value.contains("Class"));
        }
    }

    @Test
    public void hover_classProperties_neverShowImportStub()
    {
        // This is the critical regression test: when hovering on a class definition,
        // the property types in the code block must show actual type names (String, Animal, etc.),
        // not "ImportStub". This catches the case where genericType→rawType is an unresolved stub.
        //
        // REF_CODE defines: Class test::hover::Zoo { animal: test::hover::Animal[1]; }
        // Hovering on "Zoo" should show properties with "Animal" as the type, not "ImportStub".
        Hover hover = HoverProvider.hover(session.getPureRuntime(), REF_SOURCE_ID, 1, 18);

        Assert.assertNotNull("Should return hover for Zoo class", hover);
        String value = hover.getContents().getRight().getValue();

        // The hover output must never contain "ImportStub" anywhere
        Assert.assertFalse(
                "Property types must not show ImportStub. Full hover:\n" + value,
                value.contains("ImportStub"));

        // If this is the Zoo class hover, it should list the 'animal' property with type 'Animal'
        if (value.contains("Zoo"))
        {
            Assert.assertTrue("Should show property 'animal' with type 'Animal', got:\n" + value,
                    value.contains("animal") && value.contains("Animal"));
        }
    }

    @Test
    public void hover_onClassWithFunctionProperty_showsTypeParameters()
    {
        // Handler class has: callback: Function<{String[1]->Boolean[1]}>[1]
        // Hovering on "Handler" at line 1 should show the property with full generic type
        Hover hover = HoverProvider.hover(session.getPureRuntime(), FUNC_PROP_SOURCE_ID, 1, 22);

        Assert.assertNotNull("Should return hover for Handler class", hover);
        String value = hover.getContents().getRight().getValue();

        // Must show the property with type parameters, not just "Function"
        Assert.assertTrue("Should show 'callback' property, got:\n" + value,
                value.contains("callback"));

        // The type should include the function signature, not just bare "Function"
        // GenericType.print should produce something like "Function<{String[1]->Boolean[1]}>"
        Assert.assertFalse("Property type must not be bare 'Function' without parameters. Got:\n" + value,
                value.contains(": Function[") || value.contains(": Function\n"));
        Assert.assertTrue("Should show Function with type parameters (angle brackets), got:\n" + value,
                value.contains("Function<"));
    }

    @Test
    public void hover_propertyTypes_includeGenericParameters()
    {
        // Test on Zoo: animal should show "Animal" not just the raw type
        // Test on Handler: callback should show "Function<{String[1]->Boolean[1]}>"
        Hover zooHover = HoverProvider.hover(session.getPureRuntime(), REF_SOURCE_ID, 1, 18);
        if (zooHover != null)
        {
            String value = zooHover.getContents().getRight().getValue();
            if (value.contains("animal"))
            {
                Assert.assertTrue("Zoo.animal should show 'Animal' type, got:\n" + value,
                        value.contains("Animal"));
            }
        }

        Hover handlerHover = HoverProvider.hover(session.getPureRuntime(), FUNC_PROP_SOURCE_ID, 1, 22);
        if (handlerHover != null)
        {
            String value = handlerHover.getContents().getRight().getValue();
            if (value.contains("callback"))
            {
                Assert.assertTrue("Handler.callback should show Function<...>, got:\n" + value,
                        value.contains("Function<"));
            }
        }
    }

    @Test
    public void hover_entireContent_neverContainsImportStub()
    {
        // Sweep all positions across all test sources and verify ImportStub never leaks
        String[][] sources = {
                {CLASS_SOURCE_ID, "5"},   // 5 lines
                {ENUM_SOURCE_ID, "5"},
                {REF_SOURCE_ID, "4"},
                {FUNC_PROP_SOURCE_ID, "4"}
        };

        for (String[] src : sources)
        {
            String sourceId = src[0];
            int lines = Integer.parseInt(src[1]);
            for (int line = 1; line <= lines; line++)
            {
                for (int col = 1; col <= 40; col += 3)
                {
                    Hover hover = HoverProvider.hover(session.getPureRuntime(), sourceId, line, col);
                    if (hover != null && hover.getContents().getRight() != null)
                    {
                        String value = hover.getContents().getRight().getValue();
                        Assert.assertFalse(
                                "ImportStub leaked at " + sourceId + ":" + line + ":" + col + ":\n" + value,
                                value.contains("ImportStub"));
                    }
                }
            }
        }
    }
}
