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

import java.util.Locale;

/**
 * Which parser handles {@code ###Pure} sections, read from {@value #PROPERTY} on every parse.
 */
public enum RustGrammarMode
{
    /**
     * The Java parser only (the default).
     */
    OFF,
    /**
     * Both parsers; the results are compared and the Java result is returned.
     */
    SHADOW,
    /**
     * The Rust parser; when it fails, by default, the Java parser (see {@link #fallback()}).
     */
    ON;

    /**
     * System property selecting the mode: {@code off}, {@code shadow} or {@code on}.
     */
    public static final String PROPERTY = "legend.grammar.rust.mode";

    /**
     * System property deciding what {@link #ON} does when the Rust parser fails: {@code true}, the default, parses the
     * section again with the Java parser; {@code false} reports the Rust parser's error.
     */
    public static final String FALLBACK_PROPERTY = "legend.grammar.rust.fallback";

    /**
     * The mode currently selected by {@value #PROPERTY}.
     *
     * @return the selected mode, {@link #OFF} when unset
     */
    public static RustGrammarMode current()
    {
        String value = System.getProperty(PROPERTY);
        return (value == null || value.isEmpty()) ? OFF : valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    /**
     * Whether {@link #ON} parses a section again with the Java parser when the Rust parser fails, whatever the failure:
     * input it doesn't support, a syntax error, or a result the engine can't read. Read from
     * {@value #FALLBACK_PROPERTY} on every parse.
     *
     * @return {@code true} unless {@value #FALLBACK_PROPERTY} is {@code false}
     */
    public static boolean fallback()
    {
        String value = System.getProperty(FALLBACK_PROPERTY);
        return value == null || !"false".equalsIgnoreCase(value.trim());
    }
}
