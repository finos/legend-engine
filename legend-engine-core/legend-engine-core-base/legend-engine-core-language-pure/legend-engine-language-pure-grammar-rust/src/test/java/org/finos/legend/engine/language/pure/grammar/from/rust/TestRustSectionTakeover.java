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
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.finos.legend.engine.language.pure.grammar.from.CorePureGrammarParser;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.language.pure.grammar.from.extension.ConnectionValueParser;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtension;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtensions;
import org.finos.legend.engine.language.pure.grammar.from.extension.SectionParser;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.connection.ModelChainConnection;
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
 * has a grammar for (`Data`, a section without imports, `Mapping`, `Connection` and `Runtime`), and a section holding
 * grammar it has none for (a connection of a type only this test's Java extension reads), which falls back. A section
 * type the Rust library doesn't parse (one only this test's extension reads) isn't taken over.
 */
public class TestRustSectionTakeover
{
    static final String SUPPORTED = "Class a::A\n" +
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
            ")\n" +
            "\n" +
            "###Connection\n" +
            "JsonModelConnection a::C\n" +
            "{\n" +
            "  class: a::A;\n" +
            "  url: 'data:application/json,{}';\n" +
            "}\n" +
            "\n" +
            "###Runtime\n" +
            "Runtime a::R\n" +
            "{\n" +
            "  mappings: [a::M];\n" +
            "  connections: [ModelStore: [c: a::C]];\n" +
            "}\n";

    static final String JAVA_ONLY_SECTION = "\n" +
            "###TestJavaOnly\n" +
            "whatever the extension reads\n";

    static final String MODEL = SUPPORTED +
            "\n" +
            "###Connection\n" +
            "TestJavaOnlyConnection a::T\n" +
            "{\n" +
            "  whatever the extension reads\n" +
            "}\n";

    /**
     * Grammar only Java reads: a section type, and a connection type.
     */
    static final PureGrammarParserExtension JAVA_ONLY = new PureGrammarParserExtension()
    {
        @Override
        public Iterable<? extends SectionParser> getExtraSectionParsers()
        {
            return Lists.immutable.with(SectionParser.newParser("TestJavaOnly", (sectionSourceCode, elementConsumer, context) ->
            {
                DefaultCodeSection section = new DefaultCodeSection();
                section.parserName = sectionSourceCode.sectionType;
                section.sourceInformation = sectionSourceCode.sourceInformation;
                return section;
            }));
        }

        @Override
        public Iterable<? extends ConnectionValueParser> getExtraConnectionParsers()
        {
            return Lists.immutable.with(ConnectionValueParser.newParser("TestJavaOnlyConnection", connectionValueSourceCode ->
            {
                ModelChainConnection connection = new ModelChainConnection();
                connection.mappings = Lists.mutable.empty();
                connection.sourceInformation = connectionValueSourceCode.sourceInformation;
                return connection;
            }));
        }
    };

    static final PureGrammarParser JAVA = PureGrammarParser.newInstance(PureGrammarParserExtensions.fromExtensions(new CorePureGrammarParser(), JAVA_ONLY));
    private static final PureGrammarParser TAKEN_OVER = PureGrammarParser.newInstance(PureGrammarParserExtensions.fromExtensions(new CorePureGrammarParser(), JAVA_ONLY, new RustGrammarParserExtension("Pure", "Data", "Mapping", "Connection", "Runtime")));

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
        // Without the fallback, a section the Rust library can't parse would fail: the Java-only section type must be
        // left to Java.
        System.setProperty(RustGrammarMode.PROPERTY, "on");
        System.setProperty(RustGrammarMode.FALLBACK_PROPERTY, "false");
        RustGrammarParserExtension extension = new RustGrammarParserExtension();
        Assert.assertTrue(extension.getSectionTypes().toString(), extension.getSectionTypes().containsAllArguments("Pure", "Data", "Mapping", "Connection", "Runtime"));
        Assert.assertFalse(extension.getSectionTypes().toString(), extension.getSectionTypes().contains("TestJavaOnly"));
        PureGrammarParser parser = PureGrammarParser.newInstance(PureGrammarParserExtensions.fromExtensions(new CorePureGrammarParser(), JAVA_ONLY, extension));
        String model = SUPPORTED + JAVA_ONLY_SECTION;
        Assert.assertEquals(RustSectionResult.MAPPER.writeValueAsString(JAVA.parseModel(model)), RustSectionResult.MAPPER.writeValueAsString(parser.parseModel(model)));
    }

    @Test
    public void testCorpusDumpsTakeOverMore()
    {
        System.setProperty(RustGrammarParserExtension.CORPUS_SECTIONS_PROPERTY, "Service, Diagram");
        ImmutableList<String> sectionTypes = new RustGrammarParserExtension().getSectionTypes();
        Assert.assertTrue(sectionTypes.toString(), sectionTypes.containsAllArguments("Service", "Diagram"));
        // And those the library parses, Pure first, if it loaded.
        Assert.assertEquals(sectionTypes.toString(), NativeGrammarParser.isAvailable(), sectionTypes.contains("Pure"));
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
        Assert.assertEquals(Arrays.asList(ImportAwareCodeSection.class, DefaultCodeSection.class, ImportAwareCodeSection.class, ImportAwareCodeSection.class, ImportAwareCodeSection.class, ImportAwareCodeSection.class),
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
        // Pure, Data, Mapping, Connection and Runtime match; the last Connection section holds a connection only Java reads.
        Assert.assertEquals(unsupported + 1, (long) RustGrammarParity.counts().get(RustGrammarParity.Outcome.UNSUPPORTED));
        Assert.assertEquals(mismatched, (long) RustGrammarParity.counts().get(RustGrammarParity.Outcome.MISMATCH));
        Assert.assertTrue(RustGrammarParity.counts().get(RustGrammarParity.Outcome.MATCH) >= matched + 5);
    }

    private static void assertParsesAsJava() throws JsonProcessingException
    {
        PureModelContextData java = JAVA.parseModel(MODEL);
        PureModelContextData taken = TAKEN_OVER.parseModel(MODEL);
        Assert.assertEquals(RustSectionResult.MAPPER.writeValueAsString(java), RustSectionResult.MAPPER.writeValueAsString(taken));
    }
}
