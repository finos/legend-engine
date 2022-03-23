// Copyright 2021 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.language.pure.grammar.test;

import org.antlr.v4.runtime.Vocabulary;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.ServiceStoreParserGrammar;
import org.junit.Test;

import java.util.List;

public class TestServiceStoreGrammarParser extends TestGrammarParser.TestGrammarParserTestSuite
{
    @Override
    public Vocabulary getParserGrammarVocabulary()
    {
        return ServiceStoreParserGrammar.VOCABULARY;
    }

    @Override
    public String getParserGrammarIdentifierInclusionTestCode(List<String> keywords)
    {
        return "###ServiceStore\n" +
                "ServiceStore " + ListAdapter.adapt(keywords).makeString("::") + "\n" +
                "(\n" +
                ")\n";
    }

    @Test
    public void testServiceStoreGrammarErrorMessages()
    {
        // Path ending with slash
        test("###ServiceStore\n" +
                "ServiceStore test::testServiceStoreGrammarWithSingleService\n" +
                "(\n" +
                "  Service TestService\n" +
                "  (\n" +
                "    path : '/testService/';\n" +
                "    method : GET;\n" +
                "    parameters :\n" +
                "    (\n" +
                "      serializationFormat : String ( location = query )\n" +
                "    );\n" +
                "    response : ExampleClass <- tests::store::exampleBinding;\n" +
                "    security : [];\n" +
                "  )\n"+
                ")\n", "PARSER error at [6:12-26]: Path should start with '/' & should not end with '/'");

        // Path not starting with slash
        test("###ServiceStore\n" +
                "ServiceStore test::testServiceStoreGrammarWithSingleService\n" +
                "(\n" +
                "  Service TestService\n" +
                "  (\n" +
                "    path : 'testService';\n" +
                "    method : GET;\n" +
                "    parameters :\n" +
                "    (\n" +
                "      serializationFormat : String ( location = query )\n" +
                "    );\n" +
                "    response : ExampleClass <- tests::store::exampleBinding;\n" +
                "    security : [];\n" +
                "  )\n"+
                ")\n", "PARSER error at [6:12-24]: Path should start with '/' & should not end with '/'");

        // List param should have style & explode
        test("###ServiceStore\n" +
                "ServiceStore test::testServiceStoreGrammarWithSingleService\n" +
                "(\n" +
                "  Service TestService\n" +
                "  (\n" +
                "    path : '/testService';\n" +
                "    method : GET;\n" +
                "    parameters :\n" +
                "    (\n" +
                "      serializationFormat : [String] ( location = query )\n" +
                "    );\n" +
                "    response : ExampleClass <- tests::store::exampleBinding;\n" +
                "    security : [];\n" +
                "  )\n"+
                ")\n", "PARSER error at [10:7-57]: Field 'style' is required");

        // List param should have style & explode
        test("###ServiceStore\n" +
                "ServiceStore test::testServiceStoreGrammarWithSingleService\n" +
                "(\n" +
                "  Service TestService\n" +
                "  (\n" +
                "    path : '/testService';\n" +
                "    method : GET;\n" +
                "    parameters :\n" +
                "    (\n" +
                "      serializationFormat : [String] ( location = query, style = simple )\n" +
                "    );\n" +
                "    response : ExampleClass <- tests::store::exampleBinding;\n" +
                "    security : [];\n" +
                "  )\n"+
                ")\n", "PARSER error at [10:7-73]: Field 'explode' is required");

        // Non-list param should not have style & explode
        test("###ServiceStore\n" +
                "ServiceStore test::testServiceStoreGrammarWithSingleService\n" +
                "(\n" +
                "  Service TestService\n" +
                "  (\n" +
                "    path : '/testService';\n" +
                "    method : GET;\n" +
                "    parameters :\n" +
                "    (\n" +
                "      serializationFormat : String ( location = query, style = simple, explode = true )\n" +
                "    );\n" +
                "    response : ExampleClass <- tests::store::exampleBinding ;\n" +
                "    security : [];\n" +
                "  )\n"+
                ")\n", "PARSER error at [10:56-69]: style should not be provided with non-list service parameter");

        // Service with non-unique id's
        test("###ServiceStore\n" +
                "ServiceStore test::testServiceStoreGrammarWithSingleService\n" +
                "(\n" +
                "  Service TestService\n" +
                "  (\n" +
                "    path : '/testService';\n" +
                "    method : GET;\n" +
                "    parameters :\n" +
                "    (\n" +
                "      serializationFormat : String ( location = query )\n" +
                "    );\n" +
                "    response : ExampleClass <- tests::store::exampleBinding;\n" +
                "    security : [];\n" +
                "  )\n"+
                "  Service TestService\n" +
                "  (\n" +
                "    path : '/testService';\n" +
                "    method : GET;\n" +
                "    parameters :\n" +
                "    (\n" +
                "      serializationFormat : String ( location = query )\n" +
                "    );\n" +
                "    response : ExampleClass <- tests::store::exampleBinding;\n" +
                "    security : [];\n" +
                "  )\n"+
                ")\n", "PARSER error at [2:1-26:1]: Service Store Elements should have unique ids. Multiple elements found with ids - [TestService]");

        // Elements with non-unique id's
        test("###ServiceStore\n" +
                "ServiceStore test::testServiceStoreGrammarWithSingleService\n" +
                "(\n" +
                "  Service TestServices\n" +
                "  (\n" +
                "    path : '/testService';\n" +
                "    method : GET;\n" +
                "    parameters :\n" +
                "    (\n" +
                "      serializationFormat : String ( location = query )\n" +
                "    );\n" +
                "    response : ExampleClass <- tests::store::exampleBinding;\n" +
                "    security : [];\n" +
                "  )\n"+
                "  ServiceGroup TestServices\n" +
                "  (\n" +
                "    path : '/testServices';\n\n" +
                "    Service TestService\n" +
                "    (\n" +
                "      path : '/testService';\n" +
                "      method : GET;\n" +
                "      parameters :\n" +
                "      (\n" +
                "        serializationFormat : String ( location = query )\n" +
                "      );\n" +
                "      response : ExampleClass <- tests::store::exampleBinding;\n" +
                "      security : [];\n" +
                "    )\n"+
                "  )\n" +
                ")\n", "PARSER error at [2:1-31:1]: Service Store Elements should have unique ids. Multiple elements found with ids - [TestServices]");

        // GET should not have requestBody defined
        test("###ServiceStore\n" +
                "ServiceStore test::testServiceStoreGrammarWithSingleService\n" +
                "(\n" +
                "  Service TestServices\n" +
                "  (\n" +
                "    path : '/testService';\n" +
                "    requestBody : ExampleClass <- tests::store::exampleBinding;\n" +
                "    method : GET;\n" +
                "    parameters :\n" +
                "    (\n" +
                "      serializationFormat : String ( location = query )\n" +
                "    );\n" +
                "    response : ExampleClass <- tests::store::exampleBinding;\n" +
                "    security : [];\n" +
                "  )\n"+
                ")\n", "PARSER error at [4:3-15:3]: Request Body should not be specified for GET end point");

        // Path param should be part of url
        test("###ServiceStore\n" +
                "ServiceStore test::testServiceStoreGrammarWithSingleService\n" +
                "(\n" +
                "  Service TestServices\n" +
                "  (\n" +
                "    path : '/testService';\n" +
                "    method : GET;\n" +
                "    parameters :\n" +
                "    (\n" +
                "      serializationFormat : String ( location = path )\n" +
                "    );\n" +
                "    response : ExampleClass <- tests::store::exampleBinding;\n" +
                "    security : [];\n" +
                "  )\n"+
                ")\n", "PARSER error at [4:3-14:3]: Path parameters should be specified in path as '{param_name}'. [serializationFormat] parameters were not found in path /testService");

        // Invalid security scheme
        test("###ServiceStore\n" +
                "ServiceStore test::testServiceStoreGrammarWithSingleService\n" +
                "(\n" +
                "  Service TestServices\n" +
                "  (\n" +
                "    path : '/testService';\n" +
                "    method : GET;\n" +
                "    parameters :\n" +
                "    (\n" +
                "      serializationFormat : String ( location = path )\n" +
                "    );\n" +
                "    response : ExampleClass <- tests::store::exampleBinding;\n" +
                "    security : [INVALID];\n" +
                "  )\n"+
                ")\n", "PARSER error at [13:17-23]: Unsupported SecurityScheme - INVALID");

        // Invalid method
        test("###ServiceStore\n" +
                "ServiceStore test::testServiceStoreGrammarWithSingleService\n" +
                "(\n" +
                "  Service TestServices\n" +
                "  (\n" +
                "    path : '/testService';\n" +
                "    method : PUT;\n" +
                "    parameters :\n" +
                "    (\n" +
                "      serializationFormat : String ( location = path )\n" +
                "    );\n" +
                "    response : ExampleClass <- tests::store::exampleBinding;\n" +
                "    security : [];\n" +
                "  )\n"+
                ")\n", "PARSER error at [7:5-17]: Unsupported HTTP Method type - PUT. Supported types are - GET,POST");

        // Invalid location
        test("###ServiceStore\n" +
                "ServiceStore test::testServiceStoreGrammarWithSingleService\n" +
                "(\n" +
                "  Service TestServices\n" +
                "  (\n" +
                "    path : '/testService';\n" +
                "    method : GET;\n" +
                "    parameters :\n" +
                "    (\n" +
                "      serializationFormat : String ( location = requestBody )\n" +
                "    );\n" +
                "    response : ExampleClass <- tests::store::exampleBinding;\n" +
                "    security : [];\n" +
                "  )\n"+
                ")\n", "PARSER error at [10:7-61]: Unsupported Parameter Location - requestBody. Supported Locations are - header,path,query");

        // Path parameters can't be optional
        test("###ServiceStore\n" +
                "ServiceStore test::testServiceStoreGrammarWithSingleService\n" +
                "(\n" +
                "  Service TestServices\n" +
                "  (\n" +
                "    path : '/testService/{serializationFormat}';\n" +
                "    method : GET;\n" +
                "    parameters :\n" +
                "    (\n" +
                "      serializationFormat : String ( location = path, required = false )\n" +
                "    );\n" +
                "    response : ExampleClass <- tests::store::exampleBinding;\n" +
                "    security : [];\n" +
                "  )\n"+
                ")\n", "PARSER error at [10:55-70]: Path parameters cannot be optional");
    }
}