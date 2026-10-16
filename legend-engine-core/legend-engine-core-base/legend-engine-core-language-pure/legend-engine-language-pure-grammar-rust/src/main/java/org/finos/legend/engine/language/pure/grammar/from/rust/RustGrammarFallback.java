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

import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logs why the {@code on} mode falls back to the Java parser (see {@link RustGrammarMode#fallback()}), which would
 * otherwise be silent: the Rust parser's error, or its exception, at {@code WARN}, with what was parsed (a whole model,
 * a section of some type or a standalone expression of some kind), its source id, and the error's position.
 */
final class RustGrammarFallback
{
    private static final Logger LOGGER = LoggerFactory.getLogger(RustGrammarFallback.class);

    private RustGrammarFallback()
    {
    }

    /**
     * The Rust parser reported an error on {@code input}.
     *
     * @param input    what was parsed, e.g. {@code model}, {@code section Mapping} or {@code lambda}
     * @param sourceId its source id
     * @param kind     the error's kind: {@code parser}, {@code unsupported} or {@code internal}
     * @param message  the error's message
     * @param position the error's position, if any
     */
    static void error(String input, String sourceId, String kind, String message, SourceInformation position)
    {
        // A parser error is a syntax error: "Rust parser parser error" would say less.
        String error = RustSectionResult.PARSER_ERROR.equals(kind) ? "syntax" : kind;
        LOGGER.warn("Rust parser {} error, parsing with Java instead: {} [{}]", error, message, where(input, sourceId, position));
    }

    /**
     * A model holds a section of a type the Rust parser doesn't take over: the engine's parser parses the model,
     * section by section, the Rust parser taking over the sections of the other types.
     *
     * @param sourceId    the model's source id
     * @param sectionType the section's type
     * @param position    its header's position, if it is in the code (not the default {@code ###Pure})
     */
    static void sectionTypeNotTakenOver(String sourceId, String sectionType, SourceInformation position)
    {
        LOGGER.warn("Rust parser does not take over section type '{}', parsing the model section by section instead [{}]", sectionType, where("model", sourceId, position));
    }

    /**
     * The Rust parser, or the reading of its result, failed on {@code input}.
     */
    static void exception(String input, String sourceId, RuntimeException exception)
    {
        LOGGER.warn("Rust parser failed, parsing with Java instead [{}]", where(input, sourceId, null), exception);
    }

    private static String where(String input, String sourceId, SourceInformation position)
    {
        StringBuilder where = new StringBuilder(input);
        if (sourceId != null && !sourceId.isEmpty())
        {
            where.append(", ").append(sourceId);
        }
        if (position != null)
        {
            where.append(" at ").append(position.startLine).append(':').append(position.startColumn);
        }
        return where.toString();
    }
}
