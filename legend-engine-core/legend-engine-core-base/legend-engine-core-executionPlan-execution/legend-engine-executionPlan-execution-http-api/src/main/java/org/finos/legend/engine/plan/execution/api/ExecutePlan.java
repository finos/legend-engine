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

package org.finos.legend.engine.plan.execution.api;

import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import io.swagger.annotations.ApiParam;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.api.request.ExecutionRequest;
import org.finos.legend.engine.plan.execution.api.request.RequestContextHelper;
import org.finos.legend.engine.plan.execution.api.result.ResultManager;
import org.finos.legend.engine.plan.execution.authorization.PlanExecutionAuthorizer;
import org.finos.legend.engine.plan.execution.authorization.PlanExecutionAuthorizerInput;
import org.finos.legend.engine.plan.execution.authorization.PlanExecutionAuthorizerOutput;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.execution.stores.StoreExecutionState;
import org.finos.legend.engine.plan.execution.stores.StoreType;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.IdentityFactory;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.finos.legend.engine.shared.core.operational.prometheus.MetricsHandler;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;
import org.slf4j.Logger;

import static org.finos.legend.engine.plan.execution.api.result.ResultManager.manageResult;
import static org.finos.legend.engine.plan.execution.authorization.PlanExecutionAuthorizerInput.ExecutionMode.INTERACTIVE_EXECUTION;

public class ExecutePlan
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ExecutePlan.class);
    private final PlanExecutor planExecutor;
    private final PlanExecutionAuthorizer planExecutionAuthorizer;

    public ExecutePlan(PlanExecutor planExecutor)
    {
        this(planExecutor, null);
    }

    public ExecutePlan(PlanExecutor planExecutor, PlanExecutionAuthorizer planExecutionAuthorizer)
    {
        this.planExecutor = planExecutor;
        this.planExecutionAuthorizer = planExecutionAuthorizer;
    }

    public Response doExecutePlan(@Context HttpServletRequest request, ExecutionRequest executionRequest, @DefaultValue(SerializationFormat.defaultFormatString) @QueryParam("serializationFormat") SerializationFormat format, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        /*
            planExecutionAuthorizer is used as a feature flag to gradually introduce middle tier authorization into the execution flow.
            When configured, we switch to a code path that can handle both middle tier and push down executions.
            When not configured, we switch to a code path that can handle only push down executions.
         */

        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        Identity identity = Identity.makeIdentity(profiles);
        if (this.planExecutionAuthorizer == null)
        {
            return this.doExecutePlanLegacy(request, executionRequest, format, identity);
        }
        else
        {
            return this.doExecutePlanImpl(executionRequest, format, identity);
        }
    }

    public Response doExecutePlanLegacy(HttpServletRequest request, ExecutionRequest executionRequest, SerializationFormat format, Identity identity)
    {
        try
        {
            LOGGER.info(new LogInfo(identity.getName(), LoggingEventType.EXECUTION_PLAN_EXEC_START, "").toString());
            // Assume that the input exec plan has no variables
            Result result = planExecutor.execute(executionRequest.getSingleExecutionPlan(), executionRequest.getExecutionParametersAsResult(), null, identity, null, RequestContextHelper.RequestContext(request));
            try (Scope scope = GlobalTracer.get().buildSpan("Manage Results").startActive(true))
            {
                LOGGER.info(new LogInfo(identity.getName(), LoggingEventType.EXECUTION_PLAN_EXEC_STOP, "").toString());
                return ResultManager.manageResult(identity.getName(), result, format, LoggingEventType.EXECUTION_PLAN_EXEC_ERROR);
            }
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, LoggingEventType.EXECUTION_PLAN_EXEC_ERROR, identity.getName());
        }
    }

    public Response doExecutePlanImpl(ExecutionRequest executionRequest, SerializationFormat format, Identity identity)
    {
        long start = System.currentTimeMillis();
        try
        {
            LOGGER.info(new LogInfo(identity.getName(), LoggingEventType.EXECUTION_PLAN_EXEC_START, "").toString());
            Response response = execImpl(executionRequest, identity, format, start);
            LOGGER.info(new LogInfo(identity.getName(), LoggingEventType.EXECUTION_PLAN_EXEC_STOP, "").toString());
            return response;
        }
        catch (Exception ex)
        {
            MetricsHandler.observeError(LoggingEventType.EXECUTION_PLAN_EXEC_ERROR, ex, null);
            return ExceptionTool.exceptionManager(ex, LoggingEventType.EXECUTE_INTERACTIVE_ERROR, identity.getName());
        }
    }

    private Response execImpl(ExecutionRequest executionRequest, Identity identity, SerializationFormat format, long start) throws Exception
    {
        // Authorizer has not been configured. So we execute the plan with the default push down authorization behavior.
        // or plan does not make use of middle tier connections. So we execute the plan with the default push down authorization behavior.
        if (planExecutionAuthorizer == null || !this.planExecutionAuthorizer.isMiddleTierPlan(executionRequest.getExecutionPlan()))
        {
            return this.executeAsPushDownPlan(planExecutor, executionRequest, identity, format, start);
        }

        // Plan makes use of middle tier connections. So we check for authorization.
        PlanExecutionAuthorizerOutput authorizationResult = this.authorizePlan(identity, executionRequest.getSingleExecutionPlan());

        // Plan failed authorization.
        if (!authorizationResult.isAuthorized())
        {
            LOGGER.info(new LogInfo(identity.getName(), LoggingEventType.MIDDLETIER_INTERACTIVE_EXECUTION, "Plan failed middle tier authorization").toString());
            Response response = ExceptionTool.exceptionManager(authorizationResult.toJSON(), 403, LoggingEventType.MIDDLETIER_INTERACTIVE_EXECUTION, identity.getName());
            return response;
        }

        // Plan passed authorization. Now we can execute it
        return this.executeAsMiddleTierPlan(planExecutor, executionRequest, identity, format, start);
    }

    private Response executeAsPushDownPlan(PlanExecutor planExecutor, ExecutionRequest executionRequest, Identity identity, SerializationFormat format, long start)
    {
        Result result = planExecutor.execute(executionRequest.getSingleExecutionPlan(), executionRequest.getExecutionParametersAsResult(), null, identity);
        return this.wrapInResponse(identity, format, start, result);
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

    private Response executeAsMiddleTierPlan(PlanExecutor planExecutor, ExecutionRequest executionRequest, Identity identity, SerializationFormat format, long start)
    {
        StoreExecutionState.RuntimeContext runtimeContext = StoreExecutionState.newRuntimeContext(
                Maps.immutable.with(
                        PlanExecutionAuthorizerInput.USAGE_CONTEXT_PARAM, INTERACTIVE_EXECUTION.name(),
                        PlanExecutionAuthorizerInput.RESOURCE_CONTEXT_PARAM, "reserved-for-future-use"
                )
        );
        PlanExecutor.ExecuteArgs executeArgs = PlanExecutor.withArgs()
                .withPlan(executionRequest.getExecutionPlan())
                .withParams(executionRequest.getExecutionParameters())
                .withIdentity(identity)
                .withStoreRuntimeContext(StoreType.Relational, runtimeContext)
                .build();

        String logMessage = String.format("Middle tier interactive execution invoked with custom runtime context. Context=%s", runtimeContext.getContextParams());
        LOGGER.info(new LogInfo(identity.getName(), "MIDDLETIER_INTERACTIVE_EXECUTION", logMessage).toString());

        Result result = planExecutor.executeWithArgs(executeArgs);
        return this.wrapInResponse(identity, format, start, result);
    }

    private Response wrapInResponse(Identity identity, SerializationFormat format, long start, Result result)
    {
        LOGGER.info(new LogInfo(identity.getName(), LoggingEventType.EXECUTE_INTERACTIVE_STOP, (double) System.currentTimeMillis() - start).toString());
        try (Scope scope = GlobalTracer.get().buildSpan("Manage Results").startActive(true))
        {
            return manageResult(identity.getName(), result, format, LoggingEventType.EXECUTE_INTERACTIVE_ERROR);
        }
    }
}