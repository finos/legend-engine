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
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.store.inMemory.InMemoryCrossStoreGraphFetchExecutionNode;
import org.finos.legend.engine.shared.core.port.DynamicPortGenerator;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

public class TestGraphQLStoreXStoreGraphFetch
{
    private WireMockServer employeeStoreServer;
    private WireMockServer departmentStoreServer;
    private String grammar;

    @Before
    public void setup()
    {
        int employeePort = DynamicPortGenerator.generatePort();
        int departmentPort = DynamicPortGenerator.generatePort();
        employeeStoreServer = new WireMockServer(employeePort);
        departmentStoreServer = new WireMockServer(departmentPort);
        employeeStoreServer.start();
        departmentStoreServer.start();
        WireMock.configureFor("localhost", employeePort);

        String modelAndBinding =
                "###Pure\n" +
                "Class test::graphql::xstore::Employee\n" +
                "{\n" +
                "  firstName: String[1];\n" +
                "  departmentId: String[1];\n" +
                "}\n" +
                "Class test::graphql::xstore::Department\n" +
                "{\n" +
                "  id: String[1];\n" +
                "  name: String[1];\n" +
                "  departmentByJoinKeyProxy: test::graphql::xstore::Department[0..1];\n" +
                "  departmentByJoinKey(id: String[1]) { $this.departmentByJoinKeyProxy }: test::graphql::xstore::Department[0..1];\n" +
                "}\n" +
                "Association test::graphql::xstore::Employee_Department\n" +
                "{\n" +
                "  employees: test::graphql::xstore::Employee[*];\n" +
                "  department: test::graphql::xstore::Department[1];\n" +
                "}\n" +
                "###ExternalFormat\n" +
                "SchemaSet test::graphql::xstore::EmployeeSchema\n" +
                "{\n" +
                "  format: JSON;\n" +
                "  schemas: [\n" +
                "    {\n" +
                "      location: 'test/graphql/xstore/Employee.json';\n" +
                "      content: '{\"$schema\": \"http://json-schema.org/draft-07/schema#\", \"title\": \"test::graphql::xstore::Employee\", \"type\": \"object\", \"properties\": {\"firstName\": {\"type\": \"string\"}, \"departmentId\": {\"type\": \"string\"}}, \"required\": [\"firstName\", \"departmentId\"]}';\n" +
                "    }\n" +
                "  ];\n" +
                "}\n" +
                "SchemaSet test::graphql::xstore::DepartmentSchema\n" +
                "{\n" +
                "  format: JSON;\n" +
                "  schemas: [\n" +
                "    {\n" +
                "      location: 'test/graphql/xstore/Department.json';\n" +
                "      content: '{\"$schema\": \"http://json-schema.org/draft-07/schema#\", \"title\": \"test::graphql::xstore::Department\", \"type\": \"object\", \"properties\": {\"id\": {\"type\": \"string\"}, \"name\": {\"type\": \"string\"}, \"departmentByJoinKeyProxy\": {\"type\": \"object\", \"properties\": {\"id\": {\"type\": \"string\"}, \"name\": {\"type\": \"string\"}}}}, \"required\": [\"id\", \"name\"]}';\n" +
                "    }\n" +
                "  ];\n" +
                "}\n" +
                "\n" +
                "Binding test::graphql::xstore::EmployeeBinding\n" +
                "{\n" +
                "  schemaSet: test::graphql::xstore::EmployeeSchema;\n" +
                "  contentType: 'application/json';\n" +
                "  modelIncludes: [ test::graphql::xstore::Employee ];\n" +
                "}\n" +
                "Binding test::graphql::xstore::DepartmentBinding\n" +
                "{\n" +
                "  schemaSet: test::graphql::xstore::DepartmentSchema;\n" +
                "  contentType: 'application/json';\n" +
                "  modelIncludes: [ test::graphql::xstore::Department ];\n" +
                "}\n" +
                "###GraphQL\n" +
                "GraphQLStore test::graphql::xstore::StoreA\n" +
                "{\n" +
                "}\n" +
                "GraphQLStore test::graphql::xstore::StoreB\n" +
                "{\n" +
                "}\n" +
                "###Mapping\n" +
                "Mapping test::graphql::xstore::TestMapping\n" +
                "(\n" +
                "  *test::graphql::xstore::Employee[employee_set]: GraphQL\n" +
                "  {\n" +
                "    ~typeName [test::graphql::xstore::StoreA] 'Query'\n" +
                "    ~binding test::graphql::xstore::EmployeeBinding\n" +
                "  }\n" +
                "  *test::graphql::xstore::Department[department_set]: GraphQL\n" +
                "  {\n" +
                "    ~typeName [test::graphql::xstore::StoreB] 'Query'\n" +
                "    ~binding test::graphql::xstore::DepartmentBinding\n" +
                "    ~joinAccessor departmentByJoinKey\n" +
                "  }\n" +
                "  test::graphql::xstore::Employee_Department: XStore\n" +
                "  {\n" +
                "    department[employee_set, department_set]: $this.departmentId == $that.id,\n" +
                "    employees[department_set, employee_set]: $this.id == $that.departmentId\n" +
                "  }\n" +
                ")\n";

        String connectionsAndRuntime =
                "###Connection\n" +
                "GraphQLConnection test::graphql::xstore::StoreAConnection\n" +
                "{\n" +
                "  store: test::graphql::xstore::StoreA;\n" +
                "  url: 'http://127.0.0.1:" + employeePort + "/graphql';\n" +
                "}\n" +
                "GraphQLConnection test::graphql::xstore::StoreBConnection\n" +
                "{\n" +
                "  store: test::graphql::xstore::StoreB;\n" +
                "  url: 'http://127.0.0.1:" + departmentPort + "/graphql';\n" +
                "}\n" +
                "###Runtime\n" +
                "Runtime test::graphql::xstore::TestRuntime\n" +
                "{\n" +
                "  mappings: [ test::graphql::xstore::TestMapping ];\n" +
                "  connections: [\n" +
                "    test::graphql::xstore::StoreA: [ connection_1: test::graphql::xstore::StoreAConnection ],\n" +
                "    test::graphql::xstore::StoreB: [ connection_2: test::graphql::xstore::StoreBConnection ]\n" +
                "  ];\n" +
                "}\n";

        grammar = modelAndBinding + connectionsAndRuntime;
    }

    @After
    public void teardown()
    {
        employeeStoreServer.stop();
        departmentStoreServer.stop();
    }

    private static String query(String body)
    {
        return "###Pure\n" +
                "function test::graphql::xstore::query(): Any[1]\n" +
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
            // Cross-store children live in global.children, not localGraphFetchExecutionNode/executionNodes.
            if (global.children != null)
            {
                for (org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.GlobalGraphFetchExecutionNode child : global.children)
                {
                    T found = findNode(child, type);
                    if (found != null)
                    {
                        return found;
                    }
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

    @Test
    public void testCrossInstanceJoinIssuesOneRequestPerParentRowWithJoinKeyAndReturnsJoinedData()
    {
        WireMock.configureFor("localhost", employeeStoreServer.port());
        employeeStoreServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"firstName\":\"Ada\",\"departmentId\":\"d1\"}}")));

        WireMock.configureFor("localhost", departmentStoreServer.port());
        departmentStoreServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        // Keyed under the proxy property's name (the alias), not the QP's own name.
                        .withBody("{\"data\":{\"departmentByJoinKeyProxy\":{\"id\":\"d1\",\"name\":\"Engineering\"}}}")));

        String selectAndSerialize =
                "test::graphql::xstore::Employee.all()\n" +
                "    ->graphFetch(#{test::graphql::xstore::Employee{firstName,department{name}}}#)\n" +
                "    ->serialize(#{test::graphql::xstore::Employee{firstName,department{name}}}#)";

        SingleExecutionPlan plan = GraphQLStoreTestUtils.buildPlanForQuery(grammar + query(selectAndSerialize), "test::graphql::xstore::TestMapping", "test::graphql::xstore::TestRuntime");

        InMemoryCrossStoreGraphFetchExecutionNode crossStoreNode = findNode(plan.rootExecutionNode, InMemoryCrossStoreGraphFetchExecutionNode.class);
        Assert.assertNotNull("expected an InMemoryCrossStoreGraphFetchExecutionNode somewhere in the plan", crossStoreNode);
        Assert.assertFalse("Phase 5b hardcodes supportsBatching=false - no schema yet to verify a join argument accepts a list", crossStoreNode.supportsBatching);

        String result = GraphQLStoreTestUtils.executePlan(plan);

        Assert.assertTrue("expected the parent Employee's own field in the result", result.contains("Ada"));
        Assert.assertTrue("expected the cross-instance joined Department field in the result", result.contains("Engineering"));

        WireMock.configureFor("localhost", departmentStoreServer.port());
        verify(postRequestedFor(urlEqualTo("/graphql")).withRequestBody(containing("departmentByJoinKeyProxy: departmentByJoinKey(id: $Department_id)")));
        departmentStoreServer.verify(1, postRequestedFor(urlEqualTo("/graphql")));
    }

    @Test
    public void testCrossInstanceJoinScopesRequestByTheParentRowsActualJoinKeyValue()
    {
        WireMock.configureFor("localhost", employeeStoreServer.port());
        employeeStoreServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"firstName\":\"Grace\",\"departmentId\":\"d7\"}}")));

        WireMock.configureFor("localhost", departmentStoreServer.port());
        departmentStoreServer.stubFor(post(urlEqualTo("/graphql"))
                .withRequestBody(containing("\"Department_id\":\"d7\""))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"departmentByJoinKeyProxy\":{\"id\":\"d7\",\"name\":\"Research\"}}}")));

        String selectAndSerialize =
                "test::graphql::xstore::Employee.all()\n" +
                "    ->graphFetch(#{test::graphql::xstore::Employee{firstName,department{name}}}#)\n" +
                "    ->serialize(#{test::graphql::xstore::Employee{firstName,department{name}}}#)";

        SingleExecutionPlan plan = GraphQLStoreTestUtils.buildPlanForQuery(grammar + query(selectAndSerialize), "test::graphql::xstore::TestMapping", "test::graphql::xstore::TestRuntime");

        String result = GraphQLStoreTestUtils.executePlan(plan);

        Assert.assertTrue("expected the cross-instance joined Department field in the result", result.contains("Research"));

        WireMock.configureFor("localhost", departmentStoreServer.port());
        departmentStoreServer.verify(1, postRequestedFor(urlEqualTo("/graphql")).withRequestBody(containing("\"Department_id\":\"d7\"")));
    }

    @Test
    public void testCrossInstanceJoinWithoutJoinAccessorFailsPlanGenerationClearly()
    {
        String modelAndBindingWithoutJoinAccessor =
                "###Pure\n" +
                "Class test::graphql::xstore::noaccessor::Employee\n" +
                "{\n" +
                "  firstName: String[1];\n" +
                "  departmentId: String[1];\n" +
                "}\n" +
                "Class test::graphql::xstore::noaccessor::Department\n" +
                "{\n" +
                "  id: String[1];\n" +
                "  name: String[1];\n" +
                "}\n" +
                "Association test::graphql::xstore::noaccessor::Employee_Department\n" +
                "{\n" +
                "  employees: test::graphql::xstore::noaccessor::Employee[*];\n" +
                "  department: test::graphql::xstore::noaccessor::Department[1];\n" +
                "}\n" +
                "###ExternalFormat\n" +
                "SchemaSet test::graphql::xstore::noaccessor::EmployeeSchema\n" +
                "{\n" +
                "  format: JSON;\n" +
                "  schemas: [\n" +
                "    {\n" +
                "      location: 'test/graphql/xstore/noaccessor/Employee.json';\n" +
                "      content: '{\"$schema\": \"http://json-schema.org/draft-07/schema#\", \"title\": \"test::graphql::xstore::noaccessor::Employee\", \"type\": \"object\", \"properties\": {\"firstName\": {\"type\": \"string\"}, \"departmentId\": {\"type\": \"string\"}}, \"required\": [\"firstName\", \"departmentId\"]}';\n" +
                "    }\n" +
                "  ];\n" +
                "}\n" +
                "SchemaSet test::graphql::xstore::noaccessor::DepartmentSchema\n" +
                "{\n" +
                "  format: JSON;\n" +
                "  schemas: [\n" +
                "    {\n" +
                "      location: 'test/graphql/xstore/noaccessor/Department.json';\n" +
                "      content: '{\"$schema\": \"http://json-schema.org/draft-07/schema#\", \"title\": \"test::graphql::xstore::noaccessor::Department\", \"type\": \"object\", \"properties\": {\"id\": {\"type\": \"string\"}, \"name\": {\"type\": \"string\"}}, \"required\": [\"id\", \"name\"]}';\n" +
                "    }\n" +
                "  ];\n" +
                "}\n" +
                "\n" +
                "Binding test::graphql::xstore::noaccessor::EmployeeBinding\n" +
                "{\n" +
                "  schemaSet: test::graphql::xstore::noaccessor::EmployeeSchema;\n" +
                "  contentType: 'application/json';\n" +
                "  modelIncludes: [ test::graphql::xstore::noaccessor::Employee ];\n" +
                "}\n" +
                "Binding test::graphql::xstore::noaccessor::DepartmentBinding\n" +
                "{\n" +
                "  schemaSet: test::graphql::xstore::noaccessor::DepartmentSchema;\n" +
                "  contentType: 'application/json';\n" +
                "  modelIncludes: [ test::graphql::xstore::noaccessor::Department ];\n" +
                "}\n" +
                "###GraphQL\n" +
                "GraphQLStore test::graphql::xstore::noaccessor::StoreA\n" +
                "{\n" +
                "}\n" +
                "GraphQLStore test::graphql::xstore::noaccessor::StoreB\n" +
                "{\n" +
                "}\n" +
                "###Mapping\n" +
                "Mapping test::graphql::xstore::noaccessor::TestMapping\n" +
                "(\n" +
                "  *test::graphql::xstore::noaccessor::Employee[employee_set]: GraphQL\n" +
                "  {\n" +
                "    ~typeName [test::graphql::xstore::noaccessor::StoreA] 'Query'\n" +
                "    ~binding test::graphql::xstore::noaccessor::EmployeeBinding\n" +
                "  }\n" +
                "  *test::graphql::xstore::noaccessor::Department[department_set]: GraphQL\n" +
                "  {\n" +
                "    ~typeName [test::graphql::xstore::noaccessor::StoreB] 'Query'\n" +
                "    ~binding test::graphql::xstore::noaccessor::DepartmentBinding\n" +
                "  }\n" +
                "  test::graphql::xstore::noaccessor::Employee_Department: XStore\n" +
                "  {\n" +
                "    department[employee_set, department_set]: $this.departmentId == $that.id,\n" +
                "    employees[department_set, employee_set]: $this.id == $that.departmentId\n" +
                "  }\n" +
                ")\n" +
                "###Connection\n" +
                "GraphQLConnection test::graphql::xstore::noaccessor::StoreAConnection\n" +
                "{\n" +
                "  store: test::graphql::xstore::noaccessor::StoreA;\n" +
                "  url: 'http://127.0.0.1:" + employeeStoreServer.port() + "/graphql';\n" +
                "}\n" +
                "GraphQLConnection test::graphql::xstore::noaccessor::StoreBConnection\n" +
                "{\n" +
                "  store: test::graphql::xstore::noaccessor::StoreB;\n" +
                "  url: 'http://127.0.0.1:" + departmentStoreServer.port() + "/graphql';\n" +
                "}\n" +
                "###Runtime\n" +
                "Runtime test::graphql::xstore::noaccessor::TestRuntime\n" +
                "{\n" +
                "  mappings: [ test::graphql::xstore::noaccessor::TestMapping ];\n" +
                "  connections: [\n" +
                "    test::graphql::xstore::noaccessor::StoreA: [ connection_1: test::graphql::xstore::noaccessor::StoreAConnection ],\n" +
                "    test::graphql::xstore::noaccessor::StoreB: [ connection_2: test::graphql::xstore::noaccessor::StoreBConnection ]\n" +
                "  ];\n" +
                "}\n";

        String selectAndSerialize =
                "test::graphql::xstore::noaccessor::Employee.all()\n" +
                "    ->graphFetch(#{test::graphql::xstore::noaccessor::Employee{firstName,department{name}}}#)\n" +
                "    ->serialize(#{test::graphql::xstore::noaccessor::Employee{firstName,department{name}}}#)";

        try
        {
            GraphQLStoreTestUtils.buildPlanForQuery(modelAndBindingWithoutJoinAccessor + query(selectAndSerialize), "test::graphql::xstore::noaccessor::TestMapping", "test::graphql::xstore::noaccessor::TestRuntime");
            Assert.fail("expected plan generation to fail without a ~joinAccessor on the cross-instance join target");
        }
        catch (Exception e)
        {
            Assert.assertTrue("expected a clear message naming the missing ~joinAccessor requirement, got: " + e.getMessage(),
                    e.getMessage() != null && e.getMessage().contains("needs a '~joinAccessor' qualified property"));
        }
    }
}
