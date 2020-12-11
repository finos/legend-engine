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

package org.finos.legend.engine.external.shared.format.imports.loaders;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.finos.legend.engine.external.shared.format.imports.description.ImportConfigurationDescription;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.shared.core.api.result.ManageConstantResult;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Api(tags = "Import - Code")
@Path("pure/v1/codeImport")
@Produces(MediaType.APPLICATION_JSON)
public class CodeImports
{
    private final ModelManager modelManager;
    private final List<ImportConfigurationDescription> codeConfigurationDescriptions;

    public CodeImports(ModelManager modelManager, List<ImportConfigurationDescription> importConfigurationDescriptions)
    {
        this.modelManager = modelManager;
        this.codeConfigurationDescriptions = importConfigurationDescriptions;
    }

    @GET
    @Path("availableImports")
    @ApiOperation(value = "Get all code imports available alongside their configuration properties")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response codeImportsDescriptions()
    {
        try
        {
            return ManageConstantResult.manageResult(null, codeConfigurationDescriptions);
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, LoggingEventType.CODE_GENERATION_ERROR, null);
        }
    }
}
