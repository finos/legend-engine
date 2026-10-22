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

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.state.ConnectionStateManager;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Tag(name = "Server")
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
    @Operation(summary = "Provides information about executors (like connections pools, etc.)")
    public Response executePureGet()
    {
        return Response.status(200).type(MediaType.APPLICATION_JSON).entity(connectionStateManager.getConnectionStateManagerPOJO()).build();
    }

    @GET
    @Path("executorInfo/relational/pools/{poolName}")
    @Operation(summary = "Provides database pool information ")
    public Response getPoolInformation(@PathParam("poolName") String poolName)
    {
        return Response.status(200).type(MediaType.APPLICATION_JSON).entity(connectionStateManager.findByPoolName(poolName)).build();
    }

    @DELETE
    @Path("executorInfo/relational/pools/{poolName}")
    @Operation(summary = "delete and evict connections in this database pool ")
    public Response delete(@PathParam("poolName") String poolName)
    {
        return Response.status(200).type(MediaType.APPLICATION_JSON).entity(connectionStateManager.closeAndRemoveConnectionPool(poolName)).build();
    }

    @GET
    @Path("executorInfo/relational/{user}")
    @Operation(summary = "Provides pool information by user ")
    public Response getUserInformation(@PathParam("user") String user)
    {
        return Response.status(200).type(MediaType.APPLICATION_JSON).entity(connectionStateManager.getPoolInformationByUser(user)).build();
    }

}
