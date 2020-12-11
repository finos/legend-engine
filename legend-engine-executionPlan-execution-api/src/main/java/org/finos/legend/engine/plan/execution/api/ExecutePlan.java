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
import io.swagger.annotations.Api;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.api.result.ResultManager;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.ExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;
import org.slf4j.Logger;

import javax.security.auth.Subject;
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

import static org.finos.legend.engine.shared.core.operational.http.InflateInterceptor.APPLICATION_ZLIB;

@Api(tags = "Pure - Execution")
@Path("pure/v1/execution")
@Produces(MediaType.APPLICATION_JSON)
public class ExecutePlan
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");
    private final PlanExecutor planExecutor;

    public ExecutePlan(PlanExecutor planExecutor)
    {
        this.planExecutor = planExecutor;
    }

    @POST
    @Path("executePlan")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    public Response executePlan(@Context HttpServletRequest request, ExecutionPlan execPlan, @DefaultValue(SerializationFormat.defaultFormatString) @QueryParam("serializationFormat") SerializationFormat format, @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        try
        {
            if (execPlan instanceof SingleExecutionPlan)
            {
                LOGGER.info(new LogInfo(pm, LoggingEventType.EXECUTION_PLAN_EXEC_START, "").toString());
                // Assume that the input exec plan has no variables
                Result result = planExecutor.execute((SingleExecutionPlan) execPlan, Maps.mutable.empty(), null, pm);
                try (Scope scope = GlobalTracer.get().buildSpan("Manage Results").startActive(true))
                {
                    LOGGER.info(new LogInfo(pm, LoggingEventType.EXECUTION_PLAN_EXEC_STOP, "").toString());
                    return ResultManager.manageResult(pm, result, format, LoggingEventType.EXECUTION_PLAN_EXEC_ERROR);
                }
            }
            else
            {
                return Response.status(500).type(MediaType.TEXT_PLAIN).entity(new ResultManager.ErrorMessage(20, "Only SingleExecutionPlan is supported")).build();
            }
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, LoggingEventType.EXECUTION_PLAN_EXEC_ERROR, pm);
        }
    }
}
