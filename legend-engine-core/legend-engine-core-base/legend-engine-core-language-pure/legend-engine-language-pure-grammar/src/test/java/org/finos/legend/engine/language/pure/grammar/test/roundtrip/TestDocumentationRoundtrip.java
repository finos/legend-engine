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

package org.finos.legend.engine.language.pure.grammar.test.roundtrip;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.language.pure.grammar.test.TestGrammarRoundtrip;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposer;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.api.grammar.RenderStyle;
import org.junit.Assert;
import org.junit.Test;

/**
 * The protocol records whether a doc value was authored as a `'''...'''` documentation block ({@code multiLine} on
 * the tagged value's {@link org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.primitive.CString}
 * value), and the composer renders whichever form the value carries: a block stays a block, an ordinary tagged value
 * stays a tagged value. The round trips here go through JSON, so they also pin that the flag survives the wire.
 * <p>
 * Documentation has no escape mechanism at all, so a flagged value the parser's canonicalization would not read back
 * unchanged still composes as an ordinary tagged value. Those triggers are only reachable through hand-built
 * protocol, so they are tested from JSON below.
 */
public class TestDocumentationRoundtrip extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{
    // ----------------------------------------------- BLOCK FORM -----------------------------------------------

    @Test
    public void testDocumentationOnClassAndProperties()
    {
        test("'''\n" +
                "A person in the system.\n" +
                "Identity is established by name.\n" +
                "'''\n" +
                "Class model::Person\n" +
                "{\n" +
                "  '''\n" +
                "  Given name.\n" +
                "  Not guaranteed unique.\n" +
                "  '''\n" +
                "  firstName: String[1];\n" +
                "  '''\n" +
                "  Full legal name.\n" +
                "  Derived, not stored.\n" +
                "  '''\n" +
                "  fullName() {$this.firstName}: String[1];\n" +
                "}\n");
    }

    @Test
    public void testSingleLineDocumentationStaysABlock()
    {
        test("'''\n" +
                "A person in the system.\n" +
                "'''\n" +
                "Class model::Person\n" +
                "{\n" +
                "  '''\n" +
                "  Given name.\n" +
                "  '''\n" +
                "  firstName: String[1];\n" +
                "}\n");
    }

    @Test
    public void testDocumentationAlongsideStereotypesAndOtherTaggedValues()
    {
        test("'''\n" +
                "Attached to the class.\n" +
                "Everything else is untouched.\n" +
                "'''\n" +
                "Class <<access.private>> {meta::pure::profiles::doc.todo = 'x'} model::A\n" +
                "{\n" +
                "}\n");
    }

    @Test
    public void testDocumentationOnEnumerationAndItsValues()
    {
        test("'''\n" +
                "A currency.\n" +
                "ISO 4217 codes.\n" +
                "'''\n" +
                "Enum model::Currency\n" +
                "{\n" +
                "  '''\n" +
                "  US dollar.\n" +
                "  The world's reserve currency.\n" +
                "  '''\n" +
                "  USD,\n" +
                "  EUR\n" +
                "}\n");
    }

    @Test
    public void testDocumentationOnAssociation()
    {
        test("Class model::B\n" +
                "{\n" +
                "}\n" +
                "\n" +
                "'''\n" +
                "Links a B to a B.\n" +
                "Both ends are mandatory.\n" +
                "'''\n" +
                "Association model::A\n" +
                "{\n" +
                "  left: model::B[1];\n" +
                "  right: model::B[1];\n" +
                "}\n");
    }

    @Test
    public void testDocumentationOnFunction()
    {
        test("'''\n" +
                "Answers everything.\n" +
                "Deterministically.\n" +
                "'''\n" +
                "function model::f(): Integer[1]\n" +
                "{\n" +
                "  42\n" +
                "}\n");
    }

    @Test
    public void testDocumentationWithABlankInteriorLine()
    {
        test("'''\n" +
                "A person in the system.\n" +
                "\n" +
                "Identity is established by legalName.\n" +
                "'''\n" +
                "Class model::Person\n" +
                "{\n" +
                "}\n");
    }

    @Test
    public void testIndentedContentSurvivesRelativeToTheClosingDelimiter()
    {
        test("'''\n" +
                "Example:\n" +
                "\n" +
                "    indented code\n" +
                "'''\n" +
                "Class model::A\n" +
                "{\n" +
                "}\n");
    }

    @Test
    public void testPrettyRenderingIsIdentical()
    {
        // class bodies indent with the static tab rather than the context's indentation, so the block is unaffected
        test("'''\n" +
                "A person in the system.\n" +
                "Identity is established by name.\n" +
                "'''\n" +
                "Class model::Person\n" +
                "{\n" +
                "  '''\n" +
                "  Given name.\n" +
                "  '''\n" +
                "  firstName: String[1];\n" +
                "}\n", RenderStyle.PRETTY);
    }

    @Test
    public void testContentIsNeverEscaped()
    {
        test("'''\n" +
                "\\d+ and \\* and C:\\temp and a quote's worth\n" +
                "on more than one line\n" +
                "'''\n" +
                "Class model::A\n" +
                "{\n" +
                "}\n");
    }

    // ------------------------------------ TAGGED-VALUE FORM KEEPS ITS FORM ------------------------------------

    @Test
    public void testSingleLineTaggedValueStaysATaggedValue()
    {
        test("Class {meta::pure::profiles::doc.doc = 'A person in the system.'} model::Person\n" +
                "{\n" +
                "  {meta::pure::profiles::doc.doc = 'Given name.'} firstName: String[1];\n" +
                "}\n");
    }

    @Test
    public void testMultiLineContentInATaggedValueStaysATaggedValue()
    {
        // the value was authored as an escaped single-quoted literal, so it composes back as one
        test("Class {meta::pure::profiles::doc.doc = 'line one\\nline two'} model::A\n" +
                "{\n" +
                "}\n");
    }

    @Test
    public void testTwoDocTagsStayTaggedValues()
    {
        test("Class {meta::pure::profiles::doc.doc = 'first\\nspans lines', meta::pure::profiles::doc.doc = 'second'} model::A\n" +
                "{\n" +
                "}\n");
    }

    // -------------------------------- BLOCK-LITERAL TAGGED VALUES ARE PROMOTED --------------------------------

    @Test
    public void testABlockLiteralTaggedValueComposesToDocumentation()
    {
        // `{doc.doc = '''...'''}` carries the multiLine flag, and documentation is that tagged value's sugar
        testFormat("'''\n" +
                        "A person in the system.\n" +
                        "Identity is established by name.\n" +
                        "'''\n" +
                        "Class model::Person\n" +
                        "{\n" +
                        "}\n",
                "Class {meta::pure::profiles::doc.doc = '''\n" +
                        "A person in the system.\n" +
                        "Identity is established by name.'''} model::Person\n" +
                        "{\n" +
                        "}\n");
    }

    @Test
    public void testABlockLiteralWithATrailingNewlineFallsBackToATaggedValue()
    {
        // a string literal's closing delimiter on its own line puts a trailing \n in the value, which a
        // documentation block cannot represent - promotion would change the value, so the escaped form is kept
        testFormat("Class {meta::pure::profiles::doc.doc = 'line one\\nline two\\n'} model::A\n" +
                        "{\n" +
                        "}\n",
                "Class {meta::pure::profiles::doc.doc = '''\n" +
                        "line one\n" +
                        "line two\n" +
                        "'''} model::A\n" +
                        "{\n" +
                        "}\n");
    }

    @Test
    public void testAnEmptyBlockLiteralTaggedValueFallsBackToATaggedValue()
    {
        // the canonicalized content is empty, which a documentation block cannot represent
        testFormat("Class {meta::pure::profiles::doc.doc = ''} model::A\n" +
                        "{\n" +
                        "}\n",
                "Class {meta::pure::profiles::doc.doc = '''\n" +
                        "'''} model::A\n" +
                        "{\n" +
                        "}\n");
    }

    // ------------------------------------- FALLBACK TO A TAGGED VALUE -------------------------------------
    // A parsed block always canonicalizes to representable content, so a flagged-but-unrenderable value can only
    // arrive as hand-built protocol; each guard trigger is driven from JSON.

    @Test
    public void testALegacyPlainStringValueComposesToATaggedValue()
    {
        Assert.assertEquals("Class {meta::pure::profiles::doc.doc = 'line one\\nline two'} model::A\n" +
                        "{\n" +
                        "}\n",
                composeClassWhoseDocValueIs("\"line one\\nline two\""));
    }

    @Test
    public void testAFlaggedValueComposesToDocumentation()
    {
        Assert.assertEquals("'''\n" +
                        "line one\n" +
                        "line two\n" +
                        "'''\n" +
                        "Class model::A\n" +
                        "{\n" +
                        "}\n",
                composeClassWhoseDocValueIs("{\"_type\":\"string\",\"multiLine\":true,\"value\":\"line one\\nline two\"}"));
    }

    @Test
    public void testAnObjectValueWithoutTheFlagComposesToATaggedValue()
    {
        Assert.assertEquals("Class {meta::pure::profiles::doc.doc = 'line one\\nline two'} model::A\n" +
                        "{\n" +
                        "}\n",
                composeClassWhoseDocValueIs("{\"_type\":\"string\",\"value\":\"line one\\nline two\"}"));
    }

    @Test
    public void testAValueContainingTheDelimiterFallsBack()
    {
        // there is no escape for ''' inside a block
        assertFallsBack("contains ''' delimiter\\non more than one line", "contains \\'\\'\\' delimiter\\non more than one line");
    }

    @Test
    public void testAValueContainingACarriageReturnFallsBack()
    {
        // the layout normalizes line terminators, so a \r would not survive
        assertFallsBack("a\\rb\\nc", "a\\rb\\nc");
    }

    @Test
    public void testAValueWithTrailingWhitespaceOnALineFallsBack()
    {
        // the layout strips trailing whitespace per line
        assertFallsBack("trailing space \\nb", "trailing space \\nb");
    }

    @Test
    public void testAValueWithALeadingBlankLineFallsBack()
    {
        assertFallsBack("\\ntext", "\\ntext");
    }

    @Test
    public void testAValueWithATrailingBlankLineFallsBack()
    {
        assertFallsBack("text\\n", "text\\n");
    }

    @Test
    public void testAnEmptyValueFallsBack()
    {
        assertFallsBack("", "");
    }

    @Test
    public void testAValueWithASectionHeaderLineFallsBack()
    {
        // the ###Section split runs before any grammar and is not string-aware, so a line starting with ### inside a
        // block would re-split the file
        assertFallsBack("text\\n###Pure\\nmore", "text\\n###Pure\\nmore");
    }

    /**
     * A doc value flagged multi-line whose content a documentation block cannot represent composes as an ordinary
     * tagged value. Both arguments are JSON-escaped; the expected one is additionally grammar-escaped.
     */
    private static void assertFallsBack(String jsonEscapedValue, String grammarEscapedValue)
    {
        Assert.assertEquals("Class {meta::pure::profiles::doc.doc = '" + grammarEscapedValue + "'} model::A\n" +
                        "{\n" +
                        "}\n",
                composeClassWhoseDocValueIs("{\"_type\":\"string\",\"multiLine\":true,\"value\":\"" + jsonEscapedValue + "\"}"));
    }

    private static String composeClassWhoseDocValueIs(String valueJson)
    {
        String json = "{\"_type\":\"data\",\"elements\":[{\"_type\":\"class\",\"name\":\"A\",\"package\":\"model\"," +
                "\"taggedValues\":[{\"tag\":{\"profile\":\"meta::pure::profiles::doc\",\"value\":\"doc\"},\"value\":" + valueJson + "}]}]}";
        try
        {
            ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
            PureModelContextData modelData = objectMapper.readValue(json, PureModelContextData.class);
            return PureGrammarComposer.newInstance(PureGrammarComposerContext.Builder.newInstance().build()).renderPureModelContextData(modelData);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
