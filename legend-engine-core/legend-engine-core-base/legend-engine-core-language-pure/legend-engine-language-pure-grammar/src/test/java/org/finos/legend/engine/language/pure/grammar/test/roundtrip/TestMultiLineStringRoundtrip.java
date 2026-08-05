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
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.language.pure.grammar.test.GrammarParseTestUtils;
import org.finos.legend.engine.language.pure.grammar.test.TestGrammarRoundtrip;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposer;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.m3.function.Function;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.primitive.CString;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.api.grammar.RenderStyle;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.junit.Assert;
import org.junit.Test;

/**
 * Round-trip tests for triple-quote (`'''`) multi-line strings. A string value is re-emitted as a `'''` block only when
 * it was authored as one - the parser records that on the `multiLine` flag of `CString`, so a literal
 * nobody wrote as a block never reformats into one. The flag is a protocol field rather than source information, so the
 * decision survives source info being dropped.
 * <p>
 * The round-trip is value-equivalent: the composer indents the block and the parser strips that same indentation back
 * off. The "visual" cases live as `.pure` resource fixtures under {@code multiLineString/} so the exact spacing is
 * readable; each fixture is a round-trip fixed point (parse -> compose returns it verbatim) and is additionally driven
 * through {@link TestGrammarRoundtrip.TestGrammarRoundtripTestSuite#test(String)}, which round-trips it through the
 * JSON protocol on the way.
 */
public class TestMultiLineStringRoundtrip extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{
    private static final String RESOURCE_DIR = "org/finos/legend/engine/language/pure/grammar/test/roundtrip/multiLineString/";
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    private static PureModelContextData parse(String code, boolean withSourceInfo)
    {
        return PureGrammarParser.newInstance().parseModel(code, "", 0, 0, withSourceInfo);
    }

    private static String compose(PureModelContextData data)
    {
        return PureGrammarComposer.newInstance(PureGrammarComposerContext.Builder.newInstance().withRenderStyle(RenderStyle.PRETTY).build()).renderPureModelContextData(data);
    }

    private static CString firstString(PureModelContextData data)
    {
        for (PackageableElement element : data.getElements())
        {
            if (element instanceof Function)
            {
                for (ValueSpecification vs : ((Function) element).body)
                {
                    if (vs instanceof CString)
                    {
                        return (CString) vs;
                    }
                }
            }
        }
        throw new IllegalStateException("no CString found in model");
    }

    private static String firstStringValue(PureModelContextData data)
    {
        return firstString(data).value;
    }

    private static PureModelContextData throughJson(PureModelContextData data)
    {
        try
        {
            return objectMapper.readValue(objectMapper.writeValueAsString(data), PureModelContextData.class);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private static String functionWithBody(String body)
    {
        return "function test::f(): String[1]\n{\n" + body + "\n}\n";
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Visual round-trip fixtures: read the .pure file, parse it, and assert the composer reproduces it verbatim.
    // ---------------------------------------------------------------------------------------------------------------

    private String assertResourceRoundTrips(String fileName)
    {
        String code = GrammarParseTestUtils.getResource(RESOURCE_DIR + fileName, TestMultiLineStringRoundtrip.class);
        Assert.assertEquals(code, compose(parse(code, true)));
        // also drive it through the JSON protocol, which is how Studio round-trips a model
        Assert.assertEquals(code, compose(throughJson(parse(code, false))));
        return code;
    }

    @Test
    public void testMultiLineStringInFunctionBody()
    {
        assertResourceRoundTrips("functionBody.pure");
    }

    @Test
    public void testMultiLineStringInLetStatement()
    {
        assertResourceRoundTrips("letStatement.pure");
    }

    @Test
    public void testIndentedBlockContentKeepsItsOwnIndentation()
    {
        // the discriminating fixture: the content lines carry indentation of their own, on top of the block's. It is
        // the closing delimiter - alone on its line, at the block's indent - that pins the common indentation the
        // parser strips, so the content's own two spaces survive. Content flush with the block would pass either way.
        String code = assertResourceRoundTrips("indentedBlockContent.pure");
        Assert.assertEquals("  Hello\n  World\n", firstStringValue(parse(code, true)));
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Behavioural rules.
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    public void testBlockDecisionIsIndependentOfSourceInfo()
    {
        // the rule reads a protocol flag, not source information, so rendering is identical with and without it
        String code = functionWithBody("  '''\n  Hello\n  World\n  '''");
        String withSource = compose(parse(code, true));
        String withoutSource = compose(parse(code, false));
        Assert.assertTrue("expected a ''' block, got:\n" + withSource, withSource.contains("'''"));
        Assert.assertEquals("block rendering must not depend on source info", withSource, withoutSource);
    }

    @Test
    public void testRoundTripIsValueEquivalent()
    {
        // the composed indentation need not match the original (equivalence, not byte-identity): the composer indents
        // the block and the parser strips that same indentation back off, so the value is preserved and composing is
        // a fixed point
        String code = functionWithBody("  '''\n      Hello\n      World\n  '''");
        PureModelContextData data1 = parse(code, true);
        String composed1 = compose(data1);
        PureModelContextData data2 = parse(composed1, true);
        Assert.assertEquals(firstStringValue(data1), firstStringValue(data2));
        Assert.assertEquals(composed1, compose(data2));
    }

    @Test
    public void testSingleLineStringWithEscapedNewlineStaysSingleLine()
    {
        // value contains '\n' but does not end with one -> must NOT become a block
        String code = functionWithBody("  'Hello\\nWorld'");
        PureModelContextData data = parse(code, true);
        Assert.assertEquals("Hello\nWorld", firstStringValue(data));
        String composed = compose(data);
        Assert.assertFalse("single-line literal must stay single-line, got:\n" + composed, composed.contains("'''"));
        Assert.assertTrue(composed.contains("'Hello\\nWorld'"));
    }

    @Test
    public void testSingleLineLiteralEndingInNewlineStaysSingleLine()
    {
        // the point of keying off the flag rather than the value: this value would be block-representable, but nobody
        // authored it as a block, so it must not be reformatted into one
        String code = functionWithBody("  'Hello\\nWorld\\n'");
        PureModelContextData data = parse(code, true);
        CString cString = firstString(data);
        Assert.assertEquals("Hello\nWorld\n", cString.value);
        Assert.assertFalse("a single-line literal must not be flagged multi-line", cString.multiLine);
        String composed = compose(data);
        Assert.assertFalse("must not reformat into a block, got:\n" + composed, composed.contains("'''"));
        Assert.assertEquals(composed, compose(parse(composed, true)));
    }

    @Test
    public void testValueWithoutTrailingNewlineStaysSingleLine()
    {
        // a block closed on its last content line has no trailing newline in the value; it stays a block, with the
        // closing delimiter re-emitted on the content line
        String code = functionWithBody("  '''\n  Hello\n  World'''");
        PureModelContextData data = parse(code, true);
        Assert.assertEquals("Hello\nWorld", firstStringValue(data));
        Assert.assertTrue("the literal was authored as a block", firstString(data).multiLine);
        String composed = compose(data);
        Assert.assertTrue("expected a ''' block, got:\n" + composed, composed.contains("'''"));
        Assert.assertEquals("Hello\nWorld", firstStringValue(parse(composed, true)));
        Assert.assertEquals(composed, compose(parse(composed, true)));
    }

    /**
     * Compose a function whose single string literal carries {@code value} with the multi-line flag forced on. This is
     * the contract under test: the composer obeys the flag without inspecting the value, so {@code renderTextBlock}
     * has to encode <i>any</i> value such that the parser reads it back unchanged.
     */
    private static String composeWithFlagForcedOn(String value)
    {
        PureModelContextData data = parse(functionWithBody("  'x'"), false);
        CString cString = firstString(data);
        cString.value = value;
        cString.multiLine = true;
        return compose(data);
    }

    @Test
    public void testFlagIsObeyedForAnyValue()
    {
        String[] values = {
                "Hello\nWorld\n",
                "  a\n  b\n",              // the value's own indentation
                "Hello\nWorld",            // no trailing newline - closing delimiter shares the last line
                "  a\n  b",                // own indentation and no trailing newline
                "a\t\n",                   // trailing tab, which the parser strips per line
                "a   \nb\n",               // trailing spaces
                "a\r\n",                   // carriage return, which the parser normalizes
                "'''\n",                   // would terminate the block if written raw
                "a\n\nb\n",                // blank line
                "a\n   \nb\n",             // whitespace-only line, which the parser would blank out
                "a\\b\n",                  // literal backslash
                "it's\n",
                "",
        };
        for (String value : values)
        {
            String composed = composeWithFlagForcedOn(value);
            String display = value.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
            Assert.assertTrue("expected a ''' block for \"" + display + "\", got:\n" + composed, composed.contains("'''"));
            Assert.assertEquals("value must survive the round-trip for \"" + display + "\"", value, firstStringValue(parse(composed, true)));
            // and composing is a fixed point
            Assert.assertEquals("composing must be stable for \"" + display + "\"", composed, compose(parse(composed, true)));
        }
    }

    @Test
    public void testCarriageReturnStaysABlockAndIsEscaped()
    {
        // a raw CR would be normalized to '\n' on reparse, so the composer emits it as an escape and the block stands
        String code = functionWithBody("  '''\n  a\\rb\n  '''");
        PureModelContextData data = parse(code, true);
        Assert.assertEquals("a\rb\n", firstStringValue(data));
        String composed = compose(data);
        Assert.assertTrue("expected a ''' block, got:\n" + composed, composed.contains("'''"));
        Assert.assertFalse("the CR must not be emitted raw", composed.contains("a\rb"));
        Assert.assertEquals("a\rb\n", firstStringValue(parse(composed, true)));
    }

    @Test
    public void testSingleLineLiteralWithTrailingWhitespaceStaysSingleLine()
    {
        // nothing to do with block-representability: this literal simply was not authored as a block, so the flag is
        // off and it renders single-line
        String code = functionWithBody("  'a   \\nb\\n'");
        PureModelContextData data = parse(code, true);
        Assert.assertEquals("a   \nb\n", firstStringValue(data));
        Assert.assertFalse(firstString(data).multiLine);
        String composed = compose(data);
        Assert.assertFalse("expected single-line, got:\n" + composed, composed.contains("'''"));
        Assert.assertEquals("a   \nb\n", firstStringValue(parse(composed, true)));
    }

    @Test
    public void testBlockWithEmbeddedQuotesAndBackslashRoundTrips()
    {
        String code = functionWithBody("  '''\n  a 'b' \"c\" \\\\ d\n  '''");
        PureModelContextData data1 = parse(code, true);
        Assert.assertEquals("a 'b' \"c\" \\ d\n", firstStringValue(data1));
        String composed1 = compose(data1);
        Assert.assertTrue(composed1.contains("'''"));
        PureModelContextData data2 = parse(composed1, true);
        Assert.assertEquals("a 'b' \"c\" \\ d\n", firstStringValue(data2));
        Assert.assertEquals(composed1, compose(data2));
    }

    // ---------------------------------------------------------------------------------------------------------------
    // The flag is a protocol field, so it has to survive JSON - Studio round-trips models as grammar -> JSON -> grammar.
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    public void testMultiLineFlagOnStringLiteralSurvivesJson()
    {
        PureModelContextData data = throughJson(parse(functionWithBody("  '''\n  Hello\n  World\n  '''"), false));
        Assert.assertTrue("multiLine must survive the JSON protocol", firstString(data).multiLine);
        Assert.assertTrue(compose(data).contains("'''"));
    }

    @Test
    public void testStandardRenderStyleIsUnindentedButValuePreserving()
    {
        // STANDARD style tracks no indentation inside a function body, so the block renders flush-left. That is
        // cosmetic only: the parser's minimum-common-indentation is then zero, so the value still round-trips.
        String code = functionWithBody("  '''\n    Hello\n    World\n  '''");
        PureModelContextData data = parse(code, true);
        Assert.assertEquals("  Hello\n  World\n", firstStringValue(data));
        String standard = PureGrammarComposer.newInstance(PureGrammarComposerContext.Builder.newInstance().build()).renderPureModelContextData(data);
        Assert.assertTrue("expected a ''' block, got:\n" + standard, standard.contains("'''"));
        Assert.assertEquals("  Hello\n  World\n", firstStringValue(parse(standard, true)));
    }

    @Test
    public void testSingleLineLiteralDoesNotEmitTheFlagInJson()
    {
        try
        {
            String json = objectMapper.writeValueAsString(parse(functionWithBody("  'Hello'"), false));
            Assert.assertFalse("the flag must stay off the wire when unused, got:\n" + json, json.contains("multiLine"));
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Lexer.
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    public void testUnterminatedTextBlockIsAParserError()
    {
        // the TextBlock lexer rule is non-greedy; an unterminated block must fail rather than swallow the rest of the
        // source
        try
        {
            parse(functionWithBody("  '''\n  Hello\n  World"), true);
            Assert.fail("expected an unterminated ''' block to be rejected");
        }
        catch (EngineException e)
        {
            // expected
        }
    }
}
