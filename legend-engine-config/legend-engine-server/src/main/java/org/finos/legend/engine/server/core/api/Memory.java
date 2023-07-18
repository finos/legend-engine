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

package org.finos.legend.engine.server.core.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

@Api(tags = "Server")
@Path("server/v1/memory")
@Produces(MediaType.APPLICATION_JSON)
public class Memory
{
    @GET
    @Path("gc")
    @ApiOperation(value = "Performs GC")
    public Response executeGC()
    {
        Runtime.getRuntime().gc();
        return Response.status(200).type(MediaType.APPLICATION_JSON).entity("{}").build();
    }

    @GET
    @Path("info")
    @ApiOperation(value = "Provides the server JVM memory information")
    public Response executeMemoryInfo()
    {
        try
        {
            Map<String, Long> memory = UnifiedMap.newMap();
            memory.put("total", Runtime.getRuntime().totalMemory());
            memory.put("max", Runtime.getRuntime().maxMemory());
            memory.put("free", Runtime.getRuntime().freeMemory());
            return Response.status(200).type(MediaType.APPLICATION_JSON).entity(new ObjectMapper().writeValueAsString(memory)).build();
        }
        catch (JsonProcessingException e)
        {
            throw new RuntimeException(e);
        }
    }
}
