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

package org.finos.legend.engine.language.pure.grammar.from.mapping;

import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.mapping.MappingParserGrammar;

public class MappingElementSourceCode
{
    public final String code;
    public final String name;
    public final ParseTreeWalkerSourceInformation mappingElementParseTreeWalkerSourceInformation;
    public final MappingParserGrammar.MappingElementContext mappingElementParserRuleContext;
    public final ParseTreeWalkerSourceInformation mappingParseTreeWalkerSourceInformation;

    public MappingElementSourceCode(String code, String name, ParseTreeWalkerSourceInformation mappingElementParseTreeWalkerSourceInformation, MappingParserGrammar.MappingElementContext mappingElementParserRuleContext, ParseTreeWalkerSourceInformation mappingParseTreeWalkerSourceInformation)
    {
        this.code = code;
        this.name = name;
        this.mappingElementParseTreeWalkerSourceInformation = mappingElementParseTreeWalkerSourceInformation;
        this.mappingElementParserRuleContext = mappingElementParserRuleContext;
        this.mappingParseTreeWalkerSourceInformation = mappingParseTreeWalkerSourceInformation;
    }
}
