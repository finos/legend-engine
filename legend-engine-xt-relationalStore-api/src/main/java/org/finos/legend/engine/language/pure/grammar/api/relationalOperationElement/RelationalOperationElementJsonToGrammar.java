package org.finos.legend.engine.language.pure.grammar.api.relationalOperationElement;

import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.MapIterate;
import org.finos.legend.engine.language.pure.grammar.to.RelationalGrammarComposerExtension;
import org.finos.legend.engine.language.pure.grammar.to.extension.PureGrammarComposerExtensionLoader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.operation.RelationalOperationElement;
import org.finos.legend.engine.shared.core.api.grammar.RenderStyle;
import org.finos.legend.engine.shared.core.api.result.ManageConstantResult;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

import static org.finos.legend.engine.shared.core.operational.http.InflateInterceptor.APPLICATION_ZLIB;

@Api(tags = "Pure - Grammar")
@Path("pure/v1/grammar/jsonToGrammar")
public class RelationalOperationElementJsonToGrammar
{
    @POST
    @Path("relationalOperationElement")
    @ApiOperation(value = "Generates Pure language text from Pure protocol JSON for relational operation elements")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Produces(MediaType.TEXT_PLAIN)
    public Response relationalOperationElement(RelationalOperationElement input,
                                               @QueryParam("renderStyle") @DefaultValue("PRETTY") RenderStyle renderStyle,
                                               @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        try (Scope scope = GlobalTracer.get().buildSpan("Service: jsonToGrammar relationalOperationElement").startActive(true))
        {
            PureGrammarComposerExtensionLoader.logExtensionList();
            return Response.ok(RelationalGrammarComposerExtension.renderRelationalOperationElement(input)).build();
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, LoggingEventType.TRANSFORM_RELATIONAL_OPERATION_ELEMENT_JSON_TO_GRAMMAR_ERROR, profiles);
        }
    }

    @POST
    @Path("relationalOperationElement/batch")
    @ApiOperation(value = "Generates Pure language text from Pure protocol JSON for relational operation elements")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response relationalOperationElementBatch(Map<String, RelationalOperationElement> input,
                                                    @QueryParam("renderStyle") @DefaultValue("PRETTY") RenderStyle renderStyle,
                                                    @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        try (Scope scope = GlobalTracer.get().buildSpan("Service: jsonToGrammar relationalOperationElement").startActive(true))
        {
            PureGrammarComposerExtensionLoader.logExtensionList();
            // TODO: support for render style option - this has to be implemented at the grammar composer layer first
            MutableMap<String, String> result = Maps.mutable.empty();
            MapIterate.toListOfPairs(input)
                    .collect(p -> Tuples.pair(p.getOne(), RelationalGrammarComposerExtension.renderRelationalOperationElement(p.getTwo())))
                    .forEach((Procedure<Pair<String, String>>) p -> result.put(p.getOne(), p.getTwo()));
            return ManageConstantResult.manageResult(profiles, result);
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, LoggingEventType.TRANSFORM_RELATIONAL_OPERATION_ELEMENT_JSON_TO_GRAMMAR_ERROR, profiles);
        }
    }
}
