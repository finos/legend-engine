package org.finos.legend.engine.language.pure.dsl.persistence.grammar.test;

public class TestPersistenceGrammarRoundtripWithTargetSpecification extends TestPersistenceGrammarRoundtrip
{
    @Override
    protected String targetSingle()
    {
        return "Flat";
    }

    @Override
    protected String targetMulti()
    {
        return "GroupedFlat";
    }

    @Override
    protected String targetNested()
    {
        return "Nested";
    }
}
