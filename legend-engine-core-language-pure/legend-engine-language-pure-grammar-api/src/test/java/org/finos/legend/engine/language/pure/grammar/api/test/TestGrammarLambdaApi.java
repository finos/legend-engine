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
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.shared.core.api.TestGrammar;
import org.finos.legend.engine.shared.core.api.grammar.BatchResult;
import org.finos.legend.engine.shared.core.api.grammar.GrammarAPI;
import org.finos.legend.engine.shared.core.api.grammar.RenderStyle;
import org.finos.legend.engine.shared.core.function.Function5;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.Map;

public class TestGrammarLambdaApi extends TestGrammar<Lambda>
{
    @Test
    public void testSimple()
    {
        test("|1", true);
        test("|1", false);

        test("a: String[1]|'hello'", true);
        test("src: String[1]|$src", true);
        test("src: Integer[1]|$src + 1", true);
        test("src: Integer[2]|$src->first()->toOne()", true);
        test("src: Person[1]|$src.nameWithTitle('test')", true);
        test("src: Person[1]|$src.name", true);
        test("src: Boolean[1]|!($src)", true);
        test("src: Integer[1]|$src->minus()", true);
        test("src: Integer[1]|$src->add(\n" +
                "  1 - 1\n" +
                ")", true);
        test("{src: Integer[1]|\n" +
                "  let a = 1;\n" +
                "  $a + 1;\n" +
                "}", true);
        test("src: Integer[1]|myEnum.VALUE1", true);
        test("src: Integer[1]|anything", true);
        // a really meaningless lambda
        test("|anything", true);
    }

    @Test
    public void testSimpleParsingError()
    {
        testError("|1->toString(),", "no viable alternative at input '->toString(),'", new SourceInformation("", 1, 15, 1, 15));
    }

    @Test
    public void testBatch()
    {
        testBatch(createBatchInput(Tuples.pair("1", "a: String[1]|'hello'"),
                Tuples.pair("2", "src: String[1]|$src"),
                Tuples.pair("3", "src: Integer[2]|$src->first()->toOne()"),
                Tuples.pair("4", "{src: Integer[1]|\n  let a = 1;\n  $a + 1;\n}"))
        );
    }

    @Test
    public void testBatchError()
    {
        testBatchError(createBatchInput(Tuples.pair("1", "a: String[1]|'hello'"),
                        Tuples.pair("2", "src: String[1]|$src,")),
                createExpectedBatchResult(Tuples.pair("1", "a: String[1]|'hello'"),
                        Tuples.pair("2", "{\"message\":\"Unexpected token\",\"sourceInformation\":{\"endColumn\":20,\"endLine\":1,\"sourceId\":\"\",\"startColumn\":20,\"startLine\":1}}"))
        );
    }


    private static final GrammarToJson grammarToJson = new GrammarToJson();
    private static final JsonToGrammar jsonToGrammar = new JsonToGrammar();

    @Override
    public Class<Lambda> get_Class()
    {
        return Lambda.class;
    }

    public static class MyClass extends BatchResult<Lambda>
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
        return (a, b, c, d, e) -> grammarToJson.lambda(a, b, c, d, e, null);
    }

    @Override
    public Function2<Lambda, RenderStyle, Response> jsonToGrammar()
    {
        return (a, b) -> jsonToGrammar.lambda(a, b, null);
    }

    @Override
    public Function<Map<String, GrammarAPI.ParserInput>, Response> grammarToJsonB()
    {
        return (a) -> grammarToJson.lambdaBatch(a, null);
    }

    @Override
    public Function2<Map<String, Lambda>, RenderStyle, Response> jsonToGrammarB()
    {
        return (a, b) -> jsonToGrammar.lambdaBatch(a, b, null);
    }
}
