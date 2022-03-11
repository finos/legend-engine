package org.finos.legend.engine.language.pure.dsl.persistence.compiler.test;

public class TestPersistenceCompilationFromGrammarV2 extends TestPersistenceCompilationFromGrammar
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
    protected String ingestMode()
    {
        return "ingestMode";
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
