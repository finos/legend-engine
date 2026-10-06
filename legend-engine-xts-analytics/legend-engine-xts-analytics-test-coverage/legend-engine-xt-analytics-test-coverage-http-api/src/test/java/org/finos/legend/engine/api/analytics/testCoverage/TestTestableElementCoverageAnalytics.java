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

import org.finos.legend.engine.api.analytics.testCoverage.model.TestableElementCoverageResult;
import org.finos.legend.engine.api.analytics.testCoverage.model.TestableElementInfo;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.MappingTestSuite;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Service;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.ServiceTestSuite;
import org.finos.legend.engine.testable.api.TestableElementCoverageAnalyzer;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TestTestableElementCoverageAnalytics
{
    @Test
    public void testServiceWithTestSuites()
    {
        Service service = new Service();
        service.name = "MyService";
        service._package = "model";
        service.testSuites = Collections.singletonList(new ServiceTestSuite());

        TestableElementInfo info = analyzeSingle(service, "Service");

        Assert.assertEquals("model::MyService", info.path);
        Assert.assertEquals("Service", info.type);
        Assert.assertTrue(info.hasTestSuites);
        Assert.assertFalse(info.hasLegacyTests);
        Assert.assertEquals(1, info.testSuiteCount);
    }

    @Test
    public void testServiceWithoutTests()
    {
        Service service = new Service();
        service.name = "MyService";
        service._package = "model";
        service.testSuites = null;
        service.test = null;

        TestableElementInfo info = analyzeSingle(service, "Service");

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

        TestableElementInfo info = analyzeSingle(mapping, "Mapping");

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

        TestableElementInfo info = analyzeSingle(mapping, "Mapping");

        Assert.assertFalse(info.hasTestSuites);
        Assert.assertFalse(info.hasLegacyTests);
        Assert.assertEquals(0, info.testSuiteCount);
    }

    @Test
    public void testCoverageResultSummary()
    {
        TestableElementInfo withTests = new TestableElementInfo("model::A", "Service", true, false, 1);
        TestableElementInfo withoutTests = new TestableElementInfo("model::B", "Service", false, false, 0);

        Map<String, List<TestableElementInfo>> elementsByType = new LinkedHashMap<>();
        elementsByType.put("Service", Arrays.asList(withTests, withoutTests));

        TestableElementCoverageResult result = new TestableElementCoverageResult(elementsByType);

        Assert.assertEquals(2, result.coverageByType.get("Service").totalElements);
        Assert.assertEquals(1, result.coverageByType.get("Service").elementsWithTests);
        Assert.assertEquals(0.5, result.coverageByType.get("Service").coveragePercentage, 0.001);
    }

    private static TestableElementInfo analyzeSingle(PackageableElement element, String expectedType)
    {
        org.finos.legend.engine.testable.api.model.TestableElementCoverageResult analyzed =
                TestableElementCoverageAnalyzer.analyze(PureModelContextData.newPureModelContextData(null, null, Arrays.asList(element)));
        TestableElementCoverageResult result = TestableElementCoverageAnalytics.toLocalResult(analyzed);
        List<TestableElementInfo> elements = result.elementsByType.get(expectedType);
        Assert.assertNotNull("Expected a '" + expectedType + "' bucket in the coverage result", elements);
        Assert.assertEquals(1, elements.size());
        return elements.get(0);
    }
}

