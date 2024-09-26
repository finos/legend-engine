// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.generation.dataquality;

import net.javacrumbs.jsonunit.JsonAssert;
import net.javacrumbs.jsonunit.core.Option;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.exception.PureAssertFailException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class TestDataQualityLambdaGenerator
{
    @Test
    public void testAssertionForNestedConstraints()
    {
        String validation = COMPILATION_PREREQUISITE_CODE +
                "###DataQualityValidation\n" +
                "DataQualityValidation meta::dataquality::PersonDataQualityValidation\n" +
                "{\n" +
                "    context: fromMappingAndRuntime(meta::dataquality::dataqualitymappings, meta::dataquality::DataQualityRuntime);\n" +
                "    filter: p:meta::dataquality::Person[1] | $p.name=='John';\n" +
                "    validationTree: $[\n" +
                "      meta::dataquality::Person<mustBeOfLegalAge>{\n" +
                "        name,\n" +
                "        addresses<validAddressId>{\n" +
                "         addressId\n" +
                "        }\n" +
                "      }\n" +
                "    ]$;\n" +
                "}";
        PureModelContextData modelData = PureGrammarParser.newInstance().parseModel(validation);
        PureModel model = Compiler.compile(modelData, DeploymentMode.TEST_IGNORE_FUNCTION_MATCH, Identity.getAnonymousIdentity().getName());
        assertEquals("Nested constraints are not currently supported!",
                assertThrows(PureAssertFailException.class, () -> DataQualityLambdaGenerator.generateLambda(model, "meta::dataquality::PersonDataQualityValidation")).getInfo());

    }

    @Test
    public void testLambdaGeneration_multipleConstraints()
    {
        String validation = COMPILATION_PREREQUISITE_CODE +
                "###DataQualityValidation\n" +
                "DataQualityValidation meta::dataquality::PersonDataQualityValidation\n" +
                "{\n" +
                "    context: fromMappingAndRuntime(meta::dataquality::dataqualitymappings, meta::dataquality::DataQualityRuntime);\n" +
                "    validationTree: $[\n" +
                "      meta::dataquality::Person<mustBeOfLegalAge,validNameLength,ageMustBePositive>{\n" +
                "        name,\n" +
                "        addresses{\n" +
                "         addressId\n" +
                "        }\n" +
                "      }\n" +
                "    ]$;\n" +
                "}";
        PureModelContextData modelData = PureGrammarParser.newInstance().parseModel(validation);
        PureModel model = Compiler.compile(modelData, DeploymentMode.TEST_IGNORE_FUNCTION_MATCH, Identity.getAnonymousIdentity().getName());
        LambdaFunction lambdaFunction = DataQualityLambdaGenerator.generateLambda(model, "meta::dataquality::PersonDataQualityValidation");
        Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions = (PureModel p) -> PureCoreExtensionLoader.extensions().flatCollect(e -> e.extraPureCoreExtensions(p.getExecutionSupport()));
        String lambdaJson = DataQualityLambdaGenerator.transformLambdaAsJson(lambdaFunction, model, routerExtensions);
        JsonAssert.assertJsonEquals("{\"_type\":\"lambda\",\"body\":[{\"fControl\":\"from_T_m__Mapping_1__Runtime_1__T_m_\",\"function\":\"from\",\"_type\":\"func\",\"parameters\":[{\"fControl\":\"serialize_T_MANY__RootGraphFetchTree_1__String_1_\",\"function\":\"serialize\",\"_type\":\"func\",\"parameters\":[{\"fControl\":\"graphFetchChecked_T_MANY__RootGraphFetchTree_1__Checked_MANY_\",\"function\":\"graphFetchChecked\",\"_type\":\"func\",\"parameters\":[{\"fControl\":\"filter_T_MANY__Function_1__T_MANY_\",\"function\":\"filter\",\"_type\":\"func\",\"parameters\":[{\"fControl\":\"getAll_Class_1__Date_1__T_MANY_\",\"function\":\"getAll\",\"_type\":\"func\",\"parameters\":[{\"fullPath\":\"meta::dataquality::Person\",\"_type\":\"packageableElementPtr\"},{\"_type\":\"var\",\"name\":\"businessDate\"}]},{\"_type\":\"lambda\",\"body\":[{\"fControl\":\"or_Boolean_1__Boolean_1__Boolean_1_\",\"function\":\"or\",\"_type\":\"func\",\"parameters\":[{\"fControl\":\"not_Boolean_1__Boolean_1_\",\"function\":\"not\",\"_type\":\"func\",\"parameters\":[{\"fControl\":\"greaterThanEqual_Number_1__Number_1__Boolean_1_\",\"function\":\"greaterThanEqual\",\"_type\":\"func\",\"parameters\":[{\"_type\":\"property\",\"property\":\"age\",\"parameters\":[{\"_type\":\"var\",\"name\":\"this\"}]},{\"_type\":\"integer\",\"value\":0}]}]},{\"fControl\":\"or_Boolean_1__Boolean_1__Boolean_1_\",\"function\":\"or\",\"_type\":\"func\",\"parameters\":[{\"fControl\":\"not_Boolean_1__Boolean_1_\",\"function\":\"not\",\"_type\":\"func\",\"parameters\":[{\"fControl\":\"greaterThanEqual_Number_1__Number_1__Boolean_1_\",\"function\":\"greaterThanEqual\",\"_type\":\"func\",\"parameters\":[{\"_type\":\"property\",\"property\":\"age\",\"parameters\":[{\"_type\":\"var\",\"name\":\"this\"}]},{\"_type\":\"integer\",\"value\":18}]}]},{\"fControl\":\"not_Boolean_1__Boolean_1_\",\"function\":\"not\",\"_type\":\"func\",\"parameters\":[{\"fControl\":\"lessThan_Number_1__Number_1__Boolean_1_\",\"function\":\"lessThan\",\"_type\":\"func\",\"parameters\":[{\"fControl\":\"length_String_1__Integer_1_\",\"function\":\"length\",\"_type\":\"func\",\"parameters\":[{\"_type\":\"property\",\"property\":\"name\",\"parameters\":[{\"_type\":\"var\",\"name\":\"this\"}]}]},{\"_type\":\"integer\",\"value\":1000}]}]}]}]}]}]},{\"_type\":\"classInstance\",\"type\":\"rootGraphFetchTree\",\"value\":{\"subTrees\":[{\"subTrees\":[{\"_type\":\"propertyGraphFetchTree\",\"property\":\"addressId\"}],\"_type\":\"propertyGraphFetchTree\",\"property\":\"addresses\"},{\"_type\":\"propertyGraphFetchTree\",\"property\":\"age\"},{\"_type\":\"propertyGraphFetchTree\",\"property\":\"name\"}],\"_type\":\"rootGraphFetchTree\",\"class\":\"meta::dataquality::Person\"}}]},{\"_type\":\"classInstance\",\"type\":\"rootGraphFetchTree\",\"value\":{\"subTrees\":[{\"subTrees\":[{\"_type\":\"propertyGraphFetchTree\",\"property\":\"addressId\"}],\"_type\":\"propertyGraphFetchTree\",\"property\":\"addresses\"},{\"_type\":\"propertyGraphFetchTree\",\"property\":\"age\"},{\"_type\":\"propertyGraphFetchTree\",\"property\":\"name\"}],\"_type\":\"rootGraphFetchTree\",\"class\":\"meta::dataquality::Person\"}}]},{\"fullPath\":\"meta::dataquality::dataqualitymappings\",\"_type\":\"packageableElementPtr\"},{\"_type\":\"classInstance\",\"type\":\"runtimeInstance\",\"value\":{\"runtime\":{\"_type\":\"legacyRuntime\",\"connections\":[{\"_type\":\"RelationalDatabaseConnection\",\"authenticationStrategy\":{\"_type\":\"h2Default\"},\"type\":\"H2\",\"datasourceSpecification\":{\"_type\":\"h2Local\"},\"element\":\"meta::dataquality::db\"}]}}}]}]}",
                lambdaJson, JsonAssert.when(Option.IGNORING_ARRAY_ORDER));
    }

    private static final String COMPILATION_PREREQUISITE_CODE = "###Connection\n" +
            "RelationalDatabaseConnection meta::dataquality::H2\n" +
            "{\n" +
            "  store: meta::dataquality::db;\n" +
            "  type: H2;\n" +
            "  specification: LocalH2\n" +
            "  { \n" +
            "    testDataSetupSqls: [];\n" +
            "  };\n" +
            "  auth: DefaultH2;\n" +
            "}\n" +
            "\n" +
            "###Relational\n" +
            "Database meta::dataquality::db\n" +
            "(\n" +
            "   Table personTable (ID INT PRIMARY KEY, FIRSTNAME VARCHAR(200), LASTNAME VARCHAR(200), AGE INT, ADDRESSID INT, FIRMID INT)\n" +
            "   Table addressTable (ID INT PRIMARY KEY, LOCATIONID INT, TYPE INT)\n" +
            "   Table locationTable (ID INT PRIMARY KEY, STREET VARCHAR(200), LOCALITY VARCHAR(200))\n" +
            "\n" +
            "   Join Address_Person(addressTable.ID = personTable.ADDRESSID)\n" +
            "   Join Address_Location(addressTable.LOCATIONID = locationTable.ID)\n" +
            ")\n" +
            "\n" +
            "\n" +
            "###Mapping\n" +
            "Mapping meta::dataquality::dataqualitymappings\n" +
            "(\n" +
            "\n" +
            "   meta::dataquality::Person : Relational\n" +
            "   {\n" +
            "      name : [meta::dataquality::db]personTable.FIRSTNAME,\n" +
            "      age : [meta::dataquality::db]personTable.AGE,\n" +
            "      addresses : [meta::dataquality::db]@Address_Person\n" +
            "   }\n" +
            "\n" +
            "   meta::dataquality::Address : Relational\n" +
            "   {\n" +
            "      addressId : [meta::dataquality::db]addressTable.ID,\n" +
            "      location : [meta::dataquality::db]@Address_Location\n" +
            "   }\n" +
            "\n" +
            "   meta::dataquality::Location : Relational\n" +
            "   {\n" +
            "      street : [meta::dataquality::db]locationTable.STREET,\n" +
            "      locality : [meta::dataquality::db]locationTable.LOCALITY\n" +
            "   }\n" +
            ")\n" +
            "\n" +
            "###Runtime\n" +
            "Runtime meta::dataquality::DataQualityRuntime\n" +
            "{\n" +
            "  mappings:\n" +
            "  [\n" +
            "    meta::dataquality::dataqualitymappings\n" +
            "  ];\n" +
            "  connections:\n" +
            "  [\n" +
            "    meta::dataquality::db:\n" +
            "    [\n" +
            "      connection_1: meta::dataquality::H2\n" +
            "    ]\n" +
            "  ];\n" +
            "}\n" +
            "\n" +
            "\n" +
            "\n" +
            "###Pure\n" +
            "Class meta::dataquality::Person\n" +
            "[\n" +
            "  mustBeOfLegalAge: $this.age >= 18,\n" +
            "  validNameLength: $this.name->length() < 1000,\n" +
            "  ageMustBePositive: $this.age >= 0\n" +
            "]\n" +
            "{\n" +
            "   name : String[1];\n" +
            "   age : Integer[1];\n" +
            "   addresses : meta::dataquality::Address[*];\n" +
            "}\n" +
            "\n" +
            "Class meta::dataquality::Address\n" +
            "[\n" +
            "  validAddressId: $this.addressId->isNotEmpty()\n" +
            "]\n" +
            "{\n" +
            "   location: meta::dataquality::Location[1];\n" +
            "   locationStreet: String[1];\n" +
            "   addressId: String[1];\n" +
            "}\n" +
            "\n" +
            "Class meta::dataquality::Location\n" +
            "{\n" +
            "   street: String[1];\n" +
            "   locality: String[1];\n" +
            "}\n";

}
