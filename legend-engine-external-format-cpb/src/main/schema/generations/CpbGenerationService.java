package org.finos.legend.engine.external.language.cpb.schema.generations;

import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.list.MutableList;
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
import org.finos.legend.pure.generated.core_external_format_cpb_transformation_cpbSchemaGenerator;
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

@Api(tags = "Z - Deprecated - Generation - Schema")
@Path("pure/v1/schemaGeneration")
@Produces(MediaType.APPLICATION_JSON)
public class CpbGenerationService
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");
    private final ModelManager modelManager;

    public CpbGenerationService(ModelManager modelManager)
    {
        this.modelManager = modelManager;
    }

    @POST
    @Path("cpb")
    @ApiOperation(value = "Generates Cpb")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    public Response generateCpb(CpbGenerationInput generateCpbInput, @ApiParam(hidden = true)  @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        boolean interactive = generateCpbInput.model instanceof PureModelContextData;
        try (Scope scope = GlobalTracer.get().buildSpan("Service: Generate Cpb").startActive(true))
        {
            return exec(
                    generateCpbInput.config != null ? generateCpbInput.config : new CpbGenerationConfig(),
                    () -> this.modelManager.loadModelAndData(generateCpbInput.model, generateCpbInput.clientVersion, profiles, null).getTwo(),
                    interactive,
                    profiles);
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, interactive ? LoggingEventType.GENERATE_Cpb_CODE_INTERACTIVE_ERROR : LoggingEventType.GENERATE_Cpb_CODE_ERROR, profiles);
        }
    }

    private Response exec(CpbGenerationConfig cpbConfig, Function0<PureModel> pureModelFunc, boolean interactive, MutableList<CommonProfile> pm)
    {
        try
        {
            long start = System.currentTimeMillis();
            LOGGER.info(new LogInfo(pm, interactive ? LoggingEventType.GENERATE_Cpb_CODE_INTERACTIVE_START : LoggingEventType.GENERATE_Cpb_CODE_START).toString());
            PureModel pureModel = pureModelFunc.value();
            RichIterable<? extends Root_meta_pure_generation_metamodel_GenerationOutput> output = core_external_format_cpb_transformation_cpbSchemaGenerator.Root_meta_external_format_cpb_generation_generateCpbFromPureWithScope_CpbConfig_1__CpbOutput_MANY_(cpbConfig.process(pureModel), pureModel.getExecutionSupport());
            LOGGER.info(new LogInfo(pm, interactive ? LoggingEventType.GENERATE_Cpb_CODE_INTERACTIVE_STOP : LoggingEventType.GENERATE_Cpb_CODE_STOP, (double)System.currentTimeMillis() - start).toString());
            return ManageConstantResult.manageResult(pm, output.collect(v -> new GenerationOutput(v._content(), v._fileName(), v._format())).toList());
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, interactive ? LoggingEventType.GENERATE_Cpb_CODE_INTERACTIVE_ERROR : LoggingEventType.GENERATE_Cpb_CODE_ERROR, pm);
        }
    }
}