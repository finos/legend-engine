// Copyright 2023 Goldman Sachs
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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.api.analytics.model.BindingModelCoverageAnalysisInput;
import org.finos.legend.engine.language.pure.compiler.fromPureGraph.PureModelContextDataGenerator;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.api.result.ManageConstantResult;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.IdentityFactoryProvider;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.http.InflateInterceptor;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.finos.legend.pure.generated.Root_meta_analytics_binding_modelCoverage_BindingModelCoverageAnalysisResult;
import org.finos.legend.pure.generated.Root_meta_external_format_shared_binding_Binding;
import org.finos.legend.pure.generated.core_analytics_binding_modelCoverage_analytics;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Objects;

@Api(tags = "Analytics - Model")
@Path("pure/v1/analytics/binding")
public class BindingAnalytics
{
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    private final ModelManager modelManager;

    public BindingAnalytics(ModelManager modelManager)
    {
        this.modelManager = modelManager;
    }

    @POST
    @Path("modelCoverage")
    @ApiOperation(value = "Analyze the binding to identify models covered by the binding")
    @Consumes({MediaType.APPLICATION_JSON, InflateInterceptor.APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response analyzeBindingModelCoverage(BindingModelCoverageAnalysisInput input,
                                                @QueryParam("includeBinding") @DefaultValue("true") boolean includeBinding,
                                                @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        Identity identity = IdentityFactoryProvider.getInstance().makeIdentity(profiles);
        PureModelContextData pureModelContextData = this.modelManager.loadData(input.model, input.clientVersion, identity);
        PureModel pureModel = this.modelManager.loadModel(pureModelContextData, input.clientVersion, identity, null);
        Root_meta_external_format_shared_binding_Binding binding = (Root_meta_external_format_shared_binding_Binding) pureModel.getPackageableElement(input.binding);
        RichIterable<? extends Root_meta_external_format_shared_binding_Binding> bindings = Lists.immutable.of(binding);

        try (Scope scope = GlobalTracer.get().buildSpan("Analytics: binding model coverage").startActive(true))
        {
            try
            {
                Root_meta_analytics_binding_modelCoverage_BindingModelCoverageAnalysisResult result = core_analytics_binding_modelCoverage_analytics.Root_meta_analytics_binding_modelCoverage_getBindingModelCoverage_Binding_MANY__BindingModelCoverageAnalysisResult_1_(bindings, pureModel.getExecutionSupport());
                PureModelContextData classes = PureModelContextDataGenerator.generatePureModelContextDataFromClasses(result._classes(), input.clientVersion, pureModel.getExecutionSupport());
                PureModelContextData.Builder builder = PureModelContextData.newBuilder();
                if (includeBinding)
                {
                    builder.addElement(Objects.requireNonNull(pureModelContextData.getElements().stream().filter(el -> input.binding.equals(el.getPath())).findFirst().get()));
                }
                return ManageConstantResult.manageResult(identity.getName(), builder.build().combine(classes), objectMapper);
            }
            catch (Exception e)
            {
                return ExceptionTool.exceptionManager(e, LoggingEventType.ANALYTICS_ERROR, Response.Status.BAD_REQUEST, identity.getName());
            }
        }
    }
}
