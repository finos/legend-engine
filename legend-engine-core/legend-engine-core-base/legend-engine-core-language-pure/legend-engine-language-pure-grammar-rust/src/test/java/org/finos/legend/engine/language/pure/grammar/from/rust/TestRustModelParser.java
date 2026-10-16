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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
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
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Whole models: in {@code on} mode, the Rust parser parses the whole model, and the engine's parser, which splits it
 * with ANTLR, does when the Rust parser can't, or when the model holds a section type not taken over; {@code shadow}
 * compares the two; {@code off} parses with the engine's parser.
 */
public class TestRustModelParser
{
    private static final PureGrammarParser TAKEN_OVER = parserTakingOver("Pure", "Data", "Mapping", "Connection", "Runtime");

    /**
     * A parser whose Rust extension leaves {@code Mapping} to Java.
     */
    private static final PureGrammarParser PARTLY_TAKEN_OVER = parserTakingOver("Pure", "Data", "Connection", "Runtime");

    private final ListAppender<ILoggingEvent> fallbackLog = new ListAppender<>();
    private String previousMode;
    private String previousFallback;

    private static PureGrammarParser parserTakingOver(String... sectionTypes)
    {
        return PureGrammarParser.newInstance(PureGrammarParserExtensions.fromExtensions(new CorePureGrammarParser(), TestRustSectionTakeover.JAVA_ONLY, new RustGrammarParserExtension(sectionTypes)));
    }

    @Before
    public void rememberProperties()
    {
        this.previousMode = System.getProperty(RustGrammarMode.PROPERTY);
        this.previousFallback = System.getProperty(RustGrammarMode.FALLBACK_PROPERTY);
        this.fallbackLog.start();
        fallbackLogger().addAppender(this.fallbackLog);
    }

    @After
    public void restoreProperties()
    {
        restore(RustGrammarMode.PROPERTY, this.previousMode);
        restore(RustGrammarMode.FALLBACK_PROPERTY, this.previousFallback);
        fallbackLogger().detachAppender(this.fallbackLog);
    }

    private static Logger fallbackLogger()
    {
        return (Logger) LoggerFactory.getLogger(RustGrammarFallback.class);
    }

    /**
     * The fallback's warnings so far.
     */
    private List<String> warnings()
    {
        return this.fallbackLog.list.stream().filter(e -> e.getLevel() == Level.WARN).map(ILoggingEvent::getFormattedMessage).collect(Collectors.toList());
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
        // A connection of a type only Java reads, in a section type taken over.
        assertSame(() -> TestRustSectionTakeover.JAVA.parseModel(TestRustSectionTakeover.MODEL), () -> TAKEN_OVER.parseModel(TestRustSectionTakeover.MODEL));
        // The model falls back, then its Connection section does.
        List<String> warnings = warnings();
        Assert.assertEquals(warnings.toString(), 2, warnings.size());
        Assert.assertTrue(warnings.get(0), warnings.get(0).startsWith("Rust parser unsupported error, parsing with Java instead: no connection grammar is linked for 'TestJavaOnlyConnection'"));
        Assert.assertTrue(warnings.get(0), warnings.get(0).contains("[model at "));
        Assert.assertTrue(warnings.get(1), warnings.get(1).contains("[section Connection at "));
    }

    @Test
    public void testModelsWithSectionTypesNotTakenOverAreTheEngineParsers() throws JsonProcessingException
    {
        requireNativeLibrary();
        System.setProperty(RustGrammarMode.PROPERTY, "on");
        // Without the fallback, the Rust parser's attempt would fail: it doesn't make one.
        System.setProperty(RustGrammarMode.FALLBACK_PROPERTY, "false");
        String javaOnlySection = TestRustSectionTakeover.SUPPORTED + TestRustSectionTakeover.JAVA_ONLY_SECTION;
        assertSame(() -> TestRustSectionTakeover.JAVA.parseModel(javaOnlySection), () -> TAKEN_OVER.parseModel(javaOnlySection));
        assertSame(() -> TestRustSectionTakeover.JAVA.parseModel(TestRustSectionTakeover.SUPPORTED), () -> PARTLY_TAKEN_OVER.parseModel(TestRustSectionTakeover.SUPPORTED));
        // Each names the section type, and its header's position in the document.
        PARTLY_TAKEN_OVER.parseModel(TestRustSectionTakeover.SUPPORTED, "model.pure", 3, 5, true);
        Assert.assertEquals(Arrays.asList(
                "Rust parser does not take over section type 'TestJavaOnly', parsing the model section by section instead [model at 35:1]",
                "Rust parser does not take over section type 'Mapping', parsing the model section by section instead [model at 16:1]",
                "Rust parser does not take over section type 'Mapping', parsing the model section by section instead [model, model.pure at 19:1]"), warnings());
    }

    @Test
    public void testWithoutFallbackRustReportsItsErrors()
    {
        requireNativeLibrary();
        System.setProperty(RustGrammarMode.PROPERTY, "on");
        System.setProperty(RustGrammarMode.FALLBACK_PROPERTY, "false");
        EngineException unsupported = Assert.assertThrows(EngineException.class, () -> TAKEN_OVER.parseModel(TestRustSectionTakeover.MODEL));
        Assert.assertTrue(unsupported.getMessage(), unsupported.getMessage().startsWith("Rust parser unsupported error: no connection grammar is linked for 'TestJavaOnlyConnection'"));
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
        TAKEN_OVER.parseModel(TestRustSectionTakeover.MODEL);
        // Not compared: a section type is not taken over.
        TAKEN_OVER.parseModel(TestRustSectionTakeover.SUPPORTED + TestRustSectionTakeover.JAVA_ONLY_SECTION);
        Map<RustGrammarParity.Outcome, Long> after = RustGrammarParity.modelCounts();
        Assert.assertEquals(before.get(RustGrammarParity.Outcome.MATCH) + 3, (long) after.get(RustGrammarParity.Outcome.MATCH));
        Assert.assertEquals(before.get(RustGrammarParity.Outcome.UNLINKED) + 1, (long) after.get(RustGrammarParity.Outcome.UNLINKED));
        Assert.assertEquals(before.get(RustGrammarParity.Outcome.MISMATCH), after.get(RustGrammarParity.Outcome.MISMATCH));
    }

    @Test
    public void testWithoutTheLibraryOnModeFailsAndShadowComparesNothing() throws JsonProcessingException
    {
        Assume.assumeFalse("the Rust parser is available", NativeGrammarParser.isAvailable());
        // Without the library, the extension takes nothing over.
        Assert.assertTrue(new RustGrammarParserExtension().getSectionTypes().isEmpty());
        PureGrammarParser parser = PureGrammarParser.newInstance(PureGrammarParserExtensions.fromExtensions(new CorePureGrammarParser(), new RustGrammarParserExtension()));
        System.setProperty(RustGrammarMode.PROPERTY, "on");
        IllegalStateException failure = Assert.assertThrows(IllegalStateException.class, () -> parser.parseModel(TestRustSectionTakeover.SUPPORTED));
        Assert.assertTrue(failure.getMessage(), failure.getMessage().startsWith(RustGrammarMode.PROPERTY + "=on, but the Rust parser is not available"));
        System.setProperty(RustGrammarMode.PROPERTY, "shadow");
        long unavailable = RustGrammarParity.modelCounts().get(RustGrammarParity.Outcome.UNAVAILABLE);
        assertSame(() -> TestRustSectionTakeover.JAVA.parseModel(TestRustSectionTakeover.SUPPORTED), () -> parser.parseModel(TestRustSectionTakeover.SUPPORTED));
        Assert.assertEquals(unavailable + 1, (long) RustGrammarParity.modelCounts().get(RustGrammarParity.Outcome.UNAVAILABLE));
    }

    @Test
    public void testOffModeParsesWithTheEngineParser() throws JsonProcessingException
    {
        System.setProperty(RustGrammarMode.PROPERTY, "off");
        Map<RustGrammarParity.Outcome, Long> before = RustGrammarParity.modelCounts();
        String model = TestRustSectionTakeover.MODEL + TestRustSectionTakeover.JAVA_ONLY_SECTION;
        assertSame(() -> TestRustSectionTakeover.JAVA.parseModel(model), () -> TAKEN_OVER.parseModel(model));
        Assert.assertEquals(before, RustGrammarParity.modelCounts());
        Assert.assertEquals(Arrays.asList(), warnings());
    }

    private static void assertSame(Supplier<PureModelContextData> expected, Supplier<PureModelContextData> actual) throws JsonProcessingException
    {
        Assert.assertEquals(RustSectionResult.MAPPER.writeValueAsString(expected.get()), RustSectionResult.MAPPER.writeValueAsString(actual.get()));
    }
}
