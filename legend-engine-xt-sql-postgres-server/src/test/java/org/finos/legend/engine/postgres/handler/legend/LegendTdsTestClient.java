// Copyright 2023 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package org.finos.legend.engine.postgres.handler.legend;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.internal.IterableIterate;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.slf4j.Logger;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;

public class LegendTdsTestClient extends LegendTdsClient
{
    private final ResourceTestRule resourceTestRule;
    private static final ObjectMapper mapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(LegendTdsTestClient.class);


    public LegendTdsTestClient(ResourceTestRule resourceTestRule)
    {
        super(null, null, null);
        this.resourceTestRule = resourceTestRule;
    }


    @Override
    protected JsonNode executeQueryApi(String query)
    {
        Invocation.Builder builder = resourceTestRule.target("sql/v1/execution/executeQueryString").request();
        Response response = builder.post(Entity.text(query));
        String responseString = response.readEntity(String.class);

        if (response.getStatus() != 200)
        {
            LOGGER.error(String.format("Status: [%s], Response: [%s]", response.getStatusInfo(), responseString));
        }
        try
        {
            return mapper.readValue(responseString, JsonNode.class);
        }
        catch (JsonProcessingException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected JsonNode executeSchemaApi(String query)
    {
        Invocation.Builder builder = resourceTestRule.target("sql/v1/execution/getSchemaFromQueryString").request();
        Response response = builder.post(Entity.text(query));
        String responseString = response.readEntity(String.class);

        if (response.getStatus() != 200)
        {
            LOGGER.error(String.format("Status: [%s], Response: [%s]", response.getStatusInfo(), responseString));
        }
        try
        {
            return mapper.readValue(responseString, JsonNode.class);
        }
        catch (JsonProcessingException e)
        {
            throw new RuntimeException(e);
        }
    }
}
