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
 * How a whole model ({@code PureGrammarParser.parseModel}) is parsed in the {@code on} and {@code shadow} modes (see
 * {@link RustGrammarMode}), read from {@value #PROPERTY} on every parse.
 */
public enum SectionParseStrategy
{
    /**
     * The engine's ANTLR grammar splits the code into sections, and each section type's parser parses its sections:
     * the Rust parser those it takes over (see {@link RustSectionParser}), the Java parser the others (the default).
     */
    ANTLR,
    /**
     * The Rust parser parses the whole model: it splits the code into sections as the engine does and parses them all,
     * with no ANTLR parsing in Java (see {@link RustModelParser}). When it can't, as when a section holds grammar it
     * doesn't parse, the model is parsed as with {@link #ANTLR}, unless {@value RustGrammarMode#FALLBACK_PROPERTY} is
     * {@code false}.
     */
    RUST;

    /**
     * System property selecting the strategy: {@code antlr} or {@code rust}.
     */
    public static final String PROPERTY = "legend.grammar.rust.sectionParseStrategy";

    /**
     * The strategy currently selected by {@value #PROPERTY}.
     *
     * @return the selected strategy, {@link #ANTLR} when unset
     */
    public static SectionParseStrategy current()
    {
        String value = System.getProperty(PROPERTY);
        return (value == null || value.isEmpty()) ? ANTLR : valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
