package org.finos.legend.engine.language.pure.grammar.api.grammarToJson;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtensions;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.graph.RootGraphFetchTree;
import org.finos.legend.engine.shared.core.api.grammar.GrammarAPI;
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
public class GrammarToJson extends GrammarAPI
{
    @POST
    @Path("model")
    @ApiOperation(value = "Generates Pure protocol JSON from Pure language text")
    @Consumes({MediaType.TEXT_PLAIN, APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response model(String input, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm, @DefaultValue("true") @QueryParam("returnSourceInfo") boolean returnSourceInfo)
    {
        PureGrammarParserExtensions.logExtensionList();
        return grammarToJson(input, (a, b) -> PureGrammarParser.newInstance().parseModel(a, b), pm, returnSourceInfo, "Grammar to Json : Model");
    }

    @POST
    @Path("lambda")
    @ApiOperation(value = "Generates Pure protocol JSON from Pure language text")
    @Consumes({MediaType.TEXT_PLAIN, APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response lambda(String input, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm, @DefaultValue ("true") @QueryParam("returnSourceInfo") boolean returnSourceInfo)
    {
        PureGrammarParserExtensions.logExtensionList();
        return grammarToJson(input, (a, b) -> PureGrammarParser.newInstance().parseLambda(a, "", b), pm, returnSourceInfo, "Grammar to Json : Lambda");
    }

    // Required so that Jackson properly includes _type for the top level element
    private static class TypedMap extends UnifiedMap<String,Lambda>{
        public TypedMap()
        {
        }
    }

    @POST
    @Path("lambda/batch")
    @ApiOperation(value = "Generates Pure protocol JSON from Pure language text")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response lambdaBatch(Map<String, String> input, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm, @DefaultValue ("true") @QueryParam("returnSourceInfo") boolean returnSourceInfo)
    {
        PureGrammarParserExtensions.logExtensionList();
        return grammarToJsonBatch(input, (a, b)-> PureGrammarParser.newInstance().parseLambda(a, "", b), new TypedMap(), pm, returnSourceInfo, "Grammar to Json : Lambda Batch");
    }

    @POST
    @Path("graphFetch")
    @ApiOperation(value = "Generates Pure protocol JSON from Pure language text")
    @Consumes({MediaType.TEXT_PLAIN, APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response graphFetch(String input, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm, @DefaultValue ("true") @QueryParam("returnSourceInfo") boolean returnSourceInfo)
    {
        PureGrammarParserExtensions.logExtensionList();
        return grammarToJson(input, (a, b) -> PureGrammarParser.newInstance().parseGraphFetch(a, "", b), pm, returnSourceInfo, "Grammar to Json : GraphFetch");
    }

    // Required so that Jackson properly includes _type for the top level element
    private static class TypedMapGraph extends UnifiedMap<String, RootGraphFetchTree>{
        public TypedMapGraph()
        {
        }
    }

    @POST
    @Path("graphFetch/batch")
    @ApiOperation(value = "Generates Pure protocol JSON from Pure language text")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response graphFetchBatch(Map<String, String> input, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm, @DefaultValue ("true") @QueryParam("returnSourceInfo") boolean returnSourceInfo)
    {
        PureGrammarParserExtensions.logExtensionList();
        return grammarToJsonBatch(input, (a, b)-> PureGrammarParser.newInstance().parseGraphFetch(a, "", b), new TypedMapGraph(), pm, returnSourceInfo, "Grammar to Json : GraphFetch Batch");
    }


    @POST
    @Path("valueSpecification")
    @ApiOperation(value = "Generates Pure protocol JSON from Pure language text")
    @Consumes({MediaType.TEXT_PLAIN, APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response valueSpecification(String input, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm, @DefaultValue ("true") @QueryParam("returnSourceInfo") boolean returnSourceInfo)
    {
        PureGrammarParserExtensions.logExtensionList();
        return grammarToJson(input, (a, b) -> PureGrammarParser.newInstance().parseValueSpecification(a, "", b), pm, returnSourceInfo, "Grammar to Json : Value Specification");
    }

    // Required so that Jackson properly includes _type for the top level element
    private static class TypedMapVS extends UnifiedMap<String, ValueSpecification>{
        public TypedMapVS()
        {
        }
    }

    @POST
    @Path("valueSpecification/batch")
    @ApiOperation(value = "Generates Pure protocol JSON from Pure language text")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response valueSpecificationBatch(Map<String, String> input, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm, @DefaultValue ("true") @QueryParam("returnSourceInfo") boolean returnSourceInfo)
    {
        PureGrammarParserExtensions.logExtensionList();
        return grammarToJsonBatch(input, (a, b)-> PureGrammarParser.newInstance().parseValueSpecification(a, "", b), new TypedMapVS(), pm, returnSourceInfo, "Grammar to Json : Value Specification Batch");
    }
}
