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

import org.antlr.v4.runtime.tree.TerminalNode;
import org.eclipse.collections.api.block.function.Function2;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.mapping.MappingParserGrammar;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.MappingInclude;


public interface MappingIncludeParser
{

    String getMappingIncludeType();

    MappingInclude parse(MappingParserGrammar.IncludeMappingContext ctx, ParseTreeWalkerSourceInformation walkerSourceInformation);

    static String parseIncludeType(TerminalNode includeDispatch)
    {
        return includeDispatch == null ? "mapping" : includeDispatch.getText().split(" ")[1];
    }

    static MappingIncludeParser newParser(String mappingIncludeType, Function2<MappingParserGrammar.IncludeMappingContext, ParseTreeWalkerSourceInformation, MappingInclude> parser)
    {
        return new MappingIncludeParser()
        {
            @Override
            public String getMappingIncludeType()
            {
                return mappingIncludeType;
            }

            @Override
            public MappingInclude parse(MappingParserGrammar.IncludeMappingContext ctx, ParseTreeWalkerSourceInformation walkerSourceInformation)
            {
                return parser.apply(ctx, walkerSourceInformation);
            }
        };
    }
}