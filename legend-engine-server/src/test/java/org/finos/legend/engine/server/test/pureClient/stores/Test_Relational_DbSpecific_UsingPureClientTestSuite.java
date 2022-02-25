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

package org.finos.legend.engine.server.test.pureClient.stores;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.ConnectionTestManager;
import org.finos.legend.engine.server.test.shared.PureTestHelper;
import org.finos.legend.pure.m3.execution.test.TestCollection;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;

import java.util.ServiceLoader;

import static org.finos.legend.engine.server.test.shared.PureTestHelper.*;

public class Test_Relational_DbSpecific_UsingPureClientTestSuite extends TestSuite
{
    public static Test suite()
    {
        return wrapSuite(
                () -> PureTestHelper.initClientVersionIfNotAlreadySet("vX_X_X"),
                () -> {
                    CompiledExecutionSupport executionSupport = getClassLoaderExecutionSupport();
                    TestSuite suite = new TestSuite();
                    for (ConnectionTestManager connectionTestManager : ServiceLoader.load(ConnectionTestManager.class))
                    {
                        suite.addTest(buildSuite(TestCollection.collectTests(connectionTestManager.getPureTestCollectionPath(), executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    }
                    return suite;
                },
                false ,"org/finos/legend/engine/server/test/userTestConfig_withTestConnections.json");
    }
}

