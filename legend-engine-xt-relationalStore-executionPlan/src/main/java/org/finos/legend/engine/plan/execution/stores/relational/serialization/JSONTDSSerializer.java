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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.result.builder.tds.TDSBuilder;
import org.finos.legend.engine.plan.execution.result.serialization.ExecutionResultObjectMapperFactory;
import org.finos.legend.engine.plan.execution.result.serialization.Serializer;
import org.finos.legend.engine.plan.execution.stores.relational.result.RelationalResult;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.List;

public class JSONTDSSerializer extends Serializer
{
    private static final byte[] object_start = "{".getBytes();
    private static final byte[] b_Columns = "\"columns\":".getBytes();
    private static final byte[] b_rows = "\"rows\":[".getBytes();
    private static final byte[] b_comma = ",".getBytes();
    private static final byte[] b_array_close = "]".getBytes();
    private static final byte[] b_array_open = "[".getBytes();
    private static final byte[] b_values = "\"values\":".getBytes();
    private static final byte[] object_end = "}".getBytes();

    private final ObjectMapper objectMapper = ExecutionResultObjectMapperFactory.getNewObjectMapper();
    private final Boolean wrapRowsWithValues;
    private final Boolean wrapWithValues;
    private final RelationalResult relationalResult;

    public JSONTDSSerializer(RelationalResult relationalResult, Boolean wrapWithValues, Boolean wrapRowsWithValues)
    {
        this.relationalResult = relationalResult;
        this.wrapWithValues = wrapWithValues;
        this.wrapRowsWithValues = wrapRowsWithValues;
    }

    @Override
    public void stream(OutputStream stream)
    {
        try
        {
            stream.write(object_start);
            streamValues(stream);
            stream.write(object_end);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            relationalResult.close();
        }
    }

    private void streamValues(OutputStream outputStream) throws Exception
    {
        if (this.wrapWithValues)
        {
            outputStream.write(b_values);
            outputStream.write(b_array_open);
            outputStream.write(object_start);
        }


        streamColumns(outputStream);
        outputStream.write(b_comma);
        streamRows(outputStream);

        if (this.wrapWithValues)
        {
            outputStream.write(object_end);
            outputStream.write(b_array_close);
        }
    }

    private void streamColumns(OutputStream outputStream) throws Exception
    {
        outputStream.write(b_Columns);
        streamCollection(outputStream, ((TDSBuilder) relationalResult.builder).columns);
    }

    private void streamRows(OutputStream outputStream) throws Exception
    {
        outputStream.write(b_rows);

        int rowCount = 0;
        try (Scope ignored = GlobalTracer.get().buildSpan("Relational Streaming: Fetch first row").startActive(true))
        {

            if (relationalResult.resultSet.next())
            {
                processRow(outputStream);
            }
        }
        try (Scope scope = GlobalTracer.get().buildSpan("Relational Streaming: remaining rows").startActive(true))
        {
            while (relationalResult.resultSet.next())
            {
                outputStream.write(b_comma);
                processRow(outputStream);
            }
            scope.span().setTag("rowCount", rowCount);
            if (relationalResult.topSpan != null)
            {
                relationalResult.topSpan.setTag("lastQueryRowCount", rowCount);
            }
        }

        outputStream.write(b_array_close);
    }

    private void processRow(OutputStream outputStream) throws IOException, SQLException
    {
        if (this.wrapRowsWithValues)
        {
            outputStream.write(object_start);
            outputStream.write(b_values);
        }
        outputStream.write(b_array_open);

        MutableList<Function<Object, Object>> transformers = relationalResult.getTransformers();

        for (int i = 1; i <= relationalResult.columnCount - 1; i++)
        {
            objectMapper.writeValue(outputStream, transformers.get(i - 1).valueOf(relationalResult.getValue(i)));
            outputStream.write(b_comma);
        }

        objectMapper.writeValue(outputStream, transformers.get(relationalResult.columnCount - 1).valueOf(relationalResult.getValue(relationalResult.columnCount)));
        outputStream.write(b_array_close);

        if (this.wrapRowsWithValues)
        {
            outputStream.write(object_end);
        }
    }

    private void streamCollection(OutputStream outputStream, List collection) throws IOException
    {
        outputStream.write(b_array_open);
        for (int i = 0; i < collection.size() - 1; i++)
        {
            objectMapper.writeValue(outputStream, collection.get(i));
            outputStream.write(b_comma);
        }
        objectMapper.writeValue(outputStream, collection.get(collection.size() - 1));
        outputStream.write(b_array_close);
        outputStream.flush();
    }
}
