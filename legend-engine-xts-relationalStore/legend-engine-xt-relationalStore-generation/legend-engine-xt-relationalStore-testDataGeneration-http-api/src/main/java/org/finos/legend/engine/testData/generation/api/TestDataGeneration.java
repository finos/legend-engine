/**
 * Copyright (c) 2020-present, Goldman Sachs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.finos.legend.engine.testData.generation.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.api.result.ManageConstantResult;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.finos.legend.engine.shared.core.operational.prometheus.MetricsHandler;
import org.finos.legend.engine.shared.core.operational.prometheus.Prometheus;
import org.finos.legend.engine.testData.generation.model.TestDataGenerationInput;
import org.finos.legend.engine.testData.generation.model.TestDataGenerationResult;
import org.finos.legend.engine.testData.generation.service.TestDataGenerationService;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import static org.finos.legend.engine.shared.core.operational.http.InflateInterceptor.APPLICATION_ZLIB;

@Tag(name = "Testing")
@Path("pure/v1/testData/generation")
@Produces(MediaType.APPLICATION_JSON)
public class TestDataGeneration
{
    private final ModelManager modelManager;
    private final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    public TestDataGeneration(ModelManager modelManager)
    {
        this.modelManager = modelManager;
    }

    @POST
    @Path("DONOTUSE_generateTestData")
    @Operation(summary = "Studio WIP: will not be backward compatible until we remove the DONOTUSE flag")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Prometheus(name = "generate test data")
    public Response generateTestData(TestDataGenerationInput input, @Parameter(hidden = true) @Pac4JProfileManager ProfileManager profileManager, @Context UriInfo uriInfo)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(profileManager);
        Identity identity = Identity.makeIdentity(profiles);
        PureModel pureModel = modelManager.loadModel(input.model, input.clientVersion == null ? PureClientVersions.production : input.clientVersion, identity, null);
        long start = System.currentTimeMillis();
        try
        {
            TestDataGenerationResult result = new TestDataGenerationResult(TestDataGenerationService.generateEmbeddedData(input.query, pureModel.getMapping(input.mapping), pureModel));
            long end = System.currentTimeMillis();
            MetricsHandler.observeRequest(uriInfo != null ? uriInfo.getPath() : null, start, end);
            return ManageConstantResult.manageResult(identity.getName(), result, objectMapper);
        }
        catch (Exception e)
        {
            MetricsHandler.observeError(LoggingEventType.TEST_DATA_GENERATION_ERROR, e, null);
            return ExceptionTool.exceptionManager(e, LoggingEventType.TEST_DATA_GENERATION_ERROR,  Response.Status.BAD_REQUEST, identity.getName());
        }
    }
}
