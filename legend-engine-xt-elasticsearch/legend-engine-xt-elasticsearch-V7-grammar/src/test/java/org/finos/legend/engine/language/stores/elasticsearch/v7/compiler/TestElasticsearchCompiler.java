// Copyright 2023 Goldman Sachs
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
//

package org.finos.legend.engine.language.stores.elasticsearch.v7.compiler;

import org.finos.legend.engine.language.pure.compiler.test.TestCompilationFromGrammar;
import org.junit.Test;

public class TestElasticsearchCompiler extends TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite
{
    private static final String BASIC_STORE = "###Elasticsearch\n" +
            "Elasticsearch7Cluster abc::abc::Store\n" +
            "{\n" +
            "  indices: [\n" +
            "    index1: {\n" +
            "      properties: [\n" +
            "        prop1: Keyword,\n" +
            "        prop2: Text,\n" +
            "        prop3: Date,\n" +
            "        prop4: Short,\n" +
            "        prop5: Byte,\n" +
            "        prop6: Integer,\n" +
            "        prop7: Long,\n" +
            "        prop8: Float,\n" +
            "        prop9: HalfFloat,\n" +
            "        prop10: Double,\n" +
            "        prop11: Boolean\n" +
            "      ];\n" +
            "    }\n" +
            "  ];\n" +
            "}\n";

    private static final String BASIC_CONNECTION = "###Connection\n" +
            "Elasticsearch7ClusterConnection abc::abc::Connection\n" +
            "{\n" +
            "  store: abc::abc::Store;\n" +
            "  clusterDetails: # URL { http://dummyurl.com:1234/api }#;\n" +
            "  authentication: # UserPassword {\n" +
            "    username: 'hello_user';\n" +
            "    password: SystemPropertiesSecret\n" +
            "    {\n" +
            "      systemPropertyName: 'sys.prop.name';\n" +
            "    };\n" +
            "  }#;\n" +
            "}\n";

    @Override
    protected String getDuplicatedElementTestCode()
    {
        return BASIC_STORE +
                "\n" +
                BASIC_STORE;
    }

    @Override
    protected String getDuplicatedElementTestExpectedErrorMessage()
    {
        return "COMPILATION error at [24:1-43:1]: Duplicated element 'abc::abc::Store'";
    }

    @Test
    public void testCompileStore()
    {
        test(BASIC_STORE);
    }

    @Test
    public void testCompileConnection()
    {
        test(BASIC_STORE + BASIC_CONNECTION);
    }

    @Test
    public void testCompileIndexToTdsFunctionHandler()
    {
        test(BASIC_STORE + "###Pure\n" +
                "function abc::abc::indexToTdsFunction(): TabularDataSet[1] {\n" +
                "  indexToTDS(abc::abc::Store, 'index1')" +
                "}\n"
        );
    }
}
