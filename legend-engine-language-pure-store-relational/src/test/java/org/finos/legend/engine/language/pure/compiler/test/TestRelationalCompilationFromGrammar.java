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

import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RootRelationalInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Column;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.RelationalOperationElement;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.TableAliasColumn;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.Table;
import org.junit.Assert;
import org.junit.Test;

public class TestRelationalCompilationFromGrammar extends TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite
{
    public static String DB = "###Relational\n" +
            "Database model::relational::tests::db\n" +
            "(\n" +
            "    include model::relational::tests::dbInc\n" +
            "\n" +
            "    Table interactionTable(ID INT PRIMARY KEY, sourceId INT, targetId INT, time INT, active VARCHAR(1))\n" +
            "    Table tradeTable(ID INT PRIMARY KEY, prodId INT, accountID INT, quantity FLOAT, tradeDate DATE, settlementDateTime TIMESTAMP)\n" +
            "    Table accountTable(ID INT PRIMARY KEY, name VARCHAR(200), createDate DATE)\n" +
            "    Table tradeEventTable(EVENT_ID INT PRIMARY KEY, trade_id INT, eventType VARCHAR(10), eventDate DATE, person_id INT)\n" +
            "    Table orderTable(ID INT PRIMARY KEY, prodId INT, accountID INT, quantity INT, orderDate DATE, settlementDateTime TIMESTAMP)\n" +
            "    Table orderFactTable( ORDER_ID INT PRIMARY KEY, fact FLOAT,from_z DATE,thru_z DATE )\n" +
            "    Table salesPersonTable(PERSON_ID INT PRIMARY KEY, ACCOUNT_ID INT PRIMARY KEY, NAME VARCHAR(200), from_z DATE, thru_z DATE)\n" +
            "    Table otherNamesTable(PERSON_ID INT, OTHER_NAME VARCHAR(200))\n" +
            "\n" +
            "\n" +
            "    View interactionViewMaxTime  \n" +
            "    (\n" +
            "       ~filter PositiveInteractionTimeFilter\n" +
            "       ~groupBy (interactionTable.sourceId, interactionTable.targetId)\n" +
            "       sourceId : interactionTable.sourceId,\n" +
            "       targetId : interactionTable.targetId,\n" +
            "       maxTime : max(interactionTable.time)\n" +
            "    )\n" +
            "\n" +
            "    View tradeEventViewMaxTradeEventDate\n" +
            "    (\n" +
            "       ~groupBy (tradeEventTable.trade_id)\n" +
            "       trade_id : tradeEventTable.trade_id,\n" +
            "       maxTradeEventDate : max(tradeEventTable.eventDate)\n" +
            "    )\n" +
            "\n" +
            "    View orderFactView\n" +
            "    (\n" +
            "       ~filter NonNegativeFactFilter\n" +
            "       ~distinct\n" +
            "        ORDER_ID: orderFactTable.ORDER_ID PRIMARY KEY,\n" +
            "        fact: orderFactTable.fact,\n" +
            "        accountId : @OrderFactTable_Order > @Order_Account | accountTable.ID,\n" +
            "        supportContact : @OrderFactTable_Order > @Order_SalesPerson | salesPersonTable.NAME,\n" +
            "        supportContactId : @OrderFactTable_Order > @Order_SalesPerson | salesPersonTable.PERSON_ID\n" +
            "    )\n" +
            "\n" +
            "    View orderFactViewOnView\n" +
            "    (\n" +
            "        ORDER_ID: orderFactView.ORDER_ID PRIMARY KEY,\n" +
            "        fact: orderFactView.fact\n" +
            "    )\n" +
            "\n" +
            "    View orderNegativeFactView\n" +
            "    (\n" +
            "       ~filter LessThanEqualZeroFactFilter\n" +
            "       ~distinct\n" +
            "        ORDER_ID: orderFactTable.ORDER_ID PRIMARY KEY,\n" +
            "        fact: orderFactTable.fact,\n" +
            "        accountId : @OrderFactTable_Order > @Order_Account | accountTable.ID,\n" +
            "        supportContact : @OrderFactTable_Order > @Order_SalesPerson | salesPersonTable.NAME,\n" +
            "        supportContactId : @OrderFactTable_Order > @Order_SalesPerson | salesPersonTable.PERSON_ID\n" +
            "    )\n" +
            "\n" +
            "    View orderNegativeFactViewOnView\n" +
            "    (\n" +
            "        ORDER_ID: orderNegativeFactView.ORDER_ID PRIMARY KEY,\n" +
            "        fact: orderNegativeFactView.fact\n" +
            "    )\n" +
            "\n" +
            "    View accountOrderFactView\n" +
            "    (\n" +
            "       ~groupBy (orderTable.accountID)\n" +
            "        accountId : orderTable.accountID PRIMARY KEY,\n" +
            "        orderFact : sum(@OrderFactTable_Order | orderFactTable.fact)\n" +
            "    )\n" +
            "\n" +
            "    Schema productSchema\n" +
            "    (\n" +
            "       Table synonymTable(ID INT PRIMARY KEY, PRODID INT, TYPE VARCHAR(200), NAME VARCHAR(200))\n" +
            "    )\n" +
            "\n" +
            "    Filter PositiveInteractionTimeFilter(interactionTable.time > 0)\n" +
            "    Filter ProductSynonymFilter(productSchema.synonymTable.ID != 1)\n" +
            "    Filter NonNegativeFactFilter(orderFactTable.fact > 0)\n" +
            "    Filter LessThanEqualZeroFactFilter(orderFactTable.fact <= 0)\n" +
            "\n" +
            "    Join Product_Synonym(productSchema.synonymTable.PRODID = productSchema.productTable.ID)\n" +
            "    Join Trade_Product(tradeTable.prodId = productSchema.productTable.ID)\n" +
            "    Join Trade_Account(tradeTable.accountID = accountTable.ID)\n" +
            "    Join Interaction_Source(interactionTable.sourceId = personTable.ID)\n" +
            "    Join Interaction_Target(interactionTable.targetId = personTable.ID)\n" +
            "    Join InteractionTable_InteractionViewMaxTime(interactionTable.sourceId = interactionViewMaxTime.sourceId and interactionTable.targetId = interactionViewMaxTime.targetId)\n" +
            "    Join Trade_TradeEvent(tradeTable.ID = tradeEventTable.trade_id)\n" +
            "    Join Trade_TradeEventViewMaxTradeEventDate(tradeTable.ID = tradeEventViewMaxTradeEventDate.trade_id)\n" +
            "    Join TradeEvent_Person(tradeEventTable.person_id = personTable.ID)\n" +
            "    Join Interaction_Interaction(interactionTable.sourceId = {target}.sourceId and interactionTable.targetId = {target}.targetId)\n" +
            "    Join Order_SalesPerson(orderTable.accountID = salesPersonTable.ACCOUNT_ID)\n" +
            "    Join Order_Account(orderTable.accountID = accountTable.ID)\n" +
            "    Join OrderFactView_Order(orderFactView.ORDER_ID = orderTable.ID)\n" +
            "    Join OrderFactViewOnView_Order(orderFactViewOnView.ORDER_ID = orderTable.ID)\n" +
            "    Join OrderNegativeFactView_Order(orderNegativeFactView.ORDER_ID = orderTable.ID)\n" +
            "    Join OrderNegativeFactViewOnView_Order(orderNegativeFactViewOnView.ORDER_ID = orderTable.ID)\n" +
            "    Join OrderFactView_Person(orderFactView.supportContactId = personTable.ID)\n" +
            "    Join SalesPerson_PersonView(salesPersonTable.PERSON_ID = PersonFirmView.PERSON_ID)\n" +
            "    Join OrderFactTable_Order(orderFactTable.ORDER_ID = orderTable.ID)\n" +
            "    Join AccountFactView_Account(accountOrderFactView.accountId = accountTable.ID)\n" +
            "    Join Person_OtherNames(personTable.ID = otherNamesTable.PERSON_ID)\n" +
            ")\n\n";

    public static String DB_INC = "###Relational\n" +
            "Database model::relational::tests::dbInc\n" +
            "(\n" +
            "    Table personTable (ID INT PRIMARY KEY, FIRSTNAME VARCHAR(200), LASTNAME VARCHAR(200), AGE INT, ADDRESSID INT, FIRMID INT, MANAGERID INT)\n" +
            "    Table differentPersonTable (ID INT PRIMARY KEY, FIRSTNAME VARCHAR(200), LASTNAME VARCHAR(200), AGE INT, ADDRESSID INT, FIRMID INT, MANAGERID INT)\n" +
            "    \n" +
            "    Table firmTable(ID INT PRIMARY KEY, LEGALNAME VARCHAR(200), ADDRESSID INT, CEOID INT)\n" +
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
            "\n" +
            ")\n\n";

    String MODEL = "\n" +
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
            "}\n\n";

    @Override
    public String getDuplicatedElementTestCode()
    {
        return "Class anything::class {}\n" +
                "###Mapping\n" +
                "Mapping anything::somethingelse ()\n" +
                "###Relational\n" +
                "Database anything::somethingelse\n" +
                "(\n" +
                ")";
    }

    @Override
    public String getDuplicatedElementTestExpectedErrorMessage()
    {
        return "COMPILATION error at [5:1-7:1]: Duplicated element 'anything::somethingelse'";
    }

    @Test
    public void testRelationalDatabase()
    {
        test(DB_INC);
        test(DB + DB_INC);
    }

    @Test
    public void testSelfJoin()
    {
        test("###Relational\n" + "Database app::dbInc\n" +
                "(\n" +
                "    Table personTable (ID INT PRIMARY KEY, MANAGERID INT)\n" +
                "    Join Person_Manager(personTable.MANAGERID = {target}.ID)\n" +
                ")");
    }

    @Test
    public void testMissingJoin()
    {
        test("###Relational\n" +
                "Database app::dbInc\n" +
                "(\n" +
                "    Table tradeEventTable(EVENT_ID INT PRIMARY KEY, trade_id INT, eventType VARCHAR(10), eventDate DATE, person_id INT)\n" +
                "    View tradeEventViewMaxTradeEventDate\n" +
                "    (\n" +
                "       ~groupBy (tradeEventTable.trade_id)\n" +
                "       trade_id : @MissingJoin | tradeEventTable.trade_id,\n" +
                "       maxTradeEventDate : max(tradeEventTable.eventDate)\n" +
                "    )\n" +
                ")", "COMPILATION error at [8:19-30]: Can't find join 'MissingJoin' in database 'dbInc'");
    }

    @Test
    public void testFaultyDb()
    {
        test("###Relational\n" +
                        "Database model::myDb\n" +
                        "(\n" +
                        "    include model::relational::tests::dbInc\n" +
                        "    Table personTable (ID INT PRIMARY KEY, MANAGERID INT)\n" +
                        ")",
                "COMPILATION error at [2:1-6:1]: Can't find database 'model::relational::tests::dbInc'"
        );

        test("###Relational\n" +
                        "Database model::relational::tests::dbInc\n" +
                        "(\n" +
                        "    Table personTable (ID INT PRIMARY KEY, FIRMID INT)\n" +
                        "    Table firmTable(ID INT PRIMARY KEY)\n" +
                        "    Join Firm_Person(missingSchema.firmTable.ID = personTable.FIRMID)\n" +
                        "\n" +
                        ")",
                "COMPILATION error at [6:22-44]: Can't find schema 'missingSchema' in database 'dbInc'");

        test("###Relational\n" +
                        "Database model::relational::tests::dbInc\n" +
                        "(\n" +
                        "    Table personTable (ID INT PRIMARY KEY, FIRMID INT)\n" +
                        "    Table firmTable(ID INT PRIMARY KEY)\n" +
                        "    Join Firm_Person(missingTable.ID = personTable.FIRMID)\n" +
                        "\n" +
                        ")",
                "COMPILATION error at [6:22-33]: Can't find table 'missingTable' in schema 'default' and database 'dbInc'");

        test("###Relational\n" +
                        "Database model::relational::tests::dbInc\n" +
                        "(\n" +
                        "    Table personTable (ID INT PRIMARY KEY, FIRMID INT)\n" +
                        "    Table firmTable(ID INT PRIMARY KEY)\n" +
                        "    Join Firm_Person(firmTable.ID = personTable.FIRMID_MISSING)\n" +
                        "\n" +
                        ")",
                "COMPILATION error at [6:37-62]: Can't find column 'FIRMID_MISSING'");
    }

    @Test
    public void testRelationalMapping()
    {
        test("Class simple::Person\n" +
                "{\n" +
                "  firstName: String[1];\n" +
                "  lastName: String[1];\n" +
                "  age: Integer[1];\n" +
                "}\n" +
                "\n" +
                "Class simple::Firm\n" +
                "{\n" +
                "  employees: simple::Person[*];\n" +
                "  legalName: String[1];\n" +
                "}\n" +
                "\n" +
                "Enum simple::GeographicEntityType\n" +
                "{\n" +
                "  CITY,\n" +
                "  COUNTRY,\n" +
                "  REGION\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Relational\n" +
                "Database simple::dbInc\n" +
                "(\n" +
                "  Table personTable\n" +
                "  (\n" +
                "    ID INTEGER PRIMARY KEY,\n" +
                "    FIRSTNAME VARCHAR(200),\n" +
                "    LASTNAME VARCHAR(200),\n" +
                "    AGE INTEGER,\n" +
                "    ADDRESSID INTEGER,\n" +
                "    FIRMID INTEGER,\n" +
                "    MANAGERID INTEGER\n" +
                "  )\n" +
                "  Table firmTable\n" +
                "  (\n" +
                "    ID INTEGER PRIMARY KEY,\n" +
                "    LEGALNAME VARCHAR(200),\n" +
                "    ADDRESSID INTEGER,\n" +
                "    CEOID INTEGER\n" +
                "  )\n" +
                "  Join Firm_Person(firmTable.ID = personTable.FIRMID)\n" +
                ")\n" +
                "\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping simple::simpleRelationalMappingInc\n" +
                "(\n" +
                "  simple::Person[simple_Person]: Relational\n" +
                "  {\n" +
                "    ~mainTable [simple::dbInc]personTable\n" +
                "    firstName: [simple::dbInc]personTable.FIRSTNAME,\n" +
                "    lastName: [simple::dbInc]personTable.LASTNAME,\n" +
                "    age: [simple::dbInc]personTable.AGE\n" +
                "  }\n" +
                "  simple::Firm[simple_Firm]: Relational\n" +
                "  {\n" +
                "    ~mainTable [simple::dbInc]firmTable\n" +
                "    legalName: [simple::dbInc]firmTable.LEGALNAME,\n" +
                "    employees: [simple::dbInc]@Firm_Person\n" +
                "  }\n" +
                "\n" +
                "  simple::GeographicEntityType: EnumerationMapping GE\n" +
                "  {\n" +
                "    CITY: [1]\n" +
                "  }\n" +
                ")");

        // user has not defined mainTable
        test("Class model::Person {\n" +
                "   name:String[1];\n" +
                "}\n" +
                "###Relational\n" +
                "Database model::store::db\n" +
                "(\n" +
                "   Table myTable(name VARCHAR(200))\n" +
                ")\n" +
                "###Mapping\n" +
                "Mapping \n" +
                "model::mapping::myMap\n" +
                "(\n" +
                "   model::Person: Relational\n" +
                "   {\n" +
                "      name : [model::store::db]myTable.name\n" +
                "   }\n" +
                ")"
        );

        // property in supertype
        test(MODEL + DB_INC +
                "###Mapping\n" +
                "Mapping model::myRelationalMapping\n" +
                "(\n" +
                "   \n" +
                "  model::Firm : Relational\n" +
                "  {\n" +
                "    name : [model::relational::tests::dbInc]firmTable.LEGALNAME,\n" +
                "    address : [model::relational::tests::dbInc]firmTable.ADDRESSID\n" +
                "  }\n" +
                ")"
        );

        // association property
        test(MODEL +
                "Association model::Employment\n" +
                "{\n" +
                "    firm : model::Firm[0..1];\n" +
                "    employees : model::Person[*];\n" +
                "}\n" + DB_INC +
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
                ")"
        );

        // association mapping on milestoned models
        test("\n" +
                "Class model::Firm\n" +
                "{\n" +
                "   legalName: String[1];\n" +
                "}\n\n" +
                "Class <<temporal.businesstemporal>> model::Person \n" +
                "{\n" +
                "    name : String[1];\n" +
                "}\n" +
                "Association model::Employment\n" +
                "{\n" +
                "    firm : model::Firm[0..1];\n" +
                "    employees : model::Person[*];\n" +
                "}\n" + DB_INC +
                "###Mapping\n" +
                "Mapping model::myRelationalMapping\n" +
                "(\n" +
                "  model::Person: Relational\n" +
                "  {\n" +
                "    name: [model::relational::tests::dbInc]personTable.FIRSTNAME\n" +
                "  }\n" +
                "  model::Firm: Relational\n" +
                "  {\n" +
                "    legalName: [model::relational::tests::dbInc]firmTable.LEGALNAME\n" +
                "  }\n" +
                "  model::Employment : Relational\n" +
                "  {\n" +
                "     AssociationMapping\n" +
                "     (\n" +
                "        employees : [model::relational::tests::dbInc]@Firm_Person,\n" +
                "        firm : [model::relational::tests::dbInc]@Firm_Person\n" +
                "     )\n" +
                "  }" +
                ")"
        );

        // Mapping on milestoned properties
        test("\n" +
                "Class model::Firm\n" +
                "{\n" +
                "   legalName: String[1];\n" +
                "}\n\n" +
                "Class <<temporal.businesstemporal>> model::Person \n" +
                "{\n" +
                "    name : String[1];\n" +
                "}\n" +
                "Association model::Employment\n" +
                "{\n" +
                "    firm : model::Firm[0..1];\n" +
                "    employees : model::Person[*];\n" +
                "}\n" + DB_INC +
                "###Mapping\n" +
                "Mapping model::myRelationalMapping\n" +
                "(\n" +
                "  model::Person: Relational\n" +
                "  {\n" +
                "    name: [model::relational::tests::dbInc]personTable.FIRSTNAME,\n" +
                "    firm: [model::relational::tests::dbInc]@Firm_Person\n" +
                "  }\n" +
                "  model::Firm: Relational\n" +
                "  {\n" +
                "    legalName: [model::relational::tests::dbInc]firmTable.LEGALNAME,\n" +
                "    employees: [model::relational::tests::dbInc]@Firm_Person\n" +
                "  }\n" +
                ")"
        );

        // embedded Relational Mapping
        test(MODEL + DB_INC +
                "###Mapping\n" +
                "Mapping model::myRelationalMapping\n" +
                "(\n" +
                "  model::Firm: Relational\n" +
                "  {\n" +
                "    legalName: [model::relational::tests::dbInc]firmTable.LEGALNAME,\n" +
                "    employee(\n" +
                "firstName: [model::relational::tests::dbInc]@Firm_Person| personTable.FIRSTNAME\n" +
                "    )\n" +
                "  }\n" +
                ")"
        );

        // self join in property mapping
        test("Class model::Person\n" +
                "{\n" +
                "   name:String[1];\n" +
                "   firm:model::Firm[1];\n" +
                "   other:model::Other[1];\n" +
                "}\n" +
                "Class model::Firm\n" +
                "{\n" +
                "   name:String[1];\n" +
                "}\n" +
                "Class model::Other\n" +
                "{\n" +
                "   name:String[1];\n" +
                "}\n"+
                "###Relational\n" +
                "Database model::db\n" +
                "(\n" +
                "   Table personTb(name VARCHAR(200), firm VARCHAR(200))\n" +
                "   Table firmTb(name VARCHAR(200))\n" +
                "   Table otherTb(name VARCHAR(200))\n" +
                "   Table otherTb2(name VARCHAR(200))\n" +
                "   Join myJoin(personTb.firm = otherTb.name)\n" +
                "   Join selfJoin(otherTb.name = {target}.name)\n" +
                "   Join otherJoin(otherTb.name = firmTb.name)\n" +
                ")\n"+
                "###Mapping\n" +
                "Mapping model::myMap\n" +
                "(\n" +
                "    model::Firm: Relational\n" +
                "          {\n" +
                "             name : [model::db]firmTb.name\n" +
                "          }\n" +
                "    model::Person: Relational\n" +
                "            {\n" +
                "                firm:[model::db]@myJoin > @selfJoin > @otherJoin,\n" +
                "                name:[model::db]personTb.name\n" +
                "            }\n" +
                ")\n");
    }

    @Test
    public void testFaultyRelationalMapping()
    {
        test("Class model::Person {\n" +
                "   name:String[1];\n" +
                "}\n" +
                "###Mapping\n" +
                "Mapping \n" +
                "model::mapping::myMap\n" +
                "(\n" +
                "   model::Person: Relational\n" +
                "   {\n" +
                "      name : [model::store::db]myTable.name\n" +
                "   }\n" +
                ")", "COMPILATION error at [10:32-38]: Can't find store 'model::store::db'"
        );

        test(MODEL + DB_INC +
                "###Mapping\n" +
                "Mapping model::myRelationalMapping\n" +
                "(\n" +
                "   \n" +
                "  model::Firm : Relational\n" +
                "  {\n" +
                "    name : [model::relational::tests::dbInc]firmTable.LEGALNAME,\n" +
                "    propertyMissing : [model::relational::tests::dbInc]firmTable.ADDRESSID\n" +
                "  }\n" +
                ")", "COMPILATION error at [88:21-74]: Can't find property 'propertyMissing' in [Firm, LegalEntity, Any]"
        );

        // missing association
        test("###Mapping\n" +
                "Mapping model::myRelationalMapping\n" +
                "(\n" +
                "  MissingAssociation: Relational\n" +
                "  {\n" +
                "    AssociationMapping\n" +
                "    (\n" +
                "      location: [dbInc]@location_PlaceOfInterest,\n" +
                "      placeOfInterest: [dbInc]@location_PlaceOfInterest\n" +
                "    )\n" +
                "  }\n\n" +
                ")", "COMPILATION error at [4:3-11:3]: Can't find association 'MissingAssociation'"
        );

        // missing association property
        test(MODEL +
                "Association model::Employment\n" +
                "{\n" +
                "    firm : model::Firm[0..1];\n" +
                "    employees : model::Person[*];\n" +
                "}\n" + DB_INC +
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
                "         missingAssociationProperty : [model::relational::tests::dbInc]@Firm_Person,\n" +
                "         firm : [model::relational::tests::dbInc]@Firm_Person\n" +
                "      )\n" +
                "   }" +
                ")", "COMPILATION error at [103:10-35]: Can't find property 'missingAssociationProperty' in association 'model::Employment'"
        );

        // Property Mapping join does not have mainTable
        test("Class model::Person\n" +
                "{\n" +
                "   name:String[1];\n" +
                "   firm:model::Firm[1];\n" +
                "   other:model::Other[1];\n" +
                "}\n" +
                "Class model::Firm\n" +
                "{\n" +
                "   name:String[1];\n" +
                "}\n" +
                "Class model::Other\n" +
                "{\n" +
                "   name:String[1];\n" +
                "}\n"+
                "###Relational\n" +
                "Database model::db\n" +
                "(\n" +
                "   Table personTb(name VARCHAR(200), firm VARCHAR(200))\n" +
                "   Table firmTb(name VARCHAR(200))\n" +
                "   Table otherTb(name VARCHAR(200))\n" +
                "   Table otherTb2(name VARCHAR(200))\n" +
                "   Join myJoin(otherTb2.name = otherTb.name)\n" +
                "   Join otherJoin(otherTb2.name = firmTb.name)\n" +
                ")\n"+
                "###Mapping\n" +
                "Mapping model::myMap\n" +
                "(\n" +
                "    model::Firm: Relational\n" +
                "          {\n" +
                "             name : [model::db]firmTb.name\n" +
                "          }\n" +
                "    model::Person: Relational\n" +
                "            {\n" +
                "                firm:[model::db]@myJoin > @otherJoin,\n" +
                "                name:[model::db]personTb.name\n" +
                "            }\n" +
                ")\n", "COMPILATION error at [34:21-52]: Mapping error: the join myJoin does not contain the source table personTb");

        // Invalid join chain
        test("Class model::Person\n" +
                "{\n" +
                "   name:String[1];\n" +
                "   firm:model::Firm[1];\n" +
                "   other:model::Other[1];\n" +
                "}\n" +
                "Class model::Firm\n" +
                "{\n" +
                "   name:String[1];\n" +
                "}\n" +
                "Class model::Other\n" +
                "{\n" +
                "   name:String[1];\n" +
                "}\n"+
                "###Relational\n" +
                "Database model::db\n" +
                "(\n" +
                "   Table personTb(name VARCHAR(200), firm VARCHAR(200))\n" +
                "   Table firmTb(name VARCHAR(200))\n" +
                "   Table otherTb(name VARCHAR(200))\n" +
                "   Table otherTb2(name VARCHAR(200))\n" +
                "   Join myJoin(personTb.firm = otherTb.name)\n" +
                "   Join otherJoin(otherTb2.name = firmTb.name)\n" +
                ")\n"+
                "###Mapping\n" +
                "Mapping model::myMap\n" +
                "(\n" +
                "    model::Firm: Relational\n" +
                "          {\n" +
                "             name : [model::db]firmTb.name\n" +
                "          }\n" +
                "    model::Person: Relational\n" +
                "            {\n" +
                "                firm:[model::db]@myJoin > @otherJoin,\n" +
                "                name:[model::db]personTb.name\n" +
                "            }\n" +
                ")\n", "COMPILATION error at [34:21-52]: Mapping error: the join otherJoin does not contain the source table otherTb");

        // Invalid join chain (with self join)
        test("Class model::Person\n" +
                "{\n" +
                "   name:String[1];\n" +
                "   firm:model::Firm[1];\n" +
                "   other:model::Other[1];\n" +
                "}\n" +
                "Class model::Firm\n" +
                "{\n" +
                "   name:String[1];\n" +
                "}\n" +
                "Class model::Other\n" +
                "{\n" +
                "   name:String[1];\n" +
                "}\n"+
                "###Relational\n" +
                "Database model::db\n" +
                "(\n" +
                "   Table personTb(name VARCHAR(200), firm VARCHAR(200))\n" +
                "   Table firmTb(name VARCHAR(200))\n" +
                "   Table otherTb(name VARCHAR(200))\n" +
                "   Table otherTb2(name VARCHAR(200))\n" +
                "   Join myJoin(personTb.firm = otherTb.name)\n" +
                "   Join selfJoin(otherTb.name = {target}.name)\n" +
                "   Join otherJoin(otherTb2.name = firmTb.name)\n" +
                ")\n"+
                "###Mapping\n" +
                "Mapping model::myMap\n" +
                "(\n" +
                "    model::Firm: Relational\n" +
                "          {\n" +
                "             name : [model::db]firmTb.name\n" +
                "          }\n" +
                "    model::Person: Relational\n" +
                "            {\n" +
                "                firm:[model::db]@myJoin > @selfJoin > @otherJoin,\n" +
                "                name:[model::db]personTb.name\n" +
                "            }\n" +
                ")\n", "COMPILATION error at [35:21-64]: Mapping error: the join otherJoin does not contain the source table otherTb");

        // embedded Relational Mapping
        test(MODEL + DB_INC +
                "###Mapping\n" +
                "Mapping model::myRelationalMapping\n" +
                "(\n" +
                "  model::Firm: Relational\n" +
                "  {\n" +
                "    legalName: [model::relational::tests::dbInc]firmTable.LEGALNAME,\n" +
                "    employee(\n" +
                "       missingEmbeddedProperty: [model::relational::tests::dbInc]@Firm_Person| personTable.FIRSTNAME\n" +
                "    )\n" +
                "  }\n" +
                ")", "COMPILATION error at [88:31-100]: Can't find property 'missingEmbeddedProperty' in [Person, Any]"
        );

        // Incorrect filter
        test(MODEL + DB_INC +
                "###Mapping\n" +
                "Mapping model::myRelationalMapping\n" +
                "(\n" +
                "  model::Firm: Relational\n" +
                "  {\n" +
                "    ~filter [model::relational::tests::dbInc]MissingFilter\n" +
                "    legalName: [model::relational::tests::dbInc]firmTable.LEGALNAME\n" +
                "  }\n" +
                ")", "COMPILATION error at [86:5-58]: Can't find filter 'MissingFilter' in database 'dbInc'"
        );
    }

    @Test
    public void testRelationalMappingProcessing()
    {
        PureModel pureModel = test(MODEL + DB_INC +
                "###Mapping\n" +
                "import model::*;" +
                "Mapping model::myRelationalMapping\n" +
                "(\n" +
                "   \n" +
                "  Firm : Relational\n" +
                "  {\n" +
                "    name : [model::relational::tests::dbInc]firmTable.LEGALNAME,\n" +
                "    address : [model::relational::tests::dbInc]firmTable.ADDRESSID\n" +
                "  }\n" +
                ")").getTwo();
        org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping mmMapping = pureModel.getMapping("model::myRelationalMapping");
        RootRelationalInstanceSetImplementation rootRelationalInstanceSetImplementation = ((RootRelationalInstanceSetImplementation) mmMapping._classMappings().getFirst());
        RelationalOperationElement primaryKey = rootRelationalInstanceSetImplementation._primaryKey().getFirst();
        // mainTable
        Table table = (Table) rootRelationalInstanceSetImplementation._mainTableAlias()._relationalElement();
        Assert.assertEquals(table._name(), "firmTable");
        // primaryKey
        Column col = ((TableAliasColumn) primaryKey)._column();
        Assert.assertEquals(col._name(), "ID");
        Assert.assertEquals(((Table) col._owner())._name(), "firmTable");
        // classMappingId
        Assert.assertEquals(rootRelationalInstanceSetImplementation._id(), "model_Firm");
    }
}
