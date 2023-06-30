// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.language.pure.dsl.persistence.relational.grammar.test;

import org.antlr.v4.runtime.Vocabulary;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.finos.legend.engine.language.pure.grammar.test.TestGrammarParser;
import org.finos.legend.pure.grammar.from.antlr4.PersistenceRelationalParserGrammar;

import java.util.List;

public class TestPersistenceRelationalGrammarParser extends TestGrammarParser.TestGrammarParserTestSuite
{
    @Override
    public Vocabulary getParserGrammarVocabulary()
    {
        return PersistenceRelationalParserGrammar.VOCABULARY;
    }

    @Override
    public String getParserGrammarIdentifierInclusionTestCode(List<String> keywords)
    {
        return "###Persistence\n" +
                "\n" +
                "Persistence " + ListAdapter.adapt(keywords).makeString("::") + "\n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
                "  serviceOutputTargets:\n" +
                "  [\n" +
                "    TDS\n" +
                "    {\n" +
                "      keys:\n" +
                "      [\n" +
                "        foo, bar\n" +
                "      ]\n" +
                "      deduplication: AnyVersion;\n" +
                "      datasetType: Delta\n" +
                "      {\n" +
                "        actionIndicator: DeleteIndicator\n" +
                "        {\n" +
                "          deleteField: isDeleted;\n" +
                "          deleteValues: ['Y', '1', 'true'];\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "    ->\n" +
                "    Relational\n" +
                "    #{\n" +
                "      table: TableA;\n" +
                "      database: test::Database;\n" +
                "      temporality: Bitemporal\n" +
                "      {\n" +
                "        processingDimension: BatchIdAndDateTime\n" +
                "        {\n" +
                "          batchIdIn: x;\n" +
                "          batchIdOut: y;\n" +
                "          dateTimeIn: y;\n" +
                "          dateTimeOut: y;\n" +
                "        }\n" +
                "        sourceDerivedDimension: DateTime\n" +
                "        {\n" +
                "          dateTimeStart: x;\n" +
                "          dateTimeEnd: y;\n" +
                "          sourceFields: StartAndEnd\n" +
                "          {\n" +
                "            startField: timeIn;\n" +
                "            endField: timeOut;\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }#,\n" +
                "    #/test::example::MyType/prop#\n" +
                "    {\n" +
                "      keys:\n" +
                "      [\n" +
                "        #/test::example::MyType/prop/foo#, #/test::example::MyType/prop/bar#\n" +
                "      ]\n" +
                "      deduplication: MaxVersion\n" +
                "      {\n" +
                "        versionField: #/test::example::MyType/prop/version#;\n" +
                "      }\n" +
                "      datasetType: Snapshot\n" +
                "      {\n" +
                "        partitioning: None\n" +
                "        {\n" +
                "          emptyDatasetHandling: NoOp;\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "    ->\n" +
                "    Relational\n" +
                "    #{\n" +
                "      table: TableB;\n" +
                "      database: test::Database;\n" +
                "      temporality: None\n" +
                "      {\n" +
                "        updatesHandling: Overwrite;\n" +
                "        auditing: None;\n" +
                "      }\n" +
                "    }#\n" +
                "  ];\n" +
                "  tests:\n" +
                "  [\n" +
                "    test1:\n" +
                "    {\n" +
                "      testBatches:\n" +
                "      [\n" +
                "        testBatch1:\n" +
                "        {\n" +
                "         data:\n" +
                "         {\n" +
                "           connection:\n" +
                "           {\n" +
                "              ExternalFormat\n" +
                "              #{\n" +
                "                contentType: 'application/x.flatdata';\n" +
                "                data: 'FIRST_NAME,LAST_NAME\\nFred,Bloggs\\nJane,Doe';\n" +
                "              }#\n" +
                "           }\n" +
                "         }\n" +
                "         asserts:\n" +
                "         [\n" +
                "           assert1:\n" +
                "             EqualToJson\n" +
                "             #{\n" +
                "               expected: \n" +
                "                 ExternalFormat\n" +
                "                 #{\n" +
                "                   contentType: 'application/json';\n" +
                "                   data: '{\"Age\":12, \"Name\":\"dummy\"}';\n" +
                "                 }#;\n" +
                "             }#\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "      isTestDataFromServiceOutput: false;\n" +
                "    }\n" +
                "  ]\n" +
                "}\n";
    }
}
