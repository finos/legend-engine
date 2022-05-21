package org.finos.legend.engine.language.haskell.grammar.to;

import org.finos.legend.engine.protocol.haskell.metamodel.HaskellModule;

public class HaskellGrammarComposer {

    private HaskellGrammarComposer()
    {
    }

    public static HaskellGrammarComposer newInstance()
    {
        return new HaskellGrammarComposer();
    }

    public String renderModule(HaskellModule module)
    {
        return "";
    }

}
