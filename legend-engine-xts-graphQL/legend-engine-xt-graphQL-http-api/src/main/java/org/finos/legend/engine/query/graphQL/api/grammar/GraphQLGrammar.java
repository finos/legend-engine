// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.query.graphQL.api.grammar;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.finos.legend.engine.language.graphQL.grammar.from.GraphQLGrammarParser;
import org.finos.legend.engine.language.graphQL.grammar.from.GraphQLParserException;
import org.finos.legend.engine.language.graphQL.grammar.to.GraphQLGrammarComposer;
import org.finos.legend.engine.protocol.graphQL.metamodel.Document;
import org.finos.legend.engine.protocol.graphQL.metamodel.ExecutableDocument;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.shared.core.api.grammar.GrammarAPI;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.api.grammar.RenderStyle;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;

import static org.finos.legend.engine.shared.core.operational.http.InflateInterceptor.APPLICATION_ZLIB;

@Tag(name = "GraphQL - Grammar")
@Path("graphQL/v1/grammar")
public class GraphQLGrammar extends GrammarAPI
{
    @POST
    @Path("grammarToJson")
    @Operation(summary = "Generates GraphQL protocol JSON from GraphQL language text")
    @Consumes({MediaType.TEXT_PLAIN, APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response grammarToJson(String text,
                                  @DefaultValue("") @Parameter(description = "The source ID to be used by the parser") @QueryParam("sourceId") String sourceId,
                                  @DefaultValue("0") @Parameter(description = "The line number the parser will offset by") @QueryParam("lineOffset") int lineOffset,
                                  @DefaultValue("0") @Parameter(description = "The column number the parser will offset by") @QueryParam("columnOffset") int columnOffset,
                                  @DefaultValue("true") @QueryParam("returnSourceInformation") boolean returnSourceInformation,
                                  @Parameter(hidden = true) @Pac4JProfileManager ProfileManager pm)
    {
        return grammarToJson(text, (a) ->
        {
            try
            {
                return GraphQLGrammarParser.newInstance().parseDocument(a);
            }
            catch (GraphQLParserException ex)
            {
                throw new EngineException(ex.getMessage(), ex.getSourceInformation(), EngineErrorType.PARSER);
            }
        }, Identity.makeIdentity(ProfileManagerHelper.extractProfiles(pm)), "Grammar to Json : GraphQL");
    }

    // Required so that Jackson properly includes _type for the top level element
    static class TypedMap extends UnifiedMap<String, Document>
    {
    }

    @POST
    @Path("grammarToJson/batch")
    @Operation(summary = "Generates GraphQL protocol JSON from GraphQL language text (for multiple elements)")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response grammarToJsonBatch(Map<String, ParserInput> input, @Parameter(hidden = true) @Pac4JProfileManager ProfileManager pm)
    {
        return grammarToJsonBatch(input, (a, b, c, d, e) ->
        {
            try
            {
                return GraphQLGrammarParser.newInstance().parseDocument(a);
            }
            catch (GraphQLParserException ex)
            {
                throw new EngineException(ex.getMessage(), ex.getSourceInformation(), EngineErrorType.PARSER);
            }
        }, new TypedMap(), Identity.makeIdentity(ProfileManagerHelper.extractProfiles(pm)), "Grammar to Json : GraphQL Batch");
    }

    @POST
    @Path("jsonToGrammar")
    @Operation(summary = "Generates GraphQL language text from GraphQL protocol JSON")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Produces(MediaType.TEXT_PLAIN)
    public Response jsonToGrammar(ExecutableDocument document,
                                  @QueryParam("renderStyle") @DefaultValue("PRETTY") RenderStyle renderStyle,
                                  @Parameter(hidden = true) @Pac4JProfileManager ProfileManager pm)
    {
        return jsonToGrammar(document, renderStyle, (vs, renderStyle1) -> GraphQLGrammarComposer.newInstance().renderDocument(vs), Identity.makeIdentity(ProfileManagerHelper.extractProfiles(pm)), "Json to Grammar : GraphQL");
    }

    @POST
    @Path("jsonToGrammar/batch")
    @Operation(summary = "Generates GraphQL language text from GraphQL protocol JSON")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response jsonToGrammarBatch(Map<String, ExecutableDocument> documents,
                                       @QueryParam("renderStyle") @DefaultValue("PRETTY") RenderStyle renderStyle,
                                       @Parameter(hidden = true) @Pac4JProfileManager ProfileManager pm)
    {
        return jsonToGrammarBatch(renderStyle, documents, (vs, renderStyle1) -> GraphQLGrammarComposer.newInstance().renderDocument(vs), Identity.makeIdentity(ProfileManagerHelper.extractProfiles(pm)), "Json to Grammar : GraphQL Batch");
    }

}
