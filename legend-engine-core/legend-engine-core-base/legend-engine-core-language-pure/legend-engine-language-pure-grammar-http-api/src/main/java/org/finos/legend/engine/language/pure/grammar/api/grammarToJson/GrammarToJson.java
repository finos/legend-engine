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

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtensions;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.m3.function.LambdaFunction;
import org.finos.legend.engine.protocol.pure.dsl.graph.valuespecification.constant.classInstance.RootGraphFetchTree;
import org.finos.legend.engine.shared.core.api.grammar.GrammarAPI;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.prometheus.MetricsHandler;
import org.finos.legend.engine.shared.core.operational.prometheus.Prometheus;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.util.Map;

import static org.finos.legend.engine.shared.core.operational.http.InflateInterceptor.APPLICATION_ZLIB;

@Tag(name = "Pure - Grammar")
@Path("pure/v1/grammar/grammarToJson")
public class GrammarToJson extends GrammarAPI
{
    @POST
    @Path("model")
    @Operation(summary = "Generates Pure protocol JSON from Pure language text")
    @Consumes({MediaType.TEXT_PLAIN, APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    @Prometheus(name = "GrammarToJson model", doc = "Grammar to Json duration summary")
    public Response model(String text,
                          @DefaultValue("") @Parameter(description = "The source ID to be used by the parser") @QueryParam("sourceId") String sourceId,
                          @DefaultValue("0") @Parameter(description = "The line number the parser will offset by") @QueryParam("lineOffset") int lineOffset,
                          @DefaultValue("true") @QueryParam("returnSourceInformation") boolean returnSourceInformation,
                          @Parameter(hidden = true) @Pac4JProfileManager ProfileManager pm,
                          @Context UriInfo uriInfo)
    {
        long start = System.currentTimeMillis();
        PureGrammarParserExtensions.logExtensionList();
        Response response = grammarToJson(text, (a) -> PureGrammarParser.newInstance().parseModel(a, sourceId, lineOffset, 0, returnSourceInformation), Identity.makeIdentity(ProfileManagerHelper.extractProfiles(pm)), "Grammar to Json : Model");
        long end = System.currentTimeMillis();
        MetricsHandler.observeRequest(uriInfo != null ? uriInfo.getPath() : null, start, end);
        return response;
    }

    @Deprecated
    public Response model(String text, String sourceId, int lineOffset, boolean returnSourceInformation, ProfileManager pm)
    {
        return model(text, sourceId, lineOffset, returnSourceInformation, pm, null);
    }


    @POST
    @Path("lambda")
    @Operation(summary = "Generates Pure protocol JSON from Pure language text")
    @Consumes({MediaType.TEXT_PLAIN, APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response lambda(String text,
                           @DefaultValue("") @Parameter(description = "The source ID to be used by the parser") @QueryParam("sourceId") String sourceId,
                           @DefaultValue("0") @Parameter(description = "The line number the parser will offset by") @QueryParam("lineOffset") int lineOffset,
                           @DefaultValue("0") @Parameter(description = "The column number the parser will offset by") @QueryParam("columnOffset") int columnOffset,
                           @DefaultValue("true") @QueryParam("returnSourceInformation") boolean returnSourceInformation,
                           @Parameter(hidden = true) @Pac4JProfileManager ProfileManager pm)
    {
        PureGrammarParserExtensions.logExtensionList();
        return grammarToJson(text, (a) -> PureGrammarParser.newInstance().parseLambda(a, sourceId, lineOffset, columnOffset, returnSourceInformation), Identity.makeIdentity(ProfileManagerHelper.extractProfiles(pm)), "Grammar to Json : Lambda");
    }

    // Required so that Jackson properly includes _type for the top level element
    private static class TypedMap extends UnifiedMap<String, LambdaFunction>
    {
        public TypedMap()
        {
        }
    }

    @POST
    @Path("lambda/batch")
    @Operation(summary = "Generates Pure protocol JSON from Pure language text")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response lambdaBatch(Map<String, ParserInput> input, @Parameter(hidden = true) @Pac4JProfileManager ProfileManager pm)
    {
        PureGrammarParserExtensions.logExtensionList();
        return grammarToJsonBatch(input, (a, b, c, d, e) -> PureGrammarParser.newInstance().parseLambda(a, b, c, d, e), new TypedMap(), Identity.makeIdentity(ProfileManagerHelper.extractProfiles(pm)), "Grammar to Json : Lambda Batch");
    }

    @POST
    @Path("graphFetch")
    @Operation(summary = "Generates Pure protocol JSON from Pure language text")
    @Consumes({MediaType.TEXT_PLAIN, APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response graphFetch(String text,
                               @DefaultValue("") @Parameter(description = "The source ID to be used by the parser") @QueryParam("sourceId") String sourceId,
                               @DefaultValue("0") @Parameter(description = "The line number the parser will offset by") @QueryParam("lineOffset") int lineOffset,
                               @DefaultValue("0") @Parameter(description = "The column number the parser will offset by") @QueryParam("columnOffset") int columnOffset,
                               @DefaultValue("true") @QueryParam("returnSourceInformation") boolean returnSourceInformation,
                               @Parameter(hidden = true) @Pac4JProfileManager ProfileManager pm)
    {
        PureGrammarParserExtensions.logExtensionList();
        return grammarToJson(text, (a) -> PureGrammarParser.newInstance().parseGraphFetch(a, sourceId, lineOffset, columnOffset, returnSourceInformation), Identity.makeIdentity(ProfileManagerHelper.extractProfiles(pm)), "Grammar to Json : GraphFetch");
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
    @Operation(summary = "Generates Pure protocol JSON from Pure language text")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response graphFetchBatch(Map<String, ParserInput> input, @Parameter(hidden = true) @Pac4JProfileManager ProfileManager pm)
    {
        PureGrammarParserExtensions.logExtensionList();
        return grammarToJsonBatch(input, (a, b, c, d, e) -> PureGrammarParser.newInstance().parseGraphFetch(a, b, c, d, e), new TypedMapGraph(), Identity.makeIdentity(ProfileManagerHelper.extractProfiles(pm)), "Grammar to Json : GraphFetch Batch");
    }


    @POST
    @Path("valueSpecification")
    @Operation(summary = "Generates Pure protocol JSON from Pure language text")
    @Consumes({MediaType.TEXT_PLAIN, APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response valueSpecification(String text,
                                       @DefaultValue("") @Parameter(description = "The source ID to be used by the parser") @QueryParam("sourceId") String sourceId,
                                       @DefaultValue("0") @Parameter(description = "The line number the parser will offset by") @QueryParam("lineOffset") int lineOffset,
                                       @DefaultValue("0") @Parameter(description = "The column number the parser will offset by") @QueryParam("columnOffset") int columnOffset,
                                       @DefaultValue("true") @QueryParam("returnSourceInformation") boolean returnSourceInformation,
                                       @Parameter(hidden = true) @Pac4JProfileManager ProfileManager pm)
    {
        PureGrammarParserExtensions.logExtensionList();
        return grammarToJson(text, (a) -> PureGrammarParser.newInstance().parseValueSpecification(a, sourceId, lineOffset, columnOffset, returnSourceInformation), Identity.makeIdentity(ProfileManagerHelper.extractProfiles(pm)), "Grammar to Json : Value Specification");
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
    @Operation(summary = "Generates Pure protocol JSON from Pure language text")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response valueSpecificationBatch(Map<String, ParserInput> input, @Parameter(hidden = true) @Pac4JProfileManager ProfileManager pm)
    {
        PureGrammarParserExtensions.logExtensionList();
        return grammarToJsonBatch(input, (a, b, c, d, e) -> PureGrammarParser.newInstance().parseValueSpecification(a, b, c, d, e), new TypedMapVS(), Identity.makeIdentity(ProfileManagerHelper.extractProfiles(pm)), "Grammar to Json : Value Specification Batch");
    }
}
