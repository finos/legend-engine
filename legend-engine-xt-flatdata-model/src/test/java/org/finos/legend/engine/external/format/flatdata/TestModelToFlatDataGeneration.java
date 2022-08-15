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

import org.finos.legend.engine.external.format.flatdata.fromModel.ModelToFlatDataConfiguration;
import org.finos.legend.engine.external.shared.format.model.test.ModelToSchemaGenerationTest;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.externalFormat.Binding;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.externalFormat.ExternalFormatSchemaSet;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

public class TestModelToFlatDataGeneration extends ModelToSchemaGenerationTest
{
    @Test
    public void testSimpleCsv()
    {
        String modelCode = "Class test::gen::Data\n" +
                "{\n" +
                "  name        : String[1];\n" +
                "  employed    : Boolean[0..1];\n" +
                "  iq          : Integer[0..1];\n" +
                "  weightKg    : Float[0..1];\n" +
                "  heightM     : Decimal[1];\n" +
                "  dateOfBirth : StrictDate[1];\n" +
                "  timeOfDeath : DateTime[1];\n" +
                "}";

        PureModelContextData generated = generateSchema(modelCode, config("test::gen"));
        Binding binding = generated.getElementsOfType(Binding.class).stream().findFirst().get();
        Assert.assertEquals("test::gen::TestBinding", binding.getPath());
        Assert.assertEquals("test::gen::TestSchemaSet", binding.schemaSet);
        Assert.assertEquals(Collections.singletonList("test::gen::Data"), binding.modelUnit.packageableElementIncludes);

        ExternalFormatSchemaSet schemaSet = generated.getElementsOfType(ExternalFormatSchemaSet.class).stream().findFirst().get();
        String expectedDefiniiton = "section Data: DelimitedWithHeadings\n" +
                "{\n" +
                "  scope.untilEof;\n" +
                "  delimiter: ',';\n" +
                "\n" +
                "  Record\n" +
                "  {\n" +
                "    name: STRING;\n" +
                "    employed: BOOLEAN(optional);\n" +
                "    iq: INTEGER(optional);\n" +
                "    weightKg: DECIMAL(optional);\n" +
                "    heightM: DECIMAL;\n" +
                "    dateOfBirth: DATE;\n" +
                "    timeOfDeath: DATETIME;\n" +
                "  }\n" +
                "}";
        Assert.assertEquals(expectedDefiniiton, schemaSet.schemas.get(0).content);
    }

    private ModelToFlatDataConfiguration config(String targetPackage)
    {
        ModelToFlatDataConfiguration config = new ModelToFlatDataConfiguration();
        config.targetBinding = targetPackage + "::TestBinding";
        config.targetSchemaSet = targetPackage + "::TestSchemaSet";
        config.sourceModel.add("test::gen::Data");
        config.format = "FlatData";
        return config;
    }
}