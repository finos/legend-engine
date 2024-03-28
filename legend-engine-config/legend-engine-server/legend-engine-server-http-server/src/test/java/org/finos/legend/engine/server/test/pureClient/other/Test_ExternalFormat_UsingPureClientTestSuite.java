//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.server.test.pureClient.other;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.finos.legend.engine.server.test.shared.PureTestHelper;
import org.finos.legend.engine.server.test.shared.PureWithEngineHelper;
import org.finos.legend.pure.m3.execution.test.TestCollection;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.*;

public class Test_ExternalFormat_UsingPureClientTestSuite
{
    public static Test suite()
    {
        return PureTestHelper.wrapSuite(
                () -> PureWithEngineHelper.initClientVersionIfNotAlreadySet("vX_X_X"),
                () ->
                {
                    CompiledExecutionSupport executionSupport = getClassLoaderExecutionSupport();
                    TestSuite suite = new TestSuite();
                    // NOTE: temporarily ignore these tests until we bring extensions back into Legend
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::external::format::avro", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::external::format::protobuf", executionSupport.getProcessorSupport(), ci -> !ci.getName().equals("transform_testClassWithMapToProtoBuf__Boolean_1_") && satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::external::format::rosetta", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::external::format::xml", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::external::format::yaml", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));

                    suite.addTest(buildSuite(TestCollection.collectTests("meta::external::language", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::external::format::shared", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::external::store", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    return suite;
                },
                PureWithEngineHelper::cleanUp
        );
    }
}
