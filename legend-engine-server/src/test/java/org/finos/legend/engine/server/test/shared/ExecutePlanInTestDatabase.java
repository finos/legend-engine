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

package org.finos.legend.engine.server.test.shared;

import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.api.result.ResultManager;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManagerSelector;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.CompositeExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.ExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.MultiResultSequenceExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.RelationalInstantiationExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.SQLExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.finos.legend.engine.shared.core.operational.http.InflateInterceptor.APPLICATION_ZLIB;


@Api(tags = "Pure - Execution")
@Path("pure/v1/execution")
@Produces(MediaType.APPLICATION_JSON)
public class ExecutePlanInTestDatabase
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");
    private final PlanExecutor planExecutor;
    ConnectionManagerSelector manager;
    Map<DatabaseType, RelationalDatabaseConnection> connectionByDatabaseType = new HashMap<DatabaseType, RelationalDatabaseConnection>();

    public ExecutePlanInTestDatabase(PlanExecutor planExecutor, ConnectionManagerSelector manager, Map<DatabaseType, RelationalDatabaseConnection> testConnections)
    {
        this.planExecutor = planExecutor;
        this.manager = manager;
        this.connectionByDatabaseType = testConnections;

    }

    @POST
    @Path("executePlanInTestDatabase/{databaseType}")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    public Response executePlanInTestDatabase(@Context HttpServletRequest request,
                                              ExecutionPlan plan,
                                              @PathParam("databaseType") DatabaseType databaseType,
                                              @DefaultValue("true") @QueryParam("injectTestConnectionInPlan") Boolean injectTestConnectionInPlan,
                                              @DefaultValue(SerializationFormat.defaultFormatString) @QueryParam("serializationFormat") SerializationFormat format,
                                              @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);

        try
        {
            LOGGER.info(new LogInfo(profiles, LoggingEventType.EXECUTION_PLAN_EXEC_START, "").toString());

            RelationalDatabaseConnection relationalDatabaseConnection = this.getTestConnection(databaseType);
            ExecutionPlan newPlan = injectTestConnectionInPlan ? injectConnectionInPlan(plan, relationalDatabaseConnection) : plan;
            Result result = execute(newPlan, profiles);

            try (Scope scope = GlobalTracer.get().buildSpan("Manage Results").startActive(true))
            {
                LOGGER.info(new LogInfo(profiles, LoggingEventType.EXECUTION_PLAN_EXEC_STOP, "").toString());
                return ResultManager.manageResult(profiles, result, format, LoggingEventType.EXECUTION_PLAN_EXEC_ERROR);
            }
        } catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, LoggingEventType.EXECUTION_PLAN_EXEC_ERROR, profiles);
        }
    }

    public Result execute(ExecutionPlan execPlan, MutableList<CommonProfile> profiles)
    {
        if (execPlan instanceof SingleExecutionPlan)
        {
            // Assume that the input exec plan has no variables
            return this.planExecutor.execute((SingleExecutionPlan) execPlan, Maps.mutable.empty(), null, profiles);
        }
        throw new UnsupportedOperationException("not yet supported");
    }

    private ExecutionPlan injectConnectionInPlan(ExecutionPlan executionPlan, RelationalDatabaseConnection connection)
    {
        if (executionPlan instanceof SingleExecutionPlan)
        {
            SingleExecutionPlan singleExecutionPlan = (SingleExecutionPlan) executionPlan;
            ExecutionNode newRootExecutionNode = handleExecutionNode(singleExecutionPlan.rootExecutionNode, connection);
            singleExecutionPlan.rootExecutionNode = newRootExecutionNode;
            return singleExecutionPlan;
        }
        throw new UnsupportedOperationException("not yet supported");
    }

    private ExecutionNode handleExecutionNode(ExecutionNode node, RelationalDatabaseConnection connection)
    {
        if (node instanceof SQLExecutionNode)
        {
            DatabaseConnection givenConnection = ((SQLExecutionNode) node).connection;
            //fetch connection for dbtype from connection manager and point to correct store element
            connection.element = givenConnection.element;
            ((SQLExecutionNode) node).connection = connection;
            return node;
        } else if (node instanceof RelationalInstantiationExecutionNode)
        {
            List<ExecutionNode> newNodes = node.executionNodes.stream().map(childNode -> handleExecutionNode(childNode, connection)).collect(Collectors.toList());
            node.executionNodes = newNodes;
            return node;
        } else if (node instanceof MultiResultSequenceExecutionNode)
        {
            List<ExecutionNode> newNodes = node.executionNodes.stream().map(childNode -> handleExecutionNode(childNode, connection)).collect(Collectors.toList());
            node.executionNodes = newNodes;
            return node;
        }
        throw new UnsupportedOperationException("not yet supported");
    }

    private RelationalDatabaseConnection getTestConnection(DatabaseType dbType)
    {
        if (!connectionByDatabaseType.containsKey(dbType))
        {
            throw new RuntimeException(" Test Database connection not found for dbType: " + dbType + " , while executing plan");
        }
        ;
        return connectionByDatabaseType.get(dbType);
    }
}
