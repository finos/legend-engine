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

package org.finos.legend.engine.testable.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.MappingTestSuite;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Service;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.ServiceTestSuite;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.testable.api.model.TestableElementCoverageResult;
import org.finos.legend.engine.testable.api.model.TestableElementInfo;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class TestTestableElementCoverageApi
{
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final ObjectMapper protocolObjectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    // ──────────────────────────────────────────────────────────────────────────
    // Unit tests – protocol objects built directly
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    public void testServiceWithTestSuites()
    {
        Service service = new Service();
        service.name = "MyService";
        service._package = "model";
        service.testSuites = Collections.singletonList(new ServiceTestSuite());

        TestableElementInfo info = analyzeService(service);

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

        Assert.assertEquals("model::MyMapping", info.path);
        Assert.assertEquals("Mapping", info.type);
        Assert.assertTrue(info.hasTestSuites);
        Assert.assertEquals(1, info.testSuiteCount);
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

    // ──────────────────────────────────────────────────────────────────────────
    // Coverage result serialization tests – compared against golden JSON files
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    public void testServiceWithTestSuitesCoverageResult() throws IOException
    {
        Service service = new Service();
        service.name = "MyService";
        service._package = "model";
        service.testSuites = Collections.singletonList(new ServiceTestSuite());

        TestableElementCoverageResult result = new TestableElementCoverageResult(
                Collections.singletonList(analyzeService(service)),
                Collections.emptyList()
        );

        assertJsonMatchesResource(result, "coverage/expected_service_with_test_suites.json");
    }

    @Test
    public void testServiceWithoutTestsCoverageResult() throws IOException
    {
        Service service = new Service();
        service.name = "MyService";
        service._package = "model";
        service.testSuites = null;
        service.test = null;

        TestableElementCoverageResult result = new TestableElementCoverageResult(
                Collections.singletonList(analyzeService(service)),
                Collections.emptyList()
        );

        assertJsonMatchesResource(result, "coverage/expected_service_without_tests.json");
    }

    @Test
    public void testMappingWithTestSuitesCoverageResult() throws IOException
    {
        Mapping mapping = new Mapping();
        mapping.name = "MyMapping";
        mapping._package = "model";
        mapping.testSuites = Collections.singletonList(new MappingTestSuite());

        TestableElementCoverageResult result = new TestableElementCoverageResult(
                Collections.emptyList(),
                Collections.singletonList(analyzeMapping(mapping))
        );

        assertJsonMatchesResource(result, "coverage/expected_mapping_with_test_suites.json");
    }

    @Test
    public void testMixedCoverageResult() throws IOException
    {
        Service serviceA = new Service();
        serviceA.name = "ServiceA";
        serviceA._package = "model";
        serviceA.testSuites = Arrays.asList(new ServiceTestSuite(), new ServiceTestSuite());

        Service serviceB = new Service();
        serviceB.name = "ServiceB";
        serviceB._package = "model";
        serviceB.testSuites = null;
        serviceB.test = null;

        Mapping mappingA = new Mapping();
        mappingA.name = "MappingA";
        mappingA._package = "model";
        mappingA.testSuites = Collections.singletonList(new MappingTestSuite());

        Mapping mappingB = new Mapping();
        mappingB.name = "MappingB";
        mappingB._package = "model";
        mappingB.testSuites = null;
        mappingB.tests = Collections.emptyList();

        TestableElementCoverageResult result = new TestableElementCoverageResult(
                Arrays.asList(analyzeService(serviceA), analyzeService(serviceB)),
                Arrays.asList(analyzeMapping(mappingA), analyzeMapping(mappingB))
        );

        assertJsonMatchesResource(result, "coverage/expected_mixed_coverage.json");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Grammar → protocol JSON round-trip tests
    // Parses Pure grammar, verifies testSuites/tests survive in protocol JSON,
    // then runs coverage analysis and compares against golden JSON files.
    // ──────────────────────────────────────────────────────────────────────────

    private static final String SERVICE_WITH_TEST_SUITE_GRAMMAR =
            "###Service\n" +
            "Service meta::pure::myServiceSingle\n" +
            "{\n" +
            "  pattern: 'url/myUrl/';\n" +
            "  owners:\n" +
            "  [\n" +
            "    'ownerName'\n" +
            "  ];\n" +
            "  documentation: 'service with testSuites';\n" +
            "  autoActivateUpdates: true;\n" +
            "  execution: Single\n" +
            "  {\n" +
            "    query: |demo::Person.all()->graphFetch(#{demo::Person{name}}#)->serialize(#{demo::Person{name}}#);\n" +
            "    mapping: meta::myMapping;\n" +
            "    runtime: meta::myRuntime;\n" +
            "  }\n" +
            "  testSuites:\n" +
            "  [\n" +
            "    testSuite1:\n" +
            "    {\n" +
            "      tests:\n" +
            "      [\n" +
            "        test1:\n" +
            "        {\n" +
            "          asserts:\n" +
            "          [\n" +
            "            assert1:\n" +
            "              EqualToJson\n" +
            "              #{\n" +
            "                expected:\n" +
            "                  ExternalFormat\n" +
            "                  #{\n" +
            "                    contentType: 'application/json';\n" +
            "                    data: '[{\"name\":\"dummy\"}]';\n" +
            "                  }#;\n" +
            "              }#\n" +
            "          ]\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}\n";

    private static final String SERVICE_WITH_LEGACY_TEST_GRAMMAR =
            "###Service\n" +
            "Service meta::pure::myServiceSingle\n" +
            "{\n" +
            "  pattern: 'url/myUrl/';\n" +
            "  owners:\n" +
            "  [\n" +
            "    'ownerName'\n" +
            "  ];\n" +
            "  documentation: 'service with legacy test';\n" +
            "  autoActivateUpdates: true;\n" +
            "  execution: Single\n" +
            "  {\n" +
            "    query: src: meta::transform::tests::Address[1]|$src.a;\n" +
            "    mapping: meta::myMapping;\n" +
            "    runtime: meta::myRuntime;\n" +
            "  }\n" +
            "  test: Single\n" +
            "  {\n" +
            "    data: 'moreThanData';\n" +
            "    asserts:\n" +
            "    [\n" +
            "      { [], res: Result<Any|*>[1]|$res.values->cast(@TabularDataSet).rows->size() == 1 }\n" +
            "    ];\n" +
            "  }\n" +
            "}\n";

    private static final String SERVICE_WITH_EMPTY_TEST_SUITES_GRAMMAR =
            "###Service\n" +
            "Service meta::pure::myServiceSingle\n" +
            "{\n" +
            "  pattern: 'url/myUrl/';\n" +
            "  documentation: 'service with empty testSuites';\n" +
            "  autoActivateUpdates: true;\n" +
            "  execution: Single\n" +
            "  {\n" +
            "    query: src: meta::transform::tests::Address[1]|$src.a->from(meta::myMapping, meta::myRuntime);\n" +
            "  }\n" +
            "  testSuites:\n" +
            "  [\n" +
            "\n" +
            "  ]\n" +
            "}\n";

    private static final String MIXED_SERVICES_GRAMMAR =
            "###Service\n" +
            "Service meta::pure::testedService\n" +
            "{\n" +
            "  pattern: 'url/tested/';\n" +
            "  documentation: 'tested service';\n" +
            "  autoActivateUpdates: true;\n" +
            "  execution: Single\n" +
            "  {\n" +
            "    query: |demo::Person.all()->graphFetch(#{demo::Person{name}}#)->serialize(#{demo::Person{name}}#);\n" +
            "    mapping: meta::myMapping;\n" +
            "    runtime: meta::myRuntime;\n" +
            "  }\n" +
            "  testSuites:\n" +
            "  [\n" +
            "    testSuite1:\n" +
            "    {\n" +
            "      tests:\n" +
            "      [\n" +
            "        test1:\n" +
            "        {\n" +
            "          asserts:\n" +
            "          [\n" +
            "            assert1:\n" +
            "              EqualToJson\n" +
            "              #{\n" +
            "                expected:\n" +
            "                  ExternalFormat\n" +
            "                  #{\n" +
            "                    contentType: 'application/json';\n" +
            "                    data: '[]';\n" +
            "                  }#;\n" +
            "              }#\n" +
            "          ]\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}\n" +
            "\n" +
            "Service meta::pure::untestedService\n" +
            "{\n" +
            "  pattern: 'url/untested/';\n" +
            "  documentation: 'untested service';\n" +
            "  autoActivateUpdates: true;\n" +
            "  execution: Single\n" +
            "  {\n" +
            "    query: |demo::Person.all()->graphFetch(#{demo::Person{name}}#)->serialize(#{demo::Person{name}}#);\n" +
            "    mapping: meta::myMapping;\n" +
            "    runtime: meta::myRuntime;\n" +
            "  }\n" +
            "  testSuites:\n" +
            "  [\n" +
            "\n" +
            "  ]\n" +
            "}\n";

    @Test
    public void testSuitesFieldSurvivesGrammarToProtocolJson() throws IOException
    {
        PureModelContextData modelData = PureGrammarParser.newInstance().parseModel(SERVICE_WITH_TEST_SUITE_GRAMMAR);
        String json = protocolObjectMapper.writeValueAsString(modelData);
        JsonNode root = protocolObjectMapper.readTree(json);

        JsonNode serviceNode = findFirstElementByType(root, "service");
        Assert.assertNotNull("A service element should be present", serviceNode);

        // Verify testSuites field is present and has the right structure
        JsonNode testSuites = serviceNode.get("testSuites");
        Assert.assertNotNull("testSuites should be present in the serialized service JSON", testSuites);
        Assert.assertTrue("testSuites should be an array", testSuites.isArray());
        Assert.assertEquals("testSuites should contain 1 suite", 1, testSuites.size());

        JsonNode suite = testSuites.get(0);
        Assert.assertEquals("serviceTestSuite", suite.get("_type").asText());
        Assert.assertEquals("testSuite1", suite.get("id").asText());

        // Verify tests within the suite
        JsonNode tests = suite.get("tests");
        Assert.assertNotNull("tests should be present inside the test suite", tests);
        Assert.assertTrue("tests should be an array", tests.isArray());
        Assert.assertEquals(1, tests.size());

        JsonNode test = tests.get(0);
        Assert.assertEquals("serviceTest", test.get("_type").asText());
        Assert.assertEquals("test1", test.get("id").asText());

        // Verify assertions within the test
        JsonNode assertions = test.get("assertions");
        Assert.assertNotNull("assertions should be present inside the test", assertions);
        Assert.assertEquals(1, assertions.size());
        Assert.assertEquals("equalToJson", assertions.get(0).get("_type").asText());
        Assert.assertEquals("assert1", assertions.get(0).get("id").asText());

        JsonNode expected = assertions.get(0).get("expected");
        Assert.assertNotNull(expected);
        Assert.assertEquals("externalFormat", expected.get("_type").asText());
        Assert.assertEquals("application/json", expected.get("contentType").asText());
    }

    @Test
    public void testCoverageFromGrammarServiceWithTestSuite() throws IOException
    {
        PureModelContextData modelData = PureGrammarParser.newInstance().parseModel(SERVICE_WITH_TEST_SUITE_GRAMMAR);
        TestableElementCoverageResult result = buildCoverageResult(modelData);
        assertJsonMatchesResource(result, "coverage/expected_grammar_service_with_test_suite.json");
    }

    @Test
    public void testLegacyTestFieldSurvivesGrammarToProtocolJson() throws IOException
    {
        PureModelContextData modelData = PureGrammarParser.newInstance().parseModel(SERVICE_WITH_LEGACY_TEST_GRAMMAR);
        String json = protocolObjectMapper.writeValueAsString(modelData);
        JsonNode root = protocolObjectMapper.readTree(json);

        JsonNode serviceNode = findFirstElementByType(root, "service");
        Assert.assertNotNull("A service element should be present", serviceNode);

        // Legacy test uses the "test" field (not testSuites)
        JsonNode testField = serviceNode.get("test");
        Assert.assertNotNull("legacy test field should be present in the serialized service JSON", testField);
        Assert.assertEquals("singleExecutionTest", testField.get("_type").asText());

        // testSuites should be absent or null
        JsonNode testSuitesNode = serviceNode.get("testSuites");
        Assert.assertTrue("testSuites should be absent for legacy-test service",
                testSuitesNode == null || testSuitesNode.isNull());
    }

    @Test
    public void testCoverageFromGrammarServiceWithLegacyTest() throws IOException
    {
        PureModelContextData modelData = PureGrammarParser.newInstance().parseModel(SERVICE_WITH_LEGACY_TEST_GRAMMAR);
        TestableElementCoverageResult result = buildCoverageResult(modelData);
        assertJsonMatchesResource(result, "coverage/expected_grammar_service_with_legacy_test.json");
    }

    @Test
    public void testEmptyTestSuitesFieldSurvivesGrammarToProtocolJson() throws IOException
    {
        PureModelContextData modelData = PureGrammarParser.newInstance().parseModel(SERVICE_WITH_EMPTY_TEST_SUITES_GRAMMAR);
        String json = protocolObjectMapper.writeValueAsString(modelData);
        JsonNode root = protocolObjectMapper.readTree(json);

        JsonNode serviceNode = findFirstElementByType(root, "service");
        Assert.assertNotNull("A service element should be present", serviceNode);

        JsonNode testSuites = serviceNode.get("testSuites");
        Assert.assertNotNull("testSuites field should be present even when empty", testSuites);
        Assert.assertTrue("testSuites should be an array", testSuites.isArray());
        Assert.assertEquals("testSuites should be empty", 0, testSuites.size());
    }

    @Test
    public void testCoverageFromGrammarServiceWithEmptyTestSuites() throws IOException
    {
        PureModelContextData modelData = PureGrammarParser.newInstance().parseModel(SERVICE_WITH_EMPTY_TEST_SUITES_GRAMMAR);
        TestableElementCoverageResult result = buildCoverageResult(modelData);
        assertJsonMatchesResource(result, "coverage/expected_grammar_service_with_empty_test_suites.json");
    }

    @Test
    public void testMixedServicesCoverageFromGrammar() throws IOException
    {
        PureModelContextData modelData = PureGrammarParser.newInstance().parseModel(MIXED_SERVICES_GRAMMAR);
        String json = protocolObjectMapper.writeValueAsString(modelData);
        JsonNode root = protocolObjectMapper.readTree(json);

        // Verify both services are present with correct testSuites structure
        JsonNode elements = root.get("elements");
        int serviceCount = 0;
        boolean foundTestedWithSuites = false;
        boolean foundUntestedEmpty = false;
        for (JsonNode el : elements)
        {
            if ("service".equals(el.path("_type").asText()))
            {
                serviceCount++;
                String name = el.path("name").asText();
                JsonNode suites = el.get("testSuites");
                if ("testedService".equals(name))
                {
                    Assert.assertNotNull(suites);
                    Assert.assertEquals(1, suites.size());
                    foundTestedWithSuites = true;
                }
                else if ("untestedService".equals(name))
                {
                    Assert.assertNotNull(suites);
                    Assert.assertEquals(0, suites.size());
                    foundUntestedEmpty = true;
                }
            }
        }
        Assert.assertEquals("Should find 2 services", 2, serviceCount);
        Assert.assertTrue("testedService should have non-empty testSuites", foundTestedWithSuites);
        Assert.assertTrue("untestedService should have empty testSuites", foundUntestedEmpty);

        // Verify coverage result matches golden JSON
        TestableElementCoverageResult result = buildCoverageResult(modelData);
        assertJsonMatchesResource(result, "coverage/expected_grammar_mixed_services.json");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private static TestableElementCoverageResult buildCoverageResult(PureModelContextData modelData)
    {
        List<TestableElementInfo> services = modelData.getElements().stream()
                .filter(Service.class::isInstance)
                .map(Service.class::cast)
                .map(TestTestableElementCoverageApi::analyzeService)
                .collect(Collectors.toList());

        List<TestableElementInfo> mappings = modelData.getElements().stream()
                .filter(Mapping.class::isInstance)
                .map(Mapping.class::cast)
                .map(TestTestableElementCoverageApi::analyzeMapping)
                .collect(Collectors.toList());

        return new TestableElementCoverageResult(services, mappings);
    }

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

    private static JsonNode findFirstElementByType(JsonNode root, String type)
    {
        JsonNode elements = root.get("elements");
        if (elements != null)
        {
            for (JsonNode el : elements)
            {
                if (type.equals(el.path("_type").asText()))
                {
                    return el;
                }
            }
        }
        return null;
    }

    private void assertJsonMatchesResource(TestableElementCoverageResult result, String resourcePath) throws IOException
    {
        JsonNode actualJson = objectMapper.valueToTree(result);
        try (InputStream is = Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream(resourcePath),
                "Can't find resource '" + resourcePath + "'"))
        {
            JsonNode expectedJson = objectMapper.readTree(is);
            Assert.assertEquals("Coverage result JSON should match golden source at " + resourcePath,
                    expectedJson, actualJson);
        }
    }
}
