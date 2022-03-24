package org.finos.legend.engine.language.pure.grammar.api.relationalOperationElement;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.language.pure.grammar.from.RelationalGrammarParserExtension;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtensions;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.operation.RelationalOperationElement;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.api.grammar.GrammarAPI;
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
import java.util.Map;

import static org.finos.legend.engine.shared.core.operational.http.InflateInterceptor.APPLICATION_ZLIB;

@Api(tags = "Pure - Grammar")
@Path("pure/v1/grammar/grammarToJson")
public class RelationalOperationElementGrammarToJson extends GrammarAPI
{
    @POST
    @Path("relationalOperationElement")
    @ApiOperation(value = "Generates Pure protocol JSON from Pure language text for relational operation elements")
    @Consumes({MediaType.TEXT_PLAIN, APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response relationalOperationElement(String input, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm, @QueryParam("returnSourceInfo") boolean returnSourceInfo)
    {
        PureGrammarParserExtensions.logExtensionList();
        return grammarToJson(input, (a, b) -> RelationalGrammarParserExtension.parseRelationalOperationElement(a, "", b), pm, returnSourceInfo, "Grammar to Json : RelationalOperationElement");
    }

    // Required so that Jackson properly includes _type for the top level element
    static class TypedMap extends UnifiedMap<String, RelationalOperationElement>
    {}

    @POST
    @Path("relationalOperationElement/batch")
    @ApiOperation(value = "Generates Pure protocol JSON from Pure language text for relational operation elements")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response relationalOperationElementBatch(Map<String, String> input, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm, @QueryParam("returnSourceInfo") boolean returnSourceInfo)
    {
        PureGrammarParserExtensions.logExtensionList();
        return grammarToJsonBatch(input,
            RelationalGrammarParserExtension::parseRelationalOperationElement, new TypedMap(), pm, returnSourceInfo, "Grammar to Json : RelationalOperationElement Batch");
    }
}
