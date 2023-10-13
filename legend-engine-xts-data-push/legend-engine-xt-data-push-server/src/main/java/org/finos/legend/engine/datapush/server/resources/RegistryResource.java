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
import org.finos.legend.connection.LegendEnvironment;
import org.finos.legend.connection.StoreInstance;
import org.finos.legend.connection.StoreInstanceProvider;
import org.finos.legend.engine.datapush.server.DataPusher;
import org.finos.legend.engine.datapush.server.DataStager;
import org.finos.legend.engine.datapush.server.StoreCatalog;
import org.finos.legend.engine.server.support.server.resources.BaseResource;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/registry")
@Api("Registry")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class RegistryResource extends BaseResource
{
    private final LegendEnvironment environment;
    private final StoreInstanceProvider storeInstanceProvider;

    public RegistryResource(LegendEnvironment environment, StoreInstanceProvider storeInstanceProvider)
    {
        this.environment = environment;
        this.storeInstanceProvider = storeInstanceProvider;
    }

    @Path("/store")
    @GET
    @ApiOperation("Get store catalog")
    public Response getStoreCatalog(
            @ApiParam(hidden = true)
            @Pac4JProfileManager ProfileManager<CommonProfile> profileManager
    )
    {
        return executeWithLogging(
                "fetching store catalog\"",
                () -> Response.ok().entity(this.getStores()).build()
        );
    }

    @Path("/store/{storeInstanceRef}")
    @GET
    @ApiOperation("Get store instance")
    @Consumes(MediaType.TEXT_PLAIN)
    public Response getStoreInstance(
            @PathParam("storeInstanceRef") String storeInstanceRef,
            @ApiParam(hidden = true)
            @Pac4JProfileManager ProfileManager<CommonProfile> profileManager
    )
    {
        return executeWithLogging(
                "fetching store instance\"",
                () -> Response.ok().entity(this.getStore(storeInstanceRef)).build()
        );
    }

    private StoreInstance getStore(String ref)
    {
        return this.storeInstanceProvider.lookup(ref);
    }

    private StoreCatalog getStores()
    {
        StoreCatalog storeCatalog = new StoreCatalog();
        storeCatalog.stores = this.storeInstanceProvider.getAll();
        return storeCatalog;
    }
}
