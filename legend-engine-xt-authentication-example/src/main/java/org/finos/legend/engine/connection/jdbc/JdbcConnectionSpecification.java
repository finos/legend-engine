// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.connection.jdbc;

import org.finos.legend.connection.ConnectionSpecification;

public class JdbcConnectionSpecification extends ConnectionSpecification
{
    public enum DbType
    {
        H2
    }

    public String dbHostname;
    public int dbPort;
    public DbType dbType;

    public JdbcConnectionSpecification()
    {

    }

    public JdbcConnectionSpecification(String dbHostname, int dbPort, DbType dbType)
    {
        this.dbHostname = dbHostname;
        this.dbPort = dbPort;
        this.dbType = dbType;
    }
}
