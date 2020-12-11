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

package org.finos.legend.engine.external.format.rosetta.schema.generations;

import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function0;
import org.finos.legend.engine.external.shared.format.generations.GenerationOutput;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.api.result.ManageConstantResult;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.finos.legend.pure.generated.Root_meta_pure_generation_metamodel_GenerationOutput;
import org.finos.legend.pure.generated.core_external_format_rosetta_transformation_entry;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;
import org.slf4j.Logger;

import javax.security.auth.Subject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.finos.legend.engine.shared.core.operational.http.InflateInterceptor.APPLICATION_ZLIB;

@Api(tags = "Generation - Schema")
@Path("pure/v1/schemaGeneration")
@Produces(MediaType.APPLICATION_JSON)
public class RosettaGenerationService
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");
    private final ModelManager modelManager;

    public RosettaGenerationService(ModelManager modelManager)
    {
        this.modelManager = modelManager;
    }

    @POST
    @Path("cdm")
    @ApiOperation(value = "Generates Rosetta CDM classes from PureModel")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    public Response generateCdm(RosettaGenerationInput generateCdmInput, @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        Subject subject = ProfileManagerHelper.extractSubject(pm);
        boolean interactive = generateCdmInput.model instanceof PureModelContextData;
        try (Scope scope = GlobalTracer.get().buildSpan("Service: Generate CDM").startActive(true))
        {
            return exec(
                    generateCdmInput.config != null ? generateCdmInput.config : new RosettaGenerationConfig(),
                    () -> this.modelManager.loadModelAndData(generateCdmInput.model, generateCdmInput.clientVersion, subject, null).getTwo(),
                    interactive,
                    subject);
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, interactive ? LoggingEventType.GENERATE_CDM_INTERACTIVE_ERROR : LoggingEventType.GENERATE_CDM_ERROR, subject);
        }

    }

    private Response exec(RosettaGenerationConfig cdmConfig, Function0<PureModel> pureModelFunc, boolean interactive, Subject subject)
    {
        try
        {
            LOGGER.info(new LogInfo(subject, interactive ? LoggingEventType.GENERATE_CDM_INTERACTIVE_START : LoggingEventType.GENERATE_CDM_START).toString());
            PureModel pureModel = pureModelFunc.value();
            RichIterable<? extends Root_meta_pure_generation_metamodel_GenerationOutput> output = core_external_format_rosetta_transformation_entry.Root_meta_external_format_rosetta_generation_generateRosettaFromPureWithScope_RosettaConfig_1__GenerationOutput_MANY_(cdmConfig.process(pureModel), pureModel.getExecutionSupport());
            return ManageConstantResult.manageResult(subject, output.collect(v -> new GenerationOutput(v._content(), v._fileName(), v._format())).toList());
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, interactive ? LoggingEventType.GENERATE_CDM_INTERACTIVE_ERROR : LoggingEventType.GENERATE_CDM_ERROR, subject);
        }
    }
}

