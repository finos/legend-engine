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

package org.finos.legend.engine.external.format.daml.fromModel;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.external.shared.format.model.transformation.fromModel.ModelToSchemaGenerationTest;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.ModelUnit;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.externalFormat.ExternalFormatSchemaSet;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

public class TestModelToDamlSchemaGeneration extends ModelToSchemaGenerationTest
{
    @Test
    public void testSimpleDaml()
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
        PureModelContextData generated = generateSchema(modelCode, modelUnit, config("test::gen"));

        Assert.assertEquals(3, generated.getElements().size());

        ExternalFormatSchemaSet schemaSet = generated.getElementsOfType(ExternalFormatSchemaSet.class).stream().findFirst().get();
        String expectedDefinition = "module Test.Gen where\n" +
                "\n" +
                "data Data = Data with\n" +
                "  name : Text\n" +
                "  employed : Optional Bool\n" +
                "  iq : Optional Int\n" +
                "  weightKg : Optional Decimal\n" +
                "  heightM : Decimal\n" +
                "  dateOfBirth : Date\n" +
                "  timeOfDeath : Time\n" +
                "    deriving (Eq, Ord, Show)\n";
        Assert.assertEquals(expectedDefinition, schemaSet.schemas.get(0).content);
    }


    @Test
    public void testNestedModel()
    {
        String modelCode = "Enum test::Simple::AddressType\n" +
                "{\n" +
                "  HOME,\n" +
                "  OFFICE,\n" +
                "  WORKSHOP\n" +
                "}\n" +
                "\n" +
                "Class test::Simple::Person\n" +
                "{\n" +
                "  firstName: String[1];\n" +
                "  lastName: String[1];\n" +
                "  middleName: String[0..1];\n" +
                "  age: Integer[0..1];\n" +
                "  addresses: test::Simple::Address[*];\n" +
                "  firm: test::Simple::Firm[1];\n" +
                "}\n" +
                "\n" +
                "Class test::Simple::Address\n" +
                "{\n" +
                "  addressType: test::Simple::AddressType[1];\n" +
                "  addressLine1: String[1];\n" +
                "  addressLine2: String[0..1];\n" +
                "  addressLine3: String[0..1];\n" +
                "}\n" +
                "\n" +
                "Class test::Simple::Firm\n" +
                "{\n" +
                "  legalName: String[1];\n" +
                "  addresses: test::Simple::Address[*];\n" +
                "}\n";

        ModelUnit modelUnit = new ModelUnit();
        modelUnit.packageableElementIncludes = Lists.mutable.with("test::Simple::Person", "test::Simple::Firm", "test::Simple::Address", "test::Simple::AddressType");
        PureModelContextData generated = generateSchema(modelCode, modelUnit, config("test::gen"));

        ExternalFormatSchemaSet schemaSet = generated.getElementsOfType(ExternalFormatSchemaSet.class).stream().findFirst().get();
        String expectedDefinition = "module Test.Simple where\n" +
                "\n" +
                "data Person = Person with\n" +
                "  firstName : Text\n" +
                "  lastName : Text\n" +
                "  middleName : Optional Text\n" +
                "  age : Optional Int\n" +
                "  addresses : Optional [Address]\n" +
                "  firm : Firm\n" +
                "    deriving (Eq, Ord, Show)\n" +
                "\n" +
                "data Address = Address with\n" +
                "  addressType : AddressType\n" +
                "  addressLine1 : Text\n" +
                "  addressLine2 : Optional Text\n" +
                "  addressLine3 : Optional Text\n" +
                "    deriving (Eq, Ord, Show)\n" +
                "\n" +
                "data AddressType\n" +
                "  = HOME\n" +
                "  | OFFICE\n" +
                "  | WORKSHOP\n" +
                "    deriving (Eq, Ord, Show)\n" +
                "\n" +
                "data Firm = Firm with\n" +
                "  legalName : Text\n" +
                "  addresses : Optional [Address]\n" +
                "    deriving (Eq, Ord, Show)\n";
        Assert.assertEquals(expectedDefinition, schemaSet.schemas.get(0).content);
    }

    private ModelToDamlConfiguration config(String targetPackage)
    {
        ModelToDamlConfiguration config = new ModelToDamlConfiguration();
        config.targetSchemaSet = targetPackage + "::TestSchemaSet";
        config.format = "DAML";
        return config;
    }
}

