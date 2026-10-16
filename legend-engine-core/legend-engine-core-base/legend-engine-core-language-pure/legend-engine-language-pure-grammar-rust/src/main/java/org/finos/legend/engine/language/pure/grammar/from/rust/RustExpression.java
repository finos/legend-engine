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

import org.finos.legend.engine.protocol.pure.dsl.graph.valuespecification.constant.classInstance.RootGraphFetchTree;
import org.finos.legend.engine.protocol.pure.m3.function.LambdaFunction;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecification;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * A standalone expression to parse: its kind (as {@code NativeGrammarParser.parseExpression} names it), the protocol
 * class of its result, its text and its position context.
 *
 * @param <T> the protocol class of the result
 */
final class RustExpression<T>
{
    final String kind;
    final Class<T> type;
    final String code;
    final String sourceId;
    final int lineOffset;
    final int columnOffset;

    private RustExpression(String kind, Class<T> type, String code, String sourceId, int lineOffset, int columnOffset)
    {
        this.kind = kind;
        this.type = type;
        this.code = code;
        this.sourceId = sourceId;
        this.lineOffset = lineOffset;
        this.columnOffset = columnOffset;
    }

    static RustExpression<LambdaFunction> lambda(String code, String sourceId, int lineOffset, int columnOffset)
    {
        return new RustExpression<>("lambda", LambdaFunction.class, code, sourceId, lineOffset, columnOffset);
    }

    static RustExpression<ValueSpecification> valueSpecification(String code, String sourceId, int lineOffset, int columnOffset)
    {
        return new RustExpression<>("valueSpecification", ValueSpecification.class, code, sourceId, lineOffset, columnOffset);
    }

    static RustExpression<RootGraphFetchTree> graphFetch(String code, String sourceId, int lineOffset, int columnOffset)
    {
        return new RustExpression<>("graphFetch", RootGraphFetchTree.class, code, sourceId, lineOffset, columnOffset);
    }

    /**
     * A stable name for the expression: SHA-256 of its kind, offsets and text, as {@link RustGrammarParity#hash}
     * names a section.
     */
    String hash()
    {
        return hash(this.kind, this.lineOffset, this.columnOffset, this.code);
    }

    static String hash(String type, int lineOffset, int columnOffset, String code)
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String key = type + "\n" + lineOffset + "\n" + columnOffset + "\n" + code;
            byte[] bytes = digest.digest(key.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 8; i++)
            {
                hex.append(String.format("%02x", bytes[i]));
            }
            return hex.toString();
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new IllegalStateException(e);
        }
    }
}
