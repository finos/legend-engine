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

package org.finos.legend.engine.external.format.protobuf.deprecated.generation;

import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.external.format.protobuf.deprecated.generation.configuration.ProtobufGenerationConfig;
import org.finos.legend.engine.external.format.protobuf.deprecated.generation.configuration.ProtobufGenerationInput;
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
import org.finos.legend.pure.generated.core_external_format_protobuf_deprecated;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.finos.legend.engine.shared.core.operational.http.InflateInterceptor.APPLICATION_ZLIB;

@Deprecated
@Api(tags = "Z - Deprecated - Generation - Schema")
@Path("pure/v1/schemaGeneration")
@Produces(MediaType.APPLICATION_JSON)
public class ProtobufGenerationService
{
    private static final Logger LOGGER = LoggerFactory.getLogger("Alloy Execution Server");
    private final ModelManager modelManager;

    public ProtobufGenerationService(ModelManager modelManager)
    {
        this.modelManager = modelManager;
    }

    @Deprecated
    @POST
    @Path("protobuf")
    @ApiOperation(value = "Generates Protobuf for a given class and transitive dependencies")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    public Response generateProtobuf(ProtobufGenerationInput generateProtobufInput,
                                     @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        boolean interactive = generateProtobufInput.model instanceof PureModelContextData;
        try (Scope scope = GlobalTracer.get().buildSpan("Service: Generate Protobuf").startActive(true))
        {
            List<GenerationOutput> result = generateProtobufOutput(generateProtobufInput, profiles);
            return ManageConstantResult.manageResult(profiles, result);
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex,
                interactive ? LoggingEventType.GENERATE_PROTOBUF_CODE_INTERACTIVE_ERROR :
                    LoggingEventType.GENERATE_PROTOBUF_CODE_ERROR, profiles);
        }
    }

    public List<GenerationOutput> generateProtobufOutput(ProtobufGenerationInput generateProtobufInput,
                                                         @ApiParam(hidden = true) @Pac4JProfileManager
                                                             MutableList<CommonProfile> profiles)
    {
        long start = System.currentTimeMillis();
        boolean interactive = generateProtobufInput.model instanceof PureModelContextData;
        LOGGER.info(new LogInfo(profiles, interactive ? LoggingEventType.GENERATE_PROTOBUF_CODE_INTERACTIVE_START :
            LoggingEventType.GENERATE_PROTOBUF_CODE_START).toString());

        List<GenerationOutput> generationOutputs = exec(
            generateProtobufInput.config != null ? generateProtobufInput.config : new ProtobufGenerationConfig(),
            () -> this.modelManager
                .loadModelAndData(generateProtobufInput.model, generateProtobufInput.clientVersion, profiles, null)
                .getTwo()
        );

        LOGGER.info(new LogInfo(profiles, interactive ? LoggingEventType.GENERATE_PROTOBUF_CODE_INTERACTIVE_STOP :
            LoggingEventType.GENERATE_PROTOBUF_CODE_STOP, (double) System.currentTimeMillis() - start).toString());
        return generationOutputs;
    }

    private List<GenerationOutput> exec(ProtobufGenerationConfig protobufConfig, Function0<PureModel> pureModelFunc)
    {
        PureModel pureModel = pureModelFunc.value();
        RichIterable<? extends Root_meta_pure_generation_metamodel_GenerationOutput>
            result = core_external_format_protobuf_deprecated
            .Root_meta_external_format_protobuf_deprecated_generation_internal_transform_ProtobufConfig_1__GenerationOutput_MANY_(
                protobufConfig.transformToPure(pureModel), pureModel.getExecutionSupport());
        return result.collect(v -> new GenerationOutput(v._content(), v._fileName(), v._format())).toList();
    }
}
