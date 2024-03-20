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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.engine.api.analytics.model.CheckEntitlementsResult;
import org.finos.legend.engine.api.analytics.model.EntitlementReportAnalyticsInput;
import org.finos.legend.engine.api.analytics.model.StoreEntitlementAnalyticsInput;
import org.finos.legend.engine.api.analytics.model.SurveyDatasetsResult;
import org.finos.legend.engine.entitlement.model.entitlementReport.DatasetEntitlementReport;
import org.finos.legend.engine.entitlement.model.specification.DatasetSpecification;
import org.finos.legend.engine.entitlement.services.EntitlementModelObjectMapperFactory;
import org.finos.legend.engine.entitlement.services.EntitlementServiceExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.shared.core.api.result.ManageConstantResult;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.IdentityFactoryProvider;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.finos.legend.pure.generated.Root_meta_core_runtime_Runtime;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

import static org.finos.legend.engine.shared.core.operational.http.InflateInterceptor.APPLICATION_ZLIB;

@Api(tags = "Analytics - Store Entitlement")
@Path("pure/v1/analytics/store-entitlement")
public class StoreEntitlementAnalytics
{
    private static final ObjectMapper objectMapper = EntitlementModelObjectMapperFactory.getNewObjectMapper();
    private final ModelManager modelManager;
    private List<EntitlementServiceExtension> entitlementServiceExtensions;

    public StoreEntitlementAnalytics(ModelManager modelManager, List<EntitlementServiceExtension> entitlementServiceExtensions)
    {
        this.modelManager = modelManager;
        this.entitlementServiceExtensions = entitlementServiceExtensions;
    }

    @POST
    @Path("surveyDatasets")
    @ApiOperation(value = "generate dataset specifications")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response generateDatasetSpecifications(StoreEntitlementAnalyticsInput input, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        Identity identity = IdentityFactoryProvider.getInstance().makeIdentity(profiles);
        PureModel pureModel = modelManager.loadModel(input.model, input.clientVersion == null ? PureClientVersions.production : input.clientVersion, identity, null);
        Mapping mapping = pureModel.getMapping(input.mappingPath);
        Root_meta_core_runtime_Runtime runtime = pureModel.getRuntime(input.runtimePath);
        try (Scope scope = GlobalTracer.get().buildSpan("generate dataset specifications").startActive(true))
        {
            List<DatasetSpecification> datasets = LazyIterate.flatCollect(this.entitlementServiceExtensions, extension -> extension.generateDatasetSpecifications(input.query, input.runtimePath, runtime, input.mappingPath, mapping, input.model, pureModel)).toList();
            return ManageConstantResult.manageResult(identity.getName(), new SurveyDatasetsResult(datasets), objectMapper);
        }
        catch (Exception e)
        {
            return ExceptionTool.exceptionManager(e, LoggingEventType.ANALYTICS_ERROR, Response.Status.BAD_REQUEST, identity.getName());
        }
    }

    @POST
    @Path("checkDatasetEntitlements")
    @ApiOperation(value = "check data set entitlements")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response generateEntitlementReports(EntitlementReportAnalyticsInput input, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        Identity identity = IdentityFactoryProvider.getInstance().makeIdentity(profiles);
        StoreEntitlementAnalyticsInput storeEntitlementAnalyticsInput = input.getStoreEntitlementAnalyticsInput();
        PureModel pureModel = modelManager.loadModel(storeEntitlementAnalyticsInput.model, storeEntitlementAnalyticsInput.clientVersion == null ? PureClientVersions.production : storeEntitlementAnalyticsInput.clientVersion, identity, null);
        Mapping mapping = pureModel.getMapping(storeEntitlementAnalyticsInput.mappingPath);
        Root_meta_core_runtime_Runtime runtime = pureModel.getRuntime(storeEntitlementAnalyticsInput.runtimePath);
        try (Scope scope = GlobalTracer.get().buildSpan("check entitlements").startActive(true))
        {
            List<DatasetEntitlementReport> reports = LazyIterate.flatCollect(this.entitlementServiceExtensions, extension -> extension.generateDatasetEntitlementReports(input.getReports(), storeEntitlementAnalyticsInput.query, storeEntitlementAnalyticsInput.runtimePath, runtime, storeEntitlementAnalyticsInput.mappingPath, mapping, storeEntitlementAnalyticsInput.model, pureModel, identity)).toList();
            return ManageConstantResult.manageResult(identity.getName(), new CheckEntitlementsResult(reports), objectMapper);
        }
        catch (Exception e)
        {
            return ExceptionTool.exceptionManager(e, LoggingEventType.ANALYTICS_ERROR, Response.Status.BAD_REQUEST, identity.getName());
        }
    }
}
