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

package org.finos.legend.engine.language.pure.grammar.integration;

import org.finos.legend.engine.language.pure.compiler.test.TestCompilationFromGrammar;
import org.junit.Test;

public class TestMongoDBMappingCompiler extends TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite
{

    @Override
    protected String getDuplicatedElementTestCode()
    {
        return "Class anything::class {}\n" +
                "###Mapping\n" +
                "Mapping mongo::test::mapping::MongoDBMapping\n" +
                "(\n" +
                "  *meta::external::mongo::mapping::SomeClass[id1]: MongoDB\n" +
                "  {\n" +
                "    ~mainCollection [mongo::test::db] PersonRecord\n" +
                "  }\n" +
                ")\n" +
                "###MongoDB\n" +
                "Database mongo::test::mapping::MongoDBMapping\n" +
                "(\n" +
                ")";
    }

    @Override
    protected String getDuplicatedElementTestExpectedErrorMessage()
    {
        return "COMPILATION error at [11:1-13:1]: Duplicated element 'mongo::test::mapping::MongoDBMapping'";
    }

    @Test
    public void testMongoDBMappingDefinition()
    {
        test(TestMongoDBCompilerUtil.SAMPLE_STORE +
                TestMongoDBCompilerUtil.MODEL_PLUS_BINDING +
                "###Mapping\n" +
                "Mapping mongo::test::mapping::MongoDBMapping\n" +
                "(\n" +
                "  *meta::external::store::mongodb::showcase::domain::Person[Person]: MongoDB\n" +
                "  {\n" +
                "    ~mainCollection [meta::external::store::mongodb::showcase::store::PersonDatabase] PersonCollection\n" +
                "    ~binding meta::external::store::mongodb::showcase::store::PersonCollectionBinding\n" +
                "  }\n" +
                ")\n");
    }
}