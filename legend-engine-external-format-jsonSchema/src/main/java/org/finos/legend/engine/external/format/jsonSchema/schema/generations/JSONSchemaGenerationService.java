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

package org.finos.legend.engine.external.format.jsonSchema.schema.generations;

import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.external.shared.format.generations.GenerationOutput;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.api.result.ManageConstantResult;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.kerberos.SubjectTools;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.finos.legend.pure.generated.core_external_format_json_jsonSchema;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

import static org.finos.legend.engine.shared.core.operational.http.InflateInterceptor.APPLICATION_ZLIB;

@Deprecated
@Api(tags = "Z - Deprecated - Generation - Schema")
@Path("pure/v1/schemaGeneration")
@Produces(MediaType.APPLICATION_JSON)
@Singleton
public class JSONSchemaGenerationService
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Legend Execution Server");
    private final ModelManager modelManager;

    @Inject
    public JSONSchemaGenerationService(ModelManager modelManager)
    {
        this.modelManager = modelManager;
    }

    @Deprecated
    @POST
    @Path("jsonSchema")
    @ApiOperation(value = "Generates JSON schema for a given class and transitive dependencies")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    public Response generateJSONSchema(GenerateJSONSchemaInput generateJSONSchemaInput, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        String user = SubjectTools.getPrincipal(ProfileManagerHelper.extractSubject(profiles));
        boolean interactive = generateJSONSchemaInput.model instanceof PureModelContextData;
        try (Scope scope = GlobalTracer.get().buildSpan("Service: Generate JSON Schema").startActive(true))
        {
            return exec(generateJSONSchemaInput.config != null ? generateJSONSchemaInput.config : new JSONSchemaConfig(),
                    () -> this.modelManager.loadModel(generateJSONSchemaInput.model, generateJSONSchemaInput.clientVersion, profiles, null),
                    interactive,
                    profiles);
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, interactive ? LoggingEventType.GENERATE_JSONSCHEMA_INTERACTIVE_ERROR : LoggingEventType.GENERATE_JSONSCHEMA_ERROR, profiles);
        }
    }

    public static List<GenerationOutput> generate(JSONSchemaConfig jsonSchemaConfig, PureModel pureModel)
    {
        return core_external_format_json_jsonSchema.Root_meta_json_schema_generation_generateJsonSchemaFromPureWithScope_JSONSchemaConfig_1__JSONSchemaOutput_MANY_(jsonSchemaConfig.process(pureModel), pureModel.getExecutionSupport()).collect(v -> new GenerationOutput(v._content(), v._fileName(), v._format())).toList();
    }

    private Response exec(JSONSchemaConfig jsonSchemaConfig, Function0<PureModel> pureModelFunc, boolean interactive, MutableList<CommonProfile> profiles)
    {
        String user = SubjectTools.getPrincipal(ProfileManagerHelper.extractSubject(profiles));

        try
        {
            long start = System.currentTimeMillis();
            LOGGER.info(new LogInfo(user, interactive ? LoggingEventType.GENERATE_JSONSCHEMA_INTERACTIVE_START : LoggingEventType.GENERATE_JSONSCHEMA_START).toString());
            PureModel pureModel = pureModelFunc.value();
            List<GenerationOutput> result = generate(jsonSchemaConfig, pureModel);
            LOGGER.info(new LogInfo(user, interactive ? LoggingEventType.GENERATE_JSONSCHEMA_INTERACTIVE_STOP : LoggingEventType.GENERATE_JSONSCHEMA_START, (double) System.currentTimeMillis() - start).toString());
            return ManageConstantResult.manageResult(user, result);
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, interactive ? LoggingEventType.GENERATE_JSONSCHEMA_INTERACTIVE_ERROR : LoggingEventType.GENERATE_JSONSCHEMA_ERROR, profiles);
        }
    }
}

