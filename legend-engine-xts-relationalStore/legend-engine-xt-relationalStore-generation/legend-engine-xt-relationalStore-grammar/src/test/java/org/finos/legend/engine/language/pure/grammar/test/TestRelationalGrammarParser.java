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

package org.finos.legend.engine.language.pure.grammar.test;

import org.antlr.v4.runtime.Vocabulary;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.RelationalParserGrammar;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementType;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.Database;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TestRelationalGrammarParser extends TestGrammarParser.TestGrammarParserTestSuite
{
    @Override
    public Vocabulary getParserGrammarVocabulary()
    {
        return RelationalParserGrammar.VOCABULARY;
    }

    @Override
    public String getParserGrammarIdentifierInclusionTestCode(List<String> keywords)
    {
        return "###Relational\n" +
                "Database " + ListAdapter.adapt(keywords).makeString("::") + "\n" +
                "(\n" +
                ")\n";
    }

    @Test
    public void testRelationalStoreTableColumnType()
    {
        // test unsupported column data type
        test("###Relational\n" +
                "\n" +
                "Database apps::pure::dsb::sample::store::DsbSampleDb\n" +
                "(\n" +
                "   Table ORDERS (\n" +
                "      clientId UNKNOWN\n" +
                "   )\n" +
                ")", "PARSER error at [6:16-22]: Unsupported column data type 'UNKNOWN'");
        // CHAR
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId CHAR\n" +
                ")\n)", "PARSER error at [5:16-19]: Column data type CHAR requires 1 parameter (size) in declaration");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId CHAR(1)\n" +
                ")\n)");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId CHAR(2,2)\n" +
                ")\n)", "PARSER error at [5:16-24]: Column data type CHAR requires 1 parameter (size) in declaration");
        // VARCHAR
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId VARCHAR\n" +
                ")\n)", "PARSER error at [5:16-22]: Column data type VARCHAR requires 1 parameter (size) in declaration");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId VARCHAR(1)\n" +
                ")\n)");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId VARCHAR(2,2)\n" +
                ")\n)", "PARSER error at [5:16-27]: Column data type VARCHAR requires 1 parameter (size) in declaration");
        // BINARY
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId BINARY\n" +
                ")\n)", "PARSER error at [5:16-21]: Column data type BINARY requires 1 parameter (size) in declaration");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId BINARY(1)\n" +
                ")\n)");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId BINARY(2,2)\n" +
                ")\n)", "PARSER error at [5:16-26]: Column data type BINARY requires 1 parameter (size) in declaration");
        // VARBINARY
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId VARBINARY\n" +
                ")\n)", "PARSER error at [5:16-24]: Column data type VARBINARY requires 1 parameter (size) in declaration");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId VARBINARY(1)\n" +
                ")\n)");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId VARBINARY(2,2)\n" +
                ")\n)", "PARSER error at [5:16-29]: Column data type VARBINARY requires 1 parameter (size) in declaration");
        // BIT
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId BIT\n" +
                ")\n)");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId BIT(1)\n" +
                ")\n)", "PARSER error at [5:16-21]: Column data type BIT does not expect any parameters in declaration");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId BIT(2,2)\n" +
                ")\n)", "PARSER error at [5:16-23]: Column data type BIT does not expect any parameters in declaration");
        // INT
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId INT\n" +
                ")\n)");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId INT(1)\n" +
                ")\n)", "PARSER error at [5:16-21]: Column data type INTEGER does not expect any parameters in declaration");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId INT(2,2)\n" +
                ")\n)", "PARSER error at [5:16-23]: Column data type INTEGER does not expect any parameters in declaration");
        // INTEGER
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId INTEGER\n" +
                ")\n)");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId INTEGER(1)\n" +
                ")\n)", "PARSER error at [5:16-25]: Column data type INTEGER does not expect any parameters in declaration");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId INTEGER(2,2)\n" +
                ")\n)", "PARSER error at [5:16-27]: Column data type INTEGER does not expect any parameters in declaration");
        // BIGINT
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId BIGINT\n" +
                ")\n)");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId BIGINT(1)\n" +
                ")\n)", "PARSER error at [5:16-24]: Column data type BIGINT does not expect any parameters in declaration");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId BIGINT(2,2)\n" +
                ")\n)", "PARSER error at [5:16-26]: Column data type BIGINT does not expect any parameters in declaration");
        // SMALLINT
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId SMALLINT\n" +
                ")\n)");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId SMALLINT(1)\n" +
                ")\n)", "PARSER error at [5:16-26]: Column data type SMALLINT does not expect any parameters in declaration");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId SMALLINT(2,2)\n" +
                ")\n)", "PARSER error at [5:16-28]: Column data type SMALLINT does not expect any parameters in declaration");
        // TINYINT
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId TINYINT\n" +
                ")\n)");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId TINYINT(1)\n" +
                ")\n)", "PARSER error at [5:16-25]: Column data type TINYINT does not expect any parameters in declaration");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId TINYINT(2,2)\n" +
                ")\n)", "PARSER error at [5:16-27]: Column data type TINYINT does not expect any parameters in declaration");
        // TIMESTAMP
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId TIMESTAMP\n" +
                ")\n)");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId TIMESTAMP(1)\n" +
                ")\n)", "PARSER error at [5:16-27]: Column data type TIMESTAMP does not expect any parameters in declaration");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId TIMESTAMP(2,2)\n" +
                ")\n)", "PARSER error at [5:16-29]: Column data type TIMESTAMP does not expect any parameters in declaration");
        // DATE
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId DATE\n" +
                ")\n)");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId DATE(1)\n" +
                ")\n)", "PARSER error at [5:16-22]: Column data type DATE does not expect any parameters in declaration");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId DATE(2,2)\n" +
                ")\n)", "PARSER error at [5:16-24]: Column data type DATE does not expect any parameters in declaration");
        // NUMERIC
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId NUMERIC\n" +
                ")\n)", "PARSER error at [5:16-22]: Column data type NUMERIC requires 2 parameters (precision, scale) in declaration");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId NUMERIC(1)\n" +
                ")\n)", "PARSER error at [5:16-25]: Column data type NUMERIC requires 2 parameters (precision, scale) in declaration");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId NUMERIC(2,2)\n" +
                ")\n)");
        // DECIMAL
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId DECIMAL\n" +
                ")\n)", "PARSER error at [5:16-22]: Column data type DECIMAL requires 2 parameters (precision, scale) in declaration");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId DECIMAL(1)\n" +
                ")\n)", "PARSER error at [5:16-25]: Column data type DECIMAL requires 2 parameters (precision, scale) in declaration");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId DECIMAL(2,2)\n" +
                ")\n)");
        // FLOAT
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId FLOAT\n" +
                ")\n)");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId FLOAT(1)\n" +
                ")\n)", "PARSER error at [5:16-23]: Column data type FLOAT does not expect any parameters in declaration");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId FLOAT(2,2)\n" +
                ")\n)", "PARSER error at [5:16-25]: Column data type FLOAT does not expect any parameters in declaration");
        // DOUBLE
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId DOUBLE\n" +
                ")\n)");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId DOUBLE(1)\n" +
                ")\n)", "PARSER error at [5:16-24]: Column data type DOUBLE does not expect any parameters in declaration");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId DOUBLE(2,2)\n" +
                ")\n)", "PARSER error at [5:16-26]: Column data type DOUBLE does not expect any parameters in declaration");
        // REAL
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId REAL\n" +
                ")\n)");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId REAL(1)\n" +
                ")\n)", "PARSER error at [5:16-22]: Column data type REAL does not expect any parameters in declaration");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId REAL(2,2)\n" +
                ")\n)", "PARSER error at [5:16-24]: Column data type REAL does not expect any parameters in declaration");
        // ARRAY
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId ARRAY\n" +
                ")\n)");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId ARRAY(1)\n" +
                ")\n)", "PARSER error at [5:16-23]: Column data type ARRAY does not expect any parameters in declaration");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId ARRAY(2,2)\n" +
                ")\n)", "PARSER error at [5:16-25]: Column data type ARRAY does not expect any parameters in declaration");
        // OTHER
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId OTHER\n" +
                ")\n)");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId OTHER(1)\n" +
                ")\n)", "PARSER error at [5:16-23]: Column data type OTHER does not expect any parameters in declaration");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId OTHER(2,2)\n" +
                ")\n)", "PARSER error at [5:16-25]: Column data type OTHER does not expect any parameters in declaration");
    }


    @Test
    public void testIncludedDatabase()
    {
        PureModelContextData pureModelContextData = test("###Relational\n" +
                "Database example::database\n" +
                "(\n" +
                "  include example::databaseInc\n" +
                "\n" +
                "  Schema exampleRoot\n" +
                "  (\n" +
                "    Table TableC\n" +
                "    (\n" +
                "      name VARCHAR(255),\n" +
                "      id INTEGER PRIMARY KEY\n" +
                "    )\n" +
                "\n" +
                "    View dbView\n" +
                "    (\n" +
                "      nameL :exampleSub.TableASub.name,\n" +
                "      rootTable: [example::databaseInc]@AtoB > [example::database] @BtoC |exampleRoot.TableC.name\n" +
                "    )\n" +
                "    View dbViewIncTable\n" +
                "    (\n" +
                "      nameL :exampleRoot.TableC.name,\n" +
                "      incTable: [example::database]@BtoC > [example::databaseInc]@AtoB |exampleSub.TableASub.name\n" +
                "    )\n" +
                "  )\n" +
                "\n" +
                "  Join BtoC([example::databaseInc]exampleSub.TableBSub.id = exampleRoot.TableC.id)\n" +
                ")\n" +
                "\n" +
                "Database example::databaseInc\n" +
                "(\n" +
                "  Schema exampleSub\n" +
                "  (\n" +
                "    Table TableASub\n" +
                "    (\n" +
                "      name VARCHAR(255),\n" +
                "      id INTEGER PRIMARY KEY\n" +
                "    )\n" +
                "    Table TableBSub\n" +
                "    (\n" +
                "      name VARCHAR(255),\n" +
                "      id INTEGER PRIMARY KEY\n" +
                "    )\n" +
                "\n" +
                "  )\n" +
                "\n" +
                "  Join AtoB(exampleSub.TableASub.id = exampleSub.TableBSub.id)\n" +
                ")\n");

        Map<String, PackageableElement> elementMap = pureModelContextData.getElements().stream().collect(Collectors.toMap(x -> x.getPath(), Function.identity()));
        Database database = (Database) elementMap.get("example::database");

        Assert.assertEquals(1, database.includedStores.size());
        Assert.assertEquals(PackageableElementType.STORE, database.includedStores.get(0).type);
        Assert.assertEquals("example::databaseInc", database.includedStores.get(0).path);
        Assert.assertNotNull(database.includedStores.get(0).sourceInformation);
        Assert.assertEquals(4, database.includedStores.get(0).sourceInformation.startLine);
        Assert.assertEquals(4, database.includedStores.get(0).sourceInformation.endLine);
        Assert.assertEquals(3, database.includedStores.get(0).sourceInformation.startColumn);
        Assert.assertEquals(30, database.includedStores.get(0).sourceInformation.endColumn);
    }

    @Test
    public void testRelationalMappingSourceInformation()
    {
        PureModelContextData pureModelContextData = test("\n" +
                "Class model::LegalEntity \n" +
                "{\n" +
                "   name: String[1];\n" +
                "}\n" +
                "\n" +
                "Class model::Firm extends model::LegalEntity \n" +
                "{\n" +
                "   legalName: String[1];\n" +
                "   address: Integer[1];\n" +
                "   employee: model::Person[0..1];\n" +
                "}\n\n" +
                "Class model::Person \n" +
                "{\n" +
                "    firstName : String[1];\n" +
                "    lastName : String[1];\n" +
                "    otherNames : String[*];\n" +
                "    age: Integer[1];\n" +
                "}\n\n" +
                "Association model::Employment\n" +
                "{\n" +
                "    firm : model::Firm[0..1];\n" +
                "    employees : model::Person[*];\n" +
                "}\n" +
                "###Relational\n" +
                "Database model::relational::tests::dbInc\n" +
                "(\n" +
                "    Table personTable (ID INT PRIMARY KEY, FIRSTNAME VARCHAR(200), LASTNAME VARCHAR(200), AGE INT, ADDRESSID INT, FIRMID INT, MANAGERID INT)\n" +
                "    Table differentPersonTable (ID INT PRIMARY KEY, FIRSTNAME VARCHAR(200), LASTNAME VARCHAR(200), AGE INT, ADDRESSID INT, FIRMID INT, MANAGERID INT)\n" +
                "    \n" +
                "    Table firmTable(ID INT PRIMARY KEY, LEGALNAME VARCHAR(200), ADDRESSID INT, CEOID INT)\n" +
                "    Table PersonToFirm(PERSONID INT PRIMARY KEY, FIRMID INT PRIMARY KEY)\n" +

                "    Table otherFirmTable(ID INT PRIMARY KEY, LEGALNAME VARCHAR(200), ADDRESSID INT)\n" +
                "    \n" +
                "    Table addressTable(ID INT PRIMARY KEY, TYPE INT, NAME VARCHAR(200), STREET VARCHAR(100), COMMENTS VARCHAR(100))\n" +
                "    Table locationTable(ID INT PRIMARY KEY, PERSONID INT, PLACE VARCHAR(200),date DATE)\n" +
                "    Table placeOfInterestTable(ID INT PRIMARY KEY,locationID INT PRIMARY KEY, NAME VARCHAR(200))  \n" +
                "\n" +
                "    View PersonFirmView\n" +
                "    (\n" +
                "        ~filter LastNameFilter\n" +
                "        PERSON_ID: personTable.ID PRIMARY KEY, \n" +
                "        lastName:  personTable.LASTNAME,\n" +
                "        firm_name :  @Firm_Person | firmTable.LEGALNAME\n" +
                "\n" +
                "   )\n" +
                "   \n" +
                "   View personViewWithGroupBy\n" +
                "   (\n" +
                "      ~groupBy(personTable.ID)\n" +
                "      id: personTable.ID PRIMARY KEY,\n" +
                "      maxage: max(personTable.AGE)\n" +
                "   )\n" +
                "   \n" +
                "    View PersonViewWithDistinct\n" +
                "   (\n" +
                "      ~distinct\n" +
                "      id: @PersonWithPersonView| personTable.ID PRIMARY KEY,\n" +
                "      firstName: @PersonWithPersonView| personTable.FIRSTNAME,\n" +
                "      lastName: @PersonWithPersonView|personTable.LASTNAME, \n" +
                "      firmId: @PersonWithPersonView|personTable.FIRMID\n" +
                "   )\n" +
                "   \n" +
                "    Schema productSchema\n" +
                "    (\n" +
                "       Table productTable(ID INT PRIMARY KEY, NAME VARCHAR(200))\n" +
                "    )\n" +
                "    \n" +
                "    Filter FirmXFilter(firmTable.LEGALNAME = 'Firm X')\n" +
                "    Filter LastNameFilter(personTable.LASTNAME = 'Uyaguari')\n" +
                "\n" +
                "    Join personViewWithFirmTable(firmTable.ID = PersonViewWithDistinct.firmId)\n" +
                "    Join PersonWithPersonView(personTable.ID = personViewWithGroupBy.id and personTable.AGE = personViewWithGroupBy.maxage)\n" +
                "    Join Address_Firm(addressTable.ID = firmTable.ADDRESSID)\n" +
                "    Join Address_Person(addressTable.ID = personTable.ADDRESSID = personTable.ADDRESSID)\n" +
                "    Join Firm_Ceo(firmTable.CEOID = personTable.ID)\n" +
                "    Join Firm_Person(firmTable.ID = personTable.FIRMID)\n" +
                "    Join Person_Location(personTable.ID = locationTable.PERSONID)\n" +
                "    Join Person_Manager(personTable.MANAGERID = {target}.ID)\n" +
                "    Join location_PlaceOfInterest(locationTable.ID  = placeOfInterestTable.locationID)\n" +
                "    Join Person_OtherFirm(personTable.FIRMID = otherFirmTable.ID)\n" +
                "    Join Person_PersonFirm(personTable.ID = PersonToFirm.PERSONID)\n" +
                "    Join Firm_PersonFirm(firmTable.ID = PersonToFirm.FIRMID)\n" +
                "    Join OtherFirm_PersonFirm(otherFirmTable.ID = PersonToFirm.FIRMID)\n" +

                "\n" +
                ")\n\n" +
                "###Mapping\n" +
                "Mapping model::myRelationalMapping\n" +
                "(\n" +
                "  model::Person: Relational\n" +
                "  {\n" +
                "    firstName: [model::relational::tests::dbInc]personTable.FIRSTNAME,\n" +
                "    lastName: [model::relational::tests::dbInc]personTable.LASTNAME,\n" +
                "    age: [model::relational::tests::dbInc]personTable.AGE\n" +
                "  }\n" +
                "  model::Firm: Relational\n" +
                "  {\n" +
                "    legalName: [model::relational::tests::dbInc]firmTable.LEGALNAME\n" +
                "  }\n" +
                "   model::Employment : Relational\n" +
                "   {\n" +
                "      AssociationMapping\n" +
                "      (\n" +
                "         employees : [model::relational::tests::dbInc]@Firm_Person,\n" +
                "         firm : [model::relational::tests::dbInc]@Firm_Person\n" +
                "      )\n" +
                "   }" +
                ")");

        Map<String, PackageableElement> elementMap = pureModelContextData.getElements().stream().collect(Collectors.toMap(x -> x.getPath(), Function.identity()));

        Mapping mapping = (Mapping) elementMap.get("model::myRelationalMapping");

        Assert.assertEquals(1, mapping.associationMappings.size());
        Assert.assertNotNull(mapping.associationMappings.get(0).sourceInformation);

        Assert.assertEquals(103, mapping.associationMappings.get(0).sourceInformation.startLine);
        Assert.assertEquals(110, mapping.associationMappings.get(0).sourceInformation.endLine);
        Assert.assertEquals(4, mapping.associationMappings.get(0).sourceInformation.endColumn);
    }

    @Test
    public void testRelationalMappingAssociation()
    {
        PureModelContextData pureModelContextData = test("\n" +
                "Class model::LegalEntity \n" +
                "{\n" +
                "   name: String[1];\n" +
                "}\n" +
                "\n" +
                "Class model::Firm extends model::LegalEntity \n" +
                "{\n" +
                "   legalName: String[1];\n" +
                "   address: Integer[1];\n" +
                "   employee: model::Person[0..1];\n" +
                "}\n\n" +
                "Class model::Person \n" +
                "{\n" +
                "    firstName : String[1];\n" +
                "    lastName : String[1];\n" +
                "    otherNames : String[*];\n" +
                "    age: Integer[1];\n" +
                "}\n\n" +
                "Association model::Employment\n" +
                "{\n" +
                "    firm : model::Firm[0..1];\n" +
                "    employees : model::Person[*];\n" +
                "}\n" +
                "###Relational\n" +
                "Database model::relational::tests::dbInc\n" +
                "(\n" +
                "    Table personTable (ID INT PRIMARY KEY, FIRSTNAME VARCHAR(200), LASTNAME VARCHAR(200), AGE INT, ADDRESSID INT, FIRMID INT, MANAGERID INT)\n" +
                "    Table differentPersonTable (ID INT PRIMARY KEY, FIRSTNAME VARCHAR(200), LASTNAME VARCHAR(200), AGE INT, ADDRESSID INT, FIRMID INT, MANAGERID INT)\n" +
                "    \n" +
                "    Table firmTable(ID INT PRIMARY KEY, LEGALNAME VARCHAR(200), ADDRESSID INT, CEOID INT)\n" +
                "    Table PersonToFirm(PERSONID INT PRIMARY KEY, FIRMID INT PRIMARY KEY)\n" +

                "    Table otherFirmTable(ID INT PRIMARY KEY, LEGALNAME VARCHAR(200), ADDRESSID INT)\n" +
                "    \n" +
                "    Table addressTable(ID INT PRIMARY KEY, TYPE INT, NAME VARCHAR(200), STREET VARCHAR(100), COMMENTS VARCHAR(100))\n" +
                "    Table locationTable(ID INT PRIMARY KEY, PERSONID INT, PLACE VARCHAR(200),date DATE)\n" +
                "    Table placeOfInterestTable(ID INT PRIMARY KEY,locationID INT PRIMARY KEY, NAME VARCHAR(200))  \n" +
                "\n" +
                "    View PersonFirmView\n" +
                "    (\n" +
                "        ~filter LastNameFilter\n" +
                "        PERSON_ID: personTable.ID PRIMARY KEY, \n" +
                "        lastName:  personTable.LASTNAME,\n" +
                "        firm_name :  @Firm_Person | firmTable.LEGALNAME\n" +
                "\n" +
                "   )\n" +
                "   \n" +
                "   View personViewWithGroupBy\n" +
                "   (\n" +
                "      ~groupBy(personTable.ID)\n" +
                "      id: personTable.ID PRIMARY KEY,\n" +
                "      maxage: max(personTable.AGE)\n" +
                "   )\n" +
                "   \n" +
                "    View PersonViewWithDistinct\n" +
                "   (\n" +
                "      ~distinct\n" +
                "      id: @PersonWithPersonView| personTable.ID PRIMARY KEY,\n" +
                "      firstName: @PersonWithPersonView| personTable.FIRSTNAME,\n" +
                "      lastName: @PersonWithPersonView|personTable.LASTNAME, \n" +
                "      firmId: @PersonWithPersonView|personTable.FIRMID\n" +
                "   )\n" +
                "   \n" +
                "    Schema productSchema\n" +
                "    (\n" +
                "       Table productTable(ID INT PRIMARY KEY, NAME VARCHAR(200))\n" +
                "    )\n" +
                "    \n" +
                "    Filter FirmXFilter(firmTable.LEGALNAME = 'Firm X')\n" +
                "    Filter LastNameFilter(personTable.LASTNAME = 'Uyaguari')\n" +
                "\n" +
                "    Join personViewWithFirmTable(firmTable.ID = PersonViewWithDistinct.firmId)\n" +
                "    Join PersonWithPersonView(personTable.ID = personViewWithGroupBy.id and personTable.AGE = personViewWithGroupBy.maxage)\n" +
                "    Join Address_Firm(addressTable.ID = firmTable.ADDRESSID)\n" +
                "    Join Address_Person(addressTable.ID = personTable.ADDRESSID = personTable.ADDRESSID)\n" +
                "    Join Firm_Ceo(firmTable.CEOID = personTable.ID)\n" +
                "    Join Firm_Person(firmTable.ID = personTable.FIRMID)\n" +
                "    Join Person_Location(personTable.ID = locationTable.PERSONID)\n" +
                "    Join Person_Manager(personTable.MANAGERID = {target}.ID)\n" +
                "    Join location_PlaceOfInterest(locationTable.ID  = placeOfInterestTable.locationID)\n" +
                "    Join Person_OtherFirm(personTable.FIRMID = otherFirmTable.ID)\n" +
                "    Join Person_PersonFirm(personTable.ID = PersonToFirm.PERSONID)\n" +
                "    Join Firm_PersonFirm(firmTable.ID = PersonToFirm.FIRMID)\n" +
                "    Join OtherFirm_PersonFirm(otherFirmTable.ID = PersonToFirm.FIRMID)\n" +

                "\n" +
                ")\n\n" +
                "###Mapping\n" +
                "Mapping model::myRelationalMapping\n" +
                "(\n" +
                "  model::Person: Relational\n" +
                "  {\n" +
                "    firstName: [model::relational::tests::dbInc]personTable.FIRSTNAME,\n" +
                "    lastName: [model::relational::tests::dbInc]personTable.LASTNAME,\n" +
                "    age: [model::relational::tests::dbInc]personTable.AGE\n" +
                "  }\n" +
                "  model::Firm: Relational\n" +
                "  {\n" +
                "    legalName: [model::relational::tests::dbInc]firmTable.LEGALNAME\n" +
                "  }\n" +
                "   model::Employment : Relational\n" +
                "   {\n" +
                "      AssociationMapping\n" +
                "      (\n" +
                "         employees : [model::relational::tests::dbInc]@Firm_Person,\n" +
                "         firm : [model::relational::tests::dbInc]@Firm_Person\n" +
                "      )\n" +
                "   }" +
                ")");

        Map<String, PackageableElement> elementMap = pureModelContextData.getElements().stream().collect(Collectors.toMap(x -> x.getPath(), Function.identity()));

        Mapping mapping = (Mapping) elementMap.get("model::myRelationalMapping");

        Assert.assertEquals(1, mapping.associationMappings.size());
        Assert.assertNotNull(mapping.associationMappings.get(0).association.sourceInformation);

        Assert.assertEquals(103, mapping.associationMappings.get(0).association.sourceInformation.startLine);
        Assert.assertEquals(103, mapping.associationMappings.get(0).association.sourceInformation.endLine);
        Assert.assertEquals(20, mapping.associationMappings.get(0).association.sourceInformation.endColumn);
    }
}
