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
import org.finos.legend.engine.language.pure.grammar.from.extension.SectionParser;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.DefaultCodeSection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.ImportAwareCodeSection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.Section;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.rust.parser.NativeGrammarParser;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A section type's parser backed by the Rust parser according to {@link RustGrammarMode}, taking over from the Java
 * parser the engine would otherwise use (see {@link RustGrammarParserExtension}), which it keeps:
 *
 * <ul>
 *     <li>{@code off}: the Java parser's result, exactly as without this parser;</li>
 *     <li>{@code shadow}: the Java parser's result, after comparing it with the Rust parser's
 *     (see {@link RustGrammarParity});</li>
 *     <li>{@code on}: the Rust parser's result; when it fails, the Java parser's, unless
 *     {@value RustGrammarMode#FALLBACK_PROPERTY} is {@code false} (see {@link RustGrammarMode#fallback()}).</li>
 * </ul>
 */
public class RustSectionParser implements SectionParser
{
    private final String sectionType;
    private final SectionParser javaParser;

    /**
     * @param sectionType the section type parsed
     * @param javaParser  the Java parser for that section type
     */
    public RustSectionParser(String sectionType, SectionParser javaParser)
    {
        this.sectionType = sectionType;
        this.javaParser = javaParser;
    }

    @Override
    public String getSectionTypeName()
    {
        return this.sectionType;
    }

    @Override
    public Section parse(SectionSourceCode code, Consumer<PackageableElement> elementConsumer, PureGrammarParserContext context)
    {
        RustGrammarCorpus.record(code, context, this.javaParser);
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
                return this.javaParser.parse(code, elementConsumer, context);
            }
        }
    }

    /**
     * The Java parser's parse of a section, with source information whatever the section's walker asks.
     */
    static Section parseWithPositions(SectionParser javaParser, SectionSourceCode code, Consumer<PackageableElement> elementConsumer, PureGrammarParserContext context)
    {
        return javaParser.parse(withPositions(code), elementConsumer, context);
    }

    private static SectionSourceCode withPositions(SectionSourceCode code)
    {
        ParseTreeWalkerSourceInformation walker = new ParseTreeWalkerSourceInformation.Builder(code.walkerSourceInformation).withReturnSourceInfo(true).build();
        return new SectionSourceCode(code.code, code.sectionType, code.sourceInformation, walker);
    }

    private Section parseWithRust(SectionSourceCode code, Consumer<PackageableElement> elementConsumer, PureGrammarParserContext context)
    {
        if (!NativeGrammarParser.isAvailable())
        {
            throw new IllegalStateException(RustGrammarMode.PROPERTY + "=on, but the Rust parser is not available: " + NativeGrammarParser.getLoadFailure());
        }
        RustSectionResult result;
        try
        {
            result = RustSectionResult.parse(code, code.walkerSourceInformation.getReturnSourceInfo());
        }
        catch (RuntimeException e)
        {
            if (RustGrammarMode.fallback())
            {
                return this.javaParser.parse(code, elementConsumer, context);
            }
            throw e;
        }
        if (result.isError())
        {
            // Unsupported input, a syntax error or a result this engine can't read: the Java parser decides, even if
            // that means parsing twice.
            if (RustGrammarMode.fallback())
            {
                return this.javaParser.parse(code, elementConsumer, context);
            }
            String message = RustSectionResult.PARSER_ERROR.equals(result.errorKind) ? result.errorMessage : ("Rust parser " + result.errorKind + " error: " + result.errorMessage);
            throw new EngineException(message, result.errorSourceInformation, EngineErrorType.PARSER);
        }
        result.elements.forEach(elementConsumer);
        // A grammar without imports (`Data`) makes a `DefaultCodeSection`, as in Java.
        Section section;
        if (result.imports == null)
        {
            section = new DefaultCodeSection();
        }
        else
        {
            ImportAwareCodeSection importAware = new ImportAwareCodeSection();
            importAware.imports = new ArrayList<>(result.imports);
            section = importAware;
        }
        section.parserName = this.sectionType;
        section.sourceInformation = code.sourceInformation;
        result.elements.forEach(e -> section.elements.add(e.getPath()));
        return section;
    }

    private Section parseInShadow(SectionSourceCode code, Consumer<PackageableElement> elementConsumer, PureGrammarParserContext context)
    {
        // Collect privately: the consumer must see Java's elements exactly once, after the comparison.
        List<PackageableElement> javaElements = new ArrayList<>();
        Section javaSection = null;
        RuntimeException javaError = null;
        try
        {
            javaSection = this.javaParser.parse(code, javaElements::add, context);
        }
        catch (RuntimeException e)
        {
            javaError = e;
        }
        RustGrammarParity.compare(code, code.walkerSourceInformation.getReturnSourceInfo(), javaElements, javaSection, javaError);
        if (!code.walkerSourceInformation.getReturnSourceInfo())
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

    private void compareWithPositions(SectionSourceCode code, PureGrammarParserContext context)
    {
        SectionSourceCode positioned = withPositions(code);
        List<PackageableElement> javaElements = new ArrayList<>();
        Section javaSection = null;
        RuntimeException javaError = null;
        try
        {
            javaSection = this.javaParser.parse(positioned, javaElements::add, context);
        }
        catch (RuntimeException e)
        {
            javaError = e;
        }
        RustGrammarParity.compare(positioned, true, javaElements, javaSection, javaError);
    }
}
