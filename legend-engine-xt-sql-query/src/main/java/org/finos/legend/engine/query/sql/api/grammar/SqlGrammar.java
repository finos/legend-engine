// Copyright 2022 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package org.finos.legend.engine.query.sql.api.grammar;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.finos.legend.engine.language.sql.grammar.from.SQLGrammarParser;
import org.finos.legend.engine.language.sql.grammar.from.SQLParserException;
import org.finos.legend.engine.language.sql.grammar.to.SQLGrammarComposer;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.sql.metamodel.Node;
import org.finos.legend.engine.shared.core.api.grammar.GrammarAPI;
import org.finos.legend.engine.shared.core.api.grammar.RenderStyle;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
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

@Api(tags = "SQL - Grammar")
@Path("sql/v1/grammar")
public class SqlGrammar extends GrammarAPI
{
    @POST
    @Path("grammarToJson")
    @ApiOperation(value = "Generates SQL protocol JSON from SQL language text")
    @Consumes({MediaType.TEXT_PLAIN, APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response grammarToJson(String text,
                                  @DefaultValue("") @ApiParam("The source ID to be used by the parser") @QueryParam("sourceId") String sourceId,
                                  @DefaultValue("0") @ApiParam("The line number the parser will offset by") @QueryParam("lineOffset") int lineOffset,
                                  @DefaultValue("0") @ApiParam("The column number the parser will offset by") @QueryParam("columnOffset") int columnOffset,
                                  @DefaultValue("true") @QueryParam("returnSourceInformation") boolean returnSourceInformation,
                                  @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        return grammarToJson(text, (query) ->
        {
            try
            {
                return SQLGrammarParser.newInstance().parseStatement(query);
            }
            catch (SQLParserException ex)
            {
                throw new EngineException(ex.getMessage(), ex.getSourceInformation(), EngineErrorType.PARSER);
            }
        }, pm, "Grammar to Json : SQL");
    }

    // Required so that Jackson properly includes _type for the top level element
    static class TypedMap extends UnifiedMap<String, Node>
    {
    }

    @POST
    @Path("grammarToJson/batch")
    @ApiOperation(value = "Generates SQL protocol JSON from SQL language text (for multiple elements)")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response grammarToJsonBatch(Map<String, ParserInput> input, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        return grammarToJsonBatch(input, (query, b, c, d, e) ->
        {
            try
            {
                return SQLGrammarParser.newInstance().parseStatement(query);
            }
            catch (SQLParserException ex)
            {
                throw new EngineException(ex.getMessage(), ex.getSourceInformation(), EngineErrorType.PARSER);
            }
        }, new TypedMap(), pm, "Grammar to Json : SQL Batch");
    }

    @POST
    @Path("jsonToGrammar")
    @ApiOperation(value = "Generates SQL language text from SQL protocol JSON")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Produces(MediaType.TEXT_PLAIN)
    public Response jsonToGrammar(Node query,
                                  @QueryParam("renderStyle") @DefaultValue("PRETTY") RenderStyle renderStyle,
                                  @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        return jsonToGrammar(query, renderStyle, (vs, renderStyle1) -> SQLGrammarComposer.newInstance().renderNode(vs), pm, "Json to Grammar : SQL");
    }

    @POST
    @Path("jsonToGrammar/batch")
    @ApiOperation(value = "Generates SQL language text from SQL protocol JSON")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response jsonToGrammarBatch(Map<String, Node> documents,
                                       @QueryParam("renderStyle") @DefaultValue("PRETTY") RenderStyle renderStyle,
                                       @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        return jsonToGrammarBatch(renderStyle, documents, (vs, renderStyle1) -> SQLGrammarComposer.newInstance().renderNode(vs), pm, "Json to Grammar : SQL Batch");
    }
}
