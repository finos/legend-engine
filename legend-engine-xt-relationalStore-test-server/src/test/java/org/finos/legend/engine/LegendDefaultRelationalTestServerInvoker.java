package org.finos.legend.engine;

import com.fasterxml.jackson.databind.jsontype.NamedType;
import org.finos.legend.engine.authentication.LegendDefaultDatabaseAuthenticationFlowProviderConfiguration;
import org.finos.legend.engine.server.test.shared.RelationalTestServer;

public class LegendDefaultRelationalTestServerInvoker
{
    public static void main(String[] args) throws Exception
    {
        RelationalTestServer.execute(
                args.length == 0 ? new String[] {"server", "org/finos/legend/engine/server/test/userTestConfig_withH2TestConnection.json"} : args,
                new NamedType(LegendDefaultDatabaseAuthenticationFlowProviderConfiguration.class, "legendDefault")
        );
    }
}
