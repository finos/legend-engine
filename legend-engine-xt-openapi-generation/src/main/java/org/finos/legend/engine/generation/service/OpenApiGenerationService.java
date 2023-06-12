package org.finos.legend.engine.generation.service;

import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.engine.generation.model.OpenApiGenerationInput;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.plan.execution.service.ServiceModeling;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Service;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.http.InflateInterceptor;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.finos.legend.pure.generated.Root_meta_external_format_openapi_metamodel_Server_Impl;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_Service;
import org.finos.legend.pure.generated.core_external_format_openapi_generation;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Api(tags = "Generation - Code")
@Path("pure/v1/codeGeneration")
@Produces(MediaType.APPLICATION_JSON)
public class OpenApiGenerationService {
    private static final Logger LOGGER = LoggerFactory.getLogger("Alloy Execution Server");
    private final ModelManager modelManager;

    public OpenApiGenerationService(ModelManager modelManager) {
        this.modelManager = modelManager;
    }

    @POST
    @Path("openapi")
    @ApiOperation(value = "Generates OpenAPI specification from Pure model context")
    @Consumes({MediaType.APPLICATION_JSON, InflateInterceptor.APPLICATION_ZLIB})
    public Response generateOpenApi(OpenApiGenerationInput generationInput, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        boolean interactive = generationInput.model instanceof PureModelContextData;
        try (Scope scope = GlobalTracer.get().buildSpan("Service: Generate OpenAPI Spec").startActive(true))
        {
            return exec(
                    generationInput.model,
                    () -> this.modelManager.loadModelAndData(generationInput.model, generationInput.clientVersion, profiles, null).getTwo(),
                    interactive,
                    profiles
            );
        } catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, interactive ? LoggingEventType.GENERATE_EXTERNAL_FORMAT_SCHEMA_INTERACTIVE_ERROR : LoggingEventType.GENERATE_EXTERNAL_FORMAT_SCHEMA_ERROR, profiles);
        }
    }

    private Response exec(PureModelContext context, Function0<PureModel> pureModelFunc, boolean interactive, MutableList<CommonProfile> profiles)
    {
        try
        {
            long start = System.currentTimeMillis();
            LOGGER.info(new LogInfo(profiles, interactive ? LoggingEventType.GENERATE_EXTERNAL_FORMAT_SCHEMA_INTERACTIVE_ERROR: LoggingEventType.GENERATE_EXTERNAL_FORMAT_SCHEMA_ERROR).toString());
            PureModel pureModel = pureModelFunc.value();
            PureModelContextData data = this.modelManager.loadData(context, null, profiles);
            org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Service service = findService(data);
            Root_meta_legend_service_metamodel_Service pureService = new ServiceModeling(modelManager, pureModel.getDeploymentMode(),null).compileService(service, pureModel.getContext(service));
            String result = core_external_format_openapi_generation.Root_meta_external_format_openapi_generation_serviceToOpenApi_Service_1__Server_1__String_1_(pureService, new Root_meta_external_format_openapi_metamodel_Server_Impl("${HOST}"), pureModel.getExecutionSupport();
            long end = System.currentTimeMillis();
            LOGGER.debug("OpenAPI generation from pureModelContext completed in {} ms", end-start);
            return Response.accepted(result).build();
        } catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, interactive? LoggingEventType.GENERATE_EXTERNAL_FORMAT_SCHEMA_INTERACTIVE_ERROR: LoggingEventType.GENERATE_EXTERNAL_FORMAT_SCHEMA_ERROR,profiles);
        }
        
    }

    private Service findService(PureModelContextData data) {
        return (Service) Iterate.detect(data.getElements(), e -> e instanceof Service);
    }
}
