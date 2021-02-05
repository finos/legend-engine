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
