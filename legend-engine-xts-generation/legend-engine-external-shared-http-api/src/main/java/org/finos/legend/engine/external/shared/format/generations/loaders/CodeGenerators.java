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

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.finos.legend.engine.external.shared.format.generations.description.FileGenerationDescription;
import org.finos.legend.engine.external.shared.format.generations.description.GenerationConfigurationDescription;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.api.result.ManageConstantResult;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.*;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "External - Generation - Code")
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
    @Operation(summary = "Get all code generations available alongside their configuration properties")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response codeGenerationDescriptions()
    {
        try
        {
            PureModel pureModel = this.modelManager.loadModelAndData(PureModelContextData.newPureModelContextData(), null, Identity.getAnonymousIdentity(), null).getTwo();
            List<FileGenerationDescription> descriptions = codeConfigurationDescriptions.stream().map(e -> FileGenerationDescription.newDescription(e, pureModel)).collect(Collectors.toList());
            return ManageConstantResult.manageResult(Identity.getAnonymousIdentity().getName(), descriptions);
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, LoggingEventType.CODE_GENERATION_ERROR, null);
        }
    }
}
