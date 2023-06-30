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

import io.opentracing.Span;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.engine.plan.execution.result.ExecutionActivity;
import org.finos.legend.engine.plan.execution.result.ResultVisitor;
import org.finos.legend.engine.plan.execution.stores.StoreExecutableManager;
import org.finos.legend.engine.plan.execution.stores.relational.activity.RelationalExecutionActivity;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.SQLExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.result.SQLResultColumn;
import org.finos.legend.engine.shared.core.api.request.RequestContext;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.pac4j.core.profile.CommonProfile;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

public class SQLExecutionResult extends SQLResult
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");

    private final org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.SQLExecutionNode SQLExecutionNode;
    private final String databaseTimeZone;
    private final Calendar calendar;

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
        this(activities, SQLExecutionNode, databaseType, databaseTimeZone, connection, profiles, temporaryTables, topSpan, new RequestContext());
    }

    public SQLExecutionResult(List<ExecutionActivity> activities, SQLExecutionNode SQLExecutionNode, String databaseType, String databaseTimeZone, Connection connection, MutableList<CommonProfile> profiles, List<String> temporaryTables, Span topSpan, RequestContext requestContext)
    {
        this(activities, SQLExecutionNode, databaseType, databaseTimeZone, connection, profiles, temporaryTables, topSpan, requestContext, true);
    }

    public SQLExecutionResult(List<ExecutionActivity> activities, SQLExecutionNode SQLExecutionNode, String databaseType, String databaseTimeZone, Connection connection, MutableList<CommonProfile> profiles, List<String> temporaryTables, Span topSpan, RequestContext requestContext, boolean logSQLWithParamValues)
    {
        super("success", connection, activities, databaseType, temporaryTables, requestContext);
        this.SQLExecutionNode = SQLExecutionNode;
        this.databaseTimeZone = databaseTimeZone;
        this.calendar = new GregorianCalendar(TimeZone.getTimeZone(databaseTimeZone));
        this.topSpan = topSpan;
        try
        {
            long start = System.currentTimeMillis();
            RelationalExecutionActivity activity = ((RelationalExecutionActivity) activities.get(activities.size() - 1));
            String sql = activity.comment != null ? activity.comment.concat("\n").concat(activity.sql) : activity.sql;
            String logMessage = logSQLWithParamValues ? sql : SQLExecutionNode.sqlQuery();
            LOGGER.info(new LogInfo(profiles, LoggingEventType.EXECUTION_RELATIONAL_START, logMessage).toString());
            if (this.getRequestContext() != null)
            {
                StoreExecutableManager.INSTANCE.addExecutable(this.getRequestContext(), this);
            }
            this.resultSet = this.getStatement().executeQuery(sql);
            LOGGER.info(new LogInfo(profiles, LoggingEventType.EXECUTION_RELATIONAL_STOP, (double) System.currentTimeMillis() - start).toString());
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

            if (this.getRequestContext() != null)
            {
                StoreExecutableManager.INSTANCE.removeExecutable(this.getRequestContext(), this);
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

    @Override
    public List<AutoCloseable> getAdditionalCloseables()
    {
        return Lists.fixedSize.with(this.resultSet);
    }

    public SQLExecutionNode getSQLExecutionNode()
    {
        return this.SQLExecutionNode;
    }

    public String getDatabaseTimeZone()
    {
        return this.databaseTimeZone;
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
}
