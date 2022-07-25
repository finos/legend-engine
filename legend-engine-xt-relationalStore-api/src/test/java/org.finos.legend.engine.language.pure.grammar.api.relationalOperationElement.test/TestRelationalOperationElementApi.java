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

package org.finos.legend.engine.language.pure.grammar.api.relationalOperationElement.test;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.language.pure.grammar.api.relationalOperationElement.RelationalOperationElementGrammarToJson;
import org.finos.legend.engine.language.pure.grammar.api.relationalOperationElement.RelationalOperationElementJsonToGrammar;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.operation.RelationalOperationElement;
import org.finos.legend.engine.shared.core.api.TestGrammar;
import org.finos.legend.engine.shared.core.api.grammar.BatchResult;
import org.finos.legend.engine.shared.core.api.grammar.GrammarAPI;
import org.finos.legend.engine.shared.core.api.grammar.RenderStyle;
import org.finos.legend.engine.shared.core.function.Function5;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.Map;

public class TestRelationalOperationElementApi extends TestGrammar<RelationalOperationElement>
{
    @Test
    public void testSimple()
    {
        test("add(1, 2)", true);
    }

    @Test
    public void testSimpleError()
    {
        testError("add(1,", "Unexpected token '<EOF>'", new SourceInformation("", 1, 7, 1, 11));
    }

    @Test
    public void testBatch()
    {
        testBatch(createBatchInput(Tuples.pair("1", "add(1, 2)"),
                Tuples.pair("2", "'4'")));
    }

    @Test
    public void testBatchError()
    {
        testBatchError(createBatchInput(Tuples.pair("1", "add(1"),
                        Tuples.pair("2", "'4'")),
                createExpectedBatchResult(Tuples.pair("1",
                                "{\"message\":\"Unexpected token '<EOF>'\",\"sourceInformation\":{\"endColumn\":10,\"endLine\":1,\"sourceId\":\"\",\"startColumn\":6,\"startLine\":1}}"),
                        Tuples.pair("2", "'4'"))
        );
    }

    private static final RelationalOperationElementGrammarToJson grammarToJson = new RelationalOperationElementGrammarToJson();
    private static final RelationalOperationElementJsonToGrammar jsonToGrammar = new RelationalOperationElementJsonToGrammar();

    @Override
    public Class<RelationalOperationElement> get_Class()
    {
        return RelationalOperationElement.class;
    }

    public static class MyClass extends BatchResult<RelationalOperationElement>
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
        return (a, b, c, d, e) -> grammarToJson.relationalOperationElement(a, b, c, d, e, null);
    }

    @Override
    public Function2<RelationalOperationElement, RenderStyle, Response> jsonToGrammar()
    {
        return (a, b) -> jsonToGrammar.relationalOperationElement(a, b, null);
    }

    @Override
    public Function<Map<String, GrammarAPI.ParserInput>, Response> grammarToJsonB()
    {
        return (a) -> grammarToJson.relationalOperationElementBatch(a, null);
    }

    @Override
    public Function2<Map<String, RelationalOperationElement>, RenderStyle, Response> jsonToGrammarB()
    {
        return (a, b) -> jsonToGrammar.relationalOperationElementBatch(a, b, null);
    }
}
