package org.finos.legend.engine;

import com.fasterxml.jackson.databind.jsontype.NamedType;
import org.finos.legend.engine.authentication.SqlServerTestDatabaseAuthenticationFlowProviderConfiguration;
import org.finos.legend.engine.server.test.shared.RelationalTestServer;

public class SqlServerRelationalTestServerInvoker
{
    public static void main(String[] args) throws Exception
    {
        RelationalTestServer.execute(
                args.length == 0 ? new String[] {"server", "org/finos/legend/engine/server/test/userTestConfig_withSqlServerTestConnection.json"} : args,
                new NamedType(SqlServerTestDatabaseAuthenticationFlowProviderConfiguration.class, "sqlServerTest")
        );
    }
}
