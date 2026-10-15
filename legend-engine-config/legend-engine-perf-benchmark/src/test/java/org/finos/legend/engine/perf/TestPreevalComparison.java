// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.perf;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestPreevalComparison
{
    @Test
    public void testFasterJavaIsNotARegression()
    {
        Assertions.assertEquals(Collections.emptyList(), PreevalComparison.regressions(Collections.singletonList(workload("w", 100, 40.0, 80, 10.0)), 0.10, 10));
    }

    @Test
    public void testSlowerPlanPureBeyondBothThresholdsIsARegression()
    {
        List<String> found = PreevalComparison.regressions(Collections.singletonList(workload("w", 100, 10.0, 125, 10.0)), 0.10, 10);
        Assertions.assertEquals(1, found.size(), found.toString());
        Assertions.assertTrue(found.get(0).contains("w") && found.get(0).contains("planPure"), found.get(0));
    }

    @Test
    public void testSlowerPrevalBeyondBothThresholdsIsARegression()
    {
        List<String> found = PreevalComparison.regressions(Collections.singletonList(workload("w", 100, 20.0, 100, 35.0)), 0.10, 10);
        Assertions.assertEquals(1, found.size(), found.toString());
        Assertions.assertTrue(found.get(0).contains("preval"), found.get(0));
    }

    @Test
    public void testSmallAbsoluteDeltaIsNoise()
    {
        Assertions.assertEquals(Collections.emptyList(), PreevalComparison.regressions(Collections.singletonList(workload("w", 20, 2.0, 28, 9.0)), 0.10, 10));
    }

    @Test
    public void testSmallRelativeDeltaIsNoise()
    {
        Assertions.assertEquals(Collections.emptyList(), PreevalComparison.regressions(Collections.singletonList(workload("w", 1000, 10.0, 1050, 10.0)), 0.10, 10));
    }

    @Test
    public void testEveryRegressedWorkloadIsReported()
    {
        List<String> found = PreevalComparison.regressions(Arrays.asList(workload("a", 100, 10.0, 200, 10.0), workload("b", 100, 10.0, 200, 10.0)), 0.10, 10);
        Assertions.assertEquals(2, found.size(), found.toString());
    }

    @Test
    public void testCheckSpansRejectsZeroSpans()
    {
        IllegalStateException e = Assertions.assertThrows(IllegalStateException.class, () -> PreevalComparison.checkSpans("w", 0, 0));
        Assertions.assertTrue(e.getMessage().contains("no preval spans"), e.getMessage());
    }

    @Test
    public void testCheckSpansRejectsUnequalCounts()
    {
        IllegalStateException e = Assertions.assertThrows(IllegalStateException.class, () -> PreevalComparison.checkSpans("w", 3, 4));
        Assertions.assertTrue(e.getMessage().contains("PURE 3") && e.getMessage().contains("JAVA 4"), e.getMessage());
    }

    @Test
    public void testCheckSpansAcceptsEqualPositiveCounts()
    {
        PreevalComparison.checkSpans("w", 3, 3);
    }

    private static Map<String, Object> workload(String id, long purePlan, double purePreval, long javaPlan, double javaPreval)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("workload", id);
        result.put("PURE", side(purePlan, purePreval));
        result.put("JAVA", side(javaPlan, javaPreval));
        return result;
    }

    private static Map<String, Object> side(long planPureMs, double prevalMs)
    {
        Map<String, Object> side = new LinkedHashMap<>();
        side.put("planPureMs", planPureMs);
        side.put("prevalMs", prevalMs);
        side.put("prevalSpans", 1L);
        return side;
    }
}
