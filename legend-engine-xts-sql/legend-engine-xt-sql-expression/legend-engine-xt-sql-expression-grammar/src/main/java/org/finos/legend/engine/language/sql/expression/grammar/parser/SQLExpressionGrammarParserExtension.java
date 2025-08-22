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

package org.finos.legend.engine.language.sql.expression.grammar.parser;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.extension.EmbeddedPureParser;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtension;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtensions;
import org.finos.legend.engine.language.sql.expression.protocol.SQLExpressionProtocol;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;

public class SQLExpressionGrammarParserExtension implements PureGrammarParserExtension
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Query", "SQL");
    }

    @Override
    public Iterable<? extends EmbeddedPureParser> getExtraEmbeddedPureParsers()
    {
        return Lists.mutable.with(new EmbeddedPureParser()
        {
            @Override
            public String getType()
            {
                return "SQL";
            }

            @Override
            public Object parse(String code, ParseTreeWalkerSourceInformation walkerSourceInformation, SourceInformation sourceInformation, PureGrammarParserExtensions extensions)
            {
                return new SQLExpressionProtocol(code);
            }
        });
    }
}
