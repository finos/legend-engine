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

import com.fasterxml.jackson.core.JsonProcessingException;
import org.finos.legend.engine.language.pure.grammar.from.CorePureGrammarParser;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtensions;
import org.finos.legend.pure.rust.parser.NativeGrammarParser;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

/**
 * A section type the Rust parser takes over but has no grammar for yet (`Data`, another extension's) parses exactly as
 * the Java parser alone parses it, in every mode.
 */
public class TestRustSectionTakeover
{
    private static final String MODEL = "Class a::A\n" +
            "{\n" +
            "  name: String[1];\n" +
            "}\n" +
            "\n" +
            "###Data\n" +
            "Data a::D\n" +
            "{\n" +
            "  ExternalFormat\n" +
            "  #{\n" +
            "    contentType: 'application/json';\n" +
            "    data: '{}';\n" +
            "  }#\n" +
            "}\n";

    private String previousMode;

    @Before
    public void rememberMode()
    {
        this.previousMode = System.getProperty(RustGrammarMode.PROPERTY);
    }

    @After
    public void restoreMode()
    {
        if (this.previousMode == null)
        {
            System.clearProperty(RustGrammarMode.PROPERTY);
        }
        else
        {
            System.setProperty(RustGrammarMode.PROPERTY, this.previousMode);
        }
    }

    @Test
    public void testOffModeParsesWithJava() throws JsonProcessingException
    {
        System.setProperty(RustGrammarMode.PROPERTY, "off");
        assertParsesAsJava();
    }

    @Test
    public void testOnModeFallsBackToJava() throws JsonProcessingException
    {
        Assume.assumeTrue("Rust parser not available: " + NativeGrammarParser.getLoadFailure(), NativeGrammarParser.isAvailable());
        System.setProperty(RustGrammarMode.PROPERTY, "on");
        assertParsesAsJava();
    }

    @Test
    public void testShadowModeCountsTheSectionUnsupported() throws JsonProcessingException
    {
        Assume.assumeTrue("Rust parser not available: " + NativeGrammarParser.getLoadFailure(), NativeGrammarParser.isAvailable());
        System.setProperty(RustGrammarMode.PROPERTY, "shadow");
        long unsupported = RustGrammarParity.counts().get(RustGrammarParity.Outcome.UNSUPPORTED);
        assertParsesAsJava();
        Assert.assertEquals(unsupported + 1, (long) RustGrammarParity.counts().get(RustGrammarParity.Outcome.UNSUPPORTED));
    }

    private static void assertParsesAsJava() throws JsonProcessingException
    {
        String java = RustSectionResult.MAPPER.writeValueAsString(PureGrammarParser.newInstance(PureGrammarParserExtensions.fromExtensions(new CorePureGrammarParser())).parseModel(MODEL));
        String taken = RustSectionResult.MAPPER.writeValueAsString(PureGrammarParser.newInstance(PureGrammarParserExtensions.fromExtensions(new CorePureGrammarParser(), new RustGrammarParserExtension("Pure", "Data"))).parseModel(MODEL));
        Assert.assertEquals(java, taken);
    }
}
