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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtension;
import org.finos.legend.engine.language.pure.grammar.from.extension.SectionParser;

/**
 * Registers the Rust-backed {@code ###Pure} section parser. {@code PureGrammarParser} consults extension section
 * parsers before its built-in ones, so this takes over {@code ###Pure}; in the default mode ({@code off}) it parses
 * exactly as the built-in parser does. The mode is read on every parse (see {@link RustGrammarMode}), because the
 * extensions themselves are loaded once.
 */
public class RustGrammarParserExtension implements PureGrammarParserExtension
{
    @Override
    public MutableList<String> group()
    {
        return Lists.mutable.with("Core", "Rust");
    }

    @Override
    public Iterable<? extends SectionParser> getExtraSectionParsers()
    {
        return Lists.immutable.with(new RustPureSectionParser());
    }
}
