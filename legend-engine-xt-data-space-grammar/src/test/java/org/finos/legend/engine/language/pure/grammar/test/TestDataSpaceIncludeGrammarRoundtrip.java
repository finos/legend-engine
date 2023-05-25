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

package org.finos.legend.engine.language.pure.grammar.test;

import org.junit.Test;

public class TestDataSpaceIncludeGrammarRoundtrip extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{
    @Test
    public void testIncludeDispatch()
    {
        test("###Mapping\n" +
                "Mapping meta::pure::mapping::modelToModel::test::simple::simpleModelMapping\n" +
                "(\n" +
                "  include mapping meta::pure::mapping::includedMapping\n" +
                "  include mapping meta::pure::mapping::DispatchMapping\n" +
                "  include dataspace meta::pure::dataspace::IncludedDataSpace\n" +
                "\n" +
                "  *meta::pure::mapping::modelToModel::test::shared::dest::Person[meta_pure_mapping_modelToModel_test_shared_dest_Person]: Pure\n" +
                "  {\n" +
                "    ~src meta::pure::mapping::modelToModel::test::shared::src::_S_Person\n" +
                "    firstName: $src.fullName->substring(0, $src.fullName->indexOf(' ')),\n" +
                "    lastName: $src.fullName->substring($src.fullName->indexOf(' ') + 1, $src.fullName->length()),\n" +
                "    testing: if($src.fullName == 'johndoe', |if($src.lastName == 'good', |'true', |'maybe'), |'false')\n" +
                "  }\n" +
                "  *meta::pure::mapping::modelToModel::test::shared::dest::Product2Simple[meta_pure_mapping_modelToModel_test_shared_dest_Product2Simple]: Pure\n" +
                "  {\n" +
                "    ~src meta::pure::mapping::modelToModel::test::shared::src::_Product2\n" +
                "    ~filter if($src.fullName == 'johndoe', |if($src.lastName == 'good', |true, |true), |false)\n" +
                "    name: $src.name,\n" +
                "    region: $src.region\n" +
                "  }\n" +
                ")\n");
    }
}
