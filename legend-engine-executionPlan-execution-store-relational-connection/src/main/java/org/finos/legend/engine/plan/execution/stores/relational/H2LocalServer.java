package org.finos.legend.engine.plan.execution.stores.relational;

import org.finos.legend.engine.shared.core.port.DynamicPortGenerator;

import java.sql.SQLException;

public class H2LocalServer
{
    private static final H2LocalServer INSTANCE = new H2LocalServer(DynamicPortGenerator.generatePort());
    private final int port;

    private H2LocalServer(int port)
    {
        this.port = port;
        try
        {
            // We can create only one instance of the server as the databases are anyway shared in the process (VM)
            // The important part is to have an empty name in 'this.buildDataSource("127.0.0.1", localH2Port, "", null)'
            // It ensure the database creation is 'private' to the connection.
            AlloyH2Server.startServer(port);
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }

    }

    public static H2LocalServer getInstance()
    {
        return INSTANCE;
    }

    public int getPort()
    {
        return port;
    }
}
