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

package org.finos.legend.engine.constraint.pure.api;


import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.engine.constraint.pure.api.model.RelationalValidationInput;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.transformers.PlanTransformer;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.finos.legend.engine.shared.core.operational.prometheus.MetricsHandler;
import org.finos.legend.pure.generated.Root_meta_pure_router_extension_RouterExtension;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;


import static org.finos.legend.engine.plan.execution.api.result.ResultManager.manageResult;
import static org.finos.legend.engine.shared.core.operational.http.InflateInterceptor.APPLICATION_ZLIB;
import static org.finos.legend.pure.generated.core_relational_relational_validation_validation.Root_meta_relational_validation_generateValidationQueryFromClass_Class_1__Mapping_1__Runtime_1__String_MANY__FunctionDefinition_1_;

@Api(tags = "Pure - Constraints")
@Path("pure/v1/constraint")
@Produces(MediaType.APPLICATION_JSON)
public class Validate {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");
    private final ModelManager modelManager;
    private final PlanExecutor planExecutor;
    private Function<PureModel, RichIterable<? extends Root_meta_pure_router_extension_RouterExtension>> extensions;
    private MutableList<PlanTransformer> transformers;

    public Validate(ModelManager modelManager, PlanExecutor planExecutor, Function<PureModel, RichIterable<? extends Root_meta_pure_router_extension_RouterExtension>> extensions, MutableList<PlanTransformer> transformers) {
        this.modelManager = modelManager;
        this.planExecutor = planExecutor;
        this.extensions = extensions;
        this.transformers = transformers;
        MetricsHandler.createMetrics(this.getClass());
    }

    @POST
    @ApiOperation(value = "Executes the constraints on a pure class in the context of a Mapping and a relational Runtime.")
    @Path("relationalValidation")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    public Response validate(@Context HttpServletRequest request, RelationalValidationInput executeInput, @DefaultValue(SerializationFormat.defaultFormatString) @QueryParam("serializationFormat") SerializationFormat format, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm, @Context UriInfo uriInfo) {

        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        long start = System.currentTimeMillis();
        try (Scope scope = GlobalTracer.get().buildSpan("Relational Validation").startActive(true)) {
            String clientVersion = executeInput.clientVersion == null ? PureClientVersions.production : executeInput.clientVersion;
            PureModel pureModel = modelManager.loadModel(executeInput.model, clientVersion, profiles, null);
            Mapping mapping = pureModel.getMapping(executeInput.mapping);
            Class<?> _class = pureModel.getClass(executeInput._class);
            org.finos.legend.pure.m3.coreinstance.meta.pure.runtime.Runtime runtime = pureModel.getRuntime(executeInput.runtime);
            MutableList<String> constraintList = Lists.mutable.empty();
            CompiledExecutionSupport executionSupport = pureModel.getExecutionSupport();
            if (!executeInput.constraintIds.isEmpty()) {
                constraintList.addAllIterable(executeInput.constraintIds);
            }

            LambdaFunction<?> fn = (LambdaFunction<?>) Root_meta_relational_validation_generateValidationQueryFromClass_Class_1__Mapping_1__Runtime_1__String_MANY__FunctionDefinition_1_(_class, mapping, runtime, constraintList, executionSupport);
            org.finos.legend.pure.m3.coreinstance.meta.pure.runtime.ExecutionContext processedContext = null;
            if (executeInput.context != null) {
                processedContext = HelperValueSpecificationBuilder.processExecutionContext(executeInput.context, pureModel.getContext());
            }

            Response response = exec(fn,
                    () -> modelManager.loadModel(executeInput.model, clientVersion, profiles, null),
                    this.planExecutor,
                    mapping,
                    runtime,
                    processedContext,
                    clientVersion,
                    profiles, request.getRemoteUser(), format);
            if (response.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL)) {
                MetricsHandler.observeRequest(uriInfo != null ? uriInfo.getPath() : null, start, System.currentTimeMillis());
            }
            return response;
        } catch (Exception ex) {
            Response response = ExceptionTool.exceptionManager(ex, LoggingEventType.VALIDATION_INTERACTIVE_ERROR, profiles);
            MetricsHandler.incrementErrorCount(uriInfo != null ? uriInfo.getPath() : null, response.getStatus());
            return response;
        }
    }


    public Response exec(LambdaFunction functionFunc, Function0<PureModel> pureModelFunc, PlanExecutor planExecutor, Mapping mapping,
                         org.finos.legend.pure.m3.coreinstance.meta.pure.runtime.Runtime runtime, org.finos.legend.pure.m3.coreinstance.meta.pure.runtime.ExecutionContext context, String clientVersion, MutableList<CommonProfile> pm, String user, SerializationFormat format) {
        try {
            long start = System.currentTimeMillis();
            LOGGER.info(new LogInfo(pm, LoggingEventType.VALIDATION_INTERACTIVE_START, "").toString());
            PureModel pureModel = pureModelFunc.value();
            SingleExecutionPlan plan = PlanGenerator.generateExecutionPlanWithTrace(functionFunc,
                    mapping,
                    runtime,
                    context,
                    pureModel,
                    clientVersion,
                    PlanPlatform.JAVA,
                    pm,
                    this.extensions.apply(pureModel),
                    this.transformers
            );
            Result result = planExecutor.execute(plan, Maps.mutable.empty(), user, pm);
            LOGGER.info(new LogInfo(pm, LoggingEventType.VALIDATION_INTERACTIVE_STOP, (double) System.currentTimeMillis() - start).toString());
            MetricsHandler.observe("relationalValidation", start, System.currentTimeMillis());
            try (Scope scope = GlobalTracer.get().buildSpan("Manage Results").startActive(true)) {
                return manageResult(pm, result, format, LoggingEventType.VALIDATION_INTERACTIVE_ERROR);
            }

        } catch (Exception ex) {
            MetricsHandler.observeError("relationalValidation");
            Response response = ExceptionTool.exceptionManager(ex, LoggingEventType.VALIDATION_INTERACTIVE_ERROR, pm);
            MetricsHandler.incrementErrorCount("pure/v1/constraint/relationalValidation", response.getStatus());
            return response;

        }
    }
}
