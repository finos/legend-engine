package org.finos.legend.engine.language.pure.grammar.api.test;

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.language.pure.grammar.api.grammarToJson.GrammarToJson;
import org.finos.legend.engine.language.pure.grammar.api.jsonToGrammar.JsonToGrammar;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.shared.core.api.TestGrammar;
import org.finos.legend.engine.shared.core.api.grammar.BatchResult;
import org.finos.legend.engine.shared.core.api.grammar.RenderStyle;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.Map;

public class TestGrammarValueSpecificationApi extends TestGrammar<ValueSpecification>
{
    @Test
    public void testSimple()
    {
        test("1 + 1", true);
        test("true->func(\n" +
                "  2,\n" +
                "  'www'\n" +
                ")", true);
    }

    @Test
    public void testSimpleError()
    {
        testError("true->func(\n" +
                "  2\n" +
                "  'www'\n" +
                ")", "Unexpected token", new SourceInformation("", 3, 3, 3, 7));
    }

    @Test
    public void testBatch()
    {
        testBatch(with(Tuples.pair("1", "1 + 1"),
                  Tuples.pair("2", "true->func(\n" +
                                             "  2,\n" +
                                             "  'www'\n" +
                                             ")")));
    }

    @Test
    public void testBatchError()
    {
        testBatchError( with(Tuples.pair("1", "1 + 1"),
                             Tuples.pair("2", "true->func(\n" +
                                "  2\n" +
                                "  'www'\n" +
                                ")")),
                        with(Tuples.pair("1", "1 + 1"),
                                Tuples.pair("2", "{\"message\":\"Unexpected token\",\"sourceInformation\":{\"endColumn\":7,\"endLine\":3,\"sourceId\":\"\",\"startColumn\":3,\"startLine\":3}}"))
                         );
    }

    private static final GrammarToJson grammarToJson = new GrammarToJson();
    private static final JsonToGrammar jsonToGrammar = new JsonToGrammar();

    @Override
    public Class<ValueSpecification> get_Class()
    {
        return ValueSpecification.class;
    }

    public static class MyClass extends BatchResult<ValueSpecification>
    {
    }

    @Override
    public Class getBatchResultSpecializedClass()
    {
        return MyClass.class;
    }

    @Override
    public Function2<String, Boolean, Response> grammarToJson()
    {
        return (a, b) -> grammarToJson.valueSpecification(a, null, b);
    }

    @Override
    public Function2<RenderStyle, ValueSpecification, Response> jsonToGrammar()
    {
        return (a, b) -> jsonToGrammar.valueSpecification(a, b, null);
    }

    @Override
    public Function2<Map<String, String>, Boolean, Response> grammarToJsonB()
    {
        return (a, b) -> grammarToJson.valueSpecificationBatch(a, null, b);
    }

    @Override
    public Function2<RenderStyle, Map<String, ValueSpecification>, Response> jsonToGrammarB()
    {
        return (a, b) -> jsonToGrammar.valueSpecificationBatch(a, b, null);
    }
}
