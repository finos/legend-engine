package org.finos.legend.pure.runtime.java.interpreted.function.base.collection;

import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.tests.function.base.collection.AbstractTestParallelMap;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.junit.BeforeClass;

public class TestParallelMapWithoutExecutor extends AbstractTestParallelMap
{
    public TestParallelMapWithoutExecutor()
    {
    }

    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution());
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionInterpreted();
    }
}
