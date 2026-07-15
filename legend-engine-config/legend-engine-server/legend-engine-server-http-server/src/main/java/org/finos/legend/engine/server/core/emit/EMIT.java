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
import org.apache.commons.io.IOUtils;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.InputStream;
import java.util.Objects;

@Api(tags = "EMIT")
@Path("emit")
public class EMIT
{
    private static final String COVERAGE_REPORT_RESOURCE = "emit/emit-coverage.html";

    @GET
    @Path("html")
    @ApiOperation(value = "EMIT coverage report in HTML")
    @Produces(MediaType.TEXT_HTML)
    public Response htmlEMIT()
    {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl.getResource(COVERAGE_REPORT_RESOURCE) == null)
        {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .type(MediaType.TEXT_PLAIN)
                    .entity("EMIT coverage report is not bundled in this server build. Rebuild the legend-engine-server-http-server module to (re)generate it.")
                    .build();
        }
        return Response.status(Response.Status.OK).type(MediaType.TEXT_HTML).entity((StreamingOutput) out ->
        {
            try (InputStream in = Objects.requireNonNull(
                    cl.getResourceAsStream(COVERAGE_REPORT_RESOURCE),
                    COVERAGE_REPORT_RESOURCE))
            {
                IOUtils.copy(in, out);
            }
        }).build();
    }
}
