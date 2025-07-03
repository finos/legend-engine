// Copyright 2025 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.extension.functions.standard.interpreted;

import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.finos.legend.pure.m3.pct.shared.provider.PCTReportProviderLoader;
import org.junit.Assert;
import org.junit.Test;

public class TestPCTReportStandardFunctionsInterpreted
{
    @Test
    public void canFindPCTReport()
    {
        Assert.assertEquals("Native", PCTReportProviderLoader.gatherReports().collect(c -> c.adapterKey.adapter.name).distinct().sortThis().makeString(", "));
        System.out.println(PCTReportProviderLoader.gatherReports().collect(c -> c.reportScope.module));
        ImmutableSet expectedReports = Sets.immutable.of("essential", "grammar", "unclassified", "relation", "standard", "variant");
        Assert.assertEquals(expectedReports, PCTReportProviderLoader.gatherReports().collect(c -> c.reportScope.module).toSet());
    }
}
