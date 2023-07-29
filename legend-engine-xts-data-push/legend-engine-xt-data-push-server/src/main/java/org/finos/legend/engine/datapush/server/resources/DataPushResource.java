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
import org.finos.legend.engine.server.support.server.resources.BaseResource;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

@Path("/data/push")
@Api("Data Push")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class DataPushResource extends BaseResource
{
    public DataPushResource()
    {

    }

    @Path("/location/{location}/datastore/{datastore}/dataset/{dataset}")
    @POST
    @ApiOperation("Push data")
    public Response push(@PathParam("location") String location, @PathParam("datastore") String datastore, @PathParam("dataset") String dataset) throws IOException
    {
        return executeWithLogging(
                "pushing data\"",
                () -> Response.ok().entity(this.pushData( location, datastore, dataset)).build()
        );
    }

    private String pushData(String location, String datastore, String dataset)
    {
        try
        {
            // TODO - actually push the data
            return "ok";
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
