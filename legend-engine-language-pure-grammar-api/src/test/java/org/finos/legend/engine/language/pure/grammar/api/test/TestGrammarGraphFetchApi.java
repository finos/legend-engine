package org.finos.legend.engine.language.pure.grammar.api.test;

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.language.pure.grammar.api.grammarToJson.GrammarToJson;
import org.finos.legend.engine.language.pure.grammar.api.jsonToGrammar.JsonToGrammar;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.graph.RootGraphFetchTree;
import org.finos.legend.engine.shared.core.api.TestGrammar;
import org.finos.legend.engine.shared.core.api.grammar.BatchResult;
import org.finos.legend.engine.shared.core.api.grammar.RenderStyle;
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
                        "}#", "{\"message\":\"Unexpected token\",\"sourceInformation\":{\"endColumn\":9,\"endLine\":3,\"sourceId\":\"\",\"startColumn\":5,\"startLine\":3}}");
    }

    @Test
    public void testBatch()
    {
        testBatch(with( Tuples.pair("1",  "#{\n" +
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
        testBatchError(with(Tuples.pair("1",  "#{\n" +
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
                with(Tuples.pair("1",  "#{\n" +
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
    public Function2<String, Boolean, Response> grammarToJson()
    {
        return (a, b) -> grammarToJson.graphFetch(a, null, b);
    }

    @Override
    public Function2<RenderStyle, RootGraphFetchTree, Response> jsonToGrammar()
    {
        return (a, b) -> jsonToGrammar.graphFetch(a, b, null);
    }

    @Override
    public Function2<Map<String, String>, Boolean, Response> grammarToJsonB()
    {
        return (a, b) -> grammarToJson.graphFetchBatch(a, null, b);
    }

    @Override
    public Function2<RenderStyle, Map<String, RootGraphFetchTree>, Response> jsonToGrammarB()
    {
        return (a, b) -> jsonToGrammar.graphFetchBatch(a, b, null);
    }
}
