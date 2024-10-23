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

package org.finos.legend.engine.plan.execution.result;

import java.util.List;
import java.util.stream.Stream;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.plan.dependencies.store.shared.IResult;
import org.finos.legend.engine.plan.execution.result.builder.tds.TDSBuilder;
import org.finos.legend.engine.plan.execution.result.serialization.*;
import org.finos.legend.engine.plan.execution.stores.StoreExecutable;
import org.finos.legend.engine.plan.execution.stores.StoreExecutableManager;
import org.finos.legend.engine.shared.core.operational.Assert;

public final class TDSResult extends StreamingResult implements IResult, StoreExecutable
{
    private final TDSBuilder builder;
    private final Stream<Object[]> rows;
    private final List<Object[]> rowsInMemory;
    private final String sessionId;

    public TDSResult(Stream<Object[]> rows, TDSBuilder builder, List<ExecutionActivity> activities, String sessionId)
    {
        super(activities);
        this.sessionId = sessionId;
        this.builder = builder;
        this.rows = rows.peek(x -> Assert.assertTrue(x.length == builder.columns.size(), () -> "Wrong number of values on row"));
        this.rowsInMemory = null;

        if (sessionId != null)
        {
            StoreExecutableManager.INSTANCE.addExecutable(this.sessionId, this);
        }
    }

    private TDSResult(List<Object[]> rows, TDSBuilder builder, List<ExecutionActivity> activities, String sessionId)
    {
        super(activities);
        this.sessionId = sessionId;
        this.builder = builder;
        this.rows = null;
        this.rowsInMemory = rows;
    }

    @Override
    public <T> T accept(ResultVisitor<T> resultVisitor)
    {
        return resultVisitor.visit(this);
    }

    @Override
    public TDSBuilder getResultBuilder()
    {
        return this.builder;
    }

    @Override
    public Result realizeInMemory()
    {
        if (this.rows != null)
        {
            try
            {
                long limit = getRealizeRowLimit();
                List<Object[]> rows = Lists.mutable.empty();

                this.rows.forEach(x ->
                {
                    Assert.assertFalse(rows.size() == limit, () -> "TDS Result cannot be realized in memory since is bigger than " + limit);
                    rows.add(x);
                });

                return new TDSResult(
                        rows,
                        this.builder,
                        this.activities,
                        this.sessionId
                );
            }
            finally
            {
                this.close();
            }
        }
        else
        {
            return this;
        }
    }

    @Override
    public void cancel()
    {
        if (this.rows != null)
        {
            this.rows.close();
        }
    }

    @Override
    public void close()
    {
        if (this.sessionId != null)
        {
            StoreExecutableManager.INSTANCE.removeExecutable(this.sessionId, this);
        }
        this.cancel();
    }

    @Override
    public Serializer getSerializer(SerializationFormat format)
    {
        switch (format)
        {
            case PURE:
            case DEFAULT:
                return new TDSResultToPureTDSSerializer(this);
            case PURE_TDSOBJECT:
                return new TDSResultToPureTDSToObjectSerializer(this);
            case RAW:
                return new TDSResultToRawTDSObjectSerializer(this);
            case CSV:
            case CSV_TRANSFORMED:
                return new TDSResultToCSVSerializer(this, true);
            default:
                this.close();
                throw new RuntimeException(format + " format not currently supported with TDSResult");
        }

    }

    public Stream<Object[]> rowsStream()
    {
        if (this.rows != null)
        {
            return this.rows;
        }
        else
        {
            return this.rowsInMemory.stream();
        }
    }
}
