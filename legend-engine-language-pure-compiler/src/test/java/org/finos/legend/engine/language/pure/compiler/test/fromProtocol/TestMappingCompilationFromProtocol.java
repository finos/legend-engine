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

package org.finos.legend.engine.language.pure.compiler.test.fromProtocol;

import org.finos.legend.engine.language.pure.compiler.test.TestCompilationFromProtocol;
import org.junit.Test;

public class TestMappingCompilationFromProtocol extends TestCompilationFromProtocol.TestCompilationFromProtocolTestSuite
{
    @Test
    public void testEnumerationMappingWithMixedFormatSourceValues()
    {
        testWithProtocolPath("faultyEnumerationMappingWithMixedFormatSourceValues.json",
                "COMPILATION error: Error in 'meta::sMapping::tests::simpleMapping1': Mixed formats for enum value mapping source values");
    }

    @Test
    public void testResolutionOfAutoImportsWhenNoSectionInfoIsProvided()
    {
        testWithProtocolPath("enumerationWithSystemProfileButNoSection.json");
    }

    @Test
    public void testEnumerationMappingWithNoSourceValueType()
    {
        testWithProtocolPath("enumerationMappingWithNoSourceValueType.json");
        // v1_10_0 we introduced sourceType on enumeration mapping
        testWithProtocolPath("enumerationMappingWithSourceType.json");
    }

    @Test
    public void testEnumerationMappingLoadingWithPackageOffset()
    {
        testProtocolLoadingModelWithPackageOffset("enumerationMappingWithNoSourceValueType.json",null,"update::");
        testProtocolLoadingModelWithPackageOffset("enumerationMappingWithSourceType.json", null, "update::");
    }
}
