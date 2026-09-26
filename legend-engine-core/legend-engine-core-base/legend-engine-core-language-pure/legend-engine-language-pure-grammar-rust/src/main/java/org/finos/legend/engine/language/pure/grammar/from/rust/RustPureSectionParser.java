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

import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserContext;
import org.finos.legend.engine.language.pure.grammar.from.SectionSourceCode;
import org.finos.legend.engine.language.pure.grammar.from.domain.DomainParser;
import org.finos.legend.engine.language.pure.grammar.from.extension.SectionParser;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.ImportAwareCodeSection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.Section;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.rust.parser.NativeGrammarParser;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The {@code ###Pure} section parser, backed by the Rust parser according to {@link RustGrammarMode}.
 *
 * <ul>
 *     <li>{@code off}: the Java {@link DomainParser}, exactly as the built-in path parses {@code ###Pure};</li>
 *     <li>{@code shadow}: the Java parser's result, after comparing it with the Rust parser's
 *     (see {@link RustGrammarParity});</li>
 *     <li>{@code on}: the Rust parser's result; input it reports as unsupported falls back to the Java parser.</li>
 * </ul>
 */
public class RustPureSectionParser implements SectionParser
{
    /**
     * The section type this parser handles.
     */
    public static final String SECTION_TYPE = "Pure";

    @Override
    public String getSectionTypeName()
    {
        return SECTION_TYPE;
    }

    @Override
    public Section parse(SectionSourceCode code, Consumer<PackageableElement> elementConsumer, PureGrammarParserContext context)
    {
        RustGrammarCorpus.record(code, context);
        switch (RustGrammarMode.current())
        {
            case ON:
            {
                return parseWithRust(code, elementConsumer, context);
            }
            case SHADOW:
            {
                return parseInShadow(code, elementConsumer, context);
            }
            case OFF:
            default:
            {
                return parseWithJava(code, code.walkerSourceInformation, elementConsumer, context);
            }
        }
    }

    /**
     * The built-in {@code ###Pure} parse, with the given walker source information.
     */
    static Section parseWithJava(SectionSourceCode code, ParseTreeWalkerSourceInformation walker, Consumer<PackageableElement> elementConsumer, PureGrammarParserContext context)
    {
        DomainParser parser = new DomainParser();
        return parser.parse(parser.getParserInfo(code.code, code.sourceInformation, walker), elementConsumer, context);
    }

    private static Section parseWithRust(SectionSourceCode code, Consumer<PackageableElement> elementConsumer, PureGrammarParserContext context)
    {
        if (!NativeGrammarParser.isAvailable())
        {
            throw new IllegalStateException(RustGrammarMode.PROPERTY + "=on, but the Rust parser is not available: " + NativeGrammarParser.getLoadFailure());
        }
        RustSectionResult result = RustSectionResult.parse(code, code.walkerSourceInformation.getReturnSourceInfo());
        if (result.isUnsupported())
        {
            return parseWithJava(code, code.walkerSourceInformation, elementConsumer, context);
        }
        if (result.isError())
        {
            String message = RustSectionResult.PARSER_ERROR.equals(result.errorKind) ? result.errorMessage : ("Rust parser " + result.errorKind + " error: " + result.errorMessage);
            throw new EngineException(message, result.errorSourceInformation, EngineErrorType.PARSER);
        }
        result.elements.forEach(elementConsumer);
        ImportAwareCodeSection section = new ImportAwareCodeSection();
        section.parserName = SECTION_TYPE;
        section.sourceInformation = code.sourceInformation;
        section.imports = new ArrayList<>(result.imports);
        result.elements.forEach(e -> section.elements.add(e.getPath()));
        return section;
    }

    private static Section parseInShadow(SectionSourceCode code, Consumer<PackageableElement> elementConsumer, PureGrammarParserContext context)
    {
        ParseTreeWalkerSourceInformation walker = code.walkerSourceInformation;
        // Collect privately: the consumer must see Java's elements exactly once, after the comparison.
        List<PackageableElement> javaElements = new ArrayList<>();
        Section javaSection = null;
        RuntimeException javaError = null;
        try
        {
            javaSection = parseWithJava(code, walker, javaElements::add, context);
        }
        catch (RuntimeException e)
        {
            javaError = e;
        }
        RustGrammarParity.compare(code, walker.getReturnSourceInfo(), javaElements, javaSection, javaError);
        if (!walker.getReturnSourceInfo())
        {
            // The roundtrip suites parse without source information; compare positions too.
            compareWithPositions(code, context);
        }
        if (javaError != null)
        {
            throw javaError;
        }
        javaElements.forEach(elementConsumer);
        return javaSection;
    }

    private static void compareWithPositions(SectionSourceCode code, PureGrammarParserContext context)
    {
        ParseTreeWalkerSourceInformation withPositions = new ParseTreeWalkerSourceInformation.Builder(code.walkerSourceInformation).withReturnSourceInfo(true).build();
        SectionSourceCode positioned = new SectionSourceCode(code.code, code.sectionType, code.sourceInformation, withPositions);
        List<PackageableElement> javaElements = new ArrayList<>();
        Section javaSection = null;
        RuntimeException javaError = null;
        try
        {
            javaSection = parseWithJava(positioned, withPositions, javaElements::add, context);
        }
        catch (RuntimeException e)
        {
            javaError = e;
        }
        RustGrammarParity.compare(positioned, true, javaElements, javaSection, javaError);
    }
}
