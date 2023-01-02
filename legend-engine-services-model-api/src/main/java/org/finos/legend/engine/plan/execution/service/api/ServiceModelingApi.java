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
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.execution.service.ServiceModeling;
import org.finos.legend.engine.plan.execution.service.test.TestResult;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Service;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.kerberos.SubjectTools;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.finos.legend.engine.shared.core.operational.prometheus.MetricsHandler;
import org.finos.legend.engine.shared.core.operational.prometheus.Prometheus;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;
import org.slf4j.Logger;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;
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
        MetricsHandler.createMetrics(this.getClass());
    }

    @POST
    @Path("doTest")
    @ApiOperation(value = "Test a service. Only Full_Interactive mode is supported by giving appropriate PureModelContext (i.e. PureModelContextData)")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Prometheus(name = "service test", doc = "Service test execution duration")
    public Response doTest(PureModelContext service, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm, @Context UriInfo uriInfo)
    {
        MutableList<CommonProfile> profiles  = ProfileManagerHelper.extractProfiles(pm);
        String user = SubjectTools.getPrincipal(ProfileManagerHelper.extractSubject(profiles));

        long start = System.currentTimeMillis();
        try
        {
            if (!(service instanceof PureModelContextData))
            {
                throw new RuntimeException("Only Full Interactive mode currently supported.  Received " + service.getClass().getName());
            }
            LOGGER.info(new LogInfo(user, LoggingEventType.SERVICE_FACADE_R_TEST_SERVICE_FULL_INTERACTIVE, "").toString());
            String metricContext = uriInfo != null ? uriInfo.getPath() : null;
            List<TestResult> results = this.serviceModeling.testService(profiles, service, metricContext);
            MetricsHandler.observe("service test", start, System.currentTimeMillis());
            MetricsHandler.observeRequest(uriInfo != null ? uriInfo.getPath() : null, start, System.currentTimeMillis());
            return Response.ok(objectMapper.writeValueAsString(results), MediaType.APPLICATION_JSON_TYPE).build();
        }
        catch (Exception ex)
        {
            String servicePattern = null;
            if (service instanceof PureModelContextData)
            {
                PureModelContextData data = ((PureModelContextData) service).shallowCopy();
                Service invokedService = (Service) Iterate.detect(data.getElements(), e -> e instanceof Service);
                servicePattern = invokedService == null ? null : invokedService.pattern;
            }
            Response response = ExceptionTool.exceptionManager(ex, LoggingEventType.SERVICE_ERROR, user);
            MetricsHandler.observeError(LoggingEventType.SERVICE_TEST_EXECUTE_ERROR, ex, servicePattern);
            return response;
        }
    }

    @POST
    @Path("doValidation")
    @ApiOperation(value = "Execute a service validation assertion. Only Full_Interactive mode is supported by giving appropriate PureModelContext (i.e. PureModelContextData)")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Prometheus(name = "service validation", doc = "Service validation execution duration")
    public Response doValidation(PureModelContext service,
                                 @DefaultValue("") @ApiParam(value = "The ID of the assertion to execute from the service", required = true) @QueryParam("assertionId") String assertionId,
                                 @DefaultValue(SerializationFormat.defaultFormatString) @QueryParam("serializationFormat") SerializationFormat format,
                                 @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm,
                                 @Context UriInfo uriInfo)
    {
        MutableList<CommonProfile> profiles  = ProfileManagerHelper.extractProfiles(pm);
        String user = SubjectTools.getPrincipal(ProfileManagerHelper.extractSubject(profiles));
        try
        {
            if (!(service instanceof PureModelContextData))
            {
                throw new RuntimeException("Only Full Interactive mode currently supported.  Received " + service.getClass().getName());
            }
            LOGGER.info(new LogInfo(user, LoggingEventType.SERVICE_FACADE_R_TEST_SERVICE_FULL_INTERACTIVE, "").toString());
            String metricContext = uriInfo != null ? uriInfo.getPath() : null;

            return this.serviceModeling.validateService(profiles, service, metricContext, assertionId, format);
        }
        catch (Exception ex)
        {
            String servicePattern = null;
            if (service instanceof PureModelContextData)
            {
                PureModelContextData data = ((PureModelContextData) service).shallowCopy();
                Service invokedService = (Service) Iterate.detect(data.getElements(), e -> e instanceof Service);
                servicePattern = invokedService == null ? null : invokedService.pattern;
            }
            Response response = ExceptionTool.exceptionManager(ex, LoggingEventType.SERVICE_ERROR, user);
            MetricsHandler.observeError(LoggingEventType.SERVICE_TEST_EXECUTE_ERROR, ex, servicePattern);
            return response;
        }
    }
}
