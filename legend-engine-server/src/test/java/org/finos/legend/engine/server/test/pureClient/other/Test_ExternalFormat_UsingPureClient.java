package org.finos.legend.engine.server.test.pureClient.other;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.finos.legend.engine.server.test.shared.PureTestHelper;
import org.finos.legend.pure.m3.execution.test.TestCollection;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;

public class Test_ExternalFormat_UsingPureClient
{
    public static Test suite()
    {
        return PureTestHelper.wrapSuite(
                () -> PureTestHelper.initClientVersionIfNotAlreadySet("vX_X_X"),
                () -> {
                    CompiledExecutionSupport executionSupport = PureTestHelper.getClassLoaderExecutionSupport();
                    TestSuite suite = new TestSuite();
                    // NOTE: temporarily ignore these tests until we bring extensions back into Legend
                    suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::external::format", executionSupport.getProcessorSupport(), ci -> !ci.getName().equals("transform_testClassWithMapToProtoBuf__Boolean_1_") && PureTestHelper.satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::external::language", executionSupport.getProcessorSupport(), ci -> PureTestHelper.satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::external::shared", executionSupport.getProcessorSupport(), ci -> PureTestHelper.satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::external::store", executionSupport.getProcessorSupport(), ci -> PureTestHelper.satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    return suite;
                }
        );
    }
}
