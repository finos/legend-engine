// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.server.test.pureClient.executionPlan;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.finos.legend.engine.server.test.shared.PureWithEngineHelper;
import org.finos.legend.pure.m3.execution.test.TestCollection;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;
import org.junit.Ignore;

import static org.finos.legend.engine.server.test.shared.PureTestHelper.wrapSuite;
import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.buildJavaPureTestSuite;
import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.getClassLoaderExecutionSupport;

@Ignore
public class Test_ExecutionPlan_JavaPlatform_CodeGeneration_UsingPureClient extends TestSuite
{
    public static Test suite()
    {
        return wrapSuite(
                () -> PureWithEngineHelper.initClientVersionIfNotAlreadySet("vX_X_X"),
                () ->
                {
                    CompiledExecutionSupport executionSupport = getClassLoaderExecutionSupport();
                    String func = "meta::pure::executionPlan::platformBinding::legendJava::tests::utils::javaPureTestWrapper_FunctionDefinition_1__Boolean_1_";
                    CoreInstance runner = executionSupport.getProcessorSupport().package_getByUserPath(func);
                    TestSuite suite = new TestSuite();
                    TestCollection testCollection = TestCollection.collectTests("meta::pure::executionPlan::platformBinding::legendJava::library::tests", executionSupport.getProcessorSupport(), fn -> PureTestBuilderCompiled.generatePureTestCollection(fn, executionSupport), null);
                    suite.addTest(buildJavaPureTestSuite(testCollection, executionSupport, runner));
                    return suite;
                },
                PureWithEngineHelper::cleanUp
        );
    }
}


