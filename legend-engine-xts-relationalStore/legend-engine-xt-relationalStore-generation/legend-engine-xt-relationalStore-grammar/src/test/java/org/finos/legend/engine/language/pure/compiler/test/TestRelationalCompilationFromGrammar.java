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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperRelationalBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.Warning;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RootRelationalInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Column;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.RelationalOperationElement;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.TableAliasColumn;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.datatype.Json;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.datatype.SemiStructured;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.Table;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

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
            "       ~filter [model::relational::tests::db] PositiveInteractionTimeFilter\n" +
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
            ")\n\n";

    public static String DB_DUP_INC = "###Relational\n" +
            "Database model::relational::tests::dbInc\n" +
            "(\n" +
            "    Table personTable (ID INT PRIMARY KEY, FIRSTNAME VARCHAR(200) , FIRSTNAME VARCHAR(200), FIRSTNAME VARCHAR(200), LASTNAME VARCHAR(200), LASTNAME VARCHAR(200), AGE INT, ADDRESSID INT, FIRMID INT, MANAGERID INT)\n" +
            "    Table differentPersonTable (ID INT PRIMARY KEY, FIRSTNAME VARCHAR(200), LASTNAME VARCHAR(200), AGE INT, ADDRESSID INT, FIRMID INT, MANAGERID INT)\n" +
            "    \n" +
            "    Table firmTable(ID INT PRIMARY KEY, LEGALNAME VARCHAR(200), LEGALNAME VARCHAR(200), LEGALNAME VARCHAR(200), ADDRESSID INT, ADDRESSID INT, CEOID INT)\n" +
            "    Table PersonToFirm(PERSONID INT PRIMARY KEY, FIRMID INT PRIMARY KEY)\n" +

            "    Table otherFirmTable(ID INT PRIMARY KEY, LEGALNAME VARCHAR(200), LEGALNAME VARCHAR(200), ADDRESSID INT)\n" +
            "    \n" +
            "    Table addressTable(ID INT PRIMARY KEY, TYPE INT, NAME VARCHAR(200), STREET VARCHAR(100), COMMENTS VARCHAR(100))\n" +
            "    Table locationTable(ID INT PRIMARY KEY, PERSONID INT, PLACE VARCHAR(200),date DATE)\n" +
            "    Table placeOfInterestTable(ID INT PRIMARY KEY,locationID INT PRIMARY KEY, NAME VARCHAR(200))  \n" +
            "\n" +
            "   )\n";

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
    public void testRelationalDatabaseFail()
    {
        MutableList<String> warnings = Lists.mutable.empty();
        warnings.add("COMPILATION error at [4:5-212]: Duplicate column definitions [FIRSTNAME, LASTNAME] in table: personTable");
        warnings.add("COMPILATION error at [7:5-152]: Duplicate column definitions [ADDRESSID, LEGALNAME] in table: firmTable");
        warnings.add("COMPILATION error at [9:5-107]: Duplicate column definitions [LEGALNAME] in table: otherFirmTable");
        PureModel dbIncModel = test(DB_DUP_INC, null, warnings).getTwo();

    }

    @Test
    public void testRelationalDatabase()
    {
        PureModel dbIncModel = test(DB_INC).getTwo();
        PureModel dbModel = test(DB + DB_INC).getTwo();

        Database dbInc = (Database) dbIncModel.getStore("model::relational::tests::dbInc");
        Database db = (Database) dbModel.getStore("model::relational::tests::db");

        String[] tablesDb = HelperRelationalBuilder.getAllTables(db, SourceInformation.getUnknownSourceInformation()).collect(x -> x._schema()._name() + '.' + x._name()).toSortedList().toArray(new String[0]);
        String[] productSchemaTablesDb = HelperRelationalBuilder.getAllTablesInSchema(db, "productSchema", SourceInformation.getUnknownSourceInformation()).collect(x -> x._schema()._name() + '.' + x._name()).toSortedList().toArray(new String[0]);
        String[] defaultTablesDbInc = HelperRelationalBuilder.getAllTablesInSchema(dbInc, "default", SourceInformation.getUnknownSourceInformation()).collect(x -> x._schema()._name() + '.' + x._name()).toSortedList().toArray(new String[0]);

        Assert.assertArrayEquals(new String[] {
                "default.PersonToFirm",
                "default.accountTable",
                "default.addressTable",
                "default.differentPersonTable",
                "default.firmTable",
                "default.interactionTable",
                "default.locationTable",
                "default.orderFactTable",
                "default.orderTable",
                "default.otherFirmTable",
                "default.otherNamesTable",
                "default.personTable",
                "default.placeOfInterestTable",
                "default.salesPersonTable",
                "default.tradeEventTable",
                "default.tradeTable",
                "productSchema.productTable",
                "productSchema.synonymTable",
        }, tablesDb);

        Assert.assertArrayEquals(new String[] {
                "default.PersonToFirm",
                "default.addressTable",
                "default.differentPersonTable",
                "default.firmTable",
                "default.locationTable",
                "default.otherFirmTable",
                "default.personTable",
                "default.placeOfInterestTable"
        }, defaultTablesDbInc);

        Assert.assertArrayEquals(new String[] {
                "productSchema.productTable",
                "productSchema.synonymTable"
        }, productSchemaTablesDb);
    }

    @Test
    public void testDatabaseStereotype()
    {
        test("###Pure\n" +
                "Profile meta::pure::profiles::storeType\n" +
                "{\n" +
                "    stereotypes: [type1, type2];\n" +
                "}\n" +
                "\n" +
                "###Relational\n" +
                "Database <<meta::pure::profiles::storeType.type2>> app::dbInc\n" +
                "(\n" +
                "  Table Product\n" +
                "  (\n" +
                "    ProductID VARCHAR(30) PRIMARY KEY\n" +
                "  )\n" +
                ")\n" +
                "\n" +
                "###Relational\n" +
                "Database <<meta::pure::profiles::storeType.type2, meta::pure::profiles::storeType.type1>> app::db\n" +
                "(\n" +
                "  Table Product\n" +
                "  (\n" +
                "    ProductID VARCHAR(30) PRIMARY KEY\n" +
                "  )\n" +
                ")");

        test("###Pure\n" +
                        "Profile meta::pure::profiles::storeType\n" +
                        "{\n" +
                        "    stereotypes: [type1, type2];\n" +
                        "}\n" +
                        "\n" +
                        "###Relational\n" +
                        "Database <<meta::pure::profiles::storeType.type3>> app::dbInc\n" +
                        "(\n" +
                        "  Table Product\n" +
                        "  (\n" +
                        "    ProductID VARCHAR(30) PRIMARY KEY,\n" +
                        "    ProductID VARCHAR(30) PRIMARY KEY\n" +
                        "  )\n" +
                        ")",
                "COMPILATION error at [8:44-48]: Can't find stereotype 'type3' in profile 'meta::pure::profiles::storeType'"
        );
    }
    
    @Test
    public void testSchemaStereotype()
    {
        test("###Pure\n" +
                "Profile meta::pure::profiles::storeType\n" +
                "{\n" +
                "    stereotypes: [type1, type2];\n" +
                "}\n" +
                "\n" +
                "###Relational\n" +
                "Database app::db\n" +
                "(\n" +
                "  Schema <<meta::pure::profiles::storeType.type2>> mySchema\n" +
                "  (\n" +
                "    Table Product\n" +
                "    (\n" +
                "      ProductID VARCHAR(30) PRIMARY KEY\n" +
                "    )\n" +
                "  )\n" +
                ")\n");

        test("###Pure\n" +
                        "Profile meta::pure::profiles::storeType\n" +
                        "{\n" +
                        "    stereotypes: [type1, type2];\n" +
                        "}\n" +
                        "\n" +
                        "###Relational\n" +
                        "Database app::db\n" +
                        "(\n" +
                        "  Schema <<meta::pure::profiles::storeType.type3>> mySchema\n" +
                        "  (\n" +
                        "    Table Product\n" +
                        "    (\n" +
                        "      ProductID VARCHAR(30) PRIMARY KEY\n" +
                        "    )\n" +
                        "  )\n" +
                        ")",
                "COMPILATION error at [10:44-48]: Can't find stereotype 'type3' in profile 'meta::pure::profiles::storeType'"
        );
    }
    
    @Test
    public void testTableStereotype()
    {
        test("###Pure\n" +
                "Profile meta::pure::profiles::storeType\n" +
                "{\n" +
                "    stereotypes: [type1, type2];\n" +
                "}\n" +
                "\n" +
                "###Relational\n" +
                "Database app::db\n" +
                "(\n" +
                "  Table <<meta::pure::profiles::storeType.type2>> Product\n" +
                "  (\n" +
                "    ProductID VARCHAR(30) PRIMARY KEY\n" +
                "  )\n" +
                ")\n");

        test("###Pure\n" +
                        "Profile meta::pure::profiles::storeType\n" +
                        "{\n" +
                        "    stereotypes: [type1, type2];\n" +
                        "}\n" +
                        "\n" +
                        "###Relational\n" +
                        "Database app::db\n" +
                        "(\n" +
                        "  Table <<meta::pure::profiles::storeType.type3>> Product\n" +
                        "  (\n" +
                        "    ProductID VARCHAR(30) PRIMARY KEY\n" +
                        "  )\n" +
                        ")",
                "COMPILATION error at [10:43-47]: Can't find stereotype 'type3' in profile 'meta::pure::profiles::storeType'"
        );
    }
    
    @Test
    public void testViewStereotype()
    {
        test("###Pure\n" +
                "Profile meta::pure::profiles::storeType\n" +
                "{\n" +
                "    stereotypes: [type1, type2];\n" +
                "}\n" +
                "\n" +
                "###Relational\n" +
                "Database app::db\n" +
                "(\n" +
                "  Table ProductTable\n" +
                "  (\n" +
                "    id INTEGER PRIMARY KEY,\n" +
                "    name VARCHAR(30)\n" +
                "  )\n" +
                "  View <<meta::pure::profiles::storeType.type2>> ProductView\n" +
                "  (\n" +
                "    id: ProductTable.id,\n" +
                "    name: ProductTable.name\n" +
                "  )\n" +
                ")\n");

        test("###Pure\n" +
                        "Profile meta::pure::profiles::storeType\n" +
                        "{\n" +
                        "    stereotypes: [type1, type2];\n" +
                        "}\n" +
                        "\n" +
                        "###Relational\n" +
                        "Database app::db\n" +
                        "(\n" +
                        "  Table ProductTable\n" +
                        "  (\n" +
                        "    id INTEGER PRIMARY KEY,\n" +
                        "    name VARCHAR(30)\n" +
                        "  )\n" +
                        "  View <<meta::pure::profiles::storeType.type3>> ProductView\n" +
                        "  (\n" +
                        "    id: ProductTable.id,\n" +
                        "    name: ProductTable.name\n" +
                        "  )\n" +
                        ")",
                "COMPILATION error at [15:42-46]: Can't find stereotype 'type3' in profile 'meta::pure::profiles::storeType'"
        );
    }
    
    @Test
    public void testColumnStereotype()
    {
        test("###Pure\n" +
                "Profile meta::pure::profiles::storeType\n" +
                "{\n" +
                "    stereotypes: [type1, type2];\n" +
                "}\n" +
                "\n" +
                "###Relational\n" +
                "Database app::db\n" +
                "(\n" +
                "  Table Product\n" +
                "  (\n" +
                "    ProductID <<meta::pure::profiles::storeType.type2>> VARCHAR(30) PRIMARY KEY\n" +
                "  )\n" +
                ")\n");

        test("###Pure\n" +
                        "Profile meta::pure::profiles::storeType\n" +
                        "{\n" +
                        "    stereotypes: [type1, type2];\n" +
                        "}\n" +
                        "\n" +
                        "###Relational\n" +
                        "Database app::db\n" +
                        "(\n" +
                        "  Table Product\n" +
                        "  (\n" +
                        "    ProductID <<meta::pure::profiles::storeType.type3>> VARCHAR(30) PRIMARY KEY\n" +
                        "  )\n" +
                        ")",
                "COMPILATION error at [12:49-53]: Can't find stereotype 'type3' in profile 'meta::pure::profiles::storeType'"
        );
    }
    
    @Test
    public void testTaggedValues()
    {
        test("###Pure\n" +
                "Profile meta::pure::profiles::doc\n" +
                "{\n" +
                "    tags: [doc];\n" +
                "}\n" +
                "\n" +
                "###Relational\n" +
                "Database app::db\n" +
                "(\n" +
                "  Schema {meta::pure::profiles::doc.doc = 'Schema documentation'} mySchema\n" +
                "  (\n" +
                "    Table {meta::pure::profiles::doc.doc = 'Table documentation'} Product\n" +
                "    (\n" +
                "      ProductID {meta::pure::profiles::doc.doc = 'Column documentation'} VARCHAR(30) PRIMARY KEY\n" +
                "    )\n" +
                "  )\n" +
                "  View {meta::pure::profiles::doc.doc = 'View documentation'} ProductView\n" +
                "  (\n" +
                "    id: mySchema.Product.ProductID\n" +
                "  )\n" +
                ")\n");

        test("###Pure\n" +
                        "Profile meta::pure::profiles::doc\n" +
                        "{\n" +
                        "    tags: [doc];\n" +
                        "}\n" +
                        "\n" +
                        "###Relational\n" +
                        "Database app::db\n" +
                        "(\n" +
                        "  Schema {meta::pure::profiles::doc.unknown = 'Schema documentation'} mySchema\n" +
                        "  (\n" +
                        "    Table Product\n" +
                        "    (\n" +
                        "      ProductID VARCHAR(30) PRIMARY KEY\n" +
                        "    )\n" +
                        "  )\n" +
                        ")",
                "COMPILATION error at [10:37-43]: Can't find tag 'unknown' in profile 'meta::pure::profiles::doc'"
        );
    }
    
    @Test
    public void testCombinedStereotypesAndTaggedValues()
    {
        test("###Pure\n" +
                "Profile meta::pure::profiles::storeType\n" +
                "{\n" +
                "    stereotypes: [type1, type2];\n" +
                "}\n" +
                "\n" +
                "Profile meta::pure::profiles::doc\n" +
                "{\n" +
                "    tags: [doc];\n" +
                "}\n" +
                "\n" +
                "###Relational\n" +
                "Database app::db\n" +
                "(\n" +
                "  Schema <<meta::pure::profiles::storeType.type1>> {meta::pure::profiles::doc.doc = 'Schema documentation'} mySchema\n" +
                "  (\n" +
                "    Table <<meta::pure::profiles::storeType.type2>> {meta::pure::profiles::doc.doc = 'Table documentation'} Product\n" +
                "    (\n" +
                "      ProductID <<meta::pure::profiles::storeType.type1>> {meta::pure::profiles::doc.doc = 'Column documentation'} VARCHAR(30) PRIMARY KEY\n" +
                "    )\n" +
                "  )\n" +
                "  View <<meta::pure::profiles::storeType.type2>> {meta::pure::profiles::doc.doc = 'View documentation'} ProductView\n" +
                "  (\n" +
                "    id: mySchema.Product.ProductID\n" +
                "  )\n" +
                ")\n");
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
    public void testSuperMappingMainTableAlias()
    {
        test("###Relational\n" +
                "Database store::test::testDatabase\n" +
                "(\n" +
                "  Table ABC\n" +
                "  (\n" +
                "    id INTEGER PRIMARY KEY,\n" +
                "    aName VARCHAR(20),\n" +
                "    bName VARCHAR(20),\n" +
                "    cName VARCHAR(20),\n" +
                "    m1Id INTEGER,\n" +
                "    m2Id INTEGER,\n" +
                "    m3Id INTEGER,\n" +
                "    m4Id INTEGER,\n" +
                "    n1Id INTEGER,\n" +
                "    n2Id INTEGER,\n" +
                "    n3Id INTEGER,\n" +
                "    o1Id INTEGER,\n" +
                "    o2Id INTEGER\n" +
                "  )\n" +
                "  Table DEF\n" +
                "  (\n" +
                "    id INTEGER PRIMARY KEY,\n" +
                "    dName VARCHAR(20),\n" +
                "    eName VARCHAR(20),\n" +
                "    fName VARCHAR(20)\n" +
                "  )\n" +
                "  Table GHI\n" +
                "  (\n" +
                "    id INTEGER PRIMARY KEY,\n" +
                "    gName VARCHAR(20),\n" +
                "    hName VARCHAR(20),\n" +
                "    iName VARCHAR(20)\n" +
                "  )\n" +
                "  Table JKL\n" +
                "  (\n" +
                "    id INTEGER PRIMARY KEY,\n" +
                "    jName VARCHAR(20),\n" +
                "    kName VARCHAR(20),\n" +
                "    lName VARCHAR(20)\n" +
                "  )\n" +
                "  Table ABC2\n" +
                "  (\n" +
                "    id INTEGER PRIMARY KEY,\n" +
                "    aName VARCHAR(20),\n" +
                "    bName VARCHAR(20),\n" +
                "    cName VARCHAR(20),\n" +
                "    m1Id INTEGER,\n" +
                "    m2Id INTEGER,\n" +
                "    m3Id INTEGER,\n" +
                "    m4Id INTEGER,\n" +
                "    n1Id INTEGER,\n" +
                "    n2Id INTEGER,\n" +
                "    n3Id INTEGER,\n" +
                "    o1Id INTEGER,\n" +
                "    o2Id INTEGER\n" +
                "  )\n" +
                "  Table DEF2\n" +
                "  (\n" +
                "    id INTEGER PRIMARY KEY,\n" +
                "    dName VARCHAR(20),\n" +
                "    eName VARCHAR(20),\n" +
                "    fName VARCHAR(20)\n" +
                "  )\n" +
                "  Table GHI2\n" +
                "  (\n" +
                "    id INTEGER PRIMARY KEY,\n" +
                "    gName VARCHAR(20),\n" +
                "    hName VARCHAR(20),\n" +
                "    iName VARCHAR(20)\n" +
                "  )\n" +
                "  Table JKL2\n" +
                "  (\n" +
                "    id INTEGER PRIMARY KEY,\n" +
                "    jName VARCHAR(20),\n" +
                "    kName VARCHAR(20),\n" +
                "    lName VARCHAR(20)\n" +
                "  )\n" +
                "\n" +
                "  Join AE(ABC.id = DEF.id)\n" +
                "  Join BH(ABC.id = GHI.id)\n" +
                "  Join CK(ABC.id = JKL.id)\n" +
                "  Join A2E2(ABC2.id = DEF2.id)\n" +
                "  Join B2H2(ABC2.id = GHI2.id)\n" +
                "  Join C2K2(ABC2.id = JKL2.id)\n" +
                "\n" +
                "  Filter AFilter(ABC.aName = 'aName1')\n" +
                "  Filter CFilter(ABC.cName = 'cName1')\n" +
                ")\n" +
                "\n" +
                "Database my::db\n" +
                "(\n" +
                "  Table PersonTable\n" +
                "  (\n" +
                "    NAME CHAR(200) PRIMARY KEY\n" +
                "  )\n" +
                "  Table EmployeeTable\n" +
                "  (\n" +
                "    FIRMNAME CHAR(200) PRIMARY KEY\n" +
                "  )\n" +
                ")\n" +
                "\n" +
                "\n" +
                "###Pure\n" +
                "Class model::test::A\n" +
                "{\n" +
                "  id: Integer[1];\n" +
                "  aName: String[1];\n" +
                "  m1: model::test::M[1];\n" +
                "  m2: model::test::M[1];\n" +
                "}\n" +
                "\n" +
                "Class model::test::B extends model::test::A\n" +
                "{\n" +
                "  bName: String[1];\n" +
                "  n1: model::test::N[1];\n" +
                "  n2: model::test::N[1];\n" +
                "}\n" +
                "\n" +
                "Class model::test::C extends model::test::B\n" +
                "{\n" +
                "  cName: String[1];\n" +
                "  o1: model::test::O[1];\n" +
                "  o2: model::test::O[1];\n" +
                "}\n" +
                "\n" +
                "Class model::test::D\n" +
                "{\n" +
                "  id: Integer[1];\n" +
                "  dName: String[1];\n" +
                "}\n" +
                "\n" +
                "Class model::test::E extends model::test::D\n" +
                "{\n" +
                "  eName: String[1];\n" +
                "}\n" +
                "\n" +
                "Class model::test::F extends model::test::E\n" +
                "{\n" +
                "  fName: String[1];\n" +
                "}\n" +
                "\n" +
                "Class model::test::G\n" +
                "{\n" +
                "  id: Integer[1];\n" +
                "  gName: String[1];\n" +
                "}\n" +
                "\n" +
                "Class model::test::H extends model::test::G\n" +
                "{\n" +
                "  hName: String[1];\n" +
                "}\n" +
                "\n" +
                "Class model::test::I extends model::test::H\n" +
                "{\n" +
                "  iName: String[1];\n" +
                "}\n" +
                "\n" +
                "Class model::test::J\n" +
                "{\n" +
                "  id: Integer[1];\n" +
                "  jName: String[1];\n" +
                "}\n" +
                "\n" +
                "Class model::test::K extends model::test::J\n" +
                "{\n" +
                "  kName: String[1];\n" +
                "}\n" +
                "\n" +
                "Class model::test::L extends model::test::K\n" +
                "{\n" +
                "  lName: String[1];\n" +
                "}\n" +
                "\n" +
                "Class model::test::M\n" +
                "{\n" +
                "  id: Integer[1];\n" +
                "  mName: String[1];\n" +
                "}\n" +
                "\n" +
                "Class model::test::N\n" +
                "{\n" +
                "  id: Integer[1];\n" +
                "  nName: String[1];\n" +
                "}\n" +
                "\n" +
                "Class model::test::O\n" +
                "{\n" +
                "  id: Integer[1];\n" +
                "  oName: String[1];\n" +
                "}\n" +
                "\n" +
                "Class model::Person\n" +
                "{\n" +
                "  name: String[1];\n" +
                "}\n" +
                "\n" +
                "Class model::Employee extends model::Person\n" +
                "{\n" +
                "  firmName: String[1];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping mapping::testAMapping\n" +
                "(\n" +
                "  model::test::A[a]: Relational\n" +
                "  {\n" +
                "    ~primaryKey\n" +
                "    (\n" +
                "      [store::test::testDatabase]ABC.id\n" +
                "    )\n" +
                "    ~mainTable [store::test::testDatabase]ABC\n" +
                "    id: [store::test::testDatabase]ABC.id,\n" +
                "    aName: [store::test::testDatabase]ABC.aName\n" +
                "  }\n" +
                ")\n" +
                "\n" +
                "Mapping mapping::testB1Mapping\n" +
                "(\n" +
                "  include mapping mapping::testAMapping\n" +
                "\n" +
                "  model::test::B[b1] extends [a]: Relational\n" +
                "  {\n" +
                "  }\n" +
                ")\n" +
                "\n" +
                "Mapping mapping::testB2Mapping\n" +
                "(\n" +
                "  include mapping mapping::testAMapping\n" +
                "\n" +
                "  model::test::B[b2] extends [a]: Relational\n" +
                "  {\n" +
                "    aName: concat('bName_', [store::test::testDatabase]ABC.aName),\n" +
                "    bName: [store::test::testDatabase]ABC.bName\n" +
                "  }\n" +
                ")\n" +
                "\n" +
                "Mapping mapping::testC1Mapping\n" +
                "(\n" +
                "  include mapping mapping::testB1Mapping\n" +
                "\n" +
                "  model::test::C[c1] extends [b1]: Relational\n" +
                "  {\n" +
                "  }\n" +
                ")\n" +
                "\n" +
                "Mapping mapping::testC2Mapping\n" +
                "(\n" +
                "  include mapping mapping::testB2Mapping\n" +
                "\n" +
                "  model::test::C[c2] extends [b2]: Relational\n" +
                "  {\n" +
                "    aName: concat('cName_', [store::test::testDatabase]ABC.aName),\n" +
                "    bName: concat('cName_', [store::test::testDatabase]ABC.bName),\n" +
                "    cName: [store::test::testDatabase]ABC.cName\n" +
                "  }\n" +
                ")\n" +
                "\n" +
                "Mapping model::map\n" +
                "(\n" +
                "  *model::Person[id]: Relational\n" +
                "  {\n" +
                "    ~primaryKey\n" +
                "    (\n" +
                "      [my::db]PersonTable.NAME\n" +
                "    )\n" +
                "    ~mainTable [my::db]PersonTable\n" +
                "  }\n" +
                "  *model::Employee extends [id]: Relational\n" +
                "  {\n" +
                "    ~primaryKey\n" +
                "    (\n" +
                "      [my::db]EmployeeTable.FIRMNAME\n" +
                "    )\n" +
                "    ~mainTable [my::db]EmployeeTable\n" +
                "    firmName: [my::db]EmployeeTable.FIRMNAME\n" +
                "  }\n" +
                ")");
    }

    @Test
    public void testMissingColumnOnMilestoning()
    {
        // PROCESSING_IN missing
        test("###Relational\n" +
                        "Database app::db\n" +
                        "(\n" +
                        "    Table personTable" +
                        "    (" +
                        "       milestoning(processing(PROCESSING_IN = dummyIn, PROCESSING_OUT = dummyOut))" +
                        "       ID INT PRIMARY KEY, MANAGERID INT, dummyOut TIMESTAMP\n" +
                        "    )\n" +
                        ")",
                "COMPILATION error at [4:47-108]: Milestone column 'dummyIn' not found on table definition"
        );

        // PROCESSING_OUT missing
        test("###Relational\n" +
                        "Database app::db\n" +
                        "(\n" +
                        "    Table personTable" +
                        "    (" +
                        "       milestoning(processing(PROCESSING_IN = dummyIn, PROCESSING_OUT = dummyOut))" +
                        "       ID INT PRIMARY KEY, MANAGERID INT, dummyIn TIMESTAMP\n" +
                        "    )\n" +
                        ")",
                "COMPILATION error at [4:47-108]: Milestone column 'dummyOut' not found on table definition"
        );

        // BUS_FROM missing
        test("###Relational\n" +
                        "Database app::db\n" +
                        "(\n" +
                        "    Table personTable" +
                        "    (" +
                        "       milestoning(business(BUS_FROM = dummyIn, BUS_THRU = dummyOut))" +
                        "       ID INT PRIMARY KEY, MANAGERID INT, dummyOut DATE\n" +
                        "    )\n" +
                        ")",
                "COMPILATION error at [4:56-94]: Milestone column 'dummyIn' not found on table definition"
        );

        // BUS_THRU missing
        test("###Relational\n" +
                        "Database app::db\n" +
                        "(\n" +
                        "    Table personTable" +
                        "    (" +
                        "       milestoning(business(BUS_FROM = dummyIn, BUS_THRU = dummyOut))" +
                        "       ID INT PRIMARY KEY, MANAGERID INT, dummyIn DATE\n" +
                        "    )\n" +
                        ")",
                "COMPILATION error at [4:56-94]: Milestone column 'dummyOut' not found on table definition"
        );

        // BUS_SNAPSHOT_DATE missing
        test("###Relational\n" +
                        "Database app::db\n" +
                        "(\n" +
                        "    Table personTable" +
                        "    (" +
                        "       milestoning(business(BUS_SNAPSHOT_DATE = dummy))" +
                        "       ID INT PRIMARY KEY, MANAGERID INT\n" +
                        "    )\n" +
                        ")",
                "COMPILATION error at [4:56-80]: Milestone column 'dummy' not found on table definition"
        );
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
                "COMPILATION error at [4:5-43]: Can't find database 'model::relational::tests::dbInc'"
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
        //association with source and target IDs


        // association mapping on milestoned models
        PureModel model = test("\n" +
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
        ).getTwo();
        Assert.assertNotNull(model.getMapping("model::myRelationalMapping")._associationMappings().getAny()._stores());
        Assert.assertEquals("dbInc", model.getMapping("model::myRelationalMapping")._associationMappings().getAny()._stores().getAny()._name());

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
                "}\n" +
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
                ")\n" +
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
        //test multiple databases with identical elements
        test(MODEL +
                "Association model::Employment\n" +
                "{\n" +
                "    firm : model::Firm[0..1];\n" +
                "    employees : model::Person[*];\n" +
                "}\n" + DB_INC +
                "Database model::relational::tests::dbInc2\n" +
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
                "    Join OtherFirm_PersonFirm(otherFirmTable.ID = PersonToFirm.FIRMID))\n" +

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


    }

    @Test
    public void testRelationalMappingToTabularFunc()
    {
        Pair<PureModelContextData, PureModel> res = test("Class simple::Person\n" +
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
                "  TabularFunction personFunction\n" +
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

                "    View FirmView\n" +
                "    (\n" +
                "        id: firmTable.ID PRIMARY KEY, \n" +
                "        legal:  firmTable.LEGALNAME\n" +
                "\n" +
                "   )\n" +
                "  Join Firm_FirmView(firmTable.ID = FirmView.id)\n" +
                "  Join Firm_Person(firmTable.ID = personFunction.FIRMID)\n" +
                ")\n" +
                "\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping simple::simpleRelationalMappingInc\n" +
                "(\n" +
                "  simple::Person[simple_Person]: Relational\n" +
                "  {\n" +
                "    firstName: [simple::dbInc]personFunction.FIRSTNAME,\n" +
                "    lastName: [simple::dbInc]personFunction.LASTNAME,\n" +
                "    age: [simple::dbInc]personFunction.AGE\n" +
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


    }

    @Test
    public void testMappingWithSourceTargetID()
    {
        PureModel modelIds = test(MODEL +
                "Association model::Employment\n" +
                "{\n" +
                "    firm : model::Firm[0..1];\n" +
                "    employees : model::Person[*];\n" +
                "}\n" + DB_INC +
                "###Mapping\n" +
                "Mapping model::myRelationalMapping\n" +
                "(\n" +
                "  model::Person[person]: Relational\n" +
                "  {\n" +
                "    firstName: [model::relational::tests::dbInc]personTable.FIRSTNAME,\n" +
                "    lastName: [model::relational::tests::dbInc]personTable.LASTNAME,\n" +
                "    age: [model::relational::tests::dbInc]personTable.AGE\n" +
                "  }\n" +
                "  model::Firm[firm]: Relational\n" +
                "  {\n" +
                "    legalName: [model::relational::tests::dbInc]firmTable.LEGALNAME\n" +
                "  }\n" +
                "   model::Employment : Relational\n" +
                "   {\n" +
                "      AssociationMapping\n" +
                "      (\n" +
                "         employees[firm,person] : [model::relational::tests::dbInc]@Firm_Person,\n" +
                "         firm[person,firm] : [model::relational::tests::dbInc]@Firm_Person\n" +
                "      )\n" +
                "   }" +
                ")"
        ).getTwo();
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
                ")", "COMPILATION error at [10:32-38]: The store 'model::store::db' can't be found."
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
                ")", "COMPILATION error at [92:21-74]: Can't find property 'propertyMissing' in [Firm, LegalEntity, Any]"
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
                ")", "COMPILATION error at [4:3-20]: Can't find association 'MissingAssociation'"
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
                ")", "COMPILATION error at [107:10-35]: Can't find property 'missingAssociationProperty' in association 'model::Employment'"
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
                "}\n" +
                "###Relational\n" +
                "Database model::db\n" +
                "(\n" +
                "   Table personTb(name VARCHAR(200), firm VARCHAR(200))\n" +
                "   Table firmTb(name VARCHAR(200))\n" +
                "   Table otherTb(name VARCHAR(200))\n" +
                "   Table otherTb2(name VARCHAR(200))\n" +
                "   Join myJoin(otherTb2.name = otherTb.name)\n" +
                "   Join otherJoin(otherTb2.name = firmTb.name)\n" +
                ")\n" +
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
                "}\n" +
                "###Relational\n" +
                "Database model::db\n" +
                "(\n" +
                "   Table personTb(name VARCHAR(200), firm VARCHAR(200))\n" +
                "   Table firmTb(name VARCHAR(200))\n" +
                "   Table otherTb(name VARCHAR(200))\n" +
                "   Table otherTb2(name VARCHAR(200))\n" +
                "   Join myJoin(personTb.firm = otherTb.name)\n" +
                "   Join otherJoin(otherTb2.name = firmTb.name)\n" +
                ")\n" +
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
                "}\n" +
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
                ")\n" +
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
                ")", "COMPILATION error at [92:31-100]: Can't find property 'missingEmbeddedProperty' in [Person, Any]"
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
                ")", "COMPILATION error at [90:5-58]: Can't find filter 'MissingFilter' in database 'dbInc'"
        );
    }

    @Test
    public void testMappingInheritance()
    {
        test(MODEL + DB_INC +
                "###Mapping\n" +
                "Mapping model::myRelationalMapping\n" +
                "(\n" +
                "  model::LegalEntity[entity]: Relational\n" +
                "  {\n" +
                "    name: [model::relational::tests::dbInc]firmTable.LEGALNAME\n" +
                "  }\n" +
                "  model::Firm[firm] extends [entity1]: Relational\n" +
                "  {\n" +
                "    legalName: [model::relational::tests::dbInc]firmTable.LEGALNAME\n" +
                "  }\n" +
                ")", "COMPILATION error at [92:3-95:3]: Can't find extends class mapping 'entity1' in mapping 'model::myRelationalMapping'"
        );

        test(MODEL + DB_INC +
                "###Mapping\n" +
                "Mapping model::myRelationalMapping\n" +
                "(\n" +
                "  model::LegalEntity[entity]: Relational\n" +
                "  {\n" +
                "    name: [model::relational::tests::dbInc]firmTable.LEGALNAME\n" +
                "  }\n" +
                "  model::Firm[firm] extends [entity]: Relational\n" +
                "  {\n" +
                "    legalName: [model::relational::tests::dbInc]firmTable.LEGALNAME\n" +
                "  }\n" +
                ")");
    }

    @Test
    public void testTestMapping()
    {
        test(MODEL + DB_INC +
                "###Mapping\n" +
                "Mapping model::myRelationalMapping\n" +
                "(\n" +
                "  model::Firm: Relational\n" +
                "  {\n" +
                "    legalName: [model::relational::tests::dbInc]firmTable.LEGALNAME\n" +
                "  }\n" +
                "  MappingTests\n" +
                "  [\n" +
                "    test_1\n" +
                "    (\n" +
                "      query: |model::LegalEntity.all()->project([p|$p.name],['name']);\n" +
                "      data:\n" +
                "      [\n" +
                "        <Relational, SQL, model::relational::tests::dbInc, 'Drop table if exists personTable;Create Table personTable(id INT, firstName INT);Insert into PersonTable (id, firstName) values (1, \\'Doe\\;\\');Insert into PersonTable (id, lastName) values (2, \\'Doe2\\');'>\n" +
                "      ];\n" +
                "      assert: '[ {\\n  \"values\" : [ \"Doe;\" ]\\n}, {\\n  \"values\" : [ \"Wrong\" ]\\n} ]';\n" +
                "    )\n" +
                "  ]\n" +
                ")"
        );
    }

    @Test
    public void testTestMappingError()
    {
        test(MODEL + DB_INC +
                "###Mapping\n" +
                "Mapping model::myRelationalMapping\n" +
                "(\n" +
                "  model::Firm: Relational\n" +
                "  {\n" +
                "    legalName: [model::relational::tests::dbInc]firmTable.LEGALNAME\n" +
                "  }\n" +
                "  MappingTests\n" +
                "  [\n" +
                "    test_1\n" +
                "    (\n" +
                "      query: |model::LegalEntity.all()->project([p|$p.name],['name']);\n" +
                "      data:\n" +
                "      [\n" +
                "        <Relational, SQL, test::DB, 'Drop table if exists personTable;Create Table personTable(id INT, firstName INT);Insert into PersonTable (id, firstName) values (1, \\'Doe\\;\\');Insert into PersonTable (id, lastName) values (2, \\'Doe2\\');'>\n" +
                "      ];\n" +
                "      assert: '[ {\\n  \"values\" : [ \"Doe;\" ]\\n}, {\\n  \"values\" : [ \"Wrong\" ]\\n} ]';\n" +
                "    )\n" +
                "  ]\n" +
                ")", "COMPILATION error at [99:9-242]: The store 'test::DB' can't be found."
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

    @Test
    public void testFilterMappingWithInnerJoin()
    {
        test("import other::*;\n" +
                "\n" +
                "Class other::Person\n" +
                "{\n" +
                "    firstName:String[1];\n" +
                "    firm:Firm[1];\n" +
                "}\n" +
                "Class other::Firm\n" +
                "{\n" +
                "    legalName:String[1];\n" +
                "    employees:Person[1];\n" +
                "}\n" +
                "###Relational\n" +
                "Database mapping::db(\n" +
                "   Table personTable\n" +
                "   (\n" +
                "    id INT PRIMARY KEY,\n" +
                "    firstName VARCHAR(200),\n" +
                "    firmId INT,\n" +
                "    legalName VARCHAR(200)\n" +
                "   )\n" +
                "   Table firmTable\n" +
                "   (\n" +
                "    id INT PRIMARY KEY,\n" +
                "    legalName VARCHAR(200)\n" +
                "   )\n" +
                "   View personFirmView\n" +
                "   (\n" +
                "    id : personTable.id,\n" +
                "    firstName : personTable.firstName,\n" +
                "    firmId : personTable.firmId\n" +
                "   )\n" +
                "   Filter FirmFilter(firmTable.legalName = 'A')\n" +
                "   Join Firm_Person(firmTable.id = personTable.firmId)\n" +
                ")\n" +
                "###Mapping\n" +
                "import other::*;\n" +
                "import mapping::*;\n" +
                "Mapping mappingPackage::myMapping\n" +
                "(\n" +
                "    Person: Relational\n" +
                "    {\n" +
                "        ~filter [mapping::db] (INNER) @Firm_Person | [mapping::db] FirmFilter \n" +
                "        firstName : [db]personTable.firstName\n" +
                "    }\n" +
                ")\n");
    }

    @Test
    public void testInnerJoinReferenceInRelationalMapping()
    {
        String model = "Class simple::Person\n" +
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
                "   Table PersonToFirm(PERSONID INT PRIMARY KEY, FIRMID INT PRIMARY KEY)\n" +
                "  Join Firm_Person(firmTable.ID = personTable.FIRMID)\n" +
                "  Join Person_PersonFirm(personTable.ID = PersonToFirm.PERSONID)\n" +
                "  Join Firm_PersonFirm(firmTable.ID = PersonToFirm.FIRMID)\n" +

                ")\n" +
                "\n" +
                "\n";

        test(model +
                "###Mapping\n" +
                "Mapping simple::simpleRelationalMappingInc\n" +
                "(\n" +
                "  simple::Person: Relational\n" +
                "  {\n" +
                "    ~mainTable [simple::dbInc]personTable\n" +
                "    firstName: [simple::dbInc]personTable.FIRSTNAME\n" +
                "  }\n" +
                "  simple::Firm[simple_Firm]: Relational\n" +
                "  {\n" +
                "    ~mainTable [simple::dbInc]firmTable\n" +
                "    employees: [simple::dbInc] @Firm_PersonFirm > (INNER) [simple::dbInc] @Person_PersonFirm\n" +
                "  }\n" +
                "\n" +
                ")");

        test(model +
                "###Mapping\n" +
                "Mapping simple::simpleRelationalMappingInc\n" +
                "(\n" +
                "  simple::Firm[simple_Firm]: Relational\n" +
                "  {\n" +
                "    ~mainTable [simple::dbInc]firmTable\n" +
                "    employees: [simple::dbInc] (INNER) @Firm_Person > (INNER) [simple::dbInc] @Firm_Person\n" +
                "  }\n" +
                "\n" +
                ")", "COMPILATION error at [55:14-90]: Do not support specifying join type for the first join in the classMapping.");
    }

    @Test
    public void testSemiStructuredColumn()
    {
        Pair<PureModelContextData, PureModel> res = test("###Relational\n" +
                "Database simple::DB\n" +
                "(\n" +
                "  Table personTable\n" +
                "  (\n" +
                "    FIRSTNAME VARCHAR(10),\n" +
                "    FIRM SEMISTRUCTURED\n" +
                "  )\n" +
                ")\n");

        Database database = (Database) res.getTwo().getStore("simple::DB");
        Column column = (Column) database._schemas().detect(schema -> schema._name().equals("default"))._tables().detect(table -> table._name().equals("personTable"))._columns().detect(col -> col.getName().equals("FIRM"));
        Assert.assertTrue(column._type() instanceof SemiStructured);
    }

    @Test
    public void testJsonColumn()
    {
        Pair<PureModelContextData, PureModel> res = test("###Relational\n" +
                "Database simple::DB\n" +
                "(\n" +
                "  Table personTable\n" +
                "  (\n" +
                "    FIRSTNAME VARCHAR(10),\n" +
                "    FIRM JSON\n" +
                "  )\n" +
                ")\n");

        Database database = (Database) res.getTwo().getStore("simple::DB");
        Column column = (Column) database._schemas().detect(schema -> schema._name().equals("default"))._tables().detect(table -> table._name().equals("personTable"))._columns().detect(col -> col.getName().equals("FIRM"));
        Assert.assertTrue(column._type() instanceof Json);
    }

    @Test
    public void testRelationalPropertyMappingWithBindingTransformer()
    {
        String model = "Class simple::Person\n" +
                "{\n" +
                "  firstName: String[1];\n" +
                "  lastName: String[1];\n" +
                "  age: Integer[1];\n" +
                "  firm: simple::Firm[1];\n" +
                "  manager: simple::Person[0..1];\n" +
                "}\n" +
                "\n" +
                "Class simple::Firm\n" +
                "{\n" +
                "  legalName: String[1];\n" +
                "}\n" +
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
                "    FIRM SEMISTRUCTURED,\n" +
                "    FIRM_JSON JSON\n" +
                "  )\n" +
                "\n" +
                "  Join personSelfJoin(personTable.ID = {target}.ID)\n" +
                ")\n" +
                "###ExternalFormat\n" +
                "Binding simple::TestBinding\n" +
                "{\n" +
                "  contentType: 'application/json';\n" +
                "  modelIncludes: [\n" +
                "    simple::Firm\n" +
                "  ];\n" +
                "}\n";

        test(model +
                "###Mapping\n" +
                "Mapping simple::simpleRelationalMappingInc\n" +
                "(\n" +
                "  simple::Person: Relational\n" +
                "  {\n" +
                "    ~mainTable [simple::dbInc]personTable\n" +
                "    firstName: [simple::dbInc]personTable.FIRSTNAME,\n" +
                "    lastName: [simple::dbInc]personTable.LASTNAME,\n" +
                "    age: Binding simple::TestBinding: [simple::dbInc]personTable.AGE\n" +
                "  }\n" +
                ")\n", "COMPILATION error at [46:8-68]: Binding transformer can be used with complex properties only. Property 'age' return type is 'Integer'");

        test(model +
                "###Mapping\n" +
                "Mapping simple::simpleRelationalMappingInc\n" +
                "(\n" +
                "  simple::Person: Relational\n" +
                "  {\n" +
                "    ~mainTable [simple::dbInc]personTable\n" +
                "    firstName: [simple::dbInc]personTable.FIRSTNAME,\n" +
                "    lastName: [simple::dbInc]personTable.LASTNAME,\n" +
                "    age: [simple::dbInc]personTable.AGE,\n" +
                "    manager: Binding simple::TestBinding: [simple::dbInc]personTable.FIRM\n" +
                "  }\n" +
                ")\n", "COMPILATION error at [47:12-73]: Class: simple::Person should be included in modelUnit for binding: simple::TestBinding");

        test(model +
                "###Mapping\n" +
                "Mapping simple::simpleRelationalMappingInc\n" +
                "(\n" +
                "  simple::Person: Relational\n" +
                "  {\n" +
                "    ~mainTable [simple::dbInc]personTable\n" +
                "    firstName: [simple::dbInc]personTable.FIRSTNAME,\n" +
                "    lastName: [simple::dbInc]personTable.LASTNAME,\n" +
                "    age: [simple::dbInc]personTable.AGE,\n" +
                "    firm: Binding simple::TestBinding: [simple::dbInc]personTable.FIRM\n" +
                "  }\n" +
                ")\n");

        test(model +
                "###Mapping\n" +
                "Mapping simple::simpleRelationalMappingInc\n" +
                "(\n" +
                "  simple::Person: Relational\n" +
                "  {\n" +
                "    ~mainTable [simple::dbInc]personTable\n" +
                "    firstName: [simple::dbInc]personTable.FIRSTNAME,\n" +
                "    lastName: [simple::dbInc]personTable.LASTNAME,\n" +
                "    age: [simple::dbInc]personTable.AGE,\n" +
                "    firm: Binding simple::TestBinding: [simple::dbInc]personTable.FIRM_JSON\n" +
                "  }\n" +
                ")\n");

        test(model +
                "###Mapping\n" +
                "Mapping simple::simpleRelationalMappingInc\n" +
                "(\n" +
                "  simple::Person: Relational\n" +
                "  {\n" +
                "    ~mainTable [simple::dbInc]personTable\n" +
                "    firstName: [simple::dbInc]personTable.FIRSTNAME,\n" +
                "    lastName: [simple::dbInc]personTable.LASTNAME,\n" +
                "    age: [simple::dbInc]personTable.AGE,\n" +
                "    firm: Binding simple::TestBinding: [simple::dbInc]@personSelfJoin | [simple::dbInc]personTable.FIRM\n" +
                "  }\n" +
                ")\n");

        test(model +
                "###Mapping\n" +
                "Mapping simple::simpleRelationalMappingInc\n" +
                "(\n" +
                "  simple::Person: Relational\n" +
                "  {\n" +
                "    ~mainTable [simple::dbInc]personTable\n" +
                "    firstName: [simple::dbInc]personTable.FIRSTNAME,\n" +
                "    lastName: [simple::dbInc]personTable.LASTNAME,\n" +
                "    age: [simple::dbInc]personTable.AGE,\n" +
                "    firm: Binding simple::TestBinding: [simple::dbInc]@personSelfJoin | [simple::dbInc]personTable.FIRM_JSON\n" +
                "  }\n" +
                ")\n");
    }

    @Test
    public void testLocalProperties() throws Exception
    {
        PureModel model = test("Class model::Person {\n" +
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
                "      name : [model::store::db]myTable.name,\n" +
                "      + localProp : String[1] : [model::store::db]myTable.name\n" +
                "   }\n" +
                ")"
        ).getTwo();
        org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping mmMapping = model.getMapping("model::mapping::myMap");
        RootRelationalInstanceSetImplementation rootRelationalInstanceSetImplementation = ((RootRelationalInstanceSetImplementation) mmMapping._classMappings().getFirst());

        Assert.assertTrue(rootRelationalInstanceSetImplementation._propertyMappings().getLast()._localMappingProperty());

        Assert.assertNotNull(rootRelationalInstanceSetImplementation._propertyMappings().getLast()._localMappingPropertyMultiplicity());
        Assert.assertNotNull(rootRelationalInstanceSetImplementation._propertyMappings().getLast()._localMappingPropertyType());
        Assert.assertEquals("String", rootRelationalInstanceSetImplementation._propertyMappings().getLast()._localMappingPropertyType()._name());
    }

    @Test
    public void testFilerName() throws Exception
    {
        PureModel model = test("Class model::Person {\n" +
                "   name:String[1];\n" +
                "}\n" +
                "###Relational\n" +
                "Database model::store::db\n" +
                "(\n" +
                "   Table myTable(name VARCHAR(200))\n" +
                "   Filter myFilter(myTable.name = 'A')\n" +
                ")\n" +
                "###Mapping\n" +
                "Mapping \n" +
                "model::mapping::myMap\n" +
                "(\n" +
                "   model::Person: Relational\n" +
                "   {\n" +
                "      ~filter [model::store::db]myFilter \n" +
                "      name : [model::store::db]myTable.name,\n" +
                "      + localProp : String[1] : [model::store::db]myTable.name\n" +
                "   }\n" +
                ")"
        ).getTwo();
        org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping mmMapping = model.getMapping("model::mapping::myMap");
        RootRelationalInstanceSetImplementation rootRelationalInstanceSetImplementation = ((RootRelationalInstanceSetImplementation) mmMapping._classMappings().getFirst());

        Assert.assertNotNull(rootRelationalInstanceSetImplementation._filter()._filterName());
        Assert.assertEquals("myFilter", rootRelationalInstanceSetImplementation._filter()._filterName());

    }

    @Test
    public void testUnknownSetImplementationIdWarning() throws Exception
    {
        Pair<PureModelContextData, PureModel> res = test("Class simple::Person\n" +
                "{\n" +
                "  lastName: String[1];\n" +
                "  firm: simple::Firm[1];\n" +
                "}\n" +
                "\n" +
                "Class simple::Firm\n" +
                "{\n" +
                "  legalName: String[1];\n" +
                "}\n" +
                "\n" +
                "###Relational\n" +
                "Database simple::dbInc\n" +
                "(\n" +
                "  Table personTable\n" +
                "  (\n" +
                "    ID INTEGER PRIMARY KEY,\n" +
                "    LASTNAME VARCHAR(200)\n" +
                "  )\n" +
                "\n" +
                "  Join personSelfJoin(personTable.ID = {target}.ID)\n" +
                ")\n" +
                "###Mapping\n" +
                "Mapping simple::simpleRelationalMappingInc\n" +
                "(\n" +
                "  simple::Person: Relational\n" +
                "  {\n" +
                "    ~mainTable [simple::dbInc]personTable\n" +
                "    lastName: [simple::dbInc]personTable.LASTNAME, \n" +
                "    firm[x]: [simple::dbInc]@personSelfJoin\n" +
                "  }\n" +
                ")\n", null, Arrays.asList("COMPILATION error at [30:12-43]: Error 'x' can't be found in the mapping simple::simpleRelationalMappingInc"));

        MutableList<Warning> warnings = res.getTwo().getWarnings();
        Assert.assertEquals(1, warnings.size());
        Assert.assertEquals("{\"sourceInformation\":{\"sourceId\":\"\",\"startLine\":30,\"startColumn\":12,\"endLine\":30,\"endColumn\":43},\"message\":\"Error 'x' can't be found in the mapping simple::simpleRelationalMappingInc\"}", new ObjectMapper().writeValueAsString(warnings.get(0)));
    }

    @Test
    public void testRelationalMappingForTableNameInQuotesWithDots() throws Exception
    {
        PureModel model = test(
                "###Pure\n" +
                        "Class simple::Item\n" +
                        "{\n" +
                        "   id: Integer[0..1];\n" +
                        "}\n" +
                        "###Relational\n" +
                        "Database simple::DB\n" +
                        "(\n" +
                        "   Table \"tableNameInQuotes.With.Dots\"\n" +
                        "   (\n" +
                        "       ID INTEGER PRIMARY KEY\n" +
                        "   )\n" +
                        ")\n" +
                        "###Mapping\n" +
                        "Mapping simple::ItemMapping\n" +
                        "(\n" +
                        "simple::Item: Relational\n" +
                        "  {\n" +
                        "    ~primaryKey\n" +
                        "    (\n" +
                        "       [simple::DB]\"tableNameInQuotes.With.Dots\".ID\n" +
                        "    )\n" +
                        "    ~mainTable [simple::DB]\"tableNameInQuotes.With.Dots\"\n" +
                        "    id: [simple::DB]\"tableNameInQuotes.With.Dots\".ID\n" +
                        "  }\n" +
                        ")"
        ).getTwo();
        org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping mmMapping = model.getMapping("simple::ItemMapping");
        RootRelationalInstanceSetImplementation rootRelationalInstanceSetImplementation = ((RootRelationalInstanceSetImplementation) mmMapping._classMappings().getFirst());
        RelationalOperationElement primaryKey = rootRelationalInstanceSetImplementation._primaryKey().getFirst();
        // mainTable
        Table table = (Table) rootRelationalInstanceSetImplementation._mainTableAlias()._relationalElement();
        Assert.assertEquals(table._name(), "\"tableNameInQuotes.With.Dots\"");
        // primaryKey
        Column col = ((TableAliasColumn) primaryKey)._column();
        Assert.assertEquals(col._name(), "ID");
        Assert.assertEquals(((Table) col._owner())._name(), "\"tableNameInQuotes.With.Dots\"");
        // classMappingId
        Assert.assertEquals(rootRelationalInstanceSetImplementation._id(), "simple_Item");
    }

    @Test
    public void testNestedJoinFromIncludedDatabase()
    {
        test("###Relational\n" +
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

    }

    public void testRelationalClassMappingWithDuplicateSetIdsError()
    {
        test("###Pure\n" +
                "Class simple::Account\n" +
                "{\n" +
                "   id: String[1];   \n" +
                "}\n" +
                "\n" +
                "Class simple::Another extends simple::Account\n" +
                "{  \n" +
                "}\n" +
                "\n" +
                "###Relational\n" +
                "Database simple::gen1::store\n" +
                "(\n" +
                "   Table Account\n" +
                "   (\n" +
                "      ACCOUNT_ID VARCHAR(200) PRIMARY KEY\n" +
                "   )\n" +
                ")\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping simple::gen1::map\n" +
                "(\n" +
                "   simple::Account[id]: Relational\n" +
                "   {\n" +
                "      scope([simple::gen1::store]Account)\n" +
                "      (\n" +
                "         id: ACCOUNT_ID   \n" +
                "      )\n" +
                "   }\n" +
                ")\n" +
                "\n" +
                "Mapping simple::gen2::map\n" +
                "(\n" +
                "   simple::Account[id]: Relational\n" +
                "   {\n" +
                "      scope([simple::gen1::store]Account)\n" +
                "      (\n" +
                "         id: ACCOUNT_ID   \n" +
                "      )\n" +
                "   }\n" +
                ")\n" +
                "\n" +
                "Mapping simple::merged(\n" +
                "   include simple::gen1::map\n" +
                "   include simple::gen2::map\n" +
                "      \n" +
                "   simple::Another extends [id]: Relational\n" +
                "   {      \n" +
                "   }   \n" +
                ")", "COMPILATION error at [47:4-49:4]: Duplicated class mappings found with ID 'id' in mapping 'simple::merged'; parent mapping for duplicated: 'simple::gen1::map', 'simple::gen2::map'");
    }


    @Test
    public void testAssociationAndComplexPropertyCompilerErrors()
    {


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
                "         employees : [model::relational::tests::dbInc]@Address_Person,\n" + // wrong Join (the table on the other side of join is not the main  table of the employees set
                "         firm : [model::relational::tests::dbInc]@Firm_Person\n" +
                "      )\n" +
                "   }" +
                ")", null, Arrays.asList("COMPILATION error at [107:20-69]: Mapping error: model::myRelationalMapping the join Address_Person does not contain the source table [dbInc]firmTable"));

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
                "         employees : [model::relational::tests::dbInc]@OtherFirm_PersonFirm >@Firm_Person\n" + //Join chain doesn't start on correct Table
                "      )\n" +
                "   }" +
                ")", null, Arrays.asList("COMPILATION error at [107:20-89]: Mapping error: model::myRelationalMapping the join OtherFirm_PersonFirm does not contain the source table [dbInc]firmTable"));

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
                "         employees : [model::relational::tests::dbInc]@Firm_PersonFirm > @OtherFirm_PersonFirm\n" +  // Join chain doesn't end  on correct Table
                //  "         firm : [model::relational::tests::dbInc]@Firm_Person\n" +
                "      )\n" +
                "   }" +
                ")", null, Arrays.asList("COMPILATION error at [107:20-94]: Mapping error: model::myRelationalMapping the join OtherFirm_PersonFirm does not connect from the source table [dbInc]PersonToFirm to the target table [dbInc]personTable; instead it connects to [dbInc]otherFirmTable"));


        test(MODEL +
                "Class model::OtherFirm extends model::LegalEntity \n" +
                "{\n" +
                "   legalName: String[1];\n" +
                "   address: Integer[1];\n" +
                "   employee: model::Person[0..1];\n" +
                "}\n" +
                "\n" +
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
                "  model::OtherFirm[OtherFirm]: Relational\n" +
                "  {\n" +
                "    legalName: [model::relational::tests::dbInc]firmTable.LEGALNAME\n" +
                "  }\n" +
                "   model::Employment : Relational\n" +
                "   {\n" +
                "      AssociationMapping\n" +
                "      (\n" +
                "         employees : [model::relational::tests::dbInc]@Firm_Person,\n" +
                "         firm[OtherFirm] : [model::relational::tests::dbInc]@Firm_Person\n" + //id exists but it's the wrong  type (Join is correct)
                "      )\n" +
                "   }" +
                ")", null, Arrays.asList("COMPILATION error at [119:26-72]: Mapping Error: on model::myRelationalMapping The setImplementationId 'OtherFirm' is implementing the class 'OtherFirm' which is not a subType of 'Firm' return type of the mapped property 'firm'"));


        test(MODEL +
                "Association model::Employment\n" +
                "{\n" +
                "    firm : model::Firm[0..1];\n" +
                "    employees : model::Person[*];\n" +
                "}\n" + DB_INC +
                "###Mapping\n" +
                "Mapping model::myRelationalMapping\n" +
                "(\n" +
                "  model::Person[person]: Relational\n" +
                "  {\n" +
                "    firstName: [model::relational::tests::dbInc]personTable.FIRSTNAME,\n" +
                "    lastName: [model::relational::tests::dbInc]personTable.LASTNAME,\n" +
                "    age: [model::relational::tests::dbInc]personTable.AGE\n" +
                "  }\n" +
                "  model::Firm[firm]: Relational\n" +
                "  {\n" +
                "    legalName: [model::relational::tests::dbInc]firmTable.LEGALNAME\n" +
                "  }\n" +
                "   model::Employment : Relational\n" +
                "   {\n" +
                "      AssociationMapping\n" +
                "      (\n" +
                "         employees[wrongID, person] : [model::relational::tests::dbInc]@Firm_Person,\n" + // source ID is incorrect
                "         firm[person, firm] : [model::relational::tests::dbInc]@Firm_Person\n" +
                "      )\n" +
                "   }" +
                ")", null, Arrays.asList("COMPILATION error at [107:37-83]: Unable to find source class mapping (id:wrongID) for property 'employees' in Association mapping 'model::Employment'. Make sure that you have specified a valid Class mapping id as the source id and target id, using the syntax 'property[sourceId, targetId]'."));


        test(MODEL +
                "Association model::Employment\n" +
                "{\n" +
                "    firm : model::Firm[0..1];\n" +
                "    employees : model::Person[*];\n" +
                "}\n" + DB_INC +
                "###Mapping\n" +
                "Mapping model::myRelationalMapping\n" +
                "(\n" +
                "  model::Person[person]: Relational\n" +
                "  {\n" +
                "    firstName: [model::relational::tests::dbInc]personTable.FIRSTNAME,\n" +
                "    lastName: [model::relational::tests::dbInc]personTable.LASTNAME,\n" +
                "    age: [model::relational::tests::dbInc]personTable.AGE\n" +
                "  }\n" +
                "  model::Firm[firm]: Relational\n" +
                "  {\n" +
                "    legalName: [model::relational::tests::dbInc]firmTable.LEGALNAME\n" +
                "  }\n" +
                "   model::Employment : Relational\n" +
                "   {\n" +
                "      AssociationMapping\n" +
                "      (\n" +
                "         employees[firm,wrongID] : [model::relational::tests::dbInc]@Firm_Person,\n" + // target ID is incorrect
                "         firm[person, firm] : [model::relational::tests::dbInc]@Firm_Person\n" +
                "      )\n" +
                "   }" +
                ")", null, Arrays.asList("COMPILATION error at [107:34-80]: Unable to find target class mapping (id:wrongID) for property 'employees' in Association mapping 'model::Employment'. Make sure that you have specified a valid Class mapping id as the source id and target id, using the syntax 'property[sourceId, targetId]'."));


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
                "         employees : [model::relational::tests::dbInc]firmTable.LEGALNAME,\n" + // Not a Join
                "         firm : [model::relational::tests::dbInc]@Firm_Person\n" +
                "      )\n" +
                "   }" +
                ")", null, Arrays.asList("COMPILATION error at [107:20-73]: Mapping Error! on model::myRelationalMapping Expected a Join"));


        test(MODEL +
                "Class model::PersonExtended extends model::Person \n" +
                "{\n" +
                "    firmID : String[1];\n" +
                "}\n" +
                DB_INC +
                "###Mapping\n" +
                "Mapping model::myRelationalMapping\n" +
                "(\n" +
                "  model::PersonExtended[person]: Relational\n" +
                "  {\n" +
                "    firstName: [model::relational::tests::dbInc]personTable.FIRSTNAME,\n" +
                "    lastName: [model::relational::tests::dbInc]personTable.LASTNAME,\n" +
                "    age: [model::relational::tests::dbInc]personTable.AGE,\n" +
                "    firmID: [model::relational::tests::dbInc]@Firm_PersonFirm | firmTable.ID\n" + //Wrong table used in join on LHS
                "  }\n" +
                ")", "COMPILATION error at [97:11-76]: Mapping error: the join Firm_PersonFirm does not contain the source table personTable");


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
                "    age: [model::relational::tests::dbInc]@Firm_Person\n" + //not a property
                "  }\n" +
                "  model::Firm: Relational\n" +
                "  {\n" +
                "    legalName: [model::relational::tests::dbInc]firmTable.LEGALNAME\n" +
                "  }\n" +
                ")", null, Arrays.asList("COMPILATION error at [97:8-54]: Mapping error on mapping model::myRelationalMapping. The property 'age' returns a data type. However it's mapped to a Join."));

        test(MODEL +
                "Class model::OtherFirm extends model::LegalEntity \n" +
                "{\n" +
                "   legalName: String[1];\n" +
                "   address: Integer[1];\n" +
                "   employee: model::Person[0..1];\n" +
                "}\n" +
                "\n" +
                "Association model::Employment\n" +
                "{\n" +
                "    firm : model::Firm[0..1];\n" +
                "    employees : model::Person[*];\n" +
                "}\n" + DB_INC +
                "###Mapping\n" +
                "Mapping model::myRelationalMapping\n" +
                "(\n" +
                "  model::Person[person]: Relational\n" +
                "  {\n" +
                "    firstName: [model::relational::tests::dbInc]personTable.FIRSTNAME,\n" +
                "    lastName: [model::relational::tests::dbInc]personTable.LASTNAME,\n" +
                "    age: [model::relational::tests::dbInc]personTable.AGE,\n" +
                "    firm[OtherFirm]:[model::relational::tests::dbInc] @Person_OtherFirm \n" +  //wrong target ID and Join
                "  }\n" +
                "  model::Firm: Relational\n" +
                "  {\n" +
                "    legalName: [model::relational::tests::dbInc]firmTable.LEGALNAME\n" +
                "  }\n" +
                "  model::OtherFirm[OtherFirm]: Relational\n" +
                "  {\n" +
                "    legalName: [model::relational::tests::dbInc]firmTable.LEGALNAME\n" +
                "  }\n" +
                ")", null, Arrays.asList("COMPILATION error at [105:20-71]: Mapping Error: on model::myRelationalMapping The setImplementationId 'OtherFirm' is implementing the class 'OtherFirm' which is not a subType of 'Firm' return type of the mapped property 'firm'"));


        test(MODEL +
                "Association model::Employment\n" +
                "{\n" +
                "    firm : model::Firm[0..1];\n" +
                "    employees : model::Person[*];\n" +
                "}\n" +
                DB_INC +
                "###Mapping\n" +
                "Mapping model::myRelationalMapping\n" +
                "(\n" +
                "  model::Person[person]: Relational\n" +
                "  {\n" +
                "    firstName: [model::relational::tests::dbInc]personTable.FIRSTNAME,\n" +
                "    lastName: [model::relational::tests::dbInc]personTable.LASTNAME,\n" +
                "    age: [model::relational::tests::dbInc]personTable.AGE,\n" +
                "    firm:[model::relational::tests::dbInc]@Firm_Person | firmTable.ID \n" +  //Should be a JOIN
                "  }\n" +
                "  model::Firm: Relational\n" +
                "  {\n" +
                "    legalName: [model::relational::tests::dbInc]firmTable.LEGALNAME\n" +
                "  }\n" +
                ")", null, Arrays.asList("COMPILATION error at [93:3-99:3]: Mapping Error: on model::myRelationalMapping The property 'firm' doesn't return a data type. However it's mapped to a column or a function."));


        test(MODEL +
                "Association model::Employment\n" +
                "{\n" +
                "    firm : model::Firm[0..1];\n" +
                "    employees : model::Person[*];\n" +
                "}\n" +
                DB_INC +
                "###Mapping\n" +
                "Mapping model::myRelationalMapping\n" +
                "(\n" +
                "  model::Person[person]: Relational\n" +
                "  {\n" +
                "    firstName: [model::relational::tests::dbInc]personTable.FIRSTNAME,\n" +
                "    lastName: [model::relational::tests::dbInc]personTable.LASTNAME,\n" +
                "    age: [model::relational::tests::dbInc]personTable.AGE,\n" +
                "    firm:[model::relational::tests::dbInc] personTable.FIRMID \n" +  //Should be a JOIN
                "  }\n" +
                "  model::Firm: Relational\n" +
                "  {\n" +
                "    legalName: [model::relational::tests::dbInc]firmTable.LEGALNAME\n" +
                "  }\n" +
                ")", null, Arrays.asList("COMPILATION error at [93:3-99:3]: Mapping Error: on model::myRelationalMapping The target type:'Firm' on property firm is not a data type and a join is expected"));

        test(MODEL +
                "Association model::Employment\n" +
                "{\n" +
                "    firm : model::Firm[0..1];\n" +
                "    employees : model::Person[*];\n" +
                "}\n" +
                DB_INC +
                "###Mapping\n" +
                "Mapping model::myRelationalMapping\n" +
                "(\n" +
                "  model::Person[person]: Relational\n" +
                "  {\n" +
                "    firstName: [model::relational::tests::dbInc]personTable.FIRSTNAME,\n" +
                "    lastName: [model::relational::tests::dbInc]personTable.LASTNAME,\n" +
                "    age: [model::relational::tests::dbInc]personTable.AGE,\n" +
                "    firm:[model::relational::tests::dbInc]@Firm_Person \n" +  //Missing firm mapping  (Warning on properties)
                "  }\n" +
                ")", null, Arrays.asList("COMPILATION error at [98:9-54]: Error 'model_Firm' can't be found in the mapping model::myRelationalMapping"));

    }

    @Test
    public void testForMultipleRelationalConnections()
    {
        test("###Relational\n" +
                "Database relational::graphFetch::dbInc\n" +
                "(\n" +
                "  Table firmTable\n" +
                "  (\n" +
                "    FirmCode INTEGER PRIMARY KEY,\n" +
                "    FirmName VARCHAR(200)\n" +
                "  )\n" +
                ")\n" +
                "\n" +
                "###Pure\n" +
                "Class relational::graphFetch::Target_Firm\n" +
                "{\n" +
                "  firmName: String[1];\n" +
                "  firmCode: Integer[1];\n" +
                "}\n" +
                "\n" +
                "Class relational::graphFetch::Firm\n" +
                "{\n" +
                "  name: String[1];\n" +
                "  id: Integer[1];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping relational::graphFetch::Relational_Mapping\n" +
                "(\n" +
                "  *relational::graphFetch::Firm: Relational\n" +
                "  {\n" +
                "    ~primaryKey\n" +
                "    (\n" +
                "      [relational::graphFetch::dbInc]firmTable.FirmCode\n" +
                "    )\n" +
                "    ~mainTable [relational::graphFetch::dbInc]firmTable\n" +
                "    name: [relational::graphFetch::dbInc]firmTable.FirmName,\n" +
                "    id: [relational::graphFetch::dbInc]firmTable.FirmCode\n" +
                "  }\n" +
                ")\n" +
                "\n" +
                "Mapping relational::graphFetch::M2M_Mapping\n" +
                "(\n" +
                "  relational::graphFetch::Target_Firm: Pure\n" +
                "  {\n" +
                "    ~src relational::graphFetch::Firm\n" +
                "    firmName: $src.name->toUpper(),\n" +
                "    firmCode: $src.id\n" +
                "  }\n" +
                ")\n" +
                "\n" +
                "###Connection\n" +
                "RelationalDatabaseConnection relational::graphFetch::RelationalConnection\n" +
                "{\n" +
                "  type: H2;\n" +
                "  specification: Static\n" +
                "  {\n" +
                "    name: 'name';\n" +
                "    host: 'host';\n" +
                "    port: 1234;\n" +
                "  };\n" +
                "  auth: Test\n" +
                "  {\n" +
                "  };\n" +
                "}\n" +
                "\n" +
                "RelationalDatabaseConnection relational::graphFetch::SecondRelationalConnection\n" +
                "{\n" +
                "  store: relational::graphFetch::dbInc;\n" +
                "  type: H2;\n" +
                "  specification: Static\n" +
                "  {\n" +
                "    name: 'name';\n" +
                "    host: 'host';\n" +
                "    port: 1234;\n" +
                "  };\n" +
                "  auth: Test\n" +
                "  {\n" +
                "  };\n" +
                "}\n" +
                "\n" +
                "ModelChainConnection relational::graphFetch::OneMappingConnection\n" +
                "{\n" +
                "  mappings: [\n" +
                "    relational::graphFetch::Relational_Mapping\n" +
                "  ];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Runtime\n" +
                "Runtime relational::graphFetch::ModelChainConnectionRuntime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    relational::graphFetch::M2M_Mapping\n" +
                "  ];\n" +
                "  connections:\n" +
                "  [\n" +
                "    relational::graphFetch::dbInc:\n" +
                "    [\n" +
                "      connection_2: relational::graphFetch::RelationalConnection,\n" +
                "      connection_3: relational::graphFetch::SecondRelationalConnection\n" +
                "    ],\n" +
                "    ModelStore:\n" +
                "    [\n" +
                "      connection_1: relational::graphFetch::OneMappingConnection\n" +
                "    ]\n" +
                "  ];\n" +
                "}\n", "COMPILATION error at [89:1-107:1]: Found 2 connections against store [dbInc] under a single runtime.");
    }

    @Test
    public void testForRelationalRuntimeErrors()
    {
        test("###Relational\n" +
                "Database relational::graphFetch::dbInc\n" +
                "(\n" +
                "  Table firmTable\n" +
                "  (\n" +
                "    FirmCode INTEGER PRIMARY KEY,\n" +
                "    FirmName VARCHAR(200)\n" +
                "  )\n" +
                ")\n" +
                "\n" +
                "###Pure\n" +
                "Class relational::graphFetch::Target_Firm\n" +
                "{\n" +
                "  firmName: String[1];\n" +
                "  firmCode: Integer[1];\n" +
                "}\n" +
                "\n" +
                "Class relational::graphFetch::Firm\n" +
                "{\n" +
                "  name: String[1];\n" +
                "  id: Integer[1];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping relational::graphFetch::Relational_Mapping\n" +
                "(\n" +
                "  *relational::graphFetch::Firm: Relational\n" +
                "  {\n" +
                "    ~primaryKey\n" +
                "    (\n" +
                "      [relational::graphFetch::dbInc]firmTable.FirmCode\n" +
                "    )\n" +
                "    ~mainTable [relational::graphFetch::dbInc]firmTable\n" +
                "    name: [relational::graphFetch::dbInc]firmTable.FirmName,\n" +
                "    id: [relational::graphFetch::dbInc]firmTable.FirmCode\n" +
                "  }\n" +
                ")\n" +
                "\n" +
                "Mapping relational::graphFetch::M2M_Mapping\n" +
                "(\n" +
                "  relational::graphFetch::Target_Firm: Pure\n" +
                "  {\n" +
                "    ~src relational::graphFetch::Firm\n" +
                "    firmName: $src.name->toUpper(),\n" +
                "    firmCode: $src.id\n" +
                "  }\n" +
                ")\n" +
                "\n" +
                "###Connection\n" +
                "RelationalDatabaseConnection relational::graphFetch::RelationalConnection\n" +
                "{\n" +
                "  store: relational::graphFetch::dbInc;\n" +
                "  type: H2;\n" +
                "  specification: Static\n" +
                "  {\n" +
                "    name: 'name';\n" +
                "    host: 'host';\n" +
                "    port: 1234;\n" +
                "  };\n" +
                "  auth: Test\n" +
                "  {\n" +
                "  };\n" +
                "}\n" +
                "\n" +
                "RelationalDatabaseConnection relational::graphFetch::SecondRelationalConnection\n" +
                "{\n" +
                "  store: relational::graphFetch::dbInc;\n" +
                "  type: H2;\n" +
                "  specification: Static\n" +
                "  {\n" +
                "    name: 'name';\n" +
                "    host: 'host';\n" +
                "    port: 1234;\n" +
                "  };\n" +
                "  auth: Test\n" +
                "  {\n" +
                "  };\n" +
                "}\n" +
                "\n" +
                "ModelChainConnection relational::graphFetch::OneMappingConnection\n" +
                "{\n" +
                "  mappings: [\n" +
                "    relational::graphFetch::Relational_Mapping\n" +
                "  ];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Runtime\n" +
                "Runtime relational::graphFetch::ModelChainConnectionRuntime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    relational::graphFetch::M2M_Mapping\n" +
                "  ];\n" +
                "  connectionStores:\n" +
                "  [\n" +
                "    relational::graphFetch::RelationalConnection:\n" +
                "    [\n" +
                "      relational::graphFetch::dbInc\n" +
                "    ],\n" +
                "    relational::graphFetch::SecondRelationalConnection:\n" +
                "    [\n" +
                "      relational::graphFetch::dbInc\n" +
                "    ]\n" +
                "  ];\n" +
                "}\n", "COMPILATION error at [90:1-107:1]: Found 2 connections against store [dbInc] under a single runtime.");
    }

    @Test
    public void testCompilationMissingEnumMapping() throws Exception
    {
        Pair<PureModelContextData, PureModel> res = test(
                "###Relational\n" +
                        "Database test::DB\n" +
                        "(\n" +
                        "  Table employee\n" +
                        "  (\n" +
                        "    type VARCHAR(200)\n" +
                        "  )\n" +
                        ")\n" +
                        "###Pure\n" +
                        "Enum test::EmployeeType\n" +
                        "{\n" +
                        "  FULL_TIME\n" +
                        "}\n" +
                        "Class test::Employee\n" +
                        "{\n" +
                        "  type: test::EmployeeType[1];\n" +
                        "}\n" +
                        "###Mapping\n" +
                        "Mapping test::Map\n" +
                        "(\n" +
                        "  *test::Employee: Relational\n" +
                        "  {\n" +
                        "    ~mainTable [test::DB]employee\n" +
                        "    type: [test::DB]employee.type\n" +
                        "  }\n" +
                        ")", null, Arrays.asList("COMPILATION error at [24:9-33]: Missing an EnumerationMapping for the enum property 'type'. Enum properties require an EnumerationMapping in order to transform the store values into the Enum."));

        MutableList<Warning> warnings = res.getTwo().getWarnings();
        Assert.assertEquals(1, warnings.size());
        Assert.assertEquals("{\"sourceInformation\":{\"sourceId\":\"\",\"startLine\":24,\"startColumn\":9,\"endLine\":24,\"endColumn\":33},\"message\":\"Missing an EnumerationMapping for the enum property 'type'. Enum properties require an EnumerationMapping in order to transform the store values into the Enum.\"}", new ObjectMapper().writeValueAsString(warnings.get(0)));
    }

    @Test
    public void testMappingToPropertyWithDerivedPropertyConflict()
    {
        test("###Relational\n" +

                "Database demo::stores::Db\n" +
                "(\n" +
                "  Schema First\n" +
                "  (\n" +
                "    Table Source\n" +
                "    (\n" +
                "      accountNumber INTEGER PRIMARY KEY,\n" +
                "      name VARCHAR(200)\n" +
                "    )\n" +
                "  )\n" +
                ")\n" +
                "\n" +
                "\n" +
                "###Pure\n" +
                "Class demo::model::ChildClass extends demo::model::BaseClass, demo::model::BaseClass2 \n" +
                "{\n" +
                "  Name: String[1];\n" +
                "  accountNumber(num: Integer[1],name: String[1]) {$num + $name->size()}: Integer[1];\n" +
                "}\n" +
                "\n" +
                "Class demo::model::BaseClass2 extends demo::model::BaseClass2Parent\n" +
                "{\n" +
                "}\n" +
                "\n" +
                "Class demo::model::BaseClass extends demo::model::BaseClassParent\n" +
                "{\n" +
                "}\n" +
                "\n" +
                "Class demo::model::BaseClassParent\n" +
                "{\n" +
                "}\n" +
                "\n" +
                "Class demo::model::BaseClass2Parent\n" +
                "{\n" +
                "  accountNumber: Integer[1];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping demo::mapping::Map1\n" +
                "(\n" +
                "  *demo::model::ChildClass: Relational\n" +
                "  {\n" +
                "    ~primaryKey\n" +
                "    (\n" +
                "      [demo::stores::Db]First.Source.accountNumber\n" +
                "    )\n" +
                "    ~mainTable [demo::stores::Db]First.Source\n" +
                "    Name: [demo::stores::Db]First.Source.name,\n" +
                "    accountNumber: [demo::stores::Db]First.Source.accountNumber\n" +
                "  }\n" +
                ")\n");
    }

    @Test
    public void testFilterFromNonIncludedDatabase()
    {
        test("###Relational\n" +
                "Database app::myDb\n" +
                "(\n" +
                "    include app::dbInc\n" +
                "    Table myTable(EVENT_ID INT PRIMARY KEY, trade_id INT, eventType VARCHAR(10), eventDate DATE, person_id INT)\n" +
                "    View myView\n" +
                "    (\n" +
                "       maxTradeEventDate : max(myTable.eventDate)\n" +
                "    )\n" +
                "    Filter EventFilter(myTable.eventType = 'myevent')\n" +
                ")\n\n" +
                "Database app::dbInc\n" +
                "(\n" +
                "    Table tradeEventTable(EVENT_ID INT PRIMARY KEY, trade_id INT, eventType VARCHAR(10), eventDate DATE, person_id INT)\n" +
                "    View tradeEventViewMaxTradeEventDate\n" +
                "    (\n" +
                "       ~filter [app::myDb]EventFilter\n" +
                "       maxTradeEventDate : max(tradeEventTable.eventDate)\n" +
                "    )\n" +
                ")");
    }

    @Test
    public void testDuplicateNameForJoins()
    {
        test("###Relational\n" +
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
                "  Join Firm_Person(firmTable.ADDRESSID = personTable.ADDRESSID)\n" +
                ")", null, Arrays.asList("COMPILATION error at [21:3-53]: Found joins with duplicate names: Firm_Person", "COMPILATION error at [22:3-63]: Found joins with duplicate names: Firm_Person"));
    }

    @Test
    public void testDuplicateNameForFilters()
    {
        test("###Relational\n" +
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
                "  Filter FirmFilter(firmTable.LEGALNAME = 'myevent')\n" +
                "  Filter FirmFilter(firmTable.ADDRESSID = 1)\n" +
                ")", null, Arrays.asList("COMPILATION error at [21:3-52]: Found filters with duplicate names: FirmFilter", "COMPILATION error at [22:3-44]: Found filters with duplicate names: FirmFilter"));
    }

}
