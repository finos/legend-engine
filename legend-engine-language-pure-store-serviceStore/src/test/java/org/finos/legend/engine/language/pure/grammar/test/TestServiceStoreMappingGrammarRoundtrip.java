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

public class TestServiceStoreMappingGrammarRoundtrip extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{
    @Test
    public void testSimpleServiceStoreMappings()
    {
        // Without ParameterMapping & simple service path
        test("###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "  *test::model: ServiceStore\n" +
                "  {\n" +
                "    ~service [test::ServiceStore] TestService\n" +
                "  }\n" +
                ")\n");

        //With PropertyIndexedParameterMapping & simple service path
        test("###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "  *test::model: ServiceStore\n" +
                "  {\n" +
                "    ~service [test::ServiceStore] TestService\n" +
                "    (\n" +
                "      prop : $service.parameters.param\n" +
                "    )\n" +
                "  }\n" +
                ")\n");

        //With ParameterIndexedParameterMapping & simple service path
        test("###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "  *test::model: ServiceStore\n" +
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

        //With single level service group & parameter mappings
        test("###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "  *test::model: ServiceStore\n" +
                "  {\n" +
                "    ~service [test::ServiceStore] TestServiceGroup.TestService\n" +
                "    (\n" +
                "      ~paramMapping\n" +
                "      (\n" +
                "        param : $this.prop\n" +
                "      )\n" +
                "      prop1 : $service.parameters.param1\n" +
                "    )\n" +
                "  }\n" +
                ")\n");

        //With multi level service group
        test("###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "  *test::model: ServiceStore\n" +
                "  {\n" +
                "    ~service [test::ServiceStore] TestServiceGroup1.TestServiceGroup2.TestService\n" +
                "    (\n" +
                "      ~paramMapping\n" +
                "      (\n" +
                "        param : $this.prop\n" +
                "      )\n" +
                "    )\n" +
                "  }\n" +
                ")\n");

        //With multi level service group
        test("###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "  *test::model: ServiceStore\n" +
                "  {\n" +
                "    ~service [test::ServiceStore] TestServiceGroup1.TestServiceGroup2.TestService\n" +
                "    (\n" +
                "      prop1 : $service.parameters.param1\n" +
                "    )\n" +
                "  }\n" +
                ")\n");

        //With multiple parameterMapping
        test("###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "  *test::model: ServiceStore\n" +
                "  {\n" +
                "    ~service [test::ServiceStore] TestServiceGroup1.TestServiceGroup2.TestService\n" +
                "    (\n" +
                "      ~paramMapping\n" +
                "      (\n" +
                "        param2 : 1\n" +
                "      )\n" +
                "      prop1 : $service.parameters.param1\n" +
                "    )\n" +
                "  }\n" +
                ")\n");
    }

    @Test
    public void testServiceStoreMappingWithMultipleServiceMapping()
    {
        test("###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "  *test::model: ServiceStore\n" +
                "  {\n" +
                "    ~service [test::ServiceStore] TestServiceGroup.TestService1\n" +
                "    (\n" +
                "      ~paramMapping\n" +
                "      (\n" +
                "        param : $this.prop + $this.prop2,\n" +
                "        param2 : 1\n" +
                "      )\n" +
                "      prop2 : $service.parameters.param1\n" +
                "    )\n" +
                "    ~service [test::ServiceStore] TestServiceGroup.TestService2\n" +
                "    (\n" +
                "      prop1 : $service.parameters.param1,\n" +
                "      prop2 : $service.parameters.param2\n" +
                "    )\n" +
                "  }\n" +
                ")\n");
    }

    @Test
    public void testServiceStoreMappingWithLocalMappingProperties()
    {
        test("###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "  *test::model: ServiceStore\n" +
                "  {\n" +
                "    +localProp : String[1];\n" +
                "\n" +
                "    ~service [test::ServiceStore] TestServiceGroup.TestService1\n" +
                "    (\n" +
                "      ~paramMapping\n" +
                "      (\n" +
                "        param : $this.localProp + $this.prop,\n" +
                "        param2 : 1\n" +
                "      )\n" +
                "    )\n" +
                "    ~service [test::ServiceStore] TestServiceGroup.TestService2\n" +
                "    (\n" +
                "      localProp : $service.parameters.param1\n" +
                "    )\n" +
                "  }\n" +
                ")\n");

        test("###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "  *test::model: ServiceStore\n" +
                "  {\n" +
                "    +localProp1 : String[1];\n" +
                "    +localProp2 : Boolean[1];\n" +
                "\n" +
                "    ~service [test::ServiceStore] TestServiceGroup.TestService1\n" +
                "    (\n" +
                "      ~paramMapping\n" +
                "      (\n" +
                "        param : $this.prop,\n" +
                "        param2 : 1\n" +
                "      )\n" +
                "    )\n" +
                "    ~service [test::ServiceStore] TestServiceGroup.TestService2\n" +
                "    (\n" +
                "      localProp1 : $service.parameters.param1,\n" +
                "      localProp2 : $service.parameters.param2\n" +
                "    )\n" +
                "  }\n" +
                ")\n");
    }

    @Test
    public void testMultipleServiceStoreMappings()
    {
        test("###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "  *test::model: ServiceStore\n" +
                "  {\n" +
                "    ~service [test::ServiceStore] TestServiceGroup1.TestServiceGroup2.TestService\n" +
                "    (\n" +
                "      ~paramMapping\n" +
                "      (\n" +
                "        param : $this.prop,\n" +
                "        param2 : 1\n" +
                "      )\n" +
                "      prop1 : $service.parameters.param1\n" +
                "    )\n" +
                "  }\n" +
                "  *test::model1: ServiceStore\n" +
                "  {\n" +
                "    ~service [test::ServiceStore] TestServiceGroup1.TestServiceGroup2.TestService\n" +
                "    (\n" +
                "      ~paramMapping\n" +
                "      (\n" +
                "        param : $this.prop,\n" +
                "        param2 : 1\n" +
                "      )\n" +
                "      prop1 : $service.parameters.param1\n" +
                "    )\n" +
                "  }\n" +
                ")\n");
    }
}
