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

package org.finos.legend.engine.plan.execution.stores.relational.serialization;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.result.builder.tds.TDSBuilder;
import org.finos.legend.engine.plan.execution.stores.relational.result.RelationalResult;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;

public class RelationalResultToPureTDSToObjectSerializer extends RelationalResultToPureFormatSerializer
{
    public RelationalResultToPureTDSToObjectSerializer(RelationalResult relationalResult)
    {
        super(relationalResult, b_array_open, b_array_close);
    }

    public void streamValues(OutputStream outputStream) throws Exception
    {
        streamRows(outputStream);
    }

    public void processRow(OutputStream outputStream) throws IOException, SQLException
    {
        outputStream.write(object_start);

        MutableList<Function<Object, Object>> transformers = relationalResult.getTransformers();

        for (int i = 1; i <= relationalResult.columnCount - 1; i++)
        {
            objectMapper.writeValue(outputStream, ((TDSBuilder) relationalResult.builder).columns.get(i - 1).name);
            outputStream.write(b_colon);
            objectMapper.writeValue(outputStream, transformers.get(i - 1).valueOf(relationalResult.getValue(i)));
            outputStream.write(b_comma);
        }

        objectMapper.writeValue(outputStream, ((TDSBuilder) relationalResult.builder).columns.get(relationalResult.columnCount - 1).name);
        outputStream.write(b_colon);
        objectMapper.writeValue(outputStream, transformers.get(relationalResult.columnCount - 1).valueOf(relationalResult.getValue(relationalResult.columnCount)));

        outputStream.write(object_end);
    }
}
