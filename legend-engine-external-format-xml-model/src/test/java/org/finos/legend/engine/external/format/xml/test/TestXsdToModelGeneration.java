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

package org.finos.legend.engine.external.format.xml.test;

import org.finos.legend.engine.external.format.xsd.toModel.XsdToModelConfiguration;
import org.finos.legend.engine.external.shared.format.model.test.SchemaToModelGenerationTest;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.junit.Assert;
import org.junit.Test;

public class TestXsdToModelGeneration extends SchemaToModelGenerationTest
{
    @Test
    public void testSimpleClass()
    {
        String schemaCode = newExternalSchemaSetGrammarBuilder("test::Simple", "XSD")
                .withSchemaText(null, "simple.xsd", "<?xml version='1.0'?>\n" +
                        "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
                        "    <xs:complexType name=\"Rectangle\">\n" +
                        "        <xs:sequence>\n" +
                        "            <xs:element name=\"height\" type=\"xs:int\" minOccurs=\"1\" maxOccurs=\"unbounded\">\n" +
                        "                <xs:annotation>\n" +
                        "                    <xs:documentation xml:lang=\"en\">One of two dimensions of a rectangle</xs:documentation>\n" +
                        "                </xs:annotation>\n" +
                        "            </xs:element>\n" +
                        "            <xs:element name=\"width\" type=\"xs:int\" minOccurs=\"0\" maxOccurs=\"unbounded\"/>\n" +
                        "        </xs:sequence>\n" +
                        "    </xs:complexType>\n" +
                        "</xs:schema>\n")
                .build();

        PureModelContextData model = generateModel(schemaCode, config("test::Simple", "test::gen", false, true));

        String expected = ">>>test::gen::Rectangle\n" +
                "Class test::gen::Rectangle\n" +
                "{\n" +
                "  {meta::pure::profiles::doc.doc = 'One of two dimensions of a rectangle'} height: Integer[1..*];\n" +
                "  width: Integer[*];\n" +
                "}";

        Assert.assertEquals(modelTextsFromString(expected), modelTextsFromContextData(model));
    }

    @Test
    public void testAbstractClass()
    {
        String schemaCode = newExternalSchemaSetGrammarBuilder("test::Abstract", "XSD")
                .withSchemaText(null, "abstract.xsd", "<?xml version='1.0'?>\n" +
                        "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
                        "    <xs:complexType name=\"Abstract\" abstract=\"true\">\n" +
                        "    </xs:complexType>\n" +
                        "</xs:schema>")
                .build();

        PureModelContextData model = generateModel(schemaCode, config("test::Abstract", "test::gen", false, true));

        String expected = ">>>test::gen::Abstract\n" +
                "Class <<meta::pure::profiles::typemodifiers.abstract>> test::gen::Abstract\n" +
                "{\n" +
                "}";

        Assert.assertEquals(modelTextsFromString(expected), modelTextsFromContextData(model));
    }

    @Test
    public void testAttributes()
    {
        String schemaCode = newExternalSchemaSetGrammarBuilder("test::Attributes", "XSD")
                .withSchemaText(null, "attributes.xsd", "<?xml version='1.0' encoding='utf-8'?>\n" +
                        "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
                        "  <xs:complexType name=\"LinkId\">\n" +
                        "    <xs:annotation>\n" +
                        "      <xs:documentation xml:lang=\"en\">The data type used for link identifiers.</xs:documentation>\n" +
                        "    </xs:annotation>\n" +
                        "    <xs:simpleContent>\n" +
                        "      <xs:extension base=\"Scheme\">\n" +
                        "        <xs:attribute name=\"id\" type=\"xs:ID\"/>\n" +
                        "        <xs:attribute name=\"linkIdScheme\" type=\"xs:anyURI\" use=\"required\"/>\n" +
                        "      </xs:extension>\n" +
                        "    </xs:simpleContent>\n" +
                        "  </xs:complexType>\n" +
                        "  <xs:simpleType name=\"Scheme\">\n" +
                        "    <xs:annotation>\n" +
                        "      <xs:documentation xml:lang=\"en\">The base class for all types which define coding schemes.</xs:documentation>\n" +
                        "    </xs:annotation>\n" +
                        "    <xs:restriction base=\"xs:normalizedString\">\n" +
                        "      <xs:maxLength value=\"255\"/>\n" +
                        "    </xs:restriction>\n" +
                        "  </xs:simpleType>\n" +
                        "</xs:schema>\n")
                .build();

        PureModelContextData model = generateModel(schemaCode, config("test::Attributes", "test::gen", false, true));

        String expected = ">>>test::gen::LinkId\n" +
                "Class {meta::pure::profiles::doc.doc = 'The data type used for link identifiers.'} test::gen::LinkId\n" +
                "[\n" +
                "  c1_length: $this.value->length() <= 255\n" +
                "]\n" +
                "{\n" +
                "  id: String[0..1];\n" +
                "  linkIdScheme: String[1];\n" +
                "  value: String[1];\n" +
                "}";

        Assert.assertEquals(modelTextsFromString(expected), modelTextsFromContextData(model));
    }

    @Test
    public void testInheritance()
    {
        String schemaCode = newExternalSchemaSetGrammarBuilder("test::Inheritance", "XSD")
                .withSchemaText(null, "inheritance.xsd", "<?xml version='1.0'?>\n" +
                        "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
                        "  <xs:complexType name=\"Rectangle\">\n" +
                        "    <xs:annotation>\n" +
                        "      <xs:documentation xml:lang=\"en\">Class level doc</xs:documentation>\n" +
                        "    </xs:annotation>\n" +
                        "    <xs:sequence>\n" +
                        "      <xs:element name=\"height\" type=\"xs:int\"/>\n" +
                        "      <xs:element name=\"width\" type=\"xs:int\"/>\n" +
                        "    </xs:sequence>\n" +
                        "  </xs:complexType>\n" +
                        "  <xs:complexType name=\"Cuboid\">\n" +
                        "    <xs:complexContent>\n" +
                        "      <xs:extension base=\"Rectangle\">\n" +
                        "        <xs:sequence>\\n' +\n" +
                        "          <xs:element name=\"depth\" type=\"xs:int\"/>\n" +
                        "        </xs:sequence>\n" +
                        "      </xs:extension>\n" +
                        "    </xs:complexContent>\n" +
                        "  </xs:complexType>\n" +
                        "</xs:schema>\n")
                .build();

        PureModelContextData model = generateModel(schemaCode, config("test::Inheritance", "test::gen", false, true));

        String expected = ">>>test::gen::Rectangle\n" +
                "Class {meta::pure::profiles::doc.doc = 'Class level doc'} test::gen::Rectangle\n" +
                "{\n" +
                "  height: Integer[1];\n" +
                "  width: Integer[1];\n" +
                "}\n" +
                "\n" +
                ">>>test::gen::Cuboid\n" +
                "Class test::gen::Cuboid extends test::gen::Rectangle\n" +
                "{\n" +
                "  depth: Integer[1];\n" +
                "}\n";

        Assert.assertEquals(modelTextsFromString(expected), modelTextsFromContextData(model));
    }

    @Test
    public void testNestedSequences()
    {
        String schemaCode = newExternalSchemaSetGrammarBuilder("test::NestedSequences", "XSD")
                .withSchemaText(null, "nested-sequences.xsd", "<?xml version='1.0'?>\n" +
                        "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\\n' +\n" +
                        "  <xs:complexType name=\"NestedSequences\">\n" +
                        "    <xs:sequence>\n" +
                        "      <xs:element name=\"one\" type=\"xs:string\"/>\n" +
                        "      <xs:sequence>\n" +
                        "        <xs:element name=\"two\" type=\"xs:string\"/>\n" +
                        "        <xs:sequence>\n" +
                        "          <xs:element name=\"three\" type=\"xs:string\"/>\n" +
                        "        </xs:sequence>\n" +
                        "      </xs:sequence>\n" +
                        "    </xs:sequence>\n" +
                        "  </xs:complexType>\n" +
                        "</xs:schema>\n")
                .build();

        PureModelContextData model = generateModel(schemaCode, config("test::NestedSequences", "test::gen", false, true));

        String expected = ">>>test::gen::NestedSequences\n" +
                "Class test::gen::NestedSequences\n" +
                "{\n" +
                "  one: String[1];\n" +
                "  two: String[1];\n" +
                "  three: String[1];\n" +
                "}\n";

        Assert.assertEquals(modelTextsFromString(expected), modelTextsFromContextData(model));
    }

    @Test
    public void testValidations()
    {
        String schemaCode = newExternalSchemaSetGrammarBuilder("test::Validations", "XSD")
                .withSchemaResource(null, "validations.xsd", "modelGeneration/validations.xsd")
                .build();

        PureModelContextData model = generateModel(schemaCode, config("test::Validations", "test::gen", false, true));

        String expected = ">>>test::gen::ShowValidations\n" +
                "Class test::gen::ShowValidations\n" +
                "[\n" +
                "  c1_length: $this.fixedLengthString->forAll(x: String[1]|$x->length() == 12),\n" +
                "  c2_length: $this.minLengthString->length() >= 5,\n" +
                "  c3_length: $this.maxLengthString->forAll(x: String[1]|$x->length() <= 20),\n" +
                "  c4_length: $this.rangeLengthString->forAll(x: String[1]|$x->length() >= 5),\n" +
                "  c5_length: $this.rangeLengthString->forAll(x: String[1]|$x->length() <= 20),\n" +
                "  c6_range: $this.minValInteger >= 1,\n" +
                "  c7_range: $this.minValFloat > 2.4,\n" +
                "  c8_range: $this.maxValInteger <= 100,\n" +
                "  c9_range: $this.maxValFloat < 10.12,\n" +
                "  c10_range: $this.rangeValInteger > 10,\n" +
                "  c11_range: $this.rangeValInteger <= 100,\n" +
                "  c12_range: $this.rangeValFloat > 2.7,\n" +
                "  c13_range: $this.rangeValFloat < 10.99,\n" +
                "  c14_values: $this.fixedValuesString->in(['AUD', 'USD'])\n" +
                "]\n" +
                "{\n" +
                "  fixedLengthString: String[*];\n" +
                "  minLengthString: String[1];\n" +
                "  maxLengthString: String[0..1];\n" +
                "  rangeLengthString: String[*];\n" +
                "  minValInteger: Integer[1];\n" +
                "  minValFloat: Float[1];\n" +
                "  maxValInteger: Integer[1];\n" +
                "  maxValFloat: Float[1];\n" +
                "  rangeValInteger: Integer[1];\n" +
                "  rangeValFloat: Float[1];\n" +
                "  fixedValuesString: String[1];\n" +
                "}\n";

        Assert.assertEquals(modelTextsFromString(expected), modelTextsFromContextData(model));
    }

    @Test
    public void testChoice()
    {
        String schemaCode = newExternalSchemaSetGrammarBuilder("test::Choice", "XSD")
                .withSchemaText(null, "choice.xsd", "<?xml version='1.0'?>\n" +
                        "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
                        "  <xs:complexType name=\"Choice\">\n" +
                        "    <xs:choice>\n" +
                        "      <xs:element name=\"optionOne\" type=\"xs:int\"/>\n" +
                        "      <xs:element name=\"optionTwo\" type=\"xs:int\"/>\n" +
                        "    </xs:choice>\n" +
                        "  </xs:complexType>\n" +
                        "</xs:schema>\n")
                .build();

        PureModelContextData model = generateModel(schemaCode, config("test::Choice", "test::gen", false, true));

        String expected = ">>>test::gen::Choice\n" +
                "Class test::gen::Choice\n" +
                "[\n" +
                "  c1_choice: ($this.optionOne->isNotEmpty() && $this.optionTwo->isEmpty()) || ($this.optionOne->isEmpty() && $this.optionTwo->isNotEmpty())\n" +
                "]\n" +
                "{\n" +
                "  optionOne: Integer[0..1];\n" +
                "  optionTwo: Integer[0..1];\n" +
                "}\n";

        Assert.assertEquals(modelTextsFromString(expected), modelTextsFromContextData(model));
    }

    @Test
    public void testChoiceWithCommonProperty()
    {
        String schemaCode = newExternalSchemaSetGrammarBuilder("test::ChoiceWithCommonProperty", "XSD")
                .withSchemaText(null, "choice-with-common-property.xsd", "<?xml version='1.0'?>\n" +
                        "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
                        "  <xs:complexType name=\"Account\">\n" +
                        "    <xs:sequence>\n" +
                        "      <xs:element name=\"accountId\" type=\"xs:token\"/>\n" +
                        "      <xs:choice>\n" +
                        "        <xs:sequence>\n" +
                        "          <xs:element name=\"beneficiaryId\" type=\"xs:int\"/>\n" +
                        "          <xs:element name=\"servicerId\" type=\"xs:int\" minOccurs=\"0\"/>\n" +
                        "        </xs:sequence>\n" +
                        "        <xs:sequence>\n" +
                        "          <xs:element name=\"servicerId\" type=\"xs:int\"/>\n" +
                        "        </xs:sequence>\n" +
                        "      </xs:choice>\n" +
                        "    </xs:sequence>\n" +
                        "  </xs:complexType>\n" +
                        "</xs:schema>\n")
                .build();

        PureModelContextData model = generateModel(schemaCode, config("test::ChoiceWithCommonProperty", "test::gen", false, true));

        String expected = ">>>test::gen::Account\n" +
                "Class test::gen::Account\n" +
                "[\n" +
                "  c1_choice: $this.beneficiaryId->isNotEmpty() || ($this.beneficiaryId->isEmpty() && $this.servicerId->isNotEmpty())\n" +
                "]\n" +
                "{\n" +
                "  accountId: String[1];\n" +
                "  beneficiaryId: Integer[0..1];\n" +
                "  servicerId: Integer[0..1];\n" +
                "}\n";

        Assert.assertEquals(modelTextsFromString(expected), modelTextsFromContextData(model));
    }

    @Test
    public void testElementReference()
    {
        String schemaCode = newExternalSchemaSetGrammarBuilder("test::ElementRef", "XSD")
                .withSchemaText(null, "element-ref.xsd", "<?xml version='1.0'?>\n" +
                        "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
                        "  <xs:complexType name=\"Rectangle\">\n" +
                        "    <xs:sequence>\n" +
                        "      <xs:element ref=\"height\" minOccurs=\"1\" maxOccurs=\"unbounded\">\n" +
                        "      </xs:element>\n" +
                        "      <xs:element name=\"width\" type=\"xs:int\" minOccurs=\"0\" maxOccurs=\"unbounded\"/>\n" +
                        "    </xs:sequence>\n" +
                        "  </xs:complexType>\n" +
                        "  <xs:element name=\"height\" type=\"xs:int\">\n" +
                        "    <xs:annotation>\n" +
                        "      <xs:documentation xml:lang=\"en\">One of two dimensions of a rectangle</xs:documentation>\n" +
                        "    </xs:annotation>\n" +
                        "  </xs:element>\n" +
                        "</xs:schema>\n")
                .build();

        PureModelContextData model = generateModel(schemaCode, config("test::ElementRef", "test::gen", false, true));

        String expected = ">>>test::gen::Rectangle\n" +
                "Class test::gen::Rectangle\n" +
                "{\n" +
                "  {meta::pure::profiles::doc.doc = 'One of two dimensions of a rectangle'} height: Integer[1..*];\n" +
                "  width: Integer[*];\n" +
                "}";

        Assert.assertEquals(modelTextsFromString(expected), modelTextsFromContextData(model));
    }

    @Test
    public void testEnumeration()
    {
        String schemaCode = newExternalSchemaSetGrammarBuilder("test::Enumeration", "XSD")
                .withSchemaText(null, "enumeration.xsd", "<?xml version='1.0'?>\n" +
                        "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" version=\"1.0\" elementFormDefault=\"qualified\">\n" +
                        "  <xs:simpleType name=\"CurrencyEnumeration\">\n" +
                        "    <xs:restriction base=\"xs:string\">\n" +
                        "       <xs:enumeration value=\"AUD\"/><!-- Australian Dollar -->\n" +
                        "       <xs:enumeration value=\"BRL\"/><!-- Brazilian Real -->\n" +
                        "       <xs:enumeration value=\"CAD\"/><!-- Canadian Dollar -->\n" +
                        "       <xs:enumeration value=\"CNY\"/><!-- Chinese Yen -->\n" +
                        "       <xs:enumeration value=\"EUR\"/><!-- Euro -->\n" +
                        "       <xs:enumeration value=\"GBP\"/><!-- British Pound -->\n" +
                        "       <xs:enumeration value=\"INR\"/><!-- Indian Rupee -->\n" +
                        "       <xs:enumeration value=\"JPY\"/><!-- Japanese Yen -->\n" +
                        "       <xs:enumeration value=\"RUR\"/><!-- Russian Rouble -->\n" +
                        "       <xs:enumeration value=\"USD\"/><!-- US Dollar -->\n" +
                        "       <xs:length value=\"3\"/>\n" +
                        "     </xs:restriction>\n" +
                        "  </xs:simpleType>\n" +
                        "</xs:schema>\n")
                .build();

        PureModelContextData model = generateModel(schemaCode, config("test::Enumeration", "test::gen", false, true));

        String expected = ">>>test::gen::CurrencyEnumeration\n" +
                "Enum test::gen::CurrencyEnumeration\n" +
                "{\n" +
                "  AUD,\n" +
                "  BRL,\n" +
                "  CAD,\n" +
                "  CNY,\n" +
                "  EUR,\n" +
                "  GBP,\n" +
                "  INR,\n" +
                "  JPY,\n" +
                "  RUR,\n" +
                "  USD\n" +
                "}\n";

        Assert.assertEquals(modelTextsFromString(expected), modelTextsFromContextData(model));
    }

    @Test
    public void testGroup()
    {
        String schemaCode = newExternalSchemaSetGrammarBuilder("test::Group", "XSD")
                .withSchemaText(null, "group.xsd", "<?xml version='1.0'?>\n" +
                        "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
                        "  <xs:group name=\"custGroup\">\n" +
                        "    <xs:sequence>\n" +
                        "      <xs:element name=\"customer\" type=\"xs:string\"/>\n" +
                        "      <xs:element name=\"orderdetails\" type=\"xs:string\"/>\n" +
                        "      <xs:element name=\"billto\" type=\"xs:string\"/>\n" +
                        "      <xs:element name=\"shipto\" type=\"xs:string\"/>\n" +
                        "    </xs:sequence>\n" +
                        "  </xs:group>\n" +
                        "  <xs:element name=\"order\" type=\"OrderType\"/>\n" +
                        "  <xs:complexType name=\"OrderType\">\n" +
                        "    <xs:group ref=\"custGroup\"/>\n" +
                        "    <xs:attribute name=\"status\" type=\"xs:string\"/>\n" +
                        "  </xs:complexType>\n" +
                        "</xs:schema>\n")
                .build();

        PureModelContextData model = generateModel(schemaCode, config("test::Group", "test::gen", false, true));

        String expected = ">>>test::gen::OrderType\n" +
                "Class test::gen::OrderType\n" +
                "{\n" +
                "  customer: String[1];\n" +
                "  orderdetails: String[1];\n" +
                "  billto: String[1];\n" +
                "  shipto: String[1];\n" +
                "  status: String[0..1];\n" +
                "}\n";

        Assert.assertEquals(modelTextsFromString(expected), modelTextsFromContextData(model));
    }

    @Test
    public void testAttributeWithInlineType()
    {
        String schemaCode = newExternalSchemaSetGrammarBuilder("test::AttributeWithInlineType", "XSD")
                .withSchemaText(null, "attribute-with-inline-type.xsd", "<?xml version='1.0'?>\n" +
                        "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
                        "  <xs:complexType name=\"AttributeWithInlineType\">\n" +
                        "    <xs:attribute name=\"fullOrDelta\">\n" +
                        "      <xs:simpleType>\n" +
                        "        <xs:restriction base=\"xs:string\">\n" +
                        "          <xs:enumeration value=\"FULL\"/>\n" +
                        "          <xs:enumeration value=\"DELTA\"/>\n" +
                        "        </xs:restriction>\n" +
                        "      </xs:simpleType>\n" +
                        "    </xs:attribute>\n" +
                        "    <xs:attribute name=\"notTooLongString\">\n" +
                        "      <xs:simpleType>' +\n" +
                        "        <xs:restriction base=\"xs:string\">\n" +
                        "          <xs:maxLength value=\"500\"/>\n" +
                        "        </xs:restriction>\n" +
                        "      </xs:simpleType>\n" +
                        "    </xs:attribute>\n" +
                        "  </xs:complexType>\n" +
                        "</xs:schema>\n")
                .build();

        PureModelContextData model = generateModel(schemaCode, config("test::AttributeWithInlineType", "test::gen", false, true));

        String expected = ">>>test::gen::AttributeWithInlineType\n" +
                "Class test::gen::AttributeWithInlineType\n" +
                "[\n" +
                "  c1_values: $this.fullOrDelta->forAll(x: String[1]|$x->in(['FULL', 'DELTA'])),\n" +
                "  c2_length: $this.notTooLongString->forAll(x: String[1]|$x->length() <= 500)\n" +
                "]\n" +
                "{\n" +
                "  fullOrDelta: String[0..1];\n" +
                "  notTooLongString: String[0..1];\n" +
                "}";

        Assert.assertEquals(modelTextsFromString(expected), modelTextsFromContextData(model));
    }

    @Test
    public void testEnumUnion()
    {
        String schemaCode = newExternalSchemaSetGrammarBuilder("test::EnumUnion", "XSD")
                .withSchemaText(null, "enum-union.xsd", "<?xml version='1.0'?>\n" +
                        "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
                        "  <xs:simpleType name=\"DayOfWeekEnum\">\n" +
                        "    <xs:restriction base=\"xs:token\">\n" +
                        "      <xs:enumeration value=\"MON\"/>\n" +
                        "      <xs:enumeration value=\"TUE\"/>\n" +
                        "      <xs:enumeration value=\"WED\"/>\n" +
                        "      <xs:enumeration value=\"THU\"/>\n" +
                        "      <xs:enumeration value=\"FRI\"/>\n" +
                        "      <xs:enumeration value=\"SAT\"/>\n" +
                        "      <xs:enumeration value=\"SUN\"/>\n" +
                        "    </xs:restriction>\n" +
                        "  </xs:simpleType>\n" +
                        "  <xs:simpleType name=\"WeeklyRollConventionEnum\">\n" +
                        "    <xs:union memberTypes=\"DayOfWeekEnum\">\n" +
                        "      <xs:simpleType>\n" +
                        "        <xs:restriction base=\"xs:token\">\n" +
                        "          <xs:enumeration value=\"TBILL\"/>\n" +
                        "        </xs:restriction>\n" +
                        "      </xs:simpleType>\n" +
                        "    </xs:union>\n" +
                        "  </xs:simpleType>\n" +
                        "</xs:schema>\n")
                .build();

        PureModelContextData model = generateModel(schemaCode, config("test::EnumUnion", "test::gen", false, true));

        String expected = ">>>test::gen::WeeklyRollConventionEnum\n" +
                "Enum test::gen::WeeklyRollConventionEnum\n" +
                "{\n" +
                "  MON,\n" +
                "  TUE,\n" +
                "  WED,\n" +
                "  THU,\n" +
                "  FRI,\n" +
                "  SAT,\n" +
                "  SUN,\n" +
                "  TBILL\n" +
                "}\n" +
                "\n" +
                ">>>test::gen::DayOfWeekEnum\n" +
                "Enum test::gen::DayOfWeekEnum\n" +
                "{\n" +
                "  MON,\n" +
                "  TUE,\n" +
                "  WED,\n" +
                "  THU,\n" +
                "  FRI,\n" +
                "  SAT,\n" +
                "  SUN\n" +
                "}\n";

        Assert.assertEquals(modelTextsFromString(expected), modelTextsFromContextData(model));
    }

    @Test
    public void testCircularTypeDependency()
    {
        String schemaCode = newExternalSchemaSetGrammarBuilder("test::CircularTypeDependency", "XSD")
                .withSchemaText(null, "circular-type-dependency.xsd", "<?xml version='1.0'?>\n" +
                        "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
                        "  <xs:complexType name=\"Human\">\n" +
                        "    <xs:sequence>\n" +
                        "      <xs:element name=\"name\" type=\"xs:string\"/>\n" +
                        "    </xs:sequence>\n" +
                        "  </xs:complexType>\n" +
                        "  <xs:complexType name=\"Employee\">\n" +
                        "    <xs:complexContent>\n" +
                        "      <xs:extension base=\"Human\">\n" +
                        "         <xs:sequence>\n" +
                        "           <xs:element name=\"firm\" type=\"Company\"/>\n" +
                        "         </xs:sequence>\n" +
                        "       </xs:extension>\n" +
                        "    </xs:complexContent>\n" +
                        "  </xs:complexType>\n" +
                        "  <xs:complexType name=\"Company\">\n" +
                        "    <xs:sequence>\n" +
                        "      <xs:element name=\"employees\" type=\"Employee\" minOccurs=\"0\" maxOccurs=\"unbounded\"/>\n" +
                        "    </xs:sequence>\n" +
                        "  </xs:complexType>\n" +
                        "</xs:schema>\n")
                .build();

        PureModelContextData model = generateModel(schemaCode, config("test::CircularTypeDependency", "test::gen", false, true));

        String expected = ">>>test::gen::Employee\n" +
                "Class test::gen::Employee extends test::gen::Human\n" +
                "{\n" +
                "  firm: test::gen::Company[1];\n" +
                "}\n" +
                "\n" +
                ">>>test::gen::Human\n" +
                "Class test::gen::Human\n" +
                "{\n" +
                "  name: String[1];\n" +
                "}\n" +
                "\n" +
                ">>>test::gen::Company\n" +
                "Class test::gen::Company\n" +
                "{\n" +
                "  employees: test::gen::Employee[*];\n" +
                "}\n";

        Assert.assertEquals(modelTextsFromString(expected), modelTextsFromContextData(model));
    }

    @Test
    public void testGenerateClassForInlineComplexTypes()
    {
        String schemaCode = newExternalSchemaSetGrammarBuilder("test::GenerateClassForInlineComplexTypes", "XSD")
                .withSchemaText(null, "generate-class-for-inline-complex-types.xsd", "<?xml version='1.0'?>\n" +
                        "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
                        "  <xs:element name=\"person\">\n" +
                        "    <xs:complexType>\n" +
                        "      <xs:sequence>\n" +
                        "        <xs:element name=\"firstName\" type=\"xs:string\"/>\n" +
                        "        <xs:element name=\"lastName\" type=\"xs:string\"/>\n" +
                        "      </xs:sequence>\n" +
                        "    </xs:complexType>\n" +
                        "  </xs:element>\n" +
                        "</xs:schema>\n")
                .build();

        PureModelContextData model = generateModel(schemaCode, config("test::GenerateClassForInlineComplexTypes", "test::gen", false, true));

        String expected = ">>>test::gen::Person\n" +
                "Class test::gen::Person\n" +
                "{\n" +
                "  firstName: String[1];\n" +
                "  lastName: String[1];\n" +
                "}\n";

        Assert.assertEquals(modelTextsFromString(expected), modelTextsFromContextData(model));
    }

    @Test
    public void testExtendComplexTypeWithSimpleContent()
    {
        String schemaCode = newExternalSchemaSetGrammarBuilder("test::ExtendComplexTypeWithSimpleContent", "XSD")
                .withSchemaText(null, "extend-complex-type-with-simple-content.xsd", "<?xml version='1.0'?>\n" +
                        "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
                        "  <xs:complexType name=\"StringType\">\n" +
                        "    <xs:simpleContent>\n" +
                        "      <xs:extension base=\"xs:string\">\n" +
                        "        <xs:attribute name=\"source\" type=\"xs:string\"/>\n" +
                        "        <xs:attribute name=\"origValue\" type=\"xs:string\"/>\n" +
                        "        <xs:attribute name=\"comment\" type=\"xs:string\"/>\n" +
                        "      </xs:extension>\n" +
                        "    </xs:simpleContent>\n" +
                        "  </xs:complexType>\n" +
                        "  <xs:complexType name=\"IdentifierType\">\n" +
                        "    <xs:simpleContent>\n" +
                        "      <xs:extension base=\"StringType\">\n" +
                        "        <xs:attribute name=\"type\" type=\"xs:string\" use=\"required\"/>\n" +
                        "        <xs:attribute name=\"expiredDate\" type=\"xs:date\"/>\n" +
                        "      </xs:extension>\n" +
                        "    </xs:simpleContent>\n" +
                        "  </xs:complexType>\n" +
                        "</xs:schema>\n")
                .build();

        PureModelContextData model = generateModel(schemaCode, config("test::ExtendComplexTypeWithSimpleContent", "test::gen", false, true));

        String expected = ">>>test::gen::StringType\n" +
                "Class test::gen::StringType\n" +
                "{\n" +
                "  source: String[0..1];\n" +
                "  origValue: String[0..1];\n" +
                "  comment: String[0..1];\n" +
                "  value: String[1];\n" +
                "}\n" +
                "\n" +
                ">>>test::gen::IdentifierType\n" +
                "Class test::gen::IdentifierType extends test::gen::StringType\n" +
                "{\n" +
                "  type: String[1];\n" +
                "  expiredDate: StrictDate[0..1];\n" +
                "}\n";

        Assert.assertEquals(modelTextsFromString(expected), modelTextsFromContextData(model));
    }

    @Test
    // This is a test of the internals of the algorithm - partially resolved refers to the order of evaluation
    public void testExtendComplexTypeWithComplexTypePartiallyResolved()
    {
        String schemaCode = newExternalSchemaSetGrammarBuilder("test::ExtendComplexTypeWithComplexTypePartiallyResolved", "XSD")
                .withSchemaText(null, "extend-complex-type-with-complex-type-partially-resolved.xsd", "<?xml version='1.0'?>\n" +
                        "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
                        "  <xs:complexType name=\"TrailerType\">\n" +
                        "    <xs:sequence>\n" +
                        "      <xs:element name=\"Exceptions\" type=\"ExceptionType\" minOccurs=\"0\" maxOccurs=\"unbounded\"/>\n" +
                        "    </xs:sequence>\n" +
                        "    <xs:attribute name=\"numberOfElements\" type=\"xs:int\"/>\n" +
                        "  </xs:complexType>\n" +
                        "  <xs:complexType name=\"ExceptionType\">\n" +
                        "    <xs:simpleContent>\n" +
                        "      <xs:extension base=\"xs:string\">\n" +
                        "        <xs:attribute name=\"type\" use=\"required\">\n" +
                        "          <xs:simpleType>\n" +
                        "            <xs:restriction base=\"xs:string\">\n" +
                        "              <xs:enumeration value=\"INFO\"/>\n" +
                        "              <xs:enumeration value=\"WARN\"/>\n" +
                        "              <xs:enumeration value=\"ERROR\"/>\n" +
                        "            </xs:restriction>\n" +
                        "          </xs:simpleType>\n" +
                        "        </xs:attribute>\n" +
                        "      </xs:extension>\n" +
                        "    </xs:simpleContent>\n" +
                        "  </xs:complexType>\n" +
                        "  <xs:complexType name=\"DerivativeTrailerType\">\n" +
                        "    <xs:complexContent>\n" +
                        "      <xs:extension base=\"TrailerType\">\n" +
                        "        <xs:attribute name=\"CntPrd\" type=\"xs:int\" use=\"required\" />\n" +
                        "        <xs:attribute name=\"CntCtr\" type=\"xs:int\" use=\"required\" />\n" +
                        "      </xs:extension>\n" +
                        "    </xs:complexContent>\n" +
                        "  </xs:complexType>\n" +
                        "</xs:schema>\n")
                .build();

        PureModelContextData model = generateModel(schemaCode, config("test::ExtendComplexTypeWithComplexTypePartiallyResolved", "test::gen", false, true));

        String expected = ">>>test::gen::DerivativeTrailerType\n" +
                "Class test::gen::DerivativeTrailerType extends test::gen::TrailerType\n" +
                "{\n" +
                "  cntPrd: Integer[1];\n" +
                "  cntCtr: Integer[1];\n" +
                "}\n" +
                "\n" +
                ">>>test::gen::TrailerType\n" +
                "Class test::gen::TrailerType\n" +
                "{\n" +
                "  exceptions: test::gen::ExceptionType[*];\n" +
                "  numberOfElements: Integer[0..1];\n" +
                "}\n" +
                "\n" +
                ">>>test::gen::ExceptionType\n" +
                "Class test::gen::ExceptionType\n" +
                "[\n" +
                "  c1_values: $this.type->in(['INFO', 'WARN', 'ERROR'])\n" +
                "]\n" +
                "{\n" +
                "  type: String[1];\n" +
                "  value: String[1];\n" +
                "}\n";

        Assert.assertEquals(modelTextsFromString(expected), modelTextsFromContextData(model));
    }

    @Test
    public void testCollectionTypeCanBeInlined()
    {
        String schemaCode = newExternalSchemaSetGrammarBuilder("test::CollectionType", "XSD")
                .withSchemaText(null, "collection-type.xsd", "<?xml version='1.0'?>\n" +
                        "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
                        "  <xs:complexType name=\"Person\">\n" +
                        "    <xs:sequence>\n" +
                        "      <xs:element name=\"firstName\" type=\"xs:string\"/>\n" +
                        "      <xs:element name=\"lastName\" type=\"xs:string\"/>\n" +
                        "    </xs:sequence>\n" +
                        "  </xs:complexType>\n" +
                        "  <xs:complexType name=\"People\">\n" +
                        "    <xs:sequence>\n" +
                        "      <xs:element name=\"person\" type=\"Person\" minOccurs=\"0\" maxOccurs=\"unbounded\"/>\n" +
                        "    </xs:sequence>\n" +
                        "  </xs:complexType>\n" +
                        "  <xs:element name=\"firm\">\n" +
                        "    <xs:complexType>\n" +
                        "      <xs:sequence>\n" +
                        "        <xs:element name=\"employees\" type=\"People\" minOccurs=\"0\" />\n" +
                        "      </xs:sequence>\n" +
                        "    </xs:complexType>\n" +
                        "  </xs:element>\n" +
                        "</xs:schema>\n")
                .build();

        PureModelContextData directModel = generateModel(schemaCode, config("test::CollectionType", "test::gen", false, false));
        PureModelContextData inlinedModel = generateModel(schemaCode, config("test::CollectionType", "test::gen", true, false));

        String expectedDirect = ">>>test::gen::Person\n" +
                "Class test::gen::Person\n" +
                "{\n" +
                "  firstName: String[1];\n" +
                "  lastName: String[1];\n" +
                "}\n" +
                "\n" +
                ">>>test::gen::People\n" +
                "Class test::gen::People\n" +
                "{\n" +
                "  person: test::gen::Person[*];\n" +
                "}\n" +
                "\n" +
                ">>>test::gen::Firm\n" +
                "Class test::gen::Firm\n" +
                "{\n" +
                "  employees: test::gen::People[0..1];\n" +
                "}\n";

        String expectedInlined = ">>>test::gen::Person\n" +
                "Class test::gen::Person\n" +
                "{\n" +
                "  firstName: String[1];\n" +
                "  lastName: String[1];\n" +
                "}\n" +
                "\n" +
                ">>>test::gen::Firm\n" +
                "Class test::gen::Firm\n" +
                "{\n" +
                "  employees: test::gen::Person[*];\n" +
                "}\n";

        Assert.assertEquals(modelTextsFromString(expectedDirect), modelTextsFromContextData(directModel));
        Assert.assertEquals(modelTextsFromString(expectedInlined), modelTextsFromContextData(inlinedModel));
    }

    @Test
    public void testPrimitiveCollectionTypeCanBeInlined()
    {
        String schemaCode = newExternalSchemaSetGrammarBuilder("test::PrimitiveCollectionType", "XSD")
                .withSchemaText(null, "primitivec-ollection-type.xsd", "<?xml version='1.0'?>\n" +
                        "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
                        "  <xs:complexType name=\"Names\">\n" +
                        "    <xs:sequence>\n" +
                        "      <xs:element name=\"name\" type=\"xs:string\"  minOccurs=\"0\"  maxOccurs=\"unbounded\"/>\n" +
                        "    </xs:sequence>\n" +
                        "  </xs:complexType>\n" +
                        "  <xs:element name=\"person\">\n" +
                        "    <xs:complexType>\n" +
                        "      <xs:sequence>\n" +
                        "        <xs:element name=\"names\" type=\"Names\" minOccurs=\"0\" />\n" +
                        "      </xs:sequence>\n" +
                        "    </xs:complexType>\n" +
                        "  </xs:element>\n" +
                        "</xs:schema>\n")
                .build();

        PureModelContextData model = generateModel(schemaCode, config("test::PrimitiveCollectionType", "test::gen", true, false));

        String expected = ">>>test::gen::Person\n" +
                "Class test::gen::Person\n" +
                "{\n" +
                "  names: String[*];\n" +
                "}\n";

        Assert.assertEquals(modelTextsFromString(expected), modelTextsFromContextData(model));
    }

    @Test
    public void testInheritedLengthRestriction()
    {
        String schemaCode = newExternalSchemaSetGrammarBuilder("test::InheritedLengthRestriction", "XSD")
                .withSchemaText(null, "inherited-length-restriction.xsd", "<?xml version='1.0'?>\n" +
                                        "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
                                        "  <xs:simpleType name=\"NormalizedString\">\n" +
                                        "    <xs:restriction base=\"xs:normalizedString\">\n" +
                                        "      <xs:minLength value=\"0\" />\n" +
                                        "    </xs:restriction>\n" +
                                        "  </xs:simpleType>\n" +
                                        "  <xs:simpleType name=\"NonEmptyScheme\">\n" +
                                        "    <xs:restriction base=\"NormalizedString\">\n" +
                                        "      <xs:minLength value=\"1\"></xs:minLength>\n" +
                                        "      <xs:maxLength value=\"255\" />\n" +
                                        "    </xs:restriction>\n" +
                                        "  </xs:simpleType>\n" +
                                        "  <xs:complexType name=\"AccountId\">\n" +
                                        "    <xs:simpleContent>\n" +
                                        "      <xs:extension base=\"NonEmptyScheme\" />\n" +
                                        "    </xs:simpleContent>\n" +
                                        "  </xs:complexType>\n" +
                                        "</xs:schema>\n")
                .build();

        PureModelContextData model = generateModel(schemaCode, config("test::InheritedLengthRestriction", "test::gen", false, true));

        String expected = ">>>test::gen::AccountId\n" +
                "Class test::gen::AccountId\n" +
                "[\n" +
                "  c1_length: $this.value->length() >= 1,\n" +
                "  c2_length: $this.value->length() <= 255\n" +
                "]\n" +
                "{\n" +
                "  value: String[1];\n" +
                "}\n";

        Assert.assertEquals(modelTextsFromString(expected), modelTextsFromContextData(model));
    }

    @Test
    public void testInclude()
    {
        String schemaCode = newExternalSchemaSetGrammarBuilder("test::Include", "XSD")
                .withSchemaResource("simple", "simple.xsd", "modelGeneration/includeSimple.xsd")
                .withSchemaResource("complex", "complex.xsd", "modelGeneration/includeComplex.xsd")
                .withSchemaResource("main", "main.xsd", "modelGeneration/includeMain.xsd")
                .build();

        PureModelContextData model = generateModel(schemaCode, config("test::Include", "main", "test::gen", false, true));
        Assert.assertEquals(modelTextsFromResource("modelGeneration/includeResult.txt"), modelTextsFromContextData(model));
    }

    @Test
    public void testRdu()
    {
        String schemaCode = newExternalSchemaSetGrammarBuilder("test::rdu::RDU", "XSD")
                .withSchemaResource("std", "derivativesStd.xsd", "rdu-sample/derivativesStd.xsd")
                .withSchemaResource("commons", "commons.xsd", "rdu-sample/commons.xsd")
                .build();

        PureModelContextData model = generateModel(schemaCode, config("test::rdu::RDU", "std", "test::gen", true, false));
        assertModelTexts(modelTextsFromResource("rdu-sample/genResult.txt"), modelTextsFromContextData(model));
    }

    @Test
    public void testFpml()
    {
        String schemaCode = newExternalSchemaSetGrammarBuilder("test::fmpl::FpML_5_10", "XSD")
                .withSchemaResource("enum", "fpml-enum-5-10.xsd", "fpml-sample/fpml-enum-5-10.xsd")
                .withSchemaResource("shared", "fpml-shared-5-10.xsd", "fpml-sample/fpml-shared-5-10.xsd")
                .build();

        PureModelContextData model = generateModel(schemaCode, config("test::fmpl::FpML_5_10", "shared",  "test::gen", false, true));
        assertModelTexts(modelTextsFromResource("fpml-sample/genResult.txt"), modelTextsFromContextData(model));
    }

    @Test
    public void testXetra()
    {
        String schemaCode = newExternalSchemaSetGrammarBuilder("test::fixml_5_0_SP2::Xetra", "XSD")
                .withSchemaResource("main", "fixml-main-5-0-SP2_.xsd", "xetra-sample/fixml-main-5-0-SP2_.xsd")
                .withSchemaResource("pre_trade", "fixml-pretrade-5-0-SP2_.xsd", "xetra-sample/fixml-pretrade-5-0-SP2_.xsd")
                .withSchemaResource("trade", "fixml-trade-5-0-SP2_.xsd", "xetra-sample/fixml-trade-5-0-SP2_.xsd")
                .withSchemaResource("post_trade", "fixml-posttrade-5-0-SP2_.xsd", "xetra-sample/fixml-posttrade-5-0-SP2_.xsd")
                .withSchemaResource("infrasructure", "fixml-infrastructure-5-0-SP2_.xsd", "xetra-sample/fixml-infrastructure-5-0-SP2_.xsd")
                .withSchemaResource("indications_impl", "fixml-indications-impl-5-0-SP2_.xsd", "xetra-sample/fixml-indications-impl-5-0-SP2_.xsd")
                .withSchemaResource("indications_base", "fixml-indications-base-5-0-SP2_.xsd", "xetra-sample/fixml-indications-base-5-0-SP2_.xsd")
                .withSchemaResource("news_events_impl", "fixml-newsevents-impl-5-0-SP2_.xsd", "xetra-sample/fixml-newsevents-impl-5-0-SP2_.xsd")
                .withSchemaResource("news_events_base", "fixml-newsevents-base-5-0-SP2_.xsd", "xetra-sample/fixml-newsevents-base-5-0-SP2_.xsd")
                .withSchemaResource("quotation_impl", "fixml-quotation-impl-5-0-SP2_.xsd", "xetra-sample/fixml-quotation-impl-5-0-SP2_.xsd")
                .withSchemaResource("quotation_base", "fixml-quotation-base-5-0-SP2_.xsd", "xetra-sample/fixml-quotation-base-5-0-SP2_.xsd")
                .withSchemaResource("market_data_impl", "fixml-marketdata-impl-5-0-SP2_.xsd", "xetra-sample/fixml-marketdata-impl-5-0-SP2_.xsd")
                .withSchemaResource("market_data_base", "fixml-marketdata-base-5-0-SP2_.xsd", "xetra-sample/fixml-marketdata-base-5-0-SP2_.xsd")
                .withSchemaResource("market_structure_impl", "fixml-marketstructure-impl-5-0-SP2_.xsd", "xetra-sample/fixml-marketstructure-impl-5-0-SP2_.xsd")
                .withSchemaResource("market_structure_base", "fixml-marketstructure-base-5-0-SP2_.xsd", "xetra-sample/fixml-marketstructure-base-5-0-SP2_.xsd")
                .withSchemaResource("securities_reference_impl", "fixml-securitiesreference-impl-5-0-SP2_.xsd", "xetra-sample/fixml-securitiesreference-impl-5-0-SP2_.xsd")
                .withSchemaResource("securities_reference_base", "fixml-securitiesreference-base-5-0-SP2_.xsd", "xetra-sample/fixml-securitiesreference-base-5-0-SP2_.xsd")
                .withSchemaResource("order_impl", "fixml-order-impl-5-0-SP2_.xsd", "xetra-sample/fixml-order-impl-5-0-SP2_.xsd")
                .withSchemaResource("order_base", "fixml-order-base-5-0-SP2_.xsd", "xetra-sample/fixml-order-base-5-0-SP2_.xsd")
                .withSchemaResource("list_orders_impl", "fixml-listorders-impl-5-0-SP2_.xsd", "xetra-sample/fixml-listorders-impl-5-0-SP2_.xsd")
                .withSchemaResource("list_orders_base", "fixml-listorders-base-5-0-SP2_.xsd", "xetra-sample/fixml-listorders-base-5-0-SP2_.xsd")
                .withSchemaResource("order_mass_handling_impl", "fixml-ordermasshandling-impl-5-0-SP2_.xsd", "xetra-sample/fixml-ordermasshandling-impl-5-0-SP2_.xsd")
                .withSchemaResource("order_mass_handling_base", "fixml-ordermasshandling-base-5-0-SP2_.xsd", "xetra-sample/fixml-ordermasshandling-base-5-0-SP2_.xsd")
                .withSchemaResource("cross_orders_impl", "fixml-crossorders-impl-5-0-SP2_.xsd", "xetra-sample/fixml-crossorders-impl-5-0-SP2_.xsd")
                .withSchemaResource("cross_orders_base", "fixml-crossorders-base-5-0-SP2_.xsd", "xetra-sample/fixml-crossorders-base-5-0-SP2_.xsd")
                .withSchemaResource("multileg_orders_impl", "fixml-multilegorders-impl-5-0-SP2_.xsd", "xetra-sample/fixml-multilegorders-impl-5-0-SP2_.xsd")
                .withSchemaResource("multileg_orders_base", "fixml-multilegorders-base-5-0-SP2_.xsd", "xetra-sample/fixml-multilegorders-base-5-0-SP2_.xsd")
                .withSchemaResource("allocation_impl", "fixml-allocation-impl-5-0-SP2_.xsd", "xetra-sample/fixml-allocation-impl-5-0-SP2_.xsd")
                .withSchemaResource("allocation_base", "fixml-allocation-base-5-0-SP2_.xsd", "xetra-sample/fixml-allocation-base-5-0-SP2_.xsd")
                .withSchemaResource("settlement_impl", "fixml-settlement-impl-5-0-SP2_.xsd", "xetra-sample/fixml-settlement-impl-5-0-SP2_.xsd")
                .withSchemaResource("settlement_base", "fixml-settlement-base-5-0-SP2_.xsd", "xetra-sample/fixml-settlement-base-5-0-SP2_.xsd")
                .withSchemaResource("registration_impl", "fixml-registration-impl-5-0-SP2_.xsd", "xetra-sample/fixml-registration-impl-5-0-SP2_.xsd")
                .withSchemaResource("registration_base", "fixml-registration-base-5-0-SP2_.xsd", "xetra-sample/fixml-registration-base-5-0-SP2_.xsd")
                .withSchemaResource("trade_capture_impl", "fixml-tradecapture-impl-5-0-SP2_.xsd", "xetra-sample/fixml-tradecapture-impl-5-0-SP2_.xsd")
                .withSchemaResource("trade_capture_base", "fixml-tradecapture-base-5-0-SP2_.xsd", "xetra-sample/fixml-tradecapture-base-5-0-SP2_.xsd")
                .withSchemaResource("confirmation_impl", "fixml-confirmation-impl-5-0-SP2_.xsd", "xetra-sample/fixml-confirmation-impl-5-0-SP2_.xsd")
                .withSchemaResource("confirmation_base", "fixml-confirmation-base-5-0-SP2_.xsd", "xetra-sample/fixml-confirmation-base-5-0-SP2_.xsd")
                .withSchemaResource("positions_impl", "fixml-positions-impl-5-0-SP2_.xsd", "xetra-sample/fixml-positions-impl-5-0-SP2_.xsd")
                .withSchemaResource("positions_base", "fixml-positions-base-5-0-SP2_.xsd", "xetra-sample/fixml-positions-base-5-0-SP2_.xsd")
                .withSchemaResource("collateral_impl", "fixml-collateral-impl-5-0-SP2_.xsd", "xetra-sample/fixml-collateral-impl-5-0-SP2_.xsd")
                .withSchemaResource("collateral_base", "fixml-collateral-base-5-0-SP2_.xsd", "xetra-sample/fixml-collateral-base-5-0-SP2_.xsd")
                .withSchemaResource("application_impl", "fixml-application-impl-5-0-SP2_.xsd", "xetra-sample/fixml-application-impl-5-0-SP2_.xsd")
                .withSchemaResource("application_base", "fixml-application-base-5-0-SP2_.xsd", "xetra-sample/fixml-application-base-5-0-SP2_.xsd")
                .withSchemaResource("business_reject_impl", "fixml-businessreject-impl-5-0-SP2_.xsd", "xetra-sample/fixml-businessreject-impl-5-0-SP2_.xsd")
                .withSchemaResource("business_reject_base", "fixml-businessreject-base-5-0-SP2_.xsd", "xetra-sample/fixml-businessreject-base-5-0-SP2_.xsd")
                .withSchemaResource("network_impl", "fixml-network-impl-5-0-SP2_.xsd", "xetra-sample/fixml-network-impl-5-0-SP2_.xsd")
                .withSchemaResource("network_base", "fixml-network-base-5-0-SP2_.xsd", "xetra-sample/fixml-network-base-5-0-SP2_.xsd")
                .withSchemaResource("user_management_impl", "fixml-usermanagement-impl-5-0-SP2_.xsd", "xetra-sample/fixml-usermanagement-impl-5-0-SP2_.xsd")
                .withSchemaResource("user_management_base", "fixml-usermanagement-base-5-0-SP2_.xsd", "xetra-sample/fixml-usermanagement-base-5-0-SP2_.xsd")
                .withSchemaResource("components_impl", "fixml-components-impl-5-0-SP2_.xsd", "xetra-sample/fixml-components-impl-5-0-SP2_.xsd")
                .withSchemaResource("components_base", "fixml-components-base-5-0-SP2_.xsd", "xetra-sample/fixml-components-base-5-0-SP2_.xsd")
                .withSchemaResource("fields_impl", "fixml-fields-impl-5-0-SP2_.xsd", "xetra-sample/fixml-fields-impl-5-0-SP2_.xsd")
                .withSchemaResource("fields_base", "fixml-fields-base-5-0-SP2_.xsd", "xetra-sample/fixml-fields-base-5-0-SP2_.xsd")
                .withSchemaResource("datatypes", "fixml-datatypes-5-0-SP2_.xsd", "xetra-sample/fixml-datatypes-5-0-SP2_.xsd")
                .build();

        PureModelContextData model = generateModel(schemaCode, config("test::fixml_5_0_SP2::Xetra", "main", "test::gen", true, true));
        assertModelTexts(modelTextsFromResource("xetra-sample/genResult.txt"), modelTextsFromContextData(model));
    }

    @Test
    public void testNgm()
    {
        String schemaCode = newExternalSchemaSetGrammarBuilder("test::ngm::v2_6::NGM", "XSD")
                .withSchemaResource("exch", "ExchangeDataSchema_2.6.xsd", "ngm-sample/ExchangeDataSchema_2.6.xsd")
                .build();

        PureModelContextData exchModel = generateModel(schemaCode, config("test::ngm::v2_6::NGM", "exch", "test::gen", true, true));
        assertModelTexts(modelTextsFromResource("ngm-sample/exchangeGenResult.txt"), modelTextsFromContextData(exchModel));
    }

    private XsdToModelConfiguration config(String sourceSchemaSet, String targetPackage)
    {
        return config(sourceSchemaSet, targetPackage, false, false);
    }

    private XsdToModelConfiguration config(String sourceSchemaSet, String targetPackage, boolean inlineCollectionClasses, boolean includeUnreachableClasses)
    {
        return config(sourceSchemaSet, null, targetPackage, inlineCollectionClasses, includeUnreachableClasses);
    }

    private XsdToModelConfiguration config(String sourceSchemaSet, String sourceSchemaId, String targetPackage, boolean inlineCollectionClasses, boolean includeUnreachableClasses)
    {
        XsdToModelConfiguration config = new XsdToModelConfiguration();
        config.sourceSchemaSet = sourceSchemaSet;
        config.sourceSchemaId = sourceSchemaId;
        config.targetPackage = targetPackage;
        config.targetBinding = targetPackage + "::TestBinding";
        config.inlineCollectionClasses = inlineCollectionClasses;
        config.includeUnreachableClasses = includeUnreachableClasses;
        return config;
    }
}
