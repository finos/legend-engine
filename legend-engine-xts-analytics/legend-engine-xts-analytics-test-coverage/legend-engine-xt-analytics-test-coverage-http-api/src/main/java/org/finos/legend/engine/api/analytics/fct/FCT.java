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


package org.finos.legend.engine.api.analytics.fct;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.finos.legend.engine.api.analytics.mft.MFTReportCollector;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.test.mft.model.MFTTestReport;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;

@Tag(name = "FCT")
@Path("fct")

public class FCT
{
    private  ModelManager modelManager;

    public FCT(ModelManager modelManager)
    {
        this.modelManager = modelManager;
    }

    public FCT()
    {
    }

    @GET
    @Path("json")
    @Operation(summary = "FCT report in JSON")
    @Produces(MediaType.APPLICATION_JSON)
    public Response jsonFCT(@Pac4JProfileManager @Parameter(hidden = true) ProfileManager pm)
    {
        try
        {
            List<MFTTestReport> reports = MFTReportCollector.collectReports();
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            return Response.status(200).type(MediaType.APPLICATION_JSON).entity(mapper.writeValueAsString(reports)).build();
        }
        catch (JsonProcessingException e)
        {
            throw new RuntimeException(e);
        }
    }

    @GET
    @Path("html")
    @Operation(summary = "PCT report in HTML")
    @Produces(MediaType.TEXT_HTML)
    public Response htmlFCT(@Pac4JProfileManager @Parameter(hidden = true) ProfileManager pm)
    {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("FCTPage.html");
        if (inputStream == null)
        {
            return Response.status(404).entity("File not found").build();
        }
        String htmlContent;
        try (Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name()))
        {
            htmlContent = scanner.useDelimiter("\\A").next();
        }
        return Response.status(200).type(MediaType.TEXT_HTML).entity(htmlContent).build();
    }


}
