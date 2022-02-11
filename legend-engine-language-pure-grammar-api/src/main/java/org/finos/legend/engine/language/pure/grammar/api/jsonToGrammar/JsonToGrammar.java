package org.finos.legend.engine.language.pure.grammar.api.jsonToGrammar;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.finos.legend.engine.language.pure.grammar.to.DEPRECATED_PureGrammarComposerCore;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposer;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.language.pure.grammar.to.extension.PureGrammarComposerExtensionLoader;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.graph.RootGraphFetchTree;
import org.finos.legend.engine.shared.core.api.grammar.GrammarAPI;
import org.finos.legend.engine.shared.core.api.grammar.RenderStyle;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

import static org.finos.legend.engine.shared.core.operational.http.InflateInterceptor.APPLICATION_ZLIB;

@Api(tags = "Pure - Grammar")
@Path("pure/v1/grammar/jsonToGrammar")
public class JsonToGrammar extends GrammarAPI
{
    @POST
    @Path("graphFetch/{renderStyle}")
    @ApiOperation(value = "Generates Pure language text from Pure protocol GraphFetch fragment")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Produces(MediaType.TEXT_PLAIN)
    public Response graphFetch(@PathParam("renderStyle") @DefaultValue("PRETTY") RenderStyle renderStyle, RootGraphFetchTree graphFetchTree, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        PureGrammarComposerExtensionLoader.logExtensionList();
        return jsonToGrammar(graphFetchTree, renderStyle, (vs, renderStyle1) -> vs.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance().withRenderStyle(renderStyle1).build()), pm, "Json to Grammar : Graph Fetch");
    }

    @POST
    @Path("graphFetch/{renderStyle}/batch")
    @ApiOperation(value = "Generates Pure language text from Pure protocol GraphFetch fragment")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response graphFetchBatch(@PathParam("renderStyle") @DefaultValue("PRETTY") RenderStyle renderStyle, Map<String, RootGraphFetchTree> graphFetchTrees, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        PureGrammarComposerExtensionLoader.logExtensionList();
        return jsonToGrammarBatch(renderStyle, graphFetchTrees, (vs, renderStyle1) -> vs.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance().withRenderStyle(renderStyle1).build()), pm, "Json to Grammar : Graph Fetch Batch");
    }

    @POST
    @Path("valueSpecification/{renderStyle}")
    @ApiOperation(value = "Generates Pure language text from Pure protocol Value Specification fragment")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Produces(MediaType.TEXT_PLAIN)
    public Response valueSpecification(@PathParam("renderStyle") @DefaultValue("PRETTY") RenderStyle renderStyle, ValueSpecification valueSpecification, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        PureGrammarComposerExtensionLoader.logExtensionList();
        return jsonToGrammar(valueSpecification, renderStyle, (vs, renderStyle1) -> vs.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance().withRenderStyle(renderStyle1).build()), pm, "Json to Grammar : Value Specification");
    }

    @POST
    @Path("valueSpecification/{renderStyle}/batch")
    @ApiOperation(value = "Generates Pure language text from Pure protocol Value Specification fragment")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response valueSpecificationBatch(@PathParam("renderStyle") @DefaultValue("PRETTY") RenderStyle renderStyle, Map<String, ValueSpecification> valueSpecifications, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        PureGrammarComposerExtensionLoader.logExtensionList();
        return jsonToGrammarBatch(renderStyle, valueSpecifications, (vs, renderStyle1) -> vs.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance().withRenderStyle(renderStyle1).build()), pm, "Json to Grammar : Value Specification Batch");
    }

    @POST
    @Path("lambda/{renderStyle}")
    @ApiOperation(value = "Generates Pure language text from Pure protocol Value Specification fragment")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Produces(MediaType.TEXT_PLAIN)
    public Response lambda(@PathParam("renderStyle") @DefaultValue("PRETTY") RenderStyle renderStyle, Lambda lambda, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        PureGrammarComposerExtensionLoader.logExtensionList();
        return jsonToGrammar(lambda, renderStyle, (vs, renderStyle1) -> vs.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance().withRenderStyle(renderStyle1).build()), pm, "Json to Grammar : Lambda");
    }

    @POST
    @Path("lambda/{renderStyle}/batch")
    @ApiOperation(value = "Generates Pure language text from Pure protocol Value Specification fragment")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response lambdaBatch(@PathParam("renderStyle") @DefaultValue("PRETTY") RenderStyle renderStyle, Map<String, Lambda> lambdas, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        PureGrammarComposerExtensionLoader.logExtensionList();
        return jsonToGrammarBatch(renderStyle, lambdas, (vs, renderStyle1) -> vs.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance().withRenderStyle(renderStyle1).build()), pm, "Json to Grammar : Lambda Batch");
    }

    @POST
    @Path("model/{renderStyle}")
    @ApiOperation(value = "Generates Pure language text from Pure protocol Pure Model Context Data")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Produces(MediaType.TEXT_PLAIN)
    public Response model(@PathParam("renderStyle") @DefaultValue("PRETTY") RenderStyle renderStyle, PureModelContextData pureModelContext, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        PureGrammarComposerExtensionLoader.logExtensionList();
        return jsonToGrammar(pureModelContext, renderStyle, (value, renderStyle1) -> PureGrammarComposer.newInstance(PureGrammarComposerContext.Builder.newInstance().withRenderStyle(renderStyle1).build()).renderPureModelContextData(value), pm, "Json to Grammar : Model");
    }

}
