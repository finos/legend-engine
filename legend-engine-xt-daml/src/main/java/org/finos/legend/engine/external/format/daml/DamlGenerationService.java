//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.external.format.daml;

import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.external.format.daml.generation.model.DamlGenerationConfig;
import org.finos.legend.engine.external.format.daml.generation.model.DamlGenerationInput;
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
import org.finos.legend.pure.generated.core_external_language_daml_transformation;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;
import org.slf4j.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.finos.legend.engine.shared.core.operational.http.InflateInterceptor.APPLICATION_ZLIB;

@Api(tags = "External - Generation - Code")
@Path("pure/v1/codeGeneration")
@Produces(MediaType.APPLICATION_JSON)
public class DamlGenerationService
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");
    private final ModelManager modelManager;

    public DamlGenerationService(ModelManager modelManager)
    {
        this.modelManager = modelManager;
    }

    @POST
    @Path("daml")
    @ApiOperation(value = "Generates DAML classes from PureModel")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    public Response generate(DamlGenerationInput input, @ApiParam(hidden = true)  @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        boolean interactive = input.model instanceof PureModelContextData;
        try (Scope scope = GlobalTracer.get().buildSpan("Service: Generate DAML").startActive(true))
        {
            return exec(
                    input.config != null ? input.config : new DamlGenerationConfig(),
                    () -> this.modelManager.loadModelAndData(input.model, input.clientVersion, profiles, null).getTwo(),
                    interactive,
                    profiles);
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, interactive ? LoggingEventType.GENERATE_DAML_CODE_INTERACTIVE_ERROR : LoggingEventType.GENERATE_DAML_CODE_ERROR, profiles);
        }

    }

    private Response exec(DamlGenerationConfig config, Function0<PureModel> pureModelFunc, boolean interactive, MutableList<CommonProfile> pm)
    {
        try
        {
            LOGGER.info(new LogInfo(pm, interactive ? LoggingEventType.GENERATE_DAML_CODE_INTERACTIVE_START : LoggingEventType.GENERATE_DAML_CODE_START).toString());
            PureModel pureModel = pureModelFunc.value();
            RichIterable<? extends Root_meta_pure_generation_metamodel_GenerationOutput> output = core_external_language_daml_transformation.Root_meta_external_language_daml_generation_generateDamlFromPure_DamlConfig_1__GenerationOutput_MANY_(config.transformToPure(pureModel), pureModel.getExecutionSupport());
            LOGGER.info(new LogInfo(pm, interactive ? LoggingEventType.GENERATE_DAML_CODE_INTERACTIVE_STOP : LoggingEventType.GENERATE_DAML_CODE_STOP).toString());
            return ManageConstantResult.manageResult(pm, output.collect(v -> new GenerationOutput(v._content(), v._fileName(), v._format())).toList());
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, interactive ? LoggingEventType.GENERATE_DAML_CODE_INTERACTIVE_ERROR : LoggingEventType.GENERATE_DAML_CODE_ERROR, pm);
        }
    }
}



