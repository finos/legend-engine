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
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserContext;
import org.finos.legend.engine.language.pure.grammar.from.SectionSourceCode;
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
import java.util.Collections;
import java.util.List;

/**
 * Dumps every section parsed to a golden corpus for the Rust parser's own tests, when {@value #CORPUS_DIR_PROPERTY}
 * is set: {@code <hash>.pure} (the section body), {@code <hash>.meta.json} (type, source id, offsets) and
 * {@code <hash>.java.json} (the Java parser's result with source information: elements and imports, or the error).
 * Needs no native library.
 */
public final class RustGrammarCorpus
{
    /**
     * System property: directory to write the corpus to.
     */
    public static final String CORPUS_DIR_PROPERTY = "legend.grammar.rust.corpusDir";

    private RustGrammarCorpus()
    {
    }

    static void record(SectionSourceCode code, PureGrammarParserContext context)
    {
        String dir = System.getProperty(CORPUS_DIR_PROPERTY);
        if (dir == null || dir.isEmpty())
        {
            return;
        }
        ParseTreeWalkerSourceInformation withPositions = new ParseTreeWalkerSourceInformation.Builder(code.walkerSourceInformation).withReturnSourceInfo(true).build();
        ObjectNode expected = RustSectionResult.MAPPER.createObjectNode();
        List<PackageableElement> elements = new ArrayList<>();
        try
        {
            Section section = RustPureSectionParser.parseWithJava(new SectionSourceCode(code.code, code.sectionType, code.sourceInformation, withPositions), withPositions, elements::add, context);
            List<String> imports = (section instanceof ImportAwareCodeSection) ? ((ImportAwareCodeSection) section).imports : Collections.emptyList();
            expected.set("elements", RustSectionResult.MAPPER.valueToTree(RustSectionResult.roundTrip(elements)));
            expected.set("imports", RustSectionResult.MAPPER.valueToTree(imports));
        }
        catch (RuntimeException e)
        {
            EngineException engineException = EngineException.findException(e);
            ObjectNode error = expected.putObject("error");
            error.put("message", (engineException == null) ? String.valueOf(e.getMessage()) : engineException.getMessage());
            if (engineException != null && engineException.getSourceInformation() != null)
            {
                error.set("sourceInformation", RustSectionResult.MAPPER.valueToTree(engineException.getSourceInformation()));
            }
        }

        ObjectNode meta = RustSectionResult.MAPPER.createObjectNode();
        meta.put("sectionType", code.sectionType);
        meta.put("sourceId", code.walkerSourceInformation.getSourceId());
        meta.put("lineOffset", code.walkerSourceInformation.getLineOffset());
        meta.put("columnOffset", code.walkerSourceInformation.getColumnOffset());

        String name = RustGrammarParity.hash(code);
        try
        {
            Path folder = Files.createDirectories(Paths.get(dir));
            Files.write(folder.resolve(name + ".pure"), code.code.getBytes(StandardCharsets.UTF_8));
            Files.write(folder.resolve(name + ".meta.json"), RustSectionResult.MAPPER.writerWithDefaultPrettyPrinter().writeValueAsBytes(meta));
            Files.write(folder.resolve(name + ".java.json"), RustSectionResult.MAPPER.writerWithDefaultPrettyPrinter().writeValueAsBytes(expected));
        }
        catch (JsonProcessingException e)
        {
            throw new IllegalStateException("cannot render the corpus entry for section " + name, e);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }
}
