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

import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.engine.plan.execution.result.ExecutionActivity;
import org.finos.legend.engine.plan.execution.stores.relational.activity.RelationalExecutionActivity;
import org.finos.legend.engine.shared.core.api.request.RequestContext;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseConnection;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.slf4j.Logger;

import java.sql.Connection;
import java.util.List;

public class SQLUpdateResult extends SQLResult
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(SQLUpdateResult.class);

    private final int updateCount;

    public SQLUpdateResult(List<ExecutionActivity> activities, String databaseType, Connection connection, DatabaseConnection dbConnection, Identity identity, List<String> temporaryTables, RequestContext requestContext)
    {
        super("success", connection, dbConnection, activities, databaseType, temporaryTables, requestContext);
        try
        {
            long start = System.currentTimeMillis();
            RelationalExecutionActivity activity = ((RelationalExecutionActivity) activities.get(activities.size() - 1));
            String executedSql = activity.comment != null ? activity.comment.concat("\n").concat(activity.sql) : activity.sql;
            LOGGER.info(new LogInfo(identity.getName(), LoggingEventType.EXECUTION_RELATIONAL_START).toString());
            this.getStatement().executeUpdate(executedSql);
            this.updateCount = this.getStatement().getUpdateCount();
            LOGGER.info(new LogInfo(identity.getName(), LoggingEventType.EXECUTION_RELATIONAL_STOP, (double) System.currentTimeMillis() - start).toString());
            this.close();
        }
        catch (Throwable e)
        {
            this.close();
            if (e instanceof Error)
            {
                throw (Error) e;
            }
            if (e instanceof RuntimeException)
            {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e);
        }
    }

    public int getUpdateCount()
    {
        return this.updateCount;
    }

    @Override
    public List<AutoCloseable> getAdditionalCloseables()
    {
        return FastList.newList();
    }
}
