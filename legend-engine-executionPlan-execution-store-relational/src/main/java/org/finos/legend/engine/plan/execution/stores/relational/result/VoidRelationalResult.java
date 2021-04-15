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

package org.finos.legend.engine.plan.execution.stores.relational.result;

import org.finos.legend.engine.plan.execution.stores.relational.activity.RelationalExecutionActivity;

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.result.ExecutionActivity;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.ResultVisitor;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.pac4j.core.profile.CommonProfile;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class VoidRelationalResult extends Result
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");

    private Connection connection;
    private Statement statement;

    public VoidRelationalResult(MutableList<ExecutionActivity> activities, Connection connection, MutableList<CommonProfile> profiles)
    {
        super("VOID");

        try
        {
            String sql = ((RelationalExecutionActivity) activities.getLast()).sql;
            this.connection = connection;
            this.statement = connection.createStatement();
            long start = System.currentTimeMillis();
            LOGGER.info(new LogInfo(profiles, LoggingEventType.EXECUTION_RELATIONAL_START, sql).toString());
            this.statement.execute(sql);
            LOGGER.info(new LogInfo(profiles, LoggingEventType.EXECUTION_RELATIONAL_STOP, (double)System.currentTimeMillis() - start).toString());
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            this.close();
        }
    }

    @Override
    public <T> T accept(ResultVisitor<T> resultVisitor)
    {
        return ((RelationalResultVisitor<T>)resultVisitor).visit(this);
    }

    @Override
    public void close()
    {
        if (this.statement != null)
        {
            try
            {
                this.statement.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        if (this.connection != null)
        {
            try
            {
                this.connection.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }
}
