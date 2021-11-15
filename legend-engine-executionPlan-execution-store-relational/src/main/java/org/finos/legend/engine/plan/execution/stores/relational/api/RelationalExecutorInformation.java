package org.finos.legend.engine.plan.execution.stores.relational.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.state.ConnectionStateManager;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.RelationalStoreExecutor;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Api(tags = "Server")
@Path("server/v1")
@Produces(MediaType.APPLICATION_JSON)
public class RelationalExecutorInformation
{
    private final RelationalStoreExecutor relationalStoreExecutor;

    public RelationalExecutorInformation(RelationalStoreExecutor relationalStoreExecutor)
    {
        this.relationalStoreExecutor = relationalStoreExecutor;
    }

    @GET
    @Path("executorInfo/relational/pools/{poolName}")
    @ApiOperation(value = "Provides database pool information ")
    public Response getPoolInformation(@PathParam("poolName") String poolName)
    {
        return Response.status(200).type(MediaType.APPLICATION_JSON).entity(relationalStoreExecutor.getStoreState().getStoreExecutionInfo().findByPoolName(poolName)).build();
    }

    @DELETE
    @Path("executorInfo/relational/pools/{poolName}")
    @ApiOperation(value = "soft evict connections in this  database pool ")
    public Response sofEvict(@PathParam("poolName") String poolName)
    {
        return Response.status(200).type(MediaType.APPLICATION_JSON).entity(relationalStoreExecutor.getStoreState().getStoreExecutionInfo().softEvictConnections(poolName)).build();
    }

    @GET
    @Path("executorInfo/relational/{user}")
    @ApiOperation(value = "Provides pool information by user ")
    public Response getUserInformation(@PathParam("user") String user)
    {
        return Response.status(200).type(MediaType.APPLICATION_JSON).entity(relationalStoreExecutor.getStoreState().getStoreExecutionInfo().getPoolInformationByUser(user)).build();
    }

    @GET
    @Path("executorInfo/connectionState")
    @ApiOperation(value = "Provides connection state information")
    public Response getConnectionState()
    {
        ConnectionStateManager stateManager = ConnectionStateManager.getInstance();
        List<ConnectionStateManager.ConnectionStatePOJO> states = stateManager.getAll();
        return Response.status(200).type(MediaType.APPLICATION_JSON).entity(states).build();
    }
}
