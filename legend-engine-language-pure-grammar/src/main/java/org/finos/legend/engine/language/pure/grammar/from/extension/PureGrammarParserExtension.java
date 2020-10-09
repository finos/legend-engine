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

package org.finos.legend.engine.language.pure.grammar.from.extension;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function3;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserContext;
import org.finos.legend.engine.language.pure.grammar.from.SectionSourceCode;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.mapping.MappingParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.connection.ConnectionValueSourceCode;
import org.finos.legend.engine.language.pure.grammar.from.mapping.MappingElementSourceCode;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.InputData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.Section;
import org.finos.legend.engine.shared.core.function.Procedure3;

import java.util.ArrayList;
import java.util.List;

public interface PureGrammarParserExtension
{
    default List<Function3<SectionSourceCode, PureModelContextData, PureGrammarParserContext, Section>> getExtraSectionParsers()
    {
        return new ArrayList<>();
    }

    // Mapping
    default List<Function<ConnectionValueSourceCode, Connection>> getExtraConnectionParsers()
    {
        return new ArrayList<>();
    }

    default List<Procedure3<MappingElementSourceCode, Mapping, PureGrammarParserContext>> getExtraMappingElementParsers()
    {
        return new ArrayList<>();
    }

    default List<Function3<String, MappingParserGrammar.TestInputElementContext, ParseTreeWalkerSourceInformation, InputData>> getExtraMappingTestInputDataParsers()
    {
        return new ArrayList<>();
    }
}
