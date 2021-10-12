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

import org.junit.Test;

public class TestServiceStoreGrammarRoundtrip extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{
    @Test
    public void testEmptyServiceStoreGrammar()
    {
        test("###ServiceStore\n" +
                "ServiceStore test::testEmptyServiceStoreGrammar\n" +
                "(\n" +
                ")\n");
    }

    @Test
    public void testServiceStoreGrammarWithSingleService()
    {
        test("###ServiceStore\n" +
                "ServiceStore test::testServiceStoreGrammarWithSingleService\n" +
                "(\n" +
                "  Service TestService\n" +
                "  (\n" +
                "    path : '/testService';\n" +
                "    method : GET;\n" +
                "    parameters : \n" +
                "    (\n" +
                "      serializationFormat : String ( location = query )\n" +
                "    );\n" +
                "    response : ExampleClass <- tests::store::exampleBinding;\n" +
                "    security : [];\n" +
                "  )\n"+
                ")\n");

        test("###ServiceStore\n" +
                "ServiceStore test::testServiceStoreGrammarWithSingleService\n" +
                "(\n" +
                "  Service TestService\n" +
                "  (\n" +
                "    path : '/testService';\n" +
                "    method : GET;\n" +
                "    parameters : \n" +
                "    (\n" +
                "      serializationFormat : [String] ( location = query, style = simple, explode = true )\n" +
                "    );\n" +
                "    response : ExampleClass <- tests::store::exampleBinding;\n" +
                "    security : [];\n" +
                "  )\n"+
                ")\n");

        test("###ServiceStore\n" +
                "ServiceStore test::testServiceStoreGrammarWithSingleService\n" +
                "(\n" +
                "  Service TestService\n" +
                "  (\n" +
                "    path : '/testService';\n" +
                "    method : GET;\n" +
                "    parameters : \n" +
                "    (\n" +
                "      serializationFormat : [Integer] ( location = query, style = simple, explode = true, enum = test::Enum )\n" +
                "    );\n" +
                "    response : ExampleClass <- tests::store::exampleBinding;\n" +
                "    security : [];\n" +
                "  )\n"+
                ")\n");

        test("###ServiceStore\n" +
                "ServiceStore test::testServiceStoreGrammarWithSingleService\n" +
                "(\n" +
                "  Service TestService\n" +
                "  (\n" +
                "    path : '/testService/{param2}';\n" +
                "    method : GET;\n" +
                "    parameters : \n" +
                "    (\n" +
                "      param1 : String ( location = query ),\n" +
                "      param2 : String ( location = path )\n" +
                "    );\n" +
                "    response : ExampleClass <- tests::store::exampleBinding;\n" +
                "    security : [];\n" +
                "  )\n"+
                ")\n");

        test("###ServiceStore\n" +
                "ServiceStore test::testServiceStoreGrammarWithSingleService\n" +
                "(\n" +
                "  Service TestService\n" +
                "  (\n" +
                "    path : '/testService/{param2}';\n" +
                "    requestBody : [ExampleClass <- tests::store::exampleBinding];\n" +
                "    method : POST;\n" +
                "    parameters : \n" +
                "    (\n" +
                "      param1 : String ( location = query ),\n" +
                "      param2 : String ( location = path )\n" +
                "    );\n" +
                "    response : ExampleClass <- tests::store::exampleBinding;\n" +
                "    security : [];\n" +
                "  )\n"+
                ")\n");

        test("###ServiceStore\n" +
                "ServiceStore test::testServiceStoreGrammarWithSingleService\n" +
                "(\n" +
                "  Service TestService\n" +
                "  (\n" +
                "    path : '/testService';\n" +
                "    method : GET;\n" +
                "    response : ExampleClass <- tests::store::exampleBinding;\n" +
                "    security : [];\n" +
                "  )\n"+
                ")\n");
    }

    @Test
    public void testServiceStoreGrammarWithSingleServiceGroup()
    {
        test("###ServiceStore\n" +
                "ServiceStore test::testServiceStoreGrammarWithSingleServiceGroup\n" +
                "(\n" +
                "  ServiceGroup TestServices\n" +
                "  (\n" +
                "    path : '/testServices';\n\n" +
                "    Service TestService\n" +
                "    (\n" +
                "      path : '/testService';\n" +
                "      method : GET;\n" +
                "      parameters : \n" +
                "      (\n" +
                "        serializationFormat : String ( location = query )\n" +
                "      );\n" +
                "      response : ExampleClass <- tests::store::exampleBinding;\n" +
                "      security : [];\n" +
                "    )\n"+
                "  )\n" +
                ")\n");

        test("###ServiceStore\n" +
                "ServiceStore test::testServiceStoreGrammarWithSingleServiceGroup\n" +
                "(\n" +
                "  ServiceGroup TestServices\n" +
                "  (\n" +
                "    path : '/testServices';\n\n" +
                "    Service TestService1\n" +
                "    (\n" +
                "      path : '/testService';\n" +
                "      method : GET;\n" +
                "      parameters : \n" +
                "      (\n" +
                "        serializationFormat : String ( location = query )\n" +
                "      );\n" +
                "      response : ExampleClass <- tests::store::exampleBinding;\n" +
                "      security : [];\n" +
                "    )\n"+
                "    Service TestService2\n" +
                "    (\n" +
                "      path : '/testService';\n" +
                "      requestBody : ExampleClass <- tests::store::exampleBinding;\n" +
                "      method : POST;\n" +
                "      parameters : \n" +
                "      (\n" +
                "        serializationFormat : String ( location = query )\n" +
                "      );\n" +
                "      response : ExampleClass <- tests::store::exampleBinding;\n" +
                "      security : [];\n" +
                "    )\n"+
                "  )\n" +
                ")\n");
    }

    @Test
    public void testServiceStoreGrammarWithSingleServiceAndServiceGroup()
    {
        test("###ServiceStore\n" +
                "ServiceStore test::testServiceStoreGrammarWithSingleService\n" +
                "(\n" +
                "  Service TestService\n" +
                "  (\n" +
                "    path : '/testService/{param2}';\n" +
                "    requestBody : [ExampleClass <- tests::store::exampleBinding];\n" +
                "    method : POST;\n" +
                "    parameters : \n" +
                "    (\n" +
                "      param1 : String ( location = query ),\n" +
                "      param2 : String ( location = path )\n" +
                "    );\n" +
                "    response : ExampleClass <- tests::store::exampleBinding;\n" +
                "    security : [];\n" +
                "  )\n"+
                "  ServiceGroup TestServices\n" +
                "  (\n" +
                "    path : '/testServices';\n\n" +
                "    Service NestedService\n" +
                "    (\n" +
                "      path : '/testService';\n" +
                "      method : GET;\n" +
                "      parameters : \n" +
                "      (\n" +
                "        serializationFormat : String ( location = query )\n" +
                "      );\n" +
                "      response : ExampleClass <- tests::store::exampleBinding;\n" +
                "      security : [];\n" +
                "    )\n"+
                "  )\n" +
                ")\n");
    }

    @Test
    public void testServiceStoreGrammarWithMultipleServicesAndServiceGroups()
    {
        test("###ServiceStore\n" +
                "ServiceStore test::testServiceStoreGrammarWithSingleService\n" +
                "(\n" +
                "  Service TestService1\n" +
                "  (\n" +
                "    path : '/testService/{param2}';\n" +
                "    requestBody : [ExampleClass <- tests::store::exampleBinding];\n" +
                "    method : POST;\n" +
                "    parameters : \n" +
                "    (\n" +
                "      param1 : String ( location = query ),\n" +
                "      param2 : String ( location = path )\n" +
                "    );\n" +
                "    response : ExampleClass <- tests::store::exampleBinding;\n" +
                "    security : [];\n" +
                "  )\n"+
                "  Service TestService2\n" +
                "  (\n" +
                "    path : '/testService';\n" +
                "    method : GET;\n" +
                "    parameters : \n" +
                "    (\n" +
                "      serializationFormat : [Integer] ( location = query, style = simple, explode = true, enum = test::Enum )\n" +
                "    );\n" +
                "    response : ExampleClass <- tests::store::exampleBinding;\n" +
                "    security : [];\n" +
                "  )\n"+
                "  ServiceGroup TestServices3\n" +
                "  (\n" +
                "    path : '/testServices1';\n\n" +
                "    Service NestedService\n" +
                "    (\n" +
                "      path : '/testService';\n" +
                "      method : GET;\n" +
                "      parameters : \n" +
                "      (\n" +
                "        serializationFormat : String ( location = query )\n" +
                "      );\n" +
                "      response : ExampleClass <- tests::store::exampleBinding;\n" +
                "      security : [];\n" +
                "    )\n"+
                "  )\n" +
                "  ServiceGroup TestServices4\n" +
                "  (\n" +
                "    path : '/testServices1';\n\n" +
                "    ServiceGroup NestedServiceGroup\n" +
                "    (\n" +
                "      path : '/testServices1';\n\n" +
                "      Service NestedService\n" +
                "      (\n" +
                "        path : '/testService';\n" +
                "        method : GET;\n" +
                "        parameters : \n" +
                "        (\n" +
                "          serializationFormat : String ( location = query )\n" +
                "        );\n" +
                "        response : ExampleClass <- tests::store::exampleBinding;\n" +
                "        security : [];\n" +
                "      )\n"+
                "    )\n" +
                "  )\n" +
                "  ServiceGroup TestServices6\n" +
                "  (\n" +
                "    path : '/testServices2';\n\n" +
                "    Service TestService1\n" +
                "    (\n" +
                "      path : '/testService';\n" +
                "      method : GET;\n" +
                "      parameters : \n" +
                "      (\n" +
                "        serializationFormat : String ( location = query )\n" +
                "      );\n" +
                "      response : ExampleClass <- tests::store::exampleBinding;\n" +
                "      security : [];\n" +
                "    )\n"+
                "    Service TestService2\n" +
                "    (\n" +
                "      path : '/testService';\n" +
                "      method : GET;\n" +
                "      parameters : \n" +
                "      (\n" +
                "        serializationFormat : String ( location = query )\n" +
                "      );\n" +
                "      response : ExampleClass <- tests::store::exampleBinding;\n" +
                "      security : [];\n" +
                "    )\n"+
                "  )\n" +
                ")\n");
    }
}
