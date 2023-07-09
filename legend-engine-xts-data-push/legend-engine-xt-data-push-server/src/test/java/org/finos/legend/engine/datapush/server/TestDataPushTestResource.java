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

package org.finos.legend.engine.datapush.server;

import org.apache.http.client.HttpResponseException;
import org.finos.legend.engine.datapush.server.test.AbstractDataPushServerResourceTest;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;

public class TestDataPushTestResource extends AbstractDataPushServerResourceTest
{
    @Test
    public void testGet() throws HttpResponseException
    {
        Response response = this.clientFor("/api/tests/data/push/getSomething").request().get();

        String responseText = response.readEntity(String.class);

        if (response.getStatus() != 200)
        {
            throw new HttpResponseException(response.getStatus(), "Error during http call with status: " + response.getStatus() + " , entity: " + responseText);
        }

        assertEquals("{\"get\" : \"ok\"}", responseText);
    }

    @Test
    public void testPost() throws HttpResponseException
    {
        Response response = this.clientFor("/api/tests/data/push/postSomething").request().post(Entity.entity("{}", MediaType.APPLICATION_JSON_TYPE));

        String responseText = response.readEntity(String.class);

        if (response.getStatus() != 200)
        {
            throw new HttpResponseException(response.getStatus(), "Error during http call with status: " + response.getStatus() + " , entity: " + responseText);
        }

        assertEquals("{\"post\" : \"ok\"}", responseText);
    }
}
