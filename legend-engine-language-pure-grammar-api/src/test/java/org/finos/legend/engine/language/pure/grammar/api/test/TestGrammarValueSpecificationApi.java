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

package org.finos.legend.engine.language.pure.grammar.api.test;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.language.pure.grammar.api.grammarToJson.GrammarToJson;
import org.finos.legend.engine.language.pure.grammar.api.jsonToGrammar.JsonToGrammar;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.shared.core.api.TestGrammar;
import org.finos.legend.engine.shared.core.api.grammar.BatchResult;
import org.finos.legend.engine.shared.core.api.grammar.GrammarAPI;
import org.finos.legend.engine.shared.core.api.grammar.RenderStyle;
import org.finos.legend.engine.shared.core.function.Function5;
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
        testBatch(createBatchInput(Tuples.pair("1", "1 + 1"),
                Tuples.pair("2", "true->func(\n" +
                        "  2,\n" +
                        "  'www'\n" +
                        ")")));
    }

    @Test
    public void testBatchError()
    {
        testBatchError(createBatchInput(Tuples.pair("1", "1 + 1"),
                        Tuples.pair("2", "true->func(\n" +
                                "  2\n" +
                                "  'www'\n" +
                                ")")),
                createExpectedBatchResult(Tuples.pair("1", "1 + 1"),
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
    public Function5<String, String, Integer, Integer, Boolean, Response> grammarToJson()
    {
        return (a, b, c, d, e) -> grammarToJson.valueSpecification(a, b, c, d, e, null);
    }

    @Override
    public Function2<ValueSpecification, RenderStyle, Response> jsonToGrammar()
    {
        return (a, b) -> jsonToGrammar.valueSpecification(a, b, null);
    }

    @Override
    public Function<Map<String, GrammarAPI.ParserInput>, Response> grammarToJsonB()
    {
        return (a) -> grammarToJson.valueSpecificationBatch(a, null);
    }

    @Override
    public Function2<Map<String, ValueSpecification>, RenderStyle, Response> jsonToGrammarB()
    {
        return (a, b) -> jsonToGrammar.valueSpecificationBatch(a, b, null);
    }
}
