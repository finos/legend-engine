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

public class TestDataSpaceGrammarRoundtrip extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{
    @Test
    public void testDataSpace()
    {
        test("###DataSpace\n" +
            "DataSpace <<meta::pure::profiles::typemodifiers.abstract>> {doc.doc = 'bla'} model::dataSpace\n" +
            "{\n" +
            "  groupId: 'test.group';\n" +
            "  artifactId: 'test-data-space';\n" +
            "  versionId: '1.0.0';\n" +
            "  mapping: model::String;\n" +
            "  runtime: model::Runtime;\n" +
            "  description: 'some description';\n" +
            "  diagrams: [\n" +
            "    model::Diagram\n" +
            "  ];\n" +
            "  supportEmail: 'someEmail@test.org';\n" +
            "}\n");

        test("###DataSpace\n" +
            "DataSpace model::dataSpace\n" +
            "{\n" +
            "  groupId: 'test.group';\n" +
            "  artifactId: 'test-data-space';\n" +
            "  versionId: '1.0.0';\n" +
            "  mapping: model::Mapping;\n" +
            "  runtime: model::Runtime;\n" +
            "}\n");
    }
}
