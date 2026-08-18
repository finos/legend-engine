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

package org.finos.legend.engine.server.core.sql;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.io.IOUtils;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Api(tags = "SQL Documentation")
@Path("sql-docs")
public class SQLDocumentation
{
    private static final String CLASSPATH_PREFIX = "sql-parity/";

    @GET
    @Path("summary")
    @ApiOperation(value = "SQL coverage summary report")
    @Produces(MediaType.TEXT_HTML)
    public Response summary()
    {
        return serveHtml("summary.html");
    }

    @GET
    @Path("function-coverage")
    @ApiOperation(value = "SQL function coverage report")
    @Produces(MediaType.TEXT_HTML)
    public Response functionCoverage()
    {
        return serveHtml("function-coverage.html");
    }

    @GET
    @Path("structural-parity")
    @ApiOperation(value = "SQL structural parity report")
    @Produces(MediaType.TEXT_HTML)
    public Response structuralParity()
    {
        return serveHtml("structural-parity.html");
    }

    @GET
    @Path("failure-details")
    @ApiOperation(value = "SQL failure details report")
    @Produces(MediaType.TEXT_HTML)
    public Response failureDetails()
    {
        return serveHtml("failure-details.html");
    }

    private Response serveHtml(String fileName)
    {
        return serveFile(fileName, MediaType.TEXT_HTML);
    }

    private Response serveFile(String fileName, String mediaType)
    {
        String content = loadResource(fileName);
        if (content == null)
        {
            return Response.status(Response.Status.NOT_FOUND)
                    .type(MediaType.TEXT_HTML)
                    .entity("<html><body><h1>Report not available</h1>"
                            + "<p>The report <code>" + fileName + "</code> has not been generated yet.</p>"
                            + "<p>Run the SQL E2E parity tests to generate it: "
                            + "<code>mvn verify -pl legend-engine-xts-sql/legend-engine-xt-sql-e2e-tests</code></p>"
                            + "</body></html>")
                    .build();
        }
        return Response.ok(content, mediaType).build();
    }

    private String loadResource(String fileName)
    {
        InputStream is = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(CLASSPATH_PREFIX + fileName);
        if (is == null)
        {
            return null;
        }
        try (InputStream stream = is)
        {
            return IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        catch (IOException e)
        {
            return null;
        }
    }
}
