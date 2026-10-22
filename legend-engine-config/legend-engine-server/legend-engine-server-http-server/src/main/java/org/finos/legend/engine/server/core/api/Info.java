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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.finos.legend.engine.server.core.configuration.DeploymentConfiguration;
import org.finos.legend.engine.server.core.configuration.OpenTracingConfiguration;
import org.finos.legend.engine.shared.core.deployment.DeploymentStateAndVersions;
import org.finos.legend.engine.shared.core.deployment.DeploymentVersionInfo;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.Version;
import org.slf4j.Logger;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.InetAddress;
import java.util.Date;
import java.util.Map;

@Tag(name = "Server")
@Path("server/v1")
@Produces(MediaType.APPLICATION_JSON)
public class Info
{
    private String message;
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(Info.class);

    public Info(DeploymentConfiguration deploymentConfiguration, OpenTracingConfiguration openTracingConfiguration)
    {
        this(deploymentConfiguration, openTracingConfiguration, null);
    }

    public Info(DeploymentConfiguration deploymentConfiguration, OpenTracingConfiguration openTracingConfiguration, Map<String, DeploymentVersionInfo> extraDeploymentInformation)
    {
        try
        {
            ObjectMapper m = new ObjectMapper();
            String hostAddress = InetAddress.getLocalHost().getCanonicalHostName();
            String addedInfo = "";
            if (extraDeploymentInformation != null)
            {
                String extraJson = m.writeValueAsString(extraDeploymentInformation);
                addedInfo = "," + extraJson.substring(1, extraJson.length() - 1);
            }
            message = "{" +
                    "\"info\":" +
                    "   {" +
                    "     \"server\":" +
                    "         {" +
                    "             \"host\":\"" + hostAddress + "\"," +
                    "             \"startTime\":\"" + new Date() + "\"," +
                    "             \"timeZone\":\"" + java.util.TimeZone.getDefault().getID() + "\"" +
                    "         }," +
                    "     \"legendSDLC\":" + m.writeValueAsString(DeploymentStateAndVersions.sdlc) + "," +
                    "     \"deployment\":" +
                    "        {" +
                    "          \"mode\" : \"" + deploymentConfiguration.mode + "\"" +
                    "        }" +
                    addedInfo +
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
            LOGGER.error("Error creating info message", e);
            message = "{}";
        }
    }

    @GET
    @Path("info")
    @Operation(summary = "Provides server build and dependency information")
    public Response executePureGet()
    {
        return Response.status(200).type(MediaType.APPLICATION_JSON).entity(message).build();
    }
}
