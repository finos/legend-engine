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
import org.finos.legend.engine.plan.execution.result.TDSResult;

public class TDSResultToPureTDSToObjectSerializer extends TDSResultToPureFormatSerializer
{
    public TDSResultToPureTDSToObjectSerializer(TDSResult tdsResult)
    {
        super(tdsResult, JsonGenerator::writeStartArray, JsonGenerator::writeEndArray);
    }

    public void streamValues(JsonGenerator generator) throws IOException
    {
        streamRows(generator);
    }

    public void processRow(JsonGenerator generator, Object[] rowValues) throws IOException
    {
        generator.writeStartObject();
        for (int i = 0; i < this.valueSerializers.size(); i++)
        {
            generator.writeFieldName(tdsResult.getResultBuilder().columns.get(i).name);
            this.valueSerializers.get(i).serialize(generator, rowValues[i]);
        }
        generator.writeEndObject();
    }
}
