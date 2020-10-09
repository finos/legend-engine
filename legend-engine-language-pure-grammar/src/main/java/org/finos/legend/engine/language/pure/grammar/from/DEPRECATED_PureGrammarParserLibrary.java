// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.language.pure.grammar.from;

import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

// FIXME: remove this when we move everything to extension
@Deprecated
public class DEPRECATED_PureGrammarParserLibrary
{
    private final ImmutableMap<String, DEPRECATED_SectionGrammarParser> parsers;

    public DEPRECATED_PureGrammarParserLibrary(Iterable<? extends DEPRECATED_SectionGrammarParser> parsers)
    {
        this.parsers = indexParsersByName(parsers);
    }

    private static ImmutableMap<String, DEPRECATED_SectionGrammarParser> indexParsersByName(Iterable<? extends DEPRECATED_SectionGrammarParser> parsers)
    {
        MutableMap<String, DEPRECATED_SectionGrammarParser> index = Maps.mutable.empty();
        for (DEPRECATED_SectionGrammarParser parser : parsers)
        {
            DEPRECATED_SectionGrammarParser old = index.put(parser.getName(), parser);
            if ((old != null) && (parser != old))
            {
                throw new IllegalArgumentException("Duplicate parsers with name: " + parser.getName());
            }
        }
        return index.toImmutable();
    }

    public DEPRECATED_SectionGrammarParser getParser(String name, SourceInformation sourceInformation)
    {
        return this.parsers.get(name);
    }
}
