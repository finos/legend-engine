package org.finos.legend.engine.language.pure.dsl.persistence.compiler.test;

public class TestPersistenceCompilationFromGrammarWithTargetSpecification extends TestPersistenceCompilationFromGrammar
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
