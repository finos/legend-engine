// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.language.pure.grammar.api.jsonToGrammar;

import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.MapIterate;
import org.finos.legend.engine.language.pure.grammar.api.grammarToJson.GrammarToJsonInput;
import org.finos.legend.engine.language.pure.grammar.to.DEPRECATED_PureGrammarComposerCore;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposer;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.language.pure.grammar.to.extension.PureGrammarComposerExtensionLoader;
import org.finos.legend.engine.shared.core.api.result.ManageConstantResult;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;

import javax.security.auth.Subject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.finos.legend.engine.shared.core.operational.http.InflateInterceptor.APPLICATION_ZLIB;

@Api(tags = "Pure - Grammar")
@Path("pure/v1/grammar")
@Produces(MediaType.APPLICATION_JSON)
public class TransformJsonToGrammar
{
    @POST
    @Path("transformJsonToGrammar")
    @ApiOperation(value = "Generates Pure language text from Pure protocol JSON")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    public Response transformJsonToGrammar(JsonToGrammarInput jsonInput, @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        try (Scope scope = GlobalTracer.get().buildSpan("Service: transformJsonToGrammar").startActive(true))
        {
            PureGrammarComposerExtensionLoader.logExtensionList();
            PureGrammarComposer grammarTransformer = PureGrammarComposer.newInstance(PureGrammarComposerContext.Builder.newInstance().withRenderStyle(jsonInput.renderStyle).build());
            DEPRECATED_PureGrammarComposerCore grammarTransformerVisitor = DEPRECATED_PureGrammarComposerCore.Builder.newInstance().withRenderStyle(jsonInput.renderStyle).build();
            GrammarToJsonInput symmetricResult = new GrammarToJsonInput();
            MutableMap<String, String> resMap = Maps.mutable.empty();
            if (jsonInput.isolatedLambdas != null)
            {
                MapIterate.toListOfPairs(jsonInput.isolatedLambdas.lambdas).collect(p -> Tuples.pair(p.getOne(), p.getTwo().accept(grammarTransformerVisitor))).forEach((Procedure<Pair<String, String>>) p -> resMap.put(p.getOne(), p.getTwo()));
            }
            symmetricResult.isolatedLambdas = resMap;
            if (jsonInput.modelDataContext != null)
            {
                symmetricResult.code = grammarTransformer.renderPureModelContextData(jsonInput.modelDataContext);
            }
            return ManageConstantResult.manageResult(pm, symmetricResult);
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, LoggingEventType.TRANSFORM_JSON_TO_GRAMMAR_ERROR, pm);
        }
    }
}
