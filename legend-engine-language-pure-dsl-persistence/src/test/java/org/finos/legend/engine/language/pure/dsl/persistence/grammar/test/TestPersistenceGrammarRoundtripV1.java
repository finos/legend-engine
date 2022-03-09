package org.finos.legend.engine.language.pure.dsl.persistence.grammar.test;

public class TestPersistenceGrammarRoundtripV1 extends TestPersistenceGrammarRoundtrip
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
    protected String targetOpaque()
    {
        return "Nested";
    }

    @Override
    protected String batchMode()
    {
        return "batchMode";
    }

    @Override
    protected String singleFlatTarget()
    {
        return "targetSpecification";
    }

    @Override
    protected String parts()
    {
        return "components";
    }
}
