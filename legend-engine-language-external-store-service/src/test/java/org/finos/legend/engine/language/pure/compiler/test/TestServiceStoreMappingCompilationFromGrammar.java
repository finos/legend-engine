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
                "      ~paramMapping\n" +
                "      (\n" +
                "        serializationFormat : 'CSV'\n" +
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
                "      ~paramMapping\n" +
                "      (\n" +
                "        \"serialization.Format\" : 'CSV'\n" +
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
                "       alpha : $service.parameters.param\n" +
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
                "      ~paramMapping\n" +
                "      (\n" +
                "        param : $this.alpha\n" +
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
                "       alpha : $service.parameters.param\n" +
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
                "      ~paramMapping\n" +
                "      (\n" +
                "        param2 : 1\n" +
                "      )\n" +
                "      alpha : $service.parameters.param" +
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
                "      ~paramMapping\n" +
                "      (\n" +
                "        param2 : 1\n" +
                "      )\n" +
                "      alpha : $service.parameters.param" +
                "    )\n" +
                "    ~service [test::ServiceStore] TestServiceGroup.TestService2\n" +
                "    (\n" +
                "      ~paramMapping\n" +
                "      (\n" +
                "        param1 : $this.beta->toOne()\n" +
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
                "      ~paramMapping\n" +
                "      (\n" +
                "        param : $this.localProp,\n" +
                "        param2 : 1\n" +
                "      )\n" +
                "    )\n" +
                "    ~service [test::ServiceStore] TestServiceGroup.TestService2\n" +
                "    (\n" +
                "      ~paramMapping\n" +
                "      (\n" +
                "        param1 : $this.beta->toOne()\n" +
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
                "      ~paramMapping\n" +
                "      (\n" +
                "        param2 : 1\n" +
                "      )\n" +
                "      localProp1 : $service.parameters.param" +
                "    )\n" +
                "    ~service [test::ServiceStore] TestServiceGroup.TestService2\n" +
                "    (\n" +
                "      localProp2 : $service.parameters.param1" +
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
                "      ~paramMapping\n" +
                "      (\n" +
                "        param1 : 1\n" +
                "      )\n" +
                "      alpha : $service.parameters.param" +
                "    )\n" +
                "  }\n" +
                "  *test::model::B: ServiceStore\n" +
                "  {\n" +
                "    ~service [test::ServiceStore] TestServiceGroup1.TestServiceGroup2.TestService2\n" +
                "    (\n" +
                "      ~paramMapping\n" +
                "      (\n" +
                "        param : $this.beta->toOne(),\n" +
                "        param1 : 1.1\n" +
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
                "      ~paramMapping\n" +
                "      (\n" +
                "        invalid : 'PURE'\n" +
                "      )\n" +
                "    )\n" +
                "  }\n" +
                ")\n", "COMPILATION error at [72:9-24]: Service Parameter : 'invalid' is not valid");

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
                "      ~paramMapping\n" +
                "      (\n" +
                "        serializationFormat : 'CSV',\n" +
                "        serializationFormat : 'PURE'\n" +
                "      )\n" +
                "    )\n" +
                "  }\n" +
                ")\n", "COMPILATION error at [68:5-75:5]: Multiple Mappings for same parameter not allowed. Multiple mappings found for parameters : [serializationFormat].");

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
                "      param : Float ( location = query )\n" +
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
                "       delta : $service.parameters.param,\n" +
                "       zeta : $service.parameters.param\n" +
                "    )\n" +
                "  }\n" +
                ")\n", "COMPILATION error at [68:5-72:5]: Multiple Mappings for same parameter not allowed. Multiple mappings found for parameters : [param].");

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
                "      ~paramMapping\n" +
                "      (\n" +
                "        serializationFormat : 'PURE'\n" +
                "      )\n" +
                "      alpha : $service.parameters.serializationFormat\n" +
                "    )\n" +
                "  }\n" +
                ")\n", "COMPILATION error at [68:5-75:5]: Multiple Mappings for same parameter not allowed. Multiple mappings found for parameters : [serializationFormat].");
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
                "      ~paramMapping\n" +
                "      (\n" +
                "        serializationFormat : 'CSV'\n" +
                "      )\n" +
                "    )\n" +
                "  }\n" +
                ")\n", "COMPILATION error at [78:9-35]: Mapping enum service parameter is not yet supported !!");
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
                "            ~paramMapping\n" +
                "            (\n" +
                "                firmId : $this.firmId\n" +
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
}
