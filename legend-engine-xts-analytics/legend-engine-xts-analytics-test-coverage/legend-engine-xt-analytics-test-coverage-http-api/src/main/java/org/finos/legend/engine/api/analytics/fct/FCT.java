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
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.test.fct.model.FCTTestReport;
import org.finos.legend.engine.test.fct.model.FCTTestResult;
import org.finos.legend.engine.test.fct.model.FeatureTest;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;

@Api(tags = "FCT")
@Path("fct")

public class FCT
{
    private final MutableList<? extends Root_meta_pure_extension_Extension> extensions;

    public FCT(ModelManager modelManager)
    {
        PureModel pureModel = modelManager.loadModel(PureModelContextData.newPureModelContextData(), null, Identity.getAnonymousIdentity(), null);
        this.extensions  = PureCoreExtensionLoader.extensions().flatCollect(e -> e.extraPureCoreExtensions(pureModel.getExecutionSupport()));
    }


    @GET
    @Path("json")
    @ApiOperation(value = "FCT report in JSON")
    @Produces(MediaType.APPLICATION_JSON)
    public Response jsonFCT(@Pac4JProfileManager @ApiParam(hidden = true) ProfileManager<CommonProfile> pm)
    {
        try
        {
            List<FCTTestReport> reports = FCTReportCollector.collectReports(extensions);
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
    @ApiOperation(value = "PCT report in HTML")
    @Produces(MediaType.TEXT_HTML)
    public Response htmlFCT(@Pac4JProfileManager @ApiParam(hidden = true) ProfileManager<CommonProfile> pm)
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
