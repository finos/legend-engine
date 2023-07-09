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

package org.finos.legend.engine.datapush.server.resources;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/tests/data/push")
@Api("Data Push - tests")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class DataPushTestResource extends BaseResource
{
    public DataPushTestResource()
    {

    }

    @Path("/postSomething")
    @POST
    @ApiOperation("Test POST")
    public Response postSomething(Object object)
    {
        return executeWithLogging(
                "testing post \"",
                () -> Response.ok().entity("{\"post\" : \"ok\"}").build()
        );
    }

    @Path("/getSomething")
    @GET
    @ApiOperation("Test GET")
    public Response getSomething()
    {
        return executeWithLogging(
                "testing post \"",
                () -> Response.ok().entity("{\"get\" : \"ok\"}").build()
        );
    }
}
