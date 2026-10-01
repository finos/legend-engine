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
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.rust.parser.NativeGrammarParser;

/**
 * Whole models parsed according to {@link RustGrammarMode} and {@link SectionParseStrategy}, taking over from the
 * engine's own parser, which it keeps:
 *
 * <ul>
 *     <li>{@code off}, or the {@code antlr} strategy: the engine's parser's result, which splits the code into
 *     sections with its ANTLR grammar and parses each with its section type's parser, the Rust-backed ones included
 *     (see {@link RustSectionParser});</li>
 *     <li>{@code on} with the {@code rust} strategy: the Rust parser's result, the code split and every section
 *     parsed in Rust; when it fails, or reads a section of a type not taken over, the engine's parser's, unless
 *     {@value RustGrammarMode#FALLBACK_PROPERTY} is {@code false} (see {@link RustGrammarMode#fallback()});</li>
 *     <li>{@code shadow} with the {@code rust} strategy: the engine's parser's result, after comparing it with the
 *     Rust parser's (see {@link RustGrammarParity}).</li>
 * </ul>
 */
final class RustModelParser implements ModelParser
{
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
        if (mode != RustGrammarMode.ON)
        {
            // In `on` mode, the engine's parser's result holds the Rust parser's sections: not a Java result.
            RustGrammarCorpus.recordModel(model, positions -> this.engineParser.parseModel(code, sourceId, lineOffset, columnOffset, positions));
        }
        if (mode == RustGrammarMode.OFF || SectionParseStrategy.current() == SectionParseStrategy.ANTLR)
        {
            return this.engineParser.parseModel(code, sourceId, lineOffset, columnOffset, returnSourceInfo);
        }
        return (mode == RustGrammarMode.ON) ? parseWithRust(model, returnSourceInfo) : parseInShadow(model, returnSourceInfo);
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
            result = RustModelResult.parse(model, returnSourceInfo, this.sectionTypes.castToList());
        }
        catch (RuntimeException e)
        {
            if (RustGrammarMode.fallback())
            {
                return parseWithEngine(model, returnSourceInfo);
            }
            throw e;
        }
        if (result.isError())
        {
            // As for sections: the engine's parser decides, even if that means parsing twice.
            if (RustGrammarMode.fallback())
            {
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
        RustGrammarParity.compareModel(model, returnSourceInfo, this.sectionTypes.castToList(), javaModel, javaError);
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
        RustGrammarParity.compareModel(model, true, this.sectionTypes.castToList(), javaModel, javaError);
    }

    private PureModelContextData parseWithEngine(RustModel model, boolean returnSourceInfo)
    {
        return this.engineParser.parseModel(model.code, model.sourceId, model.lineOffset, model.columnOffset, returnSourceInfo);
    }
}
