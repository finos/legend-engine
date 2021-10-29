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

package org.finos.legend.engine.plan.execution.service.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.plan.execution.service.ServiceModeling;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;
import org.slf4j.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.finos.legend.engine.shared.core.operational.http.InflateInterceptor.APPLICATION_ZLIB;

@Api(tags = "Service")
@Path("service/v1")
@Produces(MediaType.APPLICATION_JSON)
public class ServiceModelingApi
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");
    public static ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
    private final ServiceModeling serviceModeling;

    public ServiceModelingApi(ModelManager modelManager, DeploymentMode deploymentMode)
    {
        this(new ServiceModeling(modelManager, deploymentMode));
    }

    public ServiceModelingApi(ServiceModeling serviceModeling)
    {
        this.serviceModeling = serviceModeling;
    }

    @POST
    @Path("doTest")
    @ApiOperation(value = "Test a service. Only Full_Interactive mode is supported by giving appropriate PureModelContext (i.e. PureModelContextData)")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    public Response doTest(PureModelContext service, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles  = ProfileManagerHelper.extractProfiles(pm);
        try
        {
            if (!(service instanceof PureModelContextData))
            {
                throw new RuntimeException("Only Full Interactive mode currently supported.  Received " + service.getClass().getName());
            }
            LOGGER.info(new LogInfo(profiles, LoggingEventType.SERVICE_FACADE_R_TEST_SERVICE_FULL_INTERACTIVE, "").toString());
            return Response.ok(objectMapper.writeValueAsString(this.serviceModeling.testService(profiles, service)), MediaType.APPLICATION_JSON_TYPE).build();
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, LoggingEventType.SERVICE_ERROR, profiles);
        }
    }
}
