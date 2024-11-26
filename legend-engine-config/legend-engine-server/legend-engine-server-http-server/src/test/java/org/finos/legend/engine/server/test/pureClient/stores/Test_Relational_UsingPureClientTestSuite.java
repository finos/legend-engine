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
import org.eclipse.collections.api.block.predicate.Predicate;
import org.finos.legend.engine.server.test.shared.PureWithEngineHelper;
import org.finos.legend.pure.m3.execution.test.PureTestBuilder;
import org.finos.legend.pure.m3.execution.test.TestCollection;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import static org.finos.legend.engine.server.test.shared.PureTestHelper.wrapSuite;
import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.*;
import static org.finos.legend.pure.m3.fct.shared.FCTTools.isFCTTest;

public class Test_Relational_UsingPureClientTestSuite extends TestSuite
{
    public static Test suite() throws Exception
    {

        return wrapSuite(
                () -> PureWithEngineHelper.initClientVersionIfNotAlreadySet("vX_X_X"),
                () ->
                {
                    CompiledExecutionSupport executionSupport = getClassLoaderExecutionSupport();
                    Predicate<? super CoreInstance> filter  = ci -> PureTestBuilder.satisfiesConditions(ci, executionSupport.getProcessorSupport()) && !isFCTTest(ci, executionSupport.getProcessorSupport());
                    TestSuite suite = new TestSuite();
                    //        suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::tests::constraints", executionSupport.getProcessorSupport(), filter), executionSupport));
                    //        suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::testDataGeneration::tests::alloy", executionSupport.getProcessorSupport(), filter), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::tests::advanced", executionSupport.getProcessorSupport(), filter), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::tests::groupBy", executionSupport.getProcessorSupport(), filter), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::tests::injection", executionSupport.getProcessorSupport(), filter), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::tests::map", executionSupport.getProcessorSupport(), filter), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::tests::mapping::association", executionSupport.getProcessorSupport(), filter), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::tests::mapping::boolean", executionSupport.getProcessorSupport(), filter), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::tests::mapping::dates", executionSupport.getProcessorSupport(), filter), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::tests::mapping::distinct", executionSupport.getProcessorSupport(), filter), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::tests::mapping::dynajoin", executionSupport.getProcessorSupport(), filter), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::tests::mapping::embedded", executionSupport.getProcessorSupport(), filter), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::tests::mapping::enumeration", executionSupport.getProcessorSupport(), filter), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::tests::mapping::extend", executionSupport.getProcessorSupport(), filter), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::tests::mapping::filter", executionSupport.getProcessorSupport(), filter), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::tests::mapping::groupBy", executionSupport.getProcessorSupport(), filter), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::tests::mapping::include", executionSupport.getProcessorSupport(), filter), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::tests::mapping::inheritance", executionSupport.getProcessorSupport(), filter), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::tests::mapping::innerjoin", executionSupport.getProcessorSupport(), filter), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::tests::mapping::join", executionSupport.getProcessorSupport(), filter), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::tests::mapping::merge", executionSupport.getProcessorSupport(), filter), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::tests::mapping::multigrain", executionSupport.getProcessorSupport(), filter), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::tests::mapping::propertyfunc", executionSupport.getProcessorSupport(), filter), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::tests::mapping::selfJoin", executionSupport.getProcessorSupport(), filter), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::tests::mapping::sqlFunction", executionSupport.getProcessorSupport(), filter), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::tests::mapping::subType", executionSupport.getProcessorSupport(), filter), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::tests::mapping::tree", executionSupport.getProcessorSupport(), filter), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::tests::mapping::union", executionSupport.getProcessorSupport(), ci -> !ci.getName().contains("testPksWithImportDataFlow") && satisfiesConditions(ci, executionSupport.getProcessorSupport())  && !isFCTTest(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::tests::mergerules", executionSupport.getProcessorSupport(), filter), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::tests::projection", executionSupport.getProcessorSupport(), filter), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::tests::query", executionSupport.getProcessorSupport(), filter), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::tests::tds", executionSupport.getProcessorSupport(), filter), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::tests::milestoning", executionSupport.getProcessorSupport(), filter), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::tests::functions::objectReferenceIn", executionSupport.getProcessorSupport(), filter), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::tests::postProcessor::filterPushDown", executionSupport.getProcessorSupport(), filter), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::graphFetch::tests::simple", executionSupport.getProcessorSupport(), filter), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::graphFetch::tests::embedded", executionSupport.getProcessorSupport(), filter), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::graphFetch::tests::qualifier", executionSupport.getProcessorSupport(), filter), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::graphFetch::tests::milestoning", executionSupport.getProcessorSupport(), filter), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::graphFetch::tests::classMappingFilterWithInnerJoin", executionSupport.getProcessorSupport(), filter), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::graphFetch::tests::chain", executionSupport.getProcessorSupport(), filter), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::graphFetch::tests::resultSourcing", executionSupport.getProcessorSupport(), filter), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::graphFetch::tests::crossDatabase", executionSupport.getProcessorSupport(), filter), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::graphFetch::tests::subType", executionSupport.getProcessorSupport(), filter), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::graphFetch::tests::union::propertyLevel", executionSupport.getProcessorSupport(), filter), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::tests::aggregationAware", executionSupport.getProcessorSupport(), filter), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::tests::advanced::resultSourcing", executionSupport.getProcessorSupport(), filter), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::pure::graphFetch::tests::XStore::inMemoryAndRelational", executionSupport.getProcessorSupport(), filter), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::pure::graphFetch::tests::XStoreUnion::inMemoryAndRelational", executionSupport.getProcessorSupport(), filter), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::pure::graphFetch::tests::XStore::ordered", executionSupport.getProcessorSupport(), filter), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::tests::functions::pureToSqlQuery::calendarAggregations", executionSupport.getProcessorSupport(), filter), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::tests::platform", executionSupport.getProcessorSupport(), filter), executionSupport));

                    return suite;
                },
                PureWithEngineHelper::cleanUp
        );
    }
}
