package org.finos.legend.engine.language.pure.grammar.from.extension;

import org.eclipse.collections.api.block.function.Function3;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserContext;
import org.finos.legend.engine.language.pure.grammar.from.SectionSourceCode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.Section;

import java.util.function.Consumer;

public interface SectionParser
{
    String getSectionTypeName();

    Section parse(SectionSourceCode sectionSourceCode, Consumer<PackageableElement> elementConsumer, PureGrammarParserContext pureGrammarParserContext);

    static SectionParser newParser(String name, Function3<SectionSourceCode, Consumer<PackageableElement>, PureGrammarParserContext, Section> parser)
    {
        return new SectionParser()
        {
            @Override
            public String getSectionTypeName()
            {
                return name;
            }

            @Override
            public Section parse(SectionSourceCode sectionSourceCode, Consumer<PackageableElement> elementConsumer, PureGrammarParserContext pureGrammarParserContext)
            {
                return parser.value(sectionSourceCode, elementConsumer, pureGrammarParserContext);
            }
        };
    }
}
