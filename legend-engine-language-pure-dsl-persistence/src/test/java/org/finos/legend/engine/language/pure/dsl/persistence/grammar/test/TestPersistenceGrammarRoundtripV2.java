package org.finos.legend.engine.language.pure.dsl.persistence.grammar.test;

public class TestPersistenceGrammarRoundtripV2 extends TestPersistenceGrammarRoundtrip
{
    @Override
    protected String targetFlat()
    {
        return "Flat";
    }

    @Override
    protected String targetMulti()
    {
        return "MultiFlat";
    }

    @Override
    protected String targetOpaque()
    {
        return "OpaqueTarget";
    }

    @Override
    protected String batchMode()
    {
        return "batchMode";
    }

    @Override
    protected String flatTarget()
    {
        return "flatTarget";
    }

    @Override
    protected String parts()
    {
        return "parts";
    }
}