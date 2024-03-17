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

public class TestDomainCompilationFromProtocol extends TestCompilationFromProtocol.TestCompilationFromProtocolTestSuite
{
    @Test
    public void testCreatePackageWithReservedName()
    {
        testWithProtocolPath("packageWithReservedName.json",
                "COMPILATION error: Error in 'testing::$implicit::Something': Can't create package with reserved name '$implicit'");
    }

    @Test
    public void testAppliedFunctionWithUnderscore()
    {
        testWithProtocolPath("functionWithUnderscores.json");
    }

    @Test
    public void testCreatePackageWithWrongStrictTimeDomain()
    {
        testWithProtocolPath("packageWithWrongStrictTimeDomain.json",
                "COMPILATION error: Error in 'ui::TestClassSibling': Can't find type 'Stricttime'");
    }

    @Test
    public void testCreatePackageWithCorrectStrictTimeDomain()
    {
        testWithProtocolPath("packageWithCorrectStrictTimeDomain.json");
    }

    @Test
    public void testCompilePathVariable()
    {
        testWithProtocolPath("queryWithPathVariable.json");
    }

    @Test
    public void testCompileLambdaVariable()
    {
        testWithProtocolPath("functionWithLambdaVariable.json");
    }

    @Test
    public void testFunctionWithDateTimeComplication()
    {
        testWithProtocolPath("functionWithDateTime.json");
    }

    @Test
    public void testFunctionWithDateTimeContainsPercentInProtocolComplication()
    {
        testWithProtocolPath("functionWithDateTimeContainingPercent.json");
    }

    @Test
    public void testFunctionLoadingWithPackageOffset()
    {
        testProtocolLoadingModelWithPackageOffset("functionExample.json", null, "update::");
    }

    @Test
    public void testProfileLoadingWithPackageOffset()
    {
        testProtocolLoadingModelWithPackageOffset("profileUsedInClassExample.json", null, "update::");
    }
}
