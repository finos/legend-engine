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

import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.engine.plan.execution.result.ExecutionActivity;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.ResultVisitor;
import org.finos.legend.engine.plan.execution.stores.StoreExecutable;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseConnection;
import org.finos.legend.engine.shared.core.api.request.RequestContext;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.*;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.function.Consumer;

public abstract class SQLResult extends Result implements StoreExecutable
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(SQLResult.class);

    private final String databaseType;
    private final List<String> temporaryTables;
    private final Connection connection;
    private final Statement statement;

    private final RequestContext requestContext;

    public SQLResult(String status, Connection connection, DatabaseConnection protocolConnection, List<ExecutionActivity> activities, String databaseType, List<String> temporaryTables, RequestContext requestContext)
    {
        super(status, activities);

        this.databaseType = databaseType;
        this.temporaryTables = temporaryTables;
        this.requestContext = requestContext;
        this.connection = connection;
        
        try
        {
            this.statement = connection.createStatement();
            if (DatabaseType.MemSQL.name().equals(databaseType))
            {
                this.statement.setFetchSize(100);
            }
            if (protocolConnection.queryTimeOutInSeconds != null)
            {
                this.statement.setQueryTimeout(protocolConnection.queryTimeOutInSeconds);
            }
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

    @Override
    public <T> T accept(ResultVisitor<T> resultVisitor)
    {
        return ((RelationalResultVisitor<T>) resultVisitor).visit(this);
    }

    public String getDatabaseType()
    {
        return this.databaseType;
    }

    public List<String> getTemporaryTables()
    {
        return this.temporaryTables;
    }

    public RequestContext getRequestContext()
    {
        return this.requestContext;
    }
    
    public Connection getConnection()
    {
        return this.connection;
    }

    public Statement getStatement()
    {
        return this.statement;
    }

    public abstract List<AutoCloseable> getAdditionalCloseables();

    @Override
    public void close()
    {
        DatabaseManager databaseManager = DatabaseManager.fromString(this.getDatabaseType());
        if (this.getTemporaryTables() != null && this.getStatement() != null)
        {
            this.getTemporaryTables().forEach((Consumer<? super String>) table ->
            {
                try
                {
                    this.getStatement().execute(databaseManager.relationalDatabaseSupport().dropTempTable(table));
                }
                catch (Exception ignored)
                {
                }
            });
        }

        Consumer<AutoCloseable> closingFunction = (AutoCloseable c) ->
        {
            if (c != null)
            {
                try
                {
                    c.close();
                }
                catch (Exception ignored)
                {
                }
            }
        };

        FastList.newList(this.getAdditionalCloseables()).with(this.getStatement(), this.getConnection())
                .forEach((Procedure<AutoCloseable>) closingFunction::accept);
        super.close();
    }

    @Override
    public void cancel()
    {
        try
        {
            if (!this.getStatement().isClosed())
            {
                this.getStatement().cancel();
                LOGGER.info(new LogInfo(Identity.getAnonymousIdentity().getName(), LoggingEventType.EXECUTABLE_CANCELLATION, "Successful cancellation of  RelationalResult " + this.getRequestContext()).toString());

            }
        }
        catch (Exception e)
        {
            LOGGER.error(new LogInfo(Identity.getAnonymousIdentity().getName(), LoggingEventType.EXECUTABLE_CANCELLATION_ERROR, "Unable to cancel  RelationalResult  for session " + this.getRequestContext() + " " + e.getMessage()).toString());
        }
    }
}
