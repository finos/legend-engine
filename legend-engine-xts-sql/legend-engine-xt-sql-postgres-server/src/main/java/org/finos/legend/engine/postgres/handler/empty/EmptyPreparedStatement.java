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

package org.finos.legend.engine.postgres.handler.empty;

import java.sql.ParameterMetaData;
import org.finos.legend.engine.postgres.handler.PostgresPreparedStatement;
import org.finos.legend.engine.postgres.handler.PostgresResultSet;
import org.finos.legend.engine.postgres.handler.PostgresResultSetMetaData;

public class EmptyPreparedStatement implements PostgresPreparedStatement
{
    @Override
    public void setObject(int i, Object o)
    {

    }

    @Override
    public PostgresResultSetMetaData getMetaData()
    {
        return null;
    }

    @Override
    public ParameterMetaData getParameterMetaData()
    {
        return null;
    }

    @Override
    public void close() throws Exception
    {

    }

    @Override
    public void setMaxRows(int maxRows)
    {

    }

    @Override
    public boolean execute()
    {
        return false;
    }

    @Override
    public PostgresResultSet getResultSet()
    {
        return null;
    }
}
