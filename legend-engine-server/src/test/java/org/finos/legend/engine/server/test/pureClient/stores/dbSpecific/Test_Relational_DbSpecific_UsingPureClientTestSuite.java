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

package org.finos.legend.engine.server.test.pureClient.stores.dbSpecific;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.finos.legend.engine.server.test.shared.PureTestHelper;
import org.finos.legend.pure.code.core.compiled.test.PureTestBuilderHelper;
import org.finos.legend.pure.m3.execution.test.TestCollection;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.junit.Ignore;

import static org.finos.legend.engine.server.test.shared.PureTestHelper.*;


//Base classs for db specific tests - dont run this test directly
@Ignore
public class Test_Relational_DbSpecific_UsingPureClientTestSuite extends TestSuite
{
    public static Test createSuite(String pureTestCollectionPath,String testServerConfigFilePath) throws Exception
    {
        //Run test engine server - needs to be setup before as we need testParam(connection details) to create test suite
        PureTestHelper.initClientVersionIfNotAlreadySet("vX_X_X");
        PureTestHelper.initEnvironment(false, testServerConfigFilePath) ;

        CompiledExecutionSupport executionSupport = getClassLoaderExecutionSupport();

        TestSuite suite = new TestSuite();
        suite.addTest(PureTestBuilderHelper.buildSuite(TestCollection.collectTests(pureTestCollectionPath, executionSupport.getProcessorSupport(), fn -> PureTestBuilderHelper.generatePureTestCollection(fn, executionSupport), ci -> PureTestBuilderHelper.satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));

        return suite;
    }
}

