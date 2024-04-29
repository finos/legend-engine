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

import org.antlr.v4.runtime.Vocabulary;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.ServiceParserGrammar;
import org.junit.Test;

import java.util.List;

public class TestServiceGrammarParser extends TestGrammarParser.TestGrammarParserTestSuite
{
    @Override
    public Vocabulary getParserGrammarVocabulary()
    {
        return ServiceParserGrammar.VOCABULARY;
    }

    @Override
    public String getParserGrammarIdentifierInclusionTestCode(List<String> keywords)
    {
        return "###Service\n" +
                "Service " + ListAdapter.adapt(keywords).makeString("::") + "\n" +
                "{\n" +
                "  pattern : 'test';\n" +
                "  owners : ['test'];\n" +
                "  documentation : 'test';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: src: meta::transform::tests::Address[1]|$src.a;\n" +
                "    mapping: test;\n" +
                "    runtime: test;\n" +
                "  }\n" +
                "  test: Single\n" +
                "  {\n" +
                "    data: 'test';\n" +
                "    asserts:\n" +
                "    [\n" +
                "      { [], res: Result<Any|*>[1]|$res.values->cast(@TabularDataSet).rows->size() == 1 },\n" +
                "      { [], res: Result<Any|*>[1]|$res.values->cast(@TabularDataSet).rows->size() == 1 }\n" +
                "    ];\n" +
                "  }\n" +
                "}\n";
    }

    @Test
    public void testService()
    {
        // Missing fields
        test("###Service\n" +
                "Service meta::pure::myServiceSingle\n" +
                "{\n" +
                "  documentation: 'this is just for context';\n" +
                "  owners: ['ownerName', 'ownerName2'];\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: src: meta::transform::tests::Address[1]|$src.a;\n" +
                "    mapping: meta::myMapping;\n" +
                "    runtime: meta::myRuntime;\n" +
                "  }\n" +
                "  test: Single\n" +
                "  {\n" +
                "    data: 'data';\n" +
                "  }\n" +
                "}\n", "PARSER error at [2:1-17:1]: Field 'pattern' is required");
        test("###Service\n" +
                "Service meta::pure::myServiceSingle\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  owners: ['ownerName', 'ownerName2'];\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: src: meta::transform::tests::Address[1]|$src.a;\n" +
                "    mapping: meta::myMapping;\n" +
                "    runtime: meta::myRuntime;\n" +
                "  }\n" +
                "  test: Single\n" +
                "  {\n" +
                "    data: 'data';\n" +
                "  }\n" +
                "}\n", "PARSER error at [2:1-17:1]: Field 'documentation' is required");
        test("###Service\n" +
                "Service meta::pure::myServiceSingle\n" +
                "{\n" +
                "  documentation: 'this is just for context';\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  test: Single\n" +
                "  {\n" +
                "    data: 'data';\n" +
                "  }\n" +
                "}\n", "PARSER error at [2:1-10:1]: Field 'execution' is required");
        // Duplicated fields
        test("###Service\n" +
                "Service meta::pure::myServiceSingle\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  documentation: 'this is just for context';\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: src: meta::transform::tests::Address[1]|$src.a;\n" +
                "    mapping: meta::myMapping;\n" +
                "    runtime: meta::myRuntime;\n" +
                "  }\n" +
                "  test: Single\n" +
                "  {\n" +
                "    data: 'moreThanData';\n" +
                "  }\n" +
                "}\n", "PARSER error at [2:1-17:1]: Field 'pattern' should be specified only once");
        test("###Service\n" +
                "Service meta::pure::myServiceSingle\n" +
                "{\n" +
                "  documentation: 'this is just for context';\n" +
                "  documentation: 'this is just for context';\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  owners: ['ownerName', 'ownerName2'];\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: src: meta::transform::tests::Address[1]|$src.a;\n" +
                "    mapping: meta::myMapping;\n" +
                "    runtime: meta::myRuntime;\n" +
                "  }\n" +
                "  test: Single\n" +
                "  {\n" +
                "    data: 'data';\n" +
                "  }\n" +
                "}\n", "PARSER error at [2:1-19:1]: Field 'documentation' should be specified only once");
        test("###Service\n" +
                "Service meta::pure::myServiceSingle\n" +
                "{\n" +
                "  documentation: 'this is just for context';\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  autoActivateUpdates: true;\n" +
                "  owners: ['ownerName', 'ownerName2'];\n" +
                "  owners: ['ownerName', 'ownerName2'];\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: src: meta::transform::tests::Address[1]|$src.a;\n" +
                "    mapping: meta::myMapping;\n" +
                "    runtime: meta::myRuntime;\n" +
                "  }\n" +
                "  test: Single\n" +
                "  {\n" +
                "    data: 'data';\n" +
                "  }\n" +
                "}\n", "PARSER error at [2:1-19:1]: Field 'owners' should be specified only once");
        test("###Service\n" +
                "Service meta::pure::myServiceSingle\n" +
                "{\n" +
                "  documentation: 'this is just for context';\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  owners: ['ownerName', 'ownerName2'];\n" +
                "  autoActivateUpdates: true;\n" +
                "  autoActivateUpdates: false;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: src: meta::transform::tests::Address[1]|$src.a;\n" +
                "    mapping: meta::myMapping;\n" +
                "    runtime: meta::myRuntime;\n" +
                "  }\n" +
                "  test: Single\n" +
                "  {\n" +
                "    data: 'data';\n" +
                "  }\n" +
                "}\n", "PARSER error at [2:1-19:1]: Field 'autoActivateUpdates' should be specified only once");
        test("###Service\n" +
                "Service meta::pure::myServiceSingle\n" +
                "{\n" +
                "  documentation: 'this is just for context';\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: src: meta::transform::tests::Address[1]|$src.a;\n" +
                "    mapping: meta::myMapping;\n" +
                "    runtime: meta::myRuntime;\n" +
                "  }\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: src: meta::transform::tests::Address[1]|$src.a;\n" +
                "    mapping: meta::myMapping;\n" +
                "    runtime: meta::myRuntime;\n" +
                "  }\n" +
                "  test: Single\n" +
                "  {\n" +
                "    data: 'data';\n" +
                "  }\n" +
                "}\n", "PARSER error at [2:1-22:1]: Field 'execution' should be specified only once");
        test("###Service\n" +
                "Service meta::pure::myServiceSingle\n" +
                "{\n" +
                "  documentation: 'this is just for context';\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  test: Single\n" +
                "  {\n" +
                "    data: 'data';\n" +
                "  }\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: src: meta::transform::tests::Address[1]|$src.a;\n" +
                "    mapping: meta::myMapping;\n" +
                "    runtime: meta::myRuntime;\n" +
                "  }\n" +
                "  test: Single\n" +
                "  {\n" +
                "    data: 'data';\n" +
                "  }\n" +
                "}\n", "PARSER error at [2:1-20:1]: Field 'test' should be specified only once");
    }

    @Test
    public void testServiceWithSingleExecution()
    {
        // Missing fields
        test("###Service\n" +
                "Service meta::pure::myServiceSingle\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  documentation: 'this is just for context';\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    runtime: meta::myRuntime;\n" +
                "    mapping: meta::myMapping;\n" +
                "  }\n" +
                "  test: Single\n" +
                "  {\n" +
                "    data: 'moreThanData';\n" +
                "  }\n" +
                "}\n", "PARSER error at [6:14-10:3]: Field 'query' is required");
        test("###Service\n" +
                "Service meta::pure::myServiceSingle\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  documentation: 'this is just for context';\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    runtime: meta::myRuntime;\n" +
                "    query: src: meta::transform::tests::Address[1]|$src.a;\n" +
                "  }\n" +
                "  test: Single\n" +
                "  {\n" +
                "    data: 'moreThanData';\n" +
                "  }\n" +
                "}\n", "PARSER error at [6:14-10:3]: Field 'mapping' is required");
        test("###Service\n" +
                "Service meta::pure::myServiceSingle\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  documentation: 'this is just for context';\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    mapping: meta::myMapping;\n" +
                "    query: src: meta::transform::tests::Address[1]|$src.a;\n" +
                "  }\n" +
                "  test: Single\n" +
                "  {\n" +
                "    data: 'moreThanData';\n" +
                "  }\n" +
                "}\n", "PARSER error at [6:14-10:3]: Field 'runtime' is required");
        test("###Service\n" +
                "Service meta::pure::myServiceSingle\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  documentation: 'this is just for context';\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    runtime: meta::myRuntime;\n" +
                "    mapping: meta::myMapping;\n" +
                "    query: src: meta::transform::tests::Address[1]|$src.a;\n" +
                "  }\n" +
                "  test: Single\n" +
                "  {\n" +
                "  }\n" +
                "}\n", "PARSER error at [12:9-14:3]: Field 'data' is required");
        // Duplicated fields
        test("###Service\n" +
                "Service meta::pure::myServiceSingle\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  documentation: 'this is just for context';\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: src: meta::transform::tests::Address[1]|$src.a;\n" +
                "    query: src: meta::transform::tests::Address[1]|$src.a;\n" +
                "    mapping: meta::myMapping;\n" +
                "    runtime: meta::myRuntime;\n" +
                "  }\n" +
                "  test: Single\n" +
                "  {\n" +
                "    data: 'moreThanData';\n" +
                "  }\n" +
                "}\n", "PARSER error at [6:14-12:3]: Field 'query' should be specified only once");
        test("###Service\n" +
                "Service meta::pure::myServiceSingle\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  documentation: 'this is just for context';\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: src: meta::transform::tests::Address[1]|$src.a;\n" +
                "    mapping: meta::myMapping;\n" +
                "    mapping: meta::myMapping;\n" +
                "    runtime: meta::myRuntime;\n" +
                "  }\n" +
                "  test: Single\n" +
                "  {\n" +
                "    data: 'moreThanData';\n" +
                "  }\n" +
                "}\n", "PARSER error at [6:14-12:3]: Field 'mapping' should be specified only once");
        test("###Service\n" +
                "Service meta::pure::myServiceSingle\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  documentation: 'this is just for context';\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: src: meta::transform::tests::Address[1]|$src.a;\n" +
                "    mapping: meta::myMapping;\n" +
                "    runtime: meta::myRuntime;\n" +
                "    runtime: meta::myRuntime;\n" +
                "  }\n" +
                "  test: Single\n" +
                "  {\n" +
                "    data: 'moreThanData';\n" +
                "  }\n" +
                "}\n", "PARSER error at [6:14-12:3]: Field 'runtime' should be specified only once");
        test("###Service\n" +
                "Service meta::pure::myServiceSingle\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  documentation: 'this is just for context';\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: src: meta::transform::tests::Address[1]|$src.a;\n" +
                "    mapping: meta::myMapping;\n" +
                "    runtime: meta::myRuntime;\n" +
                "  }\n" +
                "  test: Single\n" +
                "  {\n" +
                "    data: 'moreThanData';\n" +
                "    data: 'moreThanData';\n" +
                "  }\n" +
                "}\n", "PARSER error at [12:9-16:3]: Field 'data' should be specified only once");
        test("###Service\n" +
                "Service meta::pure::myServiceSingle\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  documentation: 'this is just for context';\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: src: meta::transform::tests::Address[1]|$src.a;\n" +
                "    mapping: meta::myMapping;\n" +
                "    runtime: meta::myRuntime;\n" +
                "  }\n" +
                "  test: Single\n" +
                "  {\n" +
                "    data: 'moreThanData';\n" +
                "    asserts: [];\n" +
                "    asserts: [];\n" +
                "  }\n" +
                "}\n", "PARSER error at [12:9-17:3]: Field 'asserts' should be specified only once");
    }

    @Test
    public void testFaultyServiceWithMissingAttributes()
    {
        test("###Service\n" +
                "Service meta::pure::myServiceSingle\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  documentation: 'this is just for context';\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: src: meta::transform::tests::Address[1]|$src.a;\n" +
                "    mapping: meta::myMapping;\n" +
                "  }\n" +
                "  test: Single\n" +
                "  {\n" +
                "    data: 'moreThanData';\n" +
                "    asserts: [];\n" +
                "  }\n" +
                "}\n", "PARSER error at [6:14-10:3]: Field 'runtime' is required");
        test("###Service\n" +
                "Service meta::pure::myServiceSingle\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  documentation: 'this is just for context';\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: src: meta::transform::tests::Address[1]|$src.a;\n" +
                "    runtime: meta::myRuntime;\n" +
                "  }\n" +
                "  test: Single\n" +
                "  {\n" +
                "    data: 'moreThanData';\n" +
                "    asserts: [];\n" +
                "  }\n" +
                "}\n", "PARSER error at [6:14-10:3]: Field 'mapping' is required");
    }

    @Test
    public void testServiceWithMultiExecution()
    {
        // Missing fields
        test("###Service\n" +
                "Service meta::pure::myServiceMulti\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  documentation: 'this is just for context';\n" +
                "  execution: Multi\n" +
                "  {\n" +
                "    executions['QA']:\n" +
                "    {\n" +
                "      mapping: meta::myMapping1;\n" +
                "      runtime: test::runtime;\n" +
                "    }\n" +
                "    query: src: meta::transform::tests::Address[1]|$src.a;\n" +
                "  }\n" +
                "  test: Multi\n" +
                "  {\n" +
                "    tests['QA']:\n" +
                "    {\n" +
                "      data: 'moreData';\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [6:14-14:3]: Field 'key' is required");
        test("###Service\n" +
                "Service meta::pure::myServiceMulti\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  documentation: 'this is just for context';\n" +
                "  execution: Multi\n" +
                "  {\n" +
                "    key: 'env';\n" +
                "    executions['QA']:\n" +
                "    {\n" +
                "      mapping: meta::myMapping1;\n" +
                "      runtime: test::runtime;\n" +
                "    }\n" +
                "  }\n" +
                "  test: Multi\n" +
                "  {\n" +
                "    tests['QA']:\n" +
                "    {\n" +
                "      data: 'moreData';\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [6:14-14:3]: Field 'query' is required");
        test("###Service\n" +
                "Service meta::pure::myServiceMulti\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  documentation: 'this is just for context';\n" +
                "  execution: Multi\n" +
                "  {\n" +
                "    query: src: meta::transform::tests::Address[1]|$src.a;\n" +
                "    key: 'env';\n" +
                "    executions['QA']:\n" +
                "    {\n" +
                "      runtime: test::runtime;\n" +
                "    }\n" +
                "  }\n" +
                "  test: Multi\n" +
                "  {\n" +
                "    tests['QA']:\n" +
                "    {\n" +
                "      data: 'moreData';\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [10:5-13:5]: Field 'mapping' is required");
        test("###Service\n" +
                "Service meta::pure::myServiceMulti\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  documentation: 'this is just for context';\n" +
                "  execution: Multi\n" +
                "  {\n" +
                "    query: src: meta::transform::tests::Address[1]|$src.a;\n" +
                "    key: 'env';\n" +
                "    executions['QA']:\n" +
                "    {\n" +
                "      mapping: meta::myMapping1;\n" +
                "    }\n" +
                "  }\n" +
                "  test: Multi\n" +
                "  {\n" +
                "    tests['QA']:\n" +
                "    {\n" +
                "      data: 'moreData';\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [10:5-13:5]: Field 'runtime' is required");
        test("###Service\n" +
                "Service meta::pure::myServiceMulti\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  documentation: 'this is just for context';\n" +
                "  execution: Multi\n" +
                "  {\n" +
                "    query: src: meta::transform::tests::Address[1]|$src.a;\n" +
                "    key: 'env';\n" +
                "    executions['QA']:\n" +
                "    {\n" +
                "      mapping: meta::myMapping1;\n" +
                "      runtime: test::runtime;\n" +
                "    }\n" +
                "  }\n" +
                "  test: Multi\n" +
                "  {\n" +
                "    tests['QA']:\n" +
                "    {\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [18:5-20:5]: Field 'data' is required");
        // Duplicated fields
        test("###Service\n" +
                "Service meta::pure::myServiceMulti\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  documentation: 'this is just for context';\n" +
                "  execution: Multi\n" +
                "  {\n" +
                "    query: src: meta::transform::tests::Address[1]|$src.a;\n" +
                "    query: src: meta::transform::tests::Address[1]|$src.a;\n" +
                "    key: 'env';\n" +
                "    executions['QA']:\n" +
                "    {\n" +
                "      mapping: meta::myMapping1;\n" +
                "      runtime: test::runtime;\n" +
                "    }\n" +
                "  }\n" +
                "  test: Multi\n" +
                "  {\n" +
                "    tests['QA']:\n" +
                "    {\n" +
                "      data: 'moreData';\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [6:14-16:3]: Field 'query' should be specified only once");
        test("###Service\n" +
                "Service meta::pure::myServiceMulti\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  documentation: 'this is just for context';\n" +
                "  execution: Multi\n" +
                "  {\n" +
                "    query: src: meta::transform::tests::Address[1]|$src.a;\n" +
                "    key: 'env';\n" +
                "    key: 'env';\n" +
                "    executions['QA']:\n" +
                "    {\n" +
                "      mapping: meta::myMapping1;\n" +
                "      runtime: test::runtime;\n" +
                "    }\n" +
                "  }\n" +
                "  test: Multi\n" +
                "  {\n" +
                "    tests['QA']:\n" +
                "    {\n" +
                "      data: 'moreData';\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [6:14-16:3]: Field 'key' should be specified only once");
        test("###Service\n" +
                "Service meta::pure::myServiceMulti\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  documentation: 'this is just for context';\n" +
                "  execution: Multi\n" +
                "  {\n" +
                "    query: src: meta::transform::tests::Address[1]|$src.a;\n" +
                "    key: 'env';\n" +
                "    executions['QA']:\n" +
                "    {\n" +
                "      mapping: meta::myMapping1;\n" +
                "      mapping: meta::myMapping1;\n" +
                "      runtime: test::runtime;\n" +
                "    }\n" +
                "  }\n" +
                "  test: Multi\n" +
                "  {\n" +
                "    tests['QA']:\n" +
                "    {\n" +
                "      data: 'moreData';\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [10:5-15:5]: Field 'mapping' should be specified only once");
        test("###Service\n" +
                "Service meta::pure::myServiceMulti\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  documentation: 'this is just for context';\n" +
                "  execution: Multi\n" +
                "  {\n" +
                "    query: src: meta::transform::tests::Address[1]|$src.a;\n" +
                "    key: 'env';\n" +
                "    executions['QA']:\n" +
                "    {\n" +
                "      mapping: meta::myMapping1;\n" +
                "      runtime: test::runtime;\n" +
                "      runtime: test::runtime;\n" +
                "    }\n" +
                "  }\n" +
                "  test: Multi\n" +
                "  {\n" +
                "    tests['QA']:\n" +
                "    {\n" +
                "      data: 'moreData';\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [10:5-15:5]: Field 'runtime' should be specified only once");
        test("###Service\n" +
                "Service meta::pure::myServiceMulti\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  documentation: 'this is just for context';\n" +
                "  execution: Multi\n" +
                "  {\n" +
                "    query: src: meta::transform::tests::Address[1]|$src.a;\n" +
                "    key: 'env';\n" +
                "    executions['QA']:\n" +
                "    {\n" +
                "      mapping: meta::myMapping1;\n" +
                "      runtime: test::runtime;\n" +
                "    }\n" +
                "  }\n" +
                "  test: Multi\n" +
                "  {\n" +
                "    tests['QA']:\n" +
                "    {\n" +
                "      data: 'moreData';\n" +
                "      data: 'moreData';\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [18:5-22:5]: Field 'data' should be specified only once");
        test("###Service\n" +
                "Service meta::pure::myServiceMulti\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  documentation: 'this is just for context';\n" +
                "  execution: Multi\n" +
                "  {\n" +
                "    query: src: meta::transform::tests::Address[1]|$src.a;\n" +
                "    key: 'env';\n" +
                "    executions['QA']:\n" +
                "    {\n" +
                "      mapping: meta::myMapping1;\n" +
                "      runtime: test::runtime;\n" +
                "    }\n" +
                "  }\n" +
                "  test: Multi\n" +
                "  {\n" +
                "    tests['QA']:\n" +
                "    {\n" +
                "      data: 'moreData';\n" +
                "      asserts: [];\n" +
                "      asserts: [];\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [18:5-23:5]: Field 'asserts' should be specified only once");
    }

    @Test
    public void testServiceRuntime()
    {
        // empty embedded runtime
        test("###Service\n" +
                "Service test::Service\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  owners: ['ownerName', 'ownerName2'];\n" +
                "  documentation: 'test';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: src: test::class[1]|$src.prop1;\n" +
                "    mapping: test::mapping;\n" +
                "    runtime: \n" +
                "    #{\n" +
                "    }#;\n" +
                "  }\n" +
                "  test: Single\n" +
                "  {\n" +
                "    data: 'moreThanData';\n" +
                "    asserts:\n" +
                "    [\n" +
                "    ];\n" +
                "  }\n" +
                "}\n", "PARSER error at [13:5-14:7]: Embedded runtime must not be empty");
        // Missing mappings field, but since the runtime is embedded, it will be fine
        test("###Service\n" +
                "Service meta::pure::myServiceSingle\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  owners: ['ownerName', 'ownerName2'];\n" +
                "  documentation: 'this is just for context';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: src: meta::transform::tests::Address[1]|$src.a;\n" +
                "    mapping: meta::myMapping;\n" +
                "    runtime:\n" +
                "    #{\n" +
                "      connections:\n" +
                "      [\n" +
                "        ModelStore:\n" +
                "        [\n" +
                "        ]\n" +
                "      ];\n" +
                "    }#;\n" +
                "  }\n" +
                "  test: Single\n" +
                "  {\n" +
                "    data: 'test';\n" +
                "    asserts:\n" +
                "    [\n" +
                "    ];\n" +
                "  }\n" +
                "}\n");
    }

    @Test
    public void testServiceTestParameters()
    {
        // check for single test parameter
        test("###Service\n" +
                "Service meta::pure::myServiceSingle\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  owners: ['ownerName', 'ownerName2'];\n" +
                "  documentation: 'this is just for context';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: p1: String[1]|service_parameters::_NPerson.all()->graphFetch(#{service_parameters::_NPerson{Age,Name}}#)->serialize(#{service_parameters::_NPerson{Age,Name,f1($p1)}}#);\n" +
                "    mapping: meta::myMapping;\n" +
                "    runtime:\n" +
                "    #{\n" +
                "      connections:\n" +
                "      [\n" +
                "        ModelStore:\n" +
                "        [\n" +
                "        ]\n" +
                "      ];\n" +
                "    }#;\n" +
                "  }\n" +
                "  test: Single\n" +
                "  {\n" +
                "    data: 'test';\n" +
                "    asserts:\n" +
                "    [\n" +
                "       { ['parameter1'], res: Result<Any|*>[1]|$res.values->cast(@TabularDataSet).rows->size() == 1 }\n" +
                "    ];\n" +
                "  }\n" +
                "}\n");

        // check for multiple test parameters
        test("###Service\n" +
                "Service meta::pure::myServiceSingle\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  owners: ['ownerName', 'ownerName2'];\n" +
                "  documentation: 'this is just for context';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: p1: String[*]|service_parameters::_NPerson.all()->graphFetch(#{service_parameters::_NPerson{Age,Name}}#)->serialize(#{service_parameters::_NPerson{Age,Name,f1($p1)}}#);\n" +
                "    mapping: meta::myMapping;\n" +
                "    runtime:\n" +
                "    #{\n" +
                "      connections:\n" +
                "      [\n" +
                "        ModelStore:\n" +
                "        [\n" +
                "        ]\n" +
                "      ];\n" +
                "    }#;\n" +
                "  }\n" +
                "  test: Single\n" +
                "  {\n" +
                "    data: 'test';\n" +
                "    asserts:\n" +
                "    [\n" +
                "      {['parameter1', %23:12:8.54, true, 440, 13.23, 88, -54, 2.3, [1, 2], %2019-05-24T00:00:00, %2019-05-24, Enum.Reference],res: meta::pure::mapping::Result[1]|$res.values->toOne()->toString()->equalJsonStrings('{}')}\n" +
                "    ];\n" +
                "  }\n" +
                "}\n");

        test("###Service\n" +
                "Service meta::pure::myServiceSingle\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  owners: ['ownerName', 'ownerName2'];\n" +
                "  documentation: 'this is just for context';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: p1: String[*]|service_parameters::_NPerson.all()->graphFetch(#{service_parameters::_NPerson{Age,Name}}#)->serialize(#{service_parameters::_NPerson{Age,Name,f1($p1)}}#);\n" +
                "    mapping: meta::myMapping;\n" +
                "    runtime:\n" +
                "    #{\n" +
                "      connections:\n" +
                "      [\n" +
                "        ModelStore:\n" +
                "        [\n" +
                "        ]\n" +
                "      ];\n" +
                "    }#;\n" +
                "  }\n" +
                "  test: Single\n" +
                "  {\n" +
                "    data: 'test';\n" +
                "    asserts:\n" +
                "    [\n" +
                "      {['testparameter1', 'testparameter2'],res: meta::pure::mapping::Result[1]|$res.values->toOne()->toString()->equalJsonStrings('{}')},\n" +
                "      {['testparameter'],res: meta::pure::mapping::Result[1]|$res.values->toOne()->toString()->equalJsonStrings('{}')}\n" +
                "    ];\n" +
                "  }\n" +
                "}\n");
    }

    @Test
    public void testServiceTestSuitesErrorMessages()
    {
        //Test suite should have tests section
        test("###Service\n" +
                        "Service meta::pure::myServiceSingle\n" +
                        "{\n" +
                        "  pattern: 'url/myUrl/';\n" +
                        "  owners:\n" +
                        "  [\n" +
                        "    'ownerName',\n" +
                        "    'ownerName2'\n" +
                        "  ];\n" +
                        "  documentation: 'this is just for context';\n" +
                        "  autoActivateUpdates: true;\n" +
                        "  execution: Single\n" +
                        "  {\n" +
                        "    query: |demo::_NPerson.all()->graphFetch(#{demo::_NPerson{Age,Name}}#)->serialize(#{demo::_NPerson{Age,Name}}#);\n" +
                        "    mapping: meta::myMapping;\n" +
                        "    runtime: meta::myRuntime;\n" +
                        "  }\n" +
                        "  testSuites:\n" +
                        "  [\n" +
                        "    testSuite1:\n" +
                        "    {\n" +
                        "      data:\n" +
                        "      [\n" +
                        "        connections:\n" +
                        "        [\n" +
                        "          connection1:\n" +
                        "            ExternalFormat\n" +
                        "            #{\n" +
                        "              contentType: 'application/x.flatdata';\n" +
                        "              data: 'FIRST_NAME,LAST_NAME\\nFred,Bloggs\\nJane,Doe';\n" +
                        "            }#\n" +
                        "        ]\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n",
                "PARSER error at [20:5-34:5]: Field 'tests' is required"
        );

        //Test should have asserts associated with it
        test("###Service\n" +
                        "Service meta::pure::myServiceSingle\n" +
                        "{\n" +
                        "  pattern: 'url/myUrl/';\n" +
                        "  owners:\n" +
                        "  [\n" +
                        "    'ownerName',\n" +
                        "    'ownerName2'\n" +
                        "  ];\n" +
                        "  documentation: 'this is just for context';\n" +
                        "  autoActivateUpdates: true;\n" +
                        "  execution: Single\n" +
                        "  {\n" +
                        "    query: |demo::_NPerson.all()->graphFetch(#{demo::_NPerson{Age,Name}}#)->serialize(#{demo::_NPerson{Age,Name}}#);\n" +
                        "    mapping: meta::myMapping;\n" +
                        "    runtime: meta::myRuntime;\n" +
                        "  }\n" +
                        "  testSuites:\n" +
                        "  [\n" +
                        "    testSuite1:\n" +
                        "    {\n" +
                        "      data:\n" +
                        "      [\n" +
                        "        connections:\n" +
                        "        [\n" +
                        "          connection1:\n" +
                        "            ExternalFormat\n" +
                        "            #{\n" +
                        "              contentType: 'application/x.flatdata';\n" +
                        "              data: 'FIRST_NAME,LAST_NAME\\nFred,Bloggs\\nJane,Doe';\n" +
                        "            }#\n" +
                        "        ]\n" +
                        "      ]\n" +
                        "      tests:\n" +
                        "      [\n" +
                        "        test1:\n" +
                        "        {\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n",
                "PARSER error at [36:9-38:9]: Field 'asserts' is required"
        );

        //Test Data should have at max 1 section for connection test data
        test("###Service\n" +
                        "Service meta::pure::myServiceSingle\n" +
                        "{\n" +
                        "  pattern: 'url/myUrl/';\n" +
                        "  owners:\n" +
                        "  [\n" +
                        "    'ownerName',\n" +
                        "    'ownerName2'\n" +
                        "  ];\n" +
                        "  documentation: 'this is just for context';\n" +
                        "  autoActivateUpdates: true;\n" +
                        "  execution: Single\n" +
                        "  {\n" +
                        "    query: |demo::_NPerson.all()->graphFetch(#{demo::_NPerson{Age,Name}}#)->serialize(#{demo::_NPerson{Age,Name}}#);\n" +
                        "    mapping: meta::myMapping;\n" +
                        "    runtime: meta::myRuntime;\n" +
                        "  }\n" +
                        "  testSuites:\n" +
                        "  [\n" +
                        "    testSuite1:\n" +
                        "    {\n" +
                        "      data:\n" +
                        "      [\n" +
                        "        connections:\n" +
                        "        [\n" +
                        "          connection1:\n" +
                        "            ExternalFormat\n" +
                        "            #{\n" +
                        "              contentType: 'application/x.flatdata';\n" +
                        "              data: 'FIRST_NAME,LAST_NAME\\nFred,Bloggs\\nJane,Doe';\n" +
                        "            }#\n" +
                        "        ]\n" +
                        "        connections:\n" +
                        "        [\n" +
                        "          connection2:\n" +
                        "            ExternalFormat\n" +
                        "            #{\n" +
                        "              contentType: 'application/x.flatdata';\n" +
                        "              data: 'FIRST_NAME,LAST_NAME\\nFred,Bloggs\\nJane,Doe';\n" +
                        "            }#\n" +
                        "        ]\n" +
                        "      ]\n" +
                        "      tests:\n" +
                        "      [\n" +
                        "        test1:\n" +
                        "        {\n" +
                        "          asserts:\n" +
                        "          [\n" +
                        "            assert1:\n" +
                        "              EqualToJson\n" +
                        "              #{\n" +
                        "                expected:\n" +
                        "                  ExternalFormat\n" +
                        "                  #{\n" +
                        "                    contentType: 'application/json';\n" +
                        "                    data: '{Age:12, Name:\"dummy\"}';\n" +
                        "                  }#;\n" +
                        "              }#\n" +
                        "          ]\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n",
                "PARSER error at [22:7-42:7]: Field 'connections' should be specified only once"
        );

        //assert keys should have atleast one element
        test("###Service\n" +
                "Service meta::pure::myServiceSingle\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  owners:\n" +
                "  [\n" +
                "    'ownerName',\n" +
                "    'ownerName2'\n" +
                "  ];\n" +
                "  documentation: 'this is just for context';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Multi\n" +
                "  {\n" +
                "    query: |model::pure::mapping::modelToModel::test::shared::dest::Product.all()->graphFetchChecked(#{model::pure::mapping::modelToModel::test::shared::dest::Product{name}}#)->serialize(#{model::pure::mapping::modelToModel::test::shared::dest::Product{name}}#);\n" +
                "    key: 'env';\n" +
                "    executions['QA']:\n" +
                "    {\n" +
                "      mapping: meta::myMapping1;\n" +
                "      runtime: test::runtime;\n" +
                "    }\n" +
                "    executions['UAT']:\n" +
                "    {\n" +
                "      mapping: meta::myMapping2;\n" +
                "      runtime: meta::myRuntime;\n" +
                "    }\n" +
                "  }\n" +
                "  testSuites:\n" +
                "  [\n" +
                "    testSuite1:\n" +
                "    {\n" +
                "      data:\n" +
                "      [\n" +
                "        connections:\n" +
                "        [\n" +
                "          connection1:\n" +
                "            ExternalFormat\n" +
                "            #{\n" +
                "              contentType: 'application/x.flatdata';\n" +
                "              data: 'FIRST_NAME,LAST_NAME\\nFred,Bloggs\\nJane,Doe';\n" +
                "            }#,\n" +
                "          connection2:\n" +
                "            ModelStore\n" +
                "            #{\n" +
                "              my::Person:\n" +
                "                [\n" +
                "                  ^my::Person(\n" +
                "                    givenNames = ['Fred', 'William'],\n" +
                "                    address = ^my::Address(street = 'A Road')\n" +
                "                  )\n" +
                "                ]\n" +
                "            }#\n" +
                "        ]\n" +
                "      ]\n" +
                "      tests:\n" +
                "      [\n" +
                "        test1:\n" +
                "        {\n" +
                "          keys:\n" +
                "          [\n" +
                "          ];\n" +
                "          asserts:\n" +
                "          [\n" +
                "            assert1:\n" +
                "              EqualToJson\n" +
                "              #{\n" +
                "                expected:\n" +
                "                  ExternalFormat\n" +
                "                  #{\n" +
                "                    contentType: 'application/json';\n" +
                "                    data: '{Age:12, Name:\"dummy\"}';\n" +
                "                  }#;\n" +
                "              }#\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n",
                "PARSER error at [60:11]: Unexpected token"
        );

        //Unknown assert type
        test("###Service\n" +
                        "Service meta::pure::myServiceSingle\n" +
                        "{\n" +
                        "  pattern: 'url/myUrl/';\n" +
                        "  owners:\n" +
                        "  [\n" +
                        "    'ownerName',\n" +
                        "    'ownerName2'\n" +
                        "  ];\n" +
                        "  documentation: 'this is just for context';\n" +
                        "  autoActivateUpdates: true;\n" +
                        "  execution: Single\n" +
                        "  {\n" +
                        "    query: |demo::_NPerson.all()->graphFetch(#{demo::_NPerson{Age,Name}}#)->serialize(#{demo::_NPerson{Age,Name}}#);\n" +
                        "    mapping: meta::myMapping;\n" +
                        "    runtime: meta::myRuntime;\n" +
                        "  }\n" +
                        "  testSuites:\n" +
                        "  [\n" +
                        "    testSuite1:\n" +
                        "    {\n" +
                        "      data:\n" +
                        "      [\n" +
                        "        connections:\n" +
                        "        [\n" +
                        "          connection1:\n" +
                        "            ExternalFormat\n" +
                        "            #{\n" +
                        "              contentType: 'application/x.flatdata';\n" +
                        "              data: 'FIRST_NAME,LAST_NAME\\nFred,Bloggs\\nJane,Doe';\n" +
                        "            }#\n" +
                        "        ]\n" +
                        "      ]\n" +
                        "      tests:\n" +
                        "      [\n" +
                        "        test1:\n" +
                        "        {\n" +
                        "          asserts:\n" +
                        "          [\n" +
                        "            assert1:\n" +
                        "              Unknown\n" +
                        "              #{\n" +
                        "              }#\n" +
                        "          ]\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n",
                "PARSER error at [41:15-21]: Unknown test assertion type: Unknown"
        );

        //Unknown embedded data type
        test("###Service\n" +
                        "Service meta::pure::myServiceSingle\n" +
                        "{\n" +
                        "  pattern: 'url/myUrl/';\n" +
                        "  owners:\n" +
                        "  [\n" +
                        "    'ownerName',\n" +
                        "    'ownerName2'\n" +
                        "  ];\n" +
                        "  documentation: 'this is just for context';\n" +
                        "  autoActivateUpdates: true;\n" +
                        "  execution: Single\n" +
                        "  {\n" +
                        "    query: |demo::_NPerson.all()->graphFetch(#{demo::_NPerson{Age,Name}}#)->serialize(#{demo::_NPerson{Age,Name}}#);\n" +
                        "    mapping: meta::myMapping;\n" +
                        "    runtime: meta::myRuntime;\n" +
                        "  }\n" +
                        "  testSuites:\n" +
                        "  [\n" +
                        "    testSuite1:\n" +
                        "    {\n" +
                        "      data:\n" +
                        "      [\n" +
                        "        connections:\n" +
                        "        [\n" +
                        "          connection1:\n" +
                        "            Unknown\n" +
                        "            #{\n" +
                        "            }#\n" +
                        "        ]\n" +
                        "      ]\n" +
                        "      tests:\n" +
                        "      [\n" +
                        "        test1:\n" +
                        "        {\n" +
                        "          asserts:\n" +
                        "          [\n" +
                        "            assert1:\n" +
                        "              EqualToJson\n" +
                        "              #{\n" +
                        "                expected:\n" +
                        "                  ExternalFormat\n" +
                        "                  #{\n" +
                        "                    contentType: 'application/json';\n" +
                        "                    data: '{Age:12, Name:\"dummy\"}';\n" +
                        "                  }#;\n" +
                        "              }#\n" +
                        "          ]\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n",
                "PARSER error at [27:13-19]: Unknown embedded data type: Unknown"
        );

        //Incorrect asserts
        test("###Service\n" +
                        "Service meta::pure::myServiceSingle\n" +
                        "{\n" +
                        "  pattern: 'url/myUrl/';\n" +
                        "  owners:\n" +
                        "  [\n" +
                        "    'ownerName',\n" +
                        "    'ownerName2'\n" +
                        "  ];\n" +
                        "  documentation: 'this is just for context';\n" +
                        "  autoActivateUpdates: true;\n" +
                        "  execution: Single\n" +
                        "  {\n" +
                        "    query: |demo::_NPerson.all()->graphFetch(#{demo::_NPerson{Age,Name}}#)->serialize(#{demo::_NPerson{Age,Name}}#);\n" +
                        "    mapping: meta::myMapping;\n" +
                        "    runtime: meta::myRuntime;\n" +
                        "  }\n" +
                        "  testSuites:\n" +
                        "  [\n" +
                        "    testSuite1:\n" +
                        "    {\n" +
                        "      data:\n" +
                        "      [\n" +
                        "        connections:\n" +
                        "        [\n" +
                        "          connection1:\n" +
                        "            ExternalFormat\n" +
                        "            #{\n" +
                        "              contentType: 'application/x.flatdata';\n" +
                        "              data: 'FIRST_NAME,LAST_NAME\\nFred,Bloggs\\nJane,Doe';\n" +
                        "            }#\n" +
                        "        ]\n" +
                        "      ]\n" +
                        "      tests:\n" +
                        "      [\n" +
                        "        test1:\n" +
                        "        {\n" +
                        "          asserts:\n" +
                        "          [\n" +
                        "            assert1:\n" +
                        "              EqualToJson\n" +
                        "              #{\n" +
                        "                actual : \n" +
                        "                  ExternalFormat\n" +
                        "                  #{\n" +
                        "                    contentType: 'application/json';\n" +
                        "                    data: '{Age:12, Name:\"dummy\"}';\n" +
                        "                  }#;\n" +
                        "              }#\n" +
                        "          ]\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n",
                "PARSER error at [43:17-22]: Unexpected token 'actual'"
        );
    }

    @Test
    public void testExecutionEnvironmentErrorMessages()
    {

        test("###Service\n" +
                "ExecutionEnvironment test::executionEnvironment\n" +
                "{\n" +
                "  executions:\n" +
                "  [\n" +
                "    UAT:\n" +
                "    {\n" +
                "      runtime: test::myRuntime1;\n" +
                "    },\n" +
                "    PROD:\n" +
                "    {\n" +
                "      mapping: test::myMapping2;\n" +
                "      runtime: test::myRuntime2;\n" +
                "    }\n" +
                "  ];\n" +
                "}\n", "PARSER error at [8:7-13]: Unexpected token 'runtime'");

        test("###Service\n" +
                "ExecutionEnvironment test::executionEnvironment\n" +
                "{\n" +
                "  executions:\n" +
                "  [\n" +
                "    UAT:\n" +
                "    {\n" +
                "      mapping: test::myMapping1;\n" +
                "    }\n" +
                "  ];\n" +
                "}\n", "PARSER error at [9:5]: Unexpected token '}'");
    }

    @Test
    public void testServiceWithExecutionEnvironment()
    {
        test("###Service\n" +
                "Service meta::pure::myServiceMulti\n" +
                "{\n" +
                "  pattern: 'url/myUrl/{env}';\n" +
                "  owners:\n" +
                "  [\n" +
                "    'ownerName'\n" +
                "  ];\n" +
                "  documentation: 'this is just for context';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Multi\n" +
                "  {\n" +
                "    query: env: String[1]|model::pure::mapping::modelToModel::test::shared::dest::Product.all()->from(test::executionEnvironment->get($env))->graphFetchChecked(#{model::pure::mapping::modelToModel::test::shared::dest::Product{name}}#)->serialize(#{model::pure::mapping::modelToModel::test::shared::dest::Product{name}}#);\n" +
                "  }\n" +
                "  test: Multi\n" +
                "  {\n" +
                "    tests['QA']:\n" +
                "    {\n" +
                "      data: 'moreData';\n" +
                "      asserts:\n" +
                "      [\n" +
                "        { [], res: Result<Any|*>[1]|$res.values->cast(@TabularDataSet).rows->size() == 1 },\n" +
                "        { [], res: Result<Any|*>[1]|$res.values->cast(@TabularDataSet).rows->size() == 1 }\n" +
                "      ];\n" +
                "    }\n" +
                "    tests['UAT']:\n" +
                "    {\n" +
                "      data: 'moreData';\n" +
                "      asserts:\n" +
                "      [\n" +
                "        { [], res: Result<Any|*>[1]|$res.values->cast(@TabularDataSet).rows->size() == 1 },\n" +
                "        { [], res: Result<Any|*>[1]|$res.values->cast(@TabularDataSet).rows->size() == 1 }\n" +
                "      ];\n" +
                "    }\n" +
                "  }\n" +
                "}\n", null);
    }

    @Test
    public void testServiceWithPostValidation()
    {
        // Missing fields
        test("###Service \n" +
                "Service test::Service \n" +
                "{ \n" +
                "  pattern: 'url/myUrl'; \n" +
                "  owners: ['ownerName']; \n" +
                "  documentation: 'test'; \n" +
                "  autoActivateUpdates: true; \n" +
                "  execution: Single \n" +
                "  { \n" +
                "    query: |test::class.all()->project([col(p|$p.prop1, 'prop1')]); \n" +
                "    mapping: test::mapping; \n" +
                "    runtime: test::runtime; \n" +
                "  }\n" +
                "  postValidations:\n" +
                "  [\n" +
                "    {\n" +
                "      params: [];\n" +
                "      assertions: [\n" +
                "          testAssert: tds: TabularDataSet[1]|$tds->filter(row|$row.getString('firstName')->startsWith('T'))->meta::legend::service::validation::assertTabularDataSetEmpty('Expected no first names to begin with the letter T');\n" +
                "      ];\n" +
                "    }\n" +
                "  ]\n" +
                "}", "PARSER error at [16:5-21:5]: Field 'description' is required");

        test("###Service \n" +
                "Service test::Service \n" +
                "{ \n" +
                "  pattern: 'url/myUrl'; \n" +
                "  owners: ['ownerName']; \n" +
                "  documentation: 'test'; \n" +
                "  autoActivateUpdates: true; \n" +
                "  execution: Single \n" +
                "  { \n" +
                "    query: |test::class.all()->project([col(p|$p.prop1, 'prop1')]); \n" +
                "    mapping: test::mapping; \n" +
                "    runtime: test::runtime; \n" +
                "  }\n" +
                "  postValidations:\n" +
                "  [\n" +
                "    {\n" +
                "      description: 'A good description of the validation';\n" +
                "      assertions: [\n" +
                "          testAssert: tds: TabularDataSet[1]|$tds->filter(row|$row.getString('firstName')->startsWith('T'))->meta::legend::service::validation::assertTabularDataSetEmpty('Expected no first names to begin with the letter T');\n" +
                "      ];\n" +
                "    }\n" +
                "  ]\n" +
                "}", "PARSER error at [16:5-21:5]: Field 'params' is required");

        test("###Service \n" +
                "Service test::Service \n" +
                "{ \n" +
                "  pattern: 'url/myUrl'; \n" +
                "  owners: ['ownerName']; \n" +
                "  documentation: 'test'; \n" +
                "  autoActivateUpdates: true; \n" +
                "  execution: Single \n" +
                "  { \n" +
                "    query: |test::class.all()->project([col(p|$p.prop1, 'prop1')]); \n" +
                "    mapping: test::mapping; \n" +
                "    runtime: test::runtime; \n" +
                "  }\n" +
                "  postValidations:\n" +
                "  [\n" +
                "    {\n" +
                "      description: 'A good description of the validation';\n" +
                "      params: [];\n" +
                "    }\n" +
                "  ]\n" +
                "}", "PARSER error at [16:5-19:5]: Field 'assertions' is required");
    }
}