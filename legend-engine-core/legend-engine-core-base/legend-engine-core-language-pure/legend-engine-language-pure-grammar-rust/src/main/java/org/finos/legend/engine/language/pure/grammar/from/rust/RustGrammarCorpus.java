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
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserContext;
import org.finos.legend.engine.language.pure.grammar.from.SectionSourceCode;
import org.finos.legend.engine.language.pure.grammar.from.extension.SectionParser;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.ImportAwareCodeSection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.Section;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Dumps what is parsed to golden corpora for the Rust parser's own tests: every section, when
 * {@value #CORPUS_DIR_PROPERTY} is set, and every standalone expression, when {@value #EXPRESSION_CORPUS_DIR_PROPERTY}
 * is. Each entry is {@code <hash>.pure} (the text), {@code <hash>.meta.json} (section type or expression kind, source
 * id, offsets) and {@code <hash>.java.json}, the Java parser's result with source information: a section's elements
 * and imports, an expression's {@code value}, or the {@code error}. Needs no native library.
 */
public final class RustGrammarCorpus
{
    /**
     * System property: directory to write the section corpus to.
     */
    public static final String CORPUS_DIR_PROPERTY = "legend.grammar.rust.corpusDir";

    /**
     * System property: directory to write the standalone expression corpus to.
     */
    public static final String EXPRESSION_CORPUS_DIR_PROPERTY = "legend.grammar.rust.expressionCorpusDir";

    private RustGrammarCorpus()
    {
    }

    static void record(SectionSourceCode code, PureGrammarParserContext context, SectionParser javaParser)
    {
        String dir = System.getProperty(CORPUS_DIR_PROPERTY);
        if (dir == null || dir.isEmpty())
        {
            return;
        }
        byte[] expected;
        try
        {
            List<PackageableElement> elements = new ArrayList<>();
            Section section = RustSectionParser.parseWithPositions(javaParser, code, elements::add, context);
            expected = render(new Parsed(RustSectionResult.roundTrip(elements), RustSectionResult.imports(section)));
        }
        catch (RuntimeException e)
        {
            expected = render(error(e));
        }
        ObjectNode meta = RustSectionResult.MAPPER.createObjectNode();
        meta.put("sectionType", code.sectionType);
        meta.put("sourceId", code.walkerSourceInformation.getSourceId());
        meta.put("lineOffset", code.walkerSourceInformation.getLineOffset());
        meta.put("columnOffset", code.walkerSourceInformation.getColumnOffset());
        write(dir, RustGrammarParity.hash(code), code.code, meta, expected);
    }

    /**
     * Records a standalone expression; {@code java} parses it, with or without source information.
     */
    static <T> void recordExpression(RustExpression<T> expression, Function<Boolean, T> java)
    {
        String dir = System.getProperty(EXPRESSION_CORPUS_DIR_PROPERTY);
        if (dir == null || dir.isEmpty())
        {
            return;
        }
        byte[] expected;
        try
        {
            T value = RustGrammarParity.roundTrip(java.apply(true), expression.type);
            // The value is written with its own type, which a field of type Object would lose, and as text: a JSON
            // tree would drop trailing zeros from decimals.
            String json = RustSectionResult.MAPPER.writerFor(expression.type).withDefaultPrettyPrinter().writeValueAsString(value);
            expected = ("{\n\"value\" : " + json + "\n}").getBytes(StandardCharsets.UTF_8);
        }
        catch (JsonProcessingException e)
        {
            throw new IllegalStateException("cannot render the corpus entry for expression " + expression.hash(), e);
        }
        catch (RuntimeException e)
        {
            expected = render(error(e));
        }
        ObjectNode meta = RustSectionResult.MAPPER.createObjectNode();
        meta.put("kind", expression.kind);
        meta.put("sourceId", expression.sourceId);
        meta.put("lineOffset", expression.lineOffset);
        meta.put("columnOffset", expression.columnOffset);
        write(dir, expression.hash(), expression.code, meta, expected);
    }

    private static ObjectNode error(RuntimeException e)
    {
        EngineException engineException = EngineException.findException(e);
        ObjectNode failed = RustSectionResult.MAPPER.createObjectNode();
        ObjectNode error = failed.putObject("error");
        error.put("message", (engineException == null) ? String.valueOf(e.getMessage()) : engineException.getMessage());
        if (engineException != null && engineException.getSourceInformation() != null)
        {
            error.set("sourceInformation", RustSectionResult.MAPPER.valueToTree(engineException.getSourceInformation()));
        }
        return failed;
    }

    private static byte[] render(Object value)
    {
        try
        {
            return RustSectionResult.MAPPER.writerWithDefaultPrettyPrinter().writeValueAsBytes(value);
        }
        catch (JsonProcessingException e)
        {
            throw new IllegalStateException("cannot render a corpus entry", e);
        }
    }

    private static void write(String dir, String name, String code, ObjectNode meta, byte[] expected)
    {
        try
        {
            Path folder = Files.createDirectories(Paths.get(dir));
            Files.write(folder.resolve(name + ".pure"), code.getBytes(StandardCharsets.UTF_8));
            Files.write(folder.resolve(name + ".meta.json"), render(meta));
            Files.write(folder.resolve(name + ".java.json"), expected);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * A parsed section as the corpus records it: the element list is declared as such, so each element is written
     * with its {@code _type}, and written directly rather than through a JSON tree, which would drop trailing zeros
     * from decimals. {@code imports} is null, so absent, for a section that is not an {@link ImportAwareCodeSection}.
     */
    static final class Parsed
    {
        public final List<PackageableElement> elements;
        public final List<String> imports;

        Parsed(List<PackageableElement> elements, List<String> imports)
        {
            this.elements = elements;
            this.imports = imports;
        }
    }
}
