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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.api.result.ResultManager;
import org.finos.legend.engine.protocol.pure.v1.PureProtocolObjectMapperFactory;
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
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

import static org.finos.legend.engine.shared.core.operational.http.InflateInterceptor.APPLICATION_ZLIB;


@Api(tags = "Utilities - TestConnection")
@Path("pure/v1/utilities/tests/connections")
@Produces(MediaType.APPLICATION_JSON)
public class TestConnectionProviderApi
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");
    private static final ObjectMapper objectMapper = PureProtocolObjectMapperFactory.getNewObjectMapper();

    Map<DatabaseType, RelationalDatabaseConnection> testConnectionByDatabaseType = new HashMap<DatabaseType, RelationalDatabaseConnection>();

    public TestConnectionProviderApi(Map<DatabaseType, RelationalDatabaseConnection> testConnections)
    {
        this.testConnectionByDatabaseType = testConnections;
    }

    @GET
    @Path("/{databaseType}")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    public Response getTestConnection(@Context HttpServletRequest request,
                                      @PathParam("databaseType") DatabaseType databaseType,
                                      @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);

        try
        {
            LOGGER.info(new LogInfo(profiles, LoggingEventType.EXECUTION_PLAN_EXEC_START, "").toString());

            if (this.testConnectionByDatabaseType.containsKey(databaseType))
            {
                RelationalDatabaseConnection conn = this.testConnectionByDatabaseType.get(databaseType);
                return Response.ok(objectMapper.writeValueAsString(conn)).build();
            }
            else
            {
                String message = "No Connection found available for Database Type:" + databaseType;
                return Response.status(500).type(MediaType.APPLICATION_JSON_TYPE)
                        .entity(new ResultManager.ErrorMessage(20, "{\"message\":\"" + message + "\"}")).build();
            }
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, LoggingEventType.EXECUTION_PLAN_EXEC_ERROR, profiles);
        }
    }
}
