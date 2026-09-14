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

package org.finos.legend.engine.api.analytics.testCoverage.model;

import java.util.List;

public class TestableElementCoverageResult
{
    public List<TestableElementInfo> services;
    public List<TestableElementInfo> mappings;
    public CoverageSummary serviceCoverage;
    public CoverageSummary mappingCoverage;

    public TestableElementCoverageResult()
    {
    }

    public TestableElementCoverageResult(List<TestableElementInfo> services, List<TestableElementInfo> mappings)
    {
        this.services = services;
        this.mappings = mappings;
        this.serviceCoverage = computeSummary(services);
        this.mappingCoverage = computeSummary(mappings);
    }

    private static CoverageSummary computeSummary(List<TestableElementInfo> elements)
    {
        int total = elements.size();
        long withTests = elements.stream().filter(e -> e.hasTestSuites || e.hasLegacyTests).count();
        return new CoverageSummary(total, (int) withTests, total > 0 ? (double) withTests / total : 0.0);
    }

    public static class CoverageSummary
    {
        public int totalElements;
        public int elementsWithTests;
        public double coveragePercentage;

        public CoverageSummary()
        {
        }

        public CoverageSummary(int totalElements, int elementsWithTests, double coveragePercentage)
        {
            this.totalElements = totalElements;
            this.elementsWithTests = elementsWithTests;
            this.coveragePercentage = coveragePercentage;
        }
    }
}

