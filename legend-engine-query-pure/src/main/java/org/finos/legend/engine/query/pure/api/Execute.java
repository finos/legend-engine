// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.query.pure.api;

import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperRuntimeBuilder;
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
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.Runtime;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.Variable;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.executionContext.ExecutionContext;
import org.finos.legend.engine.shared.core.api.model.ExecuteInput;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.finos.legend.engine.shared.core.operational.prometheus.MetricsHandler;
import org.finos.legend.engine.shared.core.operational.prometheus.Prometheus;
import org.finos.legend.pure.generated.Root_meta_pure_router_extension_RouterExtension;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
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

import static org.finos.legend.engine.plan.execution.api.result.ResultManager.manageResult;
import static org.finos.legend.engine.shared.core.operational.http.InflateInterceptor.APPLICATION_ZLIB;

@Api(tags = "Pure - Execution")
@Path("pure/v1/execution")
@Produces(MediaType.APPLICATION_JSON)
public class Execute
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");
    private final ModelManager modelManager;
    private final PlanExecutor planExecutor;
    private Function<PureModel, RichIterable<? extends Root_meta_pure_router_extension_RouterExtension>> extensions;
    private MutableList<PlanTransformer> transformers;

    public Execute(ModelManager modelManager, PlanExecutor planExecutor, Function<PureModel, RichIterable<? extends Root_meta_pure_router_extension_RouterExtension>> extensions, MutableList<PlanTransformer> transformers)
    {
        this.modelManager = modelManager;
        this.planExecutor = planExecutor;
        this.extensions = extensions;
        this.transformers = transformers;
        MetricsHandler.createMetrics(this.getClass());
    }

    @POST
    @ApiOperation(value = "Execute a Pure query (function) in the context of a Mapping and a Runtime. Full Interactive and Semi Interactive modes are supported by giving the appropriate PureModelContext (respectively PureModelDataContext and PureModelContextComposite). Production executions need to use the Service interface.")
    @Path("execute")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    public Response execute(@Context HttpServletRequest request, ExecuteInput executeInput, @DefaultValue(SerializationFormat.defaultFormatString) @QueryParam("serializationFormat") SerializationFormat format, @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        try (Scope scope = GlobalTracer.get().buildSpan("Service: Execute").startActive(true))
        {
            String clientVersion = executeInput.clientVersion == null ? PureClientVersions.latest : executeInput.clientVersion;
            return exec(pureModel -> HelperValueSpecificationBuilder.buildLambda(executeInput.function.body, Lists.fixedSize.<Variable>empty(), pureModel.getContext()),
                    () -> modelManager.loadModel(executeInput.model, clientVersion, profiles, null),
                    this.planExecutor,
                    executeInput.mapping,
                    executeInput.runtime,
                    executeInput.context,
                    clientVersion,
                    profiles, request.getRemoteUser(), format);
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, LoggingEventType.EXECUTE_INTERACTIVE_ERROR, profiles);
        }
    }

    @POST
    @Path("generatePlan")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Prometheus(name="generate plan")
    public Response generatePlan(@Context HttpServletRequest request, ExecuteInput executeInput, @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        try
        {
            long start = System.currentTimeMillis();
            LOGGER.info(new LogInfo(profiles, LoggingEventType.EXECUTION_PLAN_GENERATION_START, "").toString());
            String clientVersion = executeInput.clientVersion == null ? PureClientVersions.latest : executeInput.clientVersion;
            PureModel pureModel = modelManager.loadModel(executeInput.model, clientVersion, profiles, null);
            LambdaFunction<?> lambda = HelperValueSpecificationBuilder.buildLambda(executeInput.function.body, Lists.fixedSize.empty(), pureModel.getContext());
            Mapping mapping = executeInput.mapping == null ? null : pureModel.getMapping(executeInput.mapping);
            org.finos.legend.pure.m3.coreinstance.meta.pure.runtime.Runtime runtime = HelperRuntimeBuilder.buildPureRuntime(executeInput.runtime, pureModel.getContext());
            org.finos.legend.pure.m3.coreinstance.meta.pure.runtime.ExecutionContext context = HelperValueSpecificationBuilder.processExecutionContext(executeInput.context, pureModel.getContext());
            String plan = PlanGenerator.generateExecutionPlanAsString(lambda, mapping, runtime, context, pureModel, clientVersion, PlanPlatform.JAVA, null, this.extensions.apply(pureModel), this.transformers);
            LOGGER.info(new LogInfo(profiles, LoggingEventType.EXECUTION_PLAN_GENERATION_STOP, (double)System.currentTimeMillis() - start).toString());
            MetricsHandler.observe("generate plan", start, System.currentTimeMillis());
            return Response.ok().type(MediaType.APPLICATION_JSON_TYPE).entity(plan).build();
        }
        catch (Exception ex)
        {
            MetricsHandler.observeError("generate plan");
            return ExceptionTool.exceptionManager(ex, LoggingEventType.EXECUTION_PLAN_GENERATION_ERROR, profiles);
        }
    }

    public Response exec(Function<PureModel, LambdaFunction<?>> functionFunc, Function0<PureModel> pureModelFunc, PlanExecutor planExecutor, String mapping, Runtime runtime, ExecutionContext context, String clientVersion, MutableList<CommonProfile> pm, String user, SerializationFormat format)
    {
        try
        {
            long start = System.currentTimeMillis();
            LOGGER.info(new LogInfo(pm, LoggingEventType.EXECUTE_INTERACTIVE_START, "").toString());
            PureModel pureModel = pureModelFunc.value();
            SingleExecutionPlan plan = PlanGenerator.generateExecutionPlanWithTrace(functionFunc.valueOf(pureModel),
                    mapping == null ? null : pureModel.getMapping(mapping),
                    HelperRuntimeBuilder.buildPureRuntime(runtime, pureModel.getContext()),
                    HelperValueSpecificationBuilder.processExecutionContext(context, pureModel.getContext()),
                    pureModel,
                    clientVersion,
                    PlanPlatform.JAVA,
                    pm,
                    this.extensions.apply(pureModel),
                    this.transformers
            );
            Result result = planExecutor.execute(plan, Maps.mutable.empty(), user, pm);
            LOGGER.info(new LogInfo(pm, LoggingEventType.EXECUTE_INTERACTIVE_STOP, (double)System.currentTimeMillis() - start).toString());
            MetricsHandler.observe("execute", start, System.currentTimeMillis());
            try (Scope scope = GlobalTracer.get().buildSpan("Manage Results").startActive(true))
            {
                return manageResult(pm, result, format, LoggingEventType.EXECUTE_INTERACTIVE_ERROR);
            }

        }
        catch (Exception ex)
        {
            MetricsHandler.observeError("execute");
            return ExceptionTool.exceptionManager(ex, LoggingEventType.EXECUTE_INTERACTIVE_ERROR, pm);
        }
    }
}
