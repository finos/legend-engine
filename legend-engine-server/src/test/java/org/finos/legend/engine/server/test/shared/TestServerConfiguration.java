package org.finos.legend.engine.server.test.shared;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.server.ServerConfiguration;

public class TestServerConfiguration extends ServerConfiguration
{
    public List<DatabaseType> testConnectionsToEnable = new ArrayList<>();
    public Map<DatabaseType, RelationalDatabaseConnection> staticTestConnections = new HashMap<>();
}
