// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.language.snowflakeM2MUdf.api;

import com.fasterxml.jackson.core.type.TypeReference;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.finos.legend.engine.functionActivator.api.FunctionActivatorAPI;
import org.finos.legend.engine.functionActivator.api.input.FunctionActivatorInput;
import org.finos.legend.engine.functionActivator.api.output.FunctionActivatorInfo;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.List;


public class TestValidation
{
    private final Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions = (PureModel pureModel) -> PureCoreExtensionLoader.extensions().flatCollect(e -> e.extraPureCoreExtensions(pureModel.getExecutionSupport()));
    private final FunctionActivatorAPI api = new FunctionActivatorAPI(new ModelManager(DeploymentMode.TEST), routerExtensions);

    @Test
    public void testProperPlan()
    {
        String val = "###Relational\n" +
                "Database demo::stores::DemoDb\n" +
                "(\n" +
                "  Schema DEMO_SCHEMA\n" +
                "  (\n" +
                "    Table EMPLOYEE\n" +
                "    (\n" +
                "      ID VARCHAR(16777216) PRIMARY KEY,\n" +
                "      EMPLOYEE_NAME VARCHAR(16777216)\n" +
                "    )\n" +
                "  )\n" +
                ")\n" +
                "\n" +
                "\n" +
                "###Snowflake\n" +
                "SnowflakeM2MUdf test::udf::MyUDF\n" +
                "{\n" +
                "   udfName : 'MyUDF';\n" +
                "   function : test::query::getFirmDetailsWithInput(String[1]):String[1];\n" +
                "   ownership : Deployment { identifier: '1234'};\n" +
                "   description : 'A simple Snowflake M2M Udf!';\n" +
                "   deploymentSchema : 'legend_native_apps_1';\n" +
                "   deploymentStage : 'demo_schema.snowflakeStage';\n" +
                "   activationConfiguration : demo::connections::DeploymentConnection;\n" +
                "}\n" +
                "\n" +
                "###Pure\n" +
                "Class test::model::Firms\n" +
                "{\n" +
                "  firms: test::model::Firm[1..*];\n" +
                "}\n" +
                "\n" +
                "Class test::model::Firm\n" +
                "{\n" +
                "  legalName: String[1];\n" +
                "  employees: test::model::Person[*];\n" +
                "}\n" +
                "\n" +
                "Class test::model::Person\n" +
                "{\n" +
                "  firstName: String[1];\n" +
                "  lastName: String[1];\n" +
                "}\n" +
                "\n" +
                "function test::query::getFirmDetailsWithInput(input: String[1]): String[1]\n" +
                "{\n" +
                "  test::model::Firms.all()->graphFetch(\n" +
                "    #{\n" +
                "      test::model::Firms{\n" +
                "        firms{\n" +
                "          legalName,\n" +
                "          employees{\n" +
                "            firstName,\n" +
                "            lastName\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }#\n" +
                "  )->serialize(\n" +
                "    #{\n" +
                "      test::model::Firms{\n" +
                "        firms{\n" +
                "          legalName,\n" +
                "          employees{\n" +
                "            firstName,\n" +
                "            lastName\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }#\n" +
                "  )->from(\n" +
                "    test::mapping::firmMapping,\n" +
                "    test::runtime::testRuntimeWithInput\n" +
                "  )\n" +
                "}\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping test::mapping::firmMapping\n" +
                "(\n" +
                "  *test::model::Firms: Pure\n" +
                "  {\n" +
                "    ~src test::model::Firms\n" +
                "    firms: $src.firms\n" +
                "  }\n" +
                "  *test::model::Firm: Pure\n" +
                "  {\n" +
                "    ~src test::model::Firm\n" +
                "    legalName: $src.legalName,\n" +
                "    employees: $src.employees\n" +
                "  }\n" +
                ")\n" +
                "\n" +
                "###Connection\n" +
                "JsonModelConnection test::connection::testConnectionWithInput\n" +
                "{\n" +
                "  class: test::model::Firms;\n" +
                "  url: 'data:application/json,${input}';\n" +
                "}\n" +
                "\n" +
                "###Runtime\n" +
                "Runtime test::runtime::testRuntimeWithInput\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    test::mapping::firmMapping\n" +
                "  ];\n" +
                "  connections:\n" +
                "  [\n" +
                "    ModelStore:\n" +
                "    [\n" +
                "      connection_1: test::connection::testConnectionWithInput\n" +
                "    ]\n" +
                "  ];\n" +
                "}\n" +
                "\n" +
                "###Connection\n" +
                "RelationalDatabaseConnection demo::connections::DeploymentConnection\n" +
                "{\n" +
                "  store: demo::stores::DemoDb;\n" +
                "  type: Snowflake;\n" +
                "  specification: Snowflake\n" +
                "      {\n" +
                "           name: 'dbName';\n" +
                "           account: 'account';\n" +
                "           warehouse: 'warehouse';\n" +
                "           region: 'region';\n" +
                "      };\n" +
                "    auth: DefaultH2;\n" +
                "}\n";
        Response response = api.validate(new FunctionActivatorInput("vX_X_X", "test::udf::MyUDF", PureGrammarParser.newInstance().parseModel(val)), null, null);
        Assert.assertEquals("{\"errors\":[],\"warnings\":[]}", response.getEntity().toString());
    }

    @Test
    public void testInvalidFunctionParameters()
    {
        String val = "###Relational\n" +
                "Database demo::stores::DemoDb\n" +
                "(\n" +
                "  Schema DEMO_SCHEMA\n" +
                "  (\n" +
                "    Table EMPLOYEE\n" +
                "    (\n" +
                "      ID VARCHAR(16777216) PRIMARY KEY,\n" +
                "      EMPLOYEE_NAME VARCHAR(16777216)\n" +
                "    )\n" +
                "  )\n" +
                ")\n" +
                "\n" +
                "\n" +
                "###Snowflake\n" +
                "SnowflakeM2MUdf test::udf::InvalidInputSizeUDF\n" +
                "{\n" +
                "   udfName : 'MyUDF';\n" +
                "   function : test::query::invalidInputSizeFunction(String[1],String[1]):String[1];\n" +
                "   ownership : Deployment { identifier: '1234'};\n" +
                "   description : 'A simple Snowflake M2M Udf!';\n" +
                "   deploymentSchema : 'legend_native_apps_1';\n" +
                "   deploymentStage : 'demo_schema.snowflakeStage';\n" +
                "   activationConfiguration : demo::connections::DeploymentConnection;\n" +
                "}\n" +
                "\n" +
                "SnowflakeM2MUdf test::udf::InvalidInputTypeUDF\n" +
                "{\n" +
                "   udfName : 'MyUDF';\n" +
                "   function : test::query::invalidInputTypeFunction(Integer[1]):String[1];\n" +
                "   ownership : Deployment { identifier: '1234'};\n" +
                "   description : 'A simple Snowflake M2M Udf!';\n" +
                "   deploymentSchema : 'legend_native_apps_1';\n" +
                "   deploymentStage : 'demo_schema.snowflakeStage';\n" +
                "   activationConfiguration : demo::connections::DeploymentConnection;\n" +
                "}\n" +
                "\n" +
                "SnowflakeM2MUdf test::udf::InvalidInputMultiplicityUDF\n" +
                "{\n" +
                "   udfName : 'MyUDF';\n" +
                "   function : test::query::invalidInputMultiplicityFunction(String[2]):String[1];\n" +
                "   ownership : Deployment { identifier: '1234'};\n" +
                "   description : 'A simple Snowflake M2M Udf!';\n" +
                "   deploymentSchema : 'legend_native_apps_1';\n" +
                "   deploymentStage : 'demo_schema.snowflakeStage';\n" +
                "   activationConfiguration : demo::connections::DeploymentConnection;\n" +
                "}\n" +
                "\n" +
                "###Pure\n" +
                "Class test::model::Firms\n" +
                "{\n" +
                "  firms: test::model::Firm[1..*];\n" +
                "}\n" +
                "\n" +
                "Class test::model::Firm\n" +
                "{\n" +
                "  legalName: String[1];\n" +
                "  employees: test::model::Person[*];\n" +
                "}\n" +
                "\n" +
                "Class test::model::Person\n" +
                "{\n" +
                "  firstName: String[1];\n" +
                "  lastName: String[1];\n" +
                "}\n" +
                "\n" +
                "function test::query::invalidInputSizeFunction(input: String[1], input2: String[1]): String[1]\n" +
                "{\n" +
                "  test::model::Firms.all()->graphFetch(\n" +
                "    #{\n" +
                "      test::model::Firms{\n" +
                "        firms{\n" +
                "          legalName,\n" +
                "          employees{\n" +
                "            firstName,\n" +
                "            lastName\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }#\n" +
                "  )->serialize(\n" +
                "    #{\n" +
                "      test::model::Firms{\n" +
                "        firms{\n" +
                "          legalName,\n" +
                "          employees{\n" +
                "            firstName,\n" +
                "            lastName\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }#\n" +
                "  )->from(\n" +
                "    test::mapping::firmMapping,\n" +
                "    test::runtime::testRuntimeWithInput\n" +
                "  )\n" +
                "}\n" +
                "\n" +
                "function test::query::invalidInputTypeFunction(input: Integer[1]): String[1]\n" +
                "{\n" +
                "  test::model::Firms.all()->graphFetch(\n" +
                "    #{\n" +
                "      test::model::Firms{\n" +
                "        firms{\n" +
                "          legalName,\n" +
                "          employees{\n" +
                "            firstName,\n" +
                "            lastName\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }#\n" +
                "  )->serialize(\n" +
                "    #{\n" +
                "      test::model::Firms{\n" +
                "        firms{\n" +
                "          legalName,\n" +
                "          employees{\n" +
                "            firstName,\n" +
                "            lastName\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }#\n" +
                "  )->from(\n" +
                "    test::mapping::firmMapping,\n" +
                "    test::runtime::testRuntimeWithInput\n" +
                "  )\n" +
                "}\n" +
                "\n" +
                "function test::query::invalidInputMultiplicityFunction(input: String[2]): String[1]\n" +
                "{\n" +
                "  test::model::Firms.all()->graphFetch(\n" +
                "    #{\n" +
                "      test::model::Firms{\n" +
                "        firms{\n" +
                "          legalName,\n" +
                "          employees{\n" +
                "            firstName,\n" +
                "            lastName\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }#\n" +
                "  )->serialize(\n" +
                "    #{\n" +
                "      test::model::Firms{\n" +
                "        firms{\n" +
                "          legalName,\n" +
                "          employees{\n" +
                "            firstName,\n" +
                "            lastName\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }#\n" +
                "  )->from(\n" +
                "    test::mapping::firmMapping,\n" +
                "    test::runtime::testRuntimeWithInput\n" +
                "  )\n" +
                "}\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping test::mapping::firmMapping\n" +
                "(\n" +
                "  *test::model::Firms: Pure\n" +
                "  {\n" +
                "    ~src test::model::Firms\n" +
                "    firms: $src.firms\n" +
                "  }\n" +
                "  *test::model::Firm: Pure\n" +
                "  {\n" +
                "    ~src test::model::Firm\n" +
                "    legalName: $src.legalName,\n" +
                "    employees: $src.employees\n" +
                "  }\n" +
                ")\n" +
                "\n" +
                "###Connection\n" +
                "JsonModelConnection test::connection::testConnectionWithInput\n" +
                "{\n" +
                "  class: test::model::Firms;\n" +
                "  url: 'data:application/json,${input}';\n" +
                "}\n" +
                "\n" +
                "###Runtime\n" +
                "Runtime test::runtime::testRuntimeWithInput\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    test::mapping::firmMapping\n" +
                "  ];\n" +
                "  connections:\n" +
                "  [\n" +
                "    ModelStore:\n" +
                "    [\n" +
                "      connection_1: test::connection::testConnectionWithInput\n" +
                "    ]\n" +
                "  ];\n" +
                "}\n" +
                "\n" +
                "###Connection\n" +
                "RelationalDatabaseConnection demo::connections::DeploymentConnection\n" +
                "{\n" +
                "  store: demo::stores::DemoDb;\n" +
                "  type: Snowflake;\n" +
                "  specification: Snowflake\n" +
                "      {\n" +
                "           name: 'dbName';\n" +
                "           account: 'account';\n" +
                "           warehouse: 'warehouse';\n" +
                "           region: 'region';\n" +
                "      };\n" +
                "    auth: DefaultH2;\n" +
                "}\n";
        Response responseInvalidInputSize = api.validate(new FunctionActivatorInput("vX_X_X", "test::udf::InvalidInputSizeUDF", PureGrammarParser.newInstance().parseModel(val)), null, null);
        Response responseInvalidInputType = api.validate(new FunctionActivatorInput("vX_X_X", "test::udf::InvalidInputSizeUDF", PureGrammarParser.newInstance().parseModel(val)), null, null);
        Response responseInvalidInputMultiplicity = api.validate(new FunctionActivatorInput("vX_X_X", "test::udf::InvalidInputSizeUDF", PureGrammarParser.newInstance().parseModel(val)), null, null);
        Assert.assertEquals("{\"errors\":[{\"message\":\"SnowflakeM2MUdf function activator should have a single parameter with String type of multiplicity 1\"}],\"warnings\":[]}", responseInvalidInputSize.getEntity().toString());
        Assert.assertEquals("{\"errors\":[{\"message\":\"SnowflakeM2MUdf function activator should have a single parameter with String type of multiplicity 1\"}],\"warnings\":[]}", responseInvalidInputType.getEntity().toString());
        Assert.assertEquals("{\"errors\":[{\"message\":\"SnowflakeM2MUdf function activator should have a single parameter with String type of multiplicity 1\"}],\"warnings\":[]}", responseInvalidInputMultiplicity.getEntity().toString());
    }

    @Test
    public void testInvalidFunctionReturnType()
    {
        String val = "###Relational\n" +
                "Database demo::stores::DemoDb\n" +
                "(\n" +
                "  Schema DEMO_SCHEMA\n" +
                "  (\n" +
                "    Table EMPLOYEE\n" +
                "    (\n" +
                "      ID VARCHAR(16777216) PRIMARY KEY,\n" +
                "      EMPLOYEE_NAME VARCHAR(16777216)\n" +
                "    )\n" +
                "  )\n" +
                ")\n" +
                "\n" +
                "###Snowflake\n" +
                "SnowflakeM2MUdf test::udf::InvalidReturnTypeUDF\n" +
                "{\n" +
                "   udfName : 'MyUDF';\n" +
                "   function : test::query::invalidReturnTypeFunction(String[1]):Integer[1];\n" +
                "   ownership : Deployment { identifier: '1234'};\n" +
                "   description : 'A simple Snowflake M2M Udf!';\n" +
                "   deploymentSchema : 'legend_native_apps_1';\n" +
                "   deploymentStage : 'demo_schema.snowflakeStage';\n" +
                "   activationConfiguration : demo::connections::DeploymentConnection;\n" +
                "}\n" +
                "\n" +
                "SnowflakeM2MUdf test::udf::InvalidReturnTypeMultiplicityUDF\n" +
                "{\n" +
                "   udfName : 'MyUDF';\n" +
                "   function : test::query::invalidReturnTypeMultiplicityFunction(String[1]):String[2];\n" +
                "   ownership : Deployment { identifier: '1234'};\n" +
                "   description : 'A simple Snowflake M2M Udf!';\n" +
                "   deploymentSchema : 'legend_native_apps_1';\n" +
                "   deploymentStage : 'demo_schema.snowflakeStage';\n" +
                "   activationConfiguration : demo::connections::DeploymentConnection;\n" +
                "}\n" +
                "\n" +
                "###Pure\n" +
                "Class test::model::Firms\n" +
                "{\n" +
                "  firms: test::model::Firm[1..*];\n" +
                "}\n" +
                "\n" +
                "Class test::model::Firm\n" +
                "{\n" +
                "  legalName: String[1];\n" +
                "  employees: test::model::Person[*];\n" +
                "}\n" +
                "\n" +
                "Class test::model::Person\n" +
                "{\n" +
                "  firstName: String[1];\n" +
                "  lastName: String[1];\n" +
                "}\n" +
                "\n" +
                "function test::query::invalidReturnTypeFunction(input: String[1]): Integer[1]\n" +
                "{\n" +
                "  1\n" +
                "}\n" +
                "\n" +
                "function test::query::invalidReturnTypeMultiplicityFunction(input: String[1]): String[2]\n" +
                "{\n" +
                "  ['abc', 'xyz']\n" +
                "}\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping test::mapping::firmMapping\n" +
                "(\n" +
                "  *test::model::Firms: Pure\n" +
                "  {\n" +
                "    ~src test::model::Firms\n" +
                "    firms: $src.firms\n" +
                "  }\n" +
                "  *test::model::Firm: Pure\n" +
                "  {\n" +
                "    ~src test::model::Firm\n" +
                "    legalName: $src.legalName,\n" +
                "    employees: $src.employees\n" +
                "  }\n" +
                ")\n" +
                "\n" +
                "###Connection\n" +
                "JsonModelConnection test::connection::testConnectionWithInput\n" +
                "{\n" +
                "  class: test::model::Firms;\n" +
                "  url: 'data:application/json,${input}';\n" +
                "}\n" +
                "\n" +
                "###Runtime\n" +
                "Runtime test::runtime::testRuntimeWithInput\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    test::mapping::firmMapping\n" +
                "  ];\n" +
                "  connections:\n" +
                "  [\n" +
                "    ModelStore:\n" +
                "    [\n" +
                "      connection_1: test::connection::testConnectionWithInput\n" +
                "    ]\n" +
                "  ];\n" +
                "}\n" +
                "\n" +
                "###Connection\n" +
                "RelationalDatabaseConnection demo::connections::DeploymentConnection\n" +
                "{\n" +
                "  store: demo::stores::DemoDb;\n" +
                "  type: Snowflake;\n" +
                "  specification: Snowflake\n" +
                "      {\n" +
                "           name: 'dbName';\n" +
                "           account: 'account';\n" +
                "           warehouse: 'warehouse';\n" +
                "           region: 'region';\n" +
                "      };\n" +
                "    auth: DefaultH2;\n" +
                "}\n";
        Response responseInvalidReturnType = api.validate(new FunctionActivatorInput("vX_X_X", "test::udf::InvalidReturnTypeUDF", PureGrammarParser.newInstance().parseModel(val)), null, null);
        Response responseInvalidReturnTypeMultiplicity = api.validate(new FunctionActivatorInput("vX_X_X", "test::udf::InvalidReturnTypeMultiplicityUDF", PureGrammarParser.newInstance().parseModel(val)), null, null);
        Assert.assertEquals("{\"errors\":[{\"message\":\"SnowflakeM2MUdf function activator return type should be String of multiplicity 1\"}],\"warnings\":[]}", responseInvalidReturnType.getEntity().toString());
        Assert.assertEquals("{\"errors\":[{\"message\":\"SnowflakeM2MUdf function activator return type should be String of multiplicity 1\"}],\"warnings\":[]}", responseInvalidReturnTypeMultiplicity.getEntity().toString());
    }

    @Test
    public void testInvalidDeploymentSchema()
    {
        String val = "###Relational\n" +
                "Database demo::stores::DemoDb\n" +
                "(\n" +
                "  Schema DEMO_SCHEMA\n" +
                "  (\n" +
                "    Table EMPLOYEE\n" +
                "    (\n" +
                "      ID VARCHAR(16777216) PRIMARY KEY,\n" +
                "      EMPLOYEE_NAME VARCHAR(16777216)\n" +
                "    )\n" +
                "  )\n" +
                ")\n" +
                "\n" +
                "\n" +
                "###Snowflake\n" +
                "SnowflakeM2MUdf test::udf::MyUDF\n" +
                "{\n" +
                "   udfName : 'MyUDF';\n" +
                "   function : test::query::getFirmDetailsWithInput(String[1]):String[1];\n" +
                "   ownership : Deployment { identifier: '1234'};\n" +
                "   description : 'A simple Snowflake M2M Udf!';\n" +
                "   deploymentSchema : 'legend_native_apps 1';\n" +
                "   deploymentStage : 'demo_schema.snowflakeStage';\n" +
                "   activationConfiguration : demo::connections::DeploymentConnection;\n" +
                "}\n" +
                "\n" +
                "###Pure\n" +
                "Class test::model::Firms\n" +
                "{\n" +
                "  firms: test::model::Firm[1..*];\n" +
                "}\n" +
                "\n" +
                "Class test::model::Firm\n" +
                "{\n" +
                "  legalName: String[1];\n" +
                "  employees: test::model::Person[*];\n" +
                "}\n" +
                "\n" +
                "Class test::model::Person\n" +
                "{\n" +
                "  firstName: String[1];\n" +
                "  lastName: String[1];\n" +
                "}\n" +
                "\n" +
                "function test::query::getFirmDetailsWithInput(input: String[1]): String[1]\n" +
                "{\n" +
                "  test::model::Firms.all()->graphFetch(\n" +
                "    #{\n" +
                "      test::model::Firms{\n" +
                "        firms{\n" +
                "          legalName,\n" +
                "          employees{\n" +
                "            firstName,\n" +
                "            lastName\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }#\n" +
                "  )->serialize(\n" +
                "    #{\n" +
                "      test::model::Firms{\n" +
                "        firms{\n" +
                "          legalName,\n" +
                "          employees{\n" +
                "            firstName,\n" +
                "            lastName\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }#\n" +
                "  )->from(\n" +
                "    test::mapping::firmMapping,\n" +
                "    test::runtime::testRuntimeWithInput\n" +
                "  )\n" +
                "}\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping test::mapping::firmMapping\n" +
                "(\n" +
                "  *test::model::Firms: Pure\n" +
                "  {\n" +
                "    ~src test::model::Firms\n" +
                "    firms: $src.firms\n" +
                "  }\n" +
                "  *test::model::Firm: Pure\n" +
                "  {\n" +
                "    ~src test::model::Firm\n" +
                "    legalName: $src.legalName,\n" +
                "    employees: $src.employees\n" +
                "  }\n" +
                ")\n" +
                "\n" +
                "###Connection\n" +
                "JsonModelConnection test::connection::testConnectionWithInput\n" +
                "{\n" +
                "  class: test::model::Firms;\n" +
                "  url: 'data:application/json,${input}';\n" +
                "}\n" +
                "\n" +
                "###Runtime\n" +
                "Runtime test::runtime::testRuntimeWithInput\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    test::mapping::firmMapping\n" +
                "  ];\n" +
                "  connections:\n" +
                "  [\n" +
                "    ModelStore:\n" +
                "    [\n" +
                "      connection_1: test::connection::testConnectionWithInput\n" +
                "    ]\n" +
                "  ];\n" +
                "}\n" +
                "\n" +
                "###Connection\n" +
                "RelationalDatabaseConnection demo::connections::DeploymentConnection\n" +
                "{\n" +
                "  store: demo::stores::DemoDb;\n" +
                "  type: Snowflake;\n" +
                "  specification: Snowflake\n" +
                "      {\n" +
                "           name: 'dbName';\n" +
                "           account: 'account';\n" +
                "           warehouse: 'warehouse';\n" +
                "           region: 'region';\n" +
                "      };\n" +
                "    auth: DefaultH2;\n" +
                "}\n";
        Response response = api.validate(new FunctionActivatorInput("vX_X_X", "test::udf::MyUDF", PureGrammarParser.newInstance().parseModel(val)), null, null);
        Assert.assertEquals("{\"errors\":[{\"message\":\"Deployment schema can only contains letter, digit, underscore and dollar\"}],\"warnings\":[]}", response.getEntity().toString());
    }

    @Test
    public void testList() throws Exception
    {
        Response response = api.list(null);
        List<FunctionActivatorInfo> info = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().readValue(response.getEntity().toString(), new TypeReference<List<FunctionActivatorInfo>>(){});
        Assert.assertEquals(2, info.size());
        Assert.assertEquals("Snowflake M2MUdf", info.get(1).name);
        Assert.assertEquals("Create a scalar UDF function which can be applied on a VARIANT column to execute the M2M transform", info.get(1).description);
        Assert.assertEquals("meta::protocols::pure::vX_X_X::metamodel::function::activator::snowflakeM2MUdf::SnowflakeM2MUdf", info.get(1).configuration.topElement);
        Assert.assertEquals(8, info.get(1).configuration.model.size());
    }
}
