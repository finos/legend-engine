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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposer;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

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

        //With Parameter & simple service path
        test("###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "  *test::model: ServiceStore\n" +
                "  {\n" +
                "    ~service [test::ServiceStore] TestService\n" +
                "    (\n" +
                "      ~request\n" +
                "      (\n" +
                "        parameters\n" +
                "        (\n" +
                "          param = $this.prop\n" +
                "        )\n" +
                "      )\n" +
                "    )\n" +
                "  }\n" +
                ")\n");

        //With Parameter & special character in param name
        test("###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "  *test::model: ServiceStore\n" +
                "  {\n" +
                "    ~service [test::ServiceStore] TestService\n" +
                "    (\n" +
                "      ~request\n" +
                "      (\n" +
                "        parameters\n" +
                "        (\n" +
                "          \"param name\" = $this.prop\n" +
                "        )\n" +
                "      )\n" +
                "    )\n" +
                "  }\n" +
                ")\n");

        //With Parameter & constant value
        test("###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "  *test::model: ServiceStore\n" +
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

        //With single level service group & parameter mappings
        test("###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "  *test::model: ServiceStore\n" +
                "  {\n" +
                "    ~service [test::ServiceStore] TestServiceGroup.TestService\n" +
                "    (\n" +
                "      ~request\n" +
                "      (\n" +
                "        parameters\n" +
                "        (\n" +
                "          param = $this.prop,\n" +
                "          param1 = $this.prop1\n" +
                "        )\n" +
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
                "      ~request\n" +
                "      (\n" +
                "        parameters\n" +
                "        (\n" +
                "          param = $this.prop\n" +
                "        )\n" +
                "      )\n" +
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
                "      ~request\n" +
                "      (\n" +
                "        parameters\n" +
                "        (\n" +
                "          param2 = 1,\n" +
                "          param1 = $this.prop1\n" +
                "        )\n" +
                "      )\n" +
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
                "      ~request\n" +
                "      (\n" +
                "        parameters\n" +
                "        (\n" +
                "          param = $this.prop + $this.prop2,\n" +
                "          param2 = 1,\n" +
                "          param1 = $this.prop2\n" +
                "        )\n" +
                "      )\n" +
                "    )\n" +
                "    ~service [test::ServiceStore] TestServiceGroup.TestService2\n" +
                "    (\n" +
                "      ~request\n" +
                "      (\n" +
                "        parameters\n" +
                "        (\n" +
                "          param1 = $this.prop1,\n" +
                "          param2 = $this.prop2\n" +
                "        )\n" +
                "      )\n" +
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
                "      ~request\n" +
                "      (\n" +
                "        parameters\n" +
                "        (\n" +
                "          param = $this.localProp + $this.prop,\n" +
                "          param2 = 1\n" +
                "        )\n" +
                "      )\n" +
                "    )\n" +
                "    ~service [test::ServiceStore] TestServiceGroup.TestService2\n" +
                "    (\n" +
                "      ~request\n" +
                "      (\n" +
                "        parameters\n" +
                "        (\n" +
                "          param1 = $this.localProp\n" +
                "        )\n" +
                "      )\n" +
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
                "      ~request\n" +
                "      (\n" +
                "        parameters\n" +
                "        (\n" +
                "          param = $this.prop,\n" +
                "          param2 = 1\n" +
                "        )\n" +
                "      )\n" +
                "    )\n" +
                "    ~service [test::ServiceStore] TestServiceGroup.TestService2\n" +
                "    (\n" +
                "      ~request\n" +
                "      (\n" +
                "        parameters\n" +
                "        (\n" +
                "          param1 = $this.localProp1,\n" +
                "          param2 = $this.localProp2\n" +
                "        )\n" +
                "      )\n" +
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
                "      ~request\n" +
                "      (\n" +
                "        parameters\n" +
                "        (\n" +
                "          param = $this.prop,\n" +
                "          param2 = 1,\n" +
                "          param1 = $this.prop1\n" +
                "        )\n" +
                "      )\n" +
                "    )\n" +
                "  }\n" +
                "  *test::model1: ServiceStore\n" +
                "  {\n" +
                "    ~service [test::ServiceStore] TestServiceGroup1.TestServiceGroup2.TestService\n" +
                "    (\n" +
                "      ~request\n" +
                "      (\n" +
                "        parameters\n" +
                "        (\n" +
                "          param = $this.prop,\n" +
                "          param2 = 1,\n" +
                "          param1 = $this.prop1\n" +
                "        )\n" +
                "      )\n" +
                "    )\n" +
                "  }\n" +
                ")\n");
    }

    @Test
    public void testServiceStoreMappingPathOffset()
    {
        test("###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "  *test::model: ServiceStore\n" +
                "  {\n" +
                "    ~service [test::ServiceStore] TestServiceGroup1.TestServiceGroup2.TestService\n" +
                "    (\n" +
                "      ~path $service.response.pathProperty\n" +
                "    )\n" +
                "  }\n" +
                ")\n");

        test("###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "  *test::model: ServiceStore\n" +
                "  {\n" +
                "    ~service [test::ServiceStore] TestServiceGroup1.TestServiceGroup2.TestService\n" +
                "    (\n" +
                "      ~path $service.response.pathProperty\n" +
                "      ~request\n" +
                "      (\n" +
                "        parameters\n" +
                "        (\n" +
                "          param = $this.prop,\n" +
                "          param2 = 1\n" +
                "        )\n" +
                "      )\n" +
                "    )\n" +
                "  }\n" +
                ")\n");
    }

    @Test
    public void testRequestBodyServiceStoreMapping()
    {
        test("###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "  *test::model: ServiceStore\n" +
                "  {\n" +
                "    ~service [test::ServiceStore] TestService\n" +
                "    (\n" +
                "      ~request\n" +
                "      (\n" +
                "        body = $this.requestBodyProp\n" +
                "      )\n" +
                "    )\n" +
                "  }\n" +
                ")\n");

        test("###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "  *test::model: ServiceStore\n" +
                "  {\n" +
                "    ~service [test::ServiceStore] TestService\n" +
                "    (\n" +
                "      ~request\n" +
                "      (\n" +
                "        body = ^test::requestBody(propA=$this.prop , propB=1)\n" +
                "      )\n" +
                "    )\n" +
                "  }\n" +
                ")\n");

        test("###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "  *test::model: ServiceStore\n" +
                "  {\n" +
                "    ~service [test::ServiceStore] TestService\n" +
                "    (\n" +
                "      ~request\n" +
                "      (\n" +
                "        parameters\n" +
                "        (\n" +
                "          param = $this.prop,\n" +
                "          param2 = 1\n" +
                "        )\n" +
                "        body = ^test::requestBody(propA=$this.propA , propB=1)\n" +
                "      )\n" +
                "    )\n" +
                "  }\n" +
                ")\n");

        test("###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "  *test::model: ServiceStore\n" +
                "  {\n" +
                "    ~service [test::ServiceStore] TestService\n" +
                "    (\n" +
                "      ~request\n" +
                "      (\n" +
                "        body = ^test::requestBody(propA=$this.propA , propB=1 , propC=^test::nestedRequestBodyModel(propD='xyz' , propE=$this.prop))\n" +
                "      )\n" +
                "    )\n" +
                "  }\n" +
                ")\n");
    }
}
