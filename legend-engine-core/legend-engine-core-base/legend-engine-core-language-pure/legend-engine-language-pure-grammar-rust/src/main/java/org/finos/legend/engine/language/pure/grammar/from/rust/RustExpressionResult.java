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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.finos.legend.pure.rust.parser.NativeGrammarParser;

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
        String json = NativeGrammarParser.parseExpression(expression.kind, expression.code, expression.sourceId, expression.lineOffset, expression.columnOffset, returnSourceInfo);
        if (json == null)
        {
            return error(RustSectionResult.INTERNAL_ERROR, "the Rust parser returned no result", null);
        }
        try
        {
            JsonNode root = RustSectionResult.MAPPER.readTree(json);
            JsonNode error = root.get("error");
            if (error != null)
            {
                JsonNode si = error.get("sourceInformation");
                return error(error.path("kind").asText(RustSectionResult.INTERNAL_ERROR), error.path("message").asText(""), (si == null) ? null : RustSectionResult.MAPPER.treeToValue(si, SourceInformation.class));
            }
            String absentType = RustSectionResult.absentClassInstanceType(root.path("value"));
            if (absentType != null)
            {
                return error(RustSectionResult.UNSUPPORTED, "no protocol extension for class instance type '" + absentType + "'", null);
            }
            return new RustExpressionResult<>(RustSectionResult.MAPPER.treeToValue(root.path("value"), expression.type), null, null, null);
        }
        catch (InvalidTypeIdException e)
        {
            return error(RustSectionResult.UNSUPPORTED, "no protocol extension for type '" + e.getTypeId() + "'", null);
        }
        catch (JsonProcessingException e)
        {
            return error(RustSectionResult.INTERNAL_ERROR, "Rust protocol JSON does not match the engine protocol: " + e.getOriginalMessage(), null);
        }
    }

    private static <T> RustExpressionResult<T> error(String kind, String message, SourceInformation sourceInformation)
    {
        return new RustExpressionResult<>(null, kind, message, sourceInformation);
    }
}
