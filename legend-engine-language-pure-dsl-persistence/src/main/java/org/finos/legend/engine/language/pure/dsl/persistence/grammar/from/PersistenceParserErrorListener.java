package org.finos.legend.engine.language.pure.dsl.persistence.grammar.from;

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.ParserErrorListener;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.PersistenceLexerGrammar;

import java.util.List;

public class PersistenceParserErrorListener extends ParserErrorListener
{
    public PersistenceParserErrorListener(ParseTreeWalkerSourceInformation walkerSourceInformation)
    {
        super(walkerSourceInformation);
    }

    @Override
    protected List<String> dereferenceTokens(List<Integer> expectedTokens)
    {
        return ListIterate.collect(expectedTokens, PersistenceLexerGrammar.VOCABULARY::getLiteralName);
    }
}
