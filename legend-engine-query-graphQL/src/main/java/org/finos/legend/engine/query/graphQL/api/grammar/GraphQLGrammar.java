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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.finos.legend.engine.language.graphQL.grammar.from.GraphQLGrammarParser;
import org.finos.legend.engine.language.graphQL.grammar.from.GraphQLParserException;
import org.finos.legend.engine.language.graphQL.grammar.to.GraphQLGrammarComposer;
import org.finos.legend.engine.protocol.graphQL.metamodel.Document;
import org.finos.legend.engine.protocol.graphQL.metamodel.ExecutableDocument;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.shared.core.api.grammar.GrammarAPI;
import org.finos.legend.engine.shared.core.api.grammar.RenderStyle;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

import static org.finos.legend.engine.shared.core.operational.http.InflateInterceptor.APPLICATION_ZLIB;

@Api(tags = "GraphQL - Grammar")
@Path("graphQL/v1/grammar")
public class GraphQLGrammar extends GrammarAPI
{
    @POST
    @Path("grammarToJson")
    @ApiOperation(value = "Generates GraphQL protocol JSON from GraphQL language text")
    @Consumes({MediaType.TEXT_PLAIN, APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response grammarToJson(String grammar, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm, @DefaultValue("true") @QueryParam("returnSourceInfo") boolean returnSourceInfo)
    {
        return grammarToJson(grammar, (a, b) -> {
            try
            {
                return GraphQLGrammarParser.newInstance().parseDocument(grammar);
            }
            catch (GraphQLParserException e)
            {
                throw new EngineException(e.getMessage(), e.getSourceInformation(), EngineErrorType.PARSER);
            }
        }, pm, returnSourceInfo, "Grammar to Json : GraphQL");
    }

    // Required so that Jackson properly includes _type for the top level element
    static class TypedMap extends UnifiedMap<String, Document>
    {}

    @POST
    @Path("grammarToJson/batch")
    @ApiOperation(value = "Generates GraphQL protocol JSON from GraphQL language text (for multiple elements)")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response grammarToJsonBatch(Map<String, String> grammars, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm, @DefaultValue("true") @QueryParam("returnSourceInfo") boolean returnSourceInfo)
    {
        return grammarToJsonBatch(grammars, (a, b) -> {
            try
            {
                return GraphQLGrammarParser.newInstance().parseDocument(a);
            }
            catch (GraphQLParserException e)
            {
                throw new EngineException(e.getMessage(), e.getSourceInformation(), EngineErrorType.PARSER);
            }
        }, new TypedMap(), pm, returnSourceInfo, "Grammar to Json : GraphQL Batch");
    }

    @POST
    @Path("jsonToGrammar/{renderStyle}")
    @ApiOperation(value = "Generates GraphQL language text from GraphQL protocol JSON")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Produces(MediaType.TEXT_PLAIN)
    public Response jsonToGrammar(@PathParam("renderStyle") @DefaultValue("PRETTY") RenderStyle renderStyle, ExecutableDocument document,  @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        return jsonToGrammar(document, renderStyle, (vs, renderStyle1) -> GraphQLGrammarComposer.newInstance().renderDocument(vs), pm, "Json to Grammar : GraphQL");
    }

    @POST
    @Path("jsonToGrammar/{renderStyle}/batch")
    @ApiOperation(value = "Generates GraphQL language text from GraphQL protocol JSON")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response jsonToGrammarBatch(@PathParam("renderStyle") @DefaultValue("PRETTY") RenderStyle renderStyle, Map<String, ExecutableDocument> documents, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        return jsonToGrammarBatch(renderStyle, documents, (vs, renderStyle1) -> GraphQLGrammarComposer.newInstance().renderDocument(vs), pm, "Json to Grammar : GraphQL Batch");
    }

}
