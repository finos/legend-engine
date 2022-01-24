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

package org.finos.legend.engine.external.format.flatdata;

import org.finos.legend.engine.external.format.flatdata.toModel.FlatDataToModelConfiguration;
import org.finos.legend.engine.external.shared.format.model.test.SchemaToModelGenerationTest;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.junit.Assert;
import org.junit.Test;

public class TestFlatDataToModelGeneration extends SchemaToModelGenerationTest
{
    @Test
    public void testSimpleCsv()
    {
        String schemaCode = newExternalSchemaSetGrammarBuilder("test::Simple", "FlatData")
                .withSchemaText("section data: DelimitedWithHeadings\n" +
                                        "{\n" +
                                        "  scope.untilEof;\n" +
                                        "  delimiter: ',';\n" +
                                        "\n" +
                                        "  Record\n" +
                                        "  {\n" +
                                        "    Name            : STRING;\n" +
                                        "    Employed        : INTEGER(optional);\n" +
                                        "    IQ              : INTEGER(optional);\n" +
                                        "    'Weight KG'     : DECIMAL(optional);\n" +
                                        "    'DATE OF BIRTH' : DATE;\n" +
                                        "    TIME_OF_DEATH   : DATETIME;\n" +
                                        "  }\n" +
                                        "}")
                .build();

        PureModelContextData model = generateModel(schemaCode, config("test::Simple", "test::gen", false));

        String expected = ">>>test::gen::dataRecord\n" +
                "Class test::gen::dataRecord extends meta::pure::metamodel::type::Any\n" +
                "{\n" +
                "  Name: String[1];\n" +
                "  Employed: Integer[0..1];\n" +
                "  IQ: Integer[0..1];\n" +
                "  'Weight KG': Float[0..1];\n" +
                "  'DATE OF BIRTH': StrictDate[1];\n" +
                "  TIME_OF_DEATH: DateTime[1];\n" +
                "}";
        Assert.assertEquals(modelTextsFromString(expected), modelTextsFromContextData(model));
    }

    @Test
    public void testSimpleCsvWithPurifiedNames()
    {
        String schemaCode = newExternalSchemaSetGrammarBuilder("test::Simple", "FlatData")
                .withSchemaText("section data: DelimitedWithHeadings\n" +
                                        "{\n" +
                                        "  scope.untilEof;\n" +
                                        "  delimiter: ',';\n" +
                                        "\n" +
                                        "  Record\n" +
                                        "  {\n" +
                                        "    Name            : STRING;\n" +
                                        "    Employed        : INTEGER(optional);\n" +
                                        "    IQ              : INTEGER(optional);\n" +
                                        "    'Weight KG'     : DECIMAL(optional);\n" +
                                        "    'DATE OF BIRTH' : DATE;\n" +
                                        "    TIME_OF_DEATH   : DATETIME;\n" +
                                        "  }\n" +
                                        "}")
                .build();

        PureModelContextData model = generateModel(schemaCode, config("test::Simple", "test::gen", true));

        String expected = ">>>test::gen::DataRecord\n" +
                "Class test::gen::DataRecord extends meta::pure::metamodel::type::Any\n" +
                "{\n" +
                "  name: String[1];\n" +
                "  employed: Integer[0..1];\n" +
                "  iq: Integer[0..1];\n" +
                "  weightKg: Float[0..1];\n" +
                "  dateOfBirth: StrictDate[1];\n" +
                "  timeOfDeath: DateTime[1];\n" +
                "}";
        Assert.assertEquals(modelTextsFromString(expected), modelTextsFromContextData(model));
    }

    @Test
    public void testMultiSectionCsv()
    {
        String schemaCode = newExternalSchemaSetGrammarBuilder("test::Simple", "FlatData")
                .withSchemaText("section header: DelimitedWithoutHeadings\n" +
                                        "{\n" +
                                        "  scope.forNumberOfLines: 1;\n" +
                                        "  delimiter: ',';\n" +
                                        "\n" +
                                        "  Record\n" +
                                        "  {\n" +
                                        "    Nationality {1} : STRING;\n" +
                                        "  }\n" +
                                        "}" +
                                        "section data: DelimitedWithHeadings\n" +
                                        "{\n" +
                                        "  scope.untilEof;\n" +
                                        "  delimiter: ',';\n" +
                                        "\n" +
                                        "  Record\n" +
                                        "  {\n" +
                                        "    Name            : STRING;\n" +
                                        "    Employed        : INTEGER(optional);\n" +
                                        "    IQ              : INTEGER(optional);\n" +
                                        "    'Weight KG'     : DECIMAL(optional);\n" +
                                        "    'DATE OF BIRTH' : DATE;\n" +
                                        "    TIME_OF_DEATH   : DATETIME;\n" +
                                        "  }\n" +
                                        "}")
                .build();

        PureModelContextData model = generateModel(schemaCode, config("test::Simple", "test::gen", true, "PeopleSet"));

        String expected = ">>>test::gen::DataRecord\n" +
                "Class test::gen::DataRecord extends meta::pure::metamodel::type::Any\n" +
                "{\n" +
                "  name: String[1];\n" +
                "  employed: Integer[0..1];\n" +
                "  iq: Integer[0..1];\n" +
                "  weightKg: Float[0..1];\n" +
                "  dateOfBirth: StrictDate[1];\n" +
                "  timeOfDeath: DateTime[1];\n" +
                "}\n" +
                "\n" +
                ">>>test::gen::HeaderRecord\n" +
                "Class test::gen::HeaderRecord extends meta::pure::metamodel::type::Any\n" +
                "{\n" +
                "  nationality: String[1];\n" +
                "}\n" +
                "\n" +
                ">>>test::gen::PeopleSet\n" +
                "Class test::gen::PeopleSet extends meta::pure::metamodel::type::Any\n" +
                "{\n" +
                "}\n" +
                "\n" +
                ">>>test::gen::PeopleSet_DataRecord\n" +
                "Association test::gen::PeopleSet_DataRecord\n" +
                "{\n" +
                "  peopleSet: test::gen::PeopleSet[1];\n" +
                "  data: test::gen::DataRecord[*];\n" +
                "}\n" +
                "\n" +
                ">>>test::gen::PeopleSet_HeaderRecord\n" +
                "Association test::gen::PeopleSet_HeaderRecord\n" +
                "{\n" +
                "  peopleSet: test::gen::PeopleSet[1];\n" +
                "  header: test::gen::HeaderRecord[1];\n" +
                "}";
        Assert.assertEquals(modelTextsFromString(expected), modelTextsFromContextData(model));
    }

    private FlatDataToModelConfiguration config(String sourceSchemaSet, String targetPackage, boolean purify)
    {
        return config(sourceSchemaSet, targetPackage, purify, null);
    }

    private FlatDataToModelConfiguration config(String sourceSchemaSet, String targetPackage, boolean purify, String schemaClassName)
    {
        FlatDataToModelConfiguration config = new FlatDataToModelConfiguration();
        config.format = "FlatData";
        config.sourceSchemaSet = sourceSchemaSet;
        config.targetBinding = targetPackage + "::TestBinding";
        config.targetPackage = targetPackage;
        config.purifyNames = purify;
        config.schemaClassName = schemaClassName;
        return config;
    }
}
