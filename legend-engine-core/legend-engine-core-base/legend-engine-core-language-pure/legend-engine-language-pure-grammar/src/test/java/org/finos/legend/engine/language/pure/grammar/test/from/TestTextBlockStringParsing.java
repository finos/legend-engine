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

package org.finos.legend.engine.language.pure.grammar.test.from;

import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for the triple-single-quote (Java text-block style) multi-line string value extraction in
 * {@link PureGrammarParserUtility#processTextBlock}: normalize line terminators to '\n', drop the opening-delimiter
 * line, strip the minimum common leading whitespace, strip trailing whitespace per line, then process escapes.
 * <p>
 * These semantics are kept identical to legend-pure's {@code processMultilineString} so both grammars accept the
 * same source and produce the same value - see docs/engineering/architecture/type-system.md.
 */
public class TestTextBlockStringParsing
{
    private static String value(String grammarLiteral)
    {
        return PureGrammarParserUtility.fromGrammarString(grammarLiteral, true);
    }

    @Test
    public void testDetectsTextBlock()
    {
        Assert.assertTrue(PureGrammarParserUtility.isTextBlock("'''\n'''"));
        Assert.assertTrue(PureGrammarParserUtility.isTextBlock("'''\n  abc\n  '''"));
        // optional spaces/tabs, and a CRLF terminator, may sit between the delimiter and the newline
        Assert.assertTrue(PureGrammarParserUtility.isTextBlock("'''  \n  abc\n  '''"));
        Assert.assertTrue(PureGrammarParserUtility.isTextBlock("'''\r\n  abc\r\n  '''"));
        // the opening delimiter must be followed by a line terminator
        Assert.assertFalse(PureGrammarParserUtility.isTextBlock("'''abc'''"));
        Assert.assertFalse(PureGrammarParserUtility.isTextBlock("''''''"));
        Assert.assertFalse(PureGrammarParserUtility.isTextBlock("'abc'"));
        // a regular empty string '' must not be mistaken for the start of a text block
        Assert.assertFalse(PureGrammarParserUtility.isTextBlock("''"));
    }

    @Test
    public void testTrailingNewlineBlock()
    {
        // closing delimiter on its own line -> value ends with a newline
        Assert.assertEquals("Hello\nWorld\n", value("'''\n  Hello\n  World\n  '''"));
    }

    @Test
    public void testNoTrailingNewlineBlock()
    {
        // closing delimiter on the last content line -> no trailing newline
        Assert.assertEquals("Hello\nWorld", value("'''\n  Hello\n  World'''"));
    }

    @Test
    public void testRelativeIndentationPreserved()
    {
        Assert.assertEquals("line1\n  line2\n", value("'''\n    line1\n      line2\n    '''"));
    }

    @Test
    public void testBlankLineInMiddlePreserved()
    {
        Assert.assertEquals("a\n\nb\n", value("'''\n  a\n\n  b\n  '''"));
    }

    @Test
    public void testEscapesProcessed()
    {
        // \t becomes a tab, \' becomes a quote, \\ becomes a single backslash
        Assert.assertEquals("a\tb\n", value("'''\n  a\\tb\n  '''"));
        Assert.assertEquals("it's\n", value("'''\n  it\\'s\n  '''"));
        Assert.assertEquals("a\\b\n", value("'''\n  a\\\\b\n  '''"));
    }

    @Test
    public void testCarriageReturnNormalized()
    {
        Assert.assertEquals("a\n", value("'''\r\n  a\r\n  '''"));
    }

    @Test
    public void testEmptyTextBlock()
    {
        // an empty block still needs the terminator after the opening delimiter
        Assert.assertEquals("", value("'''\n'''"));
    }

    @Test
    public void testTrailingWhitespaceStrippedPerLine()
    {
        // incidental trailing whitespace is removed from each line (Java text-block semantics)
        Assert.assertEquals("a\nb\n", value("'''\n  a   \n  b\t\n  '''"));
    }

    @Test
    public void testWhitespaceOnlyLineBecomesEmpty()
    {
        Assert.assertEquals("a\n\nb\n", value("'''\n  a\n     \n  b\n  '''"));
    }

    @Test
    public void testEmbeddedSingleAndDoubleQuotes()
    {
        // lone and paired single quotes (not forming ''') and double quotes are content
        Assert.assertEquals("a 'b' \"c\" d\n", value("'''\n  a 'b' \"c\" d\n  '''"));
    }
}
