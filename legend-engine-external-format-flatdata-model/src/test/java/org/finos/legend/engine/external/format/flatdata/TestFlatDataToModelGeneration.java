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

        String expected = ">>>test::gen::data\n" +
                "Class test::gen::data extends meta::pure::metamodel::type::Any\n" +
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

        String expected = ">>>test::gen::Data\n" +
                "Class test::gen::Data extends meta::pure::metamodel::type::Any\n" +
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

    private FlatDataToModelConfiguration config(String sourceSchemaSet, String targetPackage, boolean purify)
    {
        FlatDataToModelConfiguration config = new FlatDataToModelConfiguration();
        config.sourceSchemaSet = sourceSchemaSet;
        config.targetBinding = targetPackage + "::TestBinding";
        config.targetPackage = targetPackage;
        config.purifyNames = purify;
        return config;
    }
}
