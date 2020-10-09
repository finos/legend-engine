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

package org.finos.legend.engine.server;

import io.swagger.annotations.ApiOperation;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.engine.shared.core.api.result.ManageConstantResult;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Fetches all available protocol to/from external formats generations for code and schemas.
 * WIP: These APIs are placeholders until we open-source external format supports
 */
@Path("pure/v1")
@Produces(MediaType.APPLICATION_JSON)
public class GenerationAndImportApi
{
    @GET
    @Path("/schemaGeneration/availableGenerations")
    @ApiOperation(value = "Get all schema generations available alongside their configuration properties")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response schemaGenerationDescriptions()
    {
        try
        {
            return ManageConstantResult.manageResult(null, FastList.newList());
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, LoggingEventType.SCHEMA_GENERATION_ERROR, null);
        }
    }

    @GET
    @Path("/schemaImport/availableImports")
    @ApiOperation(value = "Get all schema imports available alongside their configuration properties")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response schemaImportsDescriptions()
    {
        try
        {
            return ManageConstantResult.manageResult(null, FastList.newList());
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, LoggingEventType.SCHEMA_GENERATION_ERROR, null);
        }
    }

    @GET
    @Path("/codeGeneration/availableGenerations")
    @ApiOperation(value = "Get all code generations available alongside their configuration properties")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response codeGenerationDescriptions()
    {
        try
        {
            return ManageConstantResult.manageResult(null, FastList.newList());
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, LoggingEventType.CODE_GENERATION_ERROR, null);
        }
    }

    @GET
    @Path("/codeImport/availableImports")
    @ApiOperation(value = "Get all code imports available alongside their configuration properties")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response codeImportsDescriptions()
    {
        try
        {
            return ManageConstantResult.manageResult(null, FastList.newList());
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, LoggingEventType.CODE_GENERATION_ERROR, null);
        }
    }
}
