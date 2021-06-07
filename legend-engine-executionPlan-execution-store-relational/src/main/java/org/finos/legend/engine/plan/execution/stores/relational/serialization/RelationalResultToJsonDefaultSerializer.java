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
import com.fasterxml.jackson.databind.jsontype.NamedType;
import org.finos.legend.engine.plan.execution.stores.relational.activity.AggregationAwareActivity;
import org.finos.legend.engine.plan.execution.stores.relational.activity.RelationalExecutionActivity;
import org.finos.legend.engine.plan.execution.stores.relational.result.RelationalResult;
import org.finos.legend.engine.plan.execution.stores.relational.result.ResultInterpreterExtension;
import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.engine.plan.execution.result.serialization.ExecutionResultObjectMapperFactory;
import org.finos.legend.engine.plan.execution.result.serialization.Serializer;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.ServiceLoader;

public class RelationalResultToJsonDefaultSerializer extends Serializer
{
    private final ObjectMapper objectMapper = ExecutionResultObjectMapperFactory.getNewObjectMapper();
    private final RelationalResult relationalResult;
    private final byte[] b_builder = "{\"builder\": ".getBytes();
    private final byte[] b_generation = ", \"generationInfo\": ".getBytes();
    private final byte[] b_activities = ", \"activities\": [".getBytes();
    private final byte[] b_result = "], \"result\" : {".getBytes();
    private final byte[] b_sqlColumns = "\"columns\" : [".getBytes();
    private final byte[] b_rows = "], \"rows\" : [".getBytes();
    private final byte[] b_comma = ",".getBytes();
    private final byte[] b_values = "{\"values\": [".getBytes();
    private final byte[] b_end = "]}".getBytes();
    private final byte[] b_endResult = "}".getBytes();

    public RelationalResultToJsonDefaultSerializer(RelationalResult relationalResult)
    {
        this.relationalResult = relationalResult;
        this.objectMapper.registerSubtypes(new NamedType(AggregationAwareActivity.class, "aggregationAware"));
        this.objectMapper.registerSubtypes(new NamedType(RelationalExecutionActivity.class, "relational"));
        Iterate.addAllTo(ServiceLoader.load(ResultInterpreterExtension.class), Lists.mutable.empty()).flatCollect(ResultInterpreterExtension::additionalMappers).forEach(e -> this.objectMapper.registerSubtypes(new NamedType(e.getOne(),e.getTwo())));
    }

    @Override
    public void stream(OutputStream stream)
    {
        try
        {
            stream.write(b_builder);
            objectMapper.writeValue(stream, relationalResult.builder);
            if (relationalResult.generationInfo != null)
            {
                stream.write(b_generation);
                ObjectMapperFactory.getNewStandardObjectMapper().writeValue(stream, relationalResult.generationInfo);
            }
            stream.write(b_activities);
            streamCollection(stream, relationalResult.activities);
            stream.write(b_result);
            stream.write(b_sqlColumns);
            streamCollection(stream, relationalResult.getColumnListForSerializer());
            stream.write(b_rows);
            streamRows(stream);
            stream.write(b_end);
            stream.write(b_endResult);
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

    private void streamRows(OutputStream outputStream) throws Exception
    {
        int rowCount = 0;
        try (Scope scope = GlobalTracer.get().buildSpan("Relational Streaming: Fetch first row").startActive(true))
        {
            if (!relationalResult.resultSet.isClosed() && relationalResult.resultSet.next())
            {
                processRow(outputStream);
                rowCount++;
            }
        }
        try (Scope scope = GlobalTracer.get().buildSpan("Relational Streaming: remaining rows").startActive(true))
        {
            while (!relationalResult.resultSet.isClosed() && relationalResult.resultSet.next())
            {
                outputStream.write(b_comma);
                processRow(outputStream);
                rowCount++;
            }
            scope.span().setTag("rowCount", rowCount);
            if (relationalResult.topSpan != null)
            {
                relationalResult.topSpan.setTag("lastQueryRowCount", rowCount);
            }
        }
    }

    private void processRow(OutputStream outputStream) throws IOException, SQLException
    {
        outputStream.write(b_values);

        MutableList<Function<Object, Object>> transformers = relationalResult.getTransformers();

        for (int i = 1; i <= relationalResult.columnCount - 1; i++)
        {
            objectMapper.writeValue(outputStream, transformers.get(i - 1).valueOf(relationalResult.getValue(i)));
            outputStream.write(b_comma);
        }
        objectMapper.writeValue(outputStream, transformers.get(relationalResult.columnCount - 1).valueOf(relationalResult.getValue(relationalResult.columnCount)));
        outputStream.write(b_end);
    }


    private void streamCollection(OutputStream outputStream, List collection) throws IOException
    {
        for (int i = 0; i < collection.size() - 1; i++)
        {
            objectMapper.writeValue(outputStream, collection.get(i));
            outputStream.write(b_comma);
        }
        objectMapper.writeValue(outputStream, collection.get(collection.size() - 1));
        outputStream.flush();
    }
}