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

package org.finos.legend.engine.query.diagram.api.analytics;

import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperModelBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.query.diagram.api.analytics.model.DiagramModelCoverageAnalysisInput;
import org.finos.legend.engine.query.diagram.api.analytics.model.DiagramModelCoverageAnalysisResult;
import org.finos.legend.engine.shared.core.api.result.ManageConstantResult;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.http.InflateInterceptor;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_diagram_Diagram;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_diagram_analytics_modelCoverage_DiagramModelCoverageAnalysisResult;
import org.finos.legend.pure.generated.core_diagram_analytics_analytics;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Any;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.finos.legend.engine.language.pure.dsl.diagram.compiler.toPureGraph.HelperDiagramBuilder.getDiagram;

@Api(tags = "Analytics - Model")
@Path("pure/v1/analytics/diagram")
public class DiagramAnalytics
{
    private final ModelManager modelManager;

    public DiagramAnalytics(ModelManager modelManager)
    {
        this.modelManager = modelManager;
    }

    @POST
    @Path("modelCoverage")
    @ApiOperation(value = "Analyze the diagram to identify models covered by the diagram")
    @Consumes({MediaType.APPLICATION_JSON, InflateInterceptor.APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response analyzeDiagramModelCoverage(DiagramModelCoverageAnalysisInput input, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        PureModel pureModel = this.modelManager.loadModel(input.model, input.clientVersion, profiles, null);
        Root_meta_pure_metamodel_diagram_Diagram diagram = getDiagram(input.diagram, null, pureModel.getContext());

        try (Scope scope = GlobalTracer.get().buildSpan("Analytics: diagram model coverage").startActive(true))
        {
            try
            {
                Root_meta_pure_metamodel_diagram_analytics_modelCoverage_DiagramModelCoverageAnalysisResult result = core_diagram_analytics_analytics.Root_meta_pure_metamodel_diagram_analytics_modelCoverage_getDiagramModelCoverage_Diagram_1__DiagramModelCoverageAnalysisResult_1_(diagram, pureModel.getExecutionSupport());
                return ManageConstantResult.manageResult(profiles, new DiagramModelCoverageAnalysisResult(
                        result._profiles().collect(profile -> HelperModelBuilder.getElementFullPath(profile, pureModel.getExecutionSupport())).toList(),
                        result._enumerations().collect(enumeration -> HelperModelBuilder.getElementFullPath(enumeration, pureModel.getExecutionSupport())).toList(),
                        result._classes().collect(_class -> HelperModelBuilder.getElementFullPath(_class, pureModel.getExecutionSupport())).toList()));
            }
            catch (Exception e)
            {
                return ExceptionTool.exceptionManager(e, LoggingEventType.ANALYTICS_ERROR, Response.Status.BAD_REQUEST, profiles);
            }
        }
    }
}
