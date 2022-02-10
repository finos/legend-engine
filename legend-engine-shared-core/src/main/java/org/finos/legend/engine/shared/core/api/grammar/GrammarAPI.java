package org.finos.legend.engine.shared.core.api.grammar;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.map.mutable.MapAdapter;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.api.result.ManageConstantResult;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;

import javax.ws.rs.core.Response;
import java.util.Map;

public class GrammarAPI
{
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    protected <T> Response grammarToJson(String input, Function2<String, Boolean, T> func , ProfileManager<CommonProfile> pm, boolean returnSourceInfo, String spanText)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        try (Scope scope = GlobalTracer.get().buildSpan(spanText).startActive(true))
        {
            try
            {
                T data = func.apply(input, returnSourceInfo);
                return ManageConstantResult.manageResult(profiles, data, objectMapper);
            }
            catch (EngineException e)
            {
                if (!EngineErrorType.PARSER.equals(e.getErrorType()))
                {
                    return ExceptionTool.exceptionManager(e, LoggingEventType.TRANSFORM_GRAMMAR_TO_JSON_ERROR, profiles);
                }
                return ManageConstantResult.manageResult(profiles, new ParserError(e.getMessage(), e.getSourceInformation()), objectMapper);
            }
            catch (Exception e)
            {
                return ExceptionTool.exceptionManager(e, LoggingEventType.TRANSFORM_GRAMMAR_TO_JSON_ERROR, profiles);
            }
        }
    }

    protected <T> Response grammarToJsonBatch(Map<String, String> input, Function2<String, Boolean, T> func , Map<String, T> result, ProfileManager<CommonProfile> pm, boolean returnSourceInfo, String spanText)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        try (Scope scope = GlobalTracer.get().buildSpan(spanText).startActive(true))
        {
            Map<String , ParserError> errors = Maps.mutable.empty();
            input.forEach((key, value) ->
            {
                try
                {
                    result.put(key, func.apply(value, returnSourceInfo));
                }
                catch (EngineException e)
                {
                    if (!EngineErrorType.PARSER.equals(e.getErrorType()))
                    {
                        ExceptionTool.exceptionManager(e, LoggingEventType.TRANSFORM_GRAMMAR_TO_JSON_ERROR, profiles);
                        throw e;
                    }
                    errors.put(key, new ParserError(e.getMessage(), e.getSourceInformation()));
                }
                catch (Exception e)
                {
                    ExceptionTool.exceptionManager(e, LoggingEventType.TRANSFORM_GRAMMAR_TO_JSON_ERROR, profiles);
                    throw e;
                }
            });
            return ManageConstantResult.manageResult(profiles, new BatchResult<T>(result, errors), objectMapper);
        }
    }


    protected  <T> Response jsonToGrammar(T graphFetchTree, RenderStyle renderStyle, Function2<T, RenderStyle, String> func, ProfileManager<CommonProfile> pm, String spanText)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        try (Scope scope = GlobalTracer.get().buildSpan(spanText).startActive(true))
        {
            return Response.ok(func.apply(graphFetchTree, renderStyle)).build();
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, LoggingEventType.TRANSFORM_JSON_TO_GRAMMAR_ERROR, profiles);
        }
    }

    protected <T> Response jsonToGrammarBatch(RenderStyle renderStyle, Map<String, T> values, Function2<T, RenderStyle, String> func, ProfileManager<CommonProfile> pm, String spanText)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        try (Scope scope = GlobalTracer.get().buildSpan(spanText).startActive(true))
        {
            Map<String, Object> result = org.eclipse.collections.api.factory.Maps.mutable.empty();
            MapAdapter.adapt(values).forEachKeyValue((key, value) -> {
                result.put(key, func.apply(value, renderStyle));
            });
            return ManageConstantResult.manageResult(profiles, result);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            return ExceptionTool.exceptionManager(ex, LoggingEventType.TRANSFORM_JSON_TO_GRAMMAR_ERROR, profiles);
        }
    }
}
