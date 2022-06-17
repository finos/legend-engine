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

package org.finos.legend.engine.external.format.json.specifications;

import org.finos.legend.engine.external.shared.format.model.test.ExternalSchemaCompilationTest;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility;
import org.junit.Test;

public class TestJsonSchemaBindingCompilation extends ExternalSchemaCompilationTest
{
    public void testSimpleJsonSchema(String schemaString)
    {
        test("###Pure\n" +
                "Class test::model::A\n" +
                "{\n" +
                "  name        : String[1];\n" +
                "  employed    : Boolean[0..1];\n" +
                "  iq          : Integer[0..1];\n" +
                "  weightKg    : Float[0..1];\n" +
                "  heightM     : Decimal[1];\n" +
                "  dateOfBirth : StrictDate[1];\n" +
                "  timeOfDeath : DateTime[1];\n" +
                "}\n" +
                "###ExternalFormat\n" +
                jsonSchemaSet(schemaString, "test/model/A.json") +
                "\n" +
                "Binding test::Binding\n" +
                "{\n" +
                "  schemaSet: test::SchemaSet;\n" +
                "  contentType: 'application/json';\n" +
                "  modelIncludes: [ test::model::A ];\n" +
                "}\n"
        );
    }

    public void testVariousMultiplicityPropertiesWithJsonSchema(String schemaString)
    {
        test("###Pure\n" +
                "Class test::gen::Data\n" +
                "{\n" +
                "  floatField: Float[1];\n" +
                "  floatRangeField: Float[1..3];\n" +
                "  stringRangeZeroField: String[0..3];\n" +
                "  decimalRangeZeroField: Float[0..3];\n" +
                "  booleanField: Boolean[1];\n" +
                "  strictDateRangeZeroField: String[0..3];\n" +
                "  dateTimeField: DateTime[1];\n" +
                "  strictDateRangeField: String[1..3];\n" +
                "  integerRangeZeroField: Integer[0..3];\n" +
                "  strictDateField: StrictDate[1];\n" +
                "  strictDateMultipleField: String[*];\n" +
                "  dateTimeRangeField: String[1..3];\n" +
                "  floatRangeZeroField: Float[0..3];\n" +
                "  integerMultipleField: Integer[*];\n" +
                "  decimalField: Float[1];\n" +
                "  decimalRangeField: Float[1..3];\n" +
                "  dateRangeZeroField: String[0..3];\n" +
                "  dateTimeMultipleField: String[*];\n" +
                "  stringRangeField: String[1..3];\n" +
                "  dateField: DateTime[1];\n" +
                "  dateTimeRangeZeroField: String[0..3];\n" +
                "  floatMultipleField: Float[*];\n" +
                "  stringNoDescriptionField: String[1];\n" +
                "  integerRangeField: Integer[1..3];\n" +
                "  srtingMultipleField: String[*];\n" +
                "  dateRangeField: String[1..3];\n" +
                "  integerField: Integer[1];\n" +
                "  decimalMultipleField: Float[*];\n" +
                "  stringField: String[1];\n" +
                "  dateMultipleField: String[*];\n" +
                "}\n" +
                "###ExternalFormat\n" +
                jsonSchemaSet(schemaString, "test/gen/Data.json") +
                "\n" +
                "Binding test::Binding\n" +
                "{\n" +
                "  schemaSet: test::SchemaSet;\n" +
                "  contentType: 'application/json';\n" +
                "  modelIncludes: [ test::gen::Data ];\n" +
                "}\n"
        );
    }

    public void testNestingWithJsonSchema(String schemaString)
    {
        test("###Pure\n" +
                "Class test::Simple::Person\n" +
                "{\n" +
                "  firstName: String[1];\n" +
                "  lastName: String[1];\n" +
                "  middleName: String[0..1];\n" +
                "  age: Integer[0..1];\n" +
                "  firm: test::Simple::Firm[1];\n" +
                "}\n" +
                "\n" +
                "Class test::Simple::Firm\n" +
                "{\n" +
                "  legalName: String[1];\n" +
                "}\n" +
                "###ExternalFormat\n" +
                jsonSchemaSet(schemaString, "test/Simple/Person.json") +
                "\n" +
                "Binding test::Binding\n" +
                "{\n" +
                "  schemaSet: test::SchemaSet;\n" +
                "  contentType: 'application/json';\n" +
                "  modelIncludes: [ test::Simple::Person, test::Simple::Firm ];\n" +
                "}\n"
        );
    }

    public void testMultiLevelNestingWithJsonSchema(String schemaString)
    {
        test("###Pure\n" +
                "Enum test::Simple::AddressType\n" +
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
                "}\n" +
                "###ExternalFormat\n" +
                jsonSchemaSet(schemaString, "test/Simple/Person.json") +
                "\n" +
                "Binding test::Binding\n" +
                "{\n" +
                "  schemaSet: test::SchemaSet;\n" +
                "  contentType: 'application/json';\n" +
                "  modelIncludes: [ test::Simple::Person, test::Simple::Firm, test::Simple::Address, test::Simple::AddressType ];\n" +
                "}\n"
        );
    }

    private String jsonSchemaSet(String jsonSchema, String location)
    {
        return "SchemaSet test::SchemaSet\n" +
                "{\n" +
                "  format: JSON;\n" +
                "  schemas: [ { location: '" + location + "';\n" +
                "               content: " + PureGrammarComposerUtility.convertString(jsonSchema, true) + "; } ];\n" +
                "}\n";
    }
}
