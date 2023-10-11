// Copyright 2023 Goldman Sachs
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

package org.finos.legend.connection;

import org.finos.legend.connection.impl.JDBCConnectionManager;
import org.finos.legend.connection.protocol.ConnectionSpecification;
import org.finos.legend.engine.shared.core.identity.Credential;

import java.sql.Connection;

public abstract class JDBCConnectionBuilder<CRED extends Credential, SPEC extends ConnectionSpecification> extends ConnectionBuilder<Connection, CRED, SPEC>
{
    private JDBCConnectionManager.ConnectionPoolConfig connectionPoolConfig;

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

    public void setConnectionPoolConfig(JDBCConnectionManager.ConnectionPoolConfig connectionPoolConfig)
    {
        this.connectionPoolConfig = connectionPoolConfig;
    }

    public JDBCConnectionManager.ConnectionPoolConfig getConnectionPoolConfig()
    {
        return connectionPoolConfig;
    }
}
