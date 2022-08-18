// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.language.pure.dsl.mastery.grammar.from;

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.ParserErrorListener;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.MasteryLexerGrammar;
//import org.finos.legend.engine.language.pure.grammar.from.antlr4.MasteryLexerGrammar;

import java.util.List;
import java.util.Objects;

public class MasteryParserErrorListener extends ParserErrorListener
{
    public MasteryParserErrorListener(ParseTreeWalkerSourceInformation walkerSourceInformation)
    {
        super(walkerSourceInformation);
    }

    @Override
    protected List<String> dereferenceTokens(List<Integer> expectedTokens)
    {
        return ListIterate.collect(expectedTokens, MasteryLexerGrammar.VOCABULARY::getLiteralName).select(Objects::nonNull);
    }
}
