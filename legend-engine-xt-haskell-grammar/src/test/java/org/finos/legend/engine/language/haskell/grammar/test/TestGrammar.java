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
        check("type Car implements Vehicle & X & Z {\n" +
                "  id: ID!\n" +
                "  name: String!\n" +
                "  values: [String]\n" +
                "  length(unit: LengthUnit = METER): Float\n" +
                "}");
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
