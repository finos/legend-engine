// Copyright 2023 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.language.pure.grammar.integration;

import org.finos.legend.engine.language.pure.compiler.test.TestCompilationFromGrammar;
import org.junit.Test;

public class TestMongoDBConnectionCompiler extends TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite
{

    @Override
    protected String getDuplicatedElementTestCode()
    {
        return "Class anything::class {}\n" +
                "###Connection\n" +
                "MongoDBConnection test::mongodb::connection" +
                "{\n" +
                "  database: legend_db;\n" +
                "  serverURLs: [localhost:27071];\n" +
                "  authentication: # UserPassword {\n" +
                "    username: 'mongo_ro';\n" +
                "    password: SystemPropertiesSecret\n" +
                "    {\n" +
                "      systemPropertyName: 'sys.prop.name';\n" +
                "    };\n" +
                "  }#;\n" +
                "}\n" +
                "###MongoDB\n" +
                "Database test::mongodb::connection\n" +
                "(\n" +
                ")";
    }

    @Override
    protected String getDuplicatedElementTestExpectedErrorMessage()
    {
        return "COMPILATION error at [15:1-17:1]: Duplicated element 'test::mongodb::connection'";
    }

    @Test
    public void testMongoDBConnectionDefinitionv1()
    {
        // the Database meta::external::store::mongodb::showcase::store::PersonDatabase
        // is defined in the SAMPLE_STORE constant
        test(TestMongoDBCompilerUtil.SAMPLE_STORE +
                "###Connection\n" +
                "MongoDBConnection test::testConnection\n" +
                "{\n" +
                "  database: legend_db;\n" +
                "  serverURLs: [localhost:27071];\n" +
                "  authentication: # UserPassword {\n" +
                "    username: 'mongo_ro';\n" +
                "    password: SystemPropertiesSecret\n" +
                "    {\n" +
                "      systemPropertyName: 'sys.prop.name';\n" +
                "    };\n" +
                "  }#;\n" +
                "}\n");
    }
}
