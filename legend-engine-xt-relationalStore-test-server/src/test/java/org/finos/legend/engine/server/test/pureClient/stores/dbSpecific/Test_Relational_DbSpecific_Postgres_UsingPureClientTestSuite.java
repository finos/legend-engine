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

import com.fasterxml.jackson.databind.jsontype.NamedType;
import junit.framework.Test;
import org.finos.legend.engine.authentication.LegendDefaultDatabaseAuthenticationFlowProviderConfiguration;
import org.finos.legend.engine.server.test.shared.Relational_DbSpecific_UsingPureClientTestSuite;
import org.finos.legend.pure.runtime.java.compiled.testHelper.IgnoreUnsupportedApiPureTestSuiteRunner;
import org.junit.runner.RunWith;

@RunWith(IgnoreUnsupportedApiPureTestSuiteRunner.class)
public class Test_Relational_DbSpecific_Postgres_UsingPureClientTestSuite
        extends Relational_DbSpecific_UsingPureClientTestSuite
{
    public static Test suite() throws Exception
    {
        return createSuite(
                "meta::relational::tests::sqlQueryToString::postgres",
                "org/finos/legend/engine/server/test/userTestConfig_withPostgresTestConnection.json",
                new NamedType(LegendDefaultDatabaseAuthenticationFlowProviderConfiguration.class, "legendDefault")
        );
    }
}
