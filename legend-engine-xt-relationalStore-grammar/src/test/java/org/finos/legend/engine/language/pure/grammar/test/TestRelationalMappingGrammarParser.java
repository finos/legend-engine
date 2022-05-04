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
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.RelationalParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.mapping.MappingParserGrammar;
import org.finos.legend.engine.protocol.pure.v1.PureProtocolObjectMapperFactory;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class TestRelationalMappingGrammarParser extends TestGrammarParser.TestGrammarParserTestSuite
{
    @Override
    public Vocabulary getParserGrammarVocabulary()
    {
        return MappingParserGrammar.VOCABULARY;
    }

    @Override
    public List<Vocabulary> getDelegatedParserGrammarVocabulary()
    {
        return FastList.newListWith(
                RelationalParserGrammar.VOCABULARY
        );
    }

    @Override
    public String getParserGrammarIdentifierInclusionTestCode(List<String> keywords)
    {
        return "###Mapping\n" +
                "Mapping " + ListAdapter.adapt(keywords).makeString("::") + "\n" +
                "(\n" +
                "  meta::something::tests::csv::model::Person: Relational\n" +
                "  {\n" +
                "    scope([meta::something::tests::csv::store::" + ListAdapter.adapt(keywords).makeString("::") + "]TEST.another)\n" +
                "    (\n" +
                "      name: stateProvNameTXT,\n" +
                "      subdivisionCategory: if(equal(substring(stateProv, 1, 2), 'US'), 'STATE', sqlNull())\n" +
                "    )\n" +
                "  }" +
                "\n" +
                ")\n";
    }

    @Test
    public void testFaultyRelationalMapping()
    {
        // Unknown JOIN type
        test("###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "   TestScopeCM : Relational\n" +
                "   {\n" +
                "      feeAmount : divide(if(equal([testSTORE]@test_JOIN > (UNKNOWN_JOIN_TYPE) [testSTORE]@TER_JOIN | toString([testSTORE]SOMETHING.something.toValue), '09001'), 0.0,\n" +
                "                         [testSTORE]@test_JOIN > (INNER) [testSTORE]@Sample | parseFloat([testSTORE]CAR.PAINT.FeeAmount)), if(isEmpty(factor), 1.0, divide(1, factor)))\n" +
                "   }\n" +
                ")", "PARSER error at [6:60-76]: Unsupported join type 'UNKNOWN_JOIN_TYPE'");
        // bad table alias
        test("###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "   TestScopeCM : Relational\n" +
                "   {\n" +
                "      feeAmount : divide(if(equal([testSTORE]@test_JOIN > (OUTER) [testSTORE]@TER_JOIN | toString([testSTORE]SOMETHING.something.toValue), '09001'), 0.0,\n" +
                "                         [testSTORE]@test_JOIN > (INNER) [testSTORE]@Sample | parseFloat([testSTORE]CAR.PAINT.FeeAmount)), if(isEmpty(factor), 1.0, divide(1, factor)))\n" +
                "   }\n" +
                ")", "PARSER error at [7:135-140]: Missing table or alias for column 'factor'");
    }

    @Test
    public void testFaultMappingWithLowerCaseInner()
    {
        test("###Mapping\n" +
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
                "    prop1: divide(if([TEST_DB2]@join_1 > (inner) [TEST_DB2]@join_2 | toString([TEST_DB2]SOMETHING.something.toValue) = [TEST_DB2]@join_1 > (OUTER) [TEST_DB2]@join_2 | toString([TEST_DB2]SOMETHING.something.toValue), 0.0, [TEST_DB2]@join_3 > (INNER) [TEST_DB2]@join_4 | parseFloat([TEST_DB2]schema1.table2.col3)), if(isEmpty([TEST_SCOPE]TEST.something.factor), 1.0, divide(1, [TEST_SCOPE]TEST.something.factor))),\n" +
                "    part[part_TEST]: [TEST_SCOPE]@join_1 > [TEST_SCOPE]@join_2\n" +
                "  }\n" +
                ")\n","PARSER error at [12:43-47]: Unsupported join type 'inner'");

    }

    @Test
    public void testMappingInheritance()
    {
        test("###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "   test::Class[id1] extends : Relational\n" +
                "   {\n" +
                "      prop: 1\n" +
                "   }\n" +
                ")", "PARSER error at [4:29]: Unexpected token");

        test("###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "   test::Class[id1] extends []: Relational\n" +
                "   {\n" +
                "      prop: 1\n" +
                "   }\n" +
                ")", "PARSER error at [4:30]: Unexpected token");

        test("###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "   test::Class[id1] extends [id2]: Relational\n" +
                "   {\n" +
                "      prop: 1\n" +
                "   }\n" +
                ")");
    }

    @Test
    public void testMappingTestDataSQL()
    {
        test("###Mapping\n" +
                "Mapping meta::pure::mapping::modelToModel::test::simple::simpleModelMapping\n" +
                "(\n" +
                "   MappingTests\n" +
                "   [\n" +
                "      test2\n" +
                "      (\n" +
                "         query: |model::domain::Target.all()->graphFetchChecked(#{model::domain::Target{name}}#)->serialize(#{model::domain::Target{name}}#);\n" +
                "         data: [" +
                "                  <Relational, SQL, z::db, 'Drop table if exists PersonTable;\\nCreate Table PersonTable(id INT, firmId INT, lastName VARCHAR(200));\\nInsert into PersonTable (id, firmId, lastName) values (1, 1, \\'Doe\\;\\');\\nInsert into PersonTable (id, firmId, lastName) values (2, 1, \\'Doe2\\');'>" +
                "               ];\n" +
                "         assert: '{\"defects\":[],\"value\":{\"name\":\"oneName 99\"},\"source\":{\"defects\":[],\"value\":{\"oneName\":\"oneName 99\"},\"source\":{\"number\":1,\"record\":\"{\\\"oneName\\\":\\\"oneName 99\\\",\\\"anotherName\\\":\\\"anotherName 17\\\",\\\"oneDate\\\":\\\"2020-04-13\\\",\\\"anotherDate\\\":\\\"2020-02-25\\\",\\\"oneNumber\\\":27,\\\"anotherNumber\\\":28}\"}}}';\n" +
                "      )\n" +
                "   ]" +
                "\n" +
                ")\n");

        test("###Mapping\n" +
                "Mapping meta::pure::mapping::modelToModel::test::simple::simpleModelMapping\n" +
                "(\n" +
                "   MappingTests\n" +
                "   [\n" +
                "      test2\n" +
                "      (\n" +
                "         query: |model::domain::Target.all()->graphFetchChecked(#{model::domain::Target{name}}#)->serialize(#{model::domain::Target{name}}#);\n" +
                "         data: [" +
                "                  <Relational, RANDOM, a::S, 'Drop table if exists PersonTable;\\nCreate Table PersonTable(id INT, firmId INT, lastName VARCHAR(200));\\nInsert into PersonTable (id, firmId, lastName) values (1, 1, \\'Doe\\;\\');\\nInsert into PersonTable (id, firmId, lastName) values (2, 1, \\'Doe2\\');'>" +
                "               ];\n" +
                "         assert: '{\"defects\":[],\"value\":{\"name\":\"oneName 99\"},\"source\":{\"defects\":[],\"value\":{\"oneName\":\"oneName 99\"},\"source\":{\"number\":1,\"record\":\"{\\\"oneName\\\":\\\"oneName 99\\\",\\\"anotherName\\\":\\\"anotherName 17\\\",\\\"oneDate\\\":\\\"2020-04-13\\\",\\\"anotherDate\\\":\\\"2020-02-25\\\",\\\"oneNumber\\\":27,\\\"anotherNumber\\\":28}\"}}}';\n" +
                "      )\n" +
                "   ]" +
                "\n" +
                ")\n", "PARSER error at [9:48-53]: Mapping test relational input data does not support format 'RANDOM'. Possible values: SQL, CSV");
    }

    @Test
    public void testMappingTestDataCSV()
    {
        test("###Mapping\n" +
                "Mapping meta::pure::mapping::modelToModel::test::simple::simpleModelMapping\n" +
                "(\n" +
                "   MappingTests\n" +
                "   [\n" +
                "      test2\n" +
                "      (\n" +
                "         query: |model::domain::Target.all()->graphFetchChecked(#{model::domain::Target{name}}#)->serialize(#{model::domain::Target{name}}#);\n" +
                "         data: [" +
                "                  <Relational, CSV, z::db, 'default\\nPersonTable\\nid,lastName\\n1,Doe;\\n2,Doe2\\n\\n\\n\\n'>" +
                "               ];\n" +
                "         assert: '{\"defects\":[],\"value\":{\"name\":\"oneName 99\"},\"source\":{\"defects\":[],\"value\":{\"oneName\":\"oneName 99\"},\"source\":{\"number\":1,\"record\":\"{\\\"oneName\\\":\\\"oneName 99\\\",\\\"anotherName\\\":\\\"anotherName 17\\\",\\\"oneDate\\\":\\\"2020-04-13\\\",\\\"anotherDate\\\":\\\"2020-02-25\\\",\\\"oneNumber\\\":27,\\\"anotherNumber\\\":28}\"}}}';\n" +
                "      )\n" +
                "   ]" +
                "\n" +
                ")\n");

        test("###Mapping\n" +
                "Mapping meta::pure::mapping::modelToModel::test::simple::simpleModelMapping\n" +
                "(\n" +
                "   MappingTests\n" +
                "   [\n" +
                "      test2\n" +
                "      (\n" +
                "         query: |model::domain::Target.all()->graphFetchChecked(#{model::domain::Target{name}}#)->serialize(#{model::domain::Target{name}}#);\n" +
                "         data: [" +
                "                  <Relational, RANDOM, z::DB, 'default\\nPersonTable\\nid,lastName\\n1,Doe;\\n2,Doe2\\n\\n\\n\\n'>" +
                "               ];\n" +
                "         assert: '{\"defects\":[],\"value\":{\"name\":\"oneName 99\"},\"source\":{\"defects\":[],\"value\":{\"oneName\":\"oneName 99\"},\"source\":{\"number\":1,\"record\":\"{\\\"oneName\\\":\\\"oneName 99\\\",\\\"anotherName\\\":\\\"anotherName 17\\\",\\\"oneDate\\\":\\\"2020-04-13\\\",\\\"anotherDate\\\":\\\"2020-02-25\\\",\\\"oneNumber\\\":27,\\\"anotherNumber\\\":28}\"}}}';\n" +
                "      )\n" +
                "   ]" +
                "\n" +
                ")\n", "PARSER error at [9:48-53]: Mapping test relational input data does not support format 'RANDOM'. Possible values: SQL, CSV");
    }

    @Test
    public void testClassMappingFilterWithInnerJoin()
    {
        String model = "import other::*;\n" +
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
                "   View personFirmView\n"+
                "   (\n" +
                "    id : personTable.id,\n" +
                "    firstName : personTable.firstName,\n" +
                "    firmId : personTable.firmId\n" +
                "   )\n" +
                "   Filter FirmFilter(firmTable.legalName = 'A')\n" +
                "   Join Firm_Person(firmTable.id = personTable.firmId)\n" +
                ")\n";
        test(model + "###Mapping\n" +
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

        test(model + "###Mapping\n" +
                "import other::*;\n" +
                "import mapping::*;\n" +
                "Mapping mappingPackage::myMapping\n" +
                "(\n" +
                "    Person: Relational\n" +
                "    {\n" +
                "        ~filter [mapping::db] (inner) @Firm_Person | [mapping::db] FirmFilter \n" +
                "        firstName : [db]personTable.firstName\n" +
                "    }\n" +
                ")\n", "PARSER error at [43:32-36]: Unsupported join type 'inner'. The supported join types are: [INNER, OUTER]");
    }

    @Test
    public void testLocalMappingPropertyParsing() throws Exception
    {
        String val = "###Mapping\n" +
                    "Mapping mappingPackage::myMapping\n" +
                    "(\n" +
                    "    Person: Relational\n" +
                    "    {\n" +
                    "        firstName : [db]personTable.firstName,\n" +
                    "        +localProp : String[1] : [db]personTable.firstName\n" +
                    "    }\n" +
                    ")\n";

        PureModelContextData data = PureGrammarParser.newInstance().parseModel(val);
        String expected = "{\"_type\":\"data\",\"serializer\":null,\"origin\":null,\"elements\":[{\"_type\":\"mapping\",\"name\":\"myMapping\",\"sourceInformation\":{\"sourceId\":\"\",\"startLine\":2,\"startColumn\":1,\"endLine\":9,\"endColumn\":1},\"classMappings\":[{\"_type\":\"relational\",\"id\":null,\"mappingClass\":null,\"extendsClassMappingId\":null,\"root\":false,\"sourceInformation\":{\"sourceId\":\"\",\"startLine\":4,\"startColumn\":5,\"endLine\":8,\"endColumn\":5},\"classSourceInformation\":{\"sourceId\":\"\",\"startLine\":4,\"startColumn\":5,\"endLine\":4,\"endColumn\":10},\"primaryKey\":[],\"propertyMappings\":[{\"_type\":\"relationalPropertyMapping\",\"property\":{\"property\":\"firstName\",\"sourceInformation\":{\"sourceId\":\"mappingPackage::myMapping\",\"startLine\":6,\"startColumn\":9,\"endLine\":6,\"endColumn\":17},\"class\":\"Person\"},\"source\":null,\"target\":null,\"localMappingProperty\":null,\"sourceInformation\":{\"sourceId\":\"mappingPackage::myMapping\",\"startLine\":6,\"startColumn\":19,\"endLine\":6,\"endColumn\":45},\"enumMappingId\":null,\"relationalOperation\":{\"_type\":\"column\",\"sourceInformation\":{\"sourceId\":\"mappingPackage::myMapping\",\"startLine\":6,\"startColumn\":21,\"endLine\":6,\"endColumn\":45},\"table\":{\"_type\":\"Table\",\"table\":\"personTable\",\"schema\":\"default\",\"database\":\"db\",\"mainTableDb\":\"db\",\"sourceInformation\":{\"sourceId\":\"mappingPackage::myMapping\",\"startLine\":6,\"startColumn\":25,\"endLine\":6,\"endColumn\":35}},\"tableAlias\":\"personTable\",\"column\":\"firstName\"},\"bindingTransformer\":null},{\"_type\":\"relationalPropertyMapping\",\"property\":{\"property\":\"localProp\",\"sourceInformation\":{\"sourceId\":\"mappingPackage::myMapping\",\"startLine\":7,\"startColumn\":10,\"endLine\":7,\"endColumn\":18},\"class\":null},\"source\":null,\"target\":null,\"localMappingProperty\":{\"type\":\"String\",\"multiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"sourceInformation\":{\"sourceId\":\"mappingPackage::myMapping\",\"startLine\":7,\"startColumn\":20,\"endLine\":7,\"endColumn\":30}},\"sourceInformation\":{\"sourceId\":\"mappingPackage::myMapping\",\"startLine\":7,\"startColumn\":32,\"endLine\":7,\"endColumn\":58},\"enumMappingId\":null,\"relationalOperation\":{\"_type\":\"column\",\"sourceInformation\":{\"sourceId\":\"mappingPackage::myMapping\",\"startLine\":7,\"startColumn\":34,\"endLine\":7,\"endColumn\":58},\"table\":{\"_type\":\"Table\",\"table\":\"personTable\",\"schema\":\"default\",\"database\":\"db\",\"mainTableDb\":\"db\",\"sourceInformation\":{\"sourceId\":\"mappingPackage::myMapping\",\"startLine\":7,\"startColumn\":38,\"endLine\":7,\"endColumn\":48}},\"tableAlias\":\"personTable\",\"column\":\"firstName\"},\"bindingTransformer\":null}],\"mainTable\":null,\"distinct\":false,\"groupBy\":[],\"filter\":null,\"class\":\"Person\"}],\"includedMappings\":[],\"associationMappings\":[],\"enumerationMappings\":[],\"tests\":[],\"package\":\"mappingPackage\"},{\"_type\":\"sectionIndex\",\"name\":\"SectionIndex\",\"sourceInformation\":null,\"sections\":[{\"_type\":\"importAware\",\"parserName\":\"Pure\",\"elements\":[],\"sourceInformation\":{\"sourceId\":\"\",\"startLine\":1,\"startColumn\":1,\"endLine\":1,\"endColumn\":8},\"imports\":[]},{\"_type\":\"importAware\",\"parserName\":\"Mapping\",\"elements\":[\"mappingPackage::myMapping\"],\"sourceInformation\":{\"sourceId\":\"\",\"startLine\":2,\"startColumn\":8,\"endLine\":11,\"endColumn\":2},\"imports\":[]}],\"package\":\"__internal__\"}]}";

        Assert.assertEquals(expected, PureProtocolObjectMapperFactory.getNewObjectMapper().writeValueAsString(data));
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
                "    FIRM SEMISTRUCTURED(10)\n" +
                "  )\n" +
                ")\n", "PARSER error at [7:10-27]: Column data type SEMISTRUCTURED does not expect any parameters in declaration");

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
                "    Person: Relational\n" +
                "    {\n" +
                "        firstName : [db]personTable.firstName,\n" +
                "        firm : Binding : [db]personTable.jsonColumn\n" +
                "    }\n" +
                ")\n", "PARSER error at [7:24]: Unexpected token");

        test("###Mapping\n" +
                "Mapping mappingPackage::myMapping\n" +
                "(\n" +
                "    Person: Relational\n" +
                "    {\n" +
                "        firstName : [db]personTable.firstName,\n" +
                "        firm : test::binding : [db]personTable.jsonColumn\n" +
                "    }\n" +
                ")\n", "PARSER error at [7:20-21]: Unexpected token");

        test("###Mapping\n" +
                "Mapping mappingPackage::myMapping\n" +
                "(\n" +
                "    Person: Relational\n" +
                "    {\n" +
                "        firstName : [db]personTable.firstName,\n" +
                "        firm : Binding test::binding : [db]personTable.jsonColumn\n" +
                "    }\n" +
                ")\n");

        test("###Mapping\n" +
                "Mapping mappingPackage::myMapping\n" +
                "(\n" +
                "    Person: Relational\n" +
                "    {\n" +
                "        firstName : [db]personTable.firstName,\n" +
                "        firm : Binding test::binding: [db]personTable.jsonColumn\n" +
                "    }\n" +
                ")\n");

        test("###Mapping\n" +
                "Mapping mappingPackage::myMapping\n" +
                "(\n" +
                "    Person: Relational\n" +
                "    {\n" +
                "        firstName : [db]personTable.firstName,\n" +
                "        firm : Binding binding: [db]personTable.jsonColumn\n" +
                "    }\n" +
                ")\n");
    }
}
