//  Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.state.ConnectionStateManager;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


@Api(tags = "Server")
@Path("server/v1")
@Produces(MediaType.APPLICATION_JSON)
public class RelationalExecutorInformation
{
    private final ConnectionStateManager connectionStateManager = ConnectionStateManager.getInstance();

    public RelationalExecutorInformation()
    {
    }

    @GET
    @Path("executorInfo")
    @ApiOperation(value = "Provides information about executors (like connections pools, etc.)")
    public Response executePureGet()
    {
        return Response.status(200).type(MediaType.APPLICATION_JSON).entity(connectionStateManager.getConnectionStateManagerPOJO()).build();
    }

    @GET
    @Path("executorInfo/relational/pools/{poolName}")
    @ApiOperation(value = "Provides database pool information ")
    public Response getPoolInformation(@PathParam("poolName") String poolName)
    {
        return Response.status(200).type(MediaType.APPLICATION_JSON).entity(connectionStateManager.findByPoolName(poolName)).build();
    }

    @DELETE
    @Path("executorInfo/relational/pools/{poolName}")
    @ApiOperation(value = "delete and evict connections in this database pool ")
    public Response delete(@PathParam("poolName") String poolName)
    {
        return Response.status(200).type(MediaType.APPLICATION_JSON).entity(connectionStateManager.closeAndRemoveConnectionPool(poolName)).build();
    }

    @GET
    @Path("executorInfo/relational/{user}")
    @ApiOperation(value = "Provides pool information by user ")
    public Response getUserInformation(@PathParam("user") String user)
    {
        return Response.status(200).type(MediaType.APPLICATION_JSON).entity(connectionStateManager.getPoolInformationByUser(user)).build();
    }

}
