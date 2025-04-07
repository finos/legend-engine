// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.test.semiStructured;

import org.junit.Assert;
import org.junit.Test;

public class TestSemiStructuredUnionMapping extends AbstractTestSemiStructured
{
    private static final String testMapping = "test::mapping::testMapping";
    private static final String testRuntime = "test::runtime::testRuntime";

    @Test
    public void testSemiStructuredUnionMappingWithBinding()
    {
        String queryFunction = "test::query::getFirmDetails__TabularDataSet_1_";

        String result = this.executeFunction(queryFunction, testMapping, testRuntime);
        Assert.assertEquals("firm_A\n" +
                "firm_B\n" +
                "firm_C\n" +
                "firm_D\n" +
                "firm_E\n" +
                "firm_F\n", result.replace("\r\n", "\n"));
    }

    @Test
    public void testSemiStructuredUnionMappingWithBindingAndFilter()
    {
        String queryFunction = "test::query::getFirmDetailsWithFilter__TabularDataSet_1_";

        String result = this.executeFunction(queryFunction, testMapping, testRuntime);
        Assert.assertEquals("firm_A\n" +
                "firm_B\n" +
                "firm_D\n", result.replace("\r\n", "\n"));
    }

    public String modelResourcePath()
    {
        return "/org/finos/legend/engine/plan/execution/stores/relational/test/semiStructured/semiStructuredUnionMapping.pure";
    }
}
