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

import org.finos.legend.engine.language.pure.grammar.from.extension.ExpressionParser;
import org.finos.legend.engine.protocol.pure.dsl.graph.valuespecification.constant.classInstance.RootGraphFetchTree;
import org.finos.legend.engine.protocol.pure.m3.function.LambdaFunction;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.rust.parser.NativeGrammarParser;

import java.util.function.Function;

/**
 * Standalone expressions (lambdas, value specifications, graph fetch trees) parsed by the Rust parser according to
 * {@link RustGrammarMode}, taking over from the engine's own parser, which it keeps; the modes work as for sections
 * (see {@link RustSectionParser}).
 */
final class RustExpressionParser implements ExpressionParser
{
    private final ExpressionParser javaParser;

    RustExpressionParser(ExpressionParser javaParser)
    {
        this.javaParser = javaParser;
    }

    @Override
    public LambdaFunction parseLambda(String code, String sourceId, int lineOffset, int columnOffset, boolean returnSourceInfo)
    {
        return parse(RustExpression.lambda(code, sourceId, lineOffset, columnOffset), returnSourceInfo,
                positions -> this.javaParser.parseLambda(code, sourceId, lineOffset, columnOffset, positions));
    }

    @Override
    public ValueSpecification parseValueSpecification(String code, String sourceId, int lineOffset, int columnOffset, boolean returnSourceInfo)
    {
        return parse(RustExpression.valueSpecification(code, sourceId, lineOffset, columnOffset), returnSourceInfo,
                positions -> this.javaParser.parseValueSpecification(code, sourceId, lineOffset, columnOffset, positions));
    }

    @Override
    public RootGraphFetchTree parseGraphFetch(String code, String sourceId, int lineOffset, int columnOffset, boolean returnSourceInfo)
    {
        return parse(RustExpression.graphFetch(code, sourceId, lineOffset, columnOffset), returnSourceInfo,
                positions -> this.javaParser.parseGraphFetch(code, sourceId, lineOffset, columnOffset, positions));
    }

    /**
     * Parses {@code expression}; {@code java} is the Java parser's parse of it, with or without source information.
     */
    private static <T> T parse(RustExpression<T> expression, boolean returnSourceInfo, Function<Boolean, T> java)
    {
        RustGrammarCorpus.recordExpression(expression, java);
        switch (RustGrammarMode.current())
        {
            case ON:
            {
                return parseWithRust(expression, returnSourceInfo, java);
            }
            case SHADOW:
            {
                return parseInShadow(expression, returnSourceInfo, java);
            }
            case OFF:
            default:
            {
                return java.apply(returnSourceInfo);
            }
        }
    }

    private static <T> T parseWithRust(RustExpression<T> expression, boolean returnSourceInfo, Function<Boolean, T> java)
    {
        if (!NativeGrammarParser.isAvailable())
        {
            throw new IllegalStateException(RustGrammarMode.PROPERTY + "=on, but the Rust parser is not available: " + NativeGrammarParser.getLoadFailure());
        }
        RustExpressionResult<T> result;
        try
        {
            result = RustExpressionResult.parse(expression, returnSourceInfo);
        }
        catch (RuntimeException e)
        {
            if (RustGrammarMode.fallback())
            {
                return java.apply(returnSourceInfo);
            }
            throw e;
        }
        if (result.isError())
        {
            // As for sections: the Java parser decides, even if that means parsing twice.
            if (RustGrammarMode.fallback())
            {
                return java.apply(returnSourceInfo);
            }
            String message = RustSectionResult.PARSER_ERROR.equals(result.errorKind) ? result.errorMessage : ("Rust parser " + result.errorKind + " error: " + result.errorMessage);
            throw new EngineException(message, result.errorSourceInformation, EngineErrorType.PARSER);
        }
        return result.value;
    }

    private static <T> T parseInShadow(RustExpression<T> expression, boolean returnSourceInfo, Function<Boolean, T> java)
    {
        T javaValue = null;
        RuntimeException javaError = null;
        try
        {
            javaValue = java.apply(returnSourceInfo);
        }
        catch (RuntimeException e)
        {
            javaError = e;
        }
        RustGrammarParity.compareExpression(expression, returnSourceInfo, javaValue, javaError);
        if (!returnSourceInfo)
        {
            // Compare positions too.
            compareWithPositions(expression, java);
        }
        if (javaError != null)
        {
            throw javaError;
        }
        return javaValue;
    }

    private static <T> void compareWithPositions(RustExpression<T> expression, Function<Boolean, T> java)
    {
        T javaValue = null;
        RuntimeException javaError = null;
        try
        {
            javaValue = java.apply(true);
        }
        catch (RuntimeException e)
        {
            javaError = e;
        }
        RustGrammarParity.compareExpression(expression, true, javaValue, javaError);
    }
}
