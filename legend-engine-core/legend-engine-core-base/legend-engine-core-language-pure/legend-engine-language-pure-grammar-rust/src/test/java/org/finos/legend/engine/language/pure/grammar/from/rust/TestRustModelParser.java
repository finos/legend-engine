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
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.rust.parser.NativeGrammarParser;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Whole models with the {@code rust} section parse strategy: in {@code on} mode, the Rust parser parses the whole
 * model, and the engine's parser, which splits it with ANTLR, does when the Rust parser can't; {@code shadow} compares
 * the two; {@code off} parses with the engine's parser.
 */
public class TestRustModelParser
{
    private static final PureGrammarParser TAKEN_OVER = parserTakingOver("Pure", "Data", "Mapping", "Connection", "Runtime");

    /**
     * A parser whose Rust extension leaves {@code Mapping} to Java.
     */
    private static final PureGrammarParser PARTLY_TAKEN_OVER = parserTakingOver("Pure", "Data", "Connection", "Runtime");

    private String previousMode;
    private String previousFallback;
    private String previousStrategy;

    private static PureGrammarParser parserTakingOver(String... sectionTypes)
    {
        return PureGrammarParser.newInstance(PureGrammarParserExtensions.fromExtensions(new CorePureGrammarParser(), TestRustSectionTakeover.JAVA_ONLY, new RustGrammarParserExtension(sectionTypes)));
    }

    @Before
    public void rememberProperties()
    {
        this.previousMode = System.getProperty(RustGrammarMode.PROPERTY);
        this.previousFallback = System.getProperty(RustGrammarMode.FALLBACK_PROPERTY);
        this.previousStrategy = System.getProperty(SectionParseStrategy.PROPERTY);
        System.setProperty(SectionParseStrategy.PROPERTY, "rust");
    }

    @After
    public void restoreProperties()
    {
        restore(RustGrammarMode.PROPERTY, this.previousMode);
        restore(RustGrammarMode.FALLBACK_PROPERTY, this.previousFallback);
        restore(SectionParseStrategy.PROPERTY, this.previousStrategy);
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

    private static void requireNativeLibrary()
    {
        Assume.assumeTrue("Rust parser not available: " + NativeGrammarParser.getLoadFailure(), NativeGrammarParser.isAvailable());
    }

    @Test
    public void testOnModeParsesWholeModelsWithRust() throws JsonProcessingException
    {
        requireNativeLibrary();
        System.setProperty(RustGrammarMode.PROPERTY, "on");
        // Without the fallback, a model the Rust parser couldn't parse would fail.
        System.setProperty(RustGrammarMode.FALLBACK_PROPERTY, "false");
        String model = TestRustSectionTakeover.SUPPORTED;
        assertSame(() -> TestRustSectionTakeover.JAVA.parseModel(model), () -> TAKEN_OVER.parseModel(model));
        // Without source information, and with offsets.
        assertSame(() -> TestRustSectionTakeover.JAVA.parseModel(model, false), () -> TAKEN_OVER.parseModel(model, false));
        assertSame(() -> TestRustSectionTakeover.JAVA.parseModel(model, "model.pure", 3, 5, true), () -> TAKEN_OVER.parseModel(model, "model.pure", 3, 5, true));
    }

    @Test
    public void testOnModeFallsBackToTheEngineParser() throws JsonProcessingException
    {
        requireNativeLibrary();
        System.setProperty(RustGrammarMode.PROPERTY, "on");
        // Grammar only Java reads: a section type, and a connection type.
        String model = TestRustSectionTakeover.MODEL + TestRustSectionTakeover.JAVA_ONLY_SECTION;
        assertSame(() -> TestRustSectionTakeover.JAVA.parseModel(model), () -> TAKEN_OVER.parseModel(model));
        // A section type the extension doesn't take over.
        assertSame(() -> TestRustSectionTakeover.JAVA.parseModel(TestRustSectionTakeover.SUPPORTED), () -> PARTLY_TAKEN_OVER.parseModel(TestRustSectionTakeover.SUPPORTED));
    }

    @Test
    public void testWithoutFallbackRustReportsWhatItDoesNotParse()
    {
        requireNativeLibrary();
        System.setProperty(RustGrammarMode.PROPERTY, "on");
        System.setProperty(RustGrammarMode.FALLBACK_PROPERTY, "false");
        EngineException unsupported = Assert.assertThrows(EngineException.class, () -> TAKEN_OVER.parseModel(TestRustSectionTakeover.SUPPORTED + TestRustSectionTakeover.JAVA_ONLY_SECTION));
        Assert.assertEquals("Rust parser unsupported error: no section grammar is linked for '###TestJavaOnly'", unsupported.getMessage());
        EngineException notTakenOver = Assert.assertThrows(EngineException.class, () -> PARTLY_TAKEN_OVER.parseModel(TestRustSectionTakeover.SUPPORTED));
        Assert.assertEquals("Rust parser unsupported error: the section type 'Mapping' is not taken over", notTakenOver.getMessage());
        // A syntax error is reported where the engine's parser reports it.
        String invalid = "Class a::A\n{\n  name: String[1]\n}\n";
        SourceInformation rust = Assert.assertThrows(EngineException.class, () -> TAKEN_OVER.parseModel(invalid)).getSourceInformation();
        SourceInformation java = Assert.assertThrows(EngineException.class, () -> TestRustSectionTakeover.JAVA.parseModel(invalid)).getSourceInformation();
        Assert.assertEquals(java.startLine + ":" + java.startColumn, rust.startLine + ":" + rust.startColumn);
    }

    @Test
    public void testShadowModeComparesWholeModels() throws JsonProcessingException
    {
        requireNativeLibrary();
        System.setProperty(RustGrammarMode.PROPERTY, "shadow");
        Map<RustGrammarParity.Outcome, Long> before = RustGrammarParity.modelCounts();
        assertSame(() -> TestRustSectionTakeover.JAVA.parseModel(TestRustSectionTakeover.SUPPORTED), () -> TAKEN_OVER.parseModel(TestRustSectionTakeover.SUPPORTED));
        // Without source information: compared as called, and with positions.
        TAKEN_OVER.parseModel(TestRustSectionTakeover.SUPPORTED, false);
        TAKEN_OVER.parseModel(TestRustSectionTakeover.MODEL + TestRustSectionTakeover.JAVA_ONLY_SECTION);
        Map<RustGrammarParity.Outcome, Long> after = RustGrammarParity.modelCounts();
        Assert.assertEquals(before.get(RustGrammarParity.Outcome.MATCH) + 3, (long) after.get(RustGrammarParity.Outcome.MATCH));
        Assert.assertEquals(before.get(RustGrammarParity.Outcome.UNSUPPORTED) + 1, (long) after.get(RustGrammarParity.Outcome.UNSUPPORTED));
        Assert.assertEquals(before.get(RustGrammarParity.Outcome.MISMATCH), after.get(RustGrammarParity.Outcome.MISMATCH));
    }

    @Test
    public void testOffModeParsesWithTheEngineParser() throws JsonProcessingException
    {
        System.setProperty(RustGrammarMode.PROPERTY, "off");
        Map<RustGrammarParity.Outcome, Long> before = RustGrammarParity.modelCounts();
        String model = TestRustSectionTakeover.MODEL + TestRustSectionTakeover.JAVA_ONLY_SECTION;
        assertSame(() -> TestRustSectionTakeover.JAVA.parseModel(model), () -> TAKEN_OVER.parseModel(model));
        Assert.assertEquals(before, RustGrammarParity.modelCounts());
    }

    private static void assertSame(Supplier<PureModelContextData> expected, Supplier<PureModelContextData> actual) throws JsonProcessingException
    {
        Assert.assertEquals(RustSectionResult.MAPPER.writeValueAsString(expected.get()), RustSectionResult.MAPPER.writeValueAsString(actual.get()));
    }
}
