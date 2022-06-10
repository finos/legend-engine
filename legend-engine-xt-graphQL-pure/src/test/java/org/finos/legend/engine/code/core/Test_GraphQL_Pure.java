package org.finos.legend.engine.code.core;

import junit.framework.TestSuite;
import org.finos.legend.pure.code.core.compiled.test.PureTestBuilderHelper;
import org.finos.legend.pure.m3.execution.test.TestCollection;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;

public class Test_GraphQL_Pure
{
    public static TestSuite suite()
    {
        CompiledExecutionSupport executionSupport = PureTestBuilderHelper.getClassLoaderExecutionSupport();
        TestSuite suite = new TestSuite();
        suite.addTest(PureTestBuilderHelper.buildSuite(TestCollection.collectTests("meta::external::query::graphQL", executionSupport.getProcessorSupport(), ci -> PureTestBuilderHelper.satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
        return suite;
    }
}
