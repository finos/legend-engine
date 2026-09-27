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

package org.finos.legend.engine.language.pure.grammar.from.rust;

import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.m3.type.Class;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.rust.parser.NativeGrammarParser;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

/**
 * The {@code ###Pure} section parser in {@code on} mode: the Rust parser's result reaches the engine.
 */
public class TestRustPureSectionParser
{
    private String previousMode;
    private String previousFallback;

    @Before
    public void useRustParser()
    {
        Assume.assumeTrue("Rust parser not available: " + NativeGrammarParser.getLoadFailure(), NativeGrammarParser.isAvailable());
        this.previousMode = System.getProperty(RustGrammarMode.PROPERTY);
        this.previousFallback = System.getProperty(RustGrammarMode.FALLBACK_PROPERTY);
        System.setProperty(RustGrammarMode.PROPERTY, "on");
    }

    @After
    public void restoreMode()
    {
        restore(RustGrammarMode.PROPERTY, this.previousMode);
        restore(RustGrammarMode.FALLBACK_PROPERTY, this.previousFallback);
    }

    private static void restore(String property, String value)
    {
        if (value == null)
        {
            System.clearProperty(property);
        }
        else
        {
            System.setProperty(property, value);
        }
    }

    @Test
    public void testClassParsedByRust()
    {
        PureModelContextData model = PureGrammarParser.newInstance().parseModel("Class a::Person\n{\n  name: String[1];\n}\n");
        PackageableElement element = model.getElements().stream().filter(e -> e instanceof Class).findFirst().orElseThrow(AssertionError::new);
        Class person = (Class) element;
        Assert.assertEquals("a::Person", person.getPath());
        Assert.assertEquals("name", person.properties.get(0).name);
        Assert.assertEquals(1, person.sourceInformation.startLine);
        Assert.assertEquals(3, person.properties.get(0).sourceInformation.startLine);
    }

    @Test
    public void testSyntaxErrorIsAnEngineParserError()
    {
        try
        {
            PureGrammarParser.newInstance().parseModel("Class a::Person\n{\n  name: ;\n}\n");
            Assert.fail("expected a parser error");
        }
        catch (EngineException e)
        {
            Assert.assertEquals(3, e.getSourceInformation().startLine);
        }
    }

    @Test
    public void testIslandOfAnAbsentExtensionIsLeftToJava()
    {
        // The Rust library parses `#TDS{…}#`, but this classpath has no TDS extension: the Java parser reports it.
        try
        {
            PureGrammarParser.newInstance().parseModel("function f(): Any[*]\n{\n  #TDS{a\n1}#\n}\n");
            Assert.fail("expected a parser error");
        }
        catch (EngineException e)
        {
            Assert.assertEquals(EngineErrorType.PARSER, e.getErrorType());
            Assert.assertTrue(e.getMessage(), e.getMessage().startsWith("Can't find an embedded Pure parser for the type 'TDS'"));
        }
    }

    @Test
    public void testEmbeddedDataOfAnAbsentExtensionIsLeftToJava()
    {
        // The Rust library parses `Relational #{…}#`, but this classpath has no relational store extension.
        try
        {
            PureGrammarParser.newInstance().parseModel("function my::f(): Any[*]\n{\n  1\n}\n{\n  store::DB: Relational #{ default.T: 'a'; }#;\n  t | f() => 1;\n}\n");
            Assert.fail("expected a parser error");
        }
        catch (EngineException e)
        {
            Assert.assertEquals(EngineErrorType.PARSER, e.getErrorType());
            Assert.assertTrue(e.getMessage(), e.getMessage().startsWith("Unknown embedded data type: Relational"));
        }
    }

    @Test
    public void testWithoutFallbackTheRustErrorIsReported()
    {
        System.setProperty(RustGrammarMode.FALLBACK_PROPERTY, "false");
        try
        {
            PureGrammarParser.newInstance().parseModel("function f(): Any[*]\n{\n  #GQL{x}#\n}\n");
            Assert.fail("expected a parser error");
        }
        catch (EngineException e)
        {
            Assert.assertEquals(EngineErrorType.PARSER, e.getErrorType());
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("no island grammar is linked for '#GQL{…}#'; the linked ones are: "));
        }
    }
}
