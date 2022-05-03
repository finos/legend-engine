package org.finos.legend.pure.code.core.relational;

import junit.framework.TestSuite;
import org.finos.legend.pure.code.core.compiled.test.PureTestBuilderHelper;
import org.finos.legend.pure.m3.execution.test.TestCollection;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;

public class Test_Pure_Relational
{
    public static TestSuite suite()
    {
        CompiledExecutionSupport executionSupport = PureTestBuilderHelper.getClassLoaderExecutionSupport();
        TestSuite suite = new TestSuite();
        suite.addTest(PureTestBuilderHelper.buildSuite(TestCollection.collectTests("meta::relational", executionSupport.getProcessorSupport(), ci -> PureTestBuilderHelper.satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
        suite.addTest(PureTestBuilderHelper.buildSuite(TestCollection.collectTests("meta::alloy::objectReference", executionSupport.getProcessorSupport(), ci -> PureTestBuilderHelper.satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
        suite.addTest(PureTestBuilderHelper.buildSuite(TestCollection.collectTests("meta::alloy::service::execution", executionSupport.getProcessorSupport(), ci -> PureTestBuilderHelper.satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
        suite.addTest(PureTestBuilderHelper.buildSuite(TestCollection.collectTests("meta::pure::lineage", executionSupport.getProcessorSupport(), ci -> PureTestBuilderHelper.satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
        suite.addTest(PureTestBuilderHelper.buildSuite(TestCollection.collectTests("meta::pure::graphFetch", executionSupport.getProcessorSupport(), ci -> PureTestBuilderHelper.satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
        suite.addTest(PureTestBuilderHelper.buildSuite(TestCollection.collectTests("meta::pure::router", executionSupport.getProcessorSupport(), ci -> PureTestBuilderHelper.satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
        suite.addTest(PureTestBuilderHelper.buildSuite(TestCollection.collectTests("meta::pure::alloy::connections", executionSupport.getProcessorSupport(), ci -> PureTestBuilderHelper.satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
        suite.addTest(PureTestBuilderHelper.buildSuite(TestCollection.collectTests("meta::pure::legend::connections", executionSupport.getProcessorSupport(), ci -> PureTestBuilderHelper.satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
        suite.addTest(PureTestBuilderHelper.buildSuite(TestCollection.collectTests("meta::pure::tds", executionSupport.getProcessorSupport(), ci -> PureTestBuilderHelper.satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
        suite.addTest(PureTestBuilderHelper.buildSuite(TestCollection.collectTests("meta::pure::executionPlan", executionSupport.getProcessorSupport(), ci -> PureTestBuilderHelper.satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
        suite.addTest(PureTestBuilderHelper.buildSuite(TestCollection.collectTests("meta::pure::mapping", executionSupport.getProcessorSupport(), ci -> PureTestBuilderHelper.satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
        suite.addTest(PureTestBuilderHelper.buildSuite(TestCollection.collectTests("meta::pure::milestoning", executionSupport.getProcessorSupport(), ci -> PureTestBuilderHelper.satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
        suite.addTest(PureTestBuilderHelper.buildSuite(TestCollection.collectTests("meta::protocols::pure", executionSupport.getProcessorSupport(), ci -> PureTestBuilderHelper.satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
        suite.addTest(PureTestBuilderHelper.buildSuite(TestCollection.collectTests("meta::csv", executionSupport.getProcessorSupport(), ci -> PureTestBuilderHelper.satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));

        return suite;
    }
}
