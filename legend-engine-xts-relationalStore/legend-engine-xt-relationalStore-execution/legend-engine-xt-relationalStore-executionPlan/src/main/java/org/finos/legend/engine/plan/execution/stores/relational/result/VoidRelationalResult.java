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

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.result.ExecutionActivity;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.ResultVisitor;
import org.finos.legend.engine.plan.execution.stores.relational.activity.RelationalExecutionActivity;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.RelationalExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.SQLExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class VoidRelationalResult extends Result
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(VoidRelationalResult.class);

    private Connection connection;
    private Statement statement;

    @Deprecated
    public VoidRelationalResult(MutableList<ExecutionActivity> activities, Connection connection, Identity identity)
    {
        this(activities, null, connection, identity, true);
    }

    public VoidRelationalResult(MutableList<ExecutionActivity> activities, ExecutionNode node, Connection connection, Identity identity, boolean logSQLWithParamValues)
    {
        super("VOID");

        try
        {
            String sql = ((RelationalExecutionActivity) activities.getLast()).sql;
            this.connection = connection;
            this.statement = connection.createStatement();
            long start = System.currentTimeMillis();
            String nodeSql = "";
            if (node instanceof RelationalExecutionNode)
            {
                nodeSql = ((RelationalExecutionNode) node).sqlQuery;
            }
            else if (node instanceof SQLExecutionNode)
            {
                nodeSql = ((SQLExecutionNode) node).sqlQuery;
            }
            String logMessage = logSQLWithParamValues ? sql : nodeSql;
            LOGGER.info(new LogInfo(identity.getName(), LoggingEventType.EXECUTION_RELATIONAL_START, logMessage).toString());
            this.statement.execute(sql);
            LOGGER.info(new LogInfo(identity.getName(), LoggingEventType.EXECUTION_RELATIONAL_STOP, (double) System.currentTimeMillis() - start).toString());
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
        return ((RelationalResultVisitor<T>) resultVisitor).visit(this);
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
            catch (Exception ignored)
            {
            }
        }
        if (this.connection != null)
        {
            try
            {
                this.connection.close();
            }
            catch (Exception ignored)
            {
            }
        }
    }
}
