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

import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.language.pure.grammar.test.TestGrammarRoundtrip;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposer;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.shared.core.api.grammar.RenderStyle;
import org.junit.Assert;
import org.junit.Test;

/**
 * The composer promotes a `meta::pure::profiles::doc` `doc` tagged value back to the `'''...'''` documentation block
 * it is sugar for when the value spans multiple lines - a multi-line value has no readable single-line form. A
 * single-line value keeps its tagged-value formatting unless the context's `alwaysRenderDocumentation` flag opts in,
 * so multi-line blocks are fixed points here and single-line blocks compose to tagged values.
 * <p>
 * Documentation has no escape mechanism at all, so a value the parser's canonicalization would not read back
 * unchanged stays an ordinary tagged value regardless of the flag. Each of those triggers has its own test below.
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
                "  Not guaranteed unique.\n" +
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

    // ------------------------------------ SINGLE-LINE VALUES KEEP THEIR FORM ------------------------------------

    @Test
    public void testSingleLineTaggedValueStaysATaggedValueByDefault()
    {
        test("Class {meta::pure::profiles::doc.doc = 'A person in the system.'} model::Person\n" +
                "{\n" +
                "  {meta::pure::profiles::doc.doc = 'Given name.'} firstName: String[1];\n" +
                "}\n");
    }

    @Test
    public void testSingleLineDocumentationComposesToATaggedValueByDefault()
    {
        testFormat("Class {meta::pure::profiles::doc.doc = 'A person in the system.'} model::Person\n" +
                        "{\n" +
                        "}\n",
                "'''\n" +
                        "A person in the system.\n" +
                        "'''\n" +
                        "Class model::Person\n" +
                        "{\n" +
                        "}\n");
    }

    @Test
    public void testTheFlagPromotesSingleLineValues()
    {
        Assert.assertEquals("'''\n" +
                        "A person in the system.\n" +
                        "'''\n" +
                        "Class model::Person\n" +
                        "{\n" +
                        "  '''\n" +
                        "  Given name.\n" +
                        "  '''\n" +
                        "  firstName: String[1];\n" +
                        "}\n",
                composeWithAlwaysRenderDocumentation("Class {meta::pure::profiles::doc.doc = 'A person in the system.'} model::Person\n" +
                        "{\n" +
                        "  {meta::pure::profiles::doc.doc = 'Given name.'} firstName: String[1];\n" +
                        "}\n"));
    }

    @Test
    public void testTheFlagDoesNotPromoteAnUnrenderableValue()
    {
        Assert.assertEquals("Class {meta::pure::profiles::doc.doc = ''} model::A\n" +
                        "{\n" +
                        "}\n",
                composeWithAlwaysRenderDocumentation("Class {meta::pure::profiles::doc.doc = ''} model::A\n" +
                        "{\n" +
                        "}\n"));
    }

    private static String composeWithAlwaysRenderDocumentation(String code)
    {
        return PureGrammarComposer.newInstance(PureGrammarComposerContext.Builder.newInstance().withAlwaysRenderDocumentation().build())
                .renderPureModelContextData(PureGrammarParser.newInstance().parseModel(code));
    }

    // ----------------------------------------------- FALLBACK TO A TAGGED VALUE -----------------------------------

    @Test
    public void testValueContainingTheDelimiterStaysATaggedValue()
    {
        // there is no escape for ''' inside a block
        test("Class {meta::pure::profiles::doc.doc = 'contains \\'\\'\\' delimiter\\non more than one line'} model::A\n" +
                "{\n" +
                "}\n");
    }

    @Test
    public void testValueContainingACarriageReturnStaysATaggedValue()
    {
        // the layout normalizes line terminators, so a \r would not survive
        test("Class {meta::pure::profiles::doc.doc = 'a\\rb\\nc'} model::A\n" +
                "{\n" +
                "}\n");
    }

    @Test
    public void testValueWithTrailingWhitespaceOnALineStaysATaggedValue()
    {
        // the layout strips trailing whitespace per line
        test("Class {meta::pure::profiles::doc.doc = 'trailing space \\nb'} model::A\n" +
                "{\n" +
                "}\n");
    }

    @Test
    public void testValueWithALeadingBlankLineStaysATaggedValue()
    {
        test("Class {meta::pure::profiles::doc.doc = '\\ntext'} model::A\n" +
                "{\n" +
                "}\n");
    }

    @Test
    public void testValueWithATrailingBlankLineStaysATaggedValue()
    {
        test("Class {meta::pure::profiles::doc.doc = 'text\\n'} model::A\n" +
                "{\n" +
                "}\n");
    }

    @Test
    public void testEmptyValueStaysATaggedValue()
    {
        test("Class {meta::pure::profiles::doc.doc = ''} model::A\n" +
                "{\n" +
                "}\n");
    }

    @Test
    public void testValueWithASectionHeaderLineStaysATaggedValue()
    {
        // the ###Section split runs before any grammar and is not string-aware, so a line starting with ### inside a
        // block would re-split the file
        test("Class {meta::pure::profiles::doc.doc = 'text\\n###Pure\\nmore'} model::A\n" +
                "{\n" +
                "}\n");
    }

    @Test
    public void testTwoDocTagsStayTaggedValues()
    {
        // two blocks in a row is a parse error, so neither is promoted
        test("Class {meta::pure::profiles::doc.doc = 'first\\nspans lines', meta::pure::profiles::doc.doc = 'second'} model::A\n" +
                "{\n" +
                "}\n");
    }
}
