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
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Objects;
import org.eclipse.collections.impl.block.procedure.checked.ThrowingProcedure2;
import org.finos.legend.engine.plan.dependencies.domain.date.PureDate;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.result.TDSColumn;

public class TDSColumnWithSerializer<T>
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
            case "StrictDate":
                return (ThrowingProcedure2<JsonGenerator, PureDate>) (jg, d) ->
                {
                    String formatted = d.toString();
                    if (d.hasMinute())
                    {
                        formatted += "Z";
                    }
                    jg.writeString(formatted);
                };
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
