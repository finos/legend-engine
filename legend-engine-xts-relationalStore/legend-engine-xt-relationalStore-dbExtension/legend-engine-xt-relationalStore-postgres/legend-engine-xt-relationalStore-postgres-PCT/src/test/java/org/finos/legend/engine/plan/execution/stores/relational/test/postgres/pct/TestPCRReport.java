// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.test.postgres.pct;

import org.finos.legend.pure.m3.pct.config.PCTReport;
import org.junit.Assert;
import org.junit.Test;

public class TestPCRReport
{
    @Test
    public void canFindPCTReport()
    {
        Assert.assertEquals("meta::pure::test::pct::testAdapterForInMemoryExecution_Function_1__X_o_, meta::relational::tests::pct::testAdapterForRelationalWithPostgresExecution_Function_1__X_o_", PCTReport.gatherReports().collect(c -> c.adapter).distinct().sortThis().makeString(", "));
        Assert.assertEquals(8, PCTReport.gatherReports().size());
    }
}
