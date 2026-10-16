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

/**
 * A whole model to parse ({@code PureGrammarParser.parseModel}): its code and its position context.
 */
final class RustModel
{
    final String code;
    final String sourceId;
    final int lineOffset;
    final int columnOffset;

    RustModel(String code, String sourceId, int lineOffset, int columnOffset)
    {
        this.code = code;
        this.sourceId = sourceId;
        this.lineOffset = lineOffset;
        this.columnOffset = columnOffset;
    }

    /**
     * A stable name for the model: SHA-256 of its offsets and code, as {@link RustGrammarParity#hash} names a section.
     */
    String hash()
    {
        return RustExpression.hash("model", this.lineOffset, this.columnOffset, this.code);
    }
}
