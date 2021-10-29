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

import org.junit.Test;

public class TestRelationalGrammarRoundtrip extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{
    @Test
    public void testRelationalSimpleFull()
    {
        test("Class simple::Person\n" +
                "{\n" +
                "  firstName: String[1];\n" +
                "  lastName: String[1];\n" +
                "  otherNames: String[*];\n" +
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
                "  Schema productSchema\n" +
                "  (\n" +
                "    Table productTable\n" +
                "    (\n" +
                "      ID INTEGER PRIMARY KEY,\n" +
                "      NAME VARCHAR(200)\n" +
                "    )\n" +
                "  )\n" +
                "\n" +
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
                "  Table differentPersonTable\n" +
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
                "  Table otherFirmTable\n" +
                "  (\n" +
                "    ID INTEGER PRIMARY KEY,\n" +
                "    LEGALNAME VARCHAR(200),\n" +
                "    ADDRESSID INTEGER\n" +
                "  )\n" +
                "  Table addressTable\n" +
                "  (\n" +
                "    ID INTEGER PRIMARY KEY,\n" +
                "    TYPE INTEGER,\n" +
                "    NAME VARCHAR(200),\n" +
                "    STREET VARCHAR(100),\n" +
                "    COMMENTS VARCHAR(100)\n" +
                "  )\n" +
                "  Table locationTable\n" +
                "  (\n" +
                "    ID INTEGER PRIMARY KEY,\n" +
                "    PERSONID INTEGER,\n" +
                "    PLACE VARCHAR(200),\n" +
                "    date DATE\n" +
                "  )\n" +
                "  Table placeOfInterestTable\n" +
                "  (\n" +
                "    ID INTEGER PRIMARY KEY,\n" +
                "    locationID INTEGER PRIMARY KEY,\n" +
                "    NAME VARCHAR(200)\n" +
                "  )\n" +
                "\n" +
                "  Join Address_Firm(addressTable.ID = firmTable.ADDRESSID)\n" +
                "  Join Address_Person(addressTable.ID = personTable.ADDRESSID = personTable.ADDRESSID)\n" +
                "  Join Firm_Ceo(firmTable.CEOID = personTable.ID)\n" +
                "  Join Firm_Person(firmTable.ID = personTable.FIRMID)\n" +
                "  Join Person_Location(personTable.ID = locationTable.PERSONID)\n" +
                "  Join location_PlaceOfInterest(locationTable.ID = placeOfInterestTable.locationID)\n" +
                "  Join Person_OtherFirm(personTable.FIRMID = otherFirmTable.ID)\n" +
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
                "    age: [simple::dbInc]personTable.AGE,\n" +
                "    lastName: [simple::dbInc]personTable.LASTNAME,\n" +
                "    firm: [simple::dbInc]@Firm_Person,\n" +
                "    locations: [simple::dbInc]@Person_Location,\n" +
                "    manager: [simple::dbInc]@Person_Manager\n" +
                "  }\n" +
                "  simple::Firm[simple_Firm]: Relational\n" +
                "  {\n" +
                "    ~mainTable [simple::dbInc]firmTable\n" +
                "    legalName: [simple::dbInc]firmTable.LEGALNAME,\n" +
                "    employees: [simple::dbInc]@Firm_Person\n" +
                "  }\n" +
                "\n" +
                "  PlacesOfInterest: Relational\n" +
                "  {\n" +
                "    AssociationMapping\n" +
                "    (\n" +
                "      location: [dbInc]@location_PlaceOfInterest,\n" +
                "      placeOfInterest: [dbInc]@location_PlaceOfInterest\n" +
                "    )\n" +
                "  }\n\n" +
                "  simple::GeographicEntityType: EnumerationMapping GE\n" +
                "  {\n" +
                "    CITY: [1]\n" +
                "  }\n" +
                ")\n");
    }

    @Test
    public void testRelationalDatabase()
    {
        test("###Relational\n" +
                "Database test::db\n" +
                "(\n" +
                "  include Store1\n" +
                "  include test::Store2\n" +
                "\n" +
                "  Schema mySchema\n" +
                "  (\n" +
                "  )\n" +
                "\n" +
                "  Table table1\n" +
                "  (\n" +
                "    col1 CHAR(32)\n" +
                "  )\n" +
                "  Table table2\n" +
                "  (\n" +
                "    col1 CHAR(32)\n" +
                "  )\n" +
                "\n" +
                "  View view1\n" +
                "  (\n" +
                "    ~filter filter1\n" +
                "    col1: test.col1\n" +
                "  )\n" +
                "  View view2\n" +
                "  (\n" +
                "    ~filter filter2\n" +
                "    col2: test.col2\n" +
                "  )\n" +
                "\n" +
                "  Join join1(TAB.col1 = {target}.col2)\n" +
                "  Join join2(TAB2.col3 = TAB3.col2)\n" +
                "\n" +
                "  Filter filter1(TAB2.col1 is not null)\n" +
                "  MultiGrainFilter filter2(TAB1.col2 is null)\n" +
                ")\n");
    }

    @Test
    public void testRelationalDatabaseTableColumnDataType()
    {
        String unformatted = "###Relational\n" +
                "\n" +
                "Database sample::test\n" +
                "(\n" +
                "  Table table\n" +
                "  (\n" +
                "      prop1 CHAR(32),\n" +
                "      prop2 VARCHAR(32),\n" +
                "      prop3 NUMERIC(32,23),\n" +
                "      prop4 DECIMAL(32,23),\n" +
                "      prop5 FLOAT,\n" +
                "      prop6 DOUBLE PRIMARY KEY,\n" +
                "      prop7 REAL,\n" +
                "      prop8 INT,\n" + // will get converted to INTEGER
                "      prop9 INTEGER,\n" +
                "      prop10 BIGINT NOT NULL,\n" +
                "      prop11 SMALLINT PRIMARY KEY,\n" +
                "      prop12 TINYINT,\n" +
                "      prop13 DATE,\n" +
                "      prop14 TIMESTAMP,\n" +
                "      prop15 BINARY(1),\n" +
                "      prop16 VARBINARY(1) NOT NULL,\n" +
                "      prop17 BIT,\n" +
                "      prop18 ARRAY,\n" + // will get converted to OTHER
                "      prop19 OTHER\n" +
                "   )\n" +
                ")";
        testFormat("###Relational\n" +
                "Database sample::test\n" +
                "(\n" +
                "  Table table\n" +
                "  (\n" +
                "    prop1 CHAR(32),\n" +
                "    prop2 VARCHAR(32),\n" +
                "    prop3 NUMERIC(32, 23),\n" +
                "    prop4 DECIMAL(32, 23),\n" +
                "    prop5 FLOAT,\n" +
                "    prop6 DOUBLE PRIMARY KEY,\n" +
                "    prop7 REAL,\n" +
                "    prop8 INTEGER,\n" +
                "    prop9 INTEGER,\n" +
                "    prop10 BIGINT NOT NULL,\n" +
                "    prop11 SMALLINT PRIMARY KEY,\n" +
                "    prop12 TINYINT,\n" +
                "    prop13 DATE,\n" +
                "    prop14 TIMESTAMP,\n" +
                "    prop15 BINARY(1),\n" +
                "    prop16 VARBINARY(1) NOT NULL,\n" +
                "    prop17 BIT,\n" +
                "    prop18 OTHER,\n" +
                "    prop19 OTHER\n" +
                "  )\n" +
                ")\n", unformatted);
    }

    @Test
    public void testRelationalDatabaseTableWithMilestoning()
    {
        String unformatted = "###Relational\n" +
                "Database sample::test\n" +
                "(\n" +
                "  Table table\n" +
                "  (\n" +
                "    milestoning\n" +
                "    (\n" +
                // processing
                "      processing(PROCESSING_IN = dummy, PROCESSING_OUT = dummy),\n" +
                "      processing(PROCESSING_IN = dummy, PROCESSING_OUT = dummy, OUT_IS_INCLUSIVE = false, INFINITY_DATE = %9999-12-30T19:00:00.0000),\n" +
                "      processing(PROCESSING_IN = dummy, PROCESSING_OUT = dummy, OUT_IS_INCLUSIVE = true, INFINITY_DATE = %9999-12-30T19:00:00.0000),\n" +
                "      processing(PROCESSING_IN = dummy, PROCESSING_OUT = dummy, INFINITY_DATE = %9999-12-30T19:00:00.0000),\n" +
                // business snapshot
                "      business(BUS_SNAPSHOT_DATE = BUSINESS_DATE),\n" +
                // business
                "      business(BUS_FROM = dummy, BUS_THRU = dummy),\n" +
                "      business(BUS_FROM = dummy, BUS_THRU = dummy, THRU_IS_INCLUSIVE = false, INFINITY_DATE = %9999-12-30T19:00:00.0000),\n" +
                "      business(BUS_FROM = dummy, BUS_THRU = dummy, THRU_IS_INCLUSIVE = true, INFINITY_DATE = %9999-12-30T19:00:00.0000),\n" +
                "      business(BUS_FROM = dummy, BUS_THRU = dummy, INFINITY_DATE = %9999-12-30T19:00:00.0000)\n" +
                "    )\n" +
                "  )\n" +
                ")\n";
        testFormat("###Relational\n" +
                "Database sample::test\n" +
                "(\n" +
                "  Table table\n" +
                "  (\n" +
                "    milestoning\n" +
                "    (\n" +
                "      processing(PROCESSING_IN = dummy, PROCESSING_OUT = dummy),\n" +
                "      processing(PROCESSING_IN = dummy, PROCESSING_OUT = dummy, INFINITY_DATE = %9999-12-30T19:00:00.0000),\n" +
                "      processing(PROCESSING_IN = dummy, PROCESSING_OUT = dummy, OUT_IS_INCLUSIVE = true, INFINITY_DATE = %9999-12-30T19:00:00.0000),\n" +
                "      processing(PROCESSING_IN = dummy, PROCESSING_OUT = dummy, INFINITY_DATE = %9999-12-30T19:00:00.0000),\n" +
                "      business(BUS_SNAPSHOT_DATE = BUSINESS_DATE),\n" +
                "      business(BUS_FROM = dummy, BUS_THRU = dummy),\n" +
                "      business(BUS_FROM = dummy, BUS_THRU = dummy, INFINITY_DATE = %9999-12-30T19:00:00.0000),\n" +
                "      business(BUS_FROM = dummy, BUS_THRU = dummy, THRU_IS_INCLUSIVE = true, INFINITY_DATE = %9999-12-30T19:00:00.0000),\n" +
                "      business(BUS_FROM = dummy, BUS_THRU = dummy, INFINITY_DATE = %9999-12-30T19:00:00.0000)\n" +
                "    )\n" +
                "  )\n" +
                ")\n", unformatted);
    }

    @Test
    public void testRelationalDatabaseView()
    {
        String unformatted = "###Relational\n" +
                "Database sample::test\n" +
                "(\n" +
                "  View SomeView\n" +
                "  (\n" +
                "    ~filter someFilter\n" +
                "    ~groupBy (data.SOMETHING.TEST, data.SOMETHING_ELSE.TEST)\n" +
                "    ~distinct\n" +
                "    id: test.prop PRIMARY KEY,\n" +
                "    name: test.prop,\n" +
                "    site_url_namespace: @sites | public.sites.url_namespace,\n" +
                "    prefix: plus('/prefix/t/', @sites | public.sites.url, '/w/', public.repository_url, '/')\n" +
                "  )\n" +
                ")\n";
        testFormat("###Relational\n" +
                "Database sample::test\n" +
                "(\n" +
                "  View SomeView\n" +
                "  (\n" +
                "    ~filter someFilter\n" +
                "    ~groupBy\n" +
                "    (\n" +
                "      data.SOMETHING.TEST,\n" +
                "      data.SOMETHING_ELSE.TEST\n" +
                "    )\n" +
                "    ~distinct\n" +
                "    id: test.prop PRIMARY KEY,\n" +
                "    name: test.prop,\n" +
                "    site_url_namespace: [sample::test]@sites | public.sites.url_namespace,\n" +
                "    prefix: plus('/prefix/t/', [sample::test]@sites | public.sites.url, '/w/', public.repository_url, '/')\n" +
                "  )\n" +
                ")\n", unformatted);
    }

    @Test
    public void testRelationalOperations()
    {
        String unformatted = "###Relational\n" +
                "Database sample::test\n" +
                "(\n" +
                "  View SomeView\n" +
                "  (\n" +
                "    prefix: case(\n" +
                // atomic operations
                "              tab.val = 20, 1,\n" +
                "              tab.val != 20, 1,\n" +
                "              tab.val < 20, 1,\n" +
                "              tab.val > 20, 1,\n" +
                "              tab.val <= 20, 1,\n" +
                "              tab.val >= 20, 1,\n" +
                "              tab.val <> 20, 1,\n" +
                // null
                "              tab.val is null, 1,\n" +
                "              tab.val is not null, 1,\n" +
                // boolean operations
                "              tab.val and tab.val, 1,\n" +
                "              tab.val and tab.val or tab.val, 1,\n" + // we should see the later part of the boolean expression wrapped in a group () to disambiguate
                "              tab.val > 20 and tab.val < 100, 1,\n" +
                "              tab.val > 20 or tab.val <= 100, 1,\n" +
                "              (tab.val > 20 and tab.val <= 100) or tab.val <> 2, 1,\n" +
                // function operation
                "              substring(tab.val) > 100, 1,\n" +
                "              sqlNull()\n" +
                "            ) > 2\n" +
                "  )\n" +
                "  Join Abe([test::TEST_DB2]a.col1 > 2 and [test::TEST_DB2]b.col1 > 3 or [test::TEST_DB2]b.col1 > 4)\n" + // we should see the later part of the boolean expression wrapped in a group () to disambiguate
                ")\n";
        testFormat("###Relational\n" +
                "Database sample::test\n" +
                "(\n" +
                "  View SomeView\n" +
                "  (\n" +
                "    prefix: case(tab.val = 20, 1, tab.val != 20, 1, tab.val < 20, 1, tab.val > 20, 1, tab.val <= 20, 1, tab.val >= 20, 1, tab.val <> 20, 1, tab.val is null, 1, tab.val is not null, 1, tab.val and tab.val, 1, tab.val and (tab.val or tab.val), 1, tab.val > 20 and tab.val < 100, 1, tab.val > 20 or tab.val <= 100, 1, (tab.val > 20 and tab.val <= 100) or tab.val <> 2, 1, substring(tab.val) > 100, 1, sqlNull()) > 2\n" +
                "  )\n" +
                "\n" +
                "  Join Abe([test::TEST_DB2]a.col1 > 2 and ([test::TEST_DB2]b.col1 > 3 or [test::TEST_DB2]b.col1 > 4))\n" +
                ")\n", unformatted);
    }

    @Test
    public void testRelationalAtomicOperationInFunctionalForm()
    {
        String unformatted = "###Relational\n" +
                "Database sample::test\n" +
                "(\n" +
                "  View SomeView\n" +
                "  (\n" +
                "    prefix: greaterThan(case(\n" +
                // atomic operations
                "              equal(tab.val, 20), 1,\n" +
                "              notEqual(tab.val, 20), 1,\n" +
                "              lessThan(tab.val, 20), 1,\n" +
                "              greaterThan(tab.val, 20), 1,\n" +
                "              lessThanEqual(tab.val, 20), 1,\n" +
                "              greaterThanEqual(tab.val, 20), 1,\n" +
                "              notEqualAnsi(tab.val, 20), 1,\n" +
                // null
                "              isNull(tab.val), 1,\n" +
                "              isNotNull(tab.val), 1,\n" +
                // boolean operations
                "              and(tab.val, tab.val), 1,\n" +
                "              and(tab.val, group(or(tab.val, tab.val))), 1,\n" +
                "              and(greaterThan(tab.val, 20), lessThan(tab.val, 100)), 1,\n" +
                "              or(greaterThan(tab.val, 20), lessThanEqual(tab.val, 100)), 1,\n" +
                "              or(group(and(greaterThan(tab.val, 20), lessThanEqual(tab.val, 100))), notEqualAnsi(tab.val, 2)), 1,\n" +
                // function operation
                "              greaterThan(substring(tab.val), 100), 1,\n" +
                "              sqlNull()\n" +
                "            ), 2)\n" +
                "  )\n" +
                "  Join Abe(and(greaterThan([test::TEST_DB2]a.col1, 2), group(or(greaterThan([test::TEST_DB2]b.col1, 3), greaterThan([test::TEST_DB2]b.col1, 4)))))\n" +
                ")\n";

        testFormat("###Relational\n" +
                "Database sample::test\n" +
                "(\n" +
                "  View SomeView\n" +
                "  (\n" +
                "    prefix: case(tab.val = 20, 1, tab.val != 20, 1, tab.val < 20, 1, tab.val > 20, 1, tab.val <= 20, 1, tab.val >= 20, 1, tab.val <> 20, 1, tab.val is null, 1, tab.val is not null, 1, tab.val and tab.val, 1, tab.val and (tab.val or tab.val), 1, tab.val > 20 and tab.val < 100, 1, tab.val > 20 or tab.val <= 100, 1, (tab.val > 20 and tab.val <= 100) or tab.val <> 2, 1, substring(tab.val) > 100, 1, sqlNull()) > 2\n" +
                "  )\n" +
                "\n" +
                "  Join Abe([test::TEST_DB2]a.col1 > 2 and ([test::TEST_DB2]b.col1 > 3 or [test::TEST_DB2]b.col1 > 4))\n" +
                ")\n", unformatted);
    }

    @Test
    public void testRelationalMapping()
    {
        test("###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "  CM1[CM1]: Relational\n" +
                "  {\n" +
                "    ~filter [TEST_DB]filter_1\n" +
                "    ~distinct\n" +
                "    ~groupBy\n" +
                "    (\n" +
                "      [TEST_DB2]test_1.test_2.test_3,\n" +
                "      [TEST_DB3]test_1.test_1\n" +
                "    )\n" +
                "    ~primaryKey\n" +
                "    (\n" +
                "      [TEST_DB]test1.test2.test3,\n" +
                "      [TEST_DB]test4.test6\n" +
                "    )\n" +
                "    ~mainTable [TEST_DB00]test1.test2\n" +
                // local mapping property
                "    +prop1: test::Class[*]: [TEST_DB]test.someCol,\n" +
                "    +prop2: test::Class[0..1]: [TEST_DB]test.someCol,\n" +
                "    +prop3: Integer[7]: [TEST_DB]test.someCol,\n" +
                "    prop4: [TEST_DB]test.someCol,\n" +
                "    prop5: [TEST_DB]test.someCol\n" +
                "  }\n" +
                ")\n");
    }

    @Test
    public void testRelationalMappingScope()
    {
        // test scope being flattened out
        String unformatted = "###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "  CM1: Relational\n" +
                "  {\n" +
                // this scope should flat out
                "    scope([TEST_SCOPE]TEST.another)\n" +
                "    (\n" +
                "      name: stateProvNameTXT,\n" +
                "      subdivisionCategory: if(equal(substring(stateProv, 1, 2), 'US'), 'STATE', sqlNull())\n" +
                "    ),\n" +
                // this scope should flat out
                "    scope([TEST_SCOPE]TEST.something)\n" +
                "    (\n" +
                "      code: nodeCODE,\n" +
                "      scheme: @join_1,\n" +
                "      parent: @join_2,\n" +
                "      hierarchyLevel: case(equal(test_val, 20), 1, equal(test_val, 30), 2, equal(test_val, 40), 3, equal(test_val, 55), 4, equal(test_val, 60), 5, sqlNull()),\n" +
                "      prop1: divide(if(equal([TEST_DB2]@join_1 > (OUTER) [TEST_DB2]@join_2 | toString([TEST_DB2]SOMETHING.something.toValue), '0903'), 0.0, [TEST_DB2]@join_3 > (INNER) [TEST_DB2]@join_4 | parseFloat([TEST_DB2]schema1.table2.col3)), if(isEmpty(factor), 1.0, divide(1, factor))),\n" +
                "      part[part_TEST]: [TEST_SCOPE]@join_1 > @join_2\n" +
                "    ),\n" +
                // no scope so no flat out
                "    prop1: divide(if(equal([TEST_DB2]@join_1 > (OUTER) [TEST_DB2]@join_2 | toString([TEST_DB2]SOMETHING.something.toValue), '0903'), 0.0, [TEST_DB2]@join_3 > (INNER) [TEST_DB2]@join_4 | parseFloat([TEST_DB2]schema1.table2.col3)), if(isEmpty(table.factor), 1.0, divide(1, table.factor)))\n" +
                "  }\n" +
                ")\n";
        testFormat("###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "  CM1: Relational\n" +
                "  {\n" +
                "    name: [TEST_SCOPE]TEST.another.stateProvNameTXT,\n" +
                "    subdivisionCategory: if(substring([TEST_SCOPE]TEST.another.stateProv, 1, 2) = 'US', 'STATE', sqlNull()),\n" +
                "    code: [TEST_SCOPE]TEST.something.nodeCODE,\n" +
                "    scheme: [TEST_SCOPE]@join_1,\n" +
                "    parent: [TEST_SCOPE]@join_2,\n" +
                "    hierarchyLevel: case([TEST_SCOPE]TEST.something.test_val = 20, 1, [TEST_SCOPE]TEST.something.test_val = 30, 2, [TEST_SCOPE]TEST.something.test_val = 40, 3, [TEST_SCOPE]TEST.something.test_val = 55, 4, [TEST_SCOPE]TEST.something.test_val = 60, 5, sqlNull()),\n" +
                "    prop1: divide(if([TEST_DB2]@join_1 > (OUTER) [TEST_DB2]@join_2 | toString([TEST_DB2]SOMETHING.something.toValue) = '0903', 0.0, [TEST_DB2]@join_3 > (INNER) [TEST_DB2]@join_4 | parseFloat([TEST_DB2]schema1.table2.col3)), if(isEmpty([TEST_SCOPE]TEST.something.factor), 1.0, divide(1, [TEST_SCOPE]TEST.something.factor))),\n" +
                "    part[part_TEST]: [TEST_SCOPE]@join_1 > [TEST_SCOPE]@join_2,\n" +
                "    prop1: divide(if([TEST_DB2]@join_1 > (OUTER) [TEST_DB2]@join_2 | toString([TEST_DB2]SOMETHING.something.toValue) = '0903', 0.0, [TEST_DB2]@join_3 > (INNER) [TEST_DB2]@join_4 | parseFloat([TEST_DB2]schema1.table2.col3)), if(isEmpty(table.factor), 1.0, divide(1, table.factor)))\n" +
                "  }\n" +
                ")\n", unformatted);
    }

    @Test
    public void testEmbeddedRelationalMapping()
    {
        test("###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "  EmbeddedCM[dummy]: Relational\n" +
                "  {\n" +
                "    something() Inline[TEST_Id],\n" +
                "    prop1WithEmbedded\n" +
                "    (\n" +
                // deeply nested
                "      testProp2\n" +
                "      (\n" +
                "        size: 1,\n" +
                "        testProp2\n" +
                "        (\n" +
                "          size: 1,\n" +
                "          testProp2\n" +
                "          (\n" +
                "            size: 1\n" +
                "          ),\n" +
                "          size: 1\n" +
                "        )\n" +
                "      ),\n" +
                "      contractId() Inline[TEST_Id],\n" +
                "      something() Inline[TEST_Id],\n" +
                "      prop6: EnumerationMapping testEnumerationMapping: 'SOME_CONSTANT',\n" +
                "      prop3[id1]: [TEST_Store]@SOME_JOIN1 > [TEST_Store]@SOME_JOIN2,\n" +
                "      prop4[id2]: [TEST_Store]@SOME_JOIN\n" +
                "    ),\n" +
                // inline embedded property mapping
                "    something() Inline[TEST_Id],\n" +
                "    something() Inline[TEST_Id],\n" +
                "    client\n" +
                "    (\n" +
                "      id: [DummyStore]test.RESULT.CLIENT_ID,\n" +
                "      size: 1,\n" +
                "      testProp2\n" +
                "      (\n" +
                "        size: 1\n" +
                "      )\n" +
                // otherwise embedded property mapping
                "    ) Otherwise ([part_TEST]: [DummyStore]@Testing)\n" +
                "  }\n" +
                ")\n");
    }

    @Test
    public void testAssociationRelationalMapping()
    {
        test("###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "  test::SomeAssociationMapping: Relational\n" +
                "  {\n" +
                "    AssociationMapping\n" +
                "    (\n" +
                "      prop1[prop2_source,prop1_source]: [model::Test]@RandomJoin,\n" +
                "      prop2[prop1_source,prop2_source]: [model::Test]@RandomJoin\n" +
                "    )\n" +
                "  }\n" +
                ")\n");
    }

    @Test
    public void testRelationalPropertyMappingWithFunctionOperationAndDatabasePointer()
    {
        String unformatted = "###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "  test::TestScopeCM: Relational\n" +
                "  {\n" +
                "    feeAmount: [db] plus(table.quantity, table.quantity)\n" +
                "  }\n" +
                ")\n";
        testFormat("###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "  test::TestScopeCM: Relational\n" +
                "  {\n" +
                "    feeAmount: plus([db]table.quantity, [db]table.quantity)\n" +
                "  }\n" +
                ")\n", unformatted);
    }

    @Test
    public void testMappingWithIncludedMapping()
    {
        test("###Mapping\n" +
                "Mapping test::includedRelationalMapping\n" +
                "(\n" +
                "  simple::Person[simple_Person]: Relational\n" +
                "  {\n" +
                "    ~mainTable [dbInc]personTable\n" +
                "    firstName: [dbInc]personTable.FIRSTNAME,\n" +
                "    lastName: [dbInc]personTable.LASTNAME,\n" +
                "    age: [dbInc]personTable.AGE\n" +
                "  }\n" +
                ")\n\n" +
                "Mapping test::simpleRelationalMapping\n" +
                "(\n" +
                "  include test::includedRelationalMapping[dbInc->db]\n\n" +
                "  simple::Firm[simple_Firm]: Relational\n" +
                "  {\n" +
                "    ~mainTable [db]firmTable\n" +
                "    legalName: [db]firmTable.LEGALNAME,\n" +
                "    employees: [db]@Firm_Person\n" +
                "  }\n" +
                ")\n");
    }

    @Test
    public void testMappingWithRelationalTestSQL()
    {
        test("###Mapping\n" +
                "Mapping meta::pure::mapping::modelToModel::test::simple::simpleModelMapping\n" +
                "(\n" +
                "  MappingTests\n" +
                "  [\n" +
                "    test2\n" +
                "    (\n" +
                "      query: |model::domain::Target.all()->graphFetchChecked(#{model::domain::Target{name}}#)->serialize(#{model::domain::Target{name}}#);\n" +
                "      data:\n" +
                "      [\n" +
                "        <Relational, SQL, aa::db, \n" +
                "          'Drop table if exists PersonTable;\\n'+\n" +
                "          'Create Table PersonTable(id INT, firmId INT, lastName VARCHAR(200));\\n'+\n" +
                "          'Insert into PersonTable (id, firmId, lastName) values (1, 1, \\'Doe\\;\\');\\n'+\n" +
                "          'Insert into PersonTable (id, firmId, lastName) values (2, 1, \\'Doe2\\');\\n'\n" +
                "        >,\n" +
                "        <Relational, aa::db, [model::textElement::SQLData]>\n" +
                "      ];\n" +
                "      assert: '{\"defects\":[],\"value\":{\"name\":\"oneName 99\"},\"source\":{\"defects\":[],\"value\":{\"oneName\":\"oneName 99\"},\"source\":{\"number\":1,\"record\":\"{\\\"oneName\\\":\\\"oneName 99\\\",\\\"anotherName\\\":\\\"anotherName 17\\\",\\\"oneDate\\\":\\\"2020-04-13\\\",\\\"anotherDate\\\":\\\"2020-02-25\\\",\\\"oneNumber\\\":27,\\\"anotherNumber\\\":28}\"}}}';\n" +
                "    )\n" +
                "  ]\n" +
                ")\n");
    }

    @Test
    public void testMappingWithRelationalTestCSV()
    {
        test("###Mapping\n" +
                "Mapping meta::pure::mapping::modelToModel::test::simple::simpleModelMapping\n" +
                "(\n" +
                "  MappingTests\n" +
                "  [\n" +
                "    test2\n" +
                "    (\n" +
                "      query: |model::domain::Target.all()->graphFetchChecked(#{model::domain::Target{name}}#)->serialize(#{model::domain::Target{name}}#);\n" +
                "      data:\n" +
                "      [\n" +
                "        <Relational, CSV, aa::db, \n" +
                "          'default\\n'+\n" +
                "          'PersonTable\\n'+\n" +
                "          'id,lastName\\n'+\n" +
                "          '1,Doe;\\n'+\n" +
                "          '2,Doe2\\n'+\n" +
                "          '\\n\\n\\n'\n" +
                "        >\n" +
                "      ];\n" +
                "      assert: '{\"defects\":[],\"value\":{\"name\":\"oneName 99\"},\"source\":{\"defects\":[],\"value\":{\"oneName\":\"oneName 99\"},\"source\":{\"number\":1,\"record\":\"{\\\"oneName\\\":\\\"oneName 99\\\",\\\"anotherName\\\":\\\"anotherName 17\\\",\\\"oneDate\\\":\\\"2020-04-13\\\",\\\"anotherDate\\\":\\\"2020-02-25\\\",\\\"oneNumber\\\":27,\\\"anotherNumber\\\":28}\"}}}';\n" +
                "    )\n" +
                "  ]\n" +
                ")\n");
    }

    @Test
    public void testJoinOperationInMapping()
    {
        test(
                "###Mapping\n" +
                        "Mapping test::mapping\n" +
                        "(\n" +
                        "  CM1: Relational\n" +
                        "  {\n" +
                        "    name: [TEST_SCOPE]TEST.another.stateProvNameTXT,\n" +
                        "    subdivisionCategory: if(substring([TEST_SCOPE]TEST.another.stateProv, 1, 2) = 'US', 'STATE', sqlNull()),\n" +
                        "    code: [TEST_SCOPE]TEST.something.nodeCODE,\n" +
                        "    scheme: [TEST_SCOPE]@join_1,\n" +
                        "    parent: [TEST_SCOPE]@join_2,\n" +
                        "    hierarchyLevel: case([TEST_SCOPE]TEST.something.test_val = 20, 1, [TEST_SCOPE]TEST.something.test_val = 30, 2, [TEST_SCOPE]TEST.something.test_val = 40, 3, [TEST_SCOPE]TEST.something.test_val = 55, 4, [TEST_SCOPE]TEST.something.test_val = 60, 5, sqlNull()),\n" +
                        "    prop1: divide(if([TEST_DB2]@join_1 > (OUTER) [TEST_DB2]@join_2 | toString([TEST_DB2]SOMETHING.something.toValue) = [TEST_DB2]@join_1 > (OUTER) [TEST_DB2]@join_2 | toString([TEST_DB2]SOMETHING.something.toValue), 0.0, [TEST_DB2]@join_3 > (INNER) [TEST_DB2]@join_4 | parseFloat([TEST_DB2]schema1.table2.col3)), if(isEmpty([TEST_SCOPE]TEST.something.factor), 1.0, divide(1, [TEST_SCOPE]TEST.something.factor))),\n" +
                        "    part[part_TEST]: [TEST_SCOPE]@join_1 > [TEST_SCOPE]@join_2\n" +
                        "  }\n" +
                        ")\n"
        );

    }
}