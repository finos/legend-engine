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

package org.finos.legend.engine.language.pure.grammar.test.parser;

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.test.TestGrammarParser;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.m3.extension.TaggedValue;
import org.finos.legend.engine.protocol.pure.m3.function.Function;
import org.finos.legend.engine.protocol.pure.m3.relationship.Association;
import org.finos.legend.engine.protocol.pure.m3.type.Class;
import org.finos.legend.engine.protocol.pure.m3.type.Enumeration;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * A `'''...'''` literal preceding a declaration is sugar for the `meta::pure::profiles::doc` `doc` tagged value.
 * <p>
 * Documentation shares the Java-text-block layout of a multi-line string literal (covered by
 * {@code TestTextBlockStringParsing}) and differs from it in two respects: its content is literal, so nothing is
 * unescaped, and its leading and trailing blank lines are dropped. Kept identical to legend-pure's
 * {@code TestDocumentation} / {@code TestDocumentationCanonicalizer} so both grammars produce the same value.
 */
public class TestDocumentationParsing extends TestGrammarParser.TestGrammarParserTestSuite
{
    private static final String DOC_PROFILE_PATH = "meta::pure::profiles::doc";

    @Override
    public org.antlr.v4.runtime.Vocabulary getParserGrammarVocabulary()
    {
        return org.finos.legend.engine.language.pure.grammar.from.antlr4.domain.DomainParserGrammar.VOCABULARY;
    }

    @Override
    public String getParserGrammarIdentifierInclusionTestCode(List<String> keywords)
    {
        return null;
    }

    private static PackageableElement element(String code, String path)
    {
        PureModelContextData data = test(code);
        PackageableElement element = ListIterate.detect(data.getElements(), e -> path.equals(e.getPath()));
        Assert.assertNotNull("No element '" + path + "' in the parsed model", element);
        return element;
    }

    private static void assertDoc(String expectedValue, List<TaggedValue> taggedValues)
    {
        Assert.assertFalse("Element carries no tagged value", taggedValues.isEmpty());
        // documentation is prepended, so it is always the first tagged value
        TaggedValue documentation = taggedValues.get(0);
        Assert.assertEquals(DOC_PROFILE_PATH, documentation.tag.profile);
        Assert.assertEquals("doc", documentation.tag.value);
        Assert.assertEquals(expectedValue, documentation.value.value);
        Assert.assertTrue("Documentation records that it was authored multi-line", documentation.value.multiLine);
    }

    private static void assertClassDoc(String expectedValue, String code)
    {
        assertDoc(expectedValue, ((Class) element(code, "model::A")).taggedValues);
    }

    // ----------------------------------------------- ATTACHMENT -----------------------------------------------

    @Test
    public void testDocumentationOnClass()
    {
        assertClassDoc("A person in the system.",
                "'''\n" +
                        "A person in the system.\n" +
                        "'''\n" +
                        "Class model::A\n" +
                        "{\n" +
                        "}\n");
    }

    @Test
    public void testDocumentationIsSugarForTheDocTag()
    {
        Class sugared = (Class) element("'''\nDocumented.\n'''\nClass model::A\n{\n}\n", "model::A");
        Class explicit = (Class) element("Class {meta::pure::profiles::doc.doc = 'Documented.'} model::A\n{\n}\n", "model::A");
        Assert.assertEquals(1, sugared.taggedValues.size());
        Assert.assertEquals(explicit.taggedValues.get(0).tag.profile, sugared.taggedValues.get(0).tag.profile);
        Assert.assertEquals(explicit.taggedValues.get(0).tag.value, sugared.taggedValues.get(0).tag.value);
        Assert.assertEquals(explicit.taggedValues.get(0).value, sugared.taggedValues.get(0).value);
    }

    @Test
    public void testDocumentationOnProperties()
    {
        Class _class = (Class) element("Class model::A\n" +
                "{\n" +
                "  '''\n" +
                "  Given name.\n" +
                "  '''\n" +
                "  firstName: String[1];\n" +
                "  '''\n" +
                "  Full legal name.\n" +
                "  '''\n" +
                "  fullName() {$this.firstName}: String[1];\n" +
                "}\n", "model::A");
        assertDoc("Given name.", _class.properties.get(0).taggedValues);
        assertDoc("Full legal name.", _class.qualifiedProperties.get(0).taggedValues);
    }

    @Test
    public void testDocumentationOnEnumerationAndItsValues()
    {
        Enumeration enumeration = (Enumeration) element("'''\n" +
                "A currency.\n" +
                "'''\n" +
                "Enum model::A\n" +
                "{\n" +
                "  '''\n" +
                "  US dollar.\n" +
                "  '''\n" +
                "  USD,\n" +
                "  EUR\n" +
                "}\n", "model::A");
        assertDoc("A currency.", enumeration.taggedValues);
        assertDoc("US dollar.", enumeration.values.get(0).taggedValues);
        Assert.assertTrue(enumeration.values.get(1).taggedValues.isEmpty());
    }

    @Test
    public void testDocumentationOnAssociation()
    {
        Association association = (Association) element("Class model::B\n" +
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
                "}\n", "model::A");
        assertDoc("Links a B to a B.", association.taggedValues);
    }

    @Test
    public void testDocumentationOnFunction()
    {
        Function function = (Function) element("'''\n" +
                "Answers everything.\n" +
                "'''\n" +
                "function model::f(): Integer[1]\n" +
                "{\n" +
                "  42\n" +
                "}\n", "model::f__Integer_1_");
        assertDoc("Answers everything.", function.taggedValues);
    }

    @Test
    public void testDocumentationPrecedesStereotypesAndTaggedValues()
    {
        Class _class = (Class) element("'''\n" +
                "Attached to the class.\n" +
                "'''\n" +
                "Class <<access.private>> {meta::pure::profiles::doc.todo = 'x'} model::A\n" +
                "{\n" +
                "}\n", "model::A");
        assertDoc("Attached to the class.", _class.taggedValues);
        Assert.assertEquals(2, _class.taggedValues.size());
        Assert.assertEquals("todo", _class.taggedValues.get(1).tag.value);
        Assert.assertEquals(1, _class.stereotypes.size());
    }

    @Test
    public void testSourceInformationSpansTheWholeLiteral()
    {
        Class _class = (Class) element("'''\n" +
                "Documented.\n" +
                "'''\n" +
                "Class model::A\n" +
                "{\n" +
                "}\n", "model::A");
        // the literal spans lines 1-3, closing delimiter included - a token reports only the line it starts on, so
        // this is computed from the token text rather than from the shared start-column-plus-length helper
        Assert.assertEquals(1, _class.taggedValues.get(0).sourceInformation.startLine);
        Assert.assertEquals(1, _class.taggedValues.get(0).sourceInformation.startColumn);
        Assert.assertEquals(3, _class.taggedValues.get(0).sourceInformation.endLine);
        Assert.assertEquals(3, _class.taggedValues.get(0).sourceInformation.endColumn);
        // the element's own range starts at the documentation, as it does in legend-pure
        Assert.assertEquals(1, _class.sourceInformation.startLine);
    }

    @Test
    public void testInterveningCommentDoesNotDetachDocumentation()
    {
        assertClassDoc("Still attached.",
                "'''\n" +
                        "Still attached.\n" +
                        "'''\n" +
                        "// a note about the class\n" +
                        "Class model::A\n" +
                        "{\n" +
                        "}\n");
    }

    // ----------------------------------------------- CANONICALIZATION -----------------------------------------------

    @Test
    public void testIndentationIsStrippedAndInteriorBlankLinesKept()
    {
        assertClassDoc("A\n\nB",
                "'''\n" +
                        "  A\n" +
                        "\n" +
                        "  B\n" +
                        "  '''\n" +
                        "Class model::A\n" +
                        "{\n" +
                        "}\n");
    }

    @Test
    public void testClosingDelimiterSetsTheIndentationFloor()
    {
        // min indent is 2, set by the closing delimiter, so the code block keeps its relative indent
        assertClassDoc("Text\n\n    code",
                "'''\n" +
                        "  Text\n" +
                        "\n" +
                        "      code\n" +
                        "  '''\n" +
                        "Class model::A\n" +
                        "{\n" +
                        "}\n");
    }

    @Test
    public void testLeadingAndTrailingBlankLinesAreDropped()
    {
        // this is what makes documentation and the equivalent explicit doc.doc tagged value hold the same string
        assertClassDoc("Text",
                "'''\n" +
                        "\n" +
                        "Text\n" +
                        "\n" +
                        "'''\n" +
                        "Class model::A\n" +
                        "{\n" +
                        "}\n");
    }

    @Test
    public void testEmptyDocumentation()
    {
        assertClassDoc("",
                "'''\n" +
                        "'''\n" +
                        "Class model::A\n" +
                        "{\n" +
                        "}\n");
    }

    @Test
    public void testContentIsLiteralAndNeverUnescaped()
    {
        // the opposite of a string literal, and deliberate: unescaping prose would silently rewrite a regex, a
        // Markdown escape and a Windows path
        assertClassDoc("\\d+ and \\* and C:\\temp and \\n",
                "'''\n" +
                        "\\d+ and \\* and C:\\temp and \\n\n" +
                        "'''\n" +
                        "Class model::A\n" +
                        "{\n" +
                        "}\n");
    }

    @Test
    public void testMarkdownBulletsAreOrdinaryContent()
    {
        assertClassDoc("Options:\n* first\n* second",
                "'''\n" +
                        "Options:\n" +
                        "* first\n" +
                        "* second\n" +
                        "'''\n" +
                        "Class model::A\n" +
                        "{\n" +
                        "}\n");
    }

    // ----------------------------------------------- ERRORS -----------------------------------------------

    @Test
    public void testDocumentationConflictsWithAnExplicitDocTag()
    {
        test("'''\n" +
                        "From the documentation.\n" +
                        "'''\n" +
                        "Class {meta::pure::profiles::doc.doc = 'From the tagged value.'} model::A\n" +
                        "{\n" +
                        "}\n",
                "PARSER error at [4:8-63]: Element has both documentation and an explicit doc.doc tagged value. Use one.");
    }

    @Test
    public void testDocumentationConflictsWithAnImportRelativeDocTag()
    {
        // tag references are unresolved at parse time, so the profile is matched as written
        test("'''\n" +
                        "From the documentation.\n" +
                        "'''\n" +
                        "Class {doc.doc = 'From the tagged value.'} model::A\n" +
                        "{\n" +
                        "}\n",
                "PARSER error at [4:8-41]: Element has both documentation and an explicit doc.doc tagged value. Use one.");
    }

    @Test
    public void testADifferentProfileEndingInDocIsNotAConflict()
    {
        Class _class = (Class) element("'''\n" +
                "Documented.\n" +
                "'''\n" +
                "Class {my::pkg::doc.doc = 'Not the doc profile.'} model::A\n" +
                "{\n" +
                "}\n", "model::A");
        assertDoc("Documented.", _class.taggedValues);
        Assert.assertEquals(2, _class.taggedValues.size());
    }

    @Test
    public void testSingleQuotedStringIsNotDocumentation()
    {
        test("'not a block'\n" +
                        "Class model::A\n" +
                        "{\n" +
                        "}\n",
                "PARSER error at [1:1-13]: Documentation must be written as a multi-line ('''...''') literal");
    }

    // ----------------------------------------------- EXPRESSION POSITION -----------------------------------------------

    @Test
    public void testABlockInExpressionPositionIsAValue()
    {
        // the same literal inside a derived property's body is that property's return value, not documentation
        Class _class = (Class) element("Class model::A\n" +
                "{\n" +
                "  fullName() {'''\n" +
                "  Formatted name.\n" +
                "  '''}: String[1];\n" +
                "}\n", "model::A");
        Assert.assertTrue(_class.qualifiedProperties.get(0).taggedValues.isEmpty());
    }

    @Test
    public void testQuotedPropertyNameIsNotMistakenForDocumentation()
    {
        Class _class = (Class) element("Class model::A\n" +
                "{\n" +
                "  'a name': String[1];\n" +
                "}\n", "model::A");
        Assert.assertEquals("a name", _class.properties.get(0).name);
        Assert.assertTrue(_class.properties.get(0).taggedValues.isEmpty());
    }

    @Test
    public void testQuotedEnumValueIsNotMistakenForDocumentation()
    {
        Enumeration enumeration = (Enumeration) element("Enum model::A\n" +
                "{\n" +
                "  'a value',\n" +
                "  'another value'\n" +
                "}\n", "model::A");
        Assert.assertEquals("a value", enumeration.values.get(0).value);
        Assert.assertTrue(enumeration.values.get(0).taggedValues.isEmpty());
    }
}
