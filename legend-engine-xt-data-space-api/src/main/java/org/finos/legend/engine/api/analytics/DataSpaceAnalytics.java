// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.api.analytics;

import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.api.analytics.model.DataSpaceAnalysisInput;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.api.result.ManageConstantResult;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.http.InflateInterceptor;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_dataSpace_DataSpace;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperDataSpaceBuilder.getDataSpace;
import static org.finos.legend.engine.api.analytics.DataSpaceAnalyticsHelper.analyzeDataSpace;

@Api(tags = "Analytics - Model")
@Path("pure/v1/analytics/dataSpace")
public class DataSpaceAnalytics
{
    private final ModelManager modelManager;

    public DataSpaceAnalytics(ModelManager modelManager)
    {
        this.modelManager = modelManager;
    }

    @POST
    @Path("modelCoverage")
    @ApiOperation(value = "Analyze the data space to identify models covered by the data space")
    @Consumes({MediaType.APPLICATION_JSON, InflateInterceptor.APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response analyzeDataSpaceModelCoverage(DataSpaceAnalysisInput input, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        PureModelContextData pureModelContextData = this.modelManager.loadData(input.model, input.clientVersion, profiles);
        PureModel pureModel = this.modelManager.loadModel(pureModelContextData, input.clientVersion, profiles, null);
        Root_meta_pure_metamodel_dataSpace_DataSpace dataSpace = getDataSpace(input.dataSpace, null, pureModel.getContext());

        try (Scope scope = GlobalTracer.get().buildSpan("Analytics: data space model coverage").startActive(true))
        {
            try
            {
                return ManageConstantResult.manageResult(profiles, analyzeDataSpace(dataSpace, pureModelContextData, pureModel, input));
            }
            catch (Exception e)
            {
                return ExceptionTool.exceptionManager(e, LoggingEventType.ANALYTICS_ERROR, Response.Status.BAD_REQUEST, profiles);
            }
        }
    }
}
