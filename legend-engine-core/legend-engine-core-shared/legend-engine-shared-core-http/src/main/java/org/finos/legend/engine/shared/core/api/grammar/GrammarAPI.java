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

package org.finos.legend.engine.shared.core.api.grammar;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.impl.map.mutable.MapAdapter;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.api.result.ManageConstantResult;
import org.finos.legend.engine.shared.core.function.Function5;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.pac4j.core.profile.ProfileManager;

import jakarta.ws.rs.core.Response;
import java.util.Map;

public class GrammarAPI
{
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    protected <T> Response grammarToJson(String text, Function<String, T> func, Identity identity, String spanText)
    {
        try (Scope scope = GlobalTracer.get().buildSpan(spanText).startActive(true))
        {
            try
            {
                T data = func.apply(text);
                return ManageConstantResult.manageResult(identity.getName(), data, objectMapper);
            }
            catch (Exception e)
            {
                return ExceptionTool.exceptionManager(e, LoggingEventType.TRANSFORM_GRAMMAR_TO_JSON_ERROR, Response.Status.BAD_REQUEST, identity.getName());
            }
        }
    }

    protected <T> Response grammarToJsonBatch(Map<String, ParserInput> input, Function5<String, String, Integer, Integer, Boolean, T> func, Map<String, T> result, Identity identity, String spanText)
    {
        try (Scope scope = GlobalTracer.get().buildSpan(spanText).startActive(true))
        {
            Map<String, ParserError> errors = Maps.mutable.empty();
            input.forEach((key, value) ->
            {
                try
                {
                    ParserSourceInformationOffset sourceInformationOffset = value.sourceInformationOffset != null ? value.sourceInformationOffset : new ParserSourceInformationOffset();
                    result.put(key, func.value(value.value, sourceInformationOffset.sourceId, sourceInformationOffset.lineOffset, sourceInformationOffset.columnOffset, value.returnSourceInformation));
                }
                catch (EngineException e)
                {
                    if (!EngineErrorType.PARSER.equals(e.getErrorType()))
                    {
                        ExceptionTool.exceptionManager(e, LoggingEventType.TRANSFORM_GRAMMAR_TO_JSON_ERROR, identity.getName());
                        throw e;
                    }
                    errors.put(key, new ParserError(e.getMessage(), e.getSourceInformation()));
                }
                catch (Exception e)
                {
                    ExceptionTool.exceptionManager(e, LoggingEventType.TRANSFORM_GRAMMAR_TO_JSON_ERROR, identity.getName());
                    throw e;
                }
            });
            return ManageConstantResult.manageResult(identity.getName(), new BatchResult<T>(result, errors), objectMapper);
        }
    }


    protected <T> Response jsonToGrammar(T elementToRender, RenderStyle renderStyle, Function2<T, RenderStyle, String> func, Identity identity, String spanText)
    {
        try (Scope scope = GlobalTracer.get().buildSpan(spanText).startActive(true))
        {
            return Response.ok(func.apply(elementToRender, renderStyle)).build();
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, LoggingEventType.TRANSFORM_JSON_TO_GRAMMAR_ERROR, identity.getName());
        }
    }

    protected <T> Response jsonToGrammarBatch(RenderStyle renderStyle, Map<String, T> values, Function2<T, RenderStyle, String> func, Identity identity, String spanText)
    {
        try (Scope scope = GlobalTracer.get().buildSpan(spanText).startActive(true))
        {
            Map<String, Object> result = org.eclipse.collections.api.factory.Maps.mutable.empty();
            MapAdapter.adapt(values).forEachKeyValue((key, value) ->
            {
                result.put(key, func.apply(value, renderStyle));
            });
            return ManageConstantResult.manageResult(identity.getName(), result);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            return ExceptionTool.exceptionManager(ex, LoggingEventType.TRANSFORM_JSON_TO_GRAMMAR_ERROR, identity.getName());
        }
    }

    // Kept for downstream callers: erasure makes these binary-compatible with the former
    // ProfileManager<CommonProfile> overloads. No in-repo caller uses them.

    /**
     * @deprecated pass an {@link Identity}; this resolves one from the ProfileManager for you.
     */
    @Deprecated
    protected <T> Response grammarToJson(String text, Function<String, T> func, ProfileManager pm, String spanText)
    {
        return grammarToJson(text, func, identityFrom(pm), spanText);
    }

    /**
     * @deprecated pass an {@link Identity}; this resolves one from the ProfileManager for you.
     */
    @Deprecated
    protected <T> Response grammarToJsonBatch(Map<String, ParserInput> input, Function5<String, String, Integer, Integer, Boolean, T> func, Map<String, T> result, ProfileManager pm, String spanText)
    {
        return grammarToJsonBatch(input, func, result, identityFrom(pm), spanText);
    }

    /**
     * @deprecated pass an {@link Identity}; this resolves one from the ProfileManager for you.
     */
    @Deprecated
    protected <T> Response jsonToGrammar(T elementToRender, RenderStyle renderStyle, Function2<T, RenderStyle, String> func, ProfileManager pm, String spanText)
    {
        return jsonToGrammar(elementToRender, renderStyle, func, identityFrom(pm), spanText);
    }

    /**
     * @deprecated pass an {@link Identity}; this resolves one from the ProfileManager for you.
     */
    @Deprecated
    protected <T> Response jsonToGrammarBatch(RenderStyle renderStyle, Map<String, T> values, Function2<T, RenderStyle, String> func, ProfileManager pm, String spanText)
    {
        return jsonToGrammarBatch(renderStyle, values, func, identityFrom(pm), spanText);
    }

    private static Identity identityFrom(ProfileManager pm)
    {
        return Identity.makeIdentity(ProfileManagerHelper.extractProfiles(pm));
    }

    public static class ParserSourceInformationOffset
    {
        public String sourceId = "";
        public int lineOffset = 0;
        public int columnOffset = 0;
    }

    public static class ParserInput
    {
        public String value;
        /**
         * Default to `true` for backward compatibility reason
         */
        public boolean returnSourceInformation = true;
        public ParserSourceInformationOffset sourceInformationOffset;
    }
}
