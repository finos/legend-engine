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

package org.finos.legend.engine.language.pure.grammar.api.grammarToJson;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.finos.legend.engine.language.pure.grammar.api.jsonToGrammar.JsonToGrammarInput;
import org.finos.legend.engine.language.pure.grammar.api.jsonToGrammar.LambdaInput;
import org.finos.legend.engine.language.pure.grammar.from.ParserError;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtensionLoader;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.api.result.ManageConstantResult;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
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
import java.util.HashMap;
import java.util.Map;

import static org.finos.legend.engine.shared.core.operational.http.InflateInterceptor.APPLICATION_ZLIB;

@Api(tags = "Pure - Grammar")
@Path("pure/v1/grammar")
@Produces(MediaType.APPLICATION_JSON)
public class TransformGrammarToJson
{
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    @POST
    @Path("transformGrammarToJson")
    @ApiOperation(value = "Generates Pure protocol JSON from Pure language text")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    public Response transformGrammarToJson(GrammarToJsonInput grammarInput, @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        try (Scope scope = GlobalTracer.get().buildSpan("Service: transformJsonToGrammar").startActive(true))
        {
            PureGrammarParserExtensionLoader.logExtensionList();
            PureGrammarParser parser = PureGrammarParser.newInstance();
            Map<String, Lambda> lambdas = new HashMap<>();
            Map<String, ParserError> lambdaErrors = new HashMap<>();
            grammarInput.isolatedLambdas.forEach((key, value) ->
            {
                try
                {
                    Lambda lambda = parser.parseLambda(value, key);
                    lambdas.put(key, lambda);
                }
                catch (Exception e)
                {
                    if (e instanceof EngineException)
                    {
                        EngineException exception = (EngineException) e;
                        if (!EngineErrorType.PARSER.equals(exception.getErrorType()))
                        {
                            // throw non-parser error
                            throw e;
                        }
                        lambdaErrors.put(key, new ParserError(exception.getMessage(), exception.getSourceInformation()));
                    }
                    ExceptionTool.exceptionManager(e, LoggingEventType.TRANSFORM_GRAMMAR_TO_JSON_ERROR, pm);
                }
            });
            JsonToGrammarInput symmetricResult = new JsonToGrammarInput();
            symmetricResult.isolatedLambdas = new LambdaInput();
            symmetricResult.isolatedLambdas.lambdas = lambdas;
            symmetricResult.isolatedLambdas.lambdaErrors = lambdaErrors.size() > 0 ? lambdaErrors : null; // only show object is errors exist
            if (grammarInput.code != null)
            {
                try
                {
                    symmetricResult.modelDataContext = parser.parseModel(grammarInput.code);
                }
                catch (Exception e)
                {
                    if (e instanceof EngineException)
                    {
                        EngineException exception = (EngineException) e;
                        if (!EngineErrorType.PARSER.equals(exception.getErrorType()))
                        {
                            // throw non-parser error
                            throw e;
                        }
                        symmetricResult.codeError = new ParserError(exception.getMessage(), exception.getSourceInformation());
                    }
                    ExceptionTool.exceptionManager(e, LoggingEventType.TRANSFORM_GRAMMAR_TO_JSON_ERROR, pm);
                }
            }
            return ManageConstantResult.manageResult(pm, symmetricResult, objectMapper);
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, LoggingEventType.TRANSFORM_GRAMMAR_TO_JSON_ERROR, pm);
        }
    }
}
