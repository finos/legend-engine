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

package org.finos.legend.engine.language.pure.modelManager.sdlc.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.finos.legend.engine.language.pure.modelManager.sdlc.configuration.MetaDataServerConfiguration;
import org.finos.legend.engine.language.pure.modelManager.sdlc.configuration.PureServerConnectionConfiguration;
import org.slf4j.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Api(tags = "Server")
@Path("server/v1")
@Produces(MediaType.APPLICATION_JSON)
public class MetadataServerInfo
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");
    private String message;

    public MetadataServerInfo(MetaDataServerConfiguration metaDataServerConfiguration)
    {
        String typeOfServerConnectionConfiguration = "";
        String allowedOverrideUrls = "";
        if (metaDataServerConfiguration.pure instanceof PureServerConnectionConfiguration)
        {
            typeOfServerConnectionConfiguration = "      \"_type\":\"pureServerConnectionConfiguration\",";
            allowedOverrideUrls = "," + "      \"allowedOverrideUrls\":" + (((PureServerConnectionConfiguration) metaDataServerConfiguration.pure).allowedOverrideUrls == null ? "[]" : "[\"" + String.join("\",\"", ((PureServerConnectionConfiguration) metaDataServerConfiguration.pure).allowedOverrideUrls) + "\"]");
        }
        this.message = "{" +
                "\"metadataserver\": {" +
                "   \"pure\":" +
                "   {" +
                typeOfServerConnectionConfiguration +
                "      \"host\":\"" + metaDataServerConfiguration.getPure().host + "\"," +
                "      \"port\":" + metaDataServerConfiguration.getPure().port +
                allowedOverrideUrls +
                "   }," +
                "   \"alloy\":" +
                "   {" +
                "      \"host\":\"" + metaDataServerConfiguration.getAlloy().host + "\"," +
                "      \"port\":" + metaDataServerConfiguration.getAlloy().port +
                "   }" +
                "  }" +
                "}";
    }

    @GET
    @Path("info/metadataServer")
    @ApiOperation(value = "Provides metadataServer config for modelManager")
    public Response executePureGet()
    {
        return Response.status(200).type(MediaType.APPLICATION_JSON).entity(message).build();
    }
}
