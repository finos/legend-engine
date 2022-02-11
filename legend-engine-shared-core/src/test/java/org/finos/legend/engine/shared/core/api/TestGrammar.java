package org.finos.legend.engine.shared.core.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.eclipse.collections.impl.utility.MapIterate;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.api.grammar.BatchResult;
import org.finos.legend.engine.shared.core.api.grammar.RenderStyle;

import javax.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public abstract class TestGrammar<Z>
{
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    public abstract Class<Z> get_Class();
    public abstract Class<BatchResult<Z>> getBatchResultSpecializedClass();
    public abstract Function2<String, Boolean, Response> grammarToJson();
    public abstract Function2<RenderStyle, Z, Response> jsonToGrammar();
    public abstract Function2<Map<String,String>, Boolean, Response> grammarToJsonB();
    public abstract Function2<RenderStyle, Map<String, Z>, Response> jsonToGrammarB();

    protected void test(String str, boolean returnSourceInfo)
    {
        try
        {
            Response result = grammarToJson().apply(str, returnSourceInfo);
            String actual = result.getEntity().toString();
            Z lambda = objectMapper.readValue(actual, get_Class());
            Response newResult =  jsonToGrammar().apply(RenderStyle.PRETTY, lambda);
            assertEquals(str, newResult.getEntity().toString());
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    protected void testError(String str, String expected)
    {
        try
        {
            Response result = grammarToJson().apply(str, true);
            String actual = result.getEntity().toString();
            assertEquals(expected, actual);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    protected void testBatch(Map<String, String> str)
    {
        try
        {
            Response result = grammarToJsonB().apply(str, true);
            String actual = result.getEntity().toString();
            Map<String, Z> lambda = objectMapper.readValue(actual, getBatchResultSpecializedClass()).result;
            Response newResult = jsonToGrammarB().apply(RenderStyle.PRETTY, lambda);
            assertEquals(objectMapper.writeValueAsString(str), newResult.getEntity().toString());
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    protected void testBatchError(Map<String, String> str, Map<String, String> expected)
    {
        try
        {
            Response result = grammarToJsonB().apply(str, true);
            String actual = result.getEntity().toString();
            BatchResult<Z> fulLResult = objectMapper.readValue(actual, getBatchResultSpecializedClass());
            MapIterate.forEachKeyValue(expected, (a, b) -> {
                try
                {
                    Z val = fulLResult.result.get(a);
                    if (val != null)
                    {
                        assertEquals(b, jsonToGrammar().apply(RenderStyle.PRETTY, objectMapper.readValue(objectMapper.writeValueAsString(val), get_Class())).getEntity().toString());
                    }
                    else
                    {
                        assertEquals(b, objectMapper.writeValueAsString(fulLResult.errors.get(a)));
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

    protected <U, V> Map<U, V> with(Pair<U, V>... pairs)
    {
        Map<U, V> res = Maps.mutable.empty();
        ArrayIterate.forEach(pairs, val -> res.put(val.getOne(), val.getTwo()));
        return res;
    }
}
