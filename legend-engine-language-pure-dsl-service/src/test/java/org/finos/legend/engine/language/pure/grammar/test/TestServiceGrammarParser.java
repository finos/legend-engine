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
                "}\n", "PARSER error at [2:1-12:1]: Field 'test' is required");
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
    public void testServiceTags()
    {
        // check for single tag
        test("###Service\n" +
                "Service meta::pure::myServiceSingle\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  owners: ['ownerName', 'ownerName2'];\n" +
                "  serviceTags:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'testName';\n" +
                "      value: 'testValue';\n" +
                "    }\n" +
                "  ];\n"+
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

        // check for multiple tags
        test("###Service\n" +
                "Service meta::pure::myServiceSingle\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  owners: ['ownerName', 'ownerName2'];\n" +
                "  serviceTags:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'testName1';\n" +
                "      value: 'testValue1';\n" +
                "    },\n" +
                "    {\n" +
                "      name: 'testName2';\n" +
                "      value: 'testValue2';\n" +
                "    }\n" +
                "  ];\n"+
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
                "      {['testparameter1', 'testparameter2', enum.reference, [1, 2]],'expression'}\n" +
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
                "      {[],res: Result<Any|*>[1]|$res.values->cast(@TabularDataSet).rows->size() == 1},\n" +
                "      {['testparameter1', 'testparameter2'],'expression1'},\n" +
                "      {['testparameter'],'expression2'}\n" +
                "    ];\n" +
                "  }\n" +
                "}\n");
    }
}
