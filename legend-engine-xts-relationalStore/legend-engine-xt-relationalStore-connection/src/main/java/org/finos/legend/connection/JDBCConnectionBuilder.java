package org.finos.legend.connection;

import org.finos.legend.connection.ConnectionBuilder;
import org.finos.legend.connection.impl.JDBCConnectionManager;
import org.finos.legend.connection.protocol.ConnectionSpecification;
import org.finos.legend.engine.shared.core.identity.Credential;

import java.sql.Connection;

public abstract class JDBCConnectionBuilder<CRED extends Credential, SPEC extends ConnectionSpecification> extends ConnectionBuilder<Connection, CRED, SPEC>
{
    @Override
    public Class<? extends Credential> getCredentialType()
    {
        return (Class<? extends Credential>) actualTypeArguments()[0];
    }

    @Override
    public Class<? extends ConnectionSpecification> getConnectionSpecificationType()
    {
        return (Class<? extends ConnectionSpecification>) actualTypeArguments()[1];
    }

    @Override
    public JDBCConnectionManager getConnectionManager()
    {
        return JDBCConnectionManager.getInstance();
    }
}
