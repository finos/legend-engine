package org.finos.legend.engine.language.haskell.grammar.from;

import org.finos.legend.engine.protocol.haskell.metamodel.HaskellModule;

public class HaskellGrammarParser {

    private HaskellGrammarParser()
    {
    }

    public static HaskellGrammarParser newInstance()
    {
        return new HaskellGrammarParser();
    }

    public HaskellModule parseModule(String module) {
        return new HaskellModule();
    }

}
