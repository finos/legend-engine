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
//

package org.finos.legend.engine.plan.execution.stores.mongodb.test;

import junit.framework.TestSuite;
import org.finos.legend.pure.m3.execution.test.PureTestBuilder;
import org.finos.legend.pure.m3.execution.test.TestCollection;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;
import org.testcontainers.DockerClientFactory;

public class Test_Pure_MongoDB_ExecutionPlan
{
    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite();
        if (DockerClientFactory.instance().isDockerAvailable())
        {
            System.setProperty("mongo_pwd", "sa");
            CompiledExecutionSupport executionSupport = PureTestBuilderCompiled.getClassLoaderExecutionSupport();
            suite.addTest(PureTestBuilderCompiled.buildSuite(
                    TestCollection.collectTests(
                            "meta::external::store::mongodb::executionTest",
                            executionSupport.getProcessorSupport(),
                            fn -> PureTestBuilderCompiled.generatePureTestCollection(fn, executionSupport),
                            ci -> PureTestBuilder.satisfiesConditions(ci, executionSupport.getProcessorSupport())
                    ),
                    executionSupport)
            );
        }
        else
        {
            System.err.println("Skipping test cases - Docker not available for TestContainer");
        }
        return suite;
    }
}
