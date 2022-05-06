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

package org.finos.legend.engine.language.pure.compiler.test;

import org.junit.Test;

import static org.finos.legend.engine.language.pure.compiler.test.TestServiceStoreCompilationUtils.TEST_BINDING;

public class TestServiceStoreCompilationFromGrammar extends TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite
{
    @Override
    public String getDuplicatedElementTestCode()
    {
        return "Class anything::class {}\n" +
                "###Mapping\n" +
                "Mapping anything::somethingelse ()\n" +
                "###ServiceStore\n" +
                "ServiceStore anything::somethingelse\n" +
                "(\n" +
                ")";
    }

    @Override
    public String getDuplicatedElementTestExpectedErrorMessage()
    {
        return "COMPILATION error at [5:1-7:1]: Duplicated element 'anything::somethingelse'";
    }

    @Test
    public void testEmptyServiceStoreCompilation()
    {
        test("###ServiceStore\n" +
                "ServiceStore test::testEmptyServiceStoreCompilation\n" +
                "(\n" +
                ")\n");
    }

    @Test
    public void testServiceStoreCompilationWithSingleService()
    {
        test(TEST_BINDING + "###ServiceStore\n" +
                "ServiceStore test::testServiceStoreCompilationWithSingleService\n" +
                "(\n" +
                "  Service TestService\n" +
                "  (\n" +
                "    path : '/testService';\n" +
                "    method : GET;\n" +
                "    parameters :\n" +
                "    (\n" +
                "      serializationFormat : String ( location = query )\n" +
                "    );\n" +
                "    response : [ test::Person <- test::TestBinding ];\n" +
                "    security : [];\n" +
                "  )\n" +
                ")\n");

        test(TEST_BINDING + "###ServiceStore\n" +
                "ServiceStore test::testServiceStoreCompilationWithSingleService\n" +
                "(\n" +
                "  Service TestService\n" +
                "  (\n" +
                "    path : '/testService';\n" +
                "    method : GET;\n" +
                "    parameters :\n" +
                "    (\n" +
                "      serializationFormat : String ( location = header )\n" +
                "    );\n" +
                "    response : [ test::Person <- test::TestBinding ];\n" +
                "    security : [];\n" +
                "  )\n" +
                ")\n");

        test(TEST_BINDING + "###ServiceStore\n" +
                "ServiceStore test::testServiceStoreCompilationWithSingleService\n" +
                "(\n" +
                "  Service TestService\n" +
                "  (\n" +
                "    path : '/testService';\n" +
                "    method : GET;\n" +
                "    parameters :\n" +
                "    (\n" +
                "      \"abc.xyz\" : String ( location = query )\n" +
                "    );\n" +
                "    response : [ test::Person <- test::TestBinding ];\n" +
                "    security : [];\n" +
                "  )\n" +
                ")\n");

        test(TEST_BINDING + "###ServiceStore\n" +
                "ServiceStore test::testServiceStoreCompilationWithSingleService\n" +
                "(\n" +
                "  Service TestService\n" +
                "  (\n" +
                "    path : '/testService';\n" +
                "    method : GET;\n" +
                "    parameters :\n" +
                "    (\n" +
                "      serializationFormat : [String] ( location = query, style = simple, explode = true )\n" +
                "    );\n" +
                "    response : [ test::Person <- test::TestBinding ];\n" +
                "    security : [];\n" +
                "  )\n" +
                ")\n");

        test(TEST_BINDING + "###ServiceStore\n" +
                "ServiceStore test::testServiceStoreCompilationWithSingleService\n" +
                "(\n" +
                "  Service TestService\n" +
                "  (\n" +
                "    path : '/testService';\n" +
                "    method : GET;\n" +
                "    parameters :\n" +
                "    (\n" +
                "      serializationFormat : [Integer] ( location = query, style = simple, explode = true, enum = test::Enum )\n" +
                "    );\n" +
                "    response : test::Person <- test::TestBinding;\n" +
                "    security : [];\n" +
                "  )\n" +
                ")\n");

        test(TEST_BINDING + "###ServiceStore\n" +
                "ServiceStore test::testServiceStoreCompilationWithSingleService\n" +
                "(\n" +
                "  Service TestService\n" +
                "  (\n" +
                "    path : '/testService/{param2}';\n" +
                "    method : GET;\n" +
                "    parameters :\n" +
                "    (\n" +
                "      param1 : String ( location = query ),\n" +
                "      param2 : String ( location = path )\n" +
                "    );\n" +
                "    response : test::Person <- test::TestBinding;\n" +
                "    security : [];\n" +
                "  )\n" +
                ")\n");

        test(TEST_BINDING + "###ServiceStore\n" +
                "ServiceStore test::testServiceStoreCompilationWithSingleService\n" +
                "(\n" +
                "  Service TestService\n" +
                "  (\n" +
                "    path : '/testService/{param2}';\n" +
                "    requestBody : test::Person <- test::TestBinding;\n" +
                "    method : POST;\n" +
                "    parameters :\n" +
                "    (\n" +
                "      param1 : String ( location = query ),\n" +
                "      param2 : String ( location = path )\n" +
                "    );\n" +
                "    response : test::Person <- test::TestBinding;\n" +
                "    security : [];\n" +
                "  )\n" +
                ")\n");

        test(TEST_BINDING + "###ServiceStore\n" +
                "ServiceStore test::testServiceStoreCompilationWithSingleService\n" +
                "(\n" +
                "  Service TestService\n" +
                "  (\n" +
                "    path : '/testService/{param2}';\n" +
                "    requestBody : test::Person <- test::TestBinding;\n" +
                "    method : POST;\n" +
                "    parameters :\n" +
                "    (\n" +
                "      param1 : String ( location = query, allowReserved = true ),\n" +
                "      param2 : String ( location = path, allowReserved = false )\n" +
                "    );\n" +
                "    response : test::Person <- test::TestBinding;\n" +
                "    security : [];\n" +
                "  )\n" +
                ")\n");
    }

    @Test
    public void testServiceStoreCompilationWithSingleServiceGroup()
    {
        test(TEST_BINDING + "###ServiceStore\n" +
                "ServiceStore test::testServiceStoreCompilationWithSingleServiceGroup\n" +
                "(\n" +
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
                "      response : test::Person <- test::TestBinding;\n" +
                "      security : [];\n" +
                "    )\n" +
                "  )\n" +
                ")\n");

        test(TEST_BINDING + "###ServiceStore\n" +
                "ServiceStore test::testServiceStoreCompilationWithSingleServiceGroup\n" +
                "(\n" +
                "  ServiceGroup TestServices\n" +
                "  (\n" +
                "    path : '/testServices';\n\n" +
                "    Service TestService1\n" +
                "    (\n" +
                "      path : '/testService';\n" +
                "      method : GET;\n" +
                "      parameters :\n" +
                "      (\n" +
                "        serializationFormat : String ( location = query )\n" +
                "      );\n" +
                "      response : test::Person <- test::TestBinding;\n" +
                "      security : [];\n" +
                "    )\n" +
                "    Service TestService2\n" +
                "    (\n" +
                "      path : '/testService';\n" +
                "      requestBody : test::Person <- test::TestBinding;\n" +
                "      method : POST;\n" +
                "      parameters :\n" +
                "      (\n" +
                "        serializationFormat : String ( location = query )\n" +
                "      );\n" +
                "      response : test::Person <- test::TestBinding;\n" +
                "      security : [];\n" +
                "    )\n" +
                "  )\n" +
                ")\n");
    }

    @Test
    public void testServiceStoreCompilationWithSingleServiceAndServiceGroup()
    {
        test(TEST_BINDING + "###ServiceStore\n" +
                "ServiceStore test::testServiceStoreCompilationWithSingleServiceAndServiceGroup\n" +
                "(\n" +
                "  Service TestService\n" +
                "  (\n" +
                "    path : '/testService/{param2}';\n" +
                "    requestBody : test::Person <- test::TestBinding;\n" +
                "    method : POST;\n" +
                "    parameters :\n" +
                "    (\n" +
                "      param1 : String ( location = query ),\n" +
                "      param2 : String ( location = path )\n" +
                "    );\n" +
                "    response : test::Person <- test::TestBinding;\n" +
                "    security : [];\n" +
                "  )\n" +
                "  ServiceGroup TestServices\n" +
                "  (\n" +
                "    path : '/testServices';\n\n" +
                "    Service NestedService\n" +
                "    (\n" +
                "      path : '/testService';\n" +
                "      method : GET;\n" +
                "      parameters :\n" +
                "      (\n" +
                "        serializationFormat : String ( location = query )\n" +
                "      );\n" +
                "      response : test::Person <- test::TestBinding;\n" +
                "      security : [];\n" +
                "    )\n" +
                "  )\n" +
                ")\n");
    }

    @Test
    public void testServiceStoreCompilationWithMultipleServicesAndServiceGroups()
    {
        test(TEST_BINDING + "###ServiceStore\n" +
                "ServiceStore test::testServiceStoreCompilationWithMultipleServicesAndServiceGroups\n" +
                "(\n" +
                "  Service TestService1\n" +
                "  (\n" +
                "    path : '/testService/{param2}';\n" +
                "    requestBody : test::Person <- test::TestBinding;\n" +
                "    method : POST;\n" +
                "    parameters :\n" +
                "    (\n" +
                "      param1 : String ( location = query ),\n" +
                "      param2 : String ( location = path )\n" +
                "    );\n" +
                "    response : test::Person <- test::TestBinding;\n" +
                "    security : [];\n" +
                "  )\n" +
                "  Service TestService2\n" +
                "  (\n" +
                "    path : '/testService';\n" +
                "    method : GET;\n" +
                "    parameters :\n" +
                "    (\n" +
                "      serializationFormat : [Integer] ( location = query, style = simple, explode = true, enum = test::Enum )\n" +
                "    );\n" +
                "    response : test::Person <- test::TestBinding;\n" +
                "    security : [];\n" +
                "  )\n" +
                "  ServiceGroup TestServices1\n" +
                "  (\n" +
                "    path : '/testServices1';\n\n" +
                "    Service NestedService\n" +
                "    (\n" +
                "      path : '/testService';\n" +
                "      method : GET;\n" +
                "      parameters :\n" +
                "      (\n" +
                "        serializationFormat : String ( location = query )\n" +
                "      );\n" +
                "      response : test::Person <- test::TestBinding;\n" +
                "      security : [];\n" +
                "    )\n" +
                "  )\n" +
                "  ServiceGroup TestServices2\n" +
                "  (\n" +
                "    path : '/testServices1';\n\n" +
                "    ServiceGroup NestedServiceGroup\n" +
                "    (\n" +
                "      path : '/testServices1';\n\n" +
                "      Service NestedService\n" +
                "      (\n" +
                "        path : '/testService';\n" +
                "        method : GET;\n" +
                "        parameters :\n" +
                "        (\n" +
                "          serializationFormat : String ( location = query )\n" +
                "        );\n" +
                "        response : test::Person <- test::TestBinding;\n" +
                "        security : [];\n" +
                "      )\n" +
                "    )\n" +
                "  )\n" +
                "  ServiceGroup TestServices3\n" +
                "  (\n" +
                "    path : '/testServices2';\n\n" +
                "    Service TestService1\n" +
                "    (\n" +
                "      path : '/testService';\n" +
                "      method : GET;\n" +
                "      parameters :\n" +
                "      (\n" +
                "        serializationFormat : String ( location = query )\n" +
                "      );\n" +
                "      response : test::Person <- test::TestBinding;\n" +
                "      security : [];\n" +
                "    )\n" +
                "    Service TestService2\n" +
                "    (\n" +
                "      path : '/testService';\n" +
                "      method : GET;\n" +
                "      parameters :\n" +
                "      (\n" +
                "        serializationFormat : String ( location = query )\n" +
                "      );\n" +
                "      response : test::Person <- test::TestBinding;\n" +
                "      security : [];\n" +
                "    )\n" +
                "  )\n" +
                ")\n");
    }

    @Test
    public void testServiceStoreCompilationErrorMessages()
    {
        // Invalid binding path
        test(TEST_BINDING + "###ServiceStore\n" +
                "ServiceStore test::testServiceStoreCompilationWithSingleService\n" +
                "(\n" +
                "  Service TestService\n" +
                "  (\n" +
                "    path : '/testService';\n" +
                "    method : GET;\n" +
                "    parameters :\n" +
                "    (\n" +
                "      serializationFormat : String ( location = query )\n" +
                "    );\n" +
                "    response : [ test::Person <- test::TestBinding1 ];\n" +
                "    security : [];\n" +
                "  )\n" +
                ")\n", "COMPILATION error at [34:1-47:1]: Error in 'test::testServiceStoreCompilationWithSingleService': Can't find (external format) Binding 'test::TestBinding1'");

        // Invalid model from binding
        test(TEST_BINDING + "###ServiceStore\n" +
                "ServiceStore test::testServiceStoreCompilationWithSingleService\n" +
                "(\n" +
                "  Service TestService\n" +
                "  (\n" +
                "    path : '/testService';\n" +
                "    method : GET;\n" +
                "    parameters :\n" +
                "    (\n" +
                "      serializationFormat : String ( location = query )\n" +
                "    );\n" +
                "    response : [ test::Trade <- test::TestBinding ];\n" +
                "    security : [];\n" +
                "  )\n" +
                ")\n", "COMPILATION error at [34:1-47:1]: Error in 'test::testServiceStoreCompilationWithSingleService': Can't find class 'test::Trade'");

        // Header params can't have reserved names - Accept, Content-Type and Authorization
        test(TEST_BINDING + "###ServiceStore\n" +
                "ServiceStore test::testServiceStoreCompilationWithSingleService\n" +
                "(\n" +
                "  Service TestService\n" +
                "  (\n" +
                "    path : '/testService';\n" +
                "    method : GET;\n" +
                "    parameters :\n" +
                "    (\n" +
                "      Accept : String ( location = header )\n" +
                "    );\n" +
                "    response : [ test::Person <- test::TestBinding ];\n" +
                "    security : [];\n" +
                "  )\n" +
                ")\n", "COMPILATION error at [42:7-43]: Header parameters cannot have following names : [Accept,Content-Type,Authorization]");

        test(TEST_BINDING + "###ServiceStore\n" +
                "ServiceStore test::testServiceStoreCompilationWithSingleService\n" +
                "(\n" +
                "  Service TestService\n" +
                "  (\n" +
                "    path : '/testService';\n" +
                "    method : GET;\n" +
                "    parameters :\n" +
                "    (\n" +
                "      \"Content-Type\" : String ( location = header )\n" +
                "    );\n" +
                "    response : [ test::Person <- test::TestBinding ];\n" +
                "    security : [];\n" +
                "  )\n" +
                ")\n", "COMPILATION error at [42:7-51]: Header parameters cannot have following names : [Accept,Content-Type,Authorization]");

        test(TEST_BINDING + "###ServiceStore\n" +
                "ServiceStore test::testServiceStoreCompilationWithSingleService\n" +
                "(\n" +
                "  Service TestService\n" +
                "  (\n" +
                "    path : '/testService';\n" +
                "    method : GET;\n" +
                "    parameters :\n" +
                "    (\n" +
                "      Authorization : String ( location = header )\n" +
                "    );\n" +
                "    response : [ test::Person <- test::TestBinding ];\n" +
                "    security : [];\n" +
                "  )\n" +
                ")\n", "COMPILATION error at [42:7-50]: Header parameters cannot have following names : [Accept,Content-Type,Authorization]");
    }
}
