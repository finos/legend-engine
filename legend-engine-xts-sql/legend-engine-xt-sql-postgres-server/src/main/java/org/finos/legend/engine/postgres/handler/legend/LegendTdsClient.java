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
import java.io.IOException;
import java.io.InputStream;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.eclipse.collections.impl.utility.internal.IterableIterate;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.kerberos.HttpClientBuilder;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        try (InputStream inputStream = executeSchemaApi(query);)
        {
            JsonNode jsonNode = mapper.readTree(inputStream);
            if (jsonNode.get("columns") != null)
            {
                ArrayNode columns = (ArrayNode) jsonNode.get("columns");
                return IterableIterate.collect(columns, c -> new LegendColumn(c.get("name").textValue(), c.get("type").textValue()));
            }
            return Collections.emptyList();
        }
        catch (IOException e)
        {
            throw new LegendTdsClientException("Failed to parse result", e);
        }

    }

    @Override
    public LegendExecutionResult executeQuery(String query)
    {
        try
        {
            InputStream inputStream = this.executeQueryApi(query);
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
    }

    protected InputStream executeQueryApi(String query)
    {
        LOGGER.info("executing query " + query);
        String apiPath = "/api/sql/v1/execution/executeQueryString";
        return executeApi(query, apiPath);
    }


    protected InputStream executeSchemaApi(String query)
    {
        LOGGER.info("executing schema query " + query);
        String apiPath = "/api/sql/v1/execution/getSchemaFromQueryString";
        return executeApi(query, apiPath);
    }

    private InputStream executeApi(String query, String apiPath)
    {
        String uri = protocol + "://" + this.host + ":" + this.port + apiPath;
        HttpPost req = new HttpPost(uri);

        StringEntity stringEntity = new StringEntity(query, UTF_8);
        stringEntity.setContentType(TEXT_PLAIN);
        req.setEntity(stringEntity);

        try
        {
            HttpClient client =  HttpClientBuilder.getHttpClient(new BasicCookieStore());
            HttpResponse res = client.execute(req);
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


    protected static InputStream handleResponse(String query, Callable<InputStream> responseContentSupplier, Supplier<Integer> responseStatusCodeSupplier)
    {
        String errorResponse = null;
        try
        {
            InputStream in = responseContentSupplier.call();
            if (responseStatusCodeSupplier.get() == 200)
            {
                return in;
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
