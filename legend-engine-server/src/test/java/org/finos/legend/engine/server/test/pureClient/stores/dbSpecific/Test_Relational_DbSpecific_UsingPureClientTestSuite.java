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

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.finos.legend.engine.server.test.shared.PureTestHelper;
import org.finos.legend.engine.server.test.shared.ServersState;
import org.finos.legend.pure.generated.Root_meta_relational_dbTestRunner_DbTestConfig;
import org.finos.legend.pure.generated.core_relational_relational_dbTestRunner_shared;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.junit.Ignore;

import static org.finos.legend.engine.server.test.shared.PureTestHelper.*;

public abstract class Test_Relational_DbSpecific_UsingPureClientTestSuite extends TestSuite
{
    private static final ThreadLocal<ServersState> state = new ThreadLocal<>();

    public static Test createSuite( String dbType, String dbTestCollectionPath, String testServerConfigFilePath ) throws Exception
    {
        CompiledExecutionSupport executionSupport = getClassLoaderExecutionSupport();

        try
        {
            //Run test engine server - needs to be setup before as we need testParam(connection details) to create test suite
            PureTestHelper.initClientVersionIfNotAlreadySet( "vX_X_X" );
            state.set( PureTestHelper.initEnvironment( false, testServerConfigFilePath ) );
        }
        catch ( Exception e )
        {
            throw e;
        }

        Root_meta_relational_dbTestRunner_DbTestConfig dbTestConfig =
                core_relational_relational_dbTestRunner_shared.Root_meta_relational_dbTestRunner_createDbConfig_String_1__DbTestConfig_1_(
                        dbType, executionSupport );

        TestSuite suite = buildDbSuite(
                core_relational_relational_dbTestRunner_shared.Root_meta_relational_dbTestRunner_collectTests_String_1__DbTestCollection_1_(
                        dbTestCollectionPath, executionSupport ),
                dbTestConfig, executionSupport );

        return new TestSetup( suite )
        {
            @Override
            protected void tearDown() throws Exception
            {
                super.tearDown();
                state.get().shutDown();
                state.remove();
            }
        };

    }
}

