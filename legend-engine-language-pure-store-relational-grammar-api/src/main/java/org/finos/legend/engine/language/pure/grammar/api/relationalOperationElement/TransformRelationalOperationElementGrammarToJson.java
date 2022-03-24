package org.finos.legend.engine.language.pure.grammar.api.relationalOperationElement;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.grammar.from.RelationalGrammarParserExtension;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtensions;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.operation.RelationalOperationElement;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.api.grammar.ParserError;
import org.finos.legend.engine.shared.core.api.result.ManageConstantResult;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

import static org.finos.legend.engine.shared.core.operational.http.InflateInterceptor.APPLICATION_ZLIB;

@Deprecated
@Api(tags = "Pure - Grammar")
@Path("pure/v1/grammar")
@Produces(MediaType.APPLICATION_JSON)
public class TransformRelationalOperationElementGrammarToJson
{
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    @Deprecated
    @POST
    @Path("transformRelationalOperationElementGrammarToJson")
    @ApiOperation(value = "Generates Pure protocol JSON from Pure language text for relational operation elements")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    public Response transformRelationalOperationElementGrammarToJson(RelationalOperationElementGrammarToJsonInput input, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm, @QueryParam("returnSourceInfo") boolean returnSourceInfo)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        try (Scope scope = GlobalTracer.get().buildSpan("Service: transformRelationalOperationElementGrammarToJson").startActive(true))
        {
            PureGrammarParserExtensions.logExtensionList();
            Map<String, RelationalOperationElement> operations = new HashMap<>();
            Map<String, ParserError> operationErrors = new HashMap<>();
            input.operations.forEach((key, value) ->
            {
                try
                {
                    RelationalOperationElement operation = RelationalGrammarParserExtension.parseRelationalOperationElement(value, "", returnSourceInfo);
                    operations.put(key, operation);
                }
                catch (Exception e)
                {
                    if (e instanceof EngineException)
                    {
                        EngineException exception = (EngineException) e;
                        if (!EngineErrorType.PARSER.equals(exception.getErrorType()))
                        {
                            // throw non-parser error
                            throw e;
                        }
                        operationErrors.put(key, new ParserError(exception.getMessage(), exception.getSourceInformation()));
                    }
                    ExceptionTool.exceptionManager(e, LoggingEventType.TRANSFORM_RELATIONAL_OPERATION_ELEMENT_JSON_TO_GRAMMAR_ERROR, profiles);
                }
            });
            RelationalOperationElementJsonToGrammarInput symmetricResult = new RelationalOperationElementJsonToGrammarInput();
            symmetricResult.operations = operations;
            symmetricResult.operationErrors = operationErrors.size() > 0 ? operationErrors : null; // only show object is errors exist
            return ManageConstantResult.manageResult(profiles, symmetricResult, objectMapper);
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, LoggingEventType.TRANSFORM_RELATIONAL_OPERATION_ELEMENT_JSON_TO_GRAMMAR_ERROR, profiles);
        }
    }
}
