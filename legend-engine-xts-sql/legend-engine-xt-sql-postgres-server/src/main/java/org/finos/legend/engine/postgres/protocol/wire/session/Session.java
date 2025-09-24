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

package org.finos.legend.engine.postgres.protocol.wire.session;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.function.Supplier;

import org.finos.legend.engine.postgres.protocol.wire.serialization.DescribeResult;
import org.finos.legend.engine.postgres.protocol.wire.serialization.FormatCodes;
import org.finos.legend.engine.postgres.PostgresServerException;
import org.finos.legend.engine.postgres.protocol.wire.serialization.ResultSetReceiver;
import org.finos.legend.engine.postgres.protocol.wire.session.statements.prepared.PostgresPreparedStatement;
import org.finos.legend.engine.postgres.protocol.wire.session.statements.prepared.PreparedStatementExecutionTask;
import org.finos.legend.engine.postgres.protocol.wire.session.statements.regular.PostgresStatement;
import org.finos.legend.engine.postgres.protocol.wire.session.statements.regular.StatementExecutionTask;
import org.finos.legend.engine.postgres.utils.OpenTelemetryUtil;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Session implements AutoCloseable
{

    private static final Logger LOGGER = LoggerFactory.getLogger(Session.class);
    public static final String FAILED_TO_EXECUTE = "Failed to execute";
    private final Map<String, Prepared> parsed = new ConcurrentHashMap<>();
    private final Map<String, Portal> portals = new ConcurrentHashMap<>();
    private final ExecutorService executorService;
    private final Identity identity;

    public Session(ExecutorService executorService, Identity identity)
    {
        this.executorService = executorService;
        this.identity = identity;
        OpenTelemetryUtil.ACTIVE_SESSIONS.add(1);
        OpenTelemetryUtil.TOTAL_SESSIONS.add(1);
    }

    public Identity getIdentity()
    {
        return identity;
    }

    public void sync()
    {
        //TODO do we need to handle batch requests?
        LOGGER.info("Sync");
    }

    public void parse(String statementName, String query, List<Integer> paramTypes, PostgresPreparedStatement postgresPreparedStatement)
    {
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("method=parse stmtName={} query={} paramTypes={}", statementName, query, paramTypes);
        }
        Prepared p = new Prepared();
        p.name = statementName;
        p.sql = query;
        p.paramType = paramTypes.toArray(new Integer[]{});
        p.prep = postgresPreparedStatement;

        this.parsed.put(p.name, p);
    }

    public int getParamType(String statementName, int idx)
    {
        Prepared stmt = getSafeStmt(statementName);
        return stmt.paramType[idx];
    }

    public void bind(String portalName, String statementName, List<Object> params, FormatCodes.FormatCode[] resultFormatCodes)
    {
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("method=bind portalName={} statementName={} params={}", portalName,
                    statementName, params);
        }
        Prepared preparedStmt = getSafeStmt(statementName);

        Portal portal = new Portal(portalName, preparedStmt, resultFormatCodes);
        portals.put(portalName, portal);

        PostgresPreparedStatement preparedStatement = preparedStmt.prep;
        for (int i = 1; i <= params.size(); i++)
        {
            try
            {
                preparedStatement.setObject(i, params.get(i - 1));
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
        try (Scope ignored = span.makeCurrent())
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
            }
            default:
                throw new PostgresServerException("Invalid type: " + type + ", valid types are: [P, S]");
        }
    }

    public CompletableFuture<Void> execute(String portalName, int maxRows, Function<String, ResultSetReceiver> resultSetReceiverProvider)
    {
        Tracer tracer = OpenTelemetryUtil.getTracer();
        Span span = tracer.spanBuilder("Session Execute").startSpan();
        try (Scope ignored1 = span.makeCurrent())
        {
            Portal portal = getSafePortal(portalName);
            String sql = portal.prep.sql;
            span.setAttribute("portal.name", portalName);
            span.setAttribute("query", sql);
            span.setAttribute("user", identity.getName());
            ResultSetReceiver resultSetReceiver = resultSetReceiverProvider.apply(sql);
            if (LOGGER.isDebugEnabled())
            {
                LOGGER.debug("Executing query {}/{} ", portalName, sql);
            }

            //TODO IDENTIFY THE USE CASE
            PostgresPreparedStatement preparedStatement = portal.prep.prep;
            if (preparedStatement == null)
            {
                resultSetReceiver.allFinished();
                return CompletableFuture.completedFuture(null);
            }
            preparedStatement.setMaxRows(maxRows);
            PreparedStatementExecutionTask task = new PreparedStatementExecutionTask(preparedStatement, resultSetReceiver);
            // Task does not wait for any future since it is always chained asynchronously
            CompletableFuture.runAsync(task::call, executorService);
            return resultSetReceiver.completionFuture();
        }
        catch (Exception e)
        {
            span.setStatus(StatusCode.ERROR, FAILED_TO_EXECUTE);
            span.recordException(e);
            throw PostgresServerException.wrapException(e);
        }
        finally
        {
            span.end();
        }
    }


    public CompletableFuture<Void> executeSimple(PostgresStatement statement, String query, Supplier<ResultSetReceiver> resultSetReceiverProvider)
    {
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Executing simple {} ", query);
        }
        Tracer tracer = OpenTelemetryUtil.getTracer();
        Span span = tracer.spanBuilder("Session Execute Simple").startSpan();
        try (Scope ignored1 = span.makeCurrent())
        {
            span.addEvent("submit StatementExecutionTask");
            ResultSetReceiver resultSetReceiver = resultSetReceiverProvider.get();
            Context.taskWrapping(executorService).submit(new StatementExecutionTask(statement, query, resultSetReceiver));
            return resultSetReceiver.completionFuture();
        }
        catch (Exception e)
        {
            span.setStatus(StatusCode.ERROR, "Failed to execute simple query");
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


}
