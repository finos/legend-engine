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

package org.finos.legend.engine.datapush.server.resources;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.finos.legend.engine.datapush.specification.model.DummyDataPushSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.postgres.test.LegendPostgresCurrentUserCommand;
import org.finos.legend.engine.plan.execution.stores.relational.connection.postgres.test.LegendPostgresSupport;
import org.finos.legend.engine.server.support.server.exception.ServerException;
import org.finos.legend.engine.server.support.server.resources.BaseResource;
import org.finos.legend.engine.store.core.LegendStoreConnectionProvider;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.Connection;

@Path("/data/push")
@Api("Data Push")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class DataPushResource extends BaseResource
{
    public DataPushResource()
    {

    }

    @Path("/register")
    @POST
    @ApiOperation("Create a new specification")
    public Response createSpecification(DummyDataPushSpecification specification)
    {
        ServerException.validateNonNull(specification, "Input required to create spec");

        return executeWithLogging(
                "creating specification \"",
                () -> null
        );
    }

    @Path("/push")
    @POST
    @ApiOperation("Push data")
    public Response push(Object object)
    {
        ServerException.validateNonNull(object, "Input required to push");

        return executeWithLogging(
                "creating specification \"",
                () -> Response.ok().entity(this.pushData()).build()
        );
    }

    // TODO - refactor to use a command that actually pushes data
    private String pushData()
    {
        try
        {
            // TODO - inject authn/credential support via Dropwizard environment ??
            // TODO - inject store support via Dropwizard environment ??
            LegendPostgresSupport postgresSupport = new LegendPostgresSupport();
            LegendStoreConnectionProvider<Connection> connectionProvider = postgresSupport.getConnectionProvider();
            LegendPostgresCurrentUserCommand command = new LegendPostgresCurrentUserCommand();
            command.initialize(connectionProvider);

            String result = command.run();
            return result;
        }
        catch (Exception e)
        {
            throw new RuntimeException("TODO - Add proper message and hook up to exception mapper");
        }
    }
}
