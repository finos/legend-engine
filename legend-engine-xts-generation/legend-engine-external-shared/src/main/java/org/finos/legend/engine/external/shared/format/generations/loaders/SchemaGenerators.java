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

package org.finos.legend.engine.external.shared.format.generations.loaders;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.finos.legend.engine.external.shared.format.generations.description.FileGenerationDescription;
import org.finos.legend.engine.external.shared.format.generations.description.GenerationConfigurationDescription;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.api.result.ManageConstantResult;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Deprecated
@Api(tags = "Z - Deprecated - Generation - Schema")
@Path("pure/v1/schemaGeneration")
@Produces(MediaType.APPLICATION_JSON)
public class SchemaGenerators
{
    private ModelManager modelManager;
    private List<GenerationConfigurationDescription> schemaConfigurationDescriptions = Collections.emptyList();

    public SchemaGenerators(ModelManager modelManager, List<GenerationConfigurationDescription> schemaConfigurationDescriptions)
    {
        this.modelManager = modelManager;
        this.schemaConfigurationDescriptions = schemaConfigurationDescriptions;
    }

    @Deprecated
    @GET
    @Path("availableGenerations")
    @ApiOperation(value = "Get all schema generations available alongside their configuration properties")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response schemaGenerationDescriptions()
    {
        try
        {
            PureModel pureModel = this.modelManager.loadModelAndData(PureModelContextData.newPureModelContextData(), null, null, null).getTwo();
            List<FileGenerationDescription> descriptions = schemaConfigurationDescriptions.stream().map(e -> FileGenerationDescription.newDescription(e, pureModel)).collect(Collectors.toList());
            return ManageConstantResult.manageResult(null, descriptions);
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, LoggingEventType.SCHEMA_GENERATION_ERROR, null);
        }
    }
}
