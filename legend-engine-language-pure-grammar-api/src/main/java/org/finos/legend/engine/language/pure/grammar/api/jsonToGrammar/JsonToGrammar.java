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
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.graph.RootGraphFetchTree;
import org.finos.legend.engine.shared.core.api.grammar.GrammarAPI;
import org.finos.legend.engine.shared.core.api.grammar.RenderStyle;
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
public class JsonToGrammar extends GrammarAPI
{

    @POST
    @Path("model")
    @ApiOperation(value = "Generates Pure language text from Pure protocol Pure Model Context Data")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Produces(MediaType.TEXT_PLAIN)
    public Response model(PureModelContextData pureModelContext,
                          @QueryParam("renderStyle") @DefaultValue("PRETTY") RenderStyle renderStyle,
                          @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        PureGrammarComposerExtensionLoader.logExtensionList();
        return jsonToGrammar(pureModelContext, renderStyle, (value, renderStyle1) -> PureGrammarComposer.newInstance(PureGrammarComposerContext.Builder.newInstance().withRenderStyle(renderStyle1).build()).renderPureModelContextData(value), pm, "Json to Grammar : Model");
    }

    @POST
    @Path("graphFetch")
    @ApiOperation(value = "Generates Pure language text from Pure protocol GraphFetch fragment")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Produces(MediaType.TEXT_PLAIN)
    public Response graphFetch(RootGraphFetchTree graphFetchTree,
                               @QueryParam("renderStyle") @DefaultValue("PRETTY") RenderStyle renderStyle,
                               @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        PureGrammarComposerExtensionLoader.logExtensionList();
        return jsonToGrammar(graphFetchTree, renderStyle, (vs, renderStyle1) -> DEPRECATED_PureGrammarComposerCore.Builder.newInstance().withRenderStyle(renderStyle1).build().processGraphFetchTree(vs, 0), pm, "Json to Grammar : Graph Fetch");
    }

    @POST
    @Path("graphFetch/batch")
    @ApiOperation(value = "Generates Pure language text from Pure protocol GraphFetch fragment")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response graphFetchBatch(Map<String, RootGraphFetchTree> graphFetchTrees,
                                    @QueryParam("renderStyle") @DefaultValue("PRETTY") RenderStyle renderStyle,
                                    @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        PureGrammarComposerExtensionLoader.logExtensionList();
        return jsonToGrammarBatch(renderStyle, graphFetchTrees, (vs, renderStyle1) -> DEPRECATED_PureGrammarComposerCore.Builder.newInstance().withRenderStyle(renderStyle1).build().processGraphFetchTree(vs, 0), pm, "Json to Grammar : Graph Fetch Batch");
    }

    @POST
    @Path("valueSpecification")
    @ApiOperation(value = "Generates Pure language text from Pure protocol Value Specification fragment")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Produces(MediaType.TEXT_PLAIN)
    public Response valueSpecification(ValueSpecification valueSpecification,
                                       @QueryParam("renderStyle") @DefaultValue("PRETTY") RenderStyle renderStyle,
                                       @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        PureGrammarComposerExtensionLoader.logExtensionList();
        return jsonToGrammar(valueSpecification, renderStyle, (vs, renderStyle1) -> vs.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance().withRenderStyle(renderStyle1).build()), pm, "Json to Grammar : Value Specification");
    }

    @POST
    @Path("valueSpecification/batch")
    @ApiOperation(value = "Generates Pure language text from Pure protocol Value Specification fragment")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response valueSpecificationBatch(Map<String, ValueSpecification> valueSpecifications,
                                            @QueryParam("renderStyle") @DefaultValue("PRETTY") RenderStyle renderStyle,
                                            @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        PureGrammarComposerExtensionLoader.logExtensionList();
        return jsonToGrammarBatch(renderStyle, valueSpecifications, (vs, renderStyle1) -> vs.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance().withRenderStyle(renderStyle1).build()), pm, "Json to Grammar : Value Specification Batch");
    }

    @POST
    @Path("lambda")
    @ApiOperation(value = "Generates Pure language text from Pure protocol Value Specification fragment")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Produces(MediaType.TEXT_PLAIN)
    public Response lambda(Lambda lambda,
                           @QueryParam("renderStyle") @DefaultValue("PRETTY") RenderStyle renderStyle,
                           @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        PureGrammarComposerExtensionLoader.logExtensionList();
        return jsonToGrammar(lambda, renderStyle, (vs, renderStyle1) -> vs.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance().withRenderStyle(renderStyle1).build()), pm, "Json to Grammar : Lambda");
    }

    @POST
    @Path("lambda/batch")
    @ApiOperation(value = "Generates Pure language text from Pure protocol Value Specification fragment")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response lambdaBatch(Map<String, Lambda> lambdas,
                                @QueryParam("renderStyle") @DefaultValue("PRETTY") RenderStyle renderStyle,
                                @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        PureGrammarComposerExtensionLoader.logExtensionList();
        return jsonToGrammarBatch(renderStyle, lambdas, (vs, renderStyle1) -> vs.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance().withRenderStyle(renderStyle1).build()), pm, "Json to Grammar : Lambda Batch");
    }
}
