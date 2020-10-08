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

package org.finos.legend.engine.language.pure.grammar.test.roundtrip;

import org.finos.legend.engine.language.pure.grammar.test.TestGrammarRoundtrip;
import org.junit.Ignore;
import org.junit.Test;

public class TestSectionRoundtrip extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{
    @Test
    public void testSectionIndexKeepingSectionAndElementsInOrder()
    {
        // the ordering of sections is preserved
        test("###Runtime\n" +
                "import a::b::*;\n" +
                "import runtime::a::d::*;\n" +
                "import runtime::a::e::*;\n" +
                "Runtime meta::mySimpleRuntime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "  ];\n" +
                "}\n" +
                "\n\n" +
                "###Pure\n" +
                // the ordering of elements within a section is preserved
                "import pure::a::b::*;\n" +
                "import pure::a::d::*;\n" +
                "import pure::a::e::*;\n" +
                "Association myAssociation\n" +
                "{\n" +
                "  a: String[1];\n" +
                "  b: a::c::A[1];\n" +
                "}\n" +
                "\n" +
                "Enum A\n" +
                "{\n" +
                "}\n" +
                "\n" +
                "Profile meta::pure::profiles::doc\n" +
                "{\n" +
                "  stereotypes: [deprecated];\n" +
                "  tags: [doc, todo];\n" +
                "}\n" +
                "\n" +
                "Measure NewMeasure\n" +
                "{\n" +
                "  *UnitOne: x -> $x;\n" +
                "}\n" +
                "\n" +
                "Class A2\n" +
                "{\n" +
                "}\n" +
                "\n\n" +
                "###Mapping\n" +
                "Mapping meta::pure::mapping::modelToModel::test::union::unionModelMapping\n" +
                "(\n" +
                ")\n" +
                "\n" +
                "Mapping meta::pure::mapping::modelToModel::test::units::unitDecomposeMapping\n" +
                "(\n" +
                ")\n" +
                "\n\n" +
                "###Connection\n" +
                "JsonModelConnection meta::mySimpleConnection\n" +
                "{\n" +
                "  class: model::firm::Person;\n" +
                "  url: 'my_url';\n" +
                "}\n"
        );
    }

    @Test
    public void testSectionWithDuplicatedImports()
    {
        String unformatted = "###Runtime\n" +
                "import a::b::*;\n" +
                "import runtime::a1::*;\n" +
                "import runtime::a::e::*;\n" +
                "import runtime::a1::*;\n" +
                "import runtime::a::e::*;\n" +
                "import runtime::a::e::*;\n" +
                "Runtime meta::mySimpleRuntime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "  ];\n" +
                "}\n" +
                "\n\n";
        testFormat("###Runtime\n" +
                "import a::b::*;\n" +
                "import runtime::a1::*;\n" +
                "import runtime::a::e::*;\n" +
                "Runtime meta::mySimpleRuntime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "  ];\n" +
                "}\n", unformatted);
    }

    @Test
    @Ignore
    // FIXME: re-enable this when we modularize the grammar-transformer completely as right now this will cause problem during round-trip check
    public void testDuplicatedElementsAreIgnored()
    {
        String unformatted = "###Runtime\n" +
                "import a::b::*;\n" +
                "import runtime::a::d::*;\n" +
                "import runtime::a::e::*;\n" +
                "Runtime meta::mySimpleRuntime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "  ];\n" +
                "}\n" +
                "\n\n" +
                "###Pure\n" +
                "import pure::a::b::*;\n" +
                // duplicated elements will follow the rule first one wins
                "Association A\n" +
                "{\n" +
                "  a: String[1];\n" +
                "  b: a::c::A[1];\n" +
                "}\n" +
                "\n" +
                "###Pure\n" +
                "Enum A\n" +
                "{\n" +
                "}\n" +
                "\n" +
                "Profile meta::pure::profiles::doc\n" +
                "{\n" +
                "  stereotypes: [deprecated];\n" +
                "  tags: [doc, todo];\n" +
                "}\n" +
                "\n" +
                "Measure NewMeasure\n" +
                "{\n" +
                "  *UnitOne: x -> $x;\n" +
                "}\n" +
                "\n" +
                "Class A2\n" +
                "{\n" +
                "}\n" +
                "\n\n" +
                "###Mapping\n" +
                // NOTE: since this has the same name as elements from previous section, i.e. Enum A
                // this will get overridden, but since enumeration should not be rendered in Mapping block
                // we just remove it altogether as it will cause parsing error
                "Mapping A\n" +
                "{\n" +
                "}\n";
        testFormat("###Runtime\n" +
                "import a::b::*;\n" +
                "import runtime::a::d::*;\n" +
                "import runtime::a::e::*;\n" +
                "Runtime meta::mySimpleRuntime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "  ];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Pure\n" +
                "import pure::a::b::*;\n" +
                "Enum A\n" +
                "{\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Pure\n" +
                "Enum A\n" +
                "{\n" +
                "}\n" +
                "\n" +
                "Profile meta::pure::profiles::doc\n" +
                "{\n" +
                "  stereotypes: [deprecated];\n" +
                "  tags: [doc, todo];\n" +
                "}\n" +
                "\n" +
                "Measure NewMeasure\n" +
                "{\n" +
                "  *UnitOne: x -> $x;\n" +
                "}\n" +
                "\n" +
                "Class A2\n" +
                "{\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Mapping\n" +
                "Enum A\n" +
                "{\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Pure\n" +
                "Association A\n" +
                "{\n" +
                "  a: String[1];\n" +
                "  b: a::c::A[1];\n" +
                "}\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping A\n" +
                "{\n" +
                "}", unformatted);
        testFormat("Enum A\n" +
                "{\n" +
                "}\n", "Enum A\n" +
                "{\n" +
                "}\n" +
                "Enum A\n" +
                "{\n" +
                "}\n" +
                "Enum A\n" +
                "{\n" +
                "}\n" +
                "\n" +
                "Enum A\n" +
                "{\n" +
                "}\n" +
                "\n" +
                "\n");
    }
}
