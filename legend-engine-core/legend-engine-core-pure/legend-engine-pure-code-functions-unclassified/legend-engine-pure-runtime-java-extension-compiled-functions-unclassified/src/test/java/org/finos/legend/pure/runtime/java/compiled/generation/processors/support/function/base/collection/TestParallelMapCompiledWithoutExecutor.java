package org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.base.collection;

import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.tests.function.base.collection.AbstractTestParallelMap;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiledBuilder;
import org.finos.legend.pure.runtime.java.compiled.factory.JavaModelFactoryRegistryLoader;
import org.junit.BeforeClass;

public class TestParallelMapCompiledWithoutExecutor extends AbstractTestParallelMap
{
    public TestParallelMapCompiledWithoutExecutor()
    {
    }

    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution(), JavaModelFactoryRegistryLoader.loader());
    }

    public static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionCompiledBuilder().build();
    }
}
