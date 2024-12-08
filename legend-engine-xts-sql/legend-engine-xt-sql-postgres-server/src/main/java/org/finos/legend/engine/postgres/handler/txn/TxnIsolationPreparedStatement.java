// Copyright 2023 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package org.finos.legend.engine.postgres.handler.txn;

import org.finos.legend.engine.postgres.handler.PostgresPreparedStatement;
import org.finos.legend.engine.postgres.handler.PostgresResultSet;
import org.finos.legend.engine.postgres.handler.PostgresResultSetMetaData;

import java.sql.ParameterMetaData;
import java.sql.Types;

public class TxnIsolationPreparedStatement implements PostgresPreparedStatement
{
    private boolean isComplete = false;
    static final String READ_COMMITTED = "read committed";
    static final PostgresResultSetMetaData TXN_ISOLATION_RS_META_DATA = new PostgresResultSetMetaData()
    {
        @Override
        public int getColumnCount()
        {
            return 1;
        }

        @Override
        public String getColumnName(int i)
        {
            return "transaction_isolation";
        }

        @Override
        public int getColumnType(int i)
        {
            return Types.VARCHAR;
        }

        @Override
        public int getScale(int i)
        {
            return 0;
        }
    };

    @Override
    public void setObject(int i, Object o)
    {
        // empty statement doesn't require objects to be set
    }

    @Override
    public PostgresResultSetMetaData getMetaData()
    {
        return TXN_ISOLATION_RS_META_DATA;
    }

    @Override
    public ParameterMetaData getParameterMetaData()
    {
        return null;
    }

    @Override
    public void close() throws Exception
    {
        // txn isolation handler doesn't require closure
    }

    @Override
    public void setMaxRows(int maxRows)
    {
        // txn isolation handler doesn't require max rows
    }

    @Override
    public int getMaxRows()
    {
        return 0;
    }

    @Override
    public boolean isExecuted()
    {
        return true;
    }

    @Override
    public boolean execute()
    {
        return true;
    }

    @Override
    public PostgresResultSet getResultSet()
    {
        return new PostgresResultSet()
        {
            @Override
            public PostgresResultSetMetaData getMetaData()
            {
                return TXN_ISOLATION_RS_META_DATA;
            }

            @Override
            public Object getObject(int i)
            {
                return READ_COMMITTED;
            }

            @Override
            public boolean next()
            {
                if (!isComplete)
                {
                    isComplete = true;
                    return true;
                }
                else
                {
                    return false;
                }
            }

            @Override
            public void close()
            {
                // txn isolation result set doesn't require closure
            }
        };
    }

}
