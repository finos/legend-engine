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

package org.finos.legend.engine.plan.execution.result.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.util.GlobalTracer;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.collections.impl.block.procedure.checked.ThrowingProcedure;
import org.finos.legend.engine.plan.execution.result.TDSResult;

public abstract class TDSResultToPureFormatSerializer extends Serializer
{
    protected final ObjectMapper objectMapper = ExecutionResultObjectMapperFactory.getNewObjectMapper();
    private final ThrowingProcedure<JsonGenerator> start_token;
    private final ThrowingProcedure<JsonGenerator> end_token;
    protected final TDSResult tdsResult;
    protected final List<TDSColumnWithSerializer<Object>> valueSerializers;

    public TDSResultToPureFormatSerializer(TDSResult tdsResult, ThrowingProcedure<JsonGenerator> start_token, ThrowingProcedure<JsonGenerator> end_token)
    {
        this.tdsResult = tdsResult;
        this.start_token = start_token;
        this.end_token = end_token;
        this.valueSerializers = this.tdsResult.getResultBuilder().columns.stream().map(TDSColumnWithSerializer::new).collect(Collectors.toList());
    }

    @Override
    public void stream(OutputStream stream)
    {
        try (JsonGenerator generator = objectMapper.getFactory().createGenerator(stream))
        {
            this.start_token.safeValue(generator);
            this.streamValues(generator);
            this.end_token.safeValue(generator);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            this.tdsResult.close();
        }
    }

    public abstract void streamValues(JsonGenerator generator) throws IOException;

    public abstract void processRow(JsonGenerator generator, Object[] row) throws IOException;

    protected final void streamRows(JsonGenerator generator) throws IOException
    {
        int rowCount = 0;

        try (Stream<Object[]> rows = tdsResult.rowsStream())
        {
            Iterator<Object[]> iterator = rows.iterator();

            Span firstSpan = GlobalTracer.get().buildSpan("TDS Streaming: Fetch first row").start();
            try (Scope ignored = GlobalTracer.get().activateSpan(firstSpan))
            {
                if (iterator.hasNext())
                {
                    this.processRow(generator, iterator.next());
                    rowCount++;
                }
            }
            finally
            {
                firstSpan.finish();
            }

            Span remainingSpan = GlobalTracer.get().buildSpan("TDS Streaming: remaining rows").start();
            try (Scope ignore = GlobalTracer.get().activateSpan(remainingSpan))
            {
                while (iterator.hasNext())
                {
                    this.processRow(generator, iterator.next());
                    rowCount++;
                }
                remainingSpan.log("rowCount: " + rowCount);
            }
            finally
            {
                remainingSpan.finish();
            }
        }
    }

}
