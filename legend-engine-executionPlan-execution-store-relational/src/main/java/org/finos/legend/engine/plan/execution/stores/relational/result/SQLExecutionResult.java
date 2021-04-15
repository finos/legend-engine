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
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;

import io.opentracing.Span;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.engine.plan.execution.result.ExecutionActivity;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.ResultVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.SQLExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.result.SQLResultColumn;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.pac4j.core.profile.CommonProfile;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.function.Consumer;

public class SQLExecutionResult extends Result
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");

    private final org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.SQLExecutionNode SQLExecutionNode;
    private final String databaseType;
    private final String databaseTimeZone;
    private final Calendar calendar;
    private final List<String> temporaryTables;

    private final Connection connection;
    private final Statement statement;
    private final ResultSet resultSet;
    private final ResultSetMetaData resultSetMetaData;
    private final String executedSql;

    private final int columnCount;
    private final List<String> columnNames = FastList.newList();
    private final List<ResultColumn> resultColumns = FastList.newList();
    private final List<SQLResultColumn> sqlResultColumns;

    public Span topSpan;

    public SQLExecutionResult(List<ExecutionActivity> activities, SQLExecutionNode SQLExecutionNode, String databaseType, String databaseTimeZone, Connection connection, MutableList<CommonProfile> profiles, List<String> temporaryTables, Span topSpan)
    {
        super("success", activities);

        this.SQLExecutionNode = SQLExecutionNode;
        this.databaseType = databaseType;
        this.databaseTimeZone = databaseTimeZone;
        this.calendar = new GregorianCalendar(TimeZone.getTimeZone(databaseTimeZone));
        this.temporaryTables = temporaryTables;

        this.topSpan = topSpan;

        try
        {
            this.connection = connection;
            this.statement = connection.createStatement();

            long start = System.currentTimeMillis();
            String sql = ((RelationalExecutionActivity) activities.get(activities.size() - 1)).sql;
            LOGGER.info(new LogInfo(profiles, LoggingEventType.EXECUTION_RELATIONAL_START, sql).toString());
            this.resultSet = this.statement.executeQuery(sql);
            LOGGER.info(new LogInfo(profiles, LoggingEventType.EXECUTION_RELATIONAL_STOP, System.currentTimeMillis() - start).toString());
            this.executedSql = sql;

            this.resultSetMetaData = resultSet.getMetaData();

            this.columnCount = this.resultSetMetaData.getColumnCount();

            this.sqlResultColumns = this.SQLExecutionNode.getSQLResultColumns();
            for (int i = 1; i <= this.columnCount; i++)
            {
                String columnLabel = resultSetMetaData.getColumnLabel(i);
                this.columnNames.add(columnLabel);
                SQLResultColumn col = this.sqlResultColumns.get(i - 1);
                this.resultColumns.add(new ResultColumn(i, col.label, col.dataType, this.resultSetMetaData.getColumnType(i)));
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

    public SQLExecutionNode getSQLExecutionNode()
    {
        return this.SQLExecutionNode;
    }

    public String getDatabaseType()
    {
        return this.databaseType;
    }

    public String getDatabaseTimeZone()
    {
        return this.databaseTimeZone;
    }

    public List<String> getTemporaryTables()
    {
        return temporaryTables;
    }

    public Connection getConnection()
    {
        return this.connection;
    }

    public Statement getStatement()
    {
        return this.statement;
    }

    public ResultSet getResultSet()
    {
        return this.resultSet;
    }

    public ResultSetMetaData getResultSetMetaData()
    {
        return this.resultSetMetaData;
    }

    public int getColumnCount()
    {
        return this.columnCount;
    }

    public List<SQLResultColumn> getSqlResultColumns()
    {
        return sqlResultColumns;
    }

    public Span getTopSpan()
    {
        return topSpan;
    }

    public List<String> getColumnNames()
    {
        return this.columnNames;
    }

    public List<ResultColumn> getResultColumns()
    {
        return this.resultColumns;
    }

    public String getExecutedSql()
    {
        return this.executedSql;
    }

    public Object getTransformedValue(int columnIndex)
    {
        ResultColumn resultColumn = this.getResultColumns().get(columnIndex - 1);
        return resultColumn.getTransformedValue(this.getResultSet(), calendar);
    }

    @Override
    public void close()
    {
        DatabaseManager databaseManager = DatabaseManager.fromString(this.SQLExecutionNode.getDatabaseTypeName());
        if (this.temporaryTables != null && this.statement != null)
        {
            this.temporaryTables.forEach((Consumer<? super String>) table -> {
                try
                {
                    statement.execute(databaseManager.relationalDatabaseSupport().dropTempTable(table));
                }
                catch (Exception ignored)
                {
                }
            });
        }

        Consumer<AutoCloseable> closingFunction = (AutoCloseable c) -> {
            if (c != null)
            {
                try
                {
                    c.close();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        };

        FastList.newListWith(this.resultSet, this.statement, this.connection).forEach((Procedure<AutoCloseable>) closingFunction::accept);
    }
}
