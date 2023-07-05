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
import org.finos.legend.engine.plan.execution.result.builder.tds.TDSBuilder;
import org.finos.legend.engine.plan.execution.result.serialization.ExecutionResultObjectMapperFactory;
import org.finos.legend.engine.plan.execution.result.serialization.Serializer;
import org.finos.legend.engine.plan.execution.stores.relational.result.RelationalResult;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.result.TDSColumn;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.List;

public class RelationalResultToPureFormatSerializer extends Serializer
{
    public static final byte[] object_start = "{".getBytes();
    public static final byte[] b_Columns = "\"columns\":".getBytes();
    public static final byte[] b_rows = "\"rows\":".getBytes();
    public static final byte[] b_name = "\"name\":".getBytes();
    public static final byte[] b_type = "\"type\":".getBytes();
    public static final byte[] b_comma = ",".getBytes();
    public static final byte[] b_colon = ":".getBytes();
    public static final byte[] b_array_close = "]".getBytes();
    public static final byte[] b_array_open = "[".getBytes();
    public static final byte[] b_values = "\"values\":".getBytes();
    public static final byte[] object_end = "}".getBytes();

    protected final ObjectMapper objectMapper = ExecutionResultObjectMapperFactory.getNewObjectMapper();
    private final byte[] start_token;
    private final byte[] end_token;

    protected RelationalResult relationalResult;

    public RelationalResultToPureFormatSerializer(RelationalResult relationalResult, byte[] start_token, byte[] end_token)
    {
        this.relationalResult = relationalResult;
        this.start_token = start_token;
        this.end_token = end_token;
    }

    @Override
    public void stream(OutputStream stream)
    {
        try
        {
            stream.write(start_token);
            this.streamValues(stream);
            stream.write(end_token);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            this.relationalResult.close();
        }
    }

    public void streamValues(OutputStream outputStream) throws Exception
    {
    }

    public void processRow(OutputStream stream) throws IOException, SQLException
    {
    }

    public void processColumns(OutputStream stream, List<TDSColumn> columns) throws IOException
    {
    }

    public void streamColumns(OutputStream outputStream) throws Exception
    {
        this.processColumns(outputStream, ((TDSBuilder) relationalResult.builder).columns);
    }

    public void streamRows(OutputStream outputStream) throws Exception
    {
        int rowCount = 0;
        try (Scope ignored = GlobalTracer.get().buildSpan("Relational Streaming: Fetch first row").startActive(true))
        {
            if (!relationalResult.resultSet.isClosed() && relationalResult.resultSet.next())
            {
                this.processRow(outputStream);
                rowCount++;
            }
        }
        try (Scope scope = GlobalTracer.get().buildSpan("Relational Streaming: remaining rows").startActive(true))
        {
            while (!relationalResult.resultSet.isClosed() && relationalResult.resultSet.next())
            {
                outputStream.write(b_comma);
                this.processRow(outputStream);
                rowCount++;
            }
            scope.span().setTag("rowCount", rowCount);
            if (relationalResult.topSpan != null)
            {
                relationalResult.topSpan.setTag("lastQueryRowCount", rowCount);
            }
        }
    }
}
