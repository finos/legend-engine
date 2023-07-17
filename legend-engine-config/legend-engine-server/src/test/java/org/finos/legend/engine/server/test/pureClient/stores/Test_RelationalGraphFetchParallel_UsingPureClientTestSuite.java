package org.finos.legend.engine.server.test.pureClient.stores;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.finos.legend.engine.server.test.shared.PureTestHelper;
import org.finos.legend.engine.server.test.shared.PureWithEngineHelper;
import org.finos.legend.pure.m3.execution.test.TestCollection;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;

import static org.finos.legend.engine.server.test.shared.PureTestHelper.getClassLoaderExecutionSupport;
import static org.finos.legend.engine.server.test.shared.PureTestHelper.satisfiesConditions;
import static org.finos.legend.engine.server.test.shared.PureTestHelper.wrapSuite;

public class Test_RelationalGraphFetchParallel_UsingPureClientTestSuite extends TestSuite
{
    public static Test suite() throws Exception
    {
        return wrapSuite(
                () -> PureWithEngineHelper.initClientVersionIfNotAlreadySet("vX_X_X"),
                () ->
                {
                    CompiledExecutionSupport executionSupport = getClassLoaderExecutionSupport();
                    TestSuite suite = new TestSuite();
                    /*suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::relational::graphFetch::tests::simple", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::relational::graphFetch::tests::embedded", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::relational::graphFetch::tests::qualifier", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::relational::graphFetch::tests::milestoning", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::relational::graphFetch::tests::chain", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::relational::graphFetch::tests::resultSourcing", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::relational::graphFetch::tests::crossDatabase", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::relational::graphFetch::tests::subType", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::relational::graphFetch::tests::union::propertyLevel", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::relational::tests::aggregationAware", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::relational::tests::advanced::resultSourcing", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));*/
                    suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::pure::graphFetch::tests::XStore::inMemoryAndRelational", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::pure::graphFetch::tests::XStoreUnion::inMemoryAndRelational", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::pure::graphFetch::tests::XStore::ordered", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    return suite;
                },
                true,
                "org/finos/legend/engine/server/test/userTestConfigParallelizationEnabled.json");
    }
}
