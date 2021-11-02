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

import org.junit.Test;

public class TestRelationalMappingGrammarRoundtrip extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{
    @Test
    public void testMappingInheritance()
    {
        // Without extends
        test("###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "  test::Class[id1]: Relational\n" +
                "  {\n" +
                "    prop: 1\n" +
                "  }\n" +
                ")\n");

        // With extends
        test("###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "  test::Class[id1] extends [id2]: Relational\n" +
                "  {\n" +
                "    prop: 1\n" +
                "  }\n" +
                ")\n");
    }

    @Test
    public void testClassMappingFilterWithInnerJoin()
    {
        test("###Mapping\n" +
                "Mapping mappingPackage::myMapping\n" +
                "(\n" +
                "  Person: Relational\n" +
                "  {\n" +
                "    ~filter [mapping::db] (INNER) @Firm_Person | [mapping::db]FirmFilter\n" +
                "    firstName: [db]personTable.firstName\n" +
                "  }\n" +
                ")\n");
    }
}
