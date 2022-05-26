package org.finos.legend.engine.shared.core.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.eclipse.collections.impl.utility.MapIterate;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.api.grammar.BatchResult;
import org.finos.legend.engine.shared.core.api.grammar.GrammarAPI;
import org.finos.legend.engine.shared.core.api.grammar.RenderStyle;
import org.finos.legend.engine.shared.core.function.Function5;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionError;

import javax.ws.rs.core.Response;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public abstract class TestGrammar<Z>
{
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    public abstract Class<Z> get_Class();

    public abstract Class<BatchResult<Z>> getBatchResultSpecializedClass();

    public abstract Function5<String, String, Integer, Integer, Boolean, Response> grammarToJson();

    public abstract Function2<Z, RenderStyle, Response> jsonToGrammar();

    public abstract Function<Map<String, GrammarAPI.ParserInput>, Response> grammarToJsonB();

    public abstract Function2<Map<String, Z>, RenderStyle, Response> jsonToGrammarB();

    protected void test(String str, boolean returnSourceInfo)
    {
        try
        {
            Response result = grammarToJson().value(str, "", 0, 0, returnSourceInfo);
            String actual = result.getEntity().toString();
            Response newResult = jsonToGrammar().apply(objectMapper.readValue(actual, get_Class()), RenderStyle.PRETTY);
            assertEquals(str, newResult.getEntity().toString());
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    protected void testError(String str, String expectedErrorMessage, SourceInformation expectedErrorSourceInformation)
    {
        try
        {
            Response result = grammarToJson().value(str, "", 0, 0, true);
            Object errorObject = result.getEntity();
            assertTrue(errorObject instanceof ExceptionError);
            ExceptionError error = (ExceptionError) errorObject;
            assertEquals(expectedErrorMessage, error.getMessage());
            assertEquals(expectedErrorSourceInformation.sourceId, error.getSourceInformation().sourceId);
            assertEquals(expectedErrorSourceInformation.startLine, error.getSourceInformation().startLine);
            assertEquals(expectedErrorSourceInformation.startColumn, error.getSourceInformation().startColumn);
            assertEquals(expectedErrorSourceInformation.endLine, error.getSourceInformation().endLine);
            assertEquals(expectedErrorSourceInformation.endColumn, error.getSourceInformation().endColumn);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    protected void testBatch(Map<String, GrammarAPI.ParserInput> input)
    {
        try
        {
            Map<String, Z> fullResult = objectMapper.readValue(grammarToJsonB().apply(input).getEntity().toString(), getBatchResultSpecializedClass()).result;
            MapIterate.forEachKeyValue(input, (a, b) -> {
                assertEquals(b.value, jsonToGrammar().apply(fullResult.get(a), RenderStyle.PRETTY)
                    .getEntity()
                    .toString());
            });
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    protected void testBatchError(Map<String, GrammarAPI.ParserInput> input, Map<String, String> expected)
    {
        try
        {
            Response result = grammarToJsonB().apply(input);
            BatchResult<Z> fullResult = objectMapper.readValue(result.getEntity().toString(), getBatchResultSpecializedClass());
            MapIterate.forEachKeyValue(expected, (a, b) ->
            {
                try
                {
                    Z val = fullResult.result.get(a);
                    if (val != null)
                    {
                        assertEquals(b,
                            jsonToGrammar()
                                .apply(objectMapper.readValue(objectMapper.writeValueAsString(val), get_Class()), RenderStyle.PRETTY)
                                .getEntity()
                                .toString());
                    }
                    else
                    {
                        assertEquals(b, objectMapper.writeValueAsString(fullResult.errors.get(a)));
                    }
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            });
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    protected Map<String, String> createExpectedBatchResult(Pair<String, String>... pairs)
    {
        Map<String, String> res = Maps.mutable.empty();
        ArrayIterate.forEach(pairs, val -> res.put(val.getOne(), val.getTwo()));
        return res;
    }

    protected Map<String, GrammarAPI.ParserInput> createBatchInput(Pair<String, String>... pairs)
    {
        Map<String, GrammarAPI.ParserInput> res = Maps.mutable.empty();
        ArrayIterate.forEach(pairs, val ->
        {
            GrammarAPI.ParserInput input = new GrammarAPI.ParserInput();
            input.value = val.getTwo();
            res.put(val.getOne(), input);
        });
        return res;
    }
}
