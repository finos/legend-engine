package org.finos.legend.engine.plan.execution.stores.relational;

import org.h2.tools.Server;
import org.slf4j.Logger;

import java.sql.SQLException;

public class AlloyH2Server extends Server
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(AlloyH2Server.class);

    private static final String H2_ALLOWED_CLASSES_PROPERTY = "h2.allowedClasses";
    private static final String H2_ALLOWED_CLASSES_DEFAULT = "org.h2.*";

    public static Server startServer(int port) throws SQLException
    {
        LOGGER.debug("startServer port {}", port);
        System.setProperty(H2_ALLOWED_CLASSES_PROPERTY, System.getProperty(H2_ALLOWED_CLASSES_PROPERTY, H2_ALLOWED_CLASSES_DEFAULT));
        return Server.createTcpServer("-ifNotExists", "-tcpPort", String.valueOf(port)).start();
    }

    public static void main(String[] args)
    {
        try
        {
            startServer(Integer.parseInt(args[0]));
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }
}
