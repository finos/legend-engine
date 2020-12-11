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

package org.finos.legend.engine.language.pure.dsl.text.grammar.test;

import org.finos.legend.engine.language.pure.grammar.test.TestGrammarRoundtrip;
import org.junit.Test;

public class TestFileGenerationGrammarRoundtrip extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{
    @Test
    public void testFileGeneration()
    {
        test("###FileGeneration\n" +
                "Avro model::AvroConfig\n" +
                "{\n" +
                "  scopeElements: [model::MyClass, model];\n" +
                "  generationOutputPath: 'myAvroRoot';\n" +
                "  includeNamespace: true;\n" +
                "  propertyProfile: ['model::myProfile', 'model::nextProfile'];\n" +
                "}\n\n" +
                "Java model::JavaConfig\n" +
                "{\n" +
                "  scopeElements: [model::MyClass, model];\n" +
                "  useJavaPrimitives: true;\n" +
                "  optionals: true;\n" +
                "}\n\n" +
                "Protobuf model::ProtobufConfig\n" +
                "{\n" +
                "  generationOutputPath: 'myProtobufRoot/wrapperDirc';\n" +
                "  test: 2;\n" +
                "}\n\n" +
                "JsonSchema model::JSONSchemaConfig\n" +
                "{\n" +
                "}\n\n" +
                "Slang model::SlangConfig\n" +
                "{\n" +
                "  scopeElements: [model::MyClass, model];\n" +
                "  _flags: ['flag1', 'flag2', 'flag3'];\n" +
                "  includeAllRelatedClasses: true;\n" +
                "  useArrayType: true;\n" +
                "}\n\n" +
                "Cdm model::CDMConfig\n" +
                "{\n" +
                "  scopeElements: [model::MyClass, model];\n" +
                "}\n");
        test("###FileGeneration\n" +
                "Avro model::AvroConfig\n" +
                "{\n" +
                "  scopeElements: [model::MyClass, model];\n" +
                "  namespaceOverride: {\n" +
                "    key1: 'mapValue1';\n" +
                "    key2: 'mapValue2';\n" +
                "  };\n" +
                "  namespaceOverride2: {\n" +
                "    key1: 'true';\n" +
                "    key2: 'false';\n" +
                "  };\n" +
                "  namespaceOverride2: {\n" +
                "    key1: '1';\n" +
                "    key2: '2';\n" +
                "  };\n" +
                "}\n");
    }

    @Test
    public void testFileGenerationWithImport()
    {
        test("###FileGeneration\n" +
                "import anything::*;\n" +
                "Avro model::AvroConfig\n" +
                "{\n" +
                "  scopeElements: [model::MyClass, model];\n" +
                "  generationOutputPath: 'myAvroRoot';\n" +
                "  includeNamespace: true;\n" +
                "  propertyProfile: ['model::myProfile', 'model::nextProfile'];\n" +
                "}\n");
    }

    @Test
    public void testQuotedFileGenerationProperties()
    {
        test("###FileGeneration\n" +
                "Avro model::AvroConfig\n" +
                "{\n" +
                "  scopeElements: [model::MyClass, model];\n" +
                "  generationOutputPath: 'myAvroRoot';\n" +
                "  'include Namespace': true;\n" +
                "  'property Profiles': ['model::myProfile', 'model::nextProfile'];\n" +
                "}\n");
    }
}
