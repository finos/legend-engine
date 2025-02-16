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

import com.fasterxml.jackson.core.JsonProcessingException;
import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperRuntimeBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.api.request.RequestContextHelper;
import org.finos.legend.engine.plan.execution.authorization.PlanExecutionAuthorizer;
import org.finos.legend.engine.plan.execution.authorization.PlanExecutionAuthorizerInput;
import org.finos.legend.engine.plan.execution.authorization.PlanExecutionAuthorizerOutput;
import org.finos.legend.engine.plan.execution.cache.executionPlan.ExecutionPlanCache;
import org.finos.legend.engine.plan.execution.parameterization.ParameterizedValueSpecification;
import org.finos.legend.engine.plan.execution.cache.executionPlan.PlanCacheKey;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.execution.stores.StoreExecutionState;
import org.finos.legend.engine.plan.execution.stores.StoreType;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.PlanWithDebug;
import org.finos.legend.engine.plan.generation.transformers.PlanTransformer;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.SDLC;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.ExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.ParameterValue;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.Runtime;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.Variable;
import org.finos.legend.engine.protocol.pure.m3.function.LambdaFunction;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.executionContext.ExecutionContext;
import org.finos.legend.engine.query.pure.cache.PureExecutionCacheKey;
import org.finos.legend.engine.shared.core.api.model.ExecuteInput;
import org.finos.legend.engine.shared.core.api.request.RequestContext;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.finos.legend.engine.shared.core.operational.prometheus.MetricsHandler;
import org.finos.legend.engine.shared.core.operational.prometheus.Prometheus;
import org.finos.legend.engine.plan.execution.planHelper.PrimitiveValueSpecificationToObjectVisitor;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.Root_meta_pure_runtime_ExecutionContext;
import org.finos.legend.pure.generated.Root_meta_core_runtime_Runtime;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;
import org.slf4j.Logger;

import java.util.Map;
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.finos.legend.engine.plan.execution.api.result.ResultManager.manageResult;
import static org.finos.legend.engine.plan.execution.authorization.PlanExecutionAuthorizerInput.ExecutionMode.INTERACTIVE_EXECUTION;
import static org.finos.legend.engine.plan.execution.nodes.helpers.ExecuteNodeParameterTransformationHelper.buildParameterToConstantResult;
import static org.finos.legend.engine.shared.core.operational.http.InflateInterceptor.APPLICATION_ZLIB;

@Api(tags = "Pure - Execution")
@Path("pure/v1/execution")
@Produces(MediaType.APPLICATION_JSON)
public class Execute
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(Execute.class);
    private final ModelManager modelManager;
    private final PlanExecutor planExecutor;
    private final Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> extensions;
    private final Iterable<? extends PlanTransformer> transformers;
    private final PlanExecutionAuthorizer planExecutionAuthorizer;
    private final ExecutionPlanCache executionPlanCache;

    public Execute(ModelManager modelManager, PlanExecutor planExecutor, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> extensions, Iterable<? extends PlanTransformer> transformers)
    {
        this(modelManager, planExecutor, extensions, transformers, null);
    }

    public Execute(ModelManager modelManager, PlanExecutor planExecutor, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> extensions, Iterable<? extends PlanTransformer> transformers, PlanExecutionAuthorizer planExecutionAuthorizer)
    {
        this(modelManager, planExecutor, extensions, transformers, planExecutionAuthorizer, null);

    }

    public Execute(ModelManager modelManager, PlanExecutor planExecutor, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> extensions, Iterable<? extends PlanTransformer> transformers,
                   PlanExecutionAuthorizer planExecutionAuthorizer, ExecutionPlanCache executionPlanCache)
    {
        this.modelManager = modelManager;
        this.planExecutor = planExecutor;
        this.extensions = extensions;
        this.transformers = transformers;
        this.planExecutionAuthorizer = planExecutionAuthorizer;
        this.executionPlanCache = executionPlanCache;
        MetricsHandler.createMetrics(this.getClass());

    }

    private boolean usePlanCache(HttpServletRequest request, PureModelContext model)
    {
        return request.getHeader(RequestContextHelper.LEGEND_USE_PLAN_CACHE) != null && model instanceof PureModelContextPointer && executionPlanCache != null;
    }

    private static class LambdaWithParameters
    {
        List<ValueSpecification> lambdaBody;
        List<Variable> lambdaParameters;
        Map<String, Object> lambdaParameterMap;
        PureExecutionCacheKey executionCacheKey;

        public LambdaWithParameters(List<ValueSpecification> lambdaExpressions, List<Variable> lambdaParameters, List<ParameterValue> lambdaParameterValues)
        {
            this.lambdaBody = lambdaExpressions;
            this.lambdaParameters = lambdaParameters != null ? lambdaParameters : new ArrayList<>();
            this.lambdaParameterMap = lambdaParameterValues != null ? lambdaParameterValues.stream().collect(Collectors.<ParameterValue, String, Object>toMap(p -> p.name, p -> p.value.accept(new PrimitiveValueSpecificationToObjectVisitor()))) : Maps.mutable.empty();


        }

        public LambdaWithParameters(List<ValueSpecification> lambdaExpressions, List<Variable> lambdaParameters, List<ParameterValue> lambdaParameterValues, Runtime runtime, String mapping, SDLC sdlcInfo) throws JsonProcessingException
        {
            this(lambdaExpressions, lambdaParameters, lambdaParameterValues);
            this.executionCacheKey = new PureExecutionCacheKey(lambdaExpressions, runtime, mapping, sdlcInfo);
        }
    }

    private LambdaWithParameters planCacheLambdaWithParams(LambdaFunction lambda, List<Variable> lambdaParameters, List<ParameterValue> lambdaParameterValues, Runtime runtime, String mapping, SDLC sdlcInfo) throws JsonProcessingException
    {
        ParameterizedValueSpecification cachableValueSpec = new ParameterizedValueSpecification(lambda, "GENERATED");
        List<Variable> allLambdaParameters = lambdaParameters != null ? Stream.concat(lambdaParameters.stream(), cachableValueSpec.getVariables().stream()).collect(Collectors.toList()) : cachableValueSpec.getVariables();
        List<ParameterValue> allLambdaParameterValues = lambdaParameterValues != null ? Stream.concat(lambdaParameterValues.stream(), cachableValueSpec.getParameterValues().stream()).collect(Collectors.toList()) : cachableValueSpec.getParameterValues();
        return new LambdaWithParameters(((LambdaFunction) cachableValueSpec.getValueSpecification()).body, allLambdaParameters, allLambdaParameterValues, runtime, mapping, sdlcInfo);
    }

    @POST
    @ApiOperation(value = "Execute a Pure query (function) in the context of a Mapping and a Runtime. Full Interactive and Semi Interactive modes are supported by giving the appropriate PureModelContext (respectively PureModelDataContext and PureModelContextComposite). Production executions need to use the Service interface.")
    @Path("execute")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    public Response execute(@Context HttpServletRequest request, ExecuteInput executeInput, @DefaultValue(SerializationFormat.defaultFormatString) @QueryParam("serializationFormat") SerializationFormat format, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm, @Context UriInfo uriInfo)
    {
        long start = System.currentTimeMillis();
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        Identity identity = Identity.makeIdentity(profiles);
        try (Scope scope = GlobalTracer.get().buildSpan("Service: Execute").startActive(true))
        {
            String clientVersion = executeInput.clientVersion == null ? PureClientVersions.production : executeInput.clientVersion;

            LambdaWithParameters lwp = usePlanCache(request, executeInput.model) ? planCacheLambdaWithParams(executeInput.function, executeInput.function.parameters, executeInput.parameterValues, executeInput.runtime, executeInput.mapping, ((PureModelContextPointer) executeInput.model).sdlcInfo) : new LambdaWithParameters(executeInput.function.body, executeInput.function.parameters, executeInput.parameterValues);
            Response response = exec(pureModel -> HelperValueSpecificationBuilder.buildLambda(lwp.lambdaBody, lwp.lambdaParameters, pureModel.getContext()),
                    () -> modelManager.loadModel(executeInput.model, clientVersion, identity, null),
                    this.planExecutor,
                    executeInput.mapping,
                    executeInput.runtime,
                    executeInput.context,
                    clientVersion,
                    identity,
                    request.getRemoteUser(),
                    format,
                    lwp.lambdaParameterMap,
                    RequestContextHelper.RequestContext(request), lwp.executionCacheKey);
            if (response.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL))
            {
                MetricsHandler.observeRequest(uriInfo != null ? uriInfo.getPath() : null, start, System.currentTimeMillis());
            }
            return response;
        }
        catch (Exception ex)
        {
            MetricsHandler.observeError(LoggingEventType.PURE_QUERY_EXECUTE_ERROR, ex, null);
            return ExceptionTool.exceptionManager(ex, LoggingEventType.EXECUTE_INTERACTIVE_ERROR, identity.getName());
        }
    }

    @POST
    @Path("generatePlan")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Prometheus(name = "generate plan")
    public Response generatePlan(@Context HttpServletRequest request, ExecuteInput executeInput, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm, @Context UriInfo uriInfo)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        Identity identity = Identity.makeIdentity(profiles);
        long start = System.currentTimeMillis();
        try
        {
            LOGGER.info(new LogInfo(identity.getName(), LoggingEventType.EXECUTION_PLAN_GENERATION_START, "").toString());
            SingleExecutionPlan plan = buildPlan(executeInput, identity, false).plan;
            LOGGER.info(new LogInfo(identity.getName(), LoggingEventType.EXECUTION_PLAN_GENERATION_STOP, (double) System.currentTimeMillis() - start).toString());
            MetricsHandler.observe("generate plan", start, System.currentTimeMillis());
            MetricsHandler.observeRequest(uriInfo != null ? uriInfo.getPath() : null, start, System.currentTimeMillis());
            return Response.ok().type(MediaType.APPLICATION_JSON_TYPE).entity(plan).build();
        }
        catch (Exception ex)
        {
            MetricsHandler.observeError(LoggingEventType.GENERATE_PLAN_ERROR, ex, null);
            Response response = ExceptionTool.exceptionManager(ex, LoggingEventType.EXECUTION_PLAN_GENERATION_ERROR, identity.getName());
            return response;
        }

    }

    @POST
    @Path("generatePlan/debug")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Prometheus(name = "generate plan debug")
    public Response generatePlanDebug(@Context HttpServletRequest request, ExecuteInput executeInput, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm, @Context UriInfo uriInfo)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        Identity identity = Identity.makeIdentity(profiles);
        long start = System.currentTimeMillis();
        try
        {
            LOGGER.info(new LogInfo(identity.getName(), LoggingEventType.EXECUTION_PLAN_GENERATION_DEBUG_START, "").toString());
            PlanWithDebug plan = buildPlan(executeInput, identity, true);
            LOGGER.info(new LogInfo(identity.getName(), LoggingEventType.EXECUTION_PLAN_GENERATION_DEBUG_STOP, (double) System.currentTimeMillis() - start).toString());
            MetricsHandler.observe("generate plan", start, System.currentTimeMillis());
            MetricsHandler.observeRequest(uriInfo != null ? uriInfo.getPath() : null, start, System.currentTimeMillis());
            return Response.ok().type(MediaType.APPLICATION_JSON_TYPE).entity(plan).build();
        }
        catch (Exception ex)
        {
            MetricsHandler.observeError(LoggingEventType.GENERATE_PLAN_ERROR, ex, null);
            Response response = ExceptionTool.exceptionManager(ex, LoggingEventType.EXECUTION_PLAN_GENERATION_DEBUG_ERROR, identity.getName());
            return response;
        }
    }

    private PlanWithDebug buildPlan(ExecuteInput executeInput, Identity identity, boolean debug)
    {
        String clientVersion = executeInput.clientVersion == null ? PureClientVersions.production : executeInput.clientVersion;
        PureModel pureModel = modelManager.loadModel(executeInput.model, clientVersion, identity, null);
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction<?> lambda = HelperValueSpecificationBuilder.buildLambda(executeInput.function.body, executeInput.function.parameters, pureModel.getContext());
        Mapping mapping = executeInput.mapping == null ? null : pureModel.getMapping(executeInput.mapping);
        Root_meta_core_runtime_Runtime runtime = HelperRuntimeBuilder.buildPureRuntime(executeInput.runtime, pureModel.getContext());
        Root_meta_pure_runtime_ExecutionContext context = HelperValueSpecificationBuilder.processExecutionContext(executeInput.context, pureModel.getContext());
        return debug ?
                PlanGenerator.generateExecutionPlanDebug(lambda, mapping, runtime, context, pureModel, clientVersion, PlanPlatform.JAVA, null, this.extensions.apply(pureModel), this.transformers) :
                new PlanWithDebug(PlanGenerator.generateExecutionPlan(lambda, mapping, runtime, context, pureModel, clientVersion, PlanPlatform.JAVA, null, this.extensions.apply(pureModel), this.transformers), "");
    }

    public Response exec(Function<PureModel, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction<?>> functionFunc, Function0<PureModel> pureModelFunc, PlanExecutor planExecutor, String mapping, Runtime runtime, ExecutionContext context, String clientVersion, Identity identity, String user, SerializationFormat format)
    {
        return exec(functionFunc, pureModelFunc, planExecutor, mapping, runtime, context, clientVersion, identity, user, format, Maps.mutable.empty());
    }

    public Response exec(Function<PureModel, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction<?>> functionFunc, Function0<PureModel> pureModelFunc, PlanExecutor planExecutor, String mapping, Runtime runtime, ExecutionContext context, String clientVersion, Identity identity, String user, SerializationFormat format, Map<String, ?> parameters)
    {
        return exec(functionFunc, pureModelFunc, planExecutor, mapping, runtime, context, clientVersion, identity, user, format, parameters, new RequestContext(), null);
    }

    public Response exec(Function<PureModel, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction<?>> functionFunc, Function0<PureModel> pureModelFunc, PlanExecutor planExecutor, String mapping, Runtime runtime, ExecutionContext context, String clientVersion, Identity identity, String user, SerializationFormat format, Map<String, ?> parameters, RequestContext requestContext, PlanCacheKey planCacheKey)
    {
        /*
            planExecutionAuthorizer is used as a feature flag.
            When not set, we switch to a code path that supports only push down executions.
            When set, we switch to a code path that supports both push down and middle tier executions.
         */
        if (this.planExecutionAuthorizer == null)
        {
            return this.execLegacy(functionFunc, pureModelFunc, planExecutor, mapping, runtime, context, clientVersion, identity, user, format, parameters, requestContext, planCacheKey);
        }
        else
        {
            return this.execStrategic(functionFunc, pureModelFunc, planExecutor, mapping, runtime, context, clientVersion, identity, user, format, parameters, requestContext, planCacheKey);
        }
    }


    public Response execLegacy(Function<PureModel, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction<?>> functionFunc, Function0<PureModel> pureModelFunc, PlanExecutor planExecutor, String mapping, Runtime runtime, ExecutionContext context, String clientVersion, Identity identity, String user, SerializationFormat format, Map<String, ?> parameterToValues, RequestContext requestContext, PlanCacheKey planCacheKey)
    {
        try
        {
            long start = System.currentTimeMillis();
            LOGGER.info(new LogInfo(identity.getName(), LoggingEventType.EXECUTE_INTERACTIVE_START, "").toString());
            SingleExecutionPlan plan;
            if (planCacheKey != null && executionPlanCache != null)
            {
                plan = executionPlanCache.getCache().get(planCacheKey, () -> this.buildPlan(functionFunc, pureModelFunc, mapping, runtime, context, clientVersion, identity));
            }
            else
            {
                plan = this.buildPlan(functionFunc, pureModelFunc, mapping, runtime, context, clientVersion, identity);
            }

            MutableMap<String, Result> parametersToConstantResult = Maps.mutable.empty();
            buildParameterToConstantResult(plan, parameterToValues, parametersToConstantResult);
            Result result = planExecutor.execute(plan, parametersToConstantResult, user, identity, null, requestContext);
            LOGGER.info(new LogInfo(identity.getName(), LoggingEventType.EXECUTE_INTERACTIVE_STOP, (double) System.currentTimeMillis() - start).toString());
            MetricsHandler.observe("execute", start, System.currentTimeMillis());
            try (Scope scope = GlobalTracer.get().buildSpan("Manage Results").startActive(true))
            {
                return manageResult(identity.getName(), result, format, LoggingEventType.EXECUTE_INTERACTIVE_ERROR);
            }

        }
        catch (Exception ex)
        {
            MetricsHandler.observeError(LoggingEventType.PURE_QUERY_EXECUTE_ERROR, ex, null);
            Response response = ExceptionTool.exceptionManager(ex, LoggingEventType.EXECUTE_INTERACTIVE_ERROR, identity.getName());
            return response;
        }
    }

    public Response execStrategic(Function<PureModel, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction<?>> functionFunc, Function0<PureModel> pureModelFunc, PlanExecutor planExecutor, String mapping, Runtime runtime, ExecutionContext context, String clientVersion, Identity identity, String user, SerializationFormat format, Map<String, ?> parameters)
    {
        return execStrategic(functionFunc, pureModelFunc, planExecutor, mapping, runtime, context, clientVersion, identity, user, format, parameters, new RequestContext(), null);
    }

    public Response execStrategic(Function<PureModel, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction<?>> functionFunc, Function0<PureModel> pureModelFunc, PlanExecutor planExecutor, String mapping, Runtime runtime, ExecutionContext context, String clientVersion, Identity identity, String user, SerializationFormat format, Map<String, ?> parameters, RequestContext requestContext, PlanCacheKey planCacheKey)
    {
        try
        {
            long start = System.currentTimeMillis();
            LOGGER.info(new LogInfo(identity.getName(), LoggingEventType.EXECUTE_INTERACTIVE_START, "").toString());
            SingleExecutionPlan singleExecutionPlan;
            if (planCacheKey != null && executionPlanCache != null)
            {
                singleExecutionPlan = executionPlanCache.getCache().get(planCacheKey, () -> this.buildPlan(functionFunc, pureModelFunc, mapping, runtime, context, clientVersion, identity));

            }
            else
            {
                singleExecutionPlan = this.buildPlan(functionFunc, pureModelFunc, mapping, runtime, context, clientVersion, identity);
            }
            return this.execImpl(planExecutor, identity, user, format, start, singleExecutionPlan, parameters, requestContext);
        }
        catch (Exception ex)
        {
            MetricsHandler.observeError(LoggingEventType.PURE_QUERY_EXECUTE_ERROR, ex, null);
            Response response = ExceptionTool.exceptionManager(ex, LoggingEventType.EXECUTE_INTERACTIVE_ERROR, identity.getName());
            return response;
        }
    }

    private Response execImpl(PlanExecutor planExecutor, Identity identity, String user, SerializationFormat format, long start, SingleExecutionPlan plan, Map<String, ?> parameters, RequestContext requestContext) throws Exception
    {
        // Authorizer has not been configured. So we execute the plan with the default push down authorization behavior.
        if (planExecutionAuthorizer == null)
        {
            return this.executeAsPushDownPlan(planExecutor, identity, user, format, start, plan, parameters, requestContext);
        }

        // Plan does not make use of middle tier connections. So we execute the plan with the default push down authorization behavior.
        if (!this.planExecutionAuthorizer.isMiddleTierPlan(plan))
        {
            return this.executeAsPushDownPlan(planExecutor, identity, user, format, start, plan, parameters, requestContext);
        }

        // Plan makes use of middle tier connections. So we check for authorization.
        PlanExecutionAuthorizerOutput authorizationResult = this.authorizePlan(identity, plan);

        // Plan failed authorization.
        if (!authorizationResult.isAuthorized())
        {
            LOGGER.info(new LogInfo(identity.getName(), LoggingEventType.MIDDLETIER_INTERACTIVE_EXECUTION, "Plan failed middle tier authorization").toString());
            Response response = ExceptionTool.exceptionManager(authorizationResult.toJSON(), 403, LoggingEventType.MIDDLETIER_INTERACTIVE_EXECUTION, identity.getName());
            return response;
        }

        // Plan passed authorization. Now we can execute it
        return this.executeAsMiddleTierPlan(planExecutor, identity, user, format, start, authorizationResult.getTransformedPlan(), parameters, requestContext);
    }

    private SingleExecutionPlan buildPlan(Function<PureModel, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction<?>> functionFunc, Function0<PureModel> pureModelFunc, String mapping, Runtime runtime, ExecutionContext context, String clientVersion, Identity identity)
    {

        PureModel pureModel = pureModelFunc.value();
        SingleExecutionPlan plan = PlanGenerator.generateExecutionPlanWithTrace(functionFunc.valueOf(pureModel),
                mapping == null ? null : pureModel.getMapping(mapping),
                HelperRuntimeBuilder.buildPureRuntime(runtime, pureModel.getContext()),
                HelperValueSpecificationBuilder.processExecutionContext(context, pureModel.getContext()),
                pureModel,
                clientVersion,
                PlanPlatform.JAVA,
                identity,
                this.extensions.apply(pureModel),
                this.transformers
        );
        return plan;
    }

    private PlanExecutionAuthorizerOutput authorizePlan(Identity identity, SingleExecutionPlan plan) throws Exception
    {

        // Note : For interactive executions we do not have to provide a resource context. The resource context is derived from the plan
        PlanExecutionAuthorizerInput authorizationInput = PlanExecutionAuthorizerInput
                .with(INTERACTIVE_EXECUTION)
                .build();

        PlanExecutionAuthorizerOutput executionAuthorization = this.planExecutionAuthorizer.evaluate(identity, plan, authorizationInput);

        try (Scope scope = GlobalTracer.get().buildSpan("Authorize Plan Execution").startActive(true))
        {
            String executionAuthorizationJSON = executionAuthorization.toPrettyJSON();
            scope.span().setTag("plan authorization", executionAuthorizationJSON);
        }

        LOGGER.info(new LogInfo(identity.getName(), LoggingEventType.MIDDLETIER_INTERACTIVE_EXECUTION, String.format("Middle tier plan execution authorization result = %s", executionAuthorization.toJSON())).toString());
        return executionAuthorization;
    }

    private Response executeAsPushDownPlan(PlanExecutor planExecutor, Identity identity, String user, SerializationFormat format, long start, SingleExecutionPlan plan, Map<String, ?> parameterToValues, RequestContext requestContext)
    {
        MutableMap<String, Result> parametersToConstantResult = Maps.mutable.empty();
        buildParameterToConstantResult(plan, parameterToValues, parametersToConstantResult);
        Result result = planExecutor.execute(plan, parametersToConstantResult, user, identity, null, requestContext);
        return this.wrapInResponse(identity, format, start, result);
    }

    private Response executeAsMiddleTierPlan(PlanExecutor planExecutor, Identity identity, String user, SerializationFormat format, long start, ExecutionPlan plan, Map<String, ?> parameterToValues, RequestContext requestContext)
    {
        StoreExecutionState.RuntimeContext runtimeContext = StoreExecutionState.newRuntimeContext(
                Maps.immutable.with(
                        PlanExecutionAuthorizerInput.USAGE_CONTEXT_PARAM, INTERACTIVE_EXECUTION.name(),
                        PlanExecutionAuthorizerInput.RESOURCE_CONTEXT_PARAM, "reserved-for-future-use"
                )
        );
        MutableMap<String, Result> parametersToConstantResult = Maps.mutable.empty();
        buildParameterToConstantResult((SingleExecutionPlan) plan, parameterToValues, parametersToConstantResult);
        PlanExecutor.ExecuteArgs executeArgs = PlanExecutor.withArgs()
                .withPlan(plan)
                .withUser(user)
                .withIdentity(identity)
                .withParamsAsResults(parametersToConstantResult)
                .withStoreRuntimeContext(StoreType.Relational, runtimeContext)
                .withRequestContext(requestContext)
                .build();

        String logMessage = String.format("Middle tier interactive execution invoked with custom runtime context. Context=%s", runtimeContext.getContextParams());
        LOGGER.info(new LogInfo(identity.getName(), LoggingEventType.MIDDLETIER_INTERACTIVE_EXECUTION, logMessage).toString());

        Result result = planExecutor.executeWithArgs(executeArgs);
        return this.wrapInResponse(identity, format, start, result);
    }


    private void updateParametersWithValues(Map<String, Object> parameters, List<ParameterValue> inputValues)
    {
        for (ParameterValue parameterValue : inputValues)
        {
            parameters.put(parameterValue.name, parameterValue.value.accept(new PrimitiveValueSpecificationToObjectVisitor()));
        }
    }

    private Response wrapInResponse(Identity identity, SerializationFormat format, long start, Result result)
    {
        LOGGER.info(new LogInfo(identity.getName(), LoggingEventType.EXECUTE_INTERACTIVE_STOP, (double) System.currentTimeMillis() - start).toString());
        MetricsHandler.observe("execute", start, System.currentTimeMillis());
        try (Scope scope = GlobalTracer.get().buildSpan("Manage Results").startActive(true))
        {
            return manageResult(identity.getName(), result, format, LoggingEventType.EXECUTE_INTERACTIVE_ERROR);
        }
    }
}
