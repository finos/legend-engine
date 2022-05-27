package org.finos.legend.engine.language.haskell.grammar.test;

import org.finos.legend.engine.language.haskell.grammar.from.HaskellGrammarParser;
import org.finos.legend.engine.language.haskell.grammar.to.HaskellGrammarComposer;
import org.finos.legend.engine.protocol.haskell.metamodel.HaskellModule;
import org.junit.Assert;
import org.junit.Test;

public class TestGrammar {

    @Test
    public void testTypeRoundtrip()
    {
        check("module Gs.Finance\n" +
                "  where\n" +
                "\n"+
                "data Person = Person { id :: Int, name :: String }\n" +
                "    deriving (Eq, Ord, Show)\n");
    }

    @Test
    public void testTypeWithListRoundtrip()
    {
        check("module Gs.Finance\n" +
                "  where\n" +
                "\n"+
                "data Trade = Trade { id :: Int, notionals :: [Double] }\n" +
                "    deriving (Eq, Ord, Show)\n");
    }

    @Test
    public void testTypeWithOptionalFieldsRoundtrip()
    {
        check("module Gs.Finance\n" +
                "  where\n" +
                "\n"+
                "data Trade = Trade { id :: Optional Int, notionals :: Optional [Double] }\n" +
                "    deriving (Eq, Ord, Show)\n");
    }

    @Test
    public void testEnumRoundtrip()
    {
        check("module Gs.Finance\n" +
                "  where\n" +
                "\n"+
                "data Color = Red\n" +
                "  | Blue\n" +
                "  | Green\n" +
                "    deriving (Eq, Ord, Show)\n");
    }


    protected void check(String value)
    {
        HaskellGrammarParser parser = HaskellGrammarParser.newInstance();
        HaskellModule module = parser.parseModule(value);
        HaskellGrammarComposer composer = HaskellGrammarComposer.newInstance();
        String result = composer.renderModule(module);
        Assert.assertEquals(value, result);
    }
}
