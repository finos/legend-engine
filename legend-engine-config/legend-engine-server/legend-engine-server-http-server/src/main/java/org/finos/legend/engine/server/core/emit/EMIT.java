// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.server.core.emit;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.finos.legend.engine.test.emit.EMITModelDiscovery;
import org.finos.legend.engine.test.emit.catalog.EMITModelDescriptor;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Api(tags = "EMIT")
@Path("emit")
public class EMIT
{
    @GET
    @Path("html")
    @ApiOperation(value = "EMIT coverage report in HTML")
    @Produces(MediaType.TEXT_HTML)
    public Response htmlEMIT()
    {
        try
        {
            List<EMITModelDescriptor> descriptors = EMITModelDiscovery.fromClasspath(EMIT.class.getClassLoader());
            String html = EMIT_to_HTML.buildHTML(descriptors);
            return Response.status(200).type(MediaType.TEXT_HTML).entity(html).build();
        }
        catch (Exception e)
        {
            return Response.status(500).entity("Failed to generate EMIT report: " + e.getMessage()).build();
        }
    }
}
