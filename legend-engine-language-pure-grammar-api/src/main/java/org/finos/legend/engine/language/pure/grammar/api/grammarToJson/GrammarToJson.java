//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

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
@Path("pure/v1/grammar/grammarToJson")
public class GrammarToJson extends GrammarAPI
{
    @POST
    @Path("model")
    @ApiOperation(value = "Generates Pure protocol JSON from Pure language text")
    @Consumes({MediaType.TEXT_PLAIN, APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response model(String text,
                          @DefaultValue("") @ApiParam("The source ID to be used by the parser") @QueryParam("sourceId") String sourceId,
                          @DefaultValue("0") @ApiParam("The line number the parser will offset by") @QueryParam("lineOffset") int lineOffset,
                          @DefaultValue("true") @QueryParam("returnSourceInformation") boolean returnSourceInformation,
                          @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        PureGrammarParserExtensions.logExtensionList();
        return grammarToJson(text, (a) -> PureGrammarParser.newInstance().parseModel(a, sourceId, lineOffset, 0, returnSourceInformation), pm, "Grammar to Json : Model");
    }

    @POST
    @Path("lambda")
    @ApiOperation(value = "Generates Pure protocol JSON from Pure language text")
    @Consumes({MediaType.TEXT_PLAIN, APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response lambda(String text,
                           @DefaultValue("") @ApiParam("The source ID to be used by the parser") @QueryParam("sourceId") String sourceId,
                           @DefaultValue("0") @ApiParam("The line number the parser will offset by") @QueryParam("lineOffset") int lineOffset,
                           @DefaultValue("0") @ApiParam("The column number the parser will offset by") @QueryParam("columnOffset") int columnOffset,
                           @DefaultValue("true") @QueryParam("returnSourceInformation") boolean returnSourceInformation,
                           @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        PureGrammarParserExtensions.logExtensionList();
        return grammarToJson(text, (a) -> PureGrammarParser.newInstance().parseLambda(a, sourceId, lineOffset, columnOffset, returnSourceInformation), pm, "Grammar to Json : Lambda");
    }

    // Required so that Jackson properly includes _type for the top level element
    private static class TypedMap extends UnifiedMap<String, Lambda>
    {
        public TypedMap()
        {
        }
    }

    @POST
    @Path("lambda/batch")
    @ApiOperation(value = "Generates Pure protocol JSON from Pure language text")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response lambdaBatch(Map<String, ParserInput> input, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        PureGrammarParserExtensions.logExtensionList();
        return grammarToJsonBatch(input, (a, b, c, d, e) -> PureGrammarParser.newInstance().parseLambda(a, b, c, d, e), new TypedMap(), pm, "Grammar to Json : Lambda Batch");
    }

    @POST
    @Path("graphFetch")
    @ApiOperation(value = "Generates Pure protocol JSON from Pure language text")
    @Consumes({MediaType.TEXT_PLAIN, APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response graphFetch(String text,
                               @DefaultValue("") @ApiParam("The source ID to be used by the parser") @QueryParam("sourceId") String sourceId,
                               @DefaultValue("0") @ApiParam("The line number the parser will offset by") @QueryParam("lineOffset") int lineOffset,
                               @DefaultValue("0") @ApiParam("The column number the parser will offset by") @QueryParam("columnOffset") int columnOffset,
                               @DefaultValue("true") @QueryParam("returnSourceInformation") boolean returnSourceInformation,
                               @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        PureGrammarParserExtensions.logExtensionList();
        return grammarToJson(text, (a) -> PureGrammarParser.newInstance().parseGraphFetch(a, sourceId, lineOffset, columnOffset, returnSourceInformation), pm, "Grammar to Json : GraphFetch");
    }

    // Required so that Jackson properly includes _type for the top level element
    private static class TypedMapGraph extends UnifiedMap<String, RootGraphFetchTree>
    {
        public TypedMapGraph()
        {
        }
    }

    @POST
    @Path("graphFetch/batch")
    @ApiOperation(value = "Generates Pure protocol JSON from Pure language text")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response graphFetchBatch(Map<String, ParserInput> input, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        PureGrammarParserExtensions.logExtensionList();
        return grammarToJsonBatch(input, (a, b, c, d, e) -> PureGrammarParser.newInstance().parseGraphFetch(a, b, c, d, e), new TypedMapGraph(), pm, "Grammar to Json : GraphFetch Batch");
    }


    @POST
    @Path("valueSpecification")
    @ApiOperation(value = "Generates Pure protocol JSON from Pure language text")
    @Consumes({MediaType.TEXT_PLAIN, APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response valueSpecification(String text,
                                       @DefaultValue("") @ApiParam("The source ID to be used by the parser") @QueryParam("sourceId") String sourceId,
                                       @DefaultValue("0") @ApiParam("The line number the parser will offset by") @QueryParam("lineOffset") int lineOffset,
                                       @DefaultValue("0") @ApiParam("The column number the parser will offset by") @QueryParam("columnOffset") int columnOffset,
                                       @DefaultValue("true") @QueryParam("returnSourceInformation") boolean returnSourceInformation,
                                       @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        PureGrammarParserExtensions.logExtensionList();
        return grammarToJson(text, (a) -> PureGrammarParser.newInstance().parseValueSpecification(a, sourceId, lineOffset, columnOffset, returnSourceInformation), pm, "Grammar to Json : Value Specification");
    }

    // Required so that Jackson properly includes _type for the top level element
    private static class TypedMapVS extends UnifiedMap<String, ValueSpecification>
    {
        public TypedMapVS()
        {
        }
    }

    @POST
    @Path("valueSpecification/batch")
    @ApiOperation(value = "Generates Pure protocol JSON from Pure language text")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response valueSpecificationBatch(Map<String, ParserInput> input, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        PureGrammarParserExtensions.logExtensionList();
        return grammarToJsonBatch(input, (a, b, c, d, e) -> PureGrammarParser.newInstance().parseValueSpecification(a, b, c, d, e), new TypedMapVS(), pm, "Grammar to Json : Value Specification Batch");
    }
}
