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
import org.finos.legend.engine.protocol.pure.v1.model.ElementBasedSourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.api.TestGrammar;
import org.finos.legend.engine.shared.core.api.grammar.GrammarAPI;
import org.finos.legend.engine.shared.core.api.grammar.RenderStyle;
import org.finos.legend.engine.shared.core.function.Function5;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionError;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

    @Test
    public void testElementsSimple()
    {
        testElement(createElementInput(Tuples.pair("A", "###Pure\nClass A\n{\n}"), Tuples.pair("B", "###Pure\nClass B\n{\n}")), true);
    }

    @Test
    public void testElementsSimpleError()
    {
        testElementError(createElementInput(Tuples.pair("A", "###Pure\nClass A\n{")), "Unexpected token", new ElementBasedSourceInformation("", 3, 1, 3, 1, "A"));
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
    public Function<Map<String, GrammarAPI.ParserInput>, Response> grammarToJsonBatch()
    {
        throw new RuntimeException("Not supported here");
    }

    @Override
    public Function2<Map<String, PureModelContextData>, RenderStyle, Response> jsonToGrammarBatch()
    {
        throw new RuntimeException("Not supported here");
    }

    public Function5<Map<String, String>, String, Integer, Integer, Boolean, Response> grammarToJsonElement()
    {
        return (a, b, c, d, e) -> grammarToJson.elements(a, b, c, e, null);
    }

    public Function2<PureModelContextData, RenderStyle, Response> jsonToGrammarElement()
    {
        return (a, b) -> jsonToGrammar.elements(a, b, null);
    }

    protected void testElement(Map<String, String> input, boolean returnSourceInformation)
    {
        try
        {
            Response result = grammarToJsonElement().value(input, "", 0, 0, returnSourceInformation);
            String actual = result.getEntity().toString();
            Response newResult = jsonToGrammarElement().apply(ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().readValue(actual, get_Class()), RenderStyle.PRETTY);
            assertEquals(input, newResult.getEntity());
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    protected void testElementError(Map<String, String> input, String expectedErrorMessage, ElementBasedSourceInformation expectedErrorSourceInformation)
    {
        try
        {
            Response result = grammarToJsonElement().value(input, "", 0, 0, true);
            Object errorObject = result.getEntity();
            assertTrue(errorObject instanceof ExceptionError);
            ExceptionError error = (ExceptionError) errorObject;
            ElementBasedSourceInformation sourceInformation = (ElementBasedSourceInformation) error.getSourceInformation();
            assertEquals(expectedErrorMessage, error.getMessage());
            assertEquals(expectedErrorSourceInformation.sourceId, sourceInformation.sourceId);
            assertEquals(expectedErrorSourceInformation.startLine, sourceInformation.startLine);
            assertEquals(expectedErrorSourceInformation.startColumn, sourceInformation.startColumn);
            assertEquals(expectedErrorSourceInformation.endLine, sourceInformation.endLine);
            assertEquals(expectedErrorSourceInformation.endColumn, sourceInformation.endColumn);
            assertEquals(expectedErrorSourceInformation.elementPath, sourceInformation.elementPath);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
