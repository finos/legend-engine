package org.finos.legend.engine.language.pure.grammar.api.test;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.language.pure.grammar.api.grammarToJson.GrammarToJson;
import org.finos.legend.engine.language.pure.grammar.api.jsonToGrammar.JsonToGrammar;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.graph.RootGraphFetchTree;
import org.finos.legend.engine.shared.core.api.TestGrammar;
import org.finos.legend.engine.shared.core.api.grammar.BatchResult;
import org.finos.legend.engine.shared.core.api.grammar.GrammarAPI;
import org.finos.legend.engine.shared.core.api.grammar.RenderStyle;
import org.finos.legend.engine.shared.core.function.Function5;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.Map;

public class TestGrammarGraphFetchApi extends TestGrammar<RootGraphFetchTree>
{
    @Test
    public void testSimple()
    {
        test("#{\n" +
                "  demo::Query{\n" +
                "    firms{\n" +
                "      legalName,\n" +
                "      employees{\n" +
                "        lastName\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}#", true);
    }

    @Test
    public void testSimpleParsingError()
    {
        testError( "#{\n" +
                        "  demo::Query\n" +
                        "    firms{\n" +
                        "      legalName,\n" +
                        "      employees{\n" +
                        "        lastName\n" +
                        "      }\n" +
                        "    }\n" +
                        "  }\n" +
                        "}#", "Unexpected token", new SourceInformation("", 3, 5, 3, 9));
    }

    @Test
    public void testBatch()
    {
        testBatch(createBatchInput( Tuples.pair("1",  "#{\n" +
                                                    "  demo::Query{\n" +
                                                    "    firms{\n" +
                                                    "      legalName,\n" +
                                                    "      employees{\n" +
                                                    "        lastName\n" +
                                                    "      }\n" +
                                                    "    }\n" +
                                                    "  }\n" +
                                                    "}#"),
                        Tuples.pair("2",  "#{\n" +
                                                    "  demo::Query{\n" +
                                                    "    firms{\n" +
                                                    "      legalName\n" +
                                                    "    }\n" +
                                                    "  }\n" +
                                                    "}#"))
        );
    }

    @Test
    public void testBatchError()
    {
        testBatchError(createBatchInput(Tuples.pair("1",  "#{\n" +
                                                        "  demo::Query{\n" +
                                                        "    firms{\n" +
                                                        "      legalName,\n" +
                                                        "      employees{\n" +
                                                        "        lastName\n" +
                                                        "      }\n" +
                                                        "    }\n" +
                                                        "  }\n" +
                                                        "}#"),
                Tuples.pair("2",  "#{\n" +
                                            "  demo::Query" +
                                            "}#")),
                createExpectedBatchResult(Tuples.pair("1",  "#{\n" +
                                                "  demo::Query{\n" +
                                                "    firms{\n" +
                                                "      legalName,\n" +
                                                "      employees{\n" +
                                                "        lastName\n" +
                                                "      }\n" +
                                                "    }\n" +
                                                "  }\n" +
                                                "}#"),
                        Tuples.pair("2", "{\"message\":\"Unexpected token\",\"sourceInformation\":{\"endColumn\":18,\"endLine\":2,\"sourceId\":\"\",\"startColumn\":14,\"startLine\":2}}"))
        );
    }

    private static final GrammarToJson grammarToJson = new GrammarToJson();
    private static final JsonToGrammar jsonToGrammar = new JsonToGrammar();

    @Override
    public Class<RootGraphFetchTree> get_Class()
    {
        return RootGraphFetchTree.class;
    }

    public static class MyClass extends BatchResult<RootGraphFetchTree>
    {}

    @Override
    public Class getBatchResultSpecializedClass()
    {
        return MyClass.class;
    }

    @Override
    public Function5<String, String, Integer, Integer, Boolean, Response> grammarToJson()
    {
        return (a, b, c, d, e) -> grammarToJson.graphFetch(a, b, c, d, e, null);
    }

    @Override
    public Function2<RootGraphFetchTree, RenderStyle, Response> jsonToGrammar()
    {
        return (a, b) -> jsonToGrammar.graphFetch(a, b, null);
    }

    @Override
    public Function<Map<String, GrammarAPI.ParserInput>, Response> grammarToJsonB()
    {
        return (a) -> grammarToJson.graphFetchBatch(a, null);
    }

    @Override
    public Function2<Map<String, RootGraphFetchTree>, RenderStyle, Response> jsonToGrammarB()
    {
        return (a, b) -> jsonToGrammar.graphFetchBatch(a, b, null);
    }
}
