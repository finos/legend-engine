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

package org.finos.legend.engine.server.test.shared;

import com.fasterxml.jackson.databind.jsontype.NamedType;
import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.finos.legend.pure.code.core.compiled.test.PureTestBuilderHelper;
import org.finos.legend.pure.m3.execution.test.TestCollection;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.junit.Ignore;

//Base classs for db specific tests - dont run this test directly
@Ignore
public abstract class Relational_DbSpecific_UsingPureClientTestSuite extends TestSuite
{
    public static Test createSuite(String pureTestCollectionPath, String testServerConfigFilePath, NamedType... extraConfigTypes) throws Exception
    {
        //Run test engine server - needs to be setup before as we need testParam(connection details) to create test suite
        boolean shouldCleanUp = PureWithEngineHelper.initClientVersionIfNotAlreadySet("vX_X_X");
        RelationalTestServer server = PureWithEngineHelper.initEngineServer(testServerConfigFilePath, () -> new RelationalTestServer(extraConfigTypes));
        CompiledExecutionSupport executionSupport = PureTestBuilderHelper.getClassLoaderExecutionSupport();

        TestSuite suite = PureTestBuilderHelper.buildSuite(TestCollection.collectTests(pureTestCollectionPath, executionSupport.getProcessorSupport(), fn -> PureTestBuilderHelper.generatePureTestCollection(fn, executionSupport), ci -> PureTestBuilderHelper.satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport);

        return new TestSetup(suite)
        {
            @Override
            protected void tearDown() throws Exception
            {
                super.tearDown();
                server.shutDown();
                if (shouldCleanUp)
                {
                    PureWithEngineHelper.cleanUp();
                }
                System.out.println("STOP");
            }
        };
    }
}
