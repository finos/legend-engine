// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.result.object;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.plan.execution.result.builder._class.ClassBuilder;
import org.finos.legend.engine.plan.execution.result.serialization.CsvSerializer;
import org.finos.legend.engine.plan.execution.result.serialization.ExecutionResultObjectMapperFactory;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.stream.Collectors;

public class StreamingObjectResultCSVSerializer extends CsvSerializer
{
    private final StreamingObjectResult streamingObjectResult;
    private final boolean withHeader;

    public StreamingObjectResultCSVSerializer(StreamingObjectResult streamingObjectResult, boolean withHeader)
    {
        this.streamingObjectResult = streamingObjectResult;
        this.withHeader = withHeader;
    }

    @Override
    public void stream(OutputStream targetStream) throws IOException
    {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Writer out = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
        final CSVPrinter csvPrinter = new CSVPrinter(out, withHeader ? CSVFormat.DEFAULT.withFirstRecordAsHeader() : CSVFormat.DEFAULT);
        final ObjectMapper objectMapper = ExecutionResultObjectMapperFactory.getNewObjectMapper();
        try
        {
            final List<String> columns = this.getHeaderColumnsAndTypes().stream().map(Pair::getOne).collect(Collectors.toList());

            if (this.withHeader)
            {
                csvPrinter.printRecord(columns);
            }
            this.streamingObjectResult.getObjectStream().forEach(obj ->
            {
                try
                {
                    List<String> valList = Lists.mutable.empty();
                    for (String key : columns)
                    {
                        Object value;
                        try
                        {
                            value = obj.getClass().getMethod("get_" + key).invoke(obj);
                        }
                        catch (NoSuchMethodException e)
                        {
                            value = obj.getClass().getMethod("get" + key.substring(0, 1).toUpperCase() + key.substring(1)).invoke(obj);
                        }
                        if (value == null)
                        {
                            valList.add("");
                        }
                        else
                        {
                            valList.add(objectMapper.writeValueAsString(value));
                        }
                    }
                    csvPrinter.printRecord(valList);
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            });
            csvPrinter.close();
            byteArrayOutputStream.writeTo(targetStream);
        }
        catch (Exception e)
        {
            throw new RuntimeException("error creating CSV", e);
        }
        finally
        {
            try
            {
                if (csvPrinter != null)
                {
                    csvPrinter.close();
                }
                if (this.streamingObjectResult != null)
                {
                    this.streamingObjectResult.close();
                }
            }
            catch (Exception e)
            {
                throw new RuntimeException("error creating CSV", e);
            }
            try
            {
                byteArrayOutputStream.close();
            }
            catch (Exception e)
            {
                throw new RuntimeException("error creating CSV", e);
            }
        }
    }

    @Override
    public List<Pair<String, String>> getHeaderColumnsAndTypes()
    {
        ClassBuilder classBuilder = (ClassBuilder) this.streamingObjectResult.getResultBuilder();
        return classBuilder.propertyTypes();
    }
}
