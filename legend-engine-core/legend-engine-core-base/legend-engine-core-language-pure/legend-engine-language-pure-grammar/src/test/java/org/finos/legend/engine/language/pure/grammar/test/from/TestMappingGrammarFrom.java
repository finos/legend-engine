//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.language.pure.grammar.test.from;

import org.finos.legend.engine.language.pure.grammar.test.TestGrammarRoundtrip;
import org.junit.Test;

public class TestMappingGrammarFrom extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{
    @Test
    public void testSimpleModelToModelMapping()
    {
        testFrom("###Mapping\n" +
                "Mapping mapping::simpleModelMapping\n" +
                "(\n" +
                "  *model::TargetClass[my_mapping_id]: Pure\n" +
                "  {\n" +
                "    ~src model::SourceClass\n" +
                "    name: $src.name\n" +
                "  }\n" +
                ")\n", "SimpleM2MMapping.json");
    }

    @Test
    public void testIncludeDispatch()
    {
        testFrom("###Mapping\n" +
                        "Mapping mapping::simpleModelMapping\n" +
                        "(\n" +
                        "  include mapping test::mapping::DispatchMapping\n" +
                        "  include mapping test::mapping::Mapping\n" +
                        "  *model::TargetClass[my_mapping_id]: Pure\n" +
                        "  {\n" +
                        "    ~src model::SourceClass\n" +
                        "    name: $src.name\n" +
                        "  }\n" +
                        ")\n", "simpleIncludeMapping.json");
    }

}
