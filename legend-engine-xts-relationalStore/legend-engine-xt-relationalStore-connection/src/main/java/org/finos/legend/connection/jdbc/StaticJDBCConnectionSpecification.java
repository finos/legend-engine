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

package org.finos.legend.connection.jdbc;

import org.finos.legend.connection.ConnectionSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;

import java.sql.Connection;

public class StaticJDBCConnectionSpecification extends ConnectionSpecification<Connection>
{
    public String host;
    public int port;
    public DatabaseType databaseType;
    public String databaseName;

    public StaticJDBCConnectionSpecification(String host, int port, DatabaseType databaseType, String databaseName)
    {
        this.host = host;
        this.port = port;
        this.databaseType = databaseType;
        this.databaseName = databaseName;
    }
}
