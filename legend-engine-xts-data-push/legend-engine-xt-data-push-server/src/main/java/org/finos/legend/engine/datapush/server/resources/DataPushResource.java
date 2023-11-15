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
import io.swagger.annotations.ApiParam;
import org.finos.legend.connection.AuthenticationConfigurationProvider;
import org.finos.legend.connection.ConnectionFactory;
import org.finos.legend.connection.IdentityFactory;
import org.finos.legend.connection.IdentitySpecification;
import org.finos.legend.connection.LegendEnvironment;
import org.finos.legend.connection.StoreInstance;
import org.finos.legend.connection.StoreInstanceProvider;
import org.finos.legend.connection.protocol.AuthenticationConfiguration;
import org.finos.legend.engine.datapush.server.Data;
import org.finos.legend.engine.datapush.server.DataPusher;
import org.finos.legend.engine.datapush.server.DataStager;
import org.finos.legend.engine.server.support.server.resources.BaseResource;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/data-push")
@Api("Data Push")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class DataPushResource extends BaseResource
{
    private final LegendEnvironment environment;
    private final IdentityFactory identityFactory;
    private final StoreInstanceProvider storeInstanceProvider;
    private final AuthenticationConfigurationProvider authenticationConfigurationProvider;
    private final ConnectionFactory connectionFactory;
    private final DataStager dataStager;
    private final DataPusher dataPusher;

    public DataPushResource(LegendEnvironment environment, IdentityFactory identityFactory, StoreInstanceProvider storeInstanceProvider, AuthenticationConfigurationProvider authenticationConfigurationProvider, ConnectionFactory connectionFactory, DataStager dataStager, DataPusher dataPusher)
    {
        this.environment = environment;
        this.identityFactory = identityFactory;
        this.storeInstanceProvider = storeInstanceProvider;
        this.authenticationConfigurationProvider = authenticationConfigurationProvider;
        this.connectionFactory = connectionFactory;
        this.dataStager = dataStager;
        this.dataPusher = dataPusher;
    }

    @Path("/stage")
    @POST
    @ApiOperation("Push data to staging area")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response stageData(
            Data data,
            @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> profileManager
    )
    {
        Identity identity = this.identityFactory.createIdentity(
                new IdentitySpecification.Builder().withProfiles(ProfileManagerHelper.extractProfiles(profileManager)).build()
        );

        return executeWithLogging(
                "staging data\"",
                () -> Response.ok().entity(this.stageData(identity, data)).build()
        );
    }

    @Path("/__debug__/getStagedData")
    @POST
    @ApiOperation("DEBUG: Get staged data")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response stageData(
            String stagingRef,
            @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> profileManager
    )
    {
        Identity identity = this.identityFactory.createIdentity(
                new IdentitySpecification.Builder().withProfiles(ProfileManagerHelper.extractProfiles(profileManager)).build()
        );

        return executeWithLogging(
                "getting staged data\"",
                () -> Response.ok().entity(this.getStagedData(identity, stagingRef)).build()
        );
    }

    @Path("/push")
    @POST
    @ApiOperation("Push data from staging area")
    public Response pushData(
            DataPushInput input,
            @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> profileManager
    )
    {
        Identity identity = this.identityFactory.createIdentity(
                new IdentitySpecification.Builder().withProfiles(ProfileManagerHelper.extractProfiles(profileManager)).build()
        );

        return executeWithLogging(
                "pushing data\"",
                () ->
                {
                    this.pushData(identity, input.storeInstanceRef, input.stagingRef);
                    return Response.noContent().build();
                }
        );
    }

    public static class DataPushInput
    {
        public String storeInstanceRef;
        public String stagingRef;
    }

    private String stageData(Identity identity, Data data)
    {
        try
        {
            // TODO: @akphi - do we need to check for the identity here?
            return this.dataStager.write(identity, data);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private Data getStagedData(Identity identity, String stagingRef)
    {
        try
        {
            // TODO: @akphi - do we need to check for the identity here?
            return this.dataStager.read(identity, stagingRef);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private void pushData(Identity identity, String storeInstanceRef, String stagingRef)
    {
        StoreInstance storeInstance = this.storeInstanceProvider.lookup(storeInstanceRef);
        try
        {
            AuthenticationConfiguration authenticationConfiguration = this.authenticationConfigurationProvider.lookup(identity, storeInstance);
            this.dataPusher.write(identity, storeInstance, authenticationConfiguration, this.dataStager.read(identity, stagingRef));
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
