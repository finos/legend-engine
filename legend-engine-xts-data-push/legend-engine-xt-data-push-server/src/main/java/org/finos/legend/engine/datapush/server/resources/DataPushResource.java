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

import com.opencsv.CSVWriter;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.connection.jdbc.StaticJDBCConnectionSetupSpecification;
import org.finos.legend.engine.datapush.server.ConnectionFactoryBundle;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.UserPasswordAuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.PropertiesFileSecret;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.server.support.server.resources.BaseResource;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.IdentityFactoryProvider;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

@Path("/data/push")
@Api("Data Push")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class DataPushResource extends BaseResource
{
    public DataPushResource()
    {
    }

    @Path("/location/{location}/datastore/{datastore}/dataset/{dataset}")
    @POST
    @ApiOperation("Push data")
    public Response push(@PathParam("location") String location, @PathParam("datastore") String datastore, @PathParam("dataset") String dataset) throws IOException
    {
        return executeWithLogging(
                "pushing data\"",
                () -> Response.ok().entity(this.pushData(location, datastore, dataset)).build()
        );
    }

    private String pushData(String location, String datastore, String dataset)
    {
        try
        {
            // TODO - actually push the data
            return "ok";
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Path("/beta/push")
    @POST
    @Consumes({MediaType.TEXT_PLAIN})
    @ApiOperation("Push data (beta)")
    public Response pushBeta(String input, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> profileManager) throws IOException
    {
        return executeWithLogging(
                "pushing data (beta)\"",
                () -> Response.ok().entity(this.pushDataBeta(profileManager)).build()
        );
    }

    private String pushDataBeta(ProfileManager<CommonProfile> profileManager)
    {
        try
        {
            MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(profileManager);
            Identity identity = IdentityFactoryProvider.getInstance().makeIdentity(profiles);

            Connection connection = ConnectionFactoryBundle.getConnectionFactory().setupConnection(
                    new StaticJDBCConnectionSetupSpecification("localhost", 5432, DatabaseType.Postgres, "legend"),
                    new UserPasswordAuthenticationSpecification("newuser", new PropertiesFileSecret("passwordRef")),
                    identity
            );

            // Test the SQL
            String sqlQuery = "select * from student_table";
            Statement stmt = connection.createStatement();
//            stmt.setMaxRows(PREVIEW_LIMIT);
            ResultSet resultSet = stmt.executeQuery(sqlQuery);
            StringWriter stringWriter = new StringWriter();
            CSVWriter csvWriter = new CSVWriter(stringWriter);
            csvWriter.writeAll(resultSet, true, true, false);
            return stringWriter.toString();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
