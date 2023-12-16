// Copyright 2023 Goldman Sachs
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
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.plan.execution.stores.relational.test.H2TestServerResource;
import org.finos.legend.engine.server.test.shared.MetadataTestServerResource;
import org.finos.legend.engine.server.test.shared.PureWithEngineHelper;
import org.finos.legend.engine.server.test.shared.ServerTestServerResource;
import org.finos.legend.pure.m3.execution.test.TestCollection;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.*;

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
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::graphFetch::tests::simple", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::graphFetch::tests::embedded", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::graphFetch::tests::qualifier", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::graphFetch::tests::milestoning", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::graphFetch::tests::classMappingFilterWithInnerJoin", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::graphFetch::tests::chain", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::graphFetch::tests::resultSourcing", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::graphFetch::tests::crossDatabase", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::graphFetch::tests::subType", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::graphFetch::tests::union::propertyLevel", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::tests::aggregationAware", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::tests::advanced::resultSourcing", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::pure::graphFetch::tests::XStore::inMemoryAndRelational", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::pure::graphFetch::tests::XStoreUnion::inMemoryAndRelational", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::pure::graphFetch::tests::XStore::ordered", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    return suite;
                },
                PureWithEngineHelper::cleanUp,
                Lists.mutable.with(new H2TestServerResource(), new MetadataTestServerResource(), new ServerTestServerResource("org/finos/legend/engine/server/test/userTestConfigParallelizationEnabled.json"))
        );
    }
}
