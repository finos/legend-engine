// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.connection.api.extensions.relational;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.finos.legend.engine.authentication.DatabaseAuthenticationFlowMetadata;
import org.finos.legend.engine.authentication.provider.DatabaseAuthenticationFlowProvider;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;

// TODO : epsstan - Refactor path
@Api(tags = "Connectors - Metadata")
@Path("pure/v1/protocols/relational")
@Produces(MediaType.APPLICATION_JSON)

public class RelationalExtensionsMetadataApi {
    private Optional<DatabaseAuthenticationFlowProvider> flowProviderHolder;

    public RelationalExtensionsMetadataApi(Optional<DatabaseAuthenticationFlowProvider> databaseAuthenticationFlowProvider)
    {
        this.flowProviderHolder = databaseAuthenticationFlowProvider;
    }

    @Path("flow/metadata")
    @ApiOperation(value = "Get metadata about available database authentication flows")
    @GET
    public Response getFlows() {
        try {
            if (!this.flowProviderHolder.isPresent())
            {
                return Response.ok(Lists.immutable.empty(), MediaType.APPLICATION_JSON_TYPE).build();
            }
            else
            {
                ImmutableList<DatabaseAuthenticationFlowMetadata> supportedFlowsMetadata = this.flowProviderHolder.get().getSupportedFlowsMetadata();
                return Response.ok(supportedFlowsMetadata, MediaType.APPLICATION_JSON_TYPE).build();
            }
        } catch (Exception e) {
            return ExceptionTool.exceptionManager(e, LoggingEventType.CATCH_ALL, null);
        }
    }

    @Path("datasource/metadata")
    @ApiOperation(value = "Get metadata about available data sources")
    @GET
    public Response getDatasources() {
        try {
            return Response.ok().build();
        } catch (Exception e) {
            return ExceptionTool.exceptionManager(e, LoggingEventType.CATCH_ALL, null);
        }
    }

    @Path("authenticationStrategy/metadata")
    @ApiOperation(value = "Get metadata about available authentication schemes")
    @GET
    public Response getAuthenticationStrategies() {
        try {
            return Response.ok().build();
        } catch (Exception e) {
            return ExceptionTool.exceptionManager(e, LoggingEventType.CATCH_ALL, null);
        }
    }
}
