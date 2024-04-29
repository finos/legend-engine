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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposer;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.protocol.pure.v1.PureProtocolObjectMapperFactory;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.junit.Assert;
import org.junit.Test;

import java.util.Objects;

public class TestRelationalGrammarComposer
{
    private static final ObjectMapper objectMapper = PureProtocolObjectMapperFactory.getNewObjectMapper();

    @Test
    public void selfJoinTest() throws Exception
    {
        PureModelContextData context = objectMapper.readValue(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("selfJoinTest.json")), PureModelContextData.class);
        PureGrammarComposer grammarTransformer = PureGrammarComposer.newInstance(PureGrammarComposerContext.Builder.newInstance().build());
        String formatted = grammarTransformer.renderPureModelContextData(context);

        // Test checks that we do not output schema name before {target} - the grammar round trip tests do not exercise this code path as the graph
        // produced for the roundtrip always sets the schema name in the table alias to default.
        // This is testing out a graph as would be produced by tools such as Studio and checking that syntactically valid Pure is produced for self-joins
        String expected =
                "###Relational\n" +
                        "Database test::db\n" + "(\n" +
                        "  Schema mySchema\n" +
                        "  (\n" +
                        "    Table table1\n" +
                        "    (\n" +
                        "      col1 CHAR(32) PRIMARY KEY,\n" +
                        "      col2 CHAR(32)\n" +
                        "    )\n" +
                        "  )\n" +
                        "\n" +
                        "  Join selfJoin(mySchema.table1.col1 = {target}.col2)\n" +
                        ")\n";

        Assert.assertEquals(expected, formatted);
    }

    @Test
    public void testMappingWithTestSQL() throws Exception
    {
        PureModelContextData context = objectMapper.readValue(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("mappingWithTestsSQL.json")), PureModelContextData.class);
        PureGrammarComposer grammarTransformer = PureGrammarComposer.newInstance(PureGrammarComposerContext.Builder.newInstance().build());
        String formatted = grammarTransformer.renderPureModelContextData(context);

        String expected =
                "###Mapping\n" +
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
                        "        >\n" +
                        "      ];\n" +
                        "      assert: '{\"defects\":[],\"value\":{\"name\":\"oneName 99\"},\"source\":{\"defects\":[],\"value\":{\"oneName\":\"oneName 99\"},\"source\":{\"number\":1,\"record\":\"{\\\"oneName\\\":\\\"oneName 99\\\",\\\"anotherName\\\":\\\"anotherName 17\\\",\\\"oneDate\\\":\\\"2020-04-13\\\",\\\"anotherDate\\\":\\\"2020-02-25\\\",\\\"oneNumber\\\":27,\\\"anotherNumber\\\":28}\"}}}';\n" +
                        "    )\n" +
                        "  ]\n" +
                        ")\n";

        Assert.assertEquals(expected, formatted);
    }

    @Test
    public void testMappingWithTestCSV() throws Exception
    {
        PureModelContextData context = objectMapper.readValue(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("mappingWithTestsCSV.json")), PureModelContextData.class);
        PureGrammarComposer grammarTransformer = PureGrammarComposer.newInstance(PureGrammarComposerContext.Builder.newInstance().build());
        String formatted = grammarTransformer.renderPureModelContextData(context);

        String expected =
                "###Mapping\n" +
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
                        ")\n";

        Assert.assertEquals(expected, formatted);
    }

    @Test
    public void TestMappingWithLeftOuterJoin() throws Exception
    {
        PureModelContextData context = objectMapper.readValue(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("mappingWithLeftOuterJoin.json")), PureModelContextData.class);
        PureGrammarComposer grammarTransformer = PureGrammarComposer.newInstance(PureGrammarComposerContext.Builder.newInstance().build());
        String formatted = grammarTransformer.renderPureModelContextData(context);
        String expected =
                "###Mapping\n" +
                        "Mapping testDB::test\n" +
                        "(\n" +
                        "  *meta::relational::tests::model::simple::Person[meta_relational_tests_model_simple_Person]: Relational\n" +
                        "  {\n" +
                        "    ~primaryKey\n" +
                        "    (\n" +
                        "      [meta::relational::tests::mapping::join::model::store::db]personTable.ID\n" +
                        "    )\n" +
                        "    ~mainTable [meta::relational::tests::mapping::join::model::store::db]personTable\n" +
                        "    firstName: [meta::relational::tests::mapping::join::model::store::db]personTable.FIRSTNAME,\n" +
                        "    lastName: [meta::relational::tests::mapping::join::model::store::db]personTable.LASTNAME,\n " +
                        "   age: [meta::relational::tests::mapping::join::model::store::db]@Person_MiddleTable > (OUTER) [meta::relational::tests::mapping::join::model::store::db]@MiddleTable_PersonExtension | [meta::relational::tests::mapping::join::model::store::db]personExtensionTable.AGE\n" +
                        "  }\n" +
                        ")\n";

        Assert.assertEquals(expected, formatted);
    }
}
