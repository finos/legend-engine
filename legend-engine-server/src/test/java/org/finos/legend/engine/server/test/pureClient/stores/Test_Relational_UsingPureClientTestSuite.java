// Copyright 2021 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.server.test.pureClient.stores;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.finos.legend.engine.server.test.shared.PureTestHelper;
import org.finos.legend.pure.m3.execution.test.TestCollection;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;

import static org.finos.legend.engine.server.test.shared.PureTestHelper.*;

public class Test_Relational_UsingPureClientTestSuite extends TestSuite
{
    public static Test suite() throws Exception
    {
        return wrapSuite(
                () -> PureTestHelper.initClientVersionIfNotAlreadySet("vX_X_X"),
                () -> {
                    CompiledExecutionSupport executionSupport = getClassLoaderExecutionSupport();
                    TestSuite suite = new TestSuite();
            //        suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::relational::tests::constraints", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
            //        suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::relational::testDataGeneration::tests::alloy", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::relational::tests::advanced", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::relational::tests::groupBy", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::relational::tests::injection", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::relational::tests::map", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::relational::tests::mapping::association", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::relational::tests::mapping::boolean", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::relational::tests::mapping::dates", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::relational::tests::mapping::distinct", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::relational::tests::mapping::dynajoin", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::relational::tests::mapping::embedded", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::relational::tests::mapping::enumeration", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::relational::tests::mapping::extend", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::relational::tests::mapping::filter", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::relational::tests::mapping::groupBy", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::relational::tests::mapping::include", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::relational::tests::mapping::inheritance", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::relational::tests::mapping::innerjoin", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::relational::tests::mapping::join", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::relational::tests::mapping::merge", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::relational::tests::mapping::multigrain", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::relational::tests::mapping::propertyfunc", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::relational::tests::mapping::selfJoin", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::relational::tests::mapping::sqlFunction", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::relational::tests::mapping::subType", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::relational::tests::mapping::tree", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::relational::tests::mapping::union", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::relational::tests::mergerules", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::relational::tests::projection", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::relational::tests::query", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::relational::tests::tds", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::relational::tests::milestoning", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::relational::tests::functions::objectReferenceIn", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::relational::graphFetch::tests::simple", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::relational::graphFetch::tests::embedded", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::relational::graphFetch::tests::qualifier", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::relational::graphFetch::tests::milestoning", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::relational::graphFetch::tests::chain", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(PureTestHelper.buildSuite(TestCollection.collectTests("meta::relational::tests::aggregationAware", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    return suite;
                });
    }
}
