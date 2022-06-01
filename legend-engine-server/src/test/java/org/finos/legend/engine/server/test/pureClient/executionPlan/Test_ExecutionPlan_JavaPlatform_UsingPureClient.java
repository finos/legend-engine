package org.finos.legend.engine.server.test.pureClient.executionPlan;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.finos.legend.engine.server.test.shared.PureTestHelper;
import org.finos.legend.pure.m3.execution.test.TestCollection;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;

import static org.finos.legend.engine.server.test.shared.PureTestHelper.wrapSuite;

public class Test_ExecutionPlan_JavaPlatform_UsingPureClient  extends TestSuite
{
    public static Test suite()
    {
        return wrapSuite(
                () -> PureTestHelper.initClientVersionIfNotAlreadySet("vX_X_X"),
                () -> {
                    CompiledExecutionSupport executionSupport = PureTestHelper.getClassLoaderExecutionSupport();
                    TestSuite suite = new TestSuite();
                    suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::pure::executionPlan::engine::java", executionSupport.getProcessorSupport(), ci -> PureTestHelper.satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    return suite;
                });
    }
}