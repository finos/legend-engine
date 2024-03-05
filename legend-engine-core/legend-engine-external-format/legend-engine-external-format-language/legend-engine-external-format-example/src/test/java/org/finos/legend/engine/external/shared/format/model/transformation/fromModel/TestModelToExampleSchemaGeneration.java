//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.external.shared.format.model.transformation.fromModel;

import org.finos.legend.engine.external.shared.format.model.exampleSchema.ModelToExampleSchemaConfiguration;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.ModelUnit;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.externalFormat.Binding;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.externalFormat.ExternalFormatSchemaSet;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

public class TestModelToExampleSchemaGeneration extends ModelToSchemaGenerationTest
{
    @Test
    public void testSchemaGenerationWithoutBinding()
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

        ModelUnit modelUnit = new ModelUnit();
        modelUnit.packageableElementIncludes = Collections.singletonList("test::gen::Data");

        PureModelContextData generated = generateSchema(modelCode, modelUnit, config());
        Assert.assertEquals(3, generated.getElements().size());
        ExternalFormatSchemaSet schemaSet = generated.getElementsOfType(ExternalFormatSchemaSet.class).stream().findFirst().get();
        Assert.assertEquals("test::ExampleSchemaSet", schemaSet.getPath());
    }

    @Test
    public void testSchemaGenerationWithBinding()
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

        ModelUnit modelUnit = new ModelUnit();
        modelUnit.packageableElementIncludes = Collections.singletonList("test::gen::Data");

        PureModelContextData generated = generateSchema(modelCode, modelUnit, config(), true, "test::gen::TestBinding");
        Assert.assertEquals(4, generated.getElements().size());
        ExternalFormatSchemaSet schemaSet = generated.getElementsOfType(ExternalFormatSchemaSet.class).stream().findFirst().get();
        Assert.assertEquals("test::ExampleSchemaSet", schemaSet.getPath());

        Binding binding = generated.getElementsOfType(Binding.class).stream().findFirst().get();
        Assert.assertEquals("test::gen::TestBinding", binding.getPath());
        Assert.assertEquals("test::ExampleSchemaSet", binding.schemaSet);
        Assert.assertEquals(Collections.singletonList("test::gen::Data"), binding.modelUnit.packageableElementIncludes);
    }

    private ModelToExampleSchemaConfiguration config()
    {
        ModelToExampleSchemaConfiguration config = new ModelToExampleSchemaConfiguration();

        config.format = "Example";
        config.targetSchemaSet = "test:ExampleSchemaSet";

        return config;
    }
}
