package org.finos.legend.engine.language.pure.grammar.from.extension;

import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserContext;
import org.finos.legend.engine.language.pure.grammar.from.mapping.MappingElementSourceCode;

import java.util.function.BiFunction;

public interface MappingElementParser
{
    String getElementTypeName();

    Object parse(MappingElementSourceCode mappingElementSourceCode, PureGrammarParserContext parserContext);

    static MappingElementParser newParser(String elementName, BiFunction<? super MappingElementSourceCode, ? super PureGrammarParserContext, ?> parser)
    {
        return new MappingElementParser()
        {
            @Override
            public String getElementTypeName()
            {
                return elementName;
            }

            @Override
            public Object parse(MappingElementSourceCode mappingElementSourceCode, PureGrammarParserContext parserContext)
            {
                return parser.apply(mappingElementSourceCode, parserContext);
            }
        };
    }
}
