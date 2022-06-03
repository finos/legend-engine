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
import io.swagger.annotations.ApiParam;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperRuntimeBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.authorization.PlanExecutionAuthorizer;
import org.finos.legend.engine.plan.execution.authorization.PlanExecutionAuthorizerInput;
import org.finos.legend.engine.plan.execution.authorization.PlanExecutionAuthorizerOutput;
import org.finos.legend.engine.plan.execution.authorization.mac.PlanExecutionAuthorizerMACUtils;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.execution.stores.StoreExecutionState;
import org.finos.legend.engine.plan.execution.stores.StoreType;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.PlanWithDebug;
import org.finos.legend.engine.plan.generation.transformers.PlanTransformer;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.ExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.Runtime;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.Variable;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.executionContext.ExecutionContext;
import org.finos.legend.engine.shared.core.api.model.ExecuteInput;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.DefaultIdentityFactory;
import org.finos.legend.engine.shared.core.identity.factory.IdentityFactory;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.finos.legend.engine.shared.core.operational.prometheus.MetricsHandler;
import org.finos.legend.engine.shared.core.operational.prometheus.Prometheus;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
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
import javax.ws.rs.core.UriInfo;

import static org.finos.legend.engine.plan.execution.api.result.ResultManager.manageResult;
import static org.finos.legend.engine.plan.execution.authorization.PlanExecutionAuthorizerInput.ExecutionMode.INTERACTIVE_EXECUTION;
import static org.finos.legend.engine.shared.core.operational.http.InflateInterceptor.APPLICATION_ZLIB;

@Api(tags = "Pure - Execution")
@Path("pure/v1/execution")
@Produces(MediaType.APPLICATION_JSON)
public class Execute
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");
    private final ModelManager modelManager;
    private final PlanExecutor planExecutor;
    private Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> extensions;
    private MutableList<PlanTransformer> transformers;
    private PlanExecutionAuthorizer planExecutionAuthorizer;
    private IdentityFactory identityFactory;
    private String middleTierAuthorizationMACKeyVaultReference;


    public Execute(ModelManager modelManager, PlanExecutor planExecutor, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> extensions, MutableList<PlanTransformer> transformers)
    {
        this(modelManager, planExecutor, extensions, transformers, null, null, new DefaultIdentityFactory());
    }

    public Execute(ModelManager modelManager, PlanExecutor planExecutor, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> extensions, MutableList<PlanTransformer> transformers,
                   PlanExecutionAuthorizer planExecutionAuthorizer, String middleTierAuthorizationMACKeyVaultReference, IdentityFactory identityFactory)
    {
        this.modelManager = modelManager;
        this.planExecutor = planExecutor;
        this.extensions = extensions;
        this.transformers = transformers;
        this.identityFactory = identityFactory;
        if (planExecutionAuthorizer != null && middleTierAuthorizationMACKeyVaultReference == null)
        {
            throw new IllegalArgumentException("Invalid arguments. Plan authorizer is not null but MAC key vault reference is null");
        }
        this.planExecutionAuthorizer = planExecutionAuthorizer;
        this.middleTierAuthorizationMACKeyVaultReference = middleTierAuthorizationMACKeyVaultReference;
        MetricsHandler.createMetrics(this.getClass());
    }

    @POST
    @ApiOperation(value = "Execute a Pure query (function) in the context of a Mapping and a Runtime. Full Interactive and Semi Interactive modes are supported by giving the appropriate PureModelContext (respectively PureModelDataContext and PureModelContextComposite). Production executions need to use the Service interface.")
    @Path("execute")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    public Response execute(@Context HttpServletRequest request, ExecuteInput executeInput, @DefaultValue(SerializationFormat.defaultFormatString) @QueryParam("serializationFormat") SerializationFormat format, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm, @Context UriInfo uriInfo)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        long start = System.currentTimeMillis();
        try (Scope scope = GlobalTracer.get().buildSpan("Service: Execute").startActive(true))
        {
            String clientVersion = executeInput.clientVersion == null ? PureClientVersions.production : executeInput.clientVersion;
            Response response = exec(pureModel -> HelperValueSpecificationBuilder.buildLambda(executeInput.function.body, Lists.fixedSize.<Variable>empty(), pureModel.getContext()),
                    () -> modelManager.loadModel(executeInput.model, clientVersion, profiles, null),
                    this.planExecutor,
                    executeInput.mapping,
                    executeInput.runtime,
                    executeInput.context,
                    clientVersion,
                    profiles, request.getRemoteUser(), format);
            if (response.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL))
            {
                MetricsHandler.observeRequest(uriInfo != null ? uriInfo.getPath() : null, start, System.currentTimeMillis());
            }
            return response;
        }
        catch (Exception ex)
        {
            Response response = ExceptionTool.exceptionManager(ex, LoggingEventType.EXECUTE_INTERACTIVE_ERROR, profiles);
            MetricsHandler.incrementErrorCount(uriInfo != null ? uriInfo.getPath() : null, response.getStatus());
            return response;
        }
    }

    @POST
    @Path("generatePlan")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Prometheus(name = "generate plan")
    public Response generatePlan(@Context HttpServletRequest request, ExecuteInput executeInput, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm, @Context UriInfo uriInfo)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        long start = System.currentTimeMillis();
        try
        {
            LOGGER.info(new LogInfo(profiles, LoggingEventType.EXECUTION_PLAN_GENERATION_START, "").toString());
            SingleExecutionPlan plan = buildPlan(executeInput, profiles, false).plan;
            LOGGER.info(new LogInfo(profiles, LoggingEventType.EXECUTION_PLAN_GENERATION_STOP, (double) System.currentTimeMillis() - start).toString());
            MetricsHandler.observe("generate plan", start, System.currentTimeMillis());
            MetricsHandler.observeRequest(uriInfo != null ? uriInfo.getPath() : null, start, System.currentTimeMillis());
            return Response.ok().type(MediaType.APPLICATION_JSON_TYPE).entity(plan).build();
        }
        catch (Exception ex)
        {
            MetricsHandler.observeError("generate plan");
            Response response = ExceptionTool.exceptionManager(ex, LoggingEventType.EXECUTION_PLAN_GENERATION_ERROR, profiles);
            MetricsHandler.incrementErrorCount(uriInfo != null ? uriInfo.getPath() : null, response.getStatus());
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
        long start = System.currentTimeMillis();
        try
        {
            LOGGER.info(new LogInfo(profiles, LoggingEventType.EXECUTION_PLAN_GENERATION_DEBUG_START, "").toString());
            PlanWithDebug plan = buildPlan(executeInput, profiles, true);
            LOGGER.info(new LogInfo(profiles, LoggingEventType.EXECUTION_PLAN_GENERATION_DEBUG_STOP, (double) System.currentTimeMillis() - start).toString());
            MetricsHandler.observe("generate plan", start, System.currentTimeMillis());
            MetricsHandler.observeRequest(uriInfo != null ? uriInfo.getPath() : null, start, System.currentTimeMillis());
            return Response.ok().type(MediaType.APPLICATION_JSON_TYPE).entity(plan).build();
        }
        catch (Exception ex)
        {
            MetricsHandler.observeError("generate plan");
            Response response = ExceptionTool.exceptionManager(ex, LoggingEventType.EXECUTION_PLAN_GENERATION_DEBUG_ERROR, profiles);
            MetricsHandler.incrementErrorCount(uriInfo != null ? uriInfo.getPath() : null, response.getStatus());
            return response;
        }
    }

    private PlanWithDebug buildPlan(ExecuteInput executeInput, MutableList<CommonProfile> profiles, boolean debug)
    {
        String clientVersion = executeInput.clientVersion == null ? PureClientVersions.production : executeInput.clientVersion;
        PureModel pureModel = modelManager.loadModel(executeInput.model, clientVersion, profiles, null);
        LambdaFunction<?> lambda = HelperValueSpecificationBuilder.buildLambda(executeInput.function.body, executeInput.function.parameters, pureModel.getContext());
        Mapping mapping = executeInput.mapping == null ? null : pureModel.getMapping(executeInput.mapping);
        org.finos.legend.pure.m3.coreinstance.meta.pure.runtime.Runtime runtime = HelperRuntimeBuilder.buildPureRuntime(executeInput.runtime, pureModel.getContext());
        org.finos.legend.pure.m3.coreinstance.meta.pure.runtime.ExecutionContext context = HelperValueSpecificationBuilder.processExecutionContext(executeInput.context, pureModel.getContext());
        return debug ?
                PlanGenerator.generateExecutionPlanDebug(lambda, mapping, runtime, context, pureModel, clientVersion, PlanPlatform.JAVA, null, this.extensions.apply(pureModel), this.transformers) :
                new PlanWithDebug(PlanGenerator.generateExecutionPlan(lambda, mapping, runtime, context, pureModel, clientVersion, PlanPlatform.JAVA, null, this.extensions.apply(pureModel), this.transformers), "");
    }

    public Response exec(Function<PureModel, LambdaFunction<?>> functionFunc, Function0<PureModel> pureModelFunc, PlanExecutor planExecutor, String mapping, Runtime runtime, ExecutionContext context, String clientVersion, MutableList<CommonProfile> pm, String user, SerializationFormat format)
    {
        try
        {
            long start = System.currentTimeMillis();
            LOGGER.info(new LogInfo(pm, LoggingEventType.EXECUTE_INTERACTIVE_START, "").toString());
            SingleExecutionPlan plan = this.buildPlan(functionFunc, pureModelFunc, mapping, runtime, context, clientVersion, pm);
            return this.execImpl(planExecutor, pm, user, format, start, plan);
        }
        catch (Exception ex)
        {
            MetricsHandler.observeError("execute");
            Response response = ExceptionTool.exceptionManager(ex, LoggingEventType.EXECUTE_INTERACTIVE_ERROR, pm);
            MetricsHandler.incrementErrorCount("pure/v1/execution/execute", response.getStatus());
            return response;
        }
    }

    private Response execImpl(PlanExecutor planExecutor, MutableList<CommonProfile> pm, String user, SerializationFormat format, long start, SingleExecutionPlan plan) throws Exception
    {
        // Authorizer has not been configured. So we execute the plan with the default push down authorization behavior.
        if (planExecutionAuthorizer == null)
        {
            return this.executeAsPushDownPlan(planExecutor, pm, user, format, start, plan);
        }

        // Plan does not make use of middle tier connections. So we execute the plan with the default push down authorization behavior.
        if (!this.planExecutionAuthorizer.isMiddleTierPlan(plan))
        {
            return this.executeAsPushDownPlan(planExecutor, pm, user, format, start, plan);
        }

        // Plan makes use of middle tier connections. So we check for authorization.
        PlanExecutionAuthorizerOutput authorizationResult = this.authorizePlan(pm, plan);

        // Plan failed authorization.
        if (!authorizationResult.isAuthorized())
        {
            MetricsHandler.observeError("execute");
            LOGGER.info(new LogInfo(pm, LoggingEventType.MIDDLETIER_INTERACTIVE_EXECUTION, "Plan failed middle tier authorization").toString());
            Response response = ExceptionTool.exceptionManager(authorizationResult.toJSON(), 403, LoggingEventType.MIDDLETIER_INTERACTIVE_EXECUTION, pm);
            MetricsHandler.incrementErrorCount("pure/v1/execution/execute", response.getStatus());
            return response;
        }

        // Plan passed authorization. Now we can execute it
        return this.executeAsMiddleTierPlan(planExecutor, pm, user, format, start, authorizationResult.getTransformedPlan());
    }

    private SingleExecutionPlan buildPlan(Function<PureModel, LambdaFunction<?>> functionFunc, Function0<PureModel> pureModelFunc, String mapping, Runtime runtime, ExecutionContext context, String clientVersion, MutableList<CommonProfile> pm)
    {
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
        return plan;
    }

    private PlanExecutionAuthorizerOutput authorizePlan(MutableList<CommonProfile> pm, SingleExecutionPlan plan) throws Exception
    {
        Identity identity = this.identityFactory.makeIdentity(pm);

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

        LOGGER.info(new LogInfo(pm, LoggingEventType.MIDDLETIER_INTERACTIVE_EXECUTION, String.format("Middle tier plan execution authorization result = %s", executionAuthorization.toJSON())).toString());
        return executionAuthorization;
    }

    private Response executeAsPushDownPlan(PlanExecutor planExecutor, MutableList<CommonProfile> pm, String user, SerializationFormat format, long start, SingleExecutionPlan plan) throws Exception
    {
        PlanExecutor.ExecuteArgs executeArgs = PlanExecutor.withArgs()
                .withPlan(plan)
                .withUser(user)
                .withProfiles(pm)
                .build();

        Result result = planExecutor.execute(executeArgs);
        return this.wrapInResponse(pm, format, start, result);
    }

    private Response executeAsMiddleTierPlan(PlanExecutor planExecutor, MutableList<CommonProfile> pm, String user, SerializationFormat format, long start, ExecutionPlan plan) throws Exception
    {
        String generatedMAC = new PlanExecutionAuthorizerMACUtils().generateMAC("Plan execution authorization completed", this.middleTierAuthorizationMACKeyVaultReference);
        StoreExecutionState.RuntimeContext runtimeContext = StoreExecutionState.newRuntimeContext(
                Maps.immutable.with(
                        PlanExecutionAuthorizerInput.USAGE_CONTEXT_PARAM, INTERACTIVE_EXECUTION.name(),
                        PlanExecutionAuthorizerInput.RESOURCE_CONTEXT_PARAM, "reserved-for-future-use",
                        PlanExecutionAuthorizerInput.MAC_CONTEXT_PARAM, generatedMAC
                )
        );

        PlanExecutor.ExecuteArgs executeArgs = PlanExecutor.withArgs()
                .withPlan(plan)
                .withUser(user)
                .withProfiles(pm)
                .withStoreRuntimeContext(StoreType.Relational, runtimeContext)
                .build();

        String logMessage = String.format("Middle tier interactive execution invoked with custom runtime context. Context=%s", runtimeContext.getContextParams());
        LOGGER.info(new LogInfo(pm, LoggingEventType.MIDDLETIER_INTERACTIVE_EXECUTION, logMessage).toString());

        Result result = planExecutor.execute(executeArgs);
        return this.wrapInResponse(pm, format, start, result);
    }

    private Response wrapInResponse(MutableList<CommonProfile> pm, SerializationFormat format, long start, Result result)
    {
        LOGGER.info(new LogInfo(pm, LoggingEventType.EXECUTE_INTERACTIVE_STOP, (double) System.currentTimeMillis() - start).toString());
        MetricsHandler.observe("execute", start, System.currentTimeMillis());
        try (Scope scope = GlobalTracer.get().buildSpan("Manage Results").startActive(true))
        {
            return manageResult(pm, result, format, LoggingEventType.EXECUTE_INTERACTIVE_ERROR);
        }
    }
}
