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

import io.dropwizard.testing.junit.ResourceTestRule;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import java.io.InputStream;

public class LegendTdsTestClient extends LegendHttpClient
{
    private final ResourceTestRule resourceTestRule;

    public LegendTdsTestClient(ResourceTestRule resourceTestRule)
    {
        super(null, null, null);
        this.resourceTestRule = resourceTestRule;
    }


    @Override
    public InputStream executeQueryApi(String query)
    {
        String path = "sql/v1/execution/executeQueryString";
        return executeApi(query, path);
    }

    @Override
    public InputStream executeSchemaApi(String query)
    {
        String path = "sql/v1/execution/getSchemaFromQueryString";
        return executeApi(query, path);
    }

    private InputStream executeApi(String query, String path)
    {
        Invocation.Builder builder = resourceTestRule.target(path).request();
        Response response = builder.post(Entity.text(query));
        return handleResponse(query, () -> (InputStream) response.getEntity(), response::getStatus);
    }
}
