//  Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.api.concurrent;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.finos.legend.engine.plan.execution.concurrent.ConcurrentExecutionNodeExecutorPool;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Tag(name = "Server")
@Path("server/v1")
@Produces(MediaType.APPLICATION_JSON)
public class ConcurrentExecutionNodeExecutorPoolInfo
{
    private final List<ConcurrentExecutionNodeExecutorPool> concurrentExecutionNodeExecutorPools;

    public ConcurrentExecutionNodeExecutorPoolInfo(List<ConcurrentExecutionNodeExecutorPool> concurrentExecutionNodeExecutorPools)
    {
        this.concurrentExecutionNodeExecutorPools = concurrentExecutionNodeExecutorPools;
    }

    @GET
    @Path("concurrentExecutionNodeExecutorPoolInfo")
    @Operation(summary = "Provides information about ConcurrentExecutionNodeExecutorPool")
    public Response executePureGet()
    {
        return Response.status(200).type(MediaType.APPLICATION_JSON).entity(concurrentExecutionNodeExecutorPools).build();
    }
}
