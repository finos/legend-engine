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

package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection;

import java.sql.Connection;

public enum TransactionIsolationLevel
{
    NONE(Connection.TRANSACTION_NONE),
    READ_UNCOMMITTED(Connection.TRANSACTION_READ_UNCOMMITTED),
    READ_COMMITTED(Connection.TRANSACTION_READ_COMMITTED),
    REPEATABLE_READ(Connection.TRANSACTION_REPEATABLE_READ),
    SERIALIZABLE(Connection.TRANSACTION_SERIALIZABLE);

    private final int level;

    TransactionIsolationLevel(int level)
    {
        this.level = level;
    }

    public int getLevel()
    {
        return this.level;
    }

    public static TransactionIsolationLevel fromJdbcLevel(int jdbcLevel)
    {
        switch (jdbcLevel)
        {
            case Connection.TRANSACTION_NONE:
                return NONE;
            case Connection.TRANSACTION_READ_UNCOMMITTED:
                return READ_UNCOMMITTED;
            case Connection.TRANSACTION_READ_COMMITTED:
                return READ_COMMITTED;
            case Connection.TRANSACTION_REPEATABLE_READ:
                return REPEATABLE_READ;
            case Connection.TRANSACTION_SERIALIZABLE:
                return SERIALIZABLE;
            default:
                throw new IllegalArgumentException("Unknown JDBC isolation level: " + jdbcLevel);
        }
    }
}
