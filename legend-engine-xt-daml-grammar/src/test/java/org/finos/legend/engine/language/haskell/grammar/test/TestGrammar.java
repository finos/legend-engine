package org.finos.legend.engine.language.haskell.grammar.test;

import org.finos.legend.engine.language.daml.grammar.to.DamlGrammarComposer;
import org.finos.legend.engine.language.haskell.grammar.from.HaskellGrammarParser;
import org.finos.legend.engine.protocol.haskell.metamodel.HaskellModule;
import org.junit.Assert;
import org.junit.Test;

public class TestGrammar {

    @Test
    public void testDamlSyntaxToString()
    {
        String text = "module Gs.Finance\n" +
                "  where\n" +
                "\n"+
                "data Person = Person { id :: Int, name :: String }\n" +
                "    deriving (Eq, Ord, Show)\n";

        String expectedDamlResult = "module Gs.Finance\n" +
                "  where\n" +
                "\n"+
                "data Person = Person with\n" +
                "  id : Int\n" +
                "  name : String\n" +
                "    deriving (Eq, Ord, Show)\n";

        HaskellGrammarParser parser = HaskellGrammarParser.newInstance();
        HaskellModule module = parser.parseModule(text);
        DamlGrammarComposer composer = DamlGrammarComposer.newInstance();

        String result = composer.renderModule(module);
        Assert.assertEquals(expectedDamlResult, result);
    }


}
