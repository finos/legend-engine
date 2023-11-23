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
import io.swagger.annotations.ApiParam;
import org.finos.legend.connection.Connection;
import org.finos.legend.connection.ConnectionFactory;
import org.finos.legend.connection.IdentityFactory;
import org.finos.legend.connection.IdentitySpecification;
import org.finos.legend.connection.LegendEnvironment;
import org.finos.legend.engine.datapush.DataPusher;
import org.finos.legend.engine.datapush.DataPusherProvider;
import org.finos.legend.engine.datapush.data.CSVData;
import org.finos.legend.engine.datapush.server.ConnectionModelLoader;
import org.finos.legend.engine.language.pure.modelManager.sdlc.configuration.MetaDataServerConfiguration;
import org.finos.legend.engine.server.support.server.resources.BaseResource;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/data-push")
@Api("Data Push")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class DataPushResource extends BaseResource
{
    private static final String TEXT_CSV = "text/csv";
    private static final Logger LOGGER = LoggerFactory.getLogger(DataPushResource.class);

    private final ConnectionModelLoader connectionModelLoader;
    private final LegendEnvironment environment;
    private final IdentityFactory identityFactory;
    private final ConnectionFactory connectionFactory;
    private final DataPusherProvider dataPusherProvider;

    public DataPushResource(MetaDataServerConfiguration metadataserver, LegendEnvironment environment, IdentityFactory identityFactory, ConnectionFactory connectionFactory, DataPusherProvider dataPusherProvider)
    {
        this.environment = environment;
        this.identityFactory = identityFactory;
        this.connectionFactory = connectionFactory;
        this.dataPusherProvider = dataPusherProvider;
        this.connectionModelLoader = new ConnectionModelLoader(metadataserver);
    }

    @Path("/push/{groupId}/{artifactId}/{versionId}/{connectionPath}")
    @POST
    @Consumes({
            // TODO: content type will drive how we interpret the data, right now
            // we only support CSV
            MediaType.TEXT_PLAIN,
            MediaType.TEXT_XML,
            MediaType.APPLICATION_JSON,
            TEXT_CSV
    })
    @Produces(MediaType.APPLICATION_JSON)
    public Response pushData(
            @PathParam("groupId") String groupId,
            @PathParam("artifactId") String artifactId,
            @PathParam("versionId") String versionId,
            @PathParam("connectionPath") String connectionPath,
            String data,
            @Context HttpServletRequest request,
            @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> profileManager
    )
    {
        List<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(profileManager);
        Identity identity = this.identityFactory.createIdentity(
                IdentitySpecification.builder().profiles(profiles).build()
        );
        org.finos.legend.engine.protocol.pure.v1.packageableElement.connection.Connection connection = this.connectionModelLoader.getConnectionFromProject(profiles, groupId, artifactId, versionId, connectionPath);

        CSVData csvData = new CSVData();
        csvData.value = data;

        try
        {
            this.pushCSVData(identity, connection, csvData);
            return Response.noContent().build();
        }
        catch (Exception exception)
        {
            LOGGER.error("Can't push data:\n", exception);
            return handleException(profiles, exception);
        }
    }

    @Path("/pushDev/{projectId}/{workspaceId}/{connectionPath}")
    @POST
    @Consumes({
            // TODO: content type will drive how we interpret the data, right now
            // we only support CSV
            MediaType.TEXT_PLAIN,
            MediaType.TEXT_XML,
            MediaType.APPLICATION_JSON,
            TEXT_CSV
    })
    @Produces(MediaType.APPLICATION_JSON)
    public Response pushData_Dev(
            @PathParam("projectId") String projectId,
            @PathParam("workspaceId") String workspaceId,
            @PathParam("connectionPath") String connectionPath,
            @QueryParam("isGroupWorkspace") @DefaultValue("false") boolean isGroupWorkspace,
            String data,
            @Context HttpServletRequest request,
            @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> profileManager
    )
    {
        List<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(profileManager);
        Identity identity = this.identityFactory.createIdentity(
                IdentitySpecification.builder().profiles(profiles).build()
        );
        org.finos.legend.engine.protocol.pure.v1.packageableElement.connection.Connection connection = this.connectionModelLoader.getConnectionFromSDLCWorkspace(request, projectId, workspaceId, isGroupWorkspace, connectionPath);

        CSVData csvData = new CSVData();
        csvData.value = data;

        try
        {
            this.pushCSVData(identity, connection, csvData);
            return Response.noContent().build();
        }
        catch (Exception exception)
        {
            LOGGER.error("Can't push data:\n", exception);
            return handleException(profiles, exception);
        }
    }

    private Response handleException(List<CommonProfile> profiles, Exception exception)
    {
        Response.Status status = exception instanceof EngineException ? Response.Status.BAD_REQUEST : Response.Status.INTERNAL_SERVER_ERROR;
        return ExceptionTool.exceptionManager(exception, LoggingEventType.ERROR_MANAGEMENT_ERROR, status, profiles);
    }

    private void pushCSVData(Identity identity, org.finos.legend.engine.protocol.pure.v1.packageableElement.connection.Connection connectionProtocol, CSVData csvData)
    {
        Connection connection = Connection.builder().fromProtocol(connectionProtocol, this.environment).build();
        try
        {
            DataPusher dataPusher = this.dataPusherProvider.getDataPusher(connection);
            dataPusher.configure(this.connectionFactory);
            dataPusher.writeCSV(identity, connection, csvData);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    // ------------------------ DEBUG -----------------------
    // TO BE REMOVED when we stabilize the API and models

    @Path("/pushDev/debug")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response TEMPORARY__pushData_Debug(
            DebugInput input,
            @Context HttpServletRequest request,
            @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> profileManager
    )
    {
        List<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(profileManager);
        Identity identity = this.identityFactory.createIdentity(
                IdentitySpecification.builder().profiles(profiles).build()
        );
        CSVData csvData = new CSVData();
        csvData.value = input.data;

        try
        {
            this.pushCSVData(identity, input.connection, csvData);
            return Response.noContent().build();
        }
        catch (Exception exception)
        {
            LOGGER.error("Can't push data:\n", exception);
            return handleException(profiles, exception);
        }
    }

    public static class DebugInput
    {
        public org.finos.legend.engine.protocol.pure.v1.packageableElement.connection.Connection connection;
        public String data;
    }
}
