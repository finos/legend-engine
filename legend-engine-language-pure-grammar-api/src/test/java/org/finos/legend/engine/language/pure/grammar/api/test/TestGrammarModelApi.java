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
import org.finos.legend.engine.language.pure.grammar.api.grammarToJson.GrammarToJson;
import org.finos.legend.engine.language.pure.grammar.api.jsonToGrammar.JsonToGrammar;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.api.TestGrammar;
import org.finos.legend.engine.shared.core.api.grammar.GrammarAPI;
import org.finos.legend.engine.shared.core.api.grammar.RenderStyle;
import org.finos.legend.engine.shared.core.function.Function5;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.Map;

public class TestGrammarModelApi extends TestGrammar<PureModelContextData>
{
    @Test
    public void testSimple()
    {
        test("Class A\n{\n}\n", true);
        test("###Mapping\n" + "Mapping meta::pure::mapping::modelToModel::test::simple::simpleModelMapping\n" + "(\n" + "  *meta::pure::mapping::modelToModel::test::shared::dest::Person[meta_pure_mapping_modelToModel_test_shared_dest_Person]: Pure\n" + "  {\n" + "    ~src meta::pure::mapping::modelToModel::test::shared::src::_S_Person\n" + "    firstName: $src.fullName->substring(\n" + "  0,\n" + "  $src.fullName->indexOf(' ')\n" + ")\n" + "  }\n" + ")\n",
                true);
    }

    @Test
    public void testSimpleError()
    {
        testError("Class A\n{", "Unexpected token", new SourceInformation("", 2, 1, 2, 1));
    }

    private static final GrammarToJson grammarToJson = new GrammarToJson();
    private static final JsonToGrammar jsonToGrammar = new JsonToGrammar();

    @Override
    public Class<PureModelContextData> get_Class()
    {
        return PureModelContextData.class;
    }

    public static class MyClass extends LinkedHashMap<String, PureModelContextData>
    {
    }

    @Override
    public Class getBatchResultSpecializedClass()
    {
        return TestGrammarLambdaApi.MyClass.class;
    }

    @Override
    public Function5<String, String, Integer, Integer, Boolean, Response> grammarToJson()
    {
        return (a, b, c, d, e) -> grammarToJson.model(a, b, c, e, null);
    }

    @Override
    public Function2<PureModelContextData, RenderStyle, Response> jsonToGrammar()
    {
        return (a, b) -> jsonToGrammar.model(a, b, null);
    }

    @Override
    public Function<Map<String, GrammarAPI.ParserInput>, Response> grammarToJsonB()
    {
        throw new RuntimeException("Not supported here");
    }

    @Override
    public Function2<Map<String, PureModelContextData>, RenderStyle, Response> jsonToGrammarB()
    {
        throw new RuntimeException("Not supported here");
    }

}
