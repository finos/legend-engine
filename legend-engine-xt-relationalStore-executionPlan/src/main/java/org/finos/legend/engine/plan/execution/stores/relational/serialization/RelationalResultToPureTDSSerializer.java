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

import org.finos.legend.engine.plan.execution.stores.relational.result.RelationalResult;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.result.TDSColumn;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.MutableList;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.List;

public class RelationalResultToPureTDSSerializer extends RelationalResultToPureFormatSerializer
{
    public RelationalResultToPureTDSSerializer(RelationalResult relationalResult)
    {
        super(relationalResult, object_start, object_end);
    }

    public void streamValues(OutputStream outputStream) throws Exception
    {
        outputStream.write(b_Columns);
        streamColumns(outputStream);

        outputStream.write(b_comma);

        outputStream.write(b_rows);
        outputStream.write(b_array_open);
        streamRows(outputStream);
        outputStream.write(b_array_close);
    }

    private void streamTDSColumnInPureFormat(OutputStream outputStream, TDSColumn col)
    {
        try
        {
            outputStream.write(object_start);
            outputStream.write(b_name);
            objectMapper.writeValue(outputStream, col.name);
            outputStream.write(b_comma);
            outputStream.write(b_type);
            objectMapper.writeValue(outputStream, col.type);
            outputStream.write(object_end);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public void processColumns(OutputStream outputStream, List<TDSColumn> columns) throws IOException
    {
        outputStream.write(b_array_open);
        for (int i = 0; i < columns.size() - 1; i++)
        {
            streamTDSColumnInPureFormat(outputStream, columns.get(i));
            outputStream.write(b_comma);
        }
        streamTDSColumnInPureFormat(outputStream, columns.get(columns.size() - 1));
        outputStream.write(b_array_close);
        outputStream.flush();
    }

    public void processRow(OutputStream outputStream) throws IOException, SQLException
    {
        outputStream.write(object_start);
        outputStream.write(b_values);
        outputStream.write(b_array_open);

        MutableList<Function<Object, Object>> transformers = relationalResult.getTransformers();

        for (int i = 1; i <= relationalResult.columnCount - 1; i++)
        {
            objectMapper.writeValue(outputStream, transformers.get(i - 1).valueOf(relationalResult.getValue(i)));
            outputStream.write(b_comma);
        }

        objectMapper.writeValue(outputStream, transformers.get(relationalResult.columnCount - 1).valueOf(relationalResult.getValue(relationalResult.columnCount)));

        outputStream.write(b_array_close);
        outputStream.write(object_end);
    }
}
