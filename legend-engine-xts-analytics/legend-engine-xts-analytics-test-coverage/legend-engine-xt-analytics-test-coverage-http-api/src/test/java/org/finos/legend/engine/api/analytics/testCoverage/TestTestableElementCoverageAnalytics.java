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

package org.finos.legend.engine.api.analytics.testCoverage;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.api.analytics.testCoverage.model.TestableElementCoverageResult;
import org.finos.legend.engine.api.analytics.testCoverage.model.TestableElementInfo;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.MappingTestSuite;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Service;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.ServiceTestSuite;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class TestTestableElementCoverageAnalytics
{
    @Test
    public void testServiceWithTestSuites()
    {
        Service service = new Service();
        service.name = "MyService";
        service._package = "model";
        service.testSuites = Collections.singletonList(new ServiceTestSuite());

        List<TestableElementInfo> result = Collections.singletonList(analyzeService(service));

        Assert.assertEquals(1, result.size());
        Assert.assertEquals("model::MyService", result.get(0).path);
        Assert.assertTrue(result.get(0).hasTestSuites);
        Assert.assertFalse(result.get(0).hasLegacyTests);
        Assert.assertEquals(1, result.get(0).testSuiteCount);
    }

    @Test
    public void testServiceWithoutTests()
    {
        Service service = new Service();
        service.name = "MyService";
        service._package = "model";
        service.testSuites = null;
        service.test = null;

        TestableElementInfo info = analyzeService(service);

        Assert.assertFalse(info.hasTestSuites);
        Assert.assertFalse(info.hasLegacyTests);
        Assert.assertEquals(0, info.testSuiteCount);
    }

    @Test
    public void testMappingWithTestSuites()
    {
        Mapping mapping = new Mapping();
        mapping.name = "MyMapping";
        mapping._package = "model";
        mapping.testSuites = Collections.singletonList(new MappingTestSuite());

        TestableElementInfo info = analyzeMapping(mapping);

        Assert.assertTrue(info.hasTestSuites);
        Assert.assertEquals(1, info.testSuiteCount);
        Assert.assertEquals("Mapping", info.type);
    }

    @Test
    public void testMappingWithoutTests()
    {
        Mapping mapping = new Mapping();
        mapping.name = "MyMapping";
        mapping._package = "model";
        mapping.testSuites = null;
        mapping.tests = Collections.emptyList();

        TestableElementInfo info = analyzeMapping(mapping);

        Assert.assertFalse(info.hasTestSuites);
        Assert.assertFalse(info.hasLegacyTests);
        Assert.assertEquals(0, info.testSuiteCount);
    }

    @Test
    public void testCoverageResultSummary()
    {
        TestableElementInfo withTests = new TestableElementInfo("model::A", "Service", true, false, 1);
        TestableElementInfo withoutTests = new TestableElementInfo("model::B", "Service", false, false, 0);

        TestableElementCoverageResult result = new TestableElementCoverageResult(
                Arrays.asList(withTests, withoutTests),
                Collections.emptyList()
        );

        Assert.assertEquals(2, result.serviceCoverage.totalElements);
        Assert.assertEquals(1, result.serviceCoverage.elementsWithTests);
        Assert.assertEquals(0.5, result.serviceCoverage.coveragePercentage, 0.001);
        Assert.assertEquals(0, result.mappingCoverage.totalElements);
        Assert.assertEquals(0.0, result.mappingCoverage.coveragePercentage, 0.001);
    }

    // Mirror the private static methods from the API class for unit testing
    private static TestableElementInfo analyzeService(Service service)
    {
        boolean hasTestSuites = service.testSuites != null && !service.testSuites.isEmpty();
        boolean hasLegacyTests = service.test != null;
        int testSuiteCount = hasTestSuites ? service.testSuites.size() : 0;
        return new TestableElementInfo(service.getPath(), "Service", hasTestSuites, hasLegacyTests, testSuiteCount);
    }

    private static TestableElementInfo analyzeMapping(Mapping mapping)
    {
        boolean hasTestSuites = mapping.testSuites != null && !mapping.testSuites.isEmpty();
        boolean hasLegacyTests = mapping.tests != null && !mapping.tests.isEmpty();
        int testSuiteCount = hasTestSuites ? mapping.testSuites.size() : 0;
        return new TestableElementInfo(mapping.getPath(), "Mapping", hasTestSuites, hasLegacyTests, testSuiteCount);
    }
}

