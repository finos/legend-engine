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
import java.util.List;
import java.util.stream.Collectors;

@Api(tags = "External - Generation - Code")
@Path("pure/v1/codeGeneration")
@Produces(MediaType.APPLICATION_JSON)
public class CodeGenerators
{
    private final ModelManager modelManager;
    private final List<GenerationConfigurationDescription> codeConfigurationDescriptions;

    public CodeGenerators(ModelManager modelManager, List<GenerationConfigurationDescription> codeConfigurationDescriptions)
    {
        this.modelManager = modelManager;
        this.codeConfigurationDescriptions = codeConfigurationDescriptions;
    }

    @GET
    @Path("availableGenerations")
    @ApiOperation(value = "Get all code generations available alongside their configuration properties")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response codeGenerationDescriptions()
    {
        try
        {
            PureModel pureModel = this.modelManager.loadModelAndData(PureModelContextData.newPureModelContextData(), null, null, null).getTwo();
            List<FileGenerationDescription> descriptions = codeConfigurationDescriptions.stream().map(e -> FileGenerationDescription.newDescription(e, pureModel)).collect(Collectors.toList());
            return ManageConstantResult.manageResult(null, descriptions);
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, LoggingEventType.CODE_GENERATION_ERROR, null);
        }
    }
}
