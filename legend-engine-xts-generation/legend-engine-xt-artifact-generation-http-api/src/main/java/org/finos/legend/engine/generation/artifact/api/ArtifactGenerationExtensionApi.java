// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.generation.artifact.api;

import static org.finos.legend.engine.shared.core.operational.logs.LoggingEventType.SCHEMA_GENERATION_ERROR;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.api.result.ManageConstantResult;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.IdentityFactoryProvider;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.http.InflateInterceptor;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;
import org.slf4j.Logger;

@Api(tags = "External - Format")
@Path("pure/v1/generation")
@Produces(MediaType.APPLICATION_JSON)
public class ArtifactGenerationExtensionApi
{


    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ArtifactGenerationExtensionApi.class);

    private final ModelManager modelManager;


    public ArtifactGenerationExtensionApi(ModelManager modelManager)
    {
        this.modelManager = modelManager;
    }

    @POST
    @Path("generateArtifacts")
    @ApiOperation(value = "Generates artifacts based on model")
    @Consumes({MediaType.APPLICATION_JSON, InflateInterceptor.APPLICATION_ZLIB})
    public Response generate(ArtifactGenerationExtensionInput artifactGenerationExtensionInput, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        Identity identity = IdentityFactoryProvider.getInstance().makeIdentity(profiles);
        try (Scope scope = GlobalTracer.get().buildSpan("Service: Generate Model From External Format Schema").startActive(true))
        {
            LOGGER.info(new LogInfo(identity.getName(), ArtifactGenerationLoggingEventType.GENERATE_ARTIFACT_EXTENSIONS_START).toString());
            ArtifactGenerationExtensionOutput artifactGenerationExtensionOutput = new ArtifactGenerationExtensionRunner(modelManager).run(artifactGenerationExtensionInput, identity);
            LOGGER.info(new LogInfo(identity.getName(), ArtifactGenerationLoggingEventType.GENERATE_ARTIFACT_EXTENSIONS_STOP).toString());
            return ManageConstantResult.manageResult(identity.getName(), artifactGenerationExtensionOutput, objectMapper);

        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex,SCHEMA_GENERATION_ERROR, identity.getName());

        }
    }

}
