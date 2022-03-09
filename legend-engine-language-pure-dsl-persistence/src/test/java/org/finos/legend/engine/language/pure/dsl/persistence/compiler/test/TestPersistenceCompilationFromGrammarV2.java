package org.finos.legend.engine.language.pure.dsl.persistence.compiler.test;

public class TestPersistenceCompilationFromGrammarV2 extends TestPersistenceCompilationFromGrammar
{
    @Override
    protected String targetSingle()
    {
        return "SingleFlat";
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
    protected String singleFlatTarget()
    {
        return "singleFlatTarget";
    }

    @Override
    protected String parts()
    {
        return "parts";
    }
}
