// Copyright 2023 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.postgres.handler.legend;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.internal.IterableIterate;
import org.finos.legend.engine.postgres.utils.OpenTelemetry;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LegendExecutionService
{
    private final LegendClient executionClient;

    private static final ObjectMapper mapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    private static final Logger LOGGER = LoggerFactory.getLogger(LegendExecutionService.class);

    public LegendExecutionService(LegendClient executionClient)
    {
        this.executionClient = executionClient;
    }

    public List<LegendColumn> getSchema(String query)
    {
        Tracer tracer = OpenTelemetry.getTracer();
        Span span = tracer.spanBuilder("Legend ExecutionService Get Schema").startSpan();
        try (Scope scope = span.makeCurrent(); InputStream inputStream = executionClient.executeSchemaApi(query);)
        {
            span.setAttribute("query", query);
            JsonNode jsonNode = mapper.readTree(inputStream);
            if (jsonNode.get("columns") != null)
            {
                ArrayNode columns = (ArrayNode) jsonNode.get("columns");
                List<LegendColumn> legendColumns = Collections.unmodifiableList(IterableIterate.collect(columns, c -> new LegendColumn(c.get("name").textValue(), c.get("type").textValue())));
                span.setAttribute(AttributeKey.stringArrayKey("columns"), legendColumns.stream().map(legendColumn -> legendColumn.toString()).collect(Collectors.toList()));
                return legendColumns;
            }
            return Collections.emptyList();
        }
        catch (IOException e)
        {
            throw new LegendTdsClientException("Failed to parse result", e);
        }
        finally
        {
            span.end();
        }

    }

    public LegendExecutionResult executeQuery(String query)
    {
        Tracer tracer = OpenTelemetry.getTracer();
        Span span = tracer.spanBuilder("LegendExecutionService ExecuteQuery").startSpan();
        try (Scope scope = span.makeCurrent();)
        {
            span.setAttribute("query", query);
            InputStream inputStream = executionClient.executeQueryApi(query);
            span.addEvent("receivedResponse");
            LegendTdsResultParser parser = new LegendTdsResultParser(inputStream);

            return new LegendExecutionResult()
            {
                @Override
                public List<LegendColumn> getLegendColumns()
                {
                    return parser.getLegendColumns();
                }

                @Override
                public void close()
                {
                    try
                    {
                        parser.close();
                    }
                    catch (IOException e)
                    {
                        throw new LegendTdsClientException("Error while closing parser", e);
                    }
                }

                @Override
                public boolean hasNext()
                {

                    try
                    {
                        return parser.hasNext();
                    }
                    catch (IOException e)
                    {
                        throw new LegendTdsClientException("Error while retrieving a row", e);
                    }
                }

                @Override
                public List<Object> next()
                {
                    return parser.next();
                }


            };


        }
        catch (IOException e)
        {
            throw new LegendTdsClientException("Error while parsing response", e);
        }
        finally
        {
            span.end();
        }
    }


    private List<LegendColumn> getSchemaFromExecutionResponse(JsonNode jsonNode) throws JsonProcessingException
    {
        if (jsonNode.get("builder") != null)
        {
            ArrayNode columns = (ArrayNode) jsonNode.get("builder").get("columns");
            return IterableIterate.collect(columns, c -> new LegendColumn(c.get("name").asText(), c.get("type").asText()));
        }
        return Collections.emptyList();
    }


}
