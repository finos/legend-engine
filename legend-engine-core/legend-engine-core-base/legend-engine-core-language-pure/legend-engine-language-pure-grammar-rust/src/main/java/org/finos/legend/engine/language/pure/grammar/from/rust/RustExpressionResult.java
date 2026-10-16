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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.classInstance.UnknownClassInstanceTypeException;
import org.finos.legend.pure.rust.parser.NativeGrammarParser;

import java.io.IOException;

/**
 * The Rust parser's result for one standalone expression, read from the JSON
 * {@link NativeGrammarParser#parseExpression} returns and deserialized into a protocol object.
 *
 * @param <T> the protocol class of the result
 */
final class RustExpressionResult<T>
{
    final T value;
    final String errorKind;
    final String errorMessage;
    final SourceInformation errorSourceInformation;

    private RustExpressionResult(T value, String errorKind, String errorMessage, SourceInformation errorSourceInformation)
    {
        this.value = value;
        this.errorKind = errorKind;
        this.errorMessage = errorMessage;
        this.errorSourceInformation = errorSourceInformation;
    }

    boolean isError()
    {
        return this.errorKind != null;
    }

    boolean isUnsupported()
    {
        return RustSectionResult.UNSUPPORTED.equals(this.errorKind);
    }

    /**
     * Parses {@code expression} with the Rust parser.
     */
    static <T> RustExpressionResult<T> parse(RustExpression<T> expression, boolean returnSourceInfo)
    {
        return read(nativeParse(expression), expression.type, returnSourceInfo);
    }

    /**
     * The JSON {@link NativeGrammarParser#parseExpression} returns for {@code expression}.
     */
    static byte[] nativeParse(RustExpression<?> expression)
    {
        return NativeGrammarParser.parseExpression(expression.kind, expression.code, expression.sourceId, expression.lineOffset, expression.columnOffset);
    }

    /**
     * Reads the JSON {@link NativeGrammarParser#parseExpression} returns, its value as a {@code type}.
     */
    static <T> RustExpressionResult<T> read(byte[] json, Class<T> type, boolean returnSourceInfo)
    {
        if (json == null)
        {
            return error(RustSectionResult.INTERNAL_ERROR, "the Rust parser returned no result", null);
        }
        ObjectMapper mapper = RustSectionResult.mapper(returnSourceInfo);
        try (JsonParser parser = mapper.getFactory().createParser(json))
        {
            T value = null;
            for (String field = RustSectionResult.firstField(parser); field != null; field = RustSectionResult.nextField(parser))
            {
                if ("error".equals(field))
                {
                    RustSectionResult.RustError error = RustSectionResult.RustError.read(parser);
                    return error(error.kind, error.message, error.sourceInformation);
                }
                if ("value".equals(field))
                {
                    value = RustSectionResult.readValue(parser, mapper, type);
                }
                else
                {
                    parser.skipChildren();
                }
            }
            return new RustExpressionResult<>(value, null, null, null);
        }
        catch (UnknownClassInstanceTypeException e)
        {
            return error(RustSectionResult.UNSUPPORTED, RustSectionResult.unsupported(e), null);
        }
        catch (InvalidTypeIdException e)
        {
            return error(RustSectionResult.UNSUPPORTED, RustSectionResult.unsupported(e), null);
        }
        catch (IOException e)
        {
            return error(RustSectionResult.INTERNAL_ERROR, RustSectionResult.divergence(e), null);
        }
    }

    private static <T> RustExpressionResult<T> error(String kind, String message, SourceInformation sourceInformation)
    {
        return new RustExpressionResult<>(null, kind, message, sourceInformation);
    }
}
