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
import org.finos.legend.engine.language.pure.grammar.from.antlr4.RelationalParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.mapping.MappingParserGrammar;
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
                "                  <Relational, SQL, z::db, 'Drop table if exists PersonTable;Create Table PersonTable(id INT, firmId INT, lastName VARCHAR(200));Insert into PersonTable (id, firmId, lastName) values (1, 1, \\'Doe\\');Insert into PersonTable (id, firmId, lastName) values (2, 1, \\'Doe2\\');'>" +
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
                "                  <Relational, RANDOM, a::S, 'Drop table if exists PersonTable;Create Table PersonTable(id INT, firmId INT, lastName VARCHAR(200));Insert into PersonTable (id, firmId, lastName) values (1, 1, \\'Doe\\');Insert into PersonTable (id, firmId, lastName) values (2, 1, \\'Doe2\\');'>" +
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
                "                  <Relational, CSV, z::db, '{\"oneName\":\"oneName 2\",\"anotherName\":\"anotherName 16\",\"oneDate\":\"2020-02-05\",\"anotherDate\":\"2020-04-13\",\"oneNumber\":24,\"anotherNumber\":29}'>" +
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
                "                  <Relational, RANDOM, z::DB, '{\"oneName\":\"oneName 2\",\"anotherName\":\"anotherName 16\",\"oneDate\":\"2020-02-05\",\"anotherDate\":\"2020-04-13\",\"oneNumber\":24,\"anotherNumber\":29}'>" +
                "               ];\n" +
                "         assert: '{\"defects\":[],\"value\":{\"name\":\"oneName 99\"},\"source\":{\"defects\":[],\"value\":{\"oneName\":\"oneName 99\"},\"source\":{\"number\":1,\"record\":\"{\\\"oneName\\\":\\\"oneName 99\\\",\\\"anotherName\\\":\\\"anotherName 17\\\",\\\"oneDate\\\":\\\"2020-04-13\\\",\\\"anotherDate\\\":\\\"2020-02-25\\\",\\\"oneNumber\\\":27,\\\"anotherNumber\\\":28}\"}}}';\n" +
                "      )\n" +
                "   ]" +
                "\n" +
                ")\n", "PARSER error at [9:48-53]: Mapping test relational input data does not support format 'RANDOM'. Possible values: SQL, CSV");
    }

}
