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
import org.finos.legend.engine.server.core.configuration.DeploymentConfiguration;
import org.finos.legend.engine.server.core.configuration.OpenTracingConfiguration;
import org.finos.legend.engine.shared.core.deployment.DeploymentStateAndVersions;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.Version;
import org.slf4j.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.InetAddress;
import java.util.Date;

@Api(tags = "Server")
@Path("server/v1")
@Produces(MediaType.APPLICATION_JSON)
public class Info
{
    private String message;

    public Info(DeploymentConfiguration deploymentConfiguration, OpenTracingConfiguration openTracingConfiguration)
    {
        try
        {
            String hostAddress = InetAddress.getLocalHost().getCanonicalHostName();

            message = "{" +
                    "\"alloy\":" +
                    "   {" +
                    "     \"server\":" +
                    "         {" +
                    "             \"host\":\"" + hostAddress + "\"," +
                    "             \"startTime\":\"" + new Date() + "\"," +
                    "             \"timeZone\":\"" + java.util.TimeZone.getDefault().getID() + "\"" +
                    "         }," +
                    "     \"sdlc\":" + DeploymentStateAndVersions.sdlcJSON + "," +
                    "     \"deployment\":" +
                    "        {" +
                    "          \"mode\" : \"" + deploymentConfiguration.mode + "\"" +
                    "        }" +
                    "   }," +
                    "\"zipkin\":" +
                    "   {" +
                    "      \"url\":\"" + openTracingConfiguration.getZipkin() + "\"" +
                    "   }," +
                    "\"pure\":" +
                    "   {" +
                    "       \"platform\":\"" + Version.PLATFORM + "\"," +
                    "       \"server\":\"" + Version.SERVER + "\"," +
                    "       \"model\":\"" + Version.MODEL + "\"" +
                    "   }" +
                    "}";
        }
        catch (Exception e)
        {
            message = "{}";
        }
    }

    @GET
    @Path("info")
    @ApiOperation(value = "Provides server build and dependency information")
    public Response executePureGet()
    {
        return Response.status(200).type(MediaType.APPLICATION_JSON).entity(message).build();
    }
}
