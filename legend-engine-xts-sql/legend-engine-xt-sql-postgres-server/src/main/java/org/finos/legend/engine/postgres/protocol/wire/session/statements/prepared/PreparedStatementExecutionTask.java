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

package org.finos.legend.engine.postgres.protocol.wire.session.statements.prepared;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.finos.legend.engine.postgres.protocol.wire.serialization.ResultSetReceiver;
import org.finos.legend.engine.postgres.protocol.wire.session.Session;
import org.finos.legend.engine.postgres.protocol.wire.session.statements.result.PostgresResultSet;
import org.finos.legend.engine.postgres.utils.OpenTelemetryUtil;

import java.util.concurrent.Callable;

public class PreparedStatementExecutionTask implements Callable<Boolean>
{
    private final PostgresPreparedStatement preparedStatement;
    private final ResultSetReceiver resultSetReceiver;

    public PreparedStatementExecutionTask(PostgresPreparedStatement preparedStatement, ResultSetReceiver resultSetReceiver)
    {
        this.preparedStatement = preparedStatement;
        this.resultSetReceiver = resultSetReceiver;
    }

    @Override
    public Boolean call()
    {
        OpenTelemetryUtil.TOTAL_EXECUTE.add(1);
        OpenTelemetryUtil.ACTIVE_EXECUTE.add(1);
        long startTime = System.currentTimeMillis();

        Tracer tracer = OpenTelemetryUtil.getTracer();
        Span span = tracer.spanBuilder("PreparedStatement ExecutionTask Execute").startSpan();
        try (Scope ignored = span.makeCurrent())
        {
            boolean results = true;
            if (!preparedStatement.isExecuted())
            {
                results = preparedStatement.execute();
            }
            span.addEvent("receivedResults");
            if (!results)
            {
                resultSetReceiver.allFinished();
            }
            else
            {
                PostgresResultSet rs = preparedStatement.getResultSet();
                int maxRows = preparedStatement.getMaxRows();
                resultSetReceiver.sendResultSet(rs, maxRows);
                if (maxRows == 0)
                {
                    resultSetReceiver.allFinished();
                }
                else
                {
                    resultSetReceiver.batchFinished();
                }
            }
            OpenTelemetryUtil.TOTAL_SUCCESS_EXECUTE.add(1);
        }
        catch (Exception e)
        {
            span.setStatus(StatusCode.ERROR, Session.FAILED_TO_EXECUTE);
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
