package org.finos.legend.engine.language.pure.dsl.persistence.compiler.test;

public class TestPersistenceCompilationFromGrammarV1 extends TestPersistenceCompilationFromGrammar
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
