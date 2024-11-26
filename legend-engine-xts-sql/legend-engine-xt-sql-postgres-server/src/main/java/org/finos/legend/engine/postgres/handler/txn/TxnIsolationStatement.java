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

import org.finos.legend.engine.postgres.handler.PostgresResultSet;
import org.finos.legend.engine.postgres.handler.PostgresResultSetMetaData;
import org.finos.legend.engine.postgres.handler.PostgresStatement;

import static org.finos.legend.engine.postgres.handler.txn.TxnIsolationPreparedStatement.READ_COMMITTED;
import static org.finos.legend.engine.postgres.handler.txn.TxnIsolationPreparedStatement.TXN_ISOLATION_RS_META_DATA;

public class TxnIsolationStatement implements PostgresStatement
{
    private boolean isComplete = false;

    @Override
    public boolean execute(String query)
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

    @Override
    public void close() throws Exception
    {
        // txn isolation handler doesn't require closure
    }
}
