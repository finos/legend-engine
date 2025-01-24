//  Copyright 2022 Goldman Sachs
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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.http.InflateInterceptor;
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

@Api(tags = "Analytics - Quality")
@Path("pure/v1/analytics/quality")
public class DataspaceQualityAnalytics
{
    private final ModelManager modelManager;

    public DataspaceQualityAnalytics(ModelManager modelManager)
    {
        this.modelManager = modelManager;
    }

    @POST
    @Path("checkDataspaceQuality")
    @ApiOperation("Checks the quality of provided Data space using PMCD")
    @Consumes({MediaType.APPLICATION_JSON, InflateInterceptor.APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkDataSpaceConstraints(DataspaceQualityCheckInput input, @ApiParam(hidden = true)
                                              @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        Identity identity = Identity.makeIdentity(profiles);
        PureModelContextData pureModelContextData = this.modelManager.loadData(input.model, input.clientVersion, identity);
        PureModel pureModel = this.modelManager.loadModel(pureModelContextData, input.clientVersion, identity, null);
        List<PackageableElement> allElements = pureModelContextData.getElements();
        return DataspaceQualityAnalyticsHelper.getResponse(pureModel, allElements);
    }
}
