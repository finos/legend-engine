//  Copyright 2023 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.api.analytics;

import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.engine.api.analytics.model.StoreEntitlementAnalyticsInput;
import org.finos.legend.engine.entitlement.services.EntitlementServiceExtension;
import org.finos.legend.engine.entitlement.services.EntitlementServiceExtensionLoader;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.shared.core.api.result.ManageConstantResult;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Api(tags = "Entitlements - Analytics")
@Path("entitlements/v1")
public class StoreEntitlementAnalytics
{
    private final ModelManager modelManager;

    public StoreEntitlementAnalytics(ModelManager modelManager)
    {
        this.modelManager = modelManager;
    }

    @POST
    @Path("datasetSpecification")
    @ApiOperation(value = "generate datasetSpecifications")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response generateDatasetSpecifications(StoreEntitlementAnalyticsInput input, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        List<EntitlementServiceExtension> extensions = EntitlementServiceExtensionLoader.extensions();
        try (Scope scope = GlobalTracer.get().buildSpan("generate datasetSpecifications").startActive(true))
        {
            try
            {
                return ManageConstantResult.manageResult(profiles, LazyIterate.flatCollect(extensions, extension -> extension.generateDatasetSpecifications(input.query, input.runtime, input.mapping, input.model, modelManager, profiles)).toList());
            }
            catch (Exception e)
            {
                return ExceptionTool.exceptionManager(e, LoggingEventType.ANALYTICS_ERROR, Response.Status.BAD_REQUEST, profiles);
            }
        }
    }

    @POST
    @Path("checkStoreEntitlements")
    @ApiOperation(value = "check store entitlements")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response generateEntitlementReports(StoreEntitlementAnalyticsInput input, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        List<EntitlementServiceExtension> extensions = EntitlementServiceExtensionLoader.extensions();
        try (Scope scope = GlobalTracer.get().buildSpan("check store entitlements").startActive(true))
        {
            try
            {
                return ManageConstantResult.manageResult(profiles, LazyIterate.flatCollect(extensions, extension -> extension.generateDatasetEntitlementReports(input.query, input.runtime, input.mapping, input.model, modelManager, profiles)).toList());
            }
            catch (Exception e)
            {
                return ExceptionTool.exceptionManager(e, LoggingEventType.ANALYTICS_ERROR, Response.Status.BAD_REQUEST, profiles);
            }
        }
    }
}
