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

package org.finos.legend.engine.external.shared.format.model.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.external.shared.format.model.ExternalFormatExtension;
import org.finos.legend.engine.external.shared.format.model.ExternalFormatExtensionLoader;
import org.finos.legend.engine.external.shared.format.model.fromModel.ModelToSchemaGenerator;
import org.finos.legend.engine.external.shared.format.model.toModel.SchemaToModelGenerator;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.api.result.ManageConstantResult;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;
import org.slf4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.finos.legend.engine.shared.core.operational.http.InflateInterceptor.APPLICATION_ZLIB;

@Api(tags = "External - Format")
@Path("pure/v1/external/format")
@Produces(MediaType.APPLICATION_JSON)
public class ExternalFormats
{
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Legend Execution Server");
    static final Map<String, ExternalFormatExtension> extensions = ExternalFormatExtensionLoader.extensions();

    private final ModelManager modelManager;

    public ExternalFormats(ModelManager modelManager)
    {
        this.modelManager = modelManager;
    }

    @GET
    @Path("availableFormats")
    @ApiOperation(value = "Get all external formats available")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response codeGenerationDescriptions()
    {
        try
        {
            PureModel pureModel = this.modelManager.loadModelAndData(PureModelContextData.newPureModelContextData(), null, null, null).getTwo();
            List<ExternalFormatDescription> descriptions = extensions.values().stream()
                    .map(ext -> ExternalFormatDescription.newDescription(ext, pureModel))
                    .collect(Collectors.toList());
            return ManageConstantResult.manageResult(null, descriptions);
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, LoggingEventType.EXTERNAL_FORMAT_ERROR, null);
        }
    }

    @POST
    @Path("generateModel")
    @ApiOperation(value = "Generates a model from a schema for the external format")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    public Response generateModel(GenerateModelInput generateModelInput, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        boolean interactive = generateModelInput.model instanceof PureModelContextData;
        try (Scope scope = GlobalTracer.get().buildSpan("Service: Generate Model From External Format Schema").startActive(true))
        {
            long start = System.currentTimeMillis();
            LOGGER.info(new LogInfo(profiles, interactive ? LoggingEventType.GENERATE_EXTERNAL_FORMAT_MODEL_INTERACTIVE_START : LoggingEventType.GENERATE_EXTERNAL_FORMAT_MODEL_START).toString());
            ExternalFormatExtension extension = extensions.get(generateModelInput.config.format);
            if (!extension.supportsModelGeneration())
            {
                throw new UnsupportedOperationException("Model generation not supported for " + extension.getFormat());
            }
            PureModel pureModel = this.modelManager.loadModel(generateModelInput.model, generateModelInput.clientVersion, profiles, null);
            SchemaToModelGenerator generator = new SchemaToModelGenerator(pureModel, generateModelInput.clientVersion);
            PureModelContextData generated = generator.generate(generateModelInput.config);
            LOGGER.info(new LogInfo(profiles, interactive ? LoggingEventType.GENERATE_EXTERNAL_FORMAT_MODEL_INTERACTIVE_STOP : LoggingEventType.GENERATE_EXTERNAL_FORMAT_MODEL_STOP, (double) System.currentTimeMillis() - start).toString());
            return ManageConstantResult.manageResult(profiles, generated, objectMapper);
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, interactive ? LoggingEventType.GENERATE_EXTERNAL_FORMAT_MODEL_INTERACTIVE_ERROR : LoggingEventType.GENERATE_EXTERNAL_FORMAT_MODEL_ERROR, profiles);
        }
    }

    @POST
    @Path("generateSchema")
    @ApiOperation(value = "Generates a schema in an external format from a model")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    public Response generateSchema(GenerateSchemaInput generateSchemaInput, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        boolean interactive = generateSchemaInput.model instanceof PureModelContextData;
        try (Scope scope = GlobalTracer.get().buildSpan("Service: Generate Model From External Format Schema").startActive(true))
        {
            long start = System.currentTimeMillis();
            LOGGER.info(new LogInfo(profiles, interactive ? LoggingEventType.GENERATE_EXTERNAL_FORMAT_SCHEMA_INTERACTIVE_START : LoggingEventType.GENERATE_EXTERNAL_FORMAT_SCHEMA_START).toString());
            if (generateSchemaInput.config == null || generateSchemaInput.config.format == null)
            {
                throw new EngineException("Please provide a Format");
            }
            ExternalFormatExtension extension = extensions.get(generateSchemaInput.config.format);
            if (extension == null)
            {
                throw new UnsupportedOperationException("Can't find an extension supporting the external format " + generateSchemaInput.config.format);
            }
            if (!extension.supportsSchemaGeneration())
            {
                throw new UnsupportedOperationException("Model generation not supported for " + extension.getFormat());
            }
            if (generateSchemaInput.model == null)
            {
                throw new UnsupportedOperationException("Please provide a PureModelContext");
            }
            PureModel pureModel = this.modelManager.loadModel(generateSchemaInput.model, generateSchemaInput.clientVersion, profiles, null);
            ModelToSchemaGenerator generator = new ModelToSchemaGenerator(pureModel);
            PureModelContextData generated = generator.generate(generateSchemaInput.config);
            LOGGER.info(new LogInfo(profiles, interactive ? LoggingEventType.GENERATE_EXTERNAL_FORMAT_SCHEMA_INTERACTIVE_STOP : LoggingEventType.GENERATE_EXTERNAL_FORMAT_SCHEMA_STOP, (double) System.currentTimeMillis() - start).toString());
            return ManageConstantResult.manageResult(profiles, generated, objectMapper);
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, interactive ? LoggingEventType.GENERATE_EXTERNAL_FORMAT_SCHEMA_INTERACTIVE_ERROR : LoggingEventType.GENERATE_EXTERNAL_FORMAT_SCHEMA_ERROR, profiles);
        }
    }
}