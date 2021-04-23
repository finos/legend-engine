// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.language.pure.grammar.from.extension;

import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.mapping.MappingParserGrammar.TestInputElementContext;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.InputData;

import java.util.function.BiFunction;

public interface MappingTestInputDataParser
{
    String getInputDataTypeName();

    InputData parse(TestInputElementContext inputDataContext, ParseTreeWalkerSourceInformation sourceInformation);

    static MappingTestInputDataParser newParser(String inputDataTypeName, BiFunction<? super TestInputElementContext, ? super ParseTreeWalkerSourceInformation, ? extends InputData> parser)
    {
        return new MappingTestInputDataParser()
        {
            @Override
            public String getInputDataTypeName()
            {
                return inputDataTypeName;
            }

            @Override
            public InputData parse(TestInputElementContext inputDataContext, ParseTreeWalkerSourceInformation sourceInformation)
            {
                return parser.apply(inputDataContext, sourceInformation);
            }
        };
    }
}
