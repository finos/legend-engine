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

package org.finos.legend.engine.external.format.awspersistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.external.format.awspersistence.model.AwsPersistenceGenerationConfig;
import org.finos.legend.engine.external.format.awspersistence.model.AwsPersistenceGenerationInput;
import org.finos.legend.engine.external.format.awspersistence.model.AwsPersistenceTriggerSpecification;
import org.finos.legend.engine.external.shared.format.generations.GenerationOutput;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.api.result.ManageConstantResult;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.finos.legend.pure.generated.Root_meta_pure_generation_metamodel_GenerationOutput;
import org.finos.legend.pure.generated.core_persistence_external_format_awspersistence_transformation;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;
import org.slf4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.*;
import java.io.*;

import static org.finos.legend.engine.shared.core.operational.http.InflateInterceptor.APPLICATION_ZLIB;

@Api(tags = "External - Generation - Code")
@Path("pure/v1/codeGeneration")
@Produces(MediaType.APPLICATION_JSON)
public class AwsPersistenceGenerationService
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapper();
    private final ModelManager modelManager;
    private static final String awsApiGatewayUrl = Vault.INSTANCE.getValue("apigateway_url");
    private static final String awsApiGatewayKey = Vault.INSTANCE.getValue("apigateway_key");

    public AwsPersistenceGenerationService(ModelManager modelManager)
    {
        this.modelManager = modelManager;
    }

    @POST
    @Path("awsPersistence")
    @ApiOperation(value = "Generates AwsPersistence")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    public Response generateAwsPersistence(AwsPersistenceGenerationInput generateAwsPersistenceInput, @ApiParam(hidden = true)  @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        boolean interactive = generateAwsPersistenceInput.model instanceof PureModelContextData;
        try (Scope scope = GlobalTracer.get().buildSpan("Service: Generate AwsPersistence").startActive(true))
        {
            return exec(
                    generateAwsPersistenceInput.config != null ? generateAwsPersistenceInput.config : new AwsPersistenceGenerationConfig(),
                    () -> this.modelManager.loadModelAndData(generateAwsPersistenceInput.model, generateAwsPersistenceInput.clientVersion, profiles, null).getTwo(),
                    interactive,
                    profiles);
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, interactive ? LoggingEventType.GENERATE_AWSPERSISTENCE_CODE_INTERACTIVE_ERROR : LoggingEventType.GENERATE_AWSPERSISTENCE_CODE_ERROR, profiles);
        }
    }

    private Response exec(AwsPersistenceGenerationConfig awspersistenceConfig, Function0<PureModel> pureModelFunc, boolean interactive, MutableList<CommonProfile> pm)
    {
        try
        {
            long start = System.currentTimeMillis();
            LOGGER.info(new LogInfo(pm, interactive ? LoggingEventType.GENERATE_AWSPERSISTENCE_CODE_INTERACTIVE_START : LoggingEventType.GENERATE_AWSPERSISTENCE_CODE_START).toString());
            PureModel pureModel = pureModelFunc.value();
            RichIterable<? extends Root_meta_pure_generation_metamodel_GenerationOutput> output = core_persistence_external_format_awspersistence_transformation.Root_meta_external_format_awspersistence_generation_generateAwsPersistenceFromPureWithScope_AwsPersistenceConfig_1__AwsPersistenceOutput_MANY_(awspersistenceConfig.process(pureModel), pureModel.getExecutionSupport());
            LOGGER.info(new LogInfo(pm, interactive ? LoggingEventType.GENERATE_AWSPERSISTENCE_CODE_INTERACTIVE_STOP : LoggingEventType.GENERATE_AWSPERSISTENCE_CODE_STOP, (double)System.currentTimeMillis() - start).toString());
            return ManageConstantResult.manageResult(pm, output.collect(v -> new GenerationOutput(v._content(), v._fileName(), v._format())).toList());
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, interactive ? LoggingEventType.GENERATE_AWSPERSISTENCE_CODE_INTERACTIVE_ERROR : LoggingEventType.GENERATE_AWSPERSISTENCE_CODE_ERROR, pm);
        }
    }

    @POST
    @Path("awsPersistence/trigger")
    @ApiOperation(value = "Trigger AwsPersistence pipeline")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response triggerAwsPersistence(AwsPersistenceTriggerSpecification triggerSpecification, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> profileManager)
    {
        String jobName = triggerSpecification.jobName;
        try {
            URL u = new URL(awsApiGatewayUrl + "trigger/" + jobName);
            HttpURLConnection http = (HttpURLConnection) u.openConnection();
            http.setRequestMethod("POST");
            http.setRequestProperty("Content-Type", "application/json");
            http.setRequestProperty("x-api-key", awsApiGatewayKey);
            http.setRequestProperty("Accept", "application/json");

            // Empty post body. If we need to post, do something like:
//            http.setDoOutput(true);
//            String json = objectMapper.writeValueAsString(triggerSpecification);
//            try(OutputStream os = http.getOutputStream()) {
//                byte[] input = json.getBytes("utf-8");
//                os.write(input, 0, input.length);
//            }

            int responseCode = http.getResponseCode();

            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(http.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                LOGGER.info("Triggered job " + jobName + " with result:" + response);
                return monitorAwsPersistence(jobName);
            }
            else {
                return errorResponse(responseCode, "When attempting to trigger job, encountered: " + http.getResponseMessage());
            }
        }
        catch (Exception ex)
        {
            return errorResponse(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "When attempting to trigger job, encountered: " + ex.getMessage());
        }
    }

    @GET
    @Path("awsPersistence/monitor/{jobName}")
    @ApiOperation(value = "Monitor AwsPersistence pipeline")
    public Response monitorAwsPersistence(@PathParam("jobName") String jobName)
    {
        try {
            URL u = new URL(awsApiGatewayUrl + "monitor/" + jobName);
            HttpURLConnection http = (HttpURLConnection) u.openConnection();
            http.setRequestMethod("GET");
            http.setDoOutput(true);
            http.setRequestProperty("x-api-key", awsApiGatewayKey);
            http.setRequestProperty("Accept", "application/json");

            int responseCode = http.getResponseCode();

            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(http.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                return Response.status(200).type(MediaType.APPLICATION_JSON).entity(response.toString()).build();
            }
            else {
                return errorResponse(responseCode, http.getResponseMessage());
            }
        }
        catch (Exception ex)
        {
            return errorResponse(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ex.getMessage());
        }
    }

    private Response errorResponse(int status, String error)
    {
        LOGGER.error(error);
        String text = "{\"status\":\"error\", \"message\":\"" + error + "\"}";
        return Response.status(status).type(MediaType.APPLICATION_JSON).entity(text).build();
    }

    @POST
    @Path("awsPersistence/triggertest")
    @ApiOperation(value = "Test endpoint - Trigger AwsPersistence pipeline - does not do anything - returns bogus data")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response triggerAwsPersistenceTest(AwsPersistenceTriggerSpecification triggerSpecification, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> profileManager)
    {
        String jobName = triggerSpecification.jobName;
        LOGGER.info("Pretending to trigger  job " + jobName);
        return monitorAwsPersistenceTest(jobName);
    }

    @GET
    @Path("awsPersistence/monitortest/{jobName}")
    @ApiOperation(value = "Test endpoint - Monitor AwsPersistence pipeline - returns bogus data")
    public Response monitorAwsPersistenceTest (@PathParam("jobName") String jobName)
    {
        LOGGER.info("Pretending to monitor job " + jobName);
        String response = "[{\"StartedOn\": \"yesterday\", \"CompletedOn\": \"yesterday\", \"JobRunState\": \"SUCCEEDED\", \"ErrorMessage\": \"" + jobName + "\"}, {\"StartedOn\": \"yesterday\", \"CompletedOn\": \"today\", \"JobRunState\": \"SUCCEEDED\", \"ErrorMessage\": \"\"}, {\"StartedOn\": \"today\", \"CompletedOn\": \"today\", \"JobRunState\": \"SUCCEEDED\", \"ErrorMessage\": \"\"}, {\"StartedOn\": \"tomorrow\", \"CompletedOn\": \"yesterday\", \"JobRunState\": \"SUCCEEDED\", \"ErrorMessage\": \"Wait a minute doc - are you telling me you built a time machine?\"}]";
        return Response.status(200).type(MediaType.APPLICATION_JSON).entity(response).build();
    }

}
