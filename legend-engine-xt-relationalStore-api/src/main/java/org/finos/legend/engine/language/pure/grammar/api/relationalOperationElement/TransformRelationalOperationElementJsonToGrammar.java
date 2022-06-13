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
import org.finos.legend.engine.shared.core.api.result.ManageConstantResult;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.finos.legend.engine.shared.core.operational.http.InflateInterceptor.APPLICATION_ZLIB;


@Deprecated
@Api(tags = "Pure - Grammar")
@Path("pure/v1/grammar")
@Produces(MediaType.APPLICATION_JSON)
public class TransformRelationalOperationElementJsonToGrammar
{
    @Deprecated
    @POST
    @Path("transformRelationalOperationElementJsonToGrammar")
    @ApiOperation(value = "Generates Pure language text from Pure protocol JSON for relational operation elements")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    public Response transformRelationalOperationElementJsonToGrammar(RelationalOperationElementJsonToGrammarInput input, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        try (Scope scope = GlobalTracer.get().buildSpan("Service: transformRelationalOperationElementJsonToGrammar").startActive(true))
        {
            PureGrammarComposerExtensionLoader.logExtensionList();
            // TODO: support for render style option - this has to be implemented at the grammar composer layer first
            RelationalOperationElementGrammarToJsonInput symmetricResult = new RelationalOperationElementGrammarToJsonInput();
            MutableMap<String, String> operations = Maps.mutable.empty();
            if (input.operations != null)
            {
                MapIterate.toListOfPairs(input.operations)
                        .collect(p -> Tuples.pair(p.getOne(), RelationalGrammarComposerExtension.renderRelationalOperationElement(p.getTwo())))
                        .forEach((Procedure<Pair<String, String>>) p -> operations.put(p.getOne(), p.getTwo()));
            }
            symmetricResult.operations = operations;
            return ManageConstantResult.manageResult(profiles, symmetricResult);
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, LoggingEventType.TRANSFORM_RELATIONAL_OPERATION_ELEMENT_JSON_TO_GRAMMAR_ERROR, profiles);
        }
    }
}
