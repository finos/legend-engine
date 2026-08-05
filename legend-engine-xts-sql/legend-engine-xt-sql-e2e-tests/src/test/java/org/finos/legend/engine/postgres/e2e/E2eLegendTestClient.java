// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.postgres.e2e;

import io.dropwizard.testing.junit.ResourceTestRule;
import org.finos.legend.engine.postgres.protocol.sql.handler.legend.bridge.sql.LegendHttpClient;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import java.io.InputStream;

/**
 * Minimal LegendHttpClient for e2e tests (inlined to avoid test-jar dependency).
 */
public class E2eLegendTestClient extends LegendHttpClient
{
    private final ResourceTestRule resourceTestRule;

    public E2eLegendTestClient(ResourceTestRule resourceTestRule)
    {
        super(null, null, null);
        this.resourceTestRule = resourceTestRule;
    }

    @Override
    public InputStream executeQueryApi(String query)
    {
        return executeApi(query, "sql/v1/execution/executeQueryString");
    }

    @Override
    public InputStream executeSchemaApi(String query)
    {
        return executeApi(query, "sql/v1/execution/getSchemaFromQueryString");
    }

    private InputStream executeApi(String query, String path)
    {
        Invocation.Builder builder = resourceTestRule.target(path).request();
        Response response = builder.post(Entity.text(query));
        int status = response.getStatus();
        if (status == 200)
        {
            return (InputStream) response.getEntity();
        }
        else
        {
            // Read error body and throw
            String errorBody = response.readEntity(String.class);
            throw new RuntimeException("Legend SQL API returned " + status + ": " + errorBody);
        }
    }
}


