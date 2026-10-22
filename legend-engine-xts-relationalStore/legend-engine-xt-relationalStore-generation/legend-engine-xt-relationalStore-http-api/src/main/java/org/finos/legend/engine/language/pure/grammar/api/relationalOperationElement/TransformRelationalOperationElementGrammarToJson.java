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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.grammar.from.RelationalGrammarParserExtension;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtensions;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.operation.RelationalOperationElement;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.api.grammar.ParserError;
import org.finos.legend.engine.shared.core.api.result.ManageConstantResult;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

import static org.finos.legend.engine.shared.core.operational.http.InflateInterceptor.APPLICATION_ZLIB;

@Deprecated
@Tag(name = "Pure - Grammar")
@Path("pure/v1/grammar")
@Produces(MediaType.APPLICATION_JSON)
public class TransformRelationalOperationElementGrammarToJson
{
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    @Deprecated
    @POST
    @Path("transformRelationalOperationElementGrammarToJson")
    @Operation(summary = "Generates Pure protocol JSON from Pure language text for relational operation elements")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    public Response transformRelationalOperationElementGrammarToJson(RelationalOperationElementGrammarToJsonInput input, @Parameter(hidden = true) @Pac4JProfileManager ProfileManager pm, @QueryParam("returnSourceInfo") boolean returnSourceInfo)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        Identity identity = Identity.makeIdentity(profiles);
        try (Scope scope = GlobalTracer.get().buildSpan("Service: transformRelationalOperationElementGrammarToJson").startActive(true))
        {
            PureGrammarParserExtensions.logExtensionList();
            Map<String, RelationalOperationElement> operations = new HashMap<>();
            Map<String, ParserError> operationErrors = new HashMap<>();
            input.operations.forEach((key, value) ->
            {
                try
                {
                    RelationalOperationElement operation = RelationalGrammarParserExtension.parseRelationalOperationElement(value, key, 0, 0, returnSourceInfo);
                    operations.put(key, operation);
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
                        operationErrors.put(key, new ParserError(exception.getMessage(), exception.getSourceInformation()));
                    }
                    ExceptionTool.exceptionManager(e, LoggingEventType.TRANSFORM_RELATIONAL_OPERATION_ELEMENT_JSON_TO_GRAMMAR_ERROR, identity.getName());
                }
            });
            RelationalOperationElementJsonToGrammarInput symmetricResult = new RelationalOperationElementJsonToGrammarInput();
            symmetricResult.operations = operations;
            symmetricResult.operationErrors = operationErrors.size() > 0 ? operationErrors : null; // only show object is errors exist
            return ManageConstantResult.manageResult(identity.getName(), symmetricResult, objectMapper);
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, LoggingEventType.TRANSFORM_RELATIONAL_OPERATION_ELEMENT_JSON_TO_GRAMMAR_ERROR, identity.getName());
        }
    }
}
