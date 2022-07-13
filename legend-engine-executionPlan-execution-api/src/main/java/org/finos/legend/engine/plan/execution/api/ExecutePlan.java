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
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.api.result.ResultManager;
import org.finos.legend.engine.plan.execution.authorization.PlanExecutionAuthorizer;
import org.finos.legend.engine.plan.execution.authorization.PlanExecutionAuthorizerInput;
import org.finos.legend.engine.plan.execution.authorization.PlanExecutionAuthorizerOutput;
import org.finos.legend.engine.plan.execution.authorization.mac.PlanExecutionAuthorizerMACUtils;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.execution.stores.StoreExecutionState;
import org.finos.legend.engine.plan.execution.stores.StoreType;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.ExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.DefaultIdentityFactory;
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

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.finos.legend.engine.plan.execution.api.result.ResultManager.manageResult;
import static org.finos.legend.engine.plan.execution.authorization.PlanExecutionAuthorizerInput.ExecutionMode.INTERACTIVE_EXECUTION;

public class ExecutePlan
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");
    private final PlanExecutor planExecutor;
    private IdentityFactory identityFactory;
    private final PlanExecutionAuthorizer planExecutionAuthorizer;
    private final String middleTierAuthorizationMACKeyVaultReference;

    public ExecutePlan(PlanExecutor planExecutor)
    {
        this(planExecutor, null, null, new DefaultIdentityFactory());
    }

    public ExecutePlan(PlanExecutor planExecutor, PlanExecutionAuthorizer planExecutionAuthorizer, String middleTierAuthorizationMACKeyVaultReference, IdentityFactory identityFactory)
    {
        this.planExecutor = planExecutor;
        this.identityFactory = identityFactory;
        if (planExecutionAuthorizer != null && middleTierAuthorizationMACKeyVaultReference == null)
        {
            throw new IllegalArgumentException("Invalid arguments. Plan authorizer is not null but MAC key vault reference is null");
        }
        this.planExecutionAuthorizer = planExecutionAuthorizer;
        this.middleTierAuthorizationMACKeyVaultReference = middleTierAuthorizationMACKeyVaultReference;
    }

    public Response doExecutePlan(@Context HttpServletRequest request, ExecutionPlan execPlan, @DefaultValue(SerializationFormat.defaultFormatString) @QueryParam("serializationFormat") SerializationFormat format, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        try
        {
            if (execPlan instanceof SingleExecutionPlan)
            {
                return doExecutePlanImpl((SingleExecutionPlan) execPlan, format, profiles);
            }
            else
            {
                return Response.status(500).type(MediaType.TEXT_PLAIN).entity(new ResultManager.ErrorMessage(20, "Only SingleExecutionPlan is supported")).build();
            }
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, LoggingEventType.EXECUTION_PLAN_EXEC_ERROR, profiles);
        }
    }

    Response doExecutePlanImpl(SingleExecutionPlan execPlan, SerializationFormat format, MutableList<CommonProfile> profiles) throws Exception
    {
        long start = System.currentTimeMillis();
        LOGGER.info(new LogInfo(profiles, LoggingEventType.EXECUTION_PLAN_EXEC_START, "").toString());
        Response response = execImpl(execPlan, profiles, format, start);
        LOGGER.info(new LogInfo(profiles, LoggingEventType.EXECUTION_PLAN_EXEC_STOP, "").toString());
        return response;
    }

    private Response execImpl(SingleExecutionPlan execPlan, MutableList<CommonProfile> profiles, SerializationFormat format, long start) throws Exception
    {
        // Authorizer has not been configured. So we execute the plan with the default push down authorization behavior.
        if (planExecutionAuthorizer == null)
        {
            return this.executeAsPushDownPlan(planExecutor, execPlan, profiles, format, start);
        }

        // Plan does not make use of middle tier connections. So we execute the plan with the default push down authorization behavior.
        if (!this.planExecutionAuthorizer.isMiddleTierPlan(execPlan))
        {
            return this.executeAsPushDownPlan(planExecutor, execPlan, profiles, format, start);
        }

        // Plan makes use of middle tier connections. So we check for authorization.
        PlanExecutionAuthorizerOutput authorizationResult = this.authorizePlan(profiles, execPlan);

        // Plan failed authorization.
        if (!authorizationResult.isAuthorized())
        {
            MetricsHandler.observeError("execute");
            LOGGER.info(new LogInfo(profiles, LoggingEventType.MIDDLETIER_INTERACTIVE_EXECUTION, "Plan failed middle tier authorization").toString());
            Response response = ExceptionTool.exceptionManager(authorizationResult.toJSON(), 403, LoggingEventType.MIDDLETIER_INTERACTIVE_EXECUTION, profiles);
            MetricsHandler.incrementErrorCount("pure/v1/execution/execute", response.getStatus());
            return response;
        }

        // Plan passed authorization. Now we can execute it
        return this.executeAsMiddleTierPlan(planExecutor, execPlan, profiles, format, start);
    }

    private Response executeAsPushDownPlan(PlanExecutor planExecutor, SingleExecutionPlan execPlan, MutableList<CommonProfile> profiles, SerializationFormat format, long start) throws Exception
    {
        // Assume that the input exec plan has no variables
        PlanExecutor.ExecuteArgs executeArgs = PlanExecutor.withArgs()
                .withPlan(execPlan)
                .withProfiles(profiles)
                .build();

        Result result = planExecutor.execute(executeArgs);
        return this.wrapInResponse(profiles, format, start, result);
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

    private Response executeAsMiddleTierPlan(PlanExecutor planExecutor, SingleExecutionPlan execPlan, MutableList<CommonProfile> profiles, SerializationFormat format, long start) throws Exception
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
                .withPlan(execPlan)
                .withProfiles(profiles)
                .withStoreRuntimeContext(StoreType.Relational, runtimeContext)
                .build();

        String logMessage = String.format("Middle tier interactive execution invoked with custom runtime context. Context=%s", runtimeContext.getContextParams());
        LOGGER.info(new LogInfo(profiles, LoggingEventType.MIDDLETIER_INTERACTIVE_EXECUTION, logMessage).toString());

        Result result = planExecutor.execute(executeArgs);
        return this.wrapInResponse(profiles, format, start, result);
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