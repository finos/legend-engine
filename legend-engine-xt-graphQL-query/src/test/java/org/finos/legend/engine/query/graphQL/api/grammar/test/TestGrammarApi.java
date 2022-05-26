package org.finos.legend.engine.query.graphQL.api.grammar.test;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.protocol.graphQL.metamodel.ExecutableDocument;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.query.graphQL.api.grammar.GraphQLGrammar;
import org.finos.legend.engine.shared.core.api.TestGrammar;
import org.finos.legend.engine.shared.core.api.grammar.BatchResult;
import org.finos.legend.engine.shared.core.api.grammar.GrammarAPI;
import org.finos.legend.engine.shared.core.api.grammar.RenderStyle;
import org.finos.legend.engine.shared.core.function.Function5;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.Map;

public class TestGrammarApi extends TestGrammar<ExecutableDocument>
{
    @Test
    public void testSimple()
    {
        test("type Car implements Vehicle & X & Z {\n" +
                "  id: ID!\n" +
                "  name: String!\n" +
                "  values: [String]\n" +
                "  length(unit: LengthUnit = METER): Float\n" +
                "}", true);
    }

    @Test
    public void testSimpleError()
    {
        testError("type Car implements Vehicle & X & Z {\n" +
                "  id: ID!\n" +
                "  name String!\n" +
                "  values: [String]\n" +
                "  length(unit: LengthUnit = METER): Float\n" +
                "}", "Unexpected token", new SourceInformation("", 3, 8, 3, 13));
    }

    @Test
    public void testBatch()
    {
        testBatch(createBatchInput(Tuples.pair("1", "type Car implements Vehicle & X & Z {\n" +
                        "  id: ID!\n" +
                        "  name: String!\n" +
                        "  values: [String]\n" +
                        "  length(unit: LengthUnit = METER): Float\n" +
                        "}"),
                        Tuples.pair("2", "enum Direction {\n"+
                                "  NORTH\n"+
                                "  SOUTH\n"+
                                "  EAST\n"+
                                "  WEST\n"+
                                "}")));
    }

    @Test
    public void testBatchError()
    {
        testBatchError(createBatchInput(Tuples.pair("1", "type Car implements Vehicle & X & Z {\n" +
                        "  id: ID!\n" +
                        "  name: String!\n" +
                        "  values: [String]\n" +
                        "  length(unit: LengthUnit = METER): Float\n" +
                        "}"),
                Tuples.pair("2", "enu Direction {\n"+
                        "  NORTH\n"+
                        "  SOUTH\n"+
                        "  EAST\n"+
                        "  WEST\n"+
                        "}")),
                createExpectedBatchResult(Tuples.pair("1", "type Car implements Vehicle & X & Z {\n" +
                                "  id: ID!\n" +
                                "  name: String!\n" +
                                "  values: [String]\n" +
                                "  length(unit: LengthUnit = METER): Float\n" +
                                "}"),
                        Tuples.pair("2", "{\"message\":\"Unexpected token\",\"sourceInformation\":{\"endColumn\":3,\"endLine\":1,\"sourceId\":\"\",\"startColumn\":1,\"startLine\":1}}"))
                );
    }

    private static final GraphQLGrammar graphQLGrammar = new GraphQLGrammar();

    @Override
    public Class<ExecutableDocument> get_Class()
    {
        return ExecutableDocument.class;
    }

    public static class MyClass extends BatchResult<ExecutableDocument>
    {
    }

    @Override
    public Class getBatchResultSpecializedClass()
    {
        return MyClass.class;
    }

    @Override
    public Function5<String, String, Integer, Integer, Boolean, Response> grammarToJson()
    {
        return (a, b, c, d, e) -> graphQLGrammar.grammarToJson(a, b, c, d, e, null);
    }

    @Override
    public Function2<ExecutableDocument, RenderStyle, Response> jsonToGrammar()
    {
        return (a, b) -> graphQLGrammar.jsonToGrammar(a, b, null);
    }

    @Override
    public Function<Map<String, GrammarAPI.ParserInput>, Response> grammarToJsonB()
    {
        return (a) -> graphQLGrammar.grammarToJsonBatch(a, null);
    }

    @Override
    public Function2<Map<String, ExecutableDocument>, RenderStyle, Response> jsonToGrammarB()
    {
        return (a, b) -> graphQLGrammar.jsonToGrammarBatch(a, b, null);
    }
}

