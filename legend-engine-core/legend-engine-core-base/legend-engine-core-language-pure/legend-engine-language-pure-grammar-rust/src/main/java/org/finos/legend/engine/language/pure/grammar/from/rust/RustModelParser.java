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

import org.eclipse.collections.api.list.ImmutableList;
import org.finos.legend.engine.language.pure.grammar.from.extension.ModelParser;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.rust.parser.NativeGrammarParser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Whole models ({@code PureGrammarParser.parseModel}) parsed according to {@link RustGrammarMode}, taking over from the
 * engine's own parser, which it keeps. The engine's parser splits the code into sections with its ANTLR grammar and
 * parses each with its section type's parser, the Rust-backed ones included (see {@link RustSectionParser}):
 *
 * <ul>
 *     <li>{@code off}: the engine's parser's result;</li>
 *     <li>{@code on}: the Rust parser's result, the code split and every section parsed in Rust, with no ANTLR parsing
 *     in Java; when it fails, the engine's parser's, unless {@value RustGrammarMode#FALLBACK_PROPERTY} is
 *     {@code false} (see {@link RustGrammarMode#fallback()});</li>
 *     <li>{@code shadow}: the engine's parser's result, after comparing it with the Rust parser's (see
 *     {@link RustGrammarParity}); the engine's parser compares each section too.</li>
 * </ul>
 *
 * <p>A model holding a section of a type the Rust parser doesn't take over (see {@link RustGrammarParserExtension})
 * is the engine's parser's, in every mode, as such a section is; {@code on} mode logs the type (see
 * {@link RustGrammarFallback}).</p>
 */
final class RustModelParser implements ModelParser
{
    /**
     * A section header, as the engine's {@code CodeLexerGrammar} reads one: a line break, {@code ###} and a name.
     */
    private static final Pattern SECTION_HEADER = Pattern.compile("\n###([A-Za-z0-9_][A-Za-z0-9_$]*)");

    /**
     * What the engine's parser puts in front of the code: code before any header is a {@code Pure} section.
     */
    private static final String DEFAULT_SECTION_BEGIN = "\n###Pure\n";

    private final ImmutableList<String> sectionTypes;
    private final ModelParser engineParser;

    /**
     * @param sectionTypes the section types taken over
     * @param engineParser the engine's parser
     */
    RustModelParser(ImmutableList<String> sectionTypes, ModelParser engineParser)
    {
        this.sectionTypes = sectionTypes;
        this.engineParser = engineParser;
    }

    @Override
    public PureModelContextData parseModel(String code, String sourceId, int lineOffset, int columnOffset, boolean returnSourceInfo)
    {
        RustModel model = new RustModel(code, sourceId, lineOffset, columnOffset);
        RustGrammarMode mode = RustGrammarMode.current();
        if (mode == RustGrammarMode.OFF)
        {
            return parseWithEngine(model, returnSourceInfo);
        }
        Matcher notTakenOver = sectionNotTakenOver(code);
        if (notTakenOver != null)
        {
            if (mode == RustGrammarMode.ON)
            {
                RustGrammarFallback.sectionTypeNotTakenOver(sourceId, notTakenOver.group(1), position(model, notTakenOver));
            }
            return parseWithEngine(model, returnSourceInfo);
        }
        return (mode == RustGrammarMode.ON) ? parseWithRust(model, returnSourceInfo) : parseInShadow(model, returnSourceInfo);
    }

    /**
     * The header of the first section of {@code code} whose type is not taken over, if any, the headers read as the
     * engine reads them, behind the default one.
     */
    private Matcher sectionNotTakenOver(String code)
    {
        Matcher header = SECTION_HEADER.matcher(DEFAULT_SECTION_BEGIN + code);
        while (header.find())
        {
            if (!this.sectionTypes.contains(header.group(1)))
            {
                return header;
            }
        }
        return null;
    }

    /**
     * The position of a header {@link #sectionNotTakenOver} found, in the document: none for the default header,
     * which is not in the code.
     */
    private static SourceInformation position(RustModel model, Matcher header)
    {
        if (header.start() == 0)
        {
            return null;
        }
        // The header follows its line break; the default header and its line break are two lines.
        String text = DEFAULT_SECTION_BEGIN + model.code;
        int codeLine = (int) text.substring(0, header.start()).chars().filter(c -> c == '\n').count();
        int line = codeLine + model.lineOffset;
        int column = 1 + ((codeLine == 1) ? model.columnOffset : 0);
        return new SourceInformation(model.sourceId, line, column, line, column + header.group().length() - 2);
    }

    private PureModelContextData parseWithRust(RustModel model, boolean returnSourceInfo)
    {
        if (!NativeGrammarParser.isAvailable())
        {
            throw new IllegalStateException(RustGrammarMode.PROPERTY + "=on, but the Rust parser is not available: " + NativeGrammarParser.getLoadFailure());
        }
        RustModelResult result;
        try
        {
            result = RustModelResult.parse(model, returnSourceInfo);
        }
        catch (RuntimeException e)
        {
            if (RustGrammarMode.fallback())
            {
                RustGrammarFallback.exception("model", model.sourceId, e);
                return parseWithEngine(model, returnSourceInfo);
            }
            throw e;
        }
        if (result.isError())
        {
            // As for sections: the engine's parser decides, even if that means parsing twice.
            if (RustGrammarMode.fallback())
            {
                RustGrammarFallback.error("model", model.sourceId, result.errorKind, result.errorMessage, result.errorSourceInformation);
                return parseWithEngine(model, returnSourceInfo);
            }
            String message = RustSectionResult.PARSER_ERROR.equals(result.errorKind) ? result.errorMessage : ("Rust parser " + result.errorKind + " error: " + result.errorMessage);
            throw new EngineException(message, result.errorSourceInformation, EngineErrorType.PARSER);
        }
        return result.toPureModelContextData();
    }

    private PureModelContextData parseInShadow(RustModel model, boolean returnSourceInfo)
    {
        PureModelContextData javaModel = null;
        RuntimeException javaError = null;
        try
        {
            javaModel = parseWithEngine(model, returnSourceInfo);
        }
        catch (RuntimeException e)
        {
            javaError = e;
        }
        RustGrammarParity.compareModel(model, returnSourceInfo, javaModel, javaError);
        if (!returnSourceInfo)
        {
            // The roundtrip suites parse without source information; compare positions too.
            compareWithPositions(model);
        }
        if (javaError != null)
        {
            throw javaError;
        }
        return javaModel;
    }

    private void compareWithPositions(RustModel model)
    {
        PureModelContextData javaModel = null;
        RuntimeException javaError = null;
        try
        {
            javaModel = parseWithEngine(model, true);
        }
        catch (RuntimeException e)
        {
            javaError = e;
        }
        RustGrammarParity.compareModel(model, true, javaModel, javaError);
    }

    private PureModelContextData parseWithEngine(RustModel model, boolean returnSourceInfo)
    {
        return this.engineParser.parseModel(model.code, model.sourceId, model.lineOffset, model.columnOffset, returnSourceInfo);
    }
}
