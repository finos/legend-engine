/*
 * Licensed to Crate.io GmbH ("Crate") under one or more contributor
 * license agreements.  See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.  Crate licenses
 * this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial agreement.
 */

package org.finos.legend.engine.postgres;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import org.finos.legend.engine.language.sql.grammar.from.SQLGrammarParser;
import org.finos.legend.engine.language.sql.grammar.from.antlr4.SqlBaseParser;
import org.finos.legend.engine.postgres.handler.PostgresPreparedStatement;
import org.finos.legend.engine.postgres.handler.PostgresResultSet;
import org.finos.legend.engine.postgres.handler.PostgresStatement;
import org.finos.legend.engine.postgres.handler.SessionHandler;
import org.finos.legend.engine.postgres.utils.OpenTelemetryUtil;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Session implements AutoCloseable
{

    private static final Logger LOGGER = LoggerFactory.getLogger(Session.class);
    private final Map<String, Prepared> parsed = new HashMap<>();
    private final Map<String, Portal> portals = new HashMap<>();
    private final ExecutionDispatcher dispatcher;
    private ExecutorService executorService;
    private CompletableFuture<?> activeExecution;

    private Identity identity;

    public Session(SessionHandler dataSessionHandler, SessionHandler metaDataSessionHandler, ExecutorService executorService, Identity identity)
    {
        this.executorService = executorService;
        this.dispatcher = new ExecutionDispatcher(dataSessionHandler, metaDataSessionHandler);
        this.identity = identity;
        OpenTelemetryUtil.ACTIVE_SESSIONS.add(1);
        OpenTelemetryUtil.TOTAL_SESSIONS.add(1);
    }

    public Identity getIdentity()
    {
        return identity;
    }

    public CompletableFuture<?> sync()
    {
        //TODO do we need to handle batch requests?
        LOGGER.info("Sync");
        if (activeExecution == null)
        {
            CompletableFuture<String> completableFuture = new CompletableFuture<>();
            completableFuture.complete(null);
            return completableFuture;
        }
        else
        {
            CompletableFuture result = activeExecution;
            activeExecution = null;
            return result;
        }
    }

    public void parse(String statementName, String query, List<Integer> paramTypes)
    {
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("method=parse stmtName={} query={} paramTypes={}", statementName, query,
                    paramTypes);
        }

        Prepared p = new Prepared();
        p.name = statementName;
        p.sql = query;
        p.paramType = paramTypes.toArray(new Integer[] {});

        if (query != null)
        {
            try
            {
                SessionHandler sessionHandler;
                if (query.isEmpty())
                {
                    // Parser can't handle empty string, but postgres requires support for it
                    // Using an empty session handler for empty queries
                    sessionHandler = ExecutionDispatcher.getEmptySessionHandler();
                }
                else
                {
                    sessionHandler = getSessionHandler(query);
                }
                p.prep = sessionHandler.prepareStatement(query);
            }
            catch (Exception e)
            {
                throw PostgresServerException.wrapException(e);
            }
        }
        parsed.put(p.name, p);
    }

    /**
     * Identify type of query and return appropriate session handler
     * based on schema of the query.
     *
     * @param query SQL query to be executed
     * @return session handler for the given query
     */
    private SessionHandler getSessionHandler(String query)
    {
        SqlBaseParser parser = SQLGrammarParser.getSqlBaseParser(query, "query");
        SqlBaseParser.SingleStatementContext singleStatementContext = parser.singleStatement();
        SessionHandler sessionHandler = singleStatementContext.accept(dispatcher);
        if (sessionHandler == null)
        {
            throw new PostgresServerException(String.format("Unable to determine session handler for query[%s]", query));
        }
        return sessionHandler;
    }


    public int getParamType(String statementName, int idx)
    {
        Prepared stmt = getSafeStmt(statementName);
        return stmt.paramType[idx];
    }


    public void bind(String portalName, String statementName, List<Object> params,
                     FormatCodes.FormatCode[] resultFormatCodes)
    {
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("method=bind portalName={} statementName={} params={}", portalName,
                    statementName, params);
        }
        Prepared preparedStmt = getSafeStmt(statementName);

        Portal portal = new Portal(portalName, preparedStmt, resultFormatCodes);
        portals.put(portalName, portal);
/*        if (oldPortal != null) {
            // According to the wire protocol spec named portals should be removed explicitly and only
            // unnamed portals are implicitly closed/overridden.
            // We don't comply with the spec because we allow batching of statements, see #execute
            oldPortal.closeActiveConsumer();
        }*/

        PostgresPreparedStatement preparedStatement = portal.prep.prep;
        for (int i = 0; i < params.size(); i++)
        {
            try
            {
                preparedStatement.setObject(i, params.get(i));
            }
            catch (Exception e)
            {
                throw PostgresServerException.wrapException(e);
            }
        }
    }


    public DescribeResult describe(char type, String portalOrStatement)
    {
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("method=describe type={} portalOrStatement={}", type, portalOrStatement);
        }
        Tracer tracer = OpenTelemetryUtil.getTracer();
        Span span = tracer.spanBuilder("Session Describe").startSpan();
        try (Scope scope = span.makeCurrent())
        {
            span.setAttribute("type", String.valueOf(type));
            span.setAttribute("name", portalOrStatement);
            switch (type)
            {
                case 'P':
                    Portal portal = getSafePortal(portalOrStatement);
                    return describe('S', portal.prep.name);
                case 'S':
                    /*
                     * describe might be called without prior bind call.
                     *
                     * If the client uses server-side prepared statements this is usually the case.
                     *
                     * E.g. the statement is first prepared:
                     *
                     *      parse stmtName=S_1 query=insert into t (x) values ($1) paramTypes=[integer]
                     *      describe type=S portalOrStatement=S_1
                     *      sync
                     *
                     * and then used with different bind calls:
                     *
                     *      bind portalName= statementName=S_1 params=[0]
                     *      describe type=P portalOrStatement=
                     *      execute
                     *
                     *      bind portalName= statementName=S_1 params=[1]
                     *      describe type=P portalOrStatement=
                     *      execute
                     */

                    Prepared prepared = parsed.get(portalOrStatement);
                    try
                    {
                        PostgresPreparedStatement preparedStatement = prepared.prep;
                        if (portalOrStatement == null)
                        {
                            return new DescribeResult(null, null);
                        }
                        else
                        {
                            return new DescribeResult(preparedStatement.getMetaData(),
                                    preparedStatement.getParameterMetaData());
                        }
                    }
                    catch (Exception e)
                    {
                        throw PostgresServerException.wrapException(e);
                    }
                default:
                    throw new PostgresServerException("Unsupported type: " + type);
            }
        }
        finally
        {
            span.end();
        }
    }


    public FormatCodes.FormatCode[] getResultFormatCodes(String portal)
    {
        return getSafePortal(portal).resultColumnFormat;
    }


    public String getQuery(String portalName)
    {
        return getSafePortal(portalName).prep.sql;
    }


    /*TransactionState transactionState();
     */

    public void close()
    {
        clearState();
        OpenTelemetryUtil.ACTIVE_SESSIONS.add(-1);
    }

    public void close(char type, String name)
    {
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("method=close type={} name={}", type, name);
        }

        switch (type)
        {
            case 'P':
            {
                Portal portal = portals.get(name);
                if (portal == null)
                {
                    throw new PostgresServerException("Portal not found: " + name);
                }
                if (parsed.containsKey(portal.prep.name))
                {
                    close('S', portal.prep.name);
                }
                else
                {
                    LOGGER.warn("Skipping close of statement {}, from portal, as already closed",
                            portal.prep.name);
                }
                portals.remove(portal.name);
                return;
            }
            case 'S':
            {
                Prepared prepared = parsed.remove(name);
                if (prepared == null)
                {
                    throw new PostgresServerException("Prepared not found: " + name);
                }
                try
                {
                    prepared.prep.close();
                }
                catch (Exception e)
                {
                    throw PostgresServerException.wrapException(e);
                }
                parsed.remove(prepared.name);
                return;

               /* if (prepared != null) {
                    Iterator<Map.Entry<String, Portal>> it = portals.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry<String, Portal> entry = it.next();
                        Portal portal = entry.getValue();
                        if (portal.prep.equals(prepared)) {
                            try {
                                portal.prep.prep.close();
                            } catch (SQLException e) {
                                throw new RuntimeException(e);
                            }
                            it.remove();
                        }
                    }
                }
                return;*/
            }
            default:
                throw new PostgresServerException("Invalid type: " + type + ", valid types are: [P, S]");
        }
    }

    public CompletableFuture<?> execute(String portalName, int maxRows, ResultSetReceiver resultSetReceiver)
    {
        Tracer tracer = OpenTelemetryUtil.getTracer();
        Span span = tracer.spanBuilder("Session Execute").startSpan();
        try (Scope scope = span.makeCurrent())
        {
            Portal portal = getSafePortal(portalName);
            if (LOGGER.isDebugEnabled())
            {
                LOGGER.debug("Executing query {}/{} ", portalName, portal.prep.sql);
            }

            //TODO IDENTIFY THE USE CASE
            PostgresPreparedStatement preparedStatement = portal.prep.prep;
            if (preparedStatement == null)
            {
                resultSetReceiver.allFinished();
                return CompletableFuture.completedFuture(Boolean.TRUE);
            }

            preparedStatement.setMaxRows(maxRows);
            Context.taskWrapping(executorService).submit(new PreparedStatementExecutionTask(preparedStatement, resultSetReceiver));

            if (activeExecution == null)
            {
                activeExecution = resultSetReceiver.completionFuture();
            }
            else
            {
                activeExecution = activeExecution
                        .thenCompose(ignored -> resultSetReceiver.completionFuture());
            }
            return activeExecution;
        }
        catch (Exception e)
        {
            span.recordException(e);
            throw PostgresServerException.wrapException(e);
        }
        finally
        {
            span.end();
        }
    }


    public CompletableFuture<?> executeSimple(String query, ResultSetReceiver resultSetReceiver)
    {

        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Executing simple {} ", query);
        }
        Tracer tracer = OpenTelemetryUtil.getTracer();
        Span span = tracer.spanBuilder("Session Execute Simple").startSpan();
        try (Scope scope = span.makeCurrent())
        {
            PostgresStatement statement = getSessionHandler(query).createStatement();
            span.addEvent("submit StatementExecutionTask");
            Context.taskWrapping(executorService).submit(new StatementExecutionTask(statement, query, resultSetReceiver));
            if (activeExecution == null)
            {
                activeExecution = resultSetReceiver.completionFuture();
            }
            else
            {
                activeExecution = activeExecution
                        .thenCompose(ignored -> resultSetReceiver.completionFuture());
            }
            return activeExecution;
        }
        catch (Exception e)
        {
            span.recordException(e);
            throw PostgresServerException.wrapException(e);
        }
        finally
        {
            span.end();
        }
    }


    public void clearState()
    {
        LOGGER.info("clear state for session");
        for (String name : new ArrayList<>(portals.keySet()))
        {
            close('P', name);
        }
        for (String name : new ArrayList<>(parsed.keySet()))
        {
            close('S', name);
        }
    }


    private Prepared getSafeStmt(String statementName)
    {
        Prepared prepared = parsed.get(statementName);
        if (prepared == null)
        {
            throw new PostgresServerException("No statement found with name: " + statementName);
        }
        return prepared;
    }

    private Portal getSafePortal(String portalName)
    {
        Portal portal = portals.get(portalName);
        if (portal == null)
        {
            throw new PostgresServerException("Cannot find portal: " + portalName);
        }
        return portal;
    }


    /**
     * Represents a PostgeSQL Prepared Obj
     */
    static class Prepared
    {

        /**
         * Object name
         */
        String name;

        /**
         * The SQL Statement
         */
        String sql;

        /**
         * The prepared Statment
         */
        PostgresPreparedStatement prep;

        /**
         * The list of param types
         */
        Integer[] paramType;
    }

    /**
     * Represents a PostgreSQL Portal object
     */
    static class Portal
    {

        /**
         * The portal name
         */
        String name;

        /**
         * The format use in the result set column  (if set)
         */
        FormatCodes.FormatCode[] resultColumnFormat;

        /**
         * The prepared object
         */
        Prepared prep;


        public Portal(String portalName, Prepared preparedStmt,
                      FormatCodes.FormatCode[] resultColumnFormat)
        {
            this.name = portalName;
            this.prep = preparedStmt;
            this.resultColumnFormat = resultColumnFormat;
        }
    }


    private static class StatementExecutionTask implements Callable<Boolean>
    {
        private final PostgresStatement statement;
        private final ResultSetReceiver resultSetReceiver;

        private String query;

        public StatementExecutionTask(PostgresStatement statement, String query, ResultSetReceiver resultSetReceiver)
        {
            this.statement = statement;
            this.resultSetReceiver = resultSetReceiver;
            this.query = query;
        }

        @Override
        public Boolean call() throws Exception
        {
            OpenTelemetryUtil.TOTAL_EXECUTE.add(1);
            OpenTelemetryUtil.ACTIVE_EXECUTE.add(1);
            long startTime = System.currentTimeMillis();

            Tracer tracer = OpenTelemetryUtil.getTracer();
            Span span = tracer.spanBuilder("Statement ExecutionTask Execute").startSpan();
            try (Scope scope = span.makeCurrent())
            {
                boolean results = statement.execute(query);
                span.addEvent("receivedResults");
                if (!results)
                {
                    resultSetReceiver.allFinished();
                }
                else
                {
                    PostgresResultSet rs = statement.getResultSet();
                    resultSetReceiver.sendResultSet(rs);
                    resultSetReceiver.allFinished();
                }
                OpenTelemetryUtil.TOTAL_SUCCESS_EXECUTE.add(1);
                OpenTelemetryUtil.EXECUTE_DURATION.record(System.currentTimeMillis() - startTime);
            }
            catch (Exception e)
            {
                span.recordException(e);
                resultSetReceiver.fail(e);
                OpenTelemetryUtil.TOTAL_FAILURE_EXECUTE.add(1);
            }
            finally
            {
                span.end();
                OpenTelemetryUtil.ACTIVE_EXECUTE.add(-1);
            }
            return true;
        }
    }

    private static class PreparedStatementExecutionTask implements Callable<Boolean>
    {
        private final PostgresPreparedStatement preparedStatement;
        private final ResultSetReceiver resultSetReceiver;

        public PreparedStatementExecutionTask(PostgresPreparedStatement preparedStatement, ResultSetReceiver resultSetReceiver)
        {
            this.preparedStatement = preparedStatement;
            this.resultSetReceiver = resultSetReceiver;
        }

        @Override
        public Boolean call() throws Exception
        {
            OpenTelemetryUtil.TOTAL_EXECUTE.add(1);
            OpenTelemetryUtil.ACTIVE_EXECUTE.add(1);
            long startTime = System.currentTimeMillis();

            Tracer tracer = OpenTelemetryUtil.getTracer();
            Span span = tracer.spanBuilder("PreparedStatement ExecutionTask Execute").startSpan();
            try (Scope scope = span.makeCurrent())
            {
                boolean results = preparedStatement.execute();
                span.addEvent("receivedResults");
                if (!results)
                {
                    resultSetReceiver.allFinished();
                }
                else
                {
                    PostgresResultSet rs = preparedStatement.getResultSet();
                    resultSetReceiver.sendResultSet(rs);
                    resultSetReceiver.allFinished();
                }
                OpenTelemetryUtil.TOTAL_SUCCESS_EXECUTE.add(1);
            }
            catch (Exception e)
            {
                span.recordException(e);
                resultSetReceiver.fail(e);
                OpenTelemetryUtil.TOTAL_FAILURE_EXECUTE.add(1);
                OpenTelemetryUtil.EXECUTE_DURATION.record(System.currentTimeMillis() - startTime);

            }
            finally
            {
                span.end();
                OpenTelemetryUtil.ACTIVE_EXECUTE.add(-1);
            }
            return true;
        }
    }

}
