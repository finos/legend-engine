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

package org.finos.legend.engine.language.pure.grammar.test.roundtrip;

import org.finos.legend.engine.language.pure.grammar.test.TestGrammarRoundtrip;
import org.junit.Test;

public class TestMappingGrammarRoundtrip extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{
    @Test
    public void testSimpleModelMapping()
    {
        test("###Mapping\n" +
                "Mapping meta::pure::mapping::modelToModel::test::simple::simpleModelMapping\n" +
                "(\n" +
                "  *meta::pure::mapping::modelToModel::test::shared::dest::Person[meta_pure_mapping_modelToModel_test_shared_dest_Person]: Pure\n" +
                "  {\n" +
                "    ~src meta::pure::mapping::modelToModel::test::shared::src::_S_Person\n" +
                "    firstName: $src.fullName->substring(0, $src.fullName->indexOf(' ')),\n" +
                "    lastName: $src.fullName->substring($src.fullName->indexOf(' ') + 1, $src.fullName->length()),\n" +
                "    testing: if($src.fullName == 'johndoe', |if($src.lastName == 'good', |'true', |'maybe'), |'false')\n" +
                "  }\n" +
                "  *meta::pure::mapping::modelToModel::test::shared::dest::Product2Simple[meta_pure_mapping_modelToModel_test_shared_dest_Product2Simple]: Pure\n" +
                "  {\n" +
                "    ~src meta::pure::mapping::modelToModel::test::shared::src::_Product2\n" +
                "    ~filter if($src.fullName == 'johndoe', |if($src.lastName == 'good', |true, |true), |false)\n" +
                "    name: $src.name,\n" +
                "    region: $src.region\n" +
                "  }\n" +
                ")\n");
    }

    @Test
    public void testMappingWithTests()
    {
        test("###Mapping\n" +
                "Mapping meta::pure::mapping::modelToModel::test::simple::simpleModelMapping\n" +
                "(\n" +
                "  *meta::pure::mapping::modelToModel::test::shared::dest::Person[meta_pure_mapping_modelToModel_test_shared_dest_Person]: Pure\n" +
                "  {\n" +
                "    ~src meta::pure::mapping::modelToModel::test::shared::src::_S_Person\n" +
                "    firstName: $src.fullName->substring(0, $src.fullName->indexOf(' ')),\n" +
                "    lastName: $src.fullName->substring($src.fullName->indexOf(' ') + 1, $src.fullName->length()),\n" +
                "    testing: if($src.fullName == 'johndoe', |if($src.lastName == 'good', |'true', |'maybe'), |'false')\n" +
                "  }\n" +
                "\n" +
                "  MappingTests\n" +
                "  [\n" +
                "    test1\n" +
                "    (\n" +
                "      query: src: meta::slang::transform::tests::Address[1]|$src.a;\n" +
                "      data:\n" +
                "      [\n" +
                "      ];\n" +
                "      assert: 'assertString';\n" +
                "    ),\n" +
                "    test2\n" +
                "    (\n" +
                "      query: |model::domain::Target.all()->graphFetchChecked(#{model::domain::Target{name}}#)->serialize(#{model::domain::Target{name}}#);\n" +
                "      data:\n" +
                "      [\n" +
                "        <Object, JSON, model::domain::Source, '{\"oneName\":\"oneName 2\",\"anotherName\":\"anotherName 16\",\"oneDate\":\"2020-02-05\",\"anotherDate\":\"2020-04-13\",\"oneNumber\":24,\"anotherNumber\":29}'>,\n" +
                "        <Object, XML, SourceClass, '{\"defects\":[],\"value\":{\"name\":\"oneName 99\"},\"source\":{\"defects\":[],\"value\":{\"oneName\":\"oneName 99\"},\"source\":{\"number\":1,\"record\":\"{\\\"oneName\\\":\\\"oneName 99\\\",\\\"anotherName\\\":\\\"{\\\\\"anotherOne\\\\\":\\\\\"movie\\\\\"}\\\"}\"}}}'>\n" +
                "      ];\n" +
                // This is a very deeply nested string that represents a JSON, we must be extremely careful with (un)escaping this during serialization or parsing
                "      assert: '{\"defects\":[],\"value\":{\"name\":\"oneName 99\"},\"source\":{\"defects\":[],\"value\":{\"oneName\":\"oneName 99\"},\"source\":{\"number\":1,\"record\":\"{\\\"oneName\\\":\\\"oneName 99\\\",\\\"anotherName\\\":\\\"{\\\\\"anotherOne\\\\\":\\\\\"movie\\\\\"}\\\"}\"}}}';\n" +
                "    )\n" +
                "  ]\n" +
                ")\n");
    }

    @Test
    public void testEnumerationMapping()
    {
        test("###Mapping\n" +
                "Mapping meta::pure::mapping::modelToModel::test::enumerationMapping::enumToEnum::mapping::enumerationMapping\n" +
                "(\n" +
                "  meta::pure::mapping::modelToModel::test::enumerationMapping::enumToEnum::model::TargetProductType: EnumerationMapping TargetProductTypeMapping\n" +
                "  {\n" +
                "    TP1: ['MP1'],\n" +
                "    TP2: ['MP2', 'MP3']\n" +
                "  }\n" +
                "  meta::pure::mapping::modelToModel::test::enumerationMapping::enumToEnum::model::TargetProductType: EnumerationMapping TargetProductTypeMapping2\n" +
                "  {\n" +
                "    TP3: ['MP21'],\n" +
                "    TP4: ['MP22', 'MP23']\n" +
                "  }\n" +
                "  meta::pure::mapping::modelToModel::test::enumerationMapping::enumToEnum::model::TargetTradeType: EnumerationMapping TargetTradeTypeMapping\n" +
                "  {\n" +
                "    TT1: [Enum.MT1],\n" +
                "    TT2: [Enum.MT2, Enum.MT3]\n" +
                "  }\n" +
                "  meta::pure::mapping::modelToModel::test::enumerationMapping::enumToEnum::model::TargetTradeType: EnumerationMapping TargetTradeTypeMapping2\n" +
                "  {\n" +
                "    TT3: [My::Enum.MT21],\n" +
                "    TT4: [My::Enum.MT22, My::Enum.MT23]\n" +
                "  }\n" +
                "  meta::pure::mapping::modelToModel::test::enumerationMapping::enumToEnum::model::MiddleProductType: EnumerationMapping MiddleProductTypeMapping\n" +
                "  {\n" +
                "    MP1: [10],\n" +
                "    MP2: [20],\n" +
                "    MP3: [30, 40]\n" +
                "  }\n" +
                "  meta::pure::mapping::modelToModel::test::enumerationMapping::enumToEnum::model::MiddleTradeType: EnumerationMapping MiddleTradeTypeMapping\n" +
                "  {\n" +
                "    MT1: [100],\n" +
                "    MT2: [200],\n" +
                "    MT3: [300, 400]\n" +
                "  }\n" +
                "  meta::pure::mapping::modelToModel::test::enumerationMapping::enumToEnum::model::MiddleProductType2: EnumerationMapping MiddleProductType2Mapping\n" +
                "  {\n" +
                "    MP21: [10],\n" +
                "    MP22: [20],\n" +
                "    MP23: [30, 40]\n" +
                "  }\n" +
                "  meta::pure::mapping::modelToModel::test::enumerationMapping::enumToEnum::model::MiddleTradeType2: EnumerationMapping MiddleTradeType2Mapping\n" +
                "  {\n" +
                "    MT21: [100],\n" +
                "    MT22: [200],\n" +
                "    MT23: [300, 400]\n" +
                "  }\n" +
                ")\n");
    }

    @Test
    public void testEnumerationMappingId()
    {
        test("###Mapping\n" +
                "Mapping ui::meta::something::tests::mapping::enumeration::model::mapping::employeeTestMapping\n" +
                "(\n" +
                "  *ui::meta::something::tests::mapping::enumeration::model::domain::Employee[ui_meta_something_tests_mapping_enumeration_model_domain_Employee]: Pure\n" +
                "  {\n" +
                "    ~src ui::meta::something::tests::mapping::enumeration::model::domain::EmployeeSource\n" +
                "    type: EnumerationMapping Foo: $src.id->toString(),\n" +
                "    dateOfHire: $src.active\n" +
                "  }\n" +
                "\n" +
                "  ui::meta::something::tests::mapping::enumeration::model::domain::EmployeeType: EnumerationMapping\n" +
                "  {\n" +
                "    CONTRACT: ['FTC', 'FTO'],\n" +
                "    FULL_TIME: ['A']\n" +
                "  }\n" +
                "  ui::meta::something::tests::mapping::enumeration::model::domain::YesNo: EnumerationMapping YesNoEnumerationMapping\n" +
                "  {\n" +
                "    YES: [1],\n" +
                "    NO: [0]\n" +
                "  }\n" +
                ")\n");
    }

    @Test
    public void testUnionModelMapping()
    {
        test("###Mapping\n" +
                "Mapping meta::pure::mapping::modelToModel::test::union::unionModelMapping\n" +
                "(\n" +
                "  *meta::pure::mapping::modelToModel::test::shared::dest::Person[meta_pure_mapping_modelToModel_test_shared_dest_Person]: Operation\n" +
                "  {\n" +
                "    meta::pure::router::operations::union_OperationSetImplementation_1__SetImplementation_MANY_(p1,p2)\n" +
                "  }\n" +
                "  *meta::pure::mapping::modelToModel::test::shared::dest::Firm[meta_pure_mapping_modelToModel_test_shared_dest_Firm]: Operation\n" +
                "  {\n" +
                "    meta::pure::router::operations::union_OperationSetImplementation_1__SetImplementation_MANY_(f1,f2)\n" +
                "  }\n" +
                "  meta::pure::mapping::modelToModel::test::shared::dest::Firm[f1]: Pure\n" +
                "  {\n" +
                "    ~src meta::pure::mapping::modelToModel::test::shared::src::_Firm\n" +
                "    legalName: 'f1 / ' + $src.name\n" +
                "  }\n" +
                "  meta::pure::mapping::modelToModel::test::shared::dest::Firm[f2]: Pure\n" +
                "  {\n" +
                "    ~src meta::pure::mapping::modelToModel::test::shared::src::_Firm\n" +
                "    legalName: 'f2 / ' + $src.name\n" +
                "  }\n" +
                "  *meta::pure::mapping::modelToModel::test::shared::dest::Address[meta_pure_mapping_modelToModel_test_shared_dest_Address]: Pure\n" +
                "  {\n" +
                "    street: 'streetConstant'\n" +
                "  }\n" +
                "  meta::pure::mapping::modelToModel::test::shared::dest::Person[p1]: Pure\n" +
                "  {\n" +
                "    ~src meta::pure::mapping::modelToModel::test::shared::src::_Person\n" +
                "    ~filter $src.fullName->startsWith('Johny')\n" +
                "    firstName: $src.fullName->substring(0, $src.fullName->indexOf(' ')),\n" +
                "    lastName: $src.fullName->substring($src.fullName->indexOf(' ') + 1, $src.fullName->length()),\n" +
                "    addresses[meta_pure_mapping_modelToModel_test_shared_dest_Address]: $src.addresses,\n" +
                "    firm[f1]: $src.firm\n" +
                "  }\n" +
                "  meta::pure::mapping::modelToModel::test::shared::dest::Person[p2]: Pure\n" +
                "  {\n" +
                "    ~src meta::pure::mapping::modelToModel::test::shared::src::_Person\n" +
                "    ~filter $src.fullName->startsWith('_')\n" +
                "    firstName: 'N/A',\n" +
                "    lastName: 'N/A',\n" +
                "    firm[f2]: $src.firm\n" +
                "  }\n" +
                ")\n");
    }

    @Test
    public void testMappingWithImport()
    {
        test("Class anything::goes\n" +
                "{\n" +
                "  name: String[*];\n" +
                "}\n" +
                "\n" +
                "Enum anything::else::goes\n" +
                "{\n" +
                "  b\n" +
                "}\n" +
                "\n\n" +
                "###Mapping\n" +
                "import anything::*;\n" +
                "import anything::else::*;\n" +
                "Mapping anything::A\n" +
                "(\n" +
                ")\n");
    }

    @Test
    public void testMappingWithUnitTypeProperties()
    {
        test("###Mapping\n" +
                "Mapping test::decomposeMapping\n" +
                "(\n" +
                "  *test::_Person: Pure\n" +
                "  {\n" +
                "    ~src test::Person\n" +
                "    weightUnit: $src.weight->unitType(),\n" +
                "    weightValue: $src.weight->unitValue()\n" +
                "  }\n" +
                ")\n\n" +
                "Mapping test::composeMapping\n" +
                "(\n" +
                "  *test::Person: Pure\n" +
                "  {\n" +
                "    ~src test::_Person\n" +
                "    weight: test::Mass~Kilogram->newUnit($src.weightValue)->cast(@test::Mass~Kilogram)\n" +
                "  }\n" +
                ")\n\n" +
                "Mapping test::convertMapping\n" +
                "(\n" +
                "  *test::PersonWithPound: Pure\n" +
                "  {\n" +
                "    ~src test::Person\n" +
                "    weight: $src.weight->convert(test::Mass~Pound)->cast(@test::Mass~Pound)\n" +
                "  }\n" +
                ")\n");
    }

    @Test
    public void testModelMappingWithLocalProperties()
    {
        test("###Mapping\n" +
                "Mapping meta::pure::mapping::modelToModel::test::simple::simpleModelMapping\n" +
                "(\n" +
                "  *meta::pure::mapping::modelToModel::test::shared::dest::Person[meta_pure_mapping_modelToModel_test_shared_dest_Person]: Pure\n" +
                "  {\n" +
                "    ~src meta::pure::mapping::modelToModel::test::shared::src::_S_Person\n" +
                "    +prop1: String[1]: $src.fullName->substring(0, $src.fullName->indexOf(' ')),\n" +
                "    firstName: $src.fullName->substring(0, $src.fullName->indexOf(' ')),\n" +
                "    lastName: $src.fullName->substring($src.fullName->indexOf(' ') + 1, $src.fullName->length()),\n" +
                "    testing: if($src.fullName == 'johndoe', |if($src.lastName == 'good', |'true', |'maybe'), |'false')\n" +
                "  }\n" +
                ")\n");

        test("###Mapping\n" +
                "Mapping meta::pure::mapping::modelToModel::test::simple::simpleModelMapping\n" +
                "(\n" +
                "  *meta::pure::mapping::modelToModel::test::shared::dest::Person[meta_pure_mapping_modelToModel_test_shared_dest_Person]: Pure\n" +
                "  {\n" +
                "    ~src meta::pure::mapping::modelToModel::test::shared::src::_S_Person\n" +
                "    +prop1: String[1]: $src.fullName->substring(0, $src.fullName->indexOf(' ')),\n" +
                "    +prop2: Integer[1]: 1,\n" +
                "    +prop3: Float[1]: 1.0,\n" +
                "    +prop4: Boolean[1]: true,\n" +
                "    firstName: $src.fullName->substring(0, $src.fullName->indexOf(' ')),\n" +
                "    lastName: $src.fullName->substring($src.fullName->indexOf(' ') + 1, $src.fullName->length()),\n" +
                "    testing: if($src.fullName == 'johndoe', |if($src.lastName == 'good', |'true', |'maybe'), |'false')\n" +
                "  }\n" +
                ")\n");
    }

    @Test
    public void testCrossStoreAssociationMapping()
    {
        test("###Mapping\n" +
                "Mapping test::crossPropertyMappingWithLocalProperties\n" +
                "(\n" +
                "  test::Person[p]: Pure\n" +
                "  {\n" +
                "    ~src test::Person\n" +
                "    +firmId: Integer[1]: 1,\n" +
                "    name: $src.name\n" +
                "  }\n" +
                "  test::Firm[f]: Pure\n" +
                "  {\n" +
                "    ~src test::Firm\n" +
                "    id: $src.id,\n" +
                "    legalName: $src.legalName\n" +
                "  }\n" +
                "\n" +
                "  test::Firm_Person: XStore\n" +
                "  {\n" +
                "    employer[p, f]: $this.firmId == $that.id,\n" +
                "    employer: $this.firmId == $that.id\n" +
                "  }\n" +
                "  test::Firm_Person[p1]: XStore\n" +
                "  {\n" +
                "    employer[p, f]: $this.firmId == $that.id,\n" +
                "    employer: $this.firmId == $that.id\n" +
                "  }\n" +
                ")\n");
    }
}
