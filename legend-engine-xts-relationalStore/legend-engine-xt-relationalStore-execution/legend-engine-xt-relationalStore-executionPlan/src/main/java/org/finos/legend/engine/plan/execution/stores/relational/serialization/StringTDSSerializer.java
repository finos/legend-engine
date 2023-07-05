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

import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.result.builder.tds.TDSBuilder;
import org.finos.legend.engine.plan.execution.result.serialization.Serializer;
import org.finos.legend.engine.plan.execution.stores.relational.result.RelationalResult;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// This class creates a String equivalent to TdsToString
// The output is similar to CSV, except guaranteed compatibility with test assert generation
public class StringTDSSerializer extends Serializer
{
    private static final byte[] b_comma = ",".getBytes();
    private static final byte[] b_eol = "\n".getBytes();
    private final RelationalResult relationalResult;

    public StringTDSSerializer(RelationalResult relationalResult)
    {
        this.relationalResult = relationalResult;
    }

    @Override
    public void stream(OutputStream targetStream) throws IOException
    {
        streamCollection(targetStream, ((TDSBuilder) relationalResult.builder).columns.stream().map(col -> col.name).collect(Collectors.toList()));
        try
        {
            MutableList<Function<Object, Object>> transformers = relationalResult.getTransformers();

            int rowCount = 0;
            try (Scope ignored = GlobalTracer.get().buildSpan("Relational Streaming: Fetch first row").startActive(true))
            {
                if (relationalResult.resultSet.next())
                {
                    streamCollection(targetStream, IntStream.range(1, relationalResult.columnCount + 1).mapToObj(i ->
                    {
                        try
                        {
                            return transformers.get(i - 1).valueOf(relationalResult.resultSet.getObject(i));
                        }
                        catch (SQLException e)
                        {
                            throw new RuntimeException(e);
                        }
                    }).collect(Collectors.toList()));
                }
            }
            try (Scope scope = GlobalTracer.get().buildSpan("Relational Streaming: remaining rows").startActive(true))
            {
                while (relationalResult.resultSet.next())
                {
                    streamCollection(targetStream, IntStream.range(1, relationalResult.columnCount + 1).mapToObj(i ->
                    {
                        try
                        {
                            return transformers.get(i - 1).valueOf(relationalResult.resultSet.getObject(i));
                        }
                        catch (SQLException e)
                        {
                            throw new RuntimeException(e);
                        }
                    }).collect(Collectors.toList()));
                }
                scope.span().setTag("rowCount", rowCount);
                if (relationalResult.topSpan != null)
                {
                    relationalResult.topSpan.setTag("lastQueryRowCount", rowCount);
                }
            }
        }
        catch (SQLException ignored)
        {
        }
    }

    private void streamCollection(OutputStream outputStream, List collection) throws IOException
    {
        for (int i = 0; i < collection.size() - 1; i++)
        {
            streamObject(outputStream, collection.get(i));
            outputStream.write(b_comma);
        }
        streamObject(outputStream, collection.get(collection.size() - 1));

        outputStream.write(b_eol);
        outputStream.flush();
    }

    private void streamObject(OutputStream outputStream, Object object) throws IOException
    {
        if (object == null)
        {
            return;
        }
        outputStream.write(object.toString().getBytes());
    }
}
