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

import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.junit.Assert;
import org.junit.Test;

import static org.finos.legend.engine.language.pure.compiler.test.TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite.test;
import static org.finos.legend.engine.language.pure.compiler.test.TestServiceStoreCompilationUtils.FLATDATA_BINDING;
import static org.finos.legend.engine.language.pure.compiler.test.TestServiceStoreCompilationUtils.JSON_BINDING;

public class TestServiceStoreMappingCompilationFromGrammar
{
    @Test
    public void testSimpleServiceStoreMappings()
    {
        // Without ParameterMapping & simple service path
        test(FLATDATA_BINDING +
                "###ServiceStore\n" +
                "ServiceStore test::ServiceStore\n" +
                "(\n" +
                "  Service TestService\n" +
                "  (\n" +
                "    path : '/testService';\n" +
                "    method : GET;\n" +
                "    response : test::model::A <- test::Binding;\n" +
                "    security : [];\n" +
                "  )\n" +
                ")\n" +
                "###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "  *test::model::A: ServiceStore\n" +
                "  {\n" +
                "    ~service [test::ServiceStore] TestService\n" +
                "  }\n" +
                ")\n");

        //With ParameterMapping & simple service path
        test(FLATDATA_BINDING +
                "###ServiceStore\n" +
                "ServiceStore test::ServiceStore\n" +
                "(\n" +
                "  Service TestService\n" +
                "  (\n" +
                "    path : '/testService';\n" +
                "    method : GET;\n" +
                "    parameters :\n" +
                "    (\n" +
                "      serializationFormat : String ( location = query )\n" +
                "    );\n" +
                "    response : test::model::A <- test::Binding;\n" +
                "    security : [];\n" +
                "  )\n" +
                ")\n" +
                "###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "  *test::model::A: ServiceStore\n" +
                "  {\n" +
                "    ~service [test::ServiceStore] TestService\n" +
                "    (\n" +
                "      ~request\n" +
                "      (\n" +
                "        parameters\n" +
                "        (\n" +
                "          serializationFormat = 'CSV'\n" +
                "        )\n" +
                "      )\n" +
                "    )\n" +
                "  }\n" +
                ")\n");

        //With ParameterMapping & special character in param name
        test(FLATDATA_BINDING +
                "###ServiceStore\n" +
                "ServiceStore test::ServiceStore\n" +
                "(\n" +
                "  Service TestService\n" +
                "  (\n" +
                "    path : '/testService';\n" +
                "    method : GET;\n" +
                "    parameters :\n" +
                "    (\n" +
                "      \"serialization.Format\" : String ( location = query )\n" +
                "    );\n" +
                "    response : test::model::A <- test::Binding;\n" +
                "    security : [];\n" +
                "  )\n" +
                ")\n" +
                "###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "  *test::model::A: ServiceStore\n" +
                "  {\n" +
                "    ~service [test::ServiceStore] TestService\n" +
                "    (\n" +
                "      ~request\n" +
                "      (\n" +
                "        parameters\n" +
                "        (\n" +
                "          \"serialization.Format\" = 'CSV'\n" +
                "        )\n" +
                "      )\n" +
                "    )\n" +
                "  }\n" +
                ")\n");

        //With single level service group
        test(FLATDATA_BINDING +
                "###ServiceStore\n" +
                "ServiceStore test::ServiceStore\n" +
                "(\n" +
                "  ServiceGroup TestServiceGroup\n" +
                "  (\n" +
                "    path : '/testServices';\n" +
                "    Service TestService\n" +
                "    (\n" +
                "      path : '/testService/{param}';\n" +
                "      method : GET;\n" +
                "      parameters :\n" +
                "      (\n" +
                "        param : String ( location = path )\n" +
                "      );\n" +
                "      response : test::model::A <- test::Binding;\n" +
                "      security : [];\n" +
                "    )\n" +
                "  )\n" +
                ")\n" +
                "###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "  *test::model::A: ServiceStore\n" +
                "  {\n" +
                "    ~service [test::ServiceStore] TestServiceGroup.TestService\n" +
                "    (\n" +
                "      ~request\n" +
                "      (\n" +
                "        parameters\n" +
                "        (\n" +
                "           param = $this.alpha\n" +
                "        )\n" +
                "      )\n" +
                "    )\n" +
                "  }\n" +
                ")\n");

        //With multi level service group
        test(FLATDATA_BINDING +
                "###ServiceStore\n" +
                "ServiceStore test::ServiceStore\n" +
                "(\n" +
                "  ServiceGroup TestServiceGroup1\n" +
                "  (\n" +
                "    path : '/testServices1';\n" +
                "    ServiceGroup TestServiceGroup2\n" +
                "    (\n" +
                "      path : '/testServices2';\n" +
                "      Service TestService\n" +
                "      (\n" +
                "        path : '/testService/{param}';\n" +
                "        method : GET;\n" +
                "        parameters :\n" +
                "        (\n" +
                "          param : String ( location = path )\n" +
                "        );\n" +
                "        response : test::model::A <- test::Binding;\n" +
                "        security : [];\n" +
                "      )\n" +
                "    )\n" +
                "  )\n" +
                ")\n" +
                "###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "  *test::model::A: ServiceStore\n" +
                "  {\n" +
                "    ~service [test::ServiceStore] TestServiceGroup1.TestServiceGroup2.TestService\n" +
                "    (\n" +
                "      ~request\n" +
                "      (\n" +
                "        parameters\n" +
                "        (\n" +
                "           param = $this.alpha\n" +
                "        )\n" +
                "      )\n" +
                "    )\n" +
                "  }\n" +
                ")\n");

        //With multiple parameterMapping
        test(FLATDATA_BINDING +
                "###ServiceStore\n" +
                "ServiceStore test::ServiceStore\n" +
                "(\n" +
                "  ServiceGroup TestServiceGroup1\n" +
                "  (\n" +
                "    path : '/testServices1';\n" +
                "    ServiceGroup TestServiceGroup2\n" +
                "    (\n" +
                "      path : '/testServices2';\n" +
                "      Service TestService\n" +
                "      (\n" +
                "        path : '/testService/{param}';\n" +
                "        method : GET;\n" +
                "        parameters :\n" +
                "        (\n" +
                "          param : String ( location = path ),\n" +
                "          param2 : Integer ( location = query )\n" +
                "        );\n" +
                "        response : test::model::A <- test::Binding;\n" +
                "        security : [];\n" +
                "      )\n" +
                "    )\n" +
                "  )\n" +
                ")\n" +
                "###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "  *test::model::A: ServiceStore\n" +
                "  {\n" +
                "    ~service [test::ServiceStore] TestServiceGroup1.TestServiceGroup2.TestService\n" +
                "    (\n" +
                "      ~request\n" +
                "      (\n" +
                "        parameters\n" +
                "        (\n" +
                "           param2 = 1,\n" +
                "           param  = $this.alpha\n" +
                "        )\n" +
                "      )\n" +
                "    )\n" +
                "  }\n" +
                ")\n");
    }

    @Test
    public void testServiceStoreMappingWithMultipleServiceMapping()
    {
        test(FLATDATA_BINDING +
                "###ServiceStore\n" +
                "ServiceStore test::ServiceStore\n" +
                "(\n" +
                "  ServiceGroup TestServiceGroup\n" +
                "  (\n" +
                "    path : '/testServices';\n" +
                "    Service TestService1\n" +
                "    (\n" +
                "      path : '/testService1';\n" +
                "      method : GET;\n" +
                "      parameters :\n" +
                "      (\n" +
                "        param : String ( location = query ),\n" +
                "        param2 : Integer ( location = query )\n" +
                "      );\n" +
                "      response : test::model::A <- test::Binding;\n" +
                "      security : [];\n" +
                "    )\n" +
                "    Service TestService2\n" +
                "    (\n" +
                "      path : '/testService2';\n" +
                "      method : GET;\n" +
                "      parameters :\n" +
                "      (\n" +
                "        param1 : Boolean ( location = query )\n" +
                "      );\n" +
                "      response : test::model::A <- test::Binding;\n" +
                "      security : [];\n" +
                "    )\n" +
                "  )\n" +
                ")\n" +
                "###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "  *test::model::A: ServiceStore\n" +
                "  {\n" +
                "    ~service [test::ServiceStore] TestServiceGroup.TestService1\n" +
                "    (\n" +
                "      ~request\n" +
                "      (\n" +
                "        parameters\n" +
                "        (\n" +
                "           param2 = 1,\n" +
                "           param = $this.alpha" +
                "        )\n" +
                "      )\n" +
                "    )\n" +
                "    ~service [test::ServiceStore] TestServiceGroup.TestService2\n" +
                "    (\n" +
                "      ~request\n" +
                "      (\n" +
                "        parameters\n" +
                "        (\n" +
                "           param1 = $this.beta->toOne()\n" +
                "        )\n" +
                "      )\n" +
                "    )\n" +
                "  }\n" +
                ")\n");
    }

    @Test
    public void testServiceStoreMappingWithLocalMappingProperties()
    {
        test(FLATDATA_BINDING +
                "###ServiceStore\n" +
                "ServiceStore test::ServiceStore\n" +
                "(\n" +
                "  ServiceGroup TestServiceGroup\n" +
                "  (\n" +
                "    path : '/testServices';\n" +
                "    Service TestService1\n" +
                "    (\n" +
                "      path : '/testService1';\n" +
                "      method : GET;\n" +
                "      parameters :\n" +
                "      (\n" +
                "        param : String ( location = query ),\n" +
                "        param2 : Integer ( location = query )\n" +
                "      );\n" +
                "      response : test::model::A <- test::Binding;\n" +
                "      security : [];\n" +
                "    )\n" +
                "    Service TestService2\n" +
                "    (\n" +
                "      path : '/testService2';\n" +
                "      method : GET;\n" +
                "      parameters :\n" +
                "      (\n" +
                "        param1 : Boolean ( location = query )\n" +
                "      );\n" +
                "      response : test::model::A <- test::Binding;\n" +
                "      security : [];\n" +
                "    )\n" +
                "  )\n" +
                ")\n" +
                "###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "  *test::model::A: ServiceStore\n" +
                "  {\n" +
                "    +localProp : String[1];\n" +
                "\n" +
                "    ~service [test::ServiceStore] TestServiceGroup.TestService1\n" +
                "    (\n" +
                "      ~request\n" +
                "      (\n" +
                "        parameters\n" +
                "        (\n" +
                "           param2 = 1,\n" +
                "           param = $this.localProp" +
                "        )\n" +
                "      )\n" +
                "    )\n" +
                "    ~service [test::ServiceStore] TestServiceGroup.TestService2\n" +
                "    (\n" +
                "      ~request\n" +
                "      (\n" +
                "        parameters\n" +
                "        (\n" +
                "           param1 = $this.beta->toOne()\n" +
                "        )\n" +
                "      )\n" +
                "    )\n" +
                "  }\n" +
                ")\n");

        test(FLATDATA_BINDING +
                "###ServiceStore\n" +
                "ServiceStore test::ServiceStore\n" +
                "(\n" +
                "  ServiceGroup TestServiceGroup\n" +
                "  (\n" +
                "    path : '/testServices';\n" +
                "    Service TestService1\n" +
                "    (\n" +
                "      path : '/testService1';\n" +
                "      method : GET;\n" +
                "      parameters :\n" +
                "      (\n" +
                "        param : String ( location = query ),\n" +
                "        param2 : Integer ( location = query )\n" +
                "      );\n" +
                "      response : test::model::A <- test::Binding;\n" +
                "      security : [];\n" +
                "    )\n" +
                "    Service TestService2\n" +
                "    (\n" +
                "      path : '/testService2';\n" +
                "      method : GET;\n" +
                "      parameters :\n" +
                "      (\n" +
                "        param1 : Boolean ( location = query )\n" +
                "      );\n" +
                "      response : test::model::A <- test::Binding;\n" +
                "      security : [];\n" +
                "    )\n" +
                "  )\n" +
                ")\n" +
                "###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "  *test::model::A: ServiceStore\n" +
                "  {\n" +
                "    +localProp1 : String[1];\n" +
                "    +localProp2 : Boolean[1];\n" +
                "\n" +
                "    ~service [test::ServiceStore] TestServiceGroup.TestService1\n" +
                "    (\n" +
                "      ~request\n" +
                "      (\n" +
                "        parameters\n" +
                "        (\n" +
                "           param2 = 1,\n" +
                "           param = $this.localProp1" +
                "        )\n" +
                "      )\n" +
                "    )\n" +
                "    ~service [test::ServiceStore] TestServiceGroup.TestService2\n" +
                "    (\n" +
                "      ~request\n" +
                "      (\n" +
                "        parameters\n" +
                "        (\n" +
                "           param1 = $this.localProp2\n" +
                "        )\n" +
                "      )\n" +
                "    )\n" +
                "  }\n" +
                ")\n");
    }

    @Test
    public void testMultipleServiceStoreMappings()
    {
        test(FLATDATA_BINDING +
                "###ServiceStore\n" +
                "ServiceStore test::ServiceStore\n" +
                "(\n" +
                "  ServiceGroup TestServiceGroup1\n" +
                "  (\n" +
                "    path : '/testServices1';\n" +
                "    ServiceGroup TestServiceGroup2\n" +
                "    (\n" +
                "      path : '/testServices2';\n" +
                "      Service TestService1\n" +
                "      (\n" +
                "        path : '/testService1/{param}';\n" +
                "        method : GET;\n" +
                "        parameters :\n" +
                "        (\n" +
                "          param : String ( location = path ),\n" +
                "          param1 : Integer ( location = query )\n" +
                "        );\n" +
                "        response : test::model::A <- test::Binding;\n" +
                "        security : [];\n" +
                "      )\n" +
                "      Service TestService2\n" +
                "      (\n" +
                "        path : '/testService2/{param}';\n" +
                "        method : GET;\n" +
                "        parameters :\n" +
                "        (\n" +
                "          param : Boolean ( location = path ),\n" +
                "          param1 : Float ( location = query )\n" +
                "        );\n" +
                "        response : test::model::B <- test::Binding2;\n" +
                "        security : [];\n" +
                "      )\n" +
                "    )\n" +
                "  )\n" +
                ")\n" +
                "###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "  *test::model::A: ServiceStore\n" +
                "  {\n" +
                "    ~service [test::ServiceStore] TestServiceGroup1.TestServiceGroup2.TestService1\n" +
                "    (\n" +
                "      ~request\n" +
                "      (\n" +
                "        parameters\n" +
                "        (\n" +
                "           param1 = 1,\n" +
                "           param = $this.alpha" +
                "        )\n" +
                "      )\n" +
                "    )\n" +
                "  }\n" +
                "  *test::model::B: ServiceStore\n" +
                "  {\n" +
                "    ~service [test::ServiceStore] TestServiceGroup1.TestServiceGroup2.TestService2\n" +
                "    (\n" +
                "      ~request\n" +
                "      (\n" +
                "        parameters\n" +
                "        (\n" +
                "           param1 = 1.1,\n" +
                "           param = $this.beta->toOne()" +
                "        )\n" +
                "      )\n" +
                "    )\n" +
                "  }\n" +
                ")\n");
    }

    @Test
    public void testServiceStoreRequestBodyMapping()
    {
        // Constant Source for Request Body Mapping
        test(FLATDATA_BINDING +
                "###ServiceStore\n" +
                "ServiceStore test::ServiceStore\n" +
                "(\n" +
                "  ServiceGroup TestServiceGroup\n" +
                "  (\n" +
                "    path : '/testServices';\n" +
                "    Service TestService\n" +
                "    (\n" +
                "      path : '/testService';\n" +
                "      method : POST;\n" +
                "      requestBody : test::model::B <- test::Binding2 ;" +
                "      response : [ test::model::A <- test::Binding ];\n" +
                "      security : [];\n" +
                "    )\n" +
                "  )\n" +
                ")\n" +
                "###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "  *test::model::A: ServiceStore\n" +
                "  {\n" +
                "    ~service [test::ServiceStore] TestServiceGroup.TestService\n" +
                "    (\n" +
                "      ~request\n" +
                "      (\n" +
                "        body = ^test::model::B(alpha='xyz' , gamma=1)\n" +
                "      )\n" +
                "    )\n" +
                "  }\n" +
                ")\n");

        // Constant & Property Source for Request Body Mapping
        test(FLATDATA_BINDING +
                "###ServiceStore\n" +
                "ServiceStore test::ServiceStore\n" +
                "(\n" +
                "  ServiceGroup TestServiceGroup\n" +
                "  (\n" +
                "    path : '/testServices';\n" +
                "    Service TestService\n" +
                "    (\n" +
                "      path : '/testService';\n" +
                "      method : POST;\n" +
                "      requestBody : test::model::B <- test::Binding2 ;" +
                "      response : [ test::model::A <- test::Binding ];\n" +
                "      security : [];\n" +
                "    )\n" +
                "  )\n" +
                ")\n" +
                "###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "  *test::model::A: ServiceStore\n" +
                "  {\n" +
                "    ~service [test::ServiceStore] TestServiceGroup.TestService\n" +
                "    (\n" +
                "      ~request\n" +
                "      (\n" +
                "        body = ^test::model::B(alpha=$this.alpha , gamma=1)\n" +
                "      )\n" +
                "    )\n" +
                "  }\n" +
                ")\n");

        // Nested Model for Request Body Mapping
        test("###Pure\n" +
                "Class test::model::A\n" +
                "{\n" +
                "  prop1 : String[1];\n" +
                "  prop2 : String[1];\n" +
                "  prop3 : String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::model::B\n" +
                "{\n" +
                "  prop1 : String[1];\n" +
                "  prop2 : test::model::C[1];" +
                "}\n" +
                "Class test::model::C\n" +
                "{\n" +
                "  prop1 : String[1];\n" +
                "}\n" +
                "\n" +
                "###ExternalFormat\n" +
                "Binding test::TestBinding\n" +
                "{\n" +
                "  contentType: 'application/json';\n" +
                "  modelIncludes: [\n" +
                "    test::model::A,\n" +
                "    test::model::B,\n" +
                "    test::model::C\n" +
                "  ];\n" +
                "}\n" +
                "\n" +
                "###ServiceStore\n" +
                "ServiceStore test::ServiceStore\n" +
                "(\n" +
                "  ServiceGroup TestServiceGroup\n" +
                "  (\n" +
                "    path : '/testServices';\n" +
                "    Service TestService\n" +
                "    (\n" +
                "      path : '/testService';\n" +
                "      method : POST;\n" +
                "      requestBody : test::model::B <- test::TestBinding ;" +
                "      response : [ test::model::A <- test::TestBinding ];\n" +
                "      security : [];\n" +
                "    )\n" +
                "  )\n" +
                ")\n" +
                "###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "  *test::model::A: ServiceStore\n" +
                "  {\n" +
                "    ~service [test::ServiceStore] TestServiceGroup.TestService\n" +
                "    (\n" +
                "      ~request\n" +
                "      (\n" +
                "        body = ^test::model::B(prop1=$this.prop1 , prop2=^test::model::C(prop1=$this.prop2 + $this.prop3))\n" +
                "      )\n" +
                "    )\n" +
                "  }\n" +
                ")\n");
    }

    @Test
    public void testServiceStoreMappingCompilationErrorMessages()
    {
        //Invalid parameter in parameter mapping
        test(FLATDATA_BINDING +
                "###ServiceStore\n" +
                "ServiceStore test::ServiceStore\n" +
                "(\n" +
                "  Service TestService\n" +
                "  (\n" +
                "    path : '/testService';\n" +
                "    method : GET;\n" +
                "    parameters :\n" +
                "    (\n" +
                "      serializationFormat : String ( location = query )\n" +
                "    );\n" +
                "    response : test::model::A <- test::Binding;\n" +
                "    security : [];\n" +
                "  )\n" +
                ")\n" +
                "###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "  *test::model::A: ServiceStore\n" +
                "  {\n" +
                "    ~service [test::ServiceStore] TestService\n" +
                "    (\n" +
                "      ~request\n" +
                "      (\n" +
                "        parameters\n" +
                "        (\n" +
                "           invalid = 'PURE'\n" +
                "        )\n" +
                "      )\n" +
                "    )\n" +
                "  }\n" +
                ")\n", "COMPILATION error at [74:12-27]: Service Parameter : 'invalid' is not valid");

        //Multiple Parameter Mappings
        test(FLATDATA_BINDING +
                "###ServiceStore\n" +
                "ServiceStore test::ServiceStore\n" +
                "(\n" +
                "  Service TestService\n" +
                "  (\n" +
                "    path : '/testService';\n" +
                "    method : GET;\n" +
                "    parameters :\n" +
                "    (\n" +
                "      serializationFormat : String ( location = query )\n" +
                "    );\n" +
                "    response : test::model::A <- test::Binding;\n" +
                "    security : [];\n" +
                "  )\n" +
                ")\n" +
                "###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "  *test::model::A: ServiceStore\n" +
                "  {\n" +
                "    ~service [test::ServiceStore] TestService\n" +
                "    (\n" +
                "      ~request\n" +
                "      (\n" +
                "        parameters\n" +
                "        (\n" +
                "           serializationFormat = 'CSV',\n" +
                "           serializationFormat = 'PURE'\n" +
                "        )\n" +
                "      )\n" +
                "    )\n" +
                "  }\n" +
                ")\n", "COMPILATION error at [68:5-78:5]: Multiple Mappings for same parameter not allowed. Multiple mappings found for parameters : [serializationFormat].");

        //Multiple Parameter Mappings
        test(FLATDATA_BINDING +
                "###ServiceStore\n" +
                "ServiceStore test::ServiceStore\n" +
                "(\n" +
                "  Service TestService\n" +
                "  (\n" +
                "    path : '/testService';\n" +
                "    method : GET;\n" +
                "    parameters :\n" +
                "    (\n" +
                "      serializationFormat : String ( location = query )\n" +
                "    );\n" +
                "    response : test::model::A <- test::Binding;\n" +
                "    security : [];\n" +
                "  )\n" +
                ")\n" +
                "###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "  *test::model::A: ServiceStore\n" +
                "  {\n" +
                "    ~service [test::ServiceStore] TestService\n" +
                "    (\n" +
                "      ~request\n" +
                "      (\n" +
                "        parameters\n" +
                "        (\n" +
                "           serializationFormat = $this.alpha,\n" +
                "           serializationFormat = 'PURE'\n" +
                "        )\n" +
                "      )\n" +
                "    )\n" +
                "  }\n" +
                ")\n", "COMPILATION error at [68:5-78:5]: Multiple Mappings for same parameter not allowed. Multiple mappings found for parameters : [serializationFormat].");

        // Missing mapping for required parameter
        test("###Pure\n" +
                "Class test::model::A\n" +
                "{\n" +
                "  alpha   : String[1];\n" +
                "  beta    : Boolean[0..1];\n" +
                "  gamma   : Integer[1];\n" +
                "  delta   : Float[1];\n" +
                "  epsilon : Decimal[1];\n" +
                "  zeta    : Float[1];\n" +
                "  eta     : Decimal[1];\n" +
                "  theta   : StrictDate[1];\n" +
                "  iota    : DateTime[1];\n" +
                "}\n" +
                "###ExternalFormat\n" +
                "SchemaSet test::SchemaSet\n" +
                "{\n" +
                "  format: FlatData;\n" +
                "  schemas: [ { content: 'section A: DelimitedWithHeadings\\n{\\n  scope.untilEof;\\n  delimiter: \\',\\';\\n\\n  Record\\n  {\\n    alpha   : STRING;\\n    beta    : BOOLEAN(optional);\\n    gamma   : INTEGER;\\n    delta   : INTEGER;\\n    epsilon : INTEGER;\\n    zeta    : DECIMAL;\\n    eta     : DECIMAL;\\n    theta   : DATE;\\n    iota    : DATETIME;\\n  }\\n}'; } ];\n" +
                "}\n" +
                "\n" +
                "Binding test::Binding\n" +
                "{\n" +
                "  schemaSet: test::SchemaSet;\n" +
                "  contentType: 'application/x.flatdata';\n" +
                "  modelIncludes: [ test::model::A ];\n" +
                "}\n" +
                "\n" +
                "###Pure\n" +
                "Class test::model::B\n" +
                "{\n" +
                "  alpha   : String[1];\n" +
                "  beta    : Boolean[0..1];\n" +
                "  gamma   : Integer[1];\n" +
                "}\n" +
                "###ExternalFormat\n" +
                "SchemaSet test::SchemaSet2\n" +
                "{\n" +
                "  format: FlatData;\n" +
                "  schemas: [ { content: 'section A: DelimitedWithHeadings\\n{\\n  scope.untilEof;\\n  delimiter: \\',\\';\\n\\n  Record\\n  {\\n    alpha   : STRING;\\n    beta    : BOOLEAN(optional);\\n    gamma   : INTEGER;\\n  }\\n}'; } ];\n" +
                "}\n" +
                "\n" +
                "Binding test::Binding2\n" +
                "{\n" +
                "  schemaSet: test::SchemaSet2;\n" +
                "  contentType: 'application/x.flatdata';\n" +
                "  modelIncludes: [ test::model::B ];\n" +
                "}\n" +
                "###ServiceStore\n" +
                "ServiceStore test::ServiceStore\n" +
                "(\n" +
                "  Service TestService\n" +
                "  (\n" +
                "    path : '/testService';\n" +
                "    method : GET;\n" +
                "    parameters :\n" +
                "    (\n" +
                "      serializationFormat : String ( location = query, required = true )\n" +
                "    );\n" +
                "    response : test::model::A <- test::Binding;\n" +
                "    security : [];\n" +
                "  )\n" +
                ")\n" +
                "###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "  *test::model::A: ServiceStore\n" +
                "  {\n" +
                "    ~service [test::ServiceStore] TestService\n" +
                "    (\n" +
                "    )\n" +
                "  }\n" +
                ")\n", "COMPILATION error at [68:5-70:5]: All required service parameters should be mapped. Required Service Parameters : [serializationFormat]. Mapped Parameters : [].");

        // Missing mapping for path parameter
        test("###Pure\n" +
                "Class test::model::A\n" +
                "{\n" +
                "  alpha   : String[1];\n" +
                "  beta    : Boolean[0..1];\n" +
                "  gamma   : Integer[1];\n" +
                "  delta   : Float[1];\n" +
                "  epsilon : Decimal[1];\n" +
                "  zeta    : Float[1];\n" +
                "  eta     : Decimal[1];\n" +
                "  theta   : StrictDate[1];\n" +
                "  iota    : DateTime[1];\n" +
                "}\n" +
                "###ExternalFormat\n" +
                "SchemaSet test::SchemaSet\n" +
                "{\n" +
                "  format: FlatData;\n" +
                "  schemas: [ { content: 'section A: DelimitedWithHeadings\\n{\\n  scope.untilEof;\\n  delimiter: \\',\\';\\n\\n  Record\\n  {\\n    alpha   : STRING;\\n    beta    : BOOLEAN(optional);\\n    gamma   : INTEGER;\\n    delta   : INTEGER;\\n    epsilon : INTEGER;\\n    zeta    : DECIMAL;\\n    eta     : DECIMAL;\\n    theta   : DATE;\\n    iota    : DATETIME;\\n  }\\n}'; } ];\n" +
                "}\n" +
                "\n" +
                "Binding test::Binding\n" +
                "{\n" +
                "  schemaSet: test::SchemaSet;\n" +
                "  contentType: 'application/x.flatdata';\n" +
                "  modelIncludes: [ test::model::A ];\n" +
                "}\n" +
                "\n" +
                "###Pure\n" +
                "Class test::model::B\n" +
                "{\n" +
                "  alpha   : String[1];\n" +
                "  beta    : Boolean[0..1];\n" +
                "  gamma   : Integer[1];\n" +
                "}\n" +
                "###ExternalFormat\n" +
                "SchemaSet test::SchemaSet2\n" +
                "{\n" +
                "  format: FlatData;\n" +
                "  schemas: [ { content: 'section A: DelimitedWithHeadings\\n{\\n  scope.untilEof;\\n  delimiter: \\',\\';\\n\\n  Record\\n  {\\n    alpha   : STRING;\\n    beta    : BOOLEAN(optional);\\n    gamma   : INTEGER;\\n  }\\n}'; } ];\n" +
                "}\n" +
                "\n" +
                "Binding test::Binding2\n" +
                "{\n" +
                "  schemaSet: test::SchemaSet2;\n" +
                "  contentType: 'application/x.flatdata';\n" +
                "  modelIncludes: [ test::model::B ];\n" +
                "}\n" +
                "###ServiceStore\n" +
                "ServiceStore test::ServiceStore\n" +
                "(\n" +
                "  Service TestService\n" +
                "  (\n" +
                "    path : '/testService/{serializationFormat}';\n" +
                "    method : GET;\n" +
                "    parameters :\n" +
                "    (\n" +
                "      serializationFormat : String ( location = path )\n" +
                "    );\n" +
                "    response : test::model::A <- test::Binding;\n" +
                "    security : [];\n" +
                "  )\n" +
                ")\n" +
                "###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "  *test::model::A: ServiceStore\n" +
                "  {\n" +
                "    ~service [test::ServiceStore] TestService\n" +
                "    (\n" +
                "    )\n" +
                "  }\n" +
                ")\n", "COMPILATION error at [68:5-70:5]: All required service parameters should be mapped. Required Service Parameters : [serializationFormat]. Mapped Parameters : [].");

        //        typo in property name in request body mapping (TODO: fix this)
        //        test(FLATDATA_BINDING +
        //                "###ServiceStore\n" +
        //                "ServiceStore test::ServiceStore\n" +
        //                "(\n" +
        //                "  ServiceGroup TestServiceGroup\n" +
        //                "  (\n" +
        //                "    path : '/testServices';\n" +
        //                "    Service TestService\n" +
        //                "    (\n" +
        //                "      path : '/testService';\n" +
        //                "      method : POST;\n" +
        //                "      requestBody : test::model::B <- test::Binding2 ;" +
        //                "      response : [ test::model::A <- test::Binding ];\n" +
        //                "      security : [];\n" +
        //                "    )\n" +
        //                "  )\n" +
        //                ")\n" +
        //                "###Mapping\n" +
        //                "Mapping test::mapping\n" +
        //                "(\n" +
        //                "  *test::model::A: ServiceStore\n" +
        //                "  {\n" +
        //                "    ~service [test::ServiceStore] TestServiceGroup.TestService\n" +
        //                "    (\n" +
        //                "      ~request\n" +
        //                "      (\n" +
        //                "        body = ^test::model::B(alha='xyz' , gamma=1)\n" +
        //                "      )\n" +
        //                "    )\n" +
        //                "  }\n" +
        //                ")\n", "COMPILATION error at [72:9-20]: Property 'alha' not found in class 'test::model::B'");

        //        missing request body property mapping for mandatory property (TODO:fix this)
        //        test(FLATDATA_BINDING +
        //                "###ServiceStore\n" +
        //                "ServiceStore test::ServiceStore\n" +
        //                "(\n" +
        //                "  ServiceGroup TestServiceGroup\n" +
        //                "  (\n" +
        //                "    path : '/testServices';\n" +
        //                "    Service TestService\n" +
        //                "    (\n" +
        //                "      path : '/testService';\n" +
        //                "      method : POST;\n" +
        //                "      requestBody : test::model::B <- test::Binding2 ;" +
        //                "      response : [ test::model::A <- test::Binding ];\n" +
        //                "      security : [];\n" +
        //                "    )\n" +
        //                "  )\n" +
        //                ")\n" +
        //                "###Mapping\n" +
        //                "Mapping test::mapping\n" +
        //                "(\n" +
        //                "  *test::model::A: ServiceStore\n" +
        //                "  {\n" +
        //                "    ~service [test::ServiceStore] TestServiceGroup.TestService\n" +
        //                "    (\n" +
        //                "      ~request\n" +
        //                "      (\n" +
        //                "        body = ^test::model::B(alpha='xyz' , gamma=1)\n" +
        //                "      )\n" +
        //                "    )\n" +
        //                "  }\n" +
        //                ")\n", "COMPILATION error at [72:9-20]: Property 'alha' not found in class 'test::model::B'");
    }

    @Test
    public void testServiceStoreMappingNotSupportedMessages()
    {
        //Multiple Parameter Mappings
        test(FLATDATA_BINDING +
                "###Pure\n" +
                "Enum test::SerializationFormat\n" +
                "{\n" +
                "  CSV, PURE\n" +
                "}\n" +
                "\n" +
                "###ServiceStore\n" +
                "ServiceStore test::ServiceStore\n" +
                "(\n" +
                "  Service TestService\n" +
                "  (\n" +
                "    path : '/testService';\n" +
                "    method : GET;\n" +
                "    parameters :\n" +
                "    (\n" +
                "      serializationFormat : String ( location = query, enum = test::SerializationFormat )\n" +
                "    );\n" +
                "    response : test::model::A <- test::Binding;\n" +
                "    security : [];\n" +
                "  )\n" +
                ")\n" +
                "###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "  *test::model::A: ServiceStore\n" +
                "  {\n" +
                "    ~service [test::ServiceStore] TestService\n" +
                "    (\n" +
                "      ~request\n" +
                "      (\n" +
                "        parameters\n" +
                "        (\n" +
                "           serializationFormat = 'CSV'\n" +
                "        )\n" +
                "      )\n" +
                "    )\n" +
                "  }\n" +
                ")\n", "COMPILATION error at [80:12-38]: Mapping enum service parameter is not yet supported !!");
    }

    @Test
    public void testServiceStoreMappingPathOffset()
    {
        test(JSON_BINDING +
                "###Mapping\n" +
                "Mapping meta::external::store::service::showcase::mapping::ServiceStoreMapping\n" +
                "(\n" +
                "    *meta::external::store::service::showcase::domain::Person[person_set]: ServiceStore\n" +
                "    {\n" +
                "        ~service [meta::external::store::service::showcase::store::EmployeesServiceStore] EmployeesService\n" +
                "        (\n" +
                "            ~path $service.response.employees\n" +
                "        )\n" +
                "    }\n" +
                "\n" +
                "    *meta::external::store::service::showcase::domain::Firm[firm_set]: ServiceStore\n" +
                "    {\n" +
                "        ~service [meta::external::store::service::showcase::store::EmployeesServiceStore] EmployeesService\n" +
                "        (\n" +
                "            ~path $service.response.firms\n" +
                "        )\n" +
                "    }\n" +
                "\n" +
                "    meta::external::store::service::showcase::domain::Person[person_set2]: ServiceStore\n" +
                "    {\n" +
                "        +firmId : Integer[1];\n" +
                "\n" +
                "        ~service [meta::external::store::service::showcase::store::EmployeesServiceStore] EmployeesServiceByFirmId\n" +
                "        (\n" +
                "            ~path $service.response.employees\n" +
                "\n" +
                "            ~request\n" +
                "            (\n" +
                "               parameters\n" +
                "               (\n" +
                "                 firmId = $this.firmId\n" +
                "               )\n" +
                "            )\n" +
                "        )\n" +
                "    }\n" +
                "\n" +
                "    *meta::external::store::service::showcase::domain::Employment: XStore\n" +
                "    {\n" +
                "        employees[firm_set, person_set2] : $this.firmId == $that.firmId\n" +
                "    }\n" +
                ")\n\n");

        test(JSON_BINDING +
                "###Mapping\n" +
                "Mapping meta::external::store::service::showcase::mapping::ServiceStoreMapping\n" +
                "(\n" +
                "    *meta::external::store::service::showcase::domain::Person[person_set]: ServiceStore\n" +
                "    {\n" +
                "        ~service [meta::external::store::service::showcase::store::EmployeesServiceStore] EmployeesService\n" +
                "    }\n" +
                ")\n\n", "COMPILATION error at [85:9-106]: Response type of source service should match mapping class. Found response type : meta::external::store::service::showcase::domain::ApiResponse does not match mapping class : meta::external::store::service::showcase::domain::Person");

        test(JSON_BINDING +
                "###Mapping\n" +
                "Mapping meta::external::store::service::showcase::mapping::ServiceStoreMapping\n" +
                "(\n" +
                "    *meta::external::store::service::showcase::domain::Person[person_set]: ServiceStore\n" +
                "    {\n" +
                "        ~service [meta::external::store::service::showcase::store::EmployeesServiceStore] EmployeesService\n" +
                "        (\n" +
                "            ~path $service.response.firms\n" +
                "        )\n" +
                "    }\n" +
                ")\n\n", "COMPILATION error at [85:9-88:9]: Response type of source service should match mapping class. Found response type : meta::external::store::service::showcase::domain::Firm does not match mapping class : meta::external::store::service::showcase::domain::Person");
    }

    @Test
    public void testServiceStoreMappingWithOptionalParameters()
    {
        test("###Pure\n" +
                "Class test::model::A\n" +
                "{\n" +
                "  alpha   : String[1];\n" +
                "  beta    : Boolean[0..1];\n" +
                "  gamma   : Integer[1];\n" +
                "  delta   : Float[1];\n" +
                "  epsilon : Decimal[1];\n" +
                "  zeta    : Float[1];\n" +
                "  eta     : Decimal[1];\n" +
                "  theta   : StrictDate[1];\n" +
                "  iota    : DateTime[1];\n" +
                "}\n" +
                "###ExternalFormat\n" +
                "SchemaSet test::SchemaSet\n" +
                "{\n" +
                "  format: FlatData;\n" +
                "  schemas: [ { content: 'section A: DelimitedWithHeadings\\n{\\n  scope.untilEof;\\n  delimiter: \\',\\';\\n\\n  Record\\n  {\\n    alpha   : STRING;\\n    beta    : BOOLEAN(optional);\\n    gamma   : INTEGER;\\n    delta   : INTEGER;\\n    epsilon : INTEGER;\\n    zeta    : DECIMAL;\\n    eta     : DECIMAL;\\n    theta   : DATE;\\n    iota    : DATETIME;\\n  }\\n}'; } ];\n" +
                "}\n" +
                "\n" +
                "Binding test::Binding\n" +
                "{\n" +
                "  schemaSet: test::SchemaSet;\n" +
                "  contentType: 'application/x.flatdata';\n" +
                "  modelIncludes: [ test::model::A ];\n" +
                "}\n" +
                "\n" +
                "###Pure\n" +
                "Class test::model::B\n" +
                "{\n" +
                "  alpha   : String[1];\n" +
                "  beta    : Boolean[0..1];\n" +
                "  gamma   : Integer[1];\n" +
                "}\n" +
                "###ExternalFormat\n" +
                "SchemaSet test::SchemaSet2\n" +
                "{\n" +
                "  format: FlatData;\n" +
                "  schemas: [ { content: 'section A: DelimitedWithHeadings\\n{\\n  scope.untilEof;\\n  delimiter: \\',\\';\\n\\n  Record\\n  {\\n    alpha   : STRING;\\n    beta    : BOOLEAN(optional);\\n    gamma   : INTEGER;\\n  }\\n}'; } ];\n" +
                "}\n" +
                "\n" +
                "Binding test::Binding2\n" +
                "{\n" +
                "  schemaSet: test::SchemaSet2;\n" +
                "  contentType: 'application/x.flatdata';\n" +
                "  modelIncludes: [ test::model::B ];\n" +
                "}\n" +
                "###ServiceStore\n" +
                "ServiceStore test::ServiceStore\n" +
                "(\n" +
                "  Service TestService\n" +
                "  (\n" +
                "    path : '/testService';\n" +
                "    method : GET;\n" +
                "    parameters :\n" +
                "    (\n" +
                "      serializationFormat : String ( location = query )\n" +
                "    );\n" +
                "    response : test::model::A <- test::Binding;\n" +
                "    security : [];\n" +
                "  )\n" +
                ")\n" +
                "###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "  *test::model::A: ServiceStore\n" +
                "  {\n" +
                "    ~service [test::ServiceStore] TestService\n" +
                "    (\n" +
                "    )\n" +
                "  }\n" +
                ")\n");
    }

    @Test
    public void testMultiLevelNesting()
    {
        Pair<PureModelContextData, PureModel> result =
                test(JSON_BINDING +
                        "###Mapping\n" +
                        "Mapping meta::external::store::service::showcase::mapping::ServiceStoreMapping\n" +
                        "(\n" +
                        "    *meta::external::store::service::showcase::domain::ApiResponse[response_set]: ServiceStore\n" +
                        "    {\n" +
                        "        ~service [meta::external::store::service::showcase::store::EmployeesServiceStore] EmployeesService\n" +
                        "    }\n" +
                        ")\n\n");

        Assert.assertEquals(5, result.getTwo().getMapping("meta::external::store::service::showcase::mapping::ServiceStoreMapping")._classMappings().size());
    }


    @Test
    public void testModelWithMultipleReferencesToSameModelServiceStore()
    {
        String grammar = "###Pure\n" +
                "import meta::external::store::service::showcase::domain::*;\n" +
                "\n" +
                "Class meta::external::store::service::showcase::domain::Firm\n" +
                "{\n" +
                "    firmName   : String[1];\n" +
                "    firmId     : Integer[1];\n" +
                "    employees1 : Person[*];\n" +
                "    employees2 : Person[*];\n" +
                "}\n" +
                "Class meta::external::store::service::showcase::domain::Person\n" +
                "{\n" +
                "    name     : String[1];\n" +
                "}\n" +
                "\n" +
                "###ServiceStore\n" +
                "ServiceStore meta::external::store::service::showcase::store::FirmServiceStore\n" +
                "(\n" +
                "   Service FirmService\n" +
                "   (\n" +
                "      path     : '/firms';\n" +
                "      method   : GET;\n" +
                "      security : [];\n" +
                "      response : [meta::external::store::service::showcase::domain::Firm <- meta::external::store::service::showcase::store::FirmResponseSchemaBinding];\n" +
                "   )\n" +
                ")\n" +
                "\n" +
                "###ExternalFormat\n" +
                "Binding meta::external::store::service::showcase::store::FirmResponseSchemaBinding\n" +
                "{\n" +
                "  contentType   : 'application/json';\n" +
                "  modelIncludes : [\n" +
                "                    meta::external::store::service::showcase::domain::Firm,\n" +
                "                    meta::external::store::service::showcase::domain::Person\n" +
                "                  ];\n" +
                "}\n\n" +
                "###Mapping\n" +
                "Mapping meta::external::store::service::showcase::mapping::ServiceStoreMapping\n" +
                "(\n" +
                "    *meta::external::store::service::showcase::domain::Firm: ServiceStore\n" +
                "    {\n" +
                "        ~service [meta::external::store::service::showcase::store::FirmServiceStore] FirmService\n" +
                "    }\n" +
                ")\n\n";

        test(grammar);

        String grammar1 = "###Pure\n" +
                "import meta::external::store::service::showcase::domain::*;\n" +
                "\n" +
                "Class meta::external::store::service::showcase::domain::Firm\n" +
                "{\n" +
                "    firmName   : String[1];\n" +
                "    firmId     : Integer[1];\n" +
                "    employees1 : Person[*];\n" +
                "    employees2 : Person[*];\n" +
                "}\n" +
                "Class meta::external::store::service::showcase::domain::Person\n" +
                "{\n" +
                "    name             : String[1];\n" +
                "    primaryAddress   : Address[1];\n" +
                "    secondaryAddress : Address[1];\n" +
                "}\n" +
                "Class meta::external::store::service::showcase::domain::Address\n" +
                "{\n" +
                "    street : String[1];\n" +
                "}\n" +
                "\n" +
                "###ServiceStore\n" +
                "ServiceStore meta::external::store::service::showcase::store::FirmServiceStore\n" +
                "(\n" +
                "   Service FirmService\n" +
                "   (\n" +
                "      path     : '/firms';\n" +
                "      method   : GET;\n" +
                "      security : [];\n" +
                "      response : [meta::external::store::service::showcase::domain::Firm <- meta::external::store::service::showcase::store::FirmResponseSchemaBinding];\n" +
                "   )\n" +
                ")\n" +
                "\n" +
                "###ExternalFormat\n" +
                "Binding meta::external::store::service::showcase::store::FirmResponseSchemaBinding\n" +
                "{\n" +
                "  contentType   : 'application/json';\n" +
                "  modelIncludes : [\n" +
                "                    meta::external::store::service::showcase::domain::Firm,\n" +
                "                    meta::external::store::service::showcase::domain::Person,\n" +
                "                    meta::external::store::service::showcase::domain::Address" +
                "                  ];\n" +
                "}\n\n" +
                "###Mapping\n" +
                "Mapping meta::external::store::service::showcase::mapping::ServiceStoreMapping\n" +
                "(\n" +
                "    *meta::external::store::service::showcase::domain::Firm: ServiceStore\n" +
                "    {\n" +
                "        ~service [meta::external::store::service::showcase::store::FirmServiceStore] FirmService\n" +
                "    }\n" +
                ")\n\n";

        test(grammar1);
    }

    @Test
    public void testRecursiveModelWithServiceStore()
    {
        // TODO: this test should pass once we have addressed https://github.com/finos/legend-engine/issues/953
        String grammar = "###Pure\n" +
                "import meta::external::store::service::showcase::domain::*;\n" +
                "\n" +
                "Class meta::external::store::service::showcase::domain::Firm\n" +
                "{\n" +
                "    firmName     : String[1];\n" +
                "    firmId       : Integer[1];\n" +
                "    subsidiaries : Firm[*];\n" +
                "}\n" +
                "\n" +
                "###ServiceStore\n" +
                "ServiceStore meta::external::store::service::showcase::store::FirmServiceStore\n" +
                "(\n" +
                "   Service FirmService\n" +
                "   (\n" +
                "      path     : '/firms';\n" +
                "      method   : GET;\n" +
                "      security : [];\n" +
                "      response : [meta::external::store::service::showcase::domain::Firm <- meta::external::store::service::showcase::store::FirmResponseSchemaBinding];\n" +
                "   )\n" +
                ")\n" +
                "\n" +
                "###ExternalFormat\n" +
                "Binding meta::external::store::service::showcase::store::FirmResponseSchemaBinding\n" +
                "{\n" +
                "  contentType   : 'application/json';\n" +
                "  modelIncludes : [\n" +
                "                    meta::external::store::service::showcase::domain::Firm\n" +
                "                  ];\n" +
                "}\n\n" +
                "###Mapping\n" +
                "Mapping meta::external::store::service::showcase::mapping::ServiceStoreMapping\n" +
                "(\n" +
                "    *meta::external::store::service::showcase::domain::Firm: ServiceStore\n" +
                "    {\n" +
                "        ~service [meta::external::store::service::showcase::store::FirmServiceStore] FirmService\n" +
                "    }\n" +
                ")\n\n";

        test(grammar, "COMPILATION error at [35:5-38:5]: Non serializable model mapped with Service Store Mapping");

        String grammar1 = "###Pure\n" +
                "import meta::external::store::service::showcase::domain::*;\n" +
                "\n" +
                "Class meta::external::store::service::showcase::domain::Firm\n" +
                "{\n" +
                "    firmName   : String[1];\n" +
                "    firmId     : Integer[1];\n" +
                "    employees1 : Person[*];\n" +
                "    employees2 : Person[*];\n" +
                "}\n" +
                "Class meta::external::store::service::showcase::domain::Person\n" +
                "{\n" +
                "    name             : String[1];\n" +
                "    primaryAddress   : Address[1];\n" +
                "    secondaryAddress : Address[1];\n" +
                "}\n" +
                "Class meta::external::store::service::showcase::domain::Address\n" +
                "{\n" +
                "    street         : String[1];\n" +
                "    associatedFirm : meta::external::store::service::showcase::domain::Firm[1];" +
                "}\n" +
                "\n" +
                "###ServiceStore\n" +
                "ServiceStore meta::external::store::service::showcase::store::FirmServiceStore\n" +
                "(\n" +
                "   Service FirmService\n" +
                "   (\n" +
                "      path     : '/firms';\n" +
                "      method   : GET;\n" +
                "      security : [];\n" +
                "      response : [meta::external::store::service::showcase::domain::Firm <- meta::external::store::service::showcase::store::FirmResponseSchemaBinding];\n" +
                "   )\n" +
                ")\n" +
                "\n" +
                "###ExternalFormat\n" +
                "Binding meta::external::store::service::showcase::store::FirmResponseSchemaBinding\n" +
                "{\n" +
                "  contentType   : 'application/json';\n" +
                "  modelIncludes : [\n" +
                "                    meta::external::store::service::showcase::domain::Firm,\n" +
                "                    meta::external::store::service::showcase::domain::Person,\n" +
                "                    meta::external::store::service::showcase::domain::Address" +
                "                  ];\n" +
                "}\n\n" +
                "###Mapping\n" +
                "Mapping meta::external::store::service::showcase::mapping::ServiceStoreMapping\n" +
                "(\n" +
                "    *meta::external::store::service::showcase::domain::Firm: ServiceStore\n" +
                "    {\n" +
                "        ~service [meta::external::store::service::showcase::store::FirmServiceStore] FirmService\n" +
                "    }\n" +
                ")\n\n";

        test(grammar1, "COMPILATION error at [47:5-50:5]: Non serializable model mapped with Service Store Mapping");
    }
}
