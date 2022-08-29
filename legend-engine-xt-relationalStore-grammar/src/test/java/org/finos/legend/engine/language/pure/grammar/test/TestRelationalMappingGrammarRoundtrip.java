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

package org.finos.legend.engine.language.pure.grammar.test;

import org.junit.Test;

public class TestRelationalMappingGrammarRoundtrip extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{
    @Test
    public void testMappingInheritance()
    {
        // Without extends
        test("###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "  test::Class[id1]: Relational\n" +
                "  {\n" +
                "    prop: 1\n" +
                "  }\n" +
                ")\n");

        // With extends
        test("###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "  test::Class[id1] extends [id2]: Relational\n" +
                "  {\n" +
                "    prop: 1\n" +
                "  }\n" +
                ")\n");
    }

    @Test
    public void testRelationalMappingTest()
    {
        // Relational SQL mapping test
        testFormat("###Mapping\n" +
                "Mapping model::simpleMapping\n" +
                "(\n" +
                "  MappingTests\n" +
                "  [\n" +
                "    test2\n" +
                "    (\n" +
                "      query: |model::domain::Target.all()->graphFetchChecked(#{model::domain::Target{name}}#)->serialize(#{model::domain::Target{name}}#);\n" +
                "      data:\n" +
                "      [\n" +
                "        <Relational, SQL, z::db, \n" +
                "          'Drop table if exists PersonTable;\\n'+\n" +
                "          'Create Table PersonTable(id INT, firmId INT, lastName VARCHAR(200));\\n'+\n" +
                "          'Insert into PersonTable (id, firmId, lastName) values (1, 1, \\'Doe\\;\\');\\n'+\n" +
                "          'Insert into PersonTable (id, firmId, lastName) values (2, 1, \\'Doe2\\');\\n'\n" +
                "        >\n" +
                "      ];\n" +
                "      assert: '{\"defects\":[],\"value\":{\"name\":\"oneName 99\"},\"source\":{\"defects\":[],\"value\":{\"oneName\":\"oneName 99\"},\"source\":{\"number\":1,\"record\":\"{\\\"oneName\\\":\\\"oneName 99\\\",\\\"anotherName\\\":\\\"anotherName 17\\\",\\\"oneDate\\\":\\\"2020-04-13\\\",\\\"anotherDate\\\":\\\"2020-02-25\\\",\\\"oneNumber\\\":27,\\\"anotherNumber\\\":28}\"}}}';\n" +
                "    )\n" +
                "  ]\n" +
                ")\n", "###Mapping\n" +
                "Mapping model::simpleMapping\n" +
                "(\n" +
                "   MappingTests\n" +
                "   [\n" +
                "      test2\n" +
                "      (\n" +
                "         query: |model::domain::Target.all()->graphFetchChecked(#{model::domain::Target{name}}#)->serialize(#{model::domain::Target{name}}#);\n" +
                "         data: [" +
                "                  <Relational, SQL, z::db, 'Drop table if exists PersonTable;\\r\\nCreate Table PersonTable(\\rid INT, firmId INT, lastName VARCHAR(200));\\r\\nInsert into PersonTable (id, firmId, lastName) values (1, 1, \\'Doe\\;\\');\\nInsert into PersonTable (id, firmId, lastName) values (2, 1, \\'Doe2\\');'>" +
                "               ];\n" +
                "         assert: '{\"defects\":[],\"value\":{\"name\":\"oneName 99\"},\"source\":{\"defects\":[],\"value\":{\"oneName\":\"oneName 99\"},\"source\":{\"number\":1,\"record\":\"{\\\"oneName\\\":\\\"oneName 99\\\",\\\"anotherName\\\":\\\"anotherName 17\\\",\\\"oneDate\\\":\\\"2020-04-13\\\",\\\"anotherDate\\\":\\\"2020-02-25\\\",\\\"oneNumber\\\":27,\\\"anotherNumber\\\":28}\"}}}';\n" +
                "      )\n" +
                "   ]" +
                "\n" +
                ")\n");
    }

    @Test
    public void testRelationalMappingTestSuites()
    {
        test("###Mapping\n" +
                "Mapping execution::RelationalMapping\n" +
                "(\n" +
                "  *model::Person: Relational\n" +
                "  {\n" +
                "    ~primaryKey\n" +
                "    (\n" +
                "      [store::TestDB]PersonTable.id\n" +
                "    )\n" +
                "    ~mainTable [store::TestDB]PersonTable\n" +
                "    firstName: [store::TestDB]PersonTable.firstName,\n" +
                "    lastName: [store::TestDB]PersonTable.lastName\n" +
                "  }\n" +
                "  *model::Firm: Relational\n" +
                "  {\n" +
                "    ~primaryKey\n" +
                "    (\n" +
                "      [store::TestDB]FirmTable.id\n" +
                "    )\n" +
                "    ~mainTable [store::TestDB]FirmTable\n" +
                "    legalName: [store::TestDB]FirmTable.legal_name,\n" +
                "    employees[model_Person]: [store::TestDB]@FirmPerson\n" +
                "  }\n" +
                "\n" +
                "  testSuites:\n" +
                "  [\n" +
                "    testSuite1:\n" +
                "    {\n" +
                "      data:\n" +
                "      [\n" +
                "        store::TestDB:\n" +
                "          Reference\n" +
                "          #{\n" +
                "            data::RelationalData\n" +
                "          }#\n" +
                "      ];\n" +
                "      tests:\n" +
                "      [\n" +
                "        test1:\n" +
                "        {\n" +
                "          query: |model::Firm.all()->project([x|$x.employees.firstName, x|$x.employees.lastName, x|$x.legalName], ['Employees/First Name', 'Employees/Last Name', 'Legal Name']);\n" +
                "          asserts:\n" +
                "          [\n" +
                "            shouldPass:\n" +
                "              EqualToJson\n" +
                "              #{\n" +
                "                expected : \n" +
                "                  ExternalFormat\n" +
                "                  #{\n" +
                "                    contentType: 'application/json';\n" +
                "                    data: '{\"columns\":[{\"name\":\"Employees/First Name\",\"type\":\"String\"},{\"name\":\"Employees/Last Name\",\"type\":\"String\"},{\"name\":\"Legal Name\",\"type\":\"String\"}],\"rows\":[{\"values\":[\"John\",\"Doe\",\"Finos\"]},{\"values\":[\"Nicole\",\"Smith\",\"Finos\"]},{\"values\":[\"Time\",\"Smith\",\"Apple\"]}]}';\n" +
                "                  }#;\n" +
                "              }#\n" +
                "          ];\n" +
                "        }\n" +
                "      ];\n" +
                "    }\n" +
                "  ]\n" +
                ")\n");
    }

    @Test
    public void testClassMappingFilterWithInnerJoin()
    {
        test("###Mapping\n" +
                "Mapping mappingPackage::myMapping\n" +
                "(\n" +
                "  Person: Relational\n" +
                "  {\n" +
                "    ~filter [mapping::db] (INNER) @Firm_Person | [mapping::db]FirmFilter\n" +
                "    firstName: [db]personTable.firstName\n" +
                "  }\n" +
                ")\n");
    }

    @Test
    public void testSemiStructuredColumn()
    {
        test("###Relational\n" +
                "Database simple::DB\n" +
                "(\n" +
                "  Table personTable\n" +
                "  (\n" +
                "    FIRSTNAME VARCHAR(10),\n" +
                "    FIRM SEMISTRUCTURED\n" +
                "  )\n" +
                ")\n");
    }

    @Test
    public void testRelationalPropertyMappingWithBindingTransformer()
    {
        test("###Mapping\n" +
                "Mapping mappingPackage::myMapping\n" +
                "(\n" +
                "  Person: Relational\n" +
                "  {\n" +
                "    firstName: [db]personTable.firstName,\n" +
                "    firm: Binding test::binding : [db]personTable.jsonColumn\n" +
                "  }\n" +
                ")\n");
    }
}
