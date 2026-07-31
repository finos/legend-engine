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

import org.finos.legend.engine.language.pure.grammar.test.TestGrammarRoundtrip;
import org.finos.legend.engine.shared.core.api.grammar.RenderStyle;
import org.junit.Test;

/**
 * The composer promotes a `meta::pure::profiles::doc` `doc` tagged value back to the `'''...'''` documentation block
 * it is sugar for, so every test here is a fixed point: the block form is what a doc tagged value composes to.
 * <p>
 * Documentation has no escape mechanism at all, so a value the parser's canonicalization would not read back
 * unchanged stays an ordinary tagged value instead. Each of those triggers has its own test below - they are the
 * cases where the feature deliberately does not apply.
 */
public class TestDocumentationRoundtrip extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{
    // ----------------------------------------------- BLOCK FORM -----------------------------------------------

    @Test
    public void testDocumentationOnClassAndProperties()
    {
        test("'''\n" +
                "A person in the system.\n" +
                "'''\n" +
                "Class model::Person\n" +
                "{\n" +
                "  '''\n" +
                "  Given name. Not guaranteed unique.\n" +
                "  '''\n" +
                "  firstName: String[1];\n" +
                "  '''\n" +
                "  Full legal name.\n" +
                "  '''\n" +
                "  fullName() {$this.firstName}: String[1];\n" +
                "}\n");
    }

    @Test
    public void testDocumentationAlongsideStereotypesAndOtherTaggedValues()
    {
        test("'''\n" +
                "Attached to the class.\n" +
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
                "'''\n" +
                "Enum model::Currency\n" +
                "{\n" +
                "  '''\n" +
                "  US dollar.\n" +
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
                "'''\n" +
                "function model::f(): Integer[1]\n" +
                "{\n" +
                "  42\n" +
                "}\n");
    }

    @Test
    public void testMultiLineDocumentation()
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
                "'''\n" +
                "Class model::A\n" +
                "{\n" +
                "}\n");
    }

    // ----------------------------------------------- FALLBACK TO A TAGGED VALUE -----------------------------------

    @Test
    public void testValueContainingTheDelimiterStaysATaggedValue()
    {
        // there is no escape for ''' inside a block
        test("Class {meta::pure::profiles::doc.doc = 'contains \\'\\'\\' delimiter'} model::A\n" +
                "{\n" +
                "}\n");
    }

    @Test
    public void testValueContainingACarriageReturnStaysATaggedValue()
    {
        // the layout normalizes line terminators, so a \r would not survive
        test("Class {meta::pure::profiles::doc.doc = 'a\\rb'} model::A\n" +
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
        test("Class {meta::pure::profiles::doc.doc = 'first', meta::pure::profiles::doc.doc = 'second'} model::A\n" +
                "{\n" +
                "}\n");
    }
}
