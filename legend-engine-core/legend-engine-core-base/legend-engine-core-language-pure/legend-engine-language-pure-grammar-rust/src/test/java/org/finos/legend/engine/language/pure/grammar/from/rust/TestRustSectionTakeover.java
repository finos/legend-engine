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
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.DefaultCodeSection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.ImportAwareCodeSection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.SectionIndex;
import org.finos.legend.pure.rust.parser.NativeGrammarParser;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Section types the Rust parser takes over parse exactly as the Java parser alone parses them, in every mode: what it
 * has a grammar for (`Data`, a section without imports, and `Mapping`), and a section holding grammar it has none for
 * yet (a mapping's `Operation` element), which falls back.
 */
public class TestRustSectionTakeover
{
    private static final String SUPPORTED = "Class a::A\n" +
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
            "}\n" +
            "\n" +
            "###Mapping\n" +
            "Mapping a::M\n" +
            "(\n" +
            ")\n";

    private static final String MODEL = SUPPORTED +
            "\n" +
            "###Mapping\n" +
            "Mapping a::N\n" +
            "(\n" +
            "  *a::A[a]: Operation\n" +
            "  {\n" +
            "    meta::pure::router::operations::union_OperationSetImplementation_1__SetImplementation_MANY_(a)\n" +
            "  }\n" +
            ")\n";

    private static final PureGrammarParser JAVA = PureGrammarParser.newInstance(PureGrammarParserExtensions.fromExtensions(new CorePureGrammarParser()));
    private static final PureGrammarParser TAKEN_OVER = PureGrammarParser.newInstance(PureGrammarParserExtensions.fromExtensions(new CorePureGrammarParser(), new RustGrammarParserExtension("Pure", "Data", "Mapping")));

    private String previousMode;
    private String previousFallback;
    private String previousCorpusSections;

    @Before
    public void rememberProperties()
    {
        this.previousMode = System.getProperty(RustGrammarMode.PROPERTY);
        this.previousFallback = System.getProperty(RustGrammarMode.FALLBACK_PROPERTY);
        this.previousCorpusSections = System.getProperty(RustGrammarParserExtension.CORPUS_SECTIONS_PROPERTY);
    }

    @After
    public void restoreProperties()
    {
        restore(RustGrammarMode.PROPERTY, this.previousMode);
        restore(RustGrammarMode.FALLBACK_PROPERTY, this.previousFallback);
        restore(RustGrammarParserExtension.CORPUS_SECTIONS_PROPERTY, this.previousCorpusSections);
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
    public void testTakesOverTheSectionsTheLibraryParses() throws JsonProcessingException
    {
        Assume.assumeTrue("Rust parser not available: " + NativeGrammarParser.getLoadFailure(), NativeGrammarParser.isAvailable());
        // Without the fallback, a section the Rust library can't parse would fail: Connection must be left to Java.
        System.setProperty(RustGrammarMode.PROPERTY, "on");
        System.setProperty(RustGrammarMode.FALLBACK_PROPERTY, "false");
        RustGrammarParserExtension extension = new RustGrammarParserExtension();
        Assert.assertTrue(extension.getSectionTypes().toString(), extension.getSectionTypes().containsAllArguments("Pure", "Data", "Mapping"));
        Assert.assertFalse(extension.getSectionTypes().toString(), extension.getSectionTypes().contains("Connection"));
        PureGrammarParser parser = PureGrammarParser.newInstance(PureGrammarParserExtensions.fromExtensions(new CorePureGrammarParser(), extension));
        Assert.assertEquals(RustSectionResult.MAPPER.writeValueAsString(JAVA.parseModel(SUPPORTED)), RustSectionResult.MAPPER.writeValueAsString(parser.parseModel(SUPPORTED)));
    }

    @Test
    public void testCorpusDumpsTakeOverMore()
    {
        System.setProperty(RustGrammarParserExtension.CORPUS_SECTIONS_PROPERTY, "Connection, Runtime");
        Assert.assertTrue(new RustGrammarParserExtension().getSectionTypes().containsAllArguments("Pure", "Connection", "Runtime"));
    }

    @Test
    public void testOffModeParsesWithJava() throws JsonProcessingException
    {
        System.setProperty(RustGrammarMode.PROPERTY, "off");
        assertParsesAsJava();
    }

    @Test
    public void testOnModeParsesAsJava() throws JsonProcessingException
    {
        Assume.assumeTrue("Rust parser not available: " + NativeGrammarParser.getLoadFailure(), NativeGrammarParser.isAvailable());
        System.setProperty(RustGrammarMode.PROPERTY, "on");
        assertParsesAsJava();
        // The Data section has no imports.
        SectionIndex index = (SectionIndex) TAKEN_OVER.parseModel(MODEL).getElements().stream()
                .filter(SectionIndex.class::isInstance)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no section index"));
        Assert.assertEquals(Arrays.asList(ImportAwareCodeSection.class, DefaultCodeSection.class, ImportAwareCodeSection.class, ImportAwareCodeSection.class),
                index.sections.stream().map(Object::getClass).collect(Collectors.toList()));
    }

    @Test
    public void testShadowModeComparesWhatRustParses() throws JsonProcessingException
    {
        Assume.assumeTrue("Rust parser not available: " + NativeGrammarParser.getLoadFailure(), NativeGrammarParser.isAvailable());
        System.setProperty(RustGrammarMode.PROPERTY, "shadow");
        long matched = RustGrammarParity.counts().get(RustGrammarParity.Outcome.MATCH);
        long unsupported = RustGrammarParity.counts().get(RustGrammarParity.Outcome.UNSUPPORTED);
        long mismatched = RustGrammarParity.counts().get(RustGrammarParity.Outcome.MISMATCH);
        assertParsesAsJava();
        // Pure, Data and the first mapping match; the second mapping's element has no Rust grammar yet.
        Assert.assertEquals(unsupported + 1, (long) RustGrammarParity.counts().get(RustGrammarParity.Outcome.UNSUPPORTED));
        Assert.assertEquals(mismatched, (long) RustGrammarParity.counts().get(RustGrammarParity.Outcome.MISMATCH));
        Assert.assertTrue(RustGrammarParity.counts().get(RustGrammarParity.Outcome.MATCH) >= matched + 3);
    }

    private static void assertParsesAsJava() throws JsonProcessingException
    {
        PureModelContextData java = JAVA.parseModel(MODEL);
        PureModelContextData taken = TAKEN_OVER.parseModel(MODEL);
        Assert.assertEquals(RustSectionResult.MAPPER.writeValueAsString(java), RustSectionResult.MAPPER.writeValueAsString(taken));
    }
}
