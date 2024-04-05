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

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

public class SourceCodeParserInfo
{
    public final String code;
    public final CharStream input;
    public final Lexer lexer;
    public final Parser parser;
    public final ParserRuleContext rootContext;
    public final SourceInformation sourceInformation;
    public final ParseTreeWalkerSourceInformation walkerSourceInformation;

    public SourceCodeParserInfo(String code, CharStream input, SourceInformation sourceInformation, ParseTreeWalkerSourceInformation walkerSourceInformation, Lexer lexer, Parser parser, ParserRuleContext rootContext)
    {
        this.code = code;
        this.input = input;
        this.sourceInformation = sourceInformation;
        this.walkerSourceInformation = walkerSourceInformation;
        this.lexer = lexer;
        this.parser = parser;
        this.rootContext = rootContext;
    }
}
