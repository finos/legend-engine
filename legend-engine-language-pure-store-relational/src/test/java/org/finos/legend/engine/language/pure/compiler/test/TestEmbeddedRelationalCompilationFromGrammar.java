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

package org.finos.legend.engine.language.pure.compiler.test;

import org.junit.Ignore;
import org.junit.Test;

import static org.finos.legend.engine.language.pure.compiler.test.TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite.test;

public class TestEmbeddedRelationalCompilationFromGrammar
{

    @Test
    public void testEmbeddedMapping()
    {
        test("import other::*;\n" +
                "\n" +
                "Class other::Person\n" +
                "{\n" +
                "    name:String[1];\n" +
                "    firm:Firm[1];\n" +
                "    address:Address[0..1];\n" +
                "}\n" +
                "Class other::Firm\n" +
                "{\n" +
                "    legalName:String[1];\n" +
                "    employees:Person[1];\n" +
                "    address:Address[1];\n" +
                "}\n" +
                "Class other::Address\n" +
                "{\n" +
                "    line1:String[1];\n" +
                "}\n" +
                "###Relational\n" +
                "Database mapping::db(\n" +
                "   Table employeeFirmDenormTable\n" +
                "   (\n" +
                "    id INT PRIMARY KEY,\n" +
                "    name VARCHAR(200),\n" +
                "    firmId INT,\n" +
                "    legalName VARCHAR(200),\n" +
                "    address VARCHAR(200)\n" +
                "   )\n" +
                ")\n" +
                "###Mapping\n" +
                "import other::*;\n" +
                "import mapping::*;\n" +
                "Mapping mappingPackage::myMapping\n" +
                "(\n" +
                "    Person: Relational\n" +
                "    {\n" +
                "        name : [db]employeeFirmDenormTable.name,\n" +
                "        firm\n" +
                "        (\n" +
                "            ~primaryKey ([db]employeeFirmDenormTable.legalName)\n" +
                "            legalName : [db]employeeFirmDenormTable.legalName,\n" +
                "            address\n" +
                "            (\n" +
                "                line1: [db]employeeFirmDenormTable.address\n" +
                "            )\n" +
                "        ),\n" +
                "        address\n" +
                "        (\n" +
                "            line1: [db]employeeFirmDenormTable.address\n" +
                "        )\n" +
                "    }\n" +
                ")\n");
    }

    @Test
    public void testEmbeddedMappingWithIds()
    {
        test("import other::*;\n" +
                "import meta::pure::mapping::*;\n" +
                "\n" +
                "Class other::Person\n" +
                "{\n" +
                "    name:String[1];\n" +
                "    firm:Firm[1];\n" +
                "    address:Address[0..1];\n" +
                "}\n" +
                "Class other::Firm\n" +
                "{\n" +
                "    legalName:String[1];\n" +
                "    employees:Person[1];\n" +
                "    address:Address[1];\n" +
                "}\n" +
                "Class other::Address\n" +
                "{\n" +
                "    line1:String[1];\n" +
                "}\n" +
                "   function meta::pure::router::operations::union(o:OperationSetImplementation[1]):SetImplementation[*]\n" +
                "   {\n" +
                "       $o.parameters.setImplementation;\n" +
                "   }\n" +
                "###Relational\n" +
                "Database mapping::db(\n" +
                "   Table employeeFirmDenormTable\n" +
                "   (\n" +
                "    id INT PRIMARY KEY,\n" +
                "    name VARCHAR(200),\n" +
                "    firmId INT,\n" +
                "    legalName VARCHAR(200),\n" +
                "    address VARCHAR(200)\n" +
                "   )\n" +
                "   Table employeeFirmDenormTable2\n" +
                "   (\n" +
                "    id INT PRIMARY KEY,\n" +
                "    name VARCHAR(200),\n" +
                "    firmId INT,\n" +
                "    legalName VARCHAR(200),\n" +
                "    address VARCHAR(200)\n" +
                "   )\n" +
                ")\n" +
                "###Mapping\n" +
                "import other::*;\n" +
                "import mapping::*;\n" +
                "Mapping mappingPackage::myMapping\n" +
                "(\n" +
                "    Person[per1]: Relational\n" +
                "    {\n" +
                "        name : [db]employeeFirmDenormTable.name,\n" +
                "        firm\n" +
                "        (\n" +
                "            ~primaryKey ([db]employeeFirmDenormTable.legalName)\n" +
                "            legalName : [db]employeeFirmDenormTable.legalName,\n" +
                "            address\n" +
                "            (\n" +
                "                line1: [db]employeeFirmDenormTable.address\n" +
                "            )\n" +
                "        ),\n" +
                "        address\n" +
                "        (\n" +
                "            line1: [db]employeeFirmDenormTable.address\n" +
                "        )\n" +
                "    }\n" +
                "    Person[per2]: Relational\n" +
                "    {\n" +
                "        name : [db]employeeFirmDenormTable2.name,\n" +
                "        firm\n" +
                "        (\n" +
                "            ~primaryKey ([db]employeeFirmDenormTable2.legalName)\n" +
                "            legalName : [db]employeeFirmDenormTable2.legalName,\n" +
                "            address\n" +
                "            (\n" +
                "                line1: [db]employeeFirmDenormTable2.address\n" +
                "            )\n" +
                "        ),\n" +
                "        address\n" +
                "        (\n" +
                "            line1: [db]employeeFirmDenormTable2.address\n" +
                "        )\n" +
                "    }\n" +
                "    *Person : Operation\n" +
                "    {\n" +
                "               meta::pure::router::operations::union_OperationSetImplementation_1__SetImplementation_MANY_( per1, per2 )   \n" +
                "    }\n" +
                ")\n");
    }

    @Test
    @Ignore
    // TODO: investigate and fix
    public void embeddedMappingsCanBeReferenced()
    {
        test("import other::*;\n" +
                "\n" +
                "Class other::Person\n" +
                "{\n" +
                "    name:String[1];\n" +
                "    address:Address[1];\n" +
                "    firm:Firm[1];\n" +
                "}\n" +
                "Class other::Firm\n" +
                "{\n" +
                "    legalName:String[1];\n" +
                "}\n" +
                "Class other::Address\n" +
                "{\n" +
                "    line1:String[1];\n" +
                "    postcode:String[1];\n" +
                "}\n" +
                "###Relational\n" +
                "Database mapping::db(\n" +
                "   Table employeeFirmDenormTable\n" +
                "   (\n" +
                "    id INT PRIMARY KEY,\n" +
                "    name VARCHAR(200),\n" +
                "    firmId INT,\n" +
                "    legalName VARCHAR(200),\n" +
                "    address1 VARCHAR(200),\n" +
                "    postcode VARCHAR(10)\n" +
                "   )\n" +
                ")\n" +
                "###Mapping\n" +
                "import other::*;\n" +
                "import mapping::*;\n" +
                "Mapping mappingPackage::myMapping\n" +
                "(\n" +
                "    Address[alias2]: Relational\n" +
                "    {\n" +
                "       line1: [db]employeeFirmDenormTable.address1,\n" +
                "       postcode: [db]employeeFirmDenormTable.postcode\n" +
                "    }\n" +
                "    Person[alias1]: Relational\n" +
                "    {\n" +
                "        name : [db]employeeFirmDenormTable.name,\n" +
                "        firm\n" +
                "        (\n" +
                "            ~primaryKey ([db]employeeFirmDenormTable.legalName)\n" +
                "            legalName : [db]employeeFirmDenormTable.legalName\n" +
                "        ),\n" +
                "        address () Inline [alias2]\n" +
                "    }\n" +
                ")\n");
    }

    @Test
    public void embeddedMappingsWithOtherwise()
    {
        test("import other::*;\n" +
                "\n" +
                "Class other::Person\n" +
                "{\n" +
                "    name:String[1];\n" +
                "    firm:Firm[1];\n" +
                "}\n" +
                "Class other::Firm\n" +
                "{\n" +
                "    legalName:String[1];\n" +
                "    otherInformation:String[1];\n" +
                "}\n" +
                "###Relational\n" +
                "Database mapping::db(\n" +
                "   Table employeeFirmDenormTable\n" +
                "   (\n" +
                "    id INT PRIMARY KEY,\n" +
                "    name VARCHAR(200),\n" +
                "    firmId INT,\n" +
                "    legalName VARCHAR(200),\n" +
                "    address1 VARCHAR(200),\n" +
                "    postcode VARCHAR(10)\n" +
                "   )\n" +
                "   Table FirmInfoTable\n" +
                "   (\n" +
                "    id INT PRIMARY KEY,\n" +
                "    name VARCHAR(200),\n" +
                "    other VARCHAR(200)\n" +
                "   )\n" +
                "   Join PersonFirmJoin(employeeFirmDenormTable.firmId = FirmInfoTable.id)\n" +
                ")\n" +
                "###Mapping\n" +
                "import other::*;\n" +
                "import mapping::*;\n" +
                "Mapping mappingPackage::myMapping\n" +
                "(\n" +
                "    Firm[firm1]: Relational\n" +
                "    {\n" +
                "       legalName : [db]FirmInfoTable.name ,\n" +
                "       otherInformation: [db]FirmInfoTable.other\n" +
                "    }\n" +
                "    Person[alias1]: Relational\n" +
                "    {\n" +
                "        name : [db]employeeFirmDenormTable.name,\n" +
                "        firm\n" +
                "        (\n" +
                "            ~primaryKey ([db]employeeFirmDenormTable.legalName)\n" +
                "            legalName : [db]employeeFirmDenormTable.legalName\n" +
                "        ) Otherwise ( [firm1]:[db]@PersonFirmJoin) \n" +
                "    }\n" +
                ")\n");
    }

    @Test
    public void embeddedMappingsWithOtherwise2()
    {
        test("import other::*;\n" +
                "\n" +
                "Class other::Person\n" +
                "{\n" +
                "    name:String[1];\n" +
                "    firm:Firm[1];\n" +
                "}\n" +
                "Class other::Firm\n" +
                "{\n" +
                "    legalName:String[1];\n" +
                "    otherInformation:String[1];\n" +
                "}\n" +
                "###Relational\n" +
                "Database mapping::db(\n" +
                "   Table employeeFirmDenormTable\n" +
                "   (\n" +
                "    id INT PRIMARY KEY,\n" +
                "    name VARCHAR(200),\n" +
                "    firmId INT,\n" +
                "    legalName VARCHAR(200),\n" +
                "    address1 VARCHAR(200),\n" +
                "    postcode VARCHAR(10)\n" +
                "   )\n" +
                "   Table FirmInfoTable\n" +
                "   (\n" +
                "    id INT PRIMARY KEY,\n" +
                "    name VARCHAR(200),\n" +
                "    other VARCHAR(200)\n" +
                "   )\n" +
                "   Join PersonFirmJoin(employeeFirmDenormTable.firmId = FirmInfoTable.id)\n" +
                ")\n" +
                "###Mapping\n" +
                "import other::*;\n" +
                "import mapping::*;\n" +
                "Mapping mappingPackage::myMapping\n" +
                "(\n" +
                "    Firm[f1]: Relational\n" +
                "    {\n" +
                "       legalName : [db]FirmInfoTable.name ,\n" +
                "       otherInformation: [db]FirmInfoTable.other\n" +
                "    }\n" +
                "    Person[alias1]: Relational\n" +
                "    {\n" +
                "        name : [db]employeeFirmDenormTable.name,\n" +
                "       firm[f1]:[db]@PersonFirmJoin \n" +
                "    }\n" +
                ")\n");
    }

    @Test
    public void embeddedMappingsWithOtherwiseInClassMappingWithoutId()
    {
        test("import other::*;\n" +
                "\n" +
                "Class other::Person\n" +
                "{\n" +
                "    name:String[1];\n" +
                "    firm:Firm[1];\n" +
                "}\n" +
                "Class other::Firm\n" +
                "{\n" +
                "    legalName:String[1];\n" +
                "    otherInformation:String[1];\n" +
                "}\n" +
                "###Relational\n" +
                "Database mapping::db(\n" +
                "   Table employeeFirmDenormTable\n" +
                "   (\n" +
                "    id INT PRIMARY KEY,\n" +
                "    name VARCHAR(200),\n" +
                "    firmId INT,\n" +
                "    legalName VARCHAR(200),\n" +
                "    address1 VARCHAR(200),\n" +
                "    postcode VARCHAR(10)\n" +
                "   )\n" +
                "   Table FirmInfoTable\n" +
                "   (\n" +
                "    id INT PRIMARY KEY,\n" +
                "    name VARCHAR(200),\n" +
                "    other VARCHAR(200)\n" +
                "   )\n" +
                "   Join PersonFirmJoin(employeeFirmDenormTable.firmId = FirmInfoTable.id)\n" +
                ")\n" +
                "###Mapping\n" +
                "import other::*;\n" +
                "import mapping::*;\n" +
                "Mapping mappingPackage::myMapping\n" +
                "(\n" +
                "    Firm[firm1]: Relational\n" +
                "    {\n" +
                "       legalName : [db]FirmInfoTable.name ,\n" +
                "       otherInformation: [db]FirmInfoTable.other\n" +
                "    }\n" +
                "    Person: Relational\n" +
                "    {\n" +
                "        name : [db]employeeFirmDenormTable.name,\n" +
                "        firm\n" +
                "        (\n" +
                "            ~primaryKey ([db]employeeFirmDenormTable.legalName)\n" +
                "            legalName : [db]employeeFirmDenormTable.legalName\n" +
                "        ) Otherwise ( [firm1]:[db]@PersonFirmJoin) \n" +
                "    }\n" +
                ")\n");
    }

    @Test
    public void embeddedMappingsCanBeReferencedInAssociationMappingAsSource()
    {
        test("import other::*;\n" +
                "\n" +
                "Class other::Person\n" +
                "{\n" +
                "    name:String[1];\n" +
                "    firm:Firm[1];\n" +
                "}\n" +
                "Class other::Firm\n" +
                "{\n" +
                "    legalName:String[1];\n" +
                "    otherInformation:String[1];\n" +
                "}\n" +
                "###Relational\n" +
                "Database mapping::db(\n" +
                "   Table employeeFirmDenormTable\n" +
                "   (\n" +
                "    id INT PRIMARY KEY,\n" +
                "    name VARCHAR(200),\n" +
                "    firmId INT,\n" +
                "    legalName VARCHAR(200),\n" +
                "    address1 VARCHAR(200),\n" +
                "    postcode VARCHAR(10)\n" +
                "   )\n" +
                "   Table FirmInfoTable\n" +
                "   (\n" +
                "    id INT PRIMARY KEY,\n" +
                "    name VARCHAR(200),\n" +
                "    other VARCHAR(200)\n" +
                "   )\n" +
                "   Join PersonFirmJoin(employeeFirmDenormTable.firmId = FirmInfoTable.id)\n" +
                ")\n" +
                "###Mapping\n" +
                "import other::*;\n" +
                "import mapping::*;\n" +
                "Mapping mappingPackage::myMapping\n" +
                "(\n" +
                "    Firm[firm1]: Relational\n" +
                "    {\n" +
                "       legalName : [db]FirmInfoTable.name ,\n" +
                "       otherInformation: [db]FirmInfoTable.other\n" +
                "    }\n" +
                "    Person: Relational\n" +
                "    {\n" +
                "        name : [db]employeeFirmDenormTable.name,\n" +
                "        firm\n" +
                "        (\n" +
                "            ~primaryKey ([db]employeeFirmDenormTable.legalName)\n" +
                "            legalName : [db]employeeFirmDenormTable.legalName\n" +
                "        ) Otherwise ( [firm1]:[db]@PersonFirmJoin) \n" +
                "    }\n" +
                ")\n");
    }

    @Test
    public void inlinedEmbeddedMappingsCanBeReferencedInAssociationMappingAsSource()
    {
        test("import other::*;\n" +
                "\n" +
                "Class other::Person\n" +
                "{\n" +
                "    name:String[1];\n" +
                "    address:Address[1];\n" +
                "    firm:Firm[1];\n" +
                "}\n" +
                "Class other::Firm\n" +
                "{\n" +
                "    legalName:String[1];\n" +
                "}\n" +
                "Class other::Address\n" +
                "{\n" +
                "    line1:String[1];\n" +
                "    postcode:String[1];\n" +
                "}\n" +
                "Association other::Firm_Address\n" +
                "{\n" +
                "    address:Address[0..1];\n" +
                "    firm:Firm[1];\n" +
                "}\n" +
                "###Relational\n" +
                "Database mapping::db(\n" +
                "   Table employeeFirmDenormTable\n" +
                "   (\n" +
                "    id INT PRIMARY KEY,\n" +
                "    name VARCHAR(200),\n" +
                "    firmId INT,\n" +
                "    legalName VARCHAR(200),\n" +
                "    address1 VARCHAR(200),\n" +
                "    postcode VARCHAR(10)\n" +
                "   )\n" +
                "   Join firmAddress(employeeFirmDenormTable.address1 = {target}.address1)\n" +
                ")\n" +
                "###Mapping\n" +
                "import other::*;\n" +
                "import mapping::*;\n" +
                "Mapping mappingPackage::myMapping\n" +
                "(\n" +
                "    Address[a1]: Relational\n" +
                "    {\n" +
                "       line1: [db]employeeFirmDenormTable.address1,\n" +
                "       postcode: [db]employeeFirmDenormTable.postcode\n" +
                "    }\n" +
                "    Person[p1]: Relational\n" +
                "    {\n" +
                "        name : [db]employeeFirmDenormTable.name,\n" +
                "        firm\n" +
                "        (\n" +
                "            ~primaryKey ([db]employeeFirmDenormTable.legalName)\n" +
                "            legalName : [db]employeeFirmDenormTable.legalName\n" +
                "        )\n" +
                "    }\n" +
                "    Firm[f1]: Relational\n" +
                "    {\n" +
                "         legalName : [db]employeeFirmDenormTable.legalName\n" +
                "    }\n" +
                "    Firm_Address: Relational\n" +
                "    {\n" +
                "        AssociationMapping\n" +
                "        (\n" +
                "           address[p1_firm,a1] : [db]@firmAddress\n" +
                "        )\n" +
                "    }\n" +
                ")\n");
    }

    @Test
    public void embeddedMappingForMilestonedProperties()
    {
        test("###Relational\n" +
                "Database my::db\n" +
                "(\n" +
                "  Table temporalTable\n" +
                "  (\n" +
                "    milestoning(business(BUS_FROM=from, BUS_THRU=thru, INFINITY_DATE=%9999-12-31))\n" +
                "    col1 INTEGER PRIMARY KEY,\n" +
                "    col2 INTEGER PRIMARY KEY,\n" +
                "    from DATE,\n" +
                "    thru DATE\n" +
                "  )\n" +
                ")\n" +
                " \n" +
                "###Pure\n" +
                "Class <<temporal.businesstemporal>> my::Class1\n" +
                "{\n" +
                "  prop1: Integer[1];\n" +
                "  temporalProp: my::Class2[1];\n" +
                "}\n" +
                " \n" +
                "Class <<temporal.businesstemporal>> my::Class2\n" +
                "{\n" +
                "  prop2: Integer[1];\n" +
                "}\n" +
                " \n" +
                "###Mapping\n" +
                "Mapping my::map\n" +
                "(\n" +
                "  my::Class1 : Relational {\n" +
                "    ~mainTable [my::db]temporalTable\n" +
                "    prop1  : [my::db]temporalTable.col1,\n" +
                "    temporalProp(\n" +
                "      prop2 : [my::db]temporalTable.col2\n" +
                "    )\n" +
                "  }\n" +
                ") \n");
    }

}
