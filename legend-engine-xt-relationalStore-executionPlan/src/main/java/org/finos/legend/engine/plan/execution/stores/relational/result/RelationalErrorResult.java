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

package org.finos.legend.engine.plan.execution.stores.relational.result;

import org.finos.legend.engine.plan.execution.result.ErrorResult;
import org.slf4j.Logger;

import java.sql.Connection;

public class RelationalErrorResult extends ErrorResult
{
    private Connection connection;

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");


    public RelationalErrorResult(int code, String message, Connection connection)
    {
        super(code, message);
        this.connection = connection;
    }

    @Override
    public void close()
    {
        if (!childrenResults.isEmpty())
        {
            childrenResults.stream().forEach(result -> result.close());
        }
        if (connection != null)
        {
            try
            {
                connection.close();
            }
            catch (Exception e)
            {
                LOGGER.error("error closing connection", e);
            }
        }
    }
}
