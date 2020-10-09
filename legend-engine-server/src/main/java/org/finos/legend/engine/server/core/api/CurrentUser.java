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

package org.finos.legend.engine.server.core.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;
import org.slf4j.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Api(tags = "Server")
@Path("server/v1")
@Produces(MediaType.APPLICATION_JSON)
public class CurrentUser
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");

    @GET
    @Path("currentUser")
    @ApiOperation(value = "Provides server build and dependency information")
    public Response executePureGet(@Pac4JProfileManager @ApiParam(hidden = true) ProfileManager<CommonProfile> pm)
    {
        CommonProfile profile = pm.get(true).orElse(null);
        return Response.status(200).type(MediaType.APPLICATION_JSON).entity("\"" + (profile != null ? profile.getId() : "UNKNOWN") + "\"").build();
    }
}
