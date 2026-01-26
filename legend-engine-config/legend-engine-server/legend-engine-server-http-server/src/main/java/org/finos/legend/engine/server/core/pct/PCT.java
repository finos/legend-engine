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
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.io.IOUtils;
import org.finos.legend.engine.shared.core.deployment.DeploymentStateAndVersions;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.Set;

@Api(tags = "PCT")
@Path("pct")
public class PCT
{
    @GET
    @Path("form")
    @ApiOperation(value = "PCT report form")
    @Produces(MediaType.TEXT_HTML)
    public Response formPCT()
    {
        return Response.status(200).type(MediaType.TEXT_HTML).entity(new StreamingOutput()
        {
            @Override
            public void write(OutputStream outputStream) throws IOException
            {
                try (InputStream is = Objects.requireNonNull(ClassLoader.getSystemClassLoader().getResourceAsStream("pct/PCT_Report_Compatibility.html")))
                {
                    IOUtils.copy(is, outputStream);
                }
            }
        }).build();
    }

    @GET
    @Path("git-info.json")
    @ApiOperation(value = "Git info for PCT report")
    @Produces(MediaType.APPLICATION_JSON)
    public Response gitInfoPct()
    {
        return Response.status(200).type(MediaType.APPLICATION_JSON).entity(DeploymentStateAndVersions.sdlcJSON).build();
    }

    @GET
    @Path("html")
    @ApiOperation(value = "PCT report in HTML")
    @Produces(MediaType.TEXT_HTML)
    public Response htmlPCT(@QueryParam("adapter") Set<String> adapterKeys, @QueryParam("qualifier") Set<String> adapterQualifiers, @QueryParam("skipFunctionsWithoutTest") @DefaultValue("true") boolean skipFunctionsWithoutTest)
    {
        return Response.status(200).type(MediaType.TEXT_HTML).entity(PCT_to_SimpleHTML.buildHTML(adapterKeys, adapterQualifiers, skipFunctionsWithoutTest)).build();
    }

    @GET
    @Path("pct-docs.json")
    @ApiOperation(value = "PCT report in JSON")
    @Produces(MediaType.APPLICATION_JSON)
    public Response jsonPCT() throws JsonProcessingException
    {
        return Response.status(200).type(MediaType.APPLICATION_JSON).entity(GeneratePCTFiles.getDocumentationAsJson()).build();
    }
}
