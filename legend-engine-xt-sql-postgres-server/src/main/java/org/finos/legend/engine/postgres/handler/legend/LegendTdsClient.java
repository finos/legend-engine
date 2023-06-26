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
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.internal.IterableIterate;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.kerberos.HttpClientBuilder;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

public class LegendTdsClient implements LegendExecutionClient
{
    private final String protocol;
    private final String host;
    private final String port;
    private static final ObjectMapper mapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    private static final Logger LOGGER = LoggerFactory.getLogger(LegendTdsClient.class);

    public LegendTdsClient(String protocol, String host, String port)
    {

        this.protocol = protocol;
        this.host = host;
        this.port = port;
    }

    @Override
    public List<LegendColumn> getSchema(String query)
    {
        JsonNode jsonNode = this.executeSchemaApi(query);
        if (jsonNode.get("columns") != null)
        {
            ArrayNode columns = (ArrayNode) jsonNode.get("columns");
            return IterableIterate.collect(columns, c -> new LegendColumn(c.get("name").textValue(), c.get("type").textValue()));
        }
        return Collections.emptyList();
    }

    @Override
    public Iterable<TDSRow> executeQuery(String query)
    {
        JsonNode jsonNode = this.executeQueryApi(query);
        return getRowsFromExecutionResponse(jsonNode);
    }

    @Override
    public Pair<List<LegendColumn>, Iterable<TDSRow>> getSchemaAndExecuteQuery(String query)
    {
        try
        {
            JsonNode jsonNode = this.executeQueryApi(query);
            List<LegendColumn> schema = getSchemaFromExecutionResponse(jsonNode);
            Iterable<TDSRow> rows = getRowsFromExecutionResponse(jsonNode);
            return Tuples.pair(schema, rows);

        }
        catch (JsonProcessingException e)
        {
            throw new RuntimeException(e);
        }
    }

    protected JsonNode executeQueryApi(String query)
    {
        LOGGER.info("executing query " + query);
        String apiPath = "/api/sql/v1/execution/executeQueryString";
        return executeApi(query, apiPath);
    }


    protected JsonNode executeSchemaApi(String query)
    {
        LOGGER.info("executing schema query " + query);
        String apiPath = "/api/sql/v1/execution/getSchemaFromQueryString";
        return executeApi(query, apiPath);
    }

    private JsonNode executeApi(String query, String apiPath)
    {
        String uri = protocol + "://" + this.host + ":" + this.port + apiPath;
        HttpPost req = new HttpPost(uri);

        StringEntity stringEntity = new StringEntity(query, UTF_8);
        stringEntity.setContentType(TEXT_PLAIN);
        req.setEntity(stringEntity);

        try (CloseableHttpClient client = (CloseableHttpClient) HttpClientBuilder.getHttpClient(new BasicCookieStore());
             CloseableHttpResponse res = client.execute(req))
        {
            return handleResponse(query, () -> res.getEntity().getContent(), () -> res.getStatusLine().getStatusCode());
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
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


    private static Iterable<TDSRow> getRowsFromExecutionResponse(JsonNode jsonNode)
    {
        if (jsonNode.get("result") != null)
        {
            ArrayNode result = (ArrayNode) jsonNode.get("result").get("rows");
            return LazyIterate.collect(result, a -> columIndex ->
            {
                JsonNode node = a.get("values").get(columIndex);
                if (node.isNull())
                {
                    return null;
                }
                if (node.isInt())
                {
                    return node.intValue();
                }
                if (node.isFloat())
                {
                    return node.floatValue();
                }
                if (node.isDouble())
                {
                    return node.doubleValue();
                }
                if (node.isNumber())
                {
                    return node.doubleValue();
                }
                if (node.isBoolean())
                {
                    return node.booleanValue();
                }
                return node.asText();
            });
        }
        return Collections.emptyList();
    }

    protected static JsonNode handleResponse(String query, Callable<InputStream> responseContentSupplier, Supplier<Integer> responseStatusCodeSupplier)
    {
        String errorResponse = null;
        try
        {
            InputStream in = responseContentSupplier.call();
            if (responseStatusCodeSupplier.get() == 200)
            {
                return mapper.readTree(in);
            }
            else
            {
                errorResponse = IOUtils.toString(in, UTF_8);
                ExceptionError exceptionError = mapper.readValue(errorResponse, ExceptionError.class);
                String errorMessage;
                if ("error".equalsIgnoreCase(exceptionError.status))
                {
                    errorMessage = exceptionError.getMessage();
                    LOGGER.error("Failed to execute query: [{}], trace: [{}]", query, exceptionError.getTrace());
                }
                else
                {
                    errorMessage = String.format("Status: [%s], Response: [%s]", in, exceptionError);
                }
                throw new LegendTdsClientException(errorMessage);
            }
        }
        catch (Exception e)
        {
            if (e instanceof LegendTdsClientException)
            {
                throw (LegendTdsClientException) e;
            }
            else
            {
                String message = String.format("Unable to parse json. Execution API response status[%s]", responseStatusCodeSupplier.get());
                if (responseStatusCodeSupplier.get() != 200)
                {
                    message = String.format("%s, response: [%s]", message, errorResponse);
                }
                throw new LegendTdsClientException(message);
            }

        }
    }


}
