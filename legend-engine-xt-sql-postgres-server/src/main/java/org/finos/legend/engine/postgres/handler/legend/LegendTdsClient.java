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
import java.util.Collections;
import java.util.List;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.internal.IterableIterate;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.kerberos.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LegendTdsClient implements LegendExecutionClient
{
    private final String protocol;
    private final String host;
    private final String port;
    private final CookieStore cookieStore;
    private static final ObjectMapper mapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    private static final Logger LOGGER = LoggerFactory.getLogger(LegendTdsClient.class);


    public LegendTdsClient(String protocol, String host, String port, CookieStore cookieStore)
    {

        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.cookieStore = cookieStore;
    }

    @Override
    public List<LegendColumn> getSchema(String query)
    {
        try
        {
            JsonNode jsonNode = this.executeQueryApi(query);
            return getSchemaFromExecutionResponse(jsonNode);
        }
        catch (JsonProcessingException e)
        {
            throw new RuntimeException(e);
        }
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

    private JsonNode executeQueryApi(String query)
    {
        LOGGER.info("executing query " + query);
        try (CloseableHttpClient client = (CloseableHttpClient) HttpClientBuilder.getHttpClient(cookieStore))
        {
            String uri = protocol + "://" + this.host + ":" + this.port + "/api/sql/v1/execution/executeQueryString";
            HttpPost req = new HttpPost(uri);

            StringEntity stringEntity = new StringEntity(query);
            stringEntity.setContentType("text/plain");
            req.setEntity(stringEntity);

            try (CloseableHttpResponse res = client.execute(req))
            {
                JsonNode response = mapper.readValue(res.getEntity().getContent(), JsonNode.class);
                if (res.getStatusLine().getStatusCode() != 200)
                {
                    String message = "Failed to execute query " + query + "\n Cause: " + response.toPrettyString();
                    LOGGER.info(message);
                }
                return response;
            }
        }
        catch (Exception e)
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
            return LazyIterate.collect(result, a -> columIndex -> a.get("values").get(columIndex).asText());
        }
        return Collections.emptyList();
    }
}
