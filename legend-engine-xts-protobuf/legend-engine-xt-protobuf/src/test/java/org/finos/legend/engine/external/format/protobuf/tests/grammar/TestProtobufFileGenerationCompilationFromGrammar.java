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

package org.finos.legend.engine.external.format.protobuf.tests.grammar;

import org.finos.legend.engine.language.pure.compiler.test.TestCompilationFromGrammar;
import org.junit.Test;

public class TestProtobufFileGenerationCompilationFromGrammar extends TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite
{
    @Override
    public String getDuplicatedElementTestCode()
    {
        return "Class anything::class {}\n" +
                "###Mapping\n" +
                "Mapping anything::somethingelse ()\n" +
                "###FileGeneration\n" +
                "Protobuf anything::somethingelse\n" +
                "{\n" +
                "  scopeElements: [_meta];\n" +
                "}\n";
    }

    @Override
    public String getDuplicatedElementTestExpectedErrorMessage()
    {
        return "COMPILATION error at [5:1-8:1]: Duplicated element 'anything::somethingelse'";
    }

    @Test
    public void testFileGeneration()
    {
        test("###FileGeneration\n" +
                "Protobuf _meta::myProtbuf\n" +
                "{\n" +
                "  scopeElements: [_meta];\n" +
                "}\n"
                );
    }

    @Test
    public void testFileGenerationWithImport()
    {
        test("Class anything::oclass {}\n" +
                "Class anything::olp::don {}\n" +
                "###FileGeneration\n" +
                "import anything::*;\n" +
                "Protobuf model::AvroConfig\n" +
                "{\n" +
                // scope element
                "  scopeElements: [oclass, anything::olp];\n" +
                "  generationOutputPath: 'myAvroRoot';\n" +
                "  includeNamespace: true;\n" +
                "  propertyProfile: ['model::myProfile', 'model::nextProfile'];\n" +
                "}\n");
    }
}
