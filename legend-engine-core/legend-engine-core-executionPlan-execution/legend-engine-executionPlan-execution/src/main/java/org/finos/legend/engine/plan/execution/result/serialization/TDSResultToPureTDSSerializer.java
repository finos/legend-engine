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
import java.util.List;
import org.finos.legend.engine.plan.execution.result.ExecutionActivity;
import org.finos.legend.engine.plan.execution.result.TDSResult;

public class TDSResultToPureTDSSerializer extends TDSResultToPureFormatSerializer
{
    public TDSResultToPureTDSSerializer(TDSResult tdsResult)
    {
        super(tdsResult, JsonGenerator::writeStartObject, JsonGenerator::writeEndObject);
    }

    @Override
    public void streamValues(JsonGenerator generator) throws IOException
    {
        generator.writeObjectField("builder", this.tdsResult.getResultBuilder());
        streamResult(generator);
        processActivities(generator, this.tdsResult.activities);
        if (this.tdsResult.generationInfo != null)
        {
            generator.writeObjectField("generationInfo", this.tdsResult.generationInfo);
        }
    }

    private void streamResult(JsonGenerator generator) throws IOException
    {
        generator.writeObjectFieldStart("result");
        processColumns(generator);
        generator.writeArrayFieldStart("rows");
        streamRows(generator);
        generator.writeEndArray();
        generator.writeEndObject();
    }

    private void processActivities(JsonGenerator generator, List<ExecutionActivity> activities) throws IOException
    {
        generator.writeFieldName("activities");
        this.objectMapper.writeValue(generator, activities);
    }

    private void processColumns(JsonGenerator generator) throws IOException
    {
        generator.writeArrayFieldStart("columns");
        for (TDSColumnWithSerializer<Object> c : this.valueSerializers)
        {
            generator.writeString(c.tdsColumn.name);
        }
        generator.writeEndArray();
    }

    @Override
    public void processRow(JsonGenerator generator, Object[] rowValues) throws IOException
    {
        generator.writeStartObject();
        generator.writeArrayFieldStart("values");
        for (int i = 0; i < this.valueSerializers.size(); i++)
        {
            this.valueSerializers.get(i).serialize(generator, rowValues[i]);
        }
        generator.writeEndArray();
        generator.writeEndObject();
    }
}
