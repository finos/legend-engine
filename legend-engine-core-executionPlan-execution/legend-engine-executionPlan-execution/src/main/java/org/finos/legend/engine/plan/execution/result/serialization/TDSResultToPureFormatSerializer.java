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
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.collections.impl.block.procedure.checked.ThrowingProcedure;
import org.eclipse.collections.impl.block.procedure.checked.ThrowingProcedure2;
import org.finos.legend.engine.plan.dependencies.domain.date.PureDate;
import org.finos.legend.engine.plan.execution.result.TDSResult;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.result.TDSColumn;

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

    protected static class TDSColumnWithSerializer<T>
    {
        protected final TDSColumn tdsColumn;
        private final ThrowingProcedure2<JsonGenerator, T> serializer;

        public TDSColumnWithSerializer(TDSColumn tdsColumn)
        {
            this.tdsColumn = tdsColumn;
            this.serializer = (ThrowingProcedure2<JsonGenerator, T>) TDSColumnWithSerializer.serializer(tdsColumn.type);
        }

        private static ThrowingProcedure2<JsonGenerator, ?> serializer(String type)
        {
            switch (type)
            {
                case "String":
                    return (ThrowingProcedure2<JsonGenerator, String>) JsonGenerator::writeString;
                case "Integer":
                    return (ThrowingProcedure2<JsonGenerator, Long>) JsonGenerator::writeNumber;
                case "Float":
                case "Number":
                    return (ThrowingProcedure2<JsonGenerator, Double>) JsonGenerator::writeNumber;
                case "Decimal":
                    return (ThrowingProcedure2<JsonGenerator, BigDecimal>) JsonGenerator::writeNumber;
                case "Boolean":
                    return (ThrowingProcedure2<JsonGenerator, Boolean>) JsonGenerator::writeBoolean;
                case "Date":
                case "DateTime":
                    return (ThrowingProcedure2<JsonGenerator, PureDate>) (jg, d) -> jg.writeString(d.toInstant().toString());
                case "StrictDate":
                    return (ThrowingProcedure2<JsonGenerator, PureDate>) (jg, d) -> jg.writeString(d.toLocalDate().toString());
                default:
                    throw new UnsupportedOperationException("TDS type not supported: " + type);
            }
        }

        public void serialize(JsonGenerator generator, T rowValue) throws IOException
        {
            try
            {
                if (Objects.isNull(rowValue))
                {
                    generator.writeNull();
                }
                else
                {
                    this.serializer.safeValue(generator, rowValue);
                }
            }
            catch (IOException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
    }
}
