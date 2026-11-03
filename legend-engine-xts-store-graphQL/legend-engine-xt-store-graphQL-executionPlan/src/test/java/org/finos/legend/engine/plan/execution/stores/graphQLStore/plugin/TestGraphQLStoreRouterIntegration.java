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

package org.finos.legend.engine.plan.execution.stores.graphQLStore.plugin;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.finos.legend.engine.plan.execution.stores.graphQLStore.utils.GraphQLStoreTestUtils;
import org.finos.legend.engine.protocol.graphQLStore.executionPlan.GraphQLExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.externalFormat.ExternalFormatInternalizeExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.store.inMemory.InMemoryRootGraphFetchExecutionNode;
import org.finos.legend.engine.shared.core.port.DynamicPortGenerator;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

// PlanPlatform.JAVA tolerates GraphQLExecutionNode having no registered java-platform-binding codegen descriptor.
public class TestGraphQLStoreRouterIntegration
{
    private WireMockServer wireMockServer;
    private String grammar;

    private static final String MODEL_AND_BINDING =
            "###Pure\n" +
            "Class test::graphql::router::Employee\n" +
            "{\n" +
            "  firstName: String[1];\n" +
            "  lastName: String[1];\n" +
            "}\n" +
            "###ExternalFormat\n" +
            "SchemaSet test::graphql::router::TestSchema\n" +
            "{\n" +
            "  format: JSON;\n" +
            "  schemas: [\n" +
            "    {\n" +
            "      location: 'test/graphql/router/Employee.json';\n" +
            "      content: '{\"$schema\": \"http://json-schema.org/draft-07/schema#\", \"title\": \"test::graphql::router::Employee\", \"type\": \"object\", \"properties\": {\"firstName\": {\"type\": \"string\"}, \"lastName\": {\"type\": \"string\"}}, \"required\": [\"firstName\", \"lastName\"]}';\n" +
            "    }\n" +
            "  ];\n" +
            "}\n" +
            "\n" +
            "Binding test::graphql::router::TestBinding\n" +
            "{\n" +
            "  schemaSet: test::graphql::router::TestSchema;\n" +
            "  contentType: 'application/json';\n" +
            "  modelIncludes: [ test::graphql::router::Employee ];\n" +
            "}\n" +
            "###GraphQL\n" +
            "GraphQLStore test::graphql::router::TestStore\n" +
            "{\n" +
            "}\n" +
            "###Mapping\n" +
            "Mapping test::graphql::router::TestMapping\n" +
            "(\n" +
            "  *test::graphql::router::Employee: GraphQL\n" +
            "  {\n" +
            "    ~typeName [test::graphql::router::TestStore] 'Query'\n" +
            "    ~binding test::graphql::router::TestBinding\n" +
            "  }\n" +
            ")\n";

    @Before
    public void setup()
    {
        int port = DynamicPortGenerator.generatePort();
        wireMockServer = new WireMockServer(port);
        wireMockServer.start();
        WireMock.configureFor("localhost", port);

        String connection =
                "###Connection\n" +
                "GraphQLConnection test::graphql::router::TestConnection\n" +
                "{\n" +
                "  store: test::graphql::router::TestStore;\n" +
                "  url: 'http://127.0.0.1:" + port + "/graphql';\n" +
                "}\n" +
                "###Runtime\n" +
                "Runtime test::graphql::router::TestRuntime\n" +
                "{\n" +
                "  mappings: [ test::graphql::router::TestMapping ];\n" +
                "  connections: [\n" +
                "    test::graphql::router::TestStore: [ connection_1: test::graphql::router::TestConnection ]\n" +
                "  ];\n" +
                "}\n";
        grammar = MODEL_AND_BINDING + connection;
    }

    @After
    public void teardown()
    {
        wireMockServer.stop();
    }

    private static String query(String body)
    {
        return "###Pure\n" +
                "function test::graphql::router::query(): Any[1]\n" +
                "{\n" +
                "  {|" + body + "}\n" +
                "}\n";
    }

    @SuppressWarnings("unchecked")
    private static <T> T findNode(org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode node, Class<T> type)
    {
        if (type.isInstance(node))
        {
            return (T) node;
        }
        if (node instanceof org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.GlobalGraphFetchExecutionNode)
        {
            org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.GlobalGraphFetchExecutionNode global = (org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.GlobalGraphFetchExecutionNode) node;
            if (global.localGraphFetchExecutionNode != null)
            {
                T found = findNode(global.localGraphFetchExecutionNode, type);
                if (found != null)
                {
                    return found;
                }
            }
        }
        if (node.executionNodes != null)
        {
            for (org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode child : node.executionNodes)
            {
                T found = findNode(child, type);
                if (found != null)
                {
                    return found;
                }
            }
        }
        return null;
    }

    private static final String SELECT_AND_SERIALIZE =
            "test::graphql::router::Employee.all()\n" +
            "    ->graphFetch(#{test::graphql::router::Employee{firstName,lastName}}#)\n" +
            "    ->serialize(#{test::graphql::router::Employee{firstName,lastName}}#)";

    @Test
    public void testEndToEndQueryReturnsRealDeserializedObjects()
    {
        wireMockServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"firstName\":\"Ada\",\"lastName\":\"Lovelace\"}}")));

        SingleExecutionPlan plan = GraphQLStoreTestUtils.buildPlanForQuery(grammar + query(SELECT_AND_SERIALIZE), "test::graphql::router::TestMapping", "test::graphql::router::TestRuntime");

        InMemoryRootGraphFetchExecutionNode rootGraphFetchNode = findNode(plan.rootExecutionNode, InMemoryRootGraphFetchExecutionNode.class);
        Assert.assertNotNull("expected an InMemoryRootGraphFetchExecutionNode somewhere in the plan", rootGraphFetchNode);
        Assert.assertEquals(1, rootGraphFetchNode.executionNodes.size());
        Assert.assertTrue("expected ExternalFormatInternalizeExecutionNode, was " + rootGraphFetchNode.executionNodes.get(0).getClass(), rootGraphFetchNode.executionNodes.get(0) instanceof ExternalFormatInternalizeExecutionNode);
        ExternalFormatInternalizeExecutionNode internalizeNode = (ExternalFormatInternalizeExecutionNode) rootGraphFetchNode.executionNodes.get(0);
        Assert.assertEquals(1, internalizeNode.executionNodes.size());
        Assert.assertTrue("expected GraphQLExecutionNode, was " + internalizeNode.executionNodes.get(0).getClass(), internalizeNode.executionNodes.get(0) instanceof GraphQLExecutionNode);

        String result = GraphQLStoreTestUtils.executePlan(plan);
        Assert.assertTrue(result.contains("Ada"));
        Assert.assertTrue(result.contains("Lovelace"));
    }

    private static final String FILTER_THEN_GRAPH_FETCH_AND_SERIALIZE =
            "test::graphql::router::Employee.all()\n" +
            "    ->filter(e | $e.firstName == 'Ada')\n" +
            "    ->graphFetch(#{test::graphql::router::Employee{firstName,lastName}}#)\n" +
            "    ->serialize(#{test::graphql::router::Employee{firstName,lastName}}#)";

    @Test
    public void testFilterImmediatelyBeforeGraphFetchFailsPlanGenerationClearly()
    {
        try
        {
            GraphQLStoreTestUtils.buildPlanForQuery(grammar + query(FILTER_THEN_GRAPH_FETCH_AND_SERIALIZE), "test::graphql::router::TestMapping", "test::graphql::router::TestRuntime");
            Assert.fail("expected plan generation to fail for a filter() immediately before graphFetch()");
        }
        catch (Exception e)
        {
            Assert.assertTrue("expected a clear message naming filter() as unsupported, got: " + e.getMessage(),
                    e.getMessage() != null && e.getMessage().contains("Push-down/in-memory filtering is not supported"));
        }
    }

    @Test
    public void testGraphFetchWithoutFilterStillPlansAndExecutes()
    {
        wireMockServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":[{\"firstName\":\"Ada\",\"lastName\":\"Lovelace\"},{\"firstName\":\"Grace\",\"lastName\":\"Hopper\"}]}")));

        String selectAndSerialize =
                "test::graphql::router::Employee.all()\n" +
                "    ->graphFetch(#{test::graphql::router::Employee{firstName,lastName}}#)\n" +
                "    ->serialize(#{test::graphql::router::Employee{firstName,lastName}}#)";

        SingleExecutionPlan plan = GraphQLStoreTestUtils.buildPlanForQuery(grammar + query(selectAndSerialize), "test::graphql::router::TestMapping", "test::graphql::router::TestRuntime");

        String result = GraphQLStoreTestUtils.executePlan(plan);
        Assert.assertTrue("expected Ada:\n" + result, result.contains("Ada"));
        Assert.assertTrue("expected Grace:\n" + result, result.contains("Grace"));
    }

    @Test
    public void testUnsupportedArgumentTypeStillFailsPlanGenerationClearly()
    {
        String dateArgModelAndBinding =
                "###Pure\n" +
                "Class test::graphql::router::unsupportedarg::Employee\n" +
                "{\n" +
                "  firstName: String[1];\n" +
                // Delegates to an already-mapped primitive property so the fixture needs no embedded mapping.
                "  employeeAsOf(asOf: Date[1]) { $this.firstName }: String[1];\n" +
                "}\n" +
                "###ExternalFormat\n" +
                "SchemaSet test::graphql::router::unsupportedarg::TestSchema\n" +
                "{\n" +
                "  format: JSON;\n" +
                "  schemas: [\n" +
                "    {\n" +
                "      location: 'test/graphql/router/unsupportedarg/Employee.json';\n" +
                "      content: '{\"$schema\": \"http://json-schema.org/draft-07/schema#\", \"title\": \"test::graphql::router::unsupportedarg::Employee\", \"type\": \"object\", \"properties\": {\"firstName\": {\"type\": \"string\"}}, \"required\": [\"firstName\"]}';\n" +
                "    }\n" +
                "  ];\n" +
                "}\n" +
                "\n" +
                "Binding test::graphql::router::unsupportedarg::TestBinding\n" +
                "{\n" +
                "  schemaSet: test::graphql::router::unsupportedarg::TestSchema;\n" +
                "  contentType: 'application/json';\n" +
                "  modelIncludes: [ test::graphql::router::unsupportedarg::Employee ];\n" +
                "}\n" +
                "###GraphQL\n" +
                "GraphQLStore test::graphql::router::unsupportedarg::TestStore\n" +
                "{\n" +
                "}\n" +
                "###Mapping\n" +
                "Mapping test::graphql::router::unsupportedarg::TestMapping\n" +
                "(\n" +
                "  *test::graphql::router::unsupportedarg::Employee: GraphQL\n" +
                "  {\n" +
                "    ~typeName [test::graphql::router::unsupportedarg::TestStore] 'Query'\n" +
                "    ~binding test::graphql::router::unsupportedarg::TestBinding\n" +
                "  }\n" +
                ")\n" +
                "###Connection\n" +
                "GraphQLConnection test::graphql::router::unsupportedarg::TestConnection\n" +
                "{\n" +
                "  store: test::graphql::router::unsupportedarg::TestStore;\n" +
                "  url: 'http://127.0.0.1:" + wireMockServer.port() + "/graphql';\n" +
                "}\n" +
                "###Runtime\n" +
                "Runtime test::graphql::router::unsupportedarg::TestRuntime\n" +
                "{\n" +
                "  mappings: [ test::graphql::router::unsupportedarg::TestMapping ];\n" +
                "  connections: [\n" +
                "    test::graphql::router::unsupportedarg::TestStore: [ connection_1: test::graphql::router::unsupportedarg::TestConnection ]\n" +
                "  ];\n" +
                "}\n";

        String parameterizedQuery =
                "###Pure\n" +
                "function test::graphql::router::unsupportedarg::query(): Any[1]\n" +
                "{\n" +
                "  {asOf: Date[1] |\n" +
                "    test::graphql::router::unsupportedarg::Employee.all()\n" +
                "      ->graphFetch(#{test::graphql::router::unsupportedarg::Employee{employeeAsOf($asOf)}}#)\n" +
                "      ->serialize(#{test::graphql::router::unsupportedarg::Employee{employeeAsOf($asOf)}}#)}\n" +
                "}\n";

        try
        {
            GraphQLStoreTestUtils.buildPlanForQuery(dateArgModelAndBinding + parameterizedQuery,
                    "test::graphql::router::unsupportedarg::TestMapping", "test::graphql::router::unsupportedarg::TestRuntime");
            Assert.fail("expected plan generation to fail for a Date-typed runtime-variable-bound argument");
        }
        catch (Exception e)
        {
            Assert.assertTrue("expected a clear message naming the unsupported argument type, got: " + e.getMessage(),
                    e.getMessage() != null && e.getMessage().contains("GraphQL store does not support a runtime-variable-bound argument of type"));
        }
    }

}
