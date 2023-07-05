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

package org.finos.legend.engine;

import com.fasterxml.jackson.databind.jsontype.NamedType;
import org.finos.legend.engine.authentication.SqlServerTestDatabaseAuthenticationFlowProviderConfiguration;
import org.finos.legend.engine.server.test.shared.RelationalTestServer;

public class SqlServerRelationalTestServerInvoker
{
    public static void main(String[] args) throws Exception
    {
        RelationalTestServer.execute(
                args.length == 0 ? new String[] {"server", "legend-engine-xt-relationalStore-sqlserver-execution-tests/src/test/resources/org/finos/legend/engine/server/test/userTestConfig_withSqlServerTestConnection.json"} : args,
                new NamedType(SqlServerTestDatabaseAuthenticationFlowProviderConfiguration.class, "sqlServerTest")
        );
    }
}
