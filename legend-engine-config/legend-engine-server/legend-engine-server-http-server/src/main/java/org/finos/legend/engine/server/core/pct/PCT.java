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

package org.finos.legend.engine.server.core.pct;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.Set;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.finos.legend.pure.m3.pct.aggregate.generation.DocumentationGeneration;
import org.finos.legend.pure.m3.pct.aggregate.model.Documentation;

@Api(tags = "PCT")
@Path("pct")
public class PCT
{
    @GET
    @Path("html")
    @ApiOperation(value = "PCT report in HTML")
    @Produces(MediaType.TEXT_HTML)
    public Response htmlPCT(@QueryParam("adapter") Set<String> adapterKeys)
    {
        return Response.status(200).type(MediaType.TEXT_HTML).entity(PCT_to_SimpleHTML.buildHTML(adapterKeys)).build();
    }

    @GET
    @Path("json")
    @ApiOperation(value = "PCT report in JSON")
    @Produces(MediaType.APPLICATION_JSON)
    public Response jsonPCT()
    {
        try
        {
            Documentation doc = DocumentationGeneration.buildDocumentation();
            return Response.status(200).type(MediaType.APPLICATION_JSON).entity(new ObjectMapper().writeValueAsString(doc)).build();
        }
        catch (JsonProcessingException e)
        {
            throw new RuntimeException(e);
        }
    }
}
