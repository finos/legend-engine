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

package org.finos.legend.engine.plan.generation.stores.graphQLStore;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.json.JsonStreamToJsonDefaultSerializer;
import org.finos.legend.engine.plan.execution.result.json.JsonStreamingResult;
import org.finos.legend.engine.plan.execution.stores.graphQLStore.plugin.GraphQLStoreExecutorBuilder;
import org.finos.legend.engine.plan.execution.stores.inMemory.plugin.InMemory;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.transformers.LegendPlanTransformers;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.graphQLStore.executionPlan.GraphQLExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;
import org.finos.legend.engine.protocol.pure.m3.function.Function;
import org.finos.legend.engine.protocol.pure.m3.function.LambdaFunction;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.core.port.DynamicPortGenerator;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.core_external_format_json_java_platform_binding_legendJavaPlatformBinding_descriptor;
import org.finos.legend.pure.generated.core_external_store_graphql_java_platform_binding_graphQLStoreLegendJavaPlatformBindingExtension;
import org.finos.legend.server.pac4j.kerberos.KerberosProfile;
import org.finos.legend.server.pac4j.kerberos.LocalCredentials;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.notContaining;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

// A second WireMock server stands in for the live data API, distinct from the one serving introspection.
public class TestGraphQLStoreGenerationToExecutionEndToEnd
{
    private WireMockServer introspectionServer;
    private WireMockServer dataServer;

    @Before
    public void setup()
    {
        int introspectionPort = DynamicPortGenerator.generatePort();
        introspectionServer = new WireMockServer(introspectionPort);
        introspectionServer.start();

        int dataPort = DynamicPortGenerator.generatePort();
        dataServer = new WireMockServer(dataPort);
        dataServer.start();
    }

    @After
    public void teardown()
    {
        introspectionServer.stop();
        dataServer.stop();
    }

    private static final String INTROSPECTION_RESPONSE =
            "{\"data\":{\"__schema\":{" +
            "\"queryType\":{\"name\":\"Query\"}," +
            "\"mutationType\":null," +
            "\"types\":[" +
            "{\"kind\":\"OBJECT\",\"name\":\"Query\",\"fields\":[" +
            "  {\"name\":\"departments\",\"type\":{\"kind\":\"LIST\",\"ofType\":{\"kind\":\"OBJECT\",\"name\":\"Department\"}}}" +
            "]}," +
            "{\"kind\":\"OBJECT\",\"name\":\"Department\",\"fields\":[" +
            "  {\"name\":\"name\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}" +
            "]}" +
            "]" +
            "}}}";

    @Test
    public void testGeneratedMappingExecutesAgainstRealEndpoint() throws Exception
    {
        introspectionServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(INTROSPECTION_RESPONSE)));

        GraphQLStoreModelGenerationInput input = new GraphQLStoreModelGenerationInput();
        input.url = "http://127.0.0.1:" + introspectionServer.port() + "/graphql";
        input.storePath = "test::graphql::e2e::TestStore";
        input.targetPackage = "test::graphql::e2e";
        input.targetBindingPath = "test::graphql::e2e";
        input.targetMappingPath = "test::graphql::e2e::TestMapping";

        GraphQLStoreModelGenerationResult generationResult = new GraphQLStoreModelGenerator().generate(input, Identity.getAnonymousIdentity());
        Assert.assertTrue("expected no skipped types for this schema:\n" + generationResult.skippedTypes, generationResult.skippedTypes.isEmpty());

        String connectionRuntimeAndQuery =
                "###Connection\n" +
                "GraphQLConnection test::graphql::e2e::TestConnection\n" +
                "{\n" +
                "  store: test::graphql::e2e::TestStore;\n" +
                "  url: 'http://127.0.0.1:" + dataServer.port() + "/graphql';\n" +
                "}\n" +
                "###Runtime\n" +
                "Runtime test::graphql::e2e::TestRuntime\n" +
                "{\n" +
                "  mappings: [ test::graphql::e2e::TestMapping ];\n" +
                "  connections: [\n" +
                "    test::graphql::e2e::TestStore: [ connection_1: test::graphql::e2e::TestConnection ]\n" +
                "  ];\n" +
                "}\n" +
                "###Pure\n" +
                "function test::graphql::e2e::query(): Any[1]\n" +
                "{\n" +
                "  {|test::graphql::e2e::Query.all()\n" +
                "      ->graphFetch(#{test::graphql::e2e::Query{departments{name}}}#)\n" +
                "      ->serialize(#{test::graphql::e2e::Query{departments{name}}}#)}\n" +
                "}\n";

        dataServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"departments\":[{\"name\":\"Engineering\"}]}}")));

        SingleExecutionPlan plan = buildPlanForQuery(generationResult.grammar + connectionRuntimeAndQuery, "test::graphql::e2e::TestMapping", "test::graphql::e2e::TestRuntime");
        String result = executePlan(plan);

        Assert.assertTrue("expected the real fetched data in the result:\n" + result, result.contains("Engineering"));
        dataServer.verify(postRequestedFor(urlEqualTo("/graphql")));
        introspectionServer.verify(1, postRequestedFor(urlEqualTo("/graphql")));
    }

    private static final String NESTED_INTROSPECTION_RESPONSE =
            "{\"data\":{\"__schema\":{" +
            "\"queryType\":{\"name\":\"Query\"}," +
            "\"mutationType\":null," +
            "\"types\":[" +
            "{\"kind\":\"OBJECT\",\"name\":\"Query\",\"fields\":[" +
            "  {\"name\":\"employees\",\"type\":{\"kind\":\"LIST\",\"ofType\":{\"kind\":\"OBJECT\",\"name\":\"Employee\"}}}" +
            "]}," +
            "{\"kind\":\"OBJECT\",\"name\":\"Employee\",\"fields\":[" +
            "  {\"name\":\"firstName\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}," +
            "  {\"name\":\"department\",\"type\":{\"kind\":\"OBJECT\",\"name\":\"Department\"}}" +
            "]}," +
            "{\"kind\":\"OBJECT\",\"name\":\"Department\",\"fields\":[" +
            "  {\"name\":\"name\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}" +
            "]}" +
            "]" +
            "}}}";

    @Test
    public void testGeneratedNestedAssociationFoldsIntoOneRequest() throws Exception
    {
        introspectionServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(NESTED_INTROSPECTION_RESPONSE)));

        GraphQLStoreModelGenerationInput input = new GraphQLStoreModelGenerationInput();
        input.url = "http://127.0.0.1:" + introspectionServer.port() + "/graphql";
        input.storePath = "test::graphql::e2e::folding::TestStore";
        input.targetPackage = "test::graphql::e2e::folding";
        input.targetBindingPath = "test::graphql::e2e::folding";
        input.targetMappingPath = "test::graphql::e2e::folding::TestMapping";

        GraphQLStoreModelGenerationResult generationResult = new GraphQLStoreModelGenerator().generate(input, Identity.getAnonymousIdentity());
        Assert.assertTrue("expected no skipped types for this schema:\n" + generationResult.skippedTypes, generationResult.skippedTypes.isEmpty());
        Assert.assertTrue("expected the Query wrapper's own root mapping (* prefix):\n" + generationResult.grammar, generationResult.grammar.contains("*test::graphql::e2e::folding::Query: GraphQL"));
        Assert.assertTrue("expected the Query wrapper to use ~typeName:\n" + generationResult.grammar, generationResult.grammar.contains("~typeName [test::graphql::e2e::folding::TestStore] 'Query'"));
        Assert.assertFalse("expected no standalone class mapping for folded-only Employee:\n" + generationResult.grammar, generationResult.grammar.contains("test::graphql::e2e::folding::Employee: GraphQL"));
        Assert.assertFalse("expected no standalone class mapping for folded-only Department:\n" + generationResult.grammar, generationResult.grammar.contains("test::graphql::e2e::folding::Department: GraphQL"));

        String connectionRuntimeAndQuery =
                "###Connection\n" +
                "GraphQLConnection test::graphql::e2e::folding::TestConnection\n" +
                "{\n" +
                "  store: test::graphql::e2e::folding::TestStore;\n" +
                "  url: 'http://127.0.0.1:" + dataServer.port() + "/graphql';\n" +
                "}\n" +
                "###Runtime\n" +
                "Runtime test::graphql::e2e::folding::TestRuntime\n" +
                "{\n" +
                "  mappings: [ test::graphql::e2e::folding::TestMapping ];\n" +
                "  connections: [\n" +
                "    test::graphql::e2e::folding::TestStore: [ connection_1: test::graphql::e2e::folding::TestConnection ]\n" +
                "  ];\n" +
                "}\n" +
                "###Pure\n" +
                "function test::graphql::e2e::folding::query(): Any[1]\n" +
                "{\n" +
                "  {|test::graphql::e2e::folding::Query.all()\n" +
                "      ->graphFetch(#{test::graphql::e2e::folding::Query{employees{firstName,department{name}}}}#)\n" +
                "      ->serialize(#{test::graphql::e2e::folding::Query{employees{firstName,department{name}}}}#)}\n" +
                "}\n";

        dataServer.stubFor(post(urlEqualTo("/graphql"))
                .withRequestBody(containing("department"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"employees\":[{\"firstName\":\"Ada\",\"department\":{\"name\":\"Engineering\"}}]}}")));

        SingleExecutionPlan plan = buildPlanForQuery(generationResult.grammar + connectionRuntimeAndQuery, "test::graphql::e2e::folding::TestMapping", "test::graphql::e2e::folding::TestRuntime");
        String result = executePlan(plan);

        Assert.assertTrue("expected the parent Employee's own field in the result:\n" + result, result.contains("Ada"));
        Assert.assertTrue("expected the folded nested Department field in the result:\n" + result, result.contains("Engineering"));

        dataServer.verify(1, postRequestedFor(urlEqualTo("/graphql")));
        dataServer.verify(postRequestedFor(urlEqualTo("/graphql")).withRequestBody(containing("firstName")).withRequestBody(containing("department")));
    }

    private static final String QUERY_WRAPPER_INTROSPECTION_RESPONSE =
            "{\"data\":{\"__schema\":{" +
            "\"queryType\":{\"name\":\"Query\"}," +
            "\"mutationType\":null," +
            "\"types\":[" +
            "{\"kind\":\"OBJECT\",\"name\":\"Query\",\"fields\":[" +
            "  {\"name\":\"employees\",\"type\":{\"kind\":\"LIST\",\"ofType\":{\"kind\":\"OBJECT\",\"name\":\"Employee\"}}}," +
            "  {\"name\":\"employee\",\"type\":{\"kind\":\"OBJECT\",\"name\":\"Department\"},\"args\":[{\"name\":\"id\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"Int\"}}]}," +
            "  {\"name\":\"employeeByIdAndRegion\",\"type\":{\"kind\":\"OBJECT\",\"name\":\"Department\"},\"args\":[{\"name\":\"id\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"Int\"}},{\"name\":\"region\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}]}," +
            "  {\"name\":\"departmentsByRegion\",\"type\":{\"kind\":\"LIST\",\"ofType\":{\"kind\":\"OBJECT\",\"name\":\"Department\"}},\"args\":[{\"name\":\"region\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}]}" +
            "]}," +
            "{\"kind\":\"OBJECT\",\"name\":\"Employee\",\"fields\":[" +
            "  {\"name\":\"firstName\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}," +
            "  {\"name\":\"department\",\"type\":{\"kind\":\"OBJECT\",\"name\":\"Department\"}}" +
            "]}," +
            "{\"kind\":\"OBJECT\",\"name\":\"Department\",\"fields\":[" +
            "  {\"name\":\"name\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}" +
            "]}" +
            "]" +
            "}}}";

    private String buildQueryWrapperGrammar(String storePackage) throws Exception
    {
        introspectionServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(QUERY_WRAPPER_INTROSPECTION_RESPONSE)));

        GraphQLStoreModelGenerationInput input = new GraphQLStoreModelGenerationInput();
        input.url = "http://127.0.0.1:" + introspectionServer.port() + "/graphql";
        input.storePath = storePackage + "::TestStore";
        input.targetPackage = storePackage;
        input.targetBindingPath = storePackage;
        input.targetMappingPath = storePackage + "::TestMapping";

        GraphQLStoreModelGenerationResult generationResult = new GraphQLStoreModelGenerator().generate(input, Identity.getAnonymousIdentity());
        Assert.assertTrue("expected no skipped fields for this schema:\n" + generationResult.skippedTypes, generationResult.skippedTypes.isEmpty());
        return generationResult.grammar;
    }

    private static String connectionRuntimeAndQuery(String storePackage, int dataServerPort, String paramDecl, String queryBody)
    {
        return "###Connection\n" +
                "GraphQLConnection " + storePackage + "::TestConnection\n" +
                "{\n" +
                "  store: " + storePackage + "::TestStore;\n" +
                "  url: 'http://127.0.0.1:" + dataServerPort + "/graphql';\n" +
                "}\n" +
                "###Runtime\n" +
                "Runtime " + storePackage + "::TestRuntime\n" +
                "{\n" +
                "  mappings: [ " + storePackage + "::TestMapping ];\n" +
                "  connections: [\n" +
                "    " + storePackage + "::TestStore: [ connection_1: " + storePackage + "::TestConnection ]\n" +
                "  ];\n" +
                "}\n" +
                "###Pure\n" +
                "function " + storePackage + "::query(): Any[1]\n" +
                "{\n" +
                "  {" + paramDecl + " |\n" +
                "    " + queryBody + "}\n" +
                "}\n";
    }

    private static final String ENUM_QUERY_WRAPPER_INTROSPECTION_RESPONSE =
            "{\"data\":{\"__schema\":{" +
            "\"queryType\":{\"name\":\"Query\"}," +
            "\"mutationType\":null," +
            "\"types\":[" +
            "{\"kind\":\"OBJECT\",\"name\":\"Query\",\"fields\":[" +
            "  {\"name\":\"employeesByStatus\",\"type\":{\"kind\":\"LIST\",\"ofType\":{\"kind\":\"OBJECT\",\"name\":\"Employee\"}},\"args\":[{\"name\":\"status\",\"type\":{\"kind\":\"ENUM\",\"name\":\"Status\"}}]}" +
            "]}," +
            "{\"kind\":\"OBJECT\",\"name\":\"Employee\",\"fields\":[" +
            "  {\"name\":\"firstName\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}," +
            "  {\"name\":\"status\",\"type\":{\"kind\":\"ENUM\",\"name\":\"Status\"}}" +
            "]}," +
            "{\"kind\":\"ENUM\",\"name\":\"Status\",\"enumValues\":[{\"name\":\"ACTIVE\"},{\"name\":\"INACTIVE\"}]}" +
            "]" +
            "}}}";

    private String buildEnumQueryWrapperGrammar(String storePackage) throws Exception
    {
        introspectionServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(ENUM_QUERY_WRAPPER_INTROSPECTION_RESPONSE)));

        GraphQLStoreModelGenerationInput input = new GraphQLStoreModelGenerationInput();
        input.url = "http://127.0.0.1:" + introspectionServer.port() + "/graphql";
        input.storePath = storePackage + "::TestStore";
        input.targetPackage = storePackage;
        input.targetBindingPath = storePackage;
        input.targetMappingPath = storePackage + "::TestMapping";

        GraphQLStoreModelGenerationResult generationResult = new GraphQLStoreModelGenerator().generate(input, Identity.getAnonymousIdentity());
        Assert.assertTrue("expected no skipped fields - Enum return types and arguments are both supported:\n" + generationResult.skippedTypes, generationResult.skippedTypes.isEmpty());
        return generationResult.grammar;
    }

    @Test
    public void testGeneratedModelSendsLiteralEnumArgumentAsBareValue() throws Exception
    {
        String storePackage = "test::graphql::e2e::qpargsEnumLiteral";
        String grammar = buildEnumQueryWrapperGrammar(storePackage);

        dataServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"employeesByStatusProxy\":[{\"firstName\":\"Ada\"}]}}")));

        String query = connectionRuntimeAndQuery(storePackage, dataServer.port(), "",
                storePackage + "::Query.all()\n" +
                "      ->graphFetch(#{" + storePackage + "::Query{employeesByStatus(" + storePackage + "::Status.ACTIVE){firstName}}}#)\n" +
                "      ->serialize(#{" + storePackage + "::Query{employeesByStatus(" + storePackage + "::Status.ACTIVE){firstName}}}#)");

        SingleExecutionPlan plan = buildPlanForQuery(grammar + query, storePackage + "::TestMapping", storePackage + "::TestRuntime");
        String result = executePlan(plan);

        Assert.assertTrue("expected real data from the generated model's Enum-argument QP:\n" + result, result.contains("Ada"));
        dataServer.verify(postRequestedFor(urlEqualTo("/graphql"))
                .withRequestBody(containing("employeesByStatus(status: $status)"))
                .withRequestBody(containing("\"status\":\"ACTIVE\"")));
    }

    // KNOWN, PRE-EXISTING LIMITATION (confirmed unrelated to GraphQL Store): a $variable-bound Enum QP argument fails at execution time because
    // DefaultExecutionNodeContext.getResultAsClass cannot coerce the raw ConstantResult supplied for the
    // variable into the generated Enum class. Documented here as an expected failure rather than fixed;
    @Test
    public void testGeneratedModelSendsVariableBoundEnumArgumentAsBareValue() throws Exception
    {
        String storePackage = "test::graphql::e2e::qpargsEnumVariable";
        String grammar = buildEnumQueryWrapperGrammar(storePackage);

        dataServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"employeesByStatusProxy\":[{\"firstName\":\"Ada\"}]}}")));

        String query = connectionRuntimeAndQuery(storePackage, dataServer.port(), "status: " + storePackage + "::Status[1]",
                storePackage + "::Query.all()\n" +
                "        ->graphFetch(#{" + storePackage + "::Query{employeesByStatus($status){firstName}}}#)\n" +
                "        ->serialize(#{" + storePackage + "::Query{employeesByStatus($status){firstName}}}#)");

        SingleExecutionPlan plan = buildPlanForQuery(grammar + query, storePackage + "::TestMapping", storePackage + "::TestRuntime");

        IllegalArgumentException exception = Assert.assertThrows(IllegalArgumentException.class,
                () -> executePlan(plan, java.util.Collections.singletonMap("status", "ACTIVE")));
        Assert.assertTrue("expected the known ConstantResult-to-Enum conversion failure:\n" + exception.getMessage(),
                exception.getMessage().contains("Unable to convert ConstantResult to") && exception.getMessage().contains("Status"));
    }

    @Test
    public void testGeneratedModelDeserializesEnumResponseFieldIntoRealEnum() throws Exception
    {
        String storePackage = "test::graphql::e2e::enumResponseField";
        String grammar = buildEnumQueryWrapperGrammar(storePackage);

        dataServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"employeesByStatusProxy\":[{\"firstName\":\"Ada\",\"status\":\"ACTIVE\"}]}}")));

        String query = connectionRuntimeAndQuery(storePackage, dataServer.port(), "",
                storePackage + "::Query.all()\n" +
                "      ->graphFetch(#{" + storePackage + "::Query{employeesByStatus(" + storePackage + "::Status.ACTIVE){firstName,status}}}#)\n" +
                "      ->serialize(#{" + storePackage + "::Query{employeesByStatus(" + storePackage + "::Status.ACTIVE){firstName,status}}}#)");

        SingleExecutionPlan plan = buildPlanForQuery(grammar + query, storePackage + "::TestMapping", storePackage + "::TestRuntime");
        String result = executePlan(plan);

        Assert.assertTrue("expected the server's status value to deserialize and re-serialize correctly:\n" + result, result.contains("ACTIVE"));
    }

    private static final String INPUT_OBJECT_QUERY_WRAPPER_INTROSPECTION_RESPONSE =
            "{\"data\":{\"__schema\":{" +
            "\"queryType\":{\"name\":\"Query\"}," +
            "\"mutationType\":null," +
            "\"types\":[" +
            "{\"kind\":\"OBJECT\",\"name\":\"Query\",\"fields\":[" +
            "  {\"name\":\"employeesByFilter\",\"type\":{\"kind\":\"LIST\",\"ofType\":{\"kind\":\"OBJECT\",\"name\":\"Employee\"}},\"args\":[" +
            "    {\"name\":\"filters\",\"type\":{\"kind\":\"LIST\",\"ofType\":{\"kind\":\"INPUT_OBJECT\",\"name\":\"FilterKey\"}}}" +
            "  ]}" +
            "]}," +
            "{\"kind\":\"OBJECT\",\"name\":\"Employee\",\"fields\":[" +
            "  {\"name\":\"firstName\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}" +
            "]}," +
            "{\"kind\":\"INPUT_OBJECT\",\"name\":\"FilterKey\",\"inputFields\":[" +
            "  {\"name\":\"key\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}," +
            "  {\"name\":\"value\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}" +
            "]}" +
            "]" +
            "}}}";

    private String buildInputObjectQueryWrapperGrammar(String storePackage) throws Exception
    {
        introspectionServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(INPUT_OBJECT_QUERY_WRAPPER_INTROSPECTION_RESPONSE)));

        GraphQLStoreModelGenerationInput input = new GraphQLStoreModelGenerationInput();
        input.url = "http://127.0.0.1:" + introspectionServer.port() + "/graphql";
        input.storePath = storePackage + "::TestStore";
        input.targetPackage = storePackage;
        input.targetBindingPath = storePackage;
        input.targetMappingPath = storePackage + "::TestMapping";

        GraphQLStoreModelGenerationResult generationResult = new GraphQLStoreModelGenerator().generate(input, Identity.getAnonymousIdentity());
        Assert.assertTrue("expected no skipped fields - input-object-list arguments are supported:\n" + generationResult.skippedTypes, generationResult.skippedTypes.isEmpty());
        return generationResult.grammar;
    }

    // childNodes() (not the plain executionNodes list) is the polymorphic accessor - a
    // GlobalGraphFetchExecutionNode's real child lives in its own separate localGraphFetchExecutionNode
    // field, which only childNodes() knows to include.
    private static GraphQLExecutionNode findGraphQLExecutionNode(ExecutionNode node)
    {
        if (node instanceof GraphQLExecutionNode)
        {
            return (GraphQLExecutionNode) node;
        }
        for (ExecutionNode child : node.childNodes())
        {
            if (child != null)
            {
                GraphQLExecutionNode found = findGraphQLExecutionNode(child);
                if (found != null)
                {
                    return found;
                }
            }
        }
        return null;
    }

    @Test
    public void testVariableBoundInputObjectListArgumentParsesAndPlansSuccessfully() throws Exception
    {
        String storePackage = "test::graphql::e2e::qpargsInputObjectVariable";
        String grammar = buildInputObjectQueryWrapperGrammar(storePackage);

        String query = connectionRuntimeAndQuery(storePackage, dataServer.port(), "filters: " + storePackage + "::FilterKey[*]",
                storePackage + "::Query.all()\n" +
                "        ->graphFetch(#{" + storePackage + "::Query{employeesByFilter($filters){firstName}}}#)\n" +
                "        ->serialize(#{" + storePackage + "::Query{employeesByFilter($filters){firstName}}}#)");

        SingleExecutionPlan plan = buildPlanForQuery(grammar + query, storePackage + "::TestMapping", storePackage + "::TestRuntime");
        Assert.assertNotNull("expected plan generation to succeed for a $variable-bound input-object-list QP argument", plan);

        GraphQLExecutionNode graphQLNode = findGraphQLExecutionNode(plan.rootExecutionNode);
        Assert.assertNotNull("expected a GraphQLExecutionNode in the generated plan", graphQLNode);
        Assert.assertTrue("expected the generated query to reference the filters argument by variable:\n" + graphQLNode.query,
                graphQLNode.query.contains("employeesByFilter(filters: $filters)"));
        Assert.assertTrue("expected the GraphQL variable declaration to be a nullable named list type for the input-object Class, since FilterKey[*] has a zero lower bound:\n" + graphQLNode.query,
                graphQLNode.query.contains("$filters: [FilterKey!]) {"));
        Assert.assertEquals("expected the filters argument to be resolved at execution time, not a static literal",
                1, graphQLNode.requiredVariableInputs.size());
        Assert.assertEquals("filters", graphQLNode.requiredVariableInputs.get(0).name);
    }

    @Test
    public void testLiteralInputObjectListArgumentParsesAndPlansSuccessfully() throws Exception
    {
        String storePackage = "test::graphql::e2e::qpargsInputObjectLiteral";
        String grammar = buildInputObjectQueryWrapperGrammar(storePackage);

        String query = connectionRuntimeAndQuery(storePackage, dataServer.port(), "",
                storePackage + "::Query.all()\n" +
                "        ->graphFetch(#{" + storePackage + "::Query{employeesByFilter([^" + storePackage + "::FilterKey(key='todo', value='todo')]){firstName}}}#)\n" +
                "        ->serialize(#{" + storePackage + "::Query{employeesByFilter([^" + storePackage + "::FilterKey(key='todo', value='todo')]){firstName}}}#)");

        SingleExecutionPlan plan = buildPlanForQuery(grammar + query, storePackage + "::TestMapping", storePackage + "::TestRuntime");
        Assert.assertNotNull("expected plan generation to succeed for a literal input-object-list QP argument", plan);

        GraphQLExecutionNode graphQLNode = findGraphQLExecutionNode(plan.rootExecutionNode);
        Assert.assertNotNull("expected a GraphQLExecutionNode in the generated plan", graphQLNode);
        Assert.assertTrue("expected the literal filter entries to be rendered inline, not as a bound variable:\n" + graphQLNode.query,
                graphQLNode.query.contains("employeesByFilter(filters: [{key: \"todo\", value: \"todo\"}])"));
    }

    private static final String ENUM_FIELD_INPUT_OBJECT_QUERY_WRAPPER_INTROSPECTION_RESPONSE =
            "{\"data\":{\"__schema\":{" +
            "\"queryType\":{\"name\":\"Query\"}," +
            "\"mutationType\":null," +
            "\"types\":[" +
            "{\"kind\":\"OBJECT\",\"name\":\"Query\",\"fields\":[" +
            "  {\"name\":\"employeesByStatusFilter\",\"type\":{\"kind\":\"LIST\",\"ofType\":{\"kind\":\"OBJECT\",\"name\":\"Employee\"}},\"args\":[" +
            "    {\"name\":\"filters\",\"type\":{\"kind\":\"LIST\",\"ofType\":{\"kind\":\"INPUT_OBJECT\",\"name\":\"StatusFilter\"}}}" +
            "  ]}" +
            "]}," +
            "{\"kind\":\"OBJECT\",\"name\":\"Employee\",\"fields\":[" +
            "  {\"name\":\"firstName\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}" +
            "]}," +
            "{\"kind\":\"INPUT_OBJECT\",\"name\":\"StatusFilter\",\"inputFields\":[" +
            "  {\"name\":\"key\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}," +
            "  {\"name\":\"value\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"ENUM\",\"name\":\"Status\"}}}" +
            "]}," +
            "{\"kind\":\"ENUM\",\"name\":\"Status\",\"enumValues\":[{\"name\":\"ACTIVE\"},{\"name\":\"INACTIVE\"}]}" +
            "]" +
            "}}}";

    private String buildEnumFieldInputObjectQueryWrapperGrammar(String storePackage) throws Exception
    {
        introspectionServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(ENUM_FIELD_INPUT_OBJECT_QUERY_WRAPPER_INTROSPECTION_RESPONSE)));

        GraphQLStoreModelGenerationInput input = new GraphQLStoreModelGenerationInput();
        input.url = "http://127.0.0.1:" + introspectionServer.port() + "/graphql";
        input.storePath = storePackage + "::TestStore";
        input.targetPackage = storePackage;
        input.targetBindingPath = storePackage;
        input.targetMappingPath = storePackage + "::TestMapping";

        GraphQLStoreModelGenerationResult generationResult = new GraphQLStoreModelGenerator().generate(input, Identity.getAnonymousIdentity());
        Assert.assertTrue("expected no skipped fields - an Enum-typed input-object field is supported:\n" + generationResult.skippedTypes, generationResult.skippedTypes.isEmpty());
        return generationResult.grammar;
    }

    // Answers a question raised during review: a literal (non-$variable) Enum value embedded inside an
    // inline instance-creation argument (eg. ^StatusFilter(key='status', value=Status.ACTIVE)) is a
    // different code path from a $variable-bound one - sanitizeInstanceCreationVariables (serialize.pure)
    // only substitutes a dummy for a KeyExpression whose expression is a VariableExpression; a literal value
    // falls through unchanged to the ordinary instance-creation codegen (processNew, langLibrary.pure), which
    // already has full Enum support (Conventions + generateInterfacesAndEnums). This test proves that
    // reasoning holds for GraphQL's own query-text rendering too (literalToValue's explicit Enum branch,
    // graphQLArgumentTranslator.pure), not just for the Java-codegen side.
    @Test
    public void testLiteralEnumFieldInsideInstanceCreationArgumentRendersAsUnquotedEnumValue() throws Exception
    {
        String storePackage = "test::graphql::e2e::qpargsEnumFieldInInstanceCreation";
        String grammar = buildEnumFieldInputObjectQueryWrapperGrammar(storePackage);

        String query = connectionRuntimeAndQuery(storePackage, dataServer.port(), "",
                storePackage + "::Query.all()\n" +
                "        ->graphFetch(#{" + storePackage + "::Query{employeesByStatusFilter([^" + storePackage + "::StatusFilter(key='status', value=" + storePackage + "::Status.ACTIVE)]){firstName}}}#)\n" +
                "        ->serialize(#{" + storePackage + "::Query{employeesByStatusFilter([^" + storePackage + "::StatusFilter(key='status', value=" + storePackage + "::Status.ACTIVE)]){firstName}}}#)");

        SingleExecutionPlan plan = buildPlanForQuery(grammar + query, storePackage + "::TestMapping", storePackage + "::TestRuntime");
        Assert.assertNotNull("expected plan generation to succeed for a literal Enum value embedded in an instance-creation argument", plan);

        GraphQLExecutionNode graphQLNode = findGraphQLExecutionNode(plan.rootExecutionNode);
        Assert.assertNotNull("expected a GraphQLExecutionNode in the generated plan", graphQLNode);
        Assert.assertTrue("expected the literal Enum value to be rendered inline and unquoted, per GraphQL's EnumValue syntax:\n" + graphQLNode.query,
                graphQLNode.query.contains("employeesByStatusFilter(filters: [{key: \"status\", value: ACTIVE}])"));
    }

    private static final String LIST_FIELD_INPUT_OBJECT_QUERY_WRAPPER_INTROSPECTION_RESPONSE =
            "{\"data\":{\"__schema\":{" +
            "\"queryType\":{\"name\":\"Query\"}," +
            "\"mutationType\":null," +
            "\"types\":[" +
            "{\"kind\":\"OBJECT\",\"name\":\"Query\",\"fields\":[" +
            "  {\"name\":\"employeesByTagsFilter\",\"type\":{\"kind\":\"LIST\",\"ofType\":{\"kind\":\"OBJECT\",\"name\":\"Employee\"}},\"args\":[" +
            "    {\"name\":\"filters\",\"type\":{\"kind\":\"LIST\",\"ofType\":{\"kind\":\"INPUT_OBJECT\",\"name\":\"TagsFilter\"}}}" +
            "  ]}" +
            "]}," +
            "{\"kind\":\"OBJECT\",\"name\":\"Employee\",\"fields\":[" +
            "  {\"name\":\"firstName\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}" +
            "]}," +
            "{\"kind\":\"INPUT_OBJECT\",\"name\":\"TagsFilter\",\"inputFields\":[" +
            "  {\"name\":\"key\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}," +
            "  {\"name\":\"tags\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"LIST\",\"ofType\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}}}," +
            "  {\"name\":\"optionalTags\",\"type\":{\"kind\":\"LIST\",\"ofType\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}}" +
            "]}" +
            "]" +
            "}}}";

    private String buildListFieldInputObjectQueryWrapperGrammar(String storePackage) throws Exception
    {
        introspectionServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(LIST_FIELD_INPUT_OBJECT_QUERY_WRAPPER_INTROSPECTION_RESPONSE)));

        GraphQLStoreModelGenerationInput input = new GraphQLStoreModelGenerationInput();
        input.url = "http://127.0.0.1:" + introspectionServer.port() + "/graphql";
        input.storePath = storePackage + "::TestStore";
        input.targetPackage = storePackage;
        input.targetBindingPath = storePackage;
        input.targetMappingPath = storePackage + "::TestMapping";

        GraphQLStoreModelGenerationResult generationResult = new GraphQLStoreModelGenerator().generate(input, Identity.getAnonymousIdentity());
        Assert.assertTrue("expected no skipped fields - a non-null list input-object field is supported:\n" + generationResult.skippedTypes, generationResult.skippedTypes.isEmpty());
        Assert.assertTrue("expected TagsFilter.tags to be generated with the RequiredArgumentsProfile tag, since the schema marks it a non-null list:\n" + generationResult.grammar,
                generationResult.grammar.contains("{meta::external::store::graphQL::generation::RequiredArgumentsProfile.requiredArguments = 'tags'} tags: String[*];"));
        Assert.assertFalse("expected TagsFilter.optionalTags to NOT be tagged, since the schema marks it an ordinary nullable list:\n" + generationResult.grammar,
                generationResult.grammar.contains("requiredArguments = 'optionalTags'"));
        return generationResult.grammar;
    }

    @Test
    public void testNestedRequiredListFieldInsideInstanceCreationRendersNonNullVariable() throws Exception
    {
        String storePackage = "test::graphql::e2e::qpargsNestedRequiredListField";
        String grammar = buildListFieldInputObjectQueryWrapperGrammar(storePackage);

        String query = connectionRuntimeAndQuery(storePackage, dataServer.port(), "tags: String[*], optionalTags: String[*]",
                storePackage + "::Query.all()\n" +
                "        ->graphFetch(#{" + storePackage + "::Query{employeesByTagsFilter([^" + storePackage + "::TagsFilter(key='team', tags=$tags, optionalTags=$optionalTags)]){firstName}}}#)\n" +
                "        ->serialize(#{" + storePackage + "::Query{employeesByTagsFilter([^" + storePackage + "::TagsFilter(key='team', tags=$tags, optionalTags=$optionalTags)]){firstName}}}#)");

        SingleExecutionPlan plan = buildPlanForQuery(grammar + query, storePackage + "::TestMapping", storePackage + "::TestRuntime");
        Assert.assertNotNull("expected plan generation to succeed for $variable-bound nested list fields", plan);

        GraphQLExecutionNode graphQLNode = findGraphQLExecutionNode(plan.rootExecutionNode);
        Assert.assertNotNull("expected a GraphQLExecutionNode in the generated plan", graphQLNode);
        Assert.assertTrue("expected $tags non-null ([String!]!) and $optionalTags still a nullable list ([String!], not [String!]!), since only TagsFilter.tags carries the required-list tag:\n" + graphQLNode.query,
                graphQLNode.query.contains("query($tags: [String!]!, $optionalTags: [String!]) {"));
    }

    // KNOWN, PRE-EXISTING LIMITATION: binding an input-object-list QP argument to a single whole
    // $variable (FilterKey[*]) requires FilterKey's Java class to satisfy two independent codegen passes -
    // generateInterfaceForClass (common.pure, builds a plain interface) and
    // generateJavaClassForFunctionParameter (platform.pure, adds private fields + getter/setter bodies for
    // JSON-deserialization validation) - which collide into one incoherent, non-compiling merged class (an
    // `interface` with `private` instance fields). Documented here as an expected failure rather than fixed;
    // the durable fix is to give the validation-DTO pass a distinct, non-colliding Java class name in
    // platform.pure (shared, benefits every store), not something GraphQL Store can work around locally.
    @Test
    public void testVariableBoundInputObjectListArgumentSerializesCorrectlyAtExecutionTime() throws Exception
    {
        String storePackage = "test::graphql::e2e::qpargsInputObjectVariableExec";
        String grammar = buildInputObjectQueryWrapperGrammar(storePackage);

        dataServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"employeesByFilter\":[{\"firstName\":\"Dana\"}]}}")));

        String query = connectionRuntimeAndQuery(storePackage, dataServer.port(), "filters: " + storePackage + "::FilterKey[*]",
                storePackage + "::Query.all()\n" +
                "        ->graphFetch(#{" + storePackage + "::Query{employeesByFilter($filters){firstName}}}#)\n" +
                "        ->serialize(#{" + storePackage + "::Query{employeesByFilter($filters){firstName}}}#)");

        SingleExecutionPlan plan = buildPlanForQuery(grammar + query, storePackage + "::TestMapping", storePackage + "::TestRuntime");

        List<Map<String, String>> filtersValue = Lists.mutable.with(Maps.mutable.with("key", "todo", "value", "todo"));

        RuntimeException exception = Assert.assertThrows(RuntimeException.class,
                () -> executePlan(plan, java.util.Collections.singletonMap("filters", filtersValue)));
        Assert.assertTrue("expected the known FilterKey interface/field-collision compile failure:\n" + exception.getCause(),
                exception.getCause() != null && exception.getCause().getMessage() != null
                        && exception.getCause().getMessage().contains("FilterKey.java")
                        && exception.getCause().getMessage().contains("errors compiling"));
    }

    private static final String REQUIRED_LIST_ARGUMENT_QUERY_WRAPPER_INTROSPECTION_RESPONSE =
            "{\"data\":{\"__schema\":{" +
            "\"queryType\":{\"name\":\"Query\"}," +
            "\"mutationType\":null," +
            "\"types\":[" +
            "{\"kind\":\"OBJECT\",\"name\":\"Query\",\"fields\":[" +
            "  {\"name\":\"employeesByTags\",\"type\":{\"kind\":\"LIST\",\"ofType\":{\"kind\":\"OBJECT\",\"name\":\"Employee\"}},\"args\":[" +
            "    {\"name\":\"requiredTags\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"LIST\",\"ofType\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}}}," +
            "    {\"name\":\"optionalTags\",\"type\":{\"kind\":\"LIST\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}" +
            "  ]}" +
            "]}," +
            "{\"kind\":\"OBJECT\",\"name\":\"Employee\",\"fields\":[" +
            "  {\"name\":\"firstName\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}" +
            "]}" +
            "]" +
            "}}}";

    private String buildRequiredListArgumentQueryWrapperGrammar(String storePackage) throws Exception
    {
        introspectionServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(REQUIRED_LIST_ARGUMENT_QUERY_WRAPPER_INTROSPECTION_RESPONSE)));

        GraphQLStoreModelGenerationInput input = new GraphQLStoreModelGenerationInput();
        input.url = "http://127.0.0.1:" + introspectionServer.port() + "/graphql";
        input.storePath = storePackage + "::TestStore";
        input.targetPackage = storePackage;
        input.targetBindingPath = storePackage;
        input.targetMappingPath = storePackage + "::TestMapping";

        GraphQLStoreModelGenerationResult generationResult = new GraphQLStoreModelGenerator().generate(input, Identity.getAnonymousIdentity());
        Assert.assertTrue("expected no skipped fields for this schema:\n" + generationResult.skippedTypes, generationResult.skippedTypes.isEmpty());
        return generationResult.grammar;
    }

    // tags: [String!]! is required by the schema - [] must still be rendered as an explicit, empty GraphQL
    // variable value, since omitting it entirely (today's behavior for every other empty-list argument under
    // the full-call-arity convention) would violate the schema.
    @Test
    public void testRequiredListArgumentCalledWithEmptyListSendsExplicitEmptyArray() throws Exception
    {
        String storePackage = "test::graphql::e2e::qpargsRequiredListEmpty";
        String grammar = buildRequiredListArgumentQueryWrapperGrammar(storePackage);

        dataServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"employeesByTagsProxy\":[{\"firstName\":\"Dana\"}]}}")));

        String query = connectionRuntimeAndQuery(storePackage, dataServer.port(), "",
                storePackage + "::Query.all()\n" +
                "        ->graphFetch(#{" + storePackage + "::Query{employeesByTags([], ['vip']){firstName}}}#)\n" +
                "        ->serialize(#{" + storePackage + "::Query{employeesByTags([], ['vip']){firstName}}}#)");

        SingleExecutionPlan plan = buildPlanForQuery(grammar + query, storePackage + "::TestMapping", storePackage + "::TestRuntime");
        String result = executePlan(plan);

        Assert.assertTrue("expected real data back:\n" + result, result.contains("Dana"));
        dataServer.verify(postRequestedFor(urlEqualTo("/graphql"))
                .withRequestBody(containing("employeesByTags(requiredTags: $requiredTags, optionalTags: $optionalTags)"))
                .withRequestBody(containing("$requiredTags: [String!]!"))
                .withRequestBody(containing("\"requiredTags\":[]"))
                .withRequestBody(containing("\"optionalTags\":[\"vip\"]")));
    }

    // optionalTags has no NON_NULL wrapper on the list itself - [] for it stays an omission, exactly like
    // today's behavior for any other optional argument, no regression from the #6 fix above.
    @Test
    public void testOptionalListArgumentCalledWithEmptyListIsStillOmitted() throws Exception
    {
        String storePackage = "test::graphql::e2e::qpargsOptionalListEmpty";
        String grammar = buildRequiredListArgumentQueryWrapperGrammar(storePackage);

        dataServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"employeesByTagsProxy\":[{\"firstName\":\"Dana\"}]}}")));

        String query = connectionRuntimeAndQuery(storePackage, dataServer.port(), "",
                storePackage + "::Query.all()\n" +
                "        ->graphFetch(#{" + storePackage + "::Query{employeesByTags(['vip'], []){firstName}}}#)\n" +
                "        ->serialize(#{" + storePackage + "::Query{employeesByTags(['vip'], []){firstName}}}#)");

        SingleExecutionPlan plan = buildPlanForQuery(grammar + query, storePackage + "::TestMapping", storePackage + "::TestRuntime");
        String result = executePlan(plan);

        Assert.assertTrue("expected real data back:\n" + result, result.contains("Dana"));
        dataServer.verify(postRequestedFor(urlEqualTo("/graphql"))
                .withRequestBody(containing("employeesByTags(requiredTags: $requiredTags)"))
                .withRequestBody(notContaining("optionalTags")));
    }

    private static final String REQUIRED_LIST_ARGUMENT_WITH_DEFAULT_INTROSPECTION_RESPONSE =
            "{\"data\":{\"__schema\":{" +
            "\"queryType\":{\"name\":\"Query\"}," +
            "\"mutationType\":null," +
            "\"types\":[" +
            "{\"kind\":\"OBJECT\",\"name\":\"Query\",\"fields\":[" +
            "  {\"name\":\"employeesByTags\",\"type\":{\"kind\":\"LIST\",\"ofType\":{\"kind\":\"OBJECT\",\"name\":\"Employee\"}},\"args\":[" +
            "    {\"name\":\"requiredTags\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"LIST\",\"ofType\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}},\"defaultValue\":\"[]\"}" +
            "  ]}" +
            "]}," +
            "{\"kind\":\"OBJECT\",\"name\":\"Employee\",\"fields\":[" +
            "  {\"name\":\"firstName\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}" +
            "]}" +
            "]" +
            "}}}";

    // A required list argument that ALSO has a schema-level default - whether [] is ambiguous with "omit"
    // is governed purely by the list's own NON_NULL-ness, not by whether a default exists (that only
    // affects the Pure-side parameter's own optionality). Without this, an explicit [] call would silently
    // fall back to "omit", letting the server's default apply instead of the caller's intended empty list.
    @Test
    public void testRequiredListArgumentWithDefaultValueCalledWithEmptyListStillSendsExplicitEmptyArray() throws Exception
    {
        String storePackage = "test::graphql::e2e::qpargsRequiredListWithDefault";
        introspectionServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(REQUIRED_LIST_ARGUMENT_WITH_DEFAULT_INTROSPECTION_RESPONSE)));

        GraphQLStoreModelGenerationInput input = new GraphQLStoreModelGenerationInput();
        input.url = "http://127.0.0.1:" + introspectionServer.port() + "/graphql";
        input.storePath = storePackage + "::TestStore";
        input.targetPackage = storePackage;
        input.targetBindingPath = storePackage;
        input.targetMappingPath = storePackage + "::TestMapping";
        GraphQLStoreModelGenerationResult generationResult = new GraphQLStoreModelGenerator().generate(input, Identity.getAnonymousIdentity());
        Assert.assertTrue("expected no skipped fields:\n" + generationResult.skippedTypes, generationResult.skippedTypes.isEmpty());

        Assert.assertTrue("expected requiredTags to still be tagged required despite having a default:\n" + generationResult.grammar,
                generationResult.grammar.contains("RequiredArgumentsProfile.requiredArguments = 'requiredTags'"));
        Assert.assertTrue("expected requiredTags to be optional on the Pure side because of its default:\n" + generationResult.grammar,
                generationResult.grammar.contains("requiredTags: String[*]) { $this.employeesByTagsProxy }"));

        dataServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"employeesByTagsProxy\":[{\"firstName\":\"Dana\"}]}}")));

        String query = connectionRuntimeAndQuery(storePackage, dataServer.port(), "",
                storePackage + "::Query.all()\n" +
                "        ->graphFetch(#{" + storePackage + "::Query{employeesByTags([]){firstName}}}#)\n" +
                "        ->serialize(#{" + storePackage + "::Query{employeesByTags([]){firstName}}}#)");

        SingleExecutionPlan plan = buildPlanForQuery(generationResult.grammar + query, storePackage + "::TestMapping", storePackage + "::TestRuntime");
        String result = executePlan(plan);

        Assert.assertTrue("expected real data back:\n" + result, result.contains("Dana"));
        dataServer.verify(postRequestedFor(urlEqualTo("/graphql"))
                .withRequestBody(containing("employeesByTags(requiredTags: $requiredTags)"))
                .withRequestBody(containing("\"requiredTags\":[]")));
    }

    private static final String NESTED_INSTANCE_CREATION_MODEL_AND_BINDING =
            "###Pure\n" +
            "Class test::graphql::e2e::nestedArg::Employee\n" +
            "{\n" +
            "  firstName: String[1];\n" +
            "}\n" +
            "Class test::graphql::e2e::nestedArg::Labels\n" +
            "{\n" +
            "  key: String[1];\n" +
            "  value: String[1];\n" +
            "}\n" +
            "Enum test::graphql::e2e::nestedArg::GroupByOperation\n" +
            "{\n" +
            "  SUM, COUNT\n" +
            "}\n" +
            "Class test::graphql::e2e::nestedArg::CustomGroup\n" +
            "{\n" +
            "  labels: test::graphql::e2e::nestedArg::Labels[1];\n" +
            "  groupByOperation: test::graphql::e2e::nestedArg::GroupByOperation[1];\n" +
            "}\n" +
            "Class test::graphql::e2e::nestedArg::Query\n" +
            "{\n" +
            "  groupedEmployeesProxy: test::graphql::e2e::nestedArg::Employee[*];\n" +
            "  groupedEmployees(customGroup: test::graphql::e2e::nestedArg::CustomGroup[1]) { $this.groupedEmployeesProxy }: test::graphql::e2e::nestedArg::Employee[*];\n" +
            "}\n" +
            "###ExternalFormat\n" +
            "SchemaSet test::graphql::e2e::nestedArg::QuerySchema\n" +
            "{\n" +
            "  format: JSON;\n" +
            "  schemas: [\n" +
            "    {\n" +
            "      location: 'test/graphql/e2e/nestedArg/Query.json';\n" +
            "      content: '{\"$schema\": \"http://json-schema.org/draft-07/schema#\", \"title\": \"test::graphql::e2e::nestedArg::Query\", \"type\": \"object\", \"properties\": {\"groupedEmployeesProxy\": {\"type\": \"array\", \"items\": {\"$ref\": \"#/definitions/test::graphql::e2e::nestedArg::Employee\"}}}, \"definitions\": {\"test::graphql::e2e::nestedArg::Employee\": {\"type\": \"object\", \"properties\": {\"firstName\": {\"type\": \"string\"}}, \"required\": [\"firstName\"]}}}';\n" +
            "    }\n" +
            "  ];\n" +
            "}\n" +
            "\n" +
            "Binding test::graphql::e2e::nestedArg::QueryBinding\n" +
            "{\n" +
            "  schemaSet: test::graphql::e2e::nestedArg::QuerySchema;\n" +
            "  contentType: 'application/json';\n" +
            "  modelIncludes: [ test::graphql::e2e::nestedArg::Query, test::graphql::e2e::nestedArg::Employee ];\n" +
            "}\n" +
            "###GraphQL\n" +
            "GraphQLStore test::graphql::e2e::nestedArg::TestStore\n" +
            "{\n" +
            "}\n" +
            "###Mapping\n" +
            "Mapping test::graphql::e2e::nestedArg::TestMapping\n" +
            "(\n" +
            "  *test::graphql::e2e::nestedArg::Query: GraphQL\n" +
            "  {\n" +
            "    ~typeName [test::graphql::e2e::nestedArg::TestStore] 'Query'\n" +
            "    ~binding test::graphql::e2e::nestedArg::QueryBinding\n" +
            "  }\n" +
            ")\n";

    @Test
    public void testNestedInstanceCreationArgumentWithEmbeddedVariablesAtDepthTwo() throws Exception
    {
        String storePackage = "test::graphql::e2e::nestedArg";

        dataServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"groupedEmployeesProxy\":[{\"firstName\":\"Ada\"}]}}")));

        String selection = "groupedEmployees(^" + storePackage + "::CustomGroup(labels=^" + storePackage + "::Labels(key=$key, value=$value), groupByOperation=" + storePackage + "::GroupByOperation.SUM)){firstName}";
        String query = connectionRuntimeAndQuery(storePackage, dataServer.port(), "key: String[1], value: String[1]",
                storePackage + "::Query.all()\n" +
                "        ->graphFetch(#{" + storePackage + "::Query{" + selection + "}}#)\n" +
                "        ->serialize(#{" + storePackage + "::Query{" + selection + "}}#)");

        SingleExecutionPlan plan = buildPlanForQuery(NESTED_INSTANCE_CREATION_MODEL_AND_BINDING + query, storePackage + "::TestMapping", storePackage + "::TestRuntime");
        Assert.assertNotNull("expected plan generation to succeed for a nested instance-creation QP argument", plan);

        GraphQLExecutionNode graphQLNode = findGraphQLExecutionNode(plan.rootExecutionNode);
        Assert.assertNotNull("expected a GraphQLExecutionNode in the generated plan", graphQLNode);
        String expectedQuery =
                "query($key: String!, $value: String!) {\n" +
                "  groupedEmployeesProxy: groupedEmployees(customGroup: {labels: {key: $key, value: $value}, groupByOperation: SUM}) {\n" +
                "    firstName\n" +
                "  }\n" +
                "}";
        Assert.assertEquals(expectedQuery, graphQLNode.query);

        String result = executePlan(plan, Maps.mutable.with("key", "region", "value", "EMEA"));
        Assert.assertTrue("expected the real Employee data to come back:\n" + result, result.contains("Ada"));
        dataServer.verify(postRequestedFor(urlEqualTo("/graphql"))
                .withRequestBody(containing("\"key\":\"region\""))
                .withRequestBody(containing("\"value\":\"EMEA\"")));
    }

    @Test
    public void testLiteralSingleArgumentQualifiedProperty() throws Exception
    {
        String storePackage = "test::graphql::e2e::qpargsLiteral";
        String grammar = buildQueryWrapperGrammar(storePackage);

        dataServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"employeeProxy\":{\"name\":\"Engineering\"}}}")));

        String query = connectionRuntimeAndQuery(storePackage, dataServer.port(), "",
                storePackage + "::Query.all()\n" +
                "      ->graphFetch(#{" + storePackage + "::Query{employee(5){name}}}#)\n" +
                "      ->serialize(#{" + storePackage + "::Query{employee(5){name}}}#)");

        SingleExecutionPlan plan = buildPlanForQuery(grammar + query, storePackage + "::TestMapping", storePackage + "::TestRuntime");
        String result = executePlan(plan);

        Assert.assertTrue("expected real data for the literal single-argument QP:\n" + result, result.contains("Engineering"));
        dataServer.verify(postRequestedFor(urlEqualTo("/graphql")).withRequestBody(containing("employee(id: $id)")).withRequestBody(containing("\"id\":5")));
    }

    @Test
    public void testVariableSingleArgumentResolvesFreshAtExecutionTime() throws Exception
    {
        String storePackage = "test::graphql::e2e::qpargsVariable";
        String grammar = buildQueryWrapperGrammar(storePackage);

        dataServer.stubFor(post(urlEqualTo("/graphql"))
                .withRequestBody(containing("\"empId\":5"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"employeeProxy\":{\"name\":\"Engineering\"}}}")));
        dataServer.stubFor(post(urlEqualTo("/graphql"))
                .withRequestBody(containing("\"empId\":6"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"employeeProxy\":{\"name\":\"Sales\"}}}")));

        String query = connectionRuntimeAndQuery(storePackage, dataServer.port(), "empId: Integer[1]",
                storePackage + "::Query.all()\n" +
                "        ->graphFetch(#{" + storePackage + "::Query{employee($empId){name}}}#)\n" +
                "        ->serialize(#{" + storePackage + "::Query{employee($empId){name}}}#)");

        SingleExecutionPlan plan = buildPlanForQuery(grammar + query, storePackage + "::TestMapping", storePackage + "::TestRuntime");

        String resultForEngineering = executePlan(plan, java.util.Collections.singletonMap("empId", 5));
        Assert.assertTrue("expected Engineering's data for the first execution:\n" + resultForEngineering, resultForEngineering.contains("Engineering"));
        dataServer.verify(postRequestedFor(urlEqualTo("/graphql")).withRequestBody(containing("\"empId\":5")));

        String resultForSales = executePlan(plan, java.util.Collections.singletonMap("empId", 6));
        Assert.assertTrue("expected Sales' data for the second execution:\n" + resultForSales, resultForSales.contains("Sales"));
        dataServer.verify(postRequestedFor(urlEqualTo("/graphql")).withRequestBody(containing("\"empId\":6")));
    }

    @Test
    public void testMultiArgumentQualifiedPropertyMixedLiteralAndVariable() throws Exception
    {
        String storePackage = "test::graphql::e2e::qpargsMulti";
        String grammar = buildQueryWrapperGrammar(storePackage);

        dataServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"employeeByIdAndRegionProxy\":{\"name\":\"Engineering\"}}}")));

        String query = connectionRuntimeAndQuery(storePackage, dataServer.port(), "region: String[1]",
                storePackage + "::Query.all()\n" +
                "        ->graphFetch(#{" + storePackage + "::Query{employeeByIdAndRegion(5, $region){name}}}#)\n" +
                "        ->serialize(#{" + storePackage + "::Query{employeeByIdAndRegion(5, $region){name}}}#)");

        SingleExecutionPlan plan = buildPlanForQuery(grammar + query, storePackage + "::TestMapping", storePackage + "::TestRuntime");
        String result = executePlan(plan, java.util.Collections.singletonMap("region", "EMEA"));

        Assert.assertTrue("expected real data for the multi-argument QP:\n" + result, result.contains("Engineering"));
        dataServer.verify(postRequestedFor(urlEqualTo("/graphql"))
                .withRequestBody(containing("employeeByIdAndRegion(id: $id, region: $region)"))
                .withRequestBody(containing("\"id\":5"))
                .withRequestBody(containing("\"region\":\"EMEA\"")));
    }

    // employee and employeeByIdAndRegion are two DIFFERENT QPs that both happen to declare an argument
    // named `id` - binding both calls to the SAME shared $id variable is a real, intentional shape (the
    // two properties genuinely want the same value), and must declare the wire variable exactly once, not
    // once per QP.
    @Test
    public void testSharedVariableAcrossDifferentPropertiesDeclaresWireVariableOnce() throws Exception
    {
        String storePackage = "test::graphql::e2e::qpargsSharedVariable";
        String grammar = buildQueryWrapperGrammar(storePackage);

        dataServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"employeeProxy\":{\"name\":\"Engineering\"},\"employeeByIdAndRegionProxy\":{\"name\":\"Engineering\"}}}")));

        String query = connectionRuntimeAndQuery(storePackage, dataServer.port(), "id: Integer[1], region: String[1]",
                storePackage + "::Query.all()\n" +
                "        ->graphFetch(#{" + storePackage + "::Query{employee($id){name},employeeByIdAndRegion($id, $region){name}}}#)\n" +
                "        ->serialize(#{" + storePackage + "::Query{employee($id){name},employeeByIdAndRegion($id, $region){name}}}#)");

        SingleExecutionPlan plan = buildPlanForQuery(grammar + query, storePackage + "::TestMapping", storePackage + "::TestRuntime");
        String result = executePlan(plan, Maps.mutable.with("id", 5, "region", "EMEA"));

        Assert.assertTrue("expected real data back:\n" + result, result.contains("Engineering"));
        dataServer.verify(postRequestedFor(urlEqualTo("/graphql"))
                .withRequestBody(containing("query($id: Int!, $region: String!) {"))
                .withRequestBody(containing("\"id\":5"))
                .withRequestBody(containing("\"region\":\"EMEA\"")));
    }

    // Unlike the shared-$variable case above, two DIFFERENT QPs each independently calling their own `id`
    // argument with a DIFFERENT literal value is a genuine conflict, not a legitimate shared reference -
    // silently colliding them under one `$id` wire variable would corrupt one of the two values.
    @Test
    public void testCollidingLiteralArgumentNamesAcrossDifferentPropertiesFailsLoudly() throws Exception
    {
        String storePackage = "test::graphql::e2e::qpargsCollidingLiterals";
        String grammar = buildQueryWrapperGrammar(storePackage);

        String query = connectionRuntimeAndQuery(storePackage, dataServer.port(), "region: String[1]",
                storePackage + "::Query.all()\n" +
                "        ->graphFetch(#{" + storePackage + "::Query{employee(5){name},employeeByIdAndRegion(6, $region){name}}}#)\n" +
                "        ->serialize(#{" + storePackage + "::Query{employee(5){name},employeeByIdAndRegion(6, $region){name}}}#)");

        Exception exception = Assert.assertThrows(Exception.class, () -> buildPlanForQuery(grammar + query, storePackage + "::TestMapping", storePackage + "::TestRuntime"));
        Assert.assertTrue("expected a clear error about the colliding variable name, not a silently-broken request:\n" + exception.getMessage(),
                exception.getMessage() != null && exception.getMessage().contains("different literal values"));
    }

    // Unlike the colliding-different-literals case above, two DIFFERENT QPs each independently supplying the
    // SAME literal value for their own `id` argument is not a genuine conflict - comparing colliding pushed-
    // down arguments by count alone (rather than by actual value) used to false-positive on this shape.
    @Test
    public void testIdenticalLiteralArgumentNamesAcrossDifferentPropertiesDoesNotFailLoudly() throws Exception
    {
        String storePackage = "test::graphql::e2e::qpargsIdenticalLiterals";
        String grammar = buildQueryWrapperGrammar(storePackage);

        dataServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"employeeProxy\":{\"name\":\"Engineering\"},\"employeeByIdAndRegionProxy\":{\"name\":\"Engineering\"}}}")));

        String query = connectionRuntimeAndQuery(storePackage, dataServer.port(), "region: String[1]",
                storePackage + "::Query.all()\n" +
                "        ->graphFetch(#{" + storePackage + "::Query{employee(5){name},employeeByIdAndRegion(5, $region){name}}}#)\n" +
                "        ->serialize(#{" + storePackage + "::Query{employee(5){name},employeeByIdAndRegion(5, $region){name}}}#)");

        SingleExecutionPlan plan = buildPlanForQuery(grammar + query, storePackage + "::TestMapping", storePackage + "::TestRuntime");
        String result = executePlan(plan, Maps.mutable.with("region", "EMEA"));

        Assert.assertTrue("expected real data back, not a false-positive collision error:\n" + result, result.contains("Engineering"));
        dataServer.verify(postRequestedFor(urlEqualTo("/graphql"))
                .withRequestBody(containing("\"id\":5")));
    }

    @Test
    public void testListMultiplicityQualifiedPropertyReturnsRealList() throws Exception
    {
        String storePackage = "test::graphql::e2e::qpargsList";
        String grammar = buildQueryWrapperGrammar(storePackage);

        dataServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"departmentsByRegionProxy\":[{\"name\":\"Engineering\"},{\"name\":\"Sales\"}]}}")));

        String query = connectionRuntimeAndQuery(storePackage, dataServer.port(), "region: String[1]",
                storePackage + "::Query.all()\n" +
                "        ->graphFetch(#{" + storePackage + "::Query{departmentsByRegion($region){name}}}#)\n" +
                "        ->serialize(#{" + storePackage + "::Query{departmentsByRegion($region){name}}}#)");

        SingleExecutionPlan plan = buildPlanForQuery(grammar + query, storePackage + "::TestMapping", storePackage + "::TestRuntime");
        String result = executePlan(plan, java.util.Collections.singletonMap("region", "EMEA"));

        Assert.assertTrue("expected Engineering in the list result:\n" + result, result.contains("Engineering"));
        Assert.assertTrue("expected Sales in the list result:\n" + result, result.contains("Sales"));
        dataServer.verify(postRequestedFor(urlEqualTo("/graphql"))
                .withRequestBody(containing("departmentsByRegion(region: $region)"))
                .withRequestBody(containing("\"region\":\"EMEA\"")));
    }

    @Test
    public void testCombiningPlainPropertyAndQualifiedPropertyInOneRequest() throws Exception
    {
        String storePackage = "test::graphql::e2e::qpargsCombined";
        String grammar = buildQueryWrapperGrammar(storePackage);

        dataServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"employees\":[{\"firstName\":\"Ada\"}],\"employeeProxy\":{\"name\":\"Engineering\"}}}")));

        String query = connectionRuntimeAndQuery(storePackage, dataServer.port(), "",
                storePackage + "::Query.all()\n" +
                "      ->graphFetch(#{" + storePackage + "::Query{employees{firstName},employee(5){name}}}#)\n" +
                "      ->serialize(#{" + storePackage + "::Query{employees{firstName},employee(5){name}}}#)");

        SingleExecutionPlan plan = buildPlanForQuery(grammar + query, storePackage + "::TestMapping", storePackage + "::TestRuntime");
        String result = executePlan(plan);

        Assert.assertTrue("expected the plain property's data in the result:\n" + result, result.contains("Ada"));
        Assert.assertTrue("expected the qualified property's data in the result:\n" + result, result.contains("Engineering"));
        dataServer.verify(1, postRequestedFor(urlEqualTo("/graphql")));
        dataServer.verify(postRequestedFor(urlEqualTo("/graphql")).withRequestBody(containing("employees")).withRequestBody(containing("employee(id: $id)")));
    }

    private static final String PROXY_NAME_COLLISION_SELECTION_INTROSPECTION_RESPONSE =
            "{\"data\":{\"__schema\":{" +
            "\"queryType\":{\"name\":\"Query\"}," +
            "\"mutationType\":null," +
            "\"types\":[" +
            "{\"kind\":\"OBJECT\",\"name\":\"Query\",\"fields\":[" +
            "  {\"name\":\"employee\",\"type\":{\"kind\":\"OBJECT\",\"name\":\"Employee\"},\"args\":[{\"name\":\"id\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"Int\"}}]}," +
            "  {\"name\":\"employeeProxy\",\"type\":{\"kind\":\"OBJECT\",\"name\":\"Department\"}}" +
            "]}," +
            "{\"kind\":\"OBJECT\",\"name\":\"Employee\",\"fields\":[" +
            "  {\"name\":\"firstName\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}" +
            "]}," +
            "{\"kind\":\"OBJECT\",\"name\":\"Department\",\"fields\":[" +
            "  {\"name\":\"name\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}" +
            "]}" +
            "]" +
            "}}}";

    @Test
    public void testGenuinelyNamedFieldMatchingProxyNamingPatternIsNotDroppedWhenSelectedAlongsideItsLookalikeQp() throws Exception
    {
        introspectionServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(PROXY_NAME_COLLISION_SELECTION_INTROSPECTION_RESPONSE)));

        String storePackage = "test::graphql::e2e::proxyCollisionSelection";
        GraphQLStoreModelGenerationInput input = new GraphQLStoreModelGenerationInput();
        input.url = "http://127.0.0.1:" + introspectionServer.port() + "/graphql";
        input.storePath = storePackage + "::TestStore";
        input.targetPackage = storePackage;
        input.targetBindingPath = storePackage;
        input.targetMappingPath = storePackage + "::TestMapping";

        GraphQLStoreModelGenerationResult generationResult = new GraphQLStoreModelGenerator().generate(input, Identity.getAnonymousIdentity());
        Assert.assertTrue("expected no skipped types for this schema:\n" + generationResult.skippedTypes, generationResult.skippedTypes.isEmpty());
        Assert.assertTrue("expected the employee QP's own generated proxy to be renamed to avoid colliding with the real employeeProxy field:\n" + generationResult.grammar,
                generationResult.grammar.contains("employee(id: Integer[0..1]) { $this.employeeProxy2 }"));

        dataServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"employeeProxy2\":{\"firstName\":\"Ada\"},\"employeeProxy\":{\"name\":\"Engineering\"}}}")));

        String selection = "employee(5){firstName},employeeProxy{name}";
        String query = connectionRuntimeAndQuery(storePackage, dataServer.port(), "",
                storePackage + "::Query.all()\n" +
                "      ->graphFetch(#{" + storePackage + "::Query{" + selection + "}}#)\n" +
                "      ->serialize(#{" + storePackage + "::Query{" + selection + "}}#)");

        SingleExecutionPlan plan = buildPlanForQuery(generationResult.grammar + query, storePackage + "::TestMapping", storePackage + "::TestRuntime");
        GraphQLExecutionNode graphQLNode = findGraphQLExecutionNode(plan.rootExecutionNode);
        Assert.assertTrue("expected the genuinely-named employeeProxy field to still be present in the outgoing selection, not silently dropped as a false-positive generated-proxy sibling:\n" + graphQLNode.query,
                graphQLNode.query.contains("employeeProxy {\n    name\n  }"));

        String result = executePlan(plan);
        Assert.assertTrue("expected the employee QP's own data in the result:\n" + result, result.contains("Ada"));
        Assert.assertTrue("expected the real employeeProxy field's own data in the result, not dropped:\n" + result, result.contains("Engineering"));
    }

    // Distinct department names per path rule out accidental conflation between the nested and
    // qualified-property routes to the same type.
    @Test
    public void testSameReturnTypeReachableViaNestedAndQualifiedPropertyInOneRequest() throws Exception
    {
        String storePackage = "test::graphql::e2e::qpargsDualReachability";
        String grammar = buildQueryWrapperGrammar(storePackage);

        dataServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"employees\":[{\"firstName\":\"Ada\",\"department\":{\"name\":\"Engineering\"}}],\"employeeProxy\":{\"name\":\"Sales\"}}}")));

        String query = connectionRuntimeAndQuery(storePackage, dataServer.port(), "",
                storePackage + "::Query.all()\n" +
                "      ->graphFetch(#{" + storePackage + "::Query{employees{firstName,department{name}},employee(5){name}}}#)\n" +
                "      ->serialize(#{" + storePackage + "::Query{employees{firstName,department{name}},employee(5){name}}}#)");

        SingleExecutionPlan plan = buildPlanForQuery(grammar + query, storePackage + "::TestMapping", storePackage + "::TestRuntime");
        String result = executePlan(plan);

        Assert.assertTrue("expected the nested-path Department name:\n" + result, result.contains("Engineering"));
        Assert.assertTrue("expected the qualified-property-path Department name:\n" + result, result.contains("Sales"));
        dataServer.verify(1, postRequestedFor(urlEqualTo("/graphql")));
    }

    private static final String DOUBLY_NESTED_INTROSPECTION_RESPONSE =
            "{\"data\":{\"__schema\":{" +
            "\"queryType\":{\"name\":\"Query\"}," +
            "\"mutationType\":null," +
            "\"types\":[" +
            "{\"kind\":\"OBJECT\",\"name\":\"Query\",\"fields\":[" +
            "  {\"name\":\"employees\",\"type\":{\"kind\":\"LIST\",\"ofType\":{\"kind\":\"OBJECT\",\"name\":\"Employee\"}}}" +
            "]}," +
            "{\"kind\":\"OBJECT\",\"name\":\"Employee\",\"fields\":[" +
            "  {\"name\":\"firstName\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}," +
            "  {\"name\":\"department\",\"type\":{\"kind\":\"OBJECT\",\"name\":\"Department\"}}" +
            "]}," +
            "{\"kind\":\"OBJECT\",\"name\":\"Department\",\"fields\":[" +
            "  {\"name\":\"name\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}," +
            "  {\"name\":\"manager\",\"type\":{\"kind\":\"OBJECT\",\"name\":\"Manager\"}}" +
            "]}," +
            "{\"kind\":\"OBJECT\",\"name\":\"Manager\",\"fields\":[" +
            "  {\"name\":\"name\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}" +
            "]}" +
            "]" +
            "}}}";

    @Test
    public void testGeneratedDoublyNestedAssociationFoldsIntoOneRequest() throws Exception
    {
        introspectionServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(DOUBLY_NESTED_INTROSPECTION_RESPONSE)));

        GraphQLStoreModelGenerationInput input = new GraphQLStoreModelGenerationInput();
        input.url = "http://127.0.0.1:" + introspectionServer.port() + "/graphql";
        input.storePath = "test::graphql::e2e::doublefolding::TestStore";
        input.targetPackage = "test::graphql::e2e::doublefolding";
        input.targetBindingPath = "test::graphql::e2e::doublefolding";
        input.targetMappingPath = "test::graphql::e2e::doublefolding::TestMapping";

        GraphQLStoreModelGenerationResult generationResult = new GraphQLStoreModelGenerator().generate(input, Identity.getAnonymousIdentity());
        Assert.assertTrue("expected no skipped types for this schema:\n" + generationResult.skippedTypes, generationResult.skippedTypes.isEmpty());

        String connectionRuntimeAndQuery =
                "###Connection\n" +
                "GraphQLConnection test::graphql::e2e::doublefolding::TestConnection\n" +
                "{\n" +
                "  store: test::graphql::e2e::doublefolding::TestStore;\n" +
                "  url: 'http://127.0.0.1:" + dataServer.port() + "/graphql';\n" +
                "}\n" +
                "###Runtime\n" +
                "Runtime test::graphql::e2e::doublefolding::TestRuntime\n" +
                "{\n" +
                "  mappings: [ test::graphql::e2e::doublefolding::TestMapping ];\n" +
                "  connections: [\n" +
                "    test::graphql::e2e::doublefolding::TestStore: [ connection_1: test::graphql::e2e::doublefolding::TestConnection ]\n" +
                "  ];\n" +
                "}\n" +
                "###Pure\n" +
                "function test::graphql::e2e::doublefolding::query(): Any[1]\n" +
                "{\n" +
                "  {|test::graphql::e2e::doublefolding::Query.all()\n" +
                "      ->graphFetch(#{test::graphql::e2e::doublefolding::Query{employees{firstName,department{name,manager{name}}}}}#)\n" +
                "      ->serialize(#{test::graphql::e2e::doublefolding::Query{employees{firstName,department{name,manager{name}}}}}#)}\n" +
                "}\n";

        dataServer.stubFor(post(urlEqualTo("/graphql"))
                .withRequestBody(containing("manager"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"employees\":[{\"firstName\":\"Ada\",\"department\":{\"name\":\"Engineering\",\"manager\":{\"name\":\"Grace\"}}}]}}")));

        SingleExecutionPlan plan = buildPlanForQuery(generationResult.grammar + connectionRuntimeAndQuery, "test::graphql::e2e::doublefolding::TestMapping", "test::graphql::e2e::doublefolding::TestRuntime");
        String result = executePlan(plan);

        Assert.assertTrue("expected the root Employee's own field in the result:\n" + result, result.contains("Ada"));
        Assert.assertTrue("expected the once-folded Department field in the result:\n" + result, result.contains("Engineering"));
        Assert.assertTrue("expected the twice-folded Manager field in the result:\n" + result, result.contains("Grace"));

        dataServer.verify(1, postRequestedFor(urlEqualTo("/graphql")));
    }

    private static final String MULTI_ARG_LIST_INTROSPECTION_RESPONSE =
            "{\"data\":{\"__schema\":{" +
            "\"queryType\":{\"name\":\"Query\"}," +
            "\"mutationType\":null," +
            "\"types\":[" +
            "{\"kind\":\"OBJECT\",\"name\":\"Query\",\"fields\":[" +
            "  {\"name\":\"employeesByFirstNameAndLastName\",\"type\":{\"kind\":\"LIST\",\"ofType\":{\"kind\":\"OBJECT\",\"name\":\"Employee\"}},\"args\":[{\"name\":\"firstName\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}},{\"name\":\"lastName\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}]}" +
            "]}," +
            "{\"kind\":\"OBJECT\",\"name\":\"Employee\",\"fields\":[" +
            "  {\"name\":\"firstName\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}," +
            "  {\"name\":\"lastName\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}" +
            "]}" +
            "]" +
            "}}}";

    @Test
    public void testMultiArgumentListMultiplicityQualifiedProperty() throws Exception
    {
        introspectionServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(MULTI_ARG_LIST_INTROSPECTION_RESPONSE)));

        String storePackage = "test::graphql::e2e::qpargsMultiList";
        GraphQLStoreModelGenerationInput input = new GraphQLStoreModelGenerationInput();
        input.url = "http://127.0.0.1:" + introspectionServer.port() + "/graphql";
        input.storePath = storePackage + "::TestStore";
        input.targetPackage = storePackage;
        input.targetBindingPath = storePackage;
        input.targetMappingPath = storePackage + "::TestMapping";

        GraphQLStoreModelGenerationResult generationResult = new GraphQLStoreModelGenerator().generate(input, Identity.getAnonymousIdentity());
        Assert.assertTrue("expected no skipped types for this schema:\n" + generationResult.skippedTypes, generationResult.skippedTypes.isEmpty());
        Assert.assertTrue("expected the proxy to carry list multiplicity, matching the QP's own return type:\n" + generationResult.grammar,
                generationResult.grammar.contains("employeesByFirstNameAndLastNameProxy: " + storePackage + "::Employee[*]"));

        dataServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"employeesByFirstNameAndLastNameProxy\":[{\"firstName\":\"Ada\",\"lastName\":\"Lovelace\"},{\"firstName\":\"Ada\",\"lastName\":\"Wong\"}]}}")));

        String query = connectionRuntimeAndQuery(storePackage, dataServer.port(), "",
                storePackage + "::Query.all()\n" +
                "      ->graphFetch(#{" + storePackage + "::Query{employeesByFirstNameAndLastName('Ada', 'Lovelace'){firstName,lastName}}}#)\n" +
                "      ->serialize(#{" + storePackage + "::Query{employeesByFirstNameAndLastName('Ada', 'Lovelace'){firstName,lastName}}}#)");

        SingleExecutionPlan plan = buildPlanForQuery(generationResult.grammar + query, storePackage + "::TestMapping", storePackage + "::TestRuntime");
        String result = executePlan(plan);

        Assert.assertTrue("expected both list entries in the result:\n" + result, result.contains("Lovelace") && result.contains("Wong"));
        dataServer.verify(postRequestedFor(urlEqualTo("/graphql"))
                .withRequestBody(containing("employeesByFirstNameAndLastName(firstName: $firstName, lastName: $lastName)"))
                .withRequestBody(containing("\"firstName\":\"Ada\""))
                .withRequestBody(containing("\"lastName\":\"Lovelace\"")));
    }

    private static final String ARGUMENT_TAKING_POLYMORPHIC_PROPERTY_INTROSPECTION_RESPONSE =
            "{\"data\":{\"__schema\":{" +
            "\"queryType\":{\"name\":\"Query\"}," +
            "\"mutationType\":null," +
            "\"types\":[" +
            "{\"kind\":\"OBJECT\",\"name\":\"Query\",\"fields\":[" +
            "  {\"name\":\"employees\",\"type\":{\"kind\":\"LIST\",\"ofType\":{\"kind\":\"OBJECT\",\"name\":\"Employee\"}}}" +
            "]}," +
            "{\"kind\":\"OBJECT\",\"name\":\"Employee\",\"fields\":[" +
            "  {\"name\":\"firstName\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}," +
            "  {\"name\":\"findManager\",\"type\":{\"kind\":\"INTERFACE\",\"name\":\"TestParentObject\"}}" +
            "]}," +
            "{\"kind\":\"INTERFACE\",\"name\":\"TestParentObject\",\"fields\":[" +
            "  {\"name\":\"id\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}" +
            "],\"possibleTypes\":[{\"kind\":\"OBJECT\",\"name\":\"Worker\"},{\"kind\":\"OBJECT\",\"name\":\"Candidate\"}]}," +
            "{\"kind\":\"OBJECT\",\"name\":\"Worker\",\"fields\":[" +
            "  {\"name\":\"id\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}," +
            "  {\"name\":\"employeeId\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}," +
            "  {\"name\":\"key\",\"type\":{\"kind\":\"OBJECT\",\"name\":\"WorkerKey\"}}" +
            "]}," +
            "{\"kind\":\"OBJECT\",\"name\":\"WorkerKey\",\"fields\":[" +
            "  {\"name\":\"key\",\"type\":{\"kind\":\"OBJECT\",\"name\":\"WorkerKeyInner\"}}" +
            "]}," +
            "{\"kind\":\"OBJECT\",\"name\":\"WorkerKeyInner\",\"fields\":[" +
            "  {\"name\":\"employeeNumber\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}," +
            "  {\"name\":\"loginId\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}" +
            "]}," +
            "{\"kind\":\"OBJECT\",\"name\":\"Candidate\",\"fields\":[" +
            "  {\"name\":\"id\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}," +
            "  {\"name\":\"applicationId\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}" +
            "]}" +
            "]" +
            "}}}";

    private static final String NESTED_SUBTYPE_POLYMORPHISM_INTROSPECTION_RESPONSE =
            "{\"data\":{\"__schema\":{" +
            "\"queryType\":{\"name\":\"Query\"}," +
            "\"mutationType\":null," +
            "\"types\":[" +
            "{\"kind\":\"OBJECT\",\"name\":\"Query\",\"fields\":[" +
            "  {\"name\":\"employees\",\"type\":{\"kind\":\"LIST\",\"ofType\":{\"kind\":\"OBJECT\",\"name\":\"Employee\"}}}" +
            "]}," +
            "{\"kind\":\"OBJECT\",\"name\":\"Employee\",\"fields\":[" +
            "  {\"name\":\"firstName\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}," +
            "  {\"name\":\"findManager\",\"type\":{\"kind\":\"INTERFACE\",\"name\":\"TestParentObject\"}}" +
            "]}," +
            "{\"kind\":\"INTERFACE\",\"name\":\"TestParentObject\",\"fields\":[" +
            "  {\"name\":\"id\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}" +
            "],\"possibleTypes\":[{\"kind\":\"OBJECT\",\"name\":\"Worker\"},{\"kind\":\"OBJECT\",\"name\":\"Candidate\"}]}," +
            "{\"kind\":\"OBJECT\",\"name\":\"Worker\",\"fields\":[" +
            "  {\"name\":\"id\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}," +
            "  {\"name\":\"employeeId\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}," +
            "  {\"name\":\"backup\",\"type\":{\"kind\":\"INTERFACE\",\"name\":\"TestParentObject\"}}" +
            "]}," +
            "{\"kind\":\"OBJECT\",\"name\":\"Candidate\",\"fields\":[" +
            "  {\"name\":\"id\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}," +
            "  {\"name\":\"applicationId\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}" +
            "]}" +
            "]" +
            "}}}";

    @Test
    public void testNativeSubTypeSingleBranchNoPlainSiblingResolvesDeeplyNestedFields() throws Exception
    {
        introspectionServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(ARGUMENT_TAKING_POLYMORPHIC_PROPERTY_INTROSPECTION_RESPONSE)));

        String storePackage = "test::graphql::e2e::subtypescoping";
        GraphQLStoreModelGenerationInput input = new GraphQLStoreModelGenerationInput();
        input.url = "http://127.0.0.1:" + introspectionServer.port() + "/graphql";
        input.storePath = storePackage + "::TestStore";
        input.targetPackage = storePackage;
        input.targetBindingPath = storePackage;
        input.targetMappingPath = storePackage + "::TestMapping";

        GraphQLStoreModelGenerationResult generationResult = new GraphQLStoreModelGenerator().generate(input, Identity.getAnonymousIdentity());
        Assert.assertTrue("expected no skipped types for this schema:\n" + generationResult.skippedTypes, generationResult.skippedTypes.isEmpty());

        dataServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"employees\":["
                                + "{\"firstName\":\"Ada\",\"findManager\":{\"__typename\":\"Worker\",\"employeeId\":\"E1\","
                                + "\"key\":{\"key\":{\"employeeNumber\":\"P123\",\"loginId\":\"ada123\"}}}}"
                                + "]}}")));

        String selection = "employees{firstName,findManager->subType(@" + storePackage + "::Worker){employeeId,key{key{employeeNumber,loginId}}}}";
        String query = connectionRuntimeAndQuery(storePackage, dataServer.port(), "",
                storePackage + "::Query.all()\n" +
                "      ->graphFetch(#{" + storePackage + "::Query{" + selection + "}}#)\n" +
                "      ->serialize(#{" + storePackage + "::Query{" + selection + "}}#)");

        SingleExecutionPlan plan = buildPlanForQuery(generationResult.grammar + query, storePackage + "::TestMapping", storePackage + "::TestRuntime");
        String result = executePlan(plan);

        Assert.assertTrue("expected the Worker's own flat field:\n" + result, result.contains("E1"));
        Assert.assertTrue("expected the two-levels-deep nested employeeNumber field:\n" + result, result.contains("P123"));
        Assert.assertTrue("expected the two-levels-deep nested loginId field:\n" + result, result.contains("ada123"));
        dataServer.verify(postRequestedFor(urlEqualTo("/graphql"))
                .withRequestBody(containing("... on Worker"))
                .withRequestBody(containing("key"))
                .withRequestBody(containing("employeeNumber"))
                .withRequestBody(containing("loginId")));
    }

    @Test
    public void testNativeSubTypeCombinedWithPlainSiblingMergesBothSidesFields() throws Exception
    {
        introspectionServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(ARGUMENT_TAKING_POLYMORPHIC_PROPERTY_INTROSPECTION_RESPONSE)));

        String storePackage = "test::graphql::e2e::subtypecombined";
        GraphQLStoreModelGenerationInput input = new GraphQLStoreModelGenerationInput();
        input.url = "http://127.0.0.1:" + introspectionServer.port() + "/graphql";
        input.storePath = storePackage + "::TestStore";
        input.targetPackage = storePackage;
        input.targetBindingPath = storePackage;
        input.targetMappingPath = storePackage + "::TestMapping";

        GraphQLStoreModelGenerationResult generationResult = new GraphQLStoreModelGenerator().generate(input, Identity.getAnonymousIdentity());
        Assert.assertTrue("expected no skipped types for this schema:\n" + generationResult.skippedTypes, generationResult.skippedTypes.isEmpty());

        dataServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"employees\":["
                                + "{\"firstName\":\"Ada\",\"findManager\":{\"__typename\":\"Worker\",\"id\":\"m1\",\"employeeId\":\"E1\"}}"
                                + "]}}")));

        String selection = "employees{firstName,findManager{id},findManager->subType(@" + storePackage + "::Worker){employeeId}}";
        String query = connectionRuntimeAndQuery(storePackage, dataServer.port(), "",
                storePackage + "::Query.all()\n" +
                "      ->graphFetch(#{" + storePackage + "::Query{" + selection + "}}#)\n" +
                "      ->serialize(#{" + storePackage + "::Query{" + selection + "}}#)");

        SingleExecutionPlan plan = buildPlanForQuery(generationResult.grammar + query, storePackage + "::TestMapping", storePackage + "::TestRuntime");
        String result = executePlan(plan);

        Assert.assertTrue("expected the plain-selected base field 'id':\n" + result, result.contains("m1"));
        Assert.assertTrue("expected the subType-selected Worker field 'employeeId':\n" + result, result.contains("E1"));
    }

    @Test
    public void testNativeSubTypeCombinedWithMultipleDistinctSubtypeBranchesEachResolveOwnFieldPlusCommon() throws Exception
    {
        introspectionServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(ARGUMENT_TAKING_POLYMORPHIC_PROPERTY_INTROSPECTION_RESPONSE)));

        String storePackage = "test::graphql::e2e::subtypenway";
        GraphQLStoreModelGenerationInput input = new GraphQLStoreModelGenerationInput();
        input.url = "http://127.0.0.1:" + introspectionServer.port() + "/graphql";
        input.storePath = storePackage + "::TestStore";
        input.targetPackage = storePackage;
        input.targetBindingPath = storePackage;
        input.targetMappingPath = storePackage + "::TestMapping";

        GraphQLStoreModelGenerationResult generationResult = new GraphQLStoreModelGenerator().generate(input, Identity.getAnonymousIdentity());
        Assert.assertTrue("expected no skipped types for this schema:\n" + generationResult.skippedTypes, generationResult.skippedTypes.isEmpty());

        dataServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"employees\":["
                                + "{\"firstName\":\"Ada\",\"findManager\":{\"__typename\":\"Worker\",\"id\":\"m1\",\"employeeId\":\"E1\"}},"
                                + "{\"firstName\":\"Bo\",\"findManager\":{\"__typename\":\"Candidate\",\"id\":\"m2\",\"applicationId\":\"A1\"}}"
                                + "]}}")));

        String selection = "employees{firstName,findManager{id},findManager->subType(@" + storePackage + "::Worker){employeeId},findManager->subType(@" + storePackage + "::Candidate){applicationId}}";
        String query = connectionRuntimeAndQuery(storePackage, dataServer.port(), "",
                storePackage + "::Query.all()\n" +
                "      ->graphFetch(#{" + storePackage + "::Query{" + selection + "}}#)\n" +
                "      ->serialize(#{" + storePackage + "::Query{" + selection + "}}#)");

        SingleExecutionPlan plan = buildPlanForQuery(generationResult.grammar + query, storePackage + "::TestMapping", storePackage + "::TestRuntime");
        String result = executePlan(plan);

        JsonNode employees = new ObjectMapper().readTree(result).at("/values/employees");
        JsonNode workerManager = employees.get(0).get("findManager");
        JsonNode candidateManager = employees.get(1).get("findManager");

        Assert.assertEquals("Worker row's common field 'id':\n" + result, "m1", workerManager.get("id").asText());
        Assert.assertEquals("Worker row's own field 'employeeId':\n" + result, "E1", workerManager.get("employeeId").asText());
        Assert.assertFalse("Worker row must not see Candidate's own field 'applicationId':\n" + result, workerManager.has("applicationId"));

        Assert.assertEquals("Candidate row's common field 'id':\n" + result, "m2", candidateManager.get("id").asText());
        Assert.assertEquals("Candidate row's own field 'applicationId':\n" + result, "A1", candidateManager.get("applicationId").asText());
        Assert.assertFalse("Candidate row must not see Worker's own field 'employeeId':\n" + result, candidateManager.has("employeeId"));

        // The generated GraphQL query requests 'id' once, outside either type condition, alongside separate
        // inline fragments per subtype - the natural GraphQL idiom for a common field plus type-specific ones.
        dataServer.verify(postRequestedFor(urlEqualTo("/graphql"))
                .withRequestBody(containing("... on Worker"))
                .withRequestBody(containing("... on Candidate"))
                .withRequestBody(containing("employeeId"))
                .withRequestBody(containing("applicationId")));
    }

    @Test
    public void testNestedSecondLevelPolymorphismInsideASubtypeBranchResolvesBothLevels() throws Exception
    {
        introspectionServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(NESTED_SUBTYPE_POLYMORPHISM_INTROSPECTION_RESPONSE)));

        String storePackage = "test::graphql::e2e::subtypenested";
        GraphQLStoreModelGenerationInput input = new GraphQLStoreModelGenerationInput();
        input.url = "http://127.0.0.1:" + introspectionServer.port() + "/graphql";
        input.storePath = storePackage + "::TestStore";
        input.targetPackage = storePackage;
        input.targetBindingPath = storePackage;
        input.targetMappingPath = storePackage + "::TestMapping";

        GraphQLStoreModelGenerationResult generationResult = new GraphQLStoreModelGenerator().generate(input, Identity.getAnonymousIdentity());
        Assert.assertTrue("expected no skipped types for this schema:\n" + generationResult.skippedTypes, generationResult.skippedTypes.isEmpty());

        dataServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"employees\":["
                                + "{\"firstName\":\"Ada\",\"findManager\":{\"__typename\":\"Worker\",\"id\":\"m1\",\"employeeId\":\"E1\","
                                + "\"backup\":{\"__typename\":\"Candidate\",\"id\":\"m2\",\"applicationId\":\"A1\"}}}"
                                + "]}}")));

        String selection = "employees{firstName,findManager->subType(@" + storePackage + "::Worker){employeeId,backup{id},backup->subType(@" + storePackage + "::Candidate){applicationId}}}";
        String query = connectionRuntimeAndQuery(storePackage, dataServer.port(), "",
                storePackage + "::Query.all()\n" +
                "      ->graphFetch(#{" + storePackage + "::Query{" + selection + "}}#)\n" +
                "      ->serialize(#{" + storePackage + "::Query{" + selection + "}}#)");

        SingleExecutionPlan plan = buildPlanForQuery(generationResult.grammar + query, storePackage + "::TestMapping", storePackage + "::TestRuntime");
        String result = executePlan(plan);

        JsonNode backup = new ObjectMapper().readTree(result)
                .at("/values/employees").get(0).get("findManager").get("backup");
        Assert.assertEquals("expected backup's common field 'id':\n" + result, "m2", backup.get("id").asText());
        Assert.assertEquals("expected backup's own field 'applicationId':\n" + result, "A1", backup.get("applicationId").asText());
    }

    private static final String MULTI_LEVEL_INTERFACE_HIERARCHY_INTROSPECTION_RESPONSE =
            "{\"data\":{\"__schema\":{" +
            "\"queryType\":{\"name\":\"Query\"}," +
            "\"mutationType\":null," +
            "\"types\":[" +
            "{\"kind\":\"OBJECT\",\"name\":\"Query\",\"fields\":[" +
            "  {\"name\":\"employees\",\"type\":{\"kind\":\"LIST\",\"ofType\":{\"kind\":\"OBJECT\",\"name\":\"Employee\"}}}" +
            "]}," +
            "{\"kind\":\"OBJECT\",\"name\":\"Employee\",\"fields\":[" +
            "  {\"name\":\"firstName\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}," +
            "  {\"name\":\"findManager\",\"type\":{\"kind\":\"INTERFACE\",\"name\":\"TestParentObject\"}}" +
            "]}," +
            "{\"kind\":\"INTERFACE\",\"name\":\"TestParentObject\",\"fields\":[" +
            "  {\"name\":\"id\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}" +
            "],\"possibleTypes\":[{\"kind\":\"OBJECT\",\"name\":\"Candidate\"},{\"kind\":\"OBJECT\",\"name\":\"SeniorWorker\"}]}," +
            "{\"kind\":\"INTERFACE\",\"name\":\"WorkerBase\",\"interfaces\":[{\"kind\":\"INTERFACE\",\"name\":\"TestParentObject\"}],\"fields\":[" +
            "  {\"name\":\"id\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}," +
            "  {\"name\":\"employeeId\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}" +
            "],\"possibleTypes\":[{\"kind\":\"OBJECT\",\"name\":\"SeniorWorker\"}]}," +
            "{\"kind\":\"OBJECT\",\"name\":\"SeniorWorker\",\"interfaces\":[{\"kind\":\"INTERFACE\",\"name\":\"WorkerBase\"},{\"kind\":\"INTERFACE\",\"name\":\"TestParentObject\"}],\"fields\":[" +
            "  {\"name\":\"id\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}," +
            "  {\"name\":\"employeeId\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}," +
            "  {\"name\":\"bonus\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}" +
            "]}," +
            "{\"kind\":\"OBJECT\",\"name\":\"Candidate\",\"interfaces\":[{\"kind\":\"INTERFACE\",\"name\":\"TestParentObject\"}],\"fields\":[" +
            "  {\"name\":\"id\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}," +
            "  {\"name\":\"applicationId\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}" +
            "]}" +
            "]" +
            "}}}";

    @Test
    public void testNativeSubTypeResolvesTransitivelyInheritedFieldThroughMultiLevelInterfaceHierarchy() throws Exception
    {
        introspectionServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(MULTI_LEVEL_INTERFACE_HIERARCHY_INTROSPECTION_RESPONSE)));

        String storePackage = "test::graphql::e2e::multilevel";
        GraphQLStoreModelGenerationInput input = new GraphQLStoreModelGenerationInput();
        input.url = "http://127.0.0.1:" + introspectionServer.port() + "/graphql";
        input.storePath = storePackage + "::TestStore";
        input.targetPackage = storePackage;
        input.targetBindingPath = storePackage;
        input.targetMappingPath = storePackage + "::TestMapping";

        GraphQLStoreModelGenerationResult generationResult = new GraphQLStoreModelGenerator().generate(input, Identity.getAnonymousIdentity());
        Assert.assertTrue("expected no skipped types for this schema:\n" + generationResult.skippedTypes, generationResult.skippedTypes.isEmpty());
        Assert.assertTrue("expected SeniorWorker to extend the intermediate WorkerBase, not TestParentObject directly:\n" + generationResult.grammar,
                generationResult.grammar.contains(storePackage + "::SeniorWorker extends " + storePackage + "::WorkerBase"));

        dataServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"employees\":["
                                + "{\"firstName\":\"Ada\",\"findManager\":{\"__typename\":\"SeniorWorker\",\"id\":\"m1\",\"employeeId\":\"E1\",\"bonus\":\"B1\"}}"
                                + "]}}")));

        String selection = "employees{firstName,findManager{id},findManager->subType(@" + storePackage + "::SeniorWorker){employeeId,bonus}}";
        String query = connectionRuntimeAndQuery(storePackage, dataServer.port(), "",
                storePackage + "::Query.all()\n" +
                "      ->graphFetch(#{" + storePackage + "::Query{" + selection + "}}#)\n" +
                "      ->serialize(#{" + storePackage + "::Query{" + selection + "}}#)");

        SingleExecutionPlan plan = buildPlanForQuery(generationResult.grammar + query, storePackage + "::TestMapping", storePackage + "::TestRuntime");
        String result = executePlan(plan);

        JsonNode manager = new ObjectMapper().readTree(result)
                .at("/values/employees").get(0).get("findManager");
        Assert.assertEquals("expected the common TestParentObject field 'id':\n" + result, "m1", manager.get("id").asText());
        Assert.assertEquals("expected the intermediate WorkerBase field 'employeeId', resolved transitively through SeniorWorker's own classMapping:\n" + result, "E1", manager.get("employeeId").asText());
        Assert.assertEquals("expected SeniorWorker's own field 'bonus':\n" + result, "B1", manager.get("bonus").asText());
    }

    @Test
    public void testNativeSubTypeMixedSubtypeDatasetSkipsNonMatchingRows() throws Exception
    {
        introspectionServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(ARGUMENT_TAKING_POLYMORPHIC_PROPERTY_INTROSPECTION_RESPONSE)));

        String storePackage = "test::graphql::e2e::subtypescoping";
        GraphQLStoreModelGenerationInput input = new GraphQLStoreModelGenerationInput();
        input.url = "http://127.0.0.1:" + introspectionServer.port() + "/graphql";
        input.storePath = storePackage + "::TestStore";
        input.targetPackage = storePackage;
        input.targetBindingPath = storePackage;
        input.targetMappingPath = storePackage + "::TestMapping";

        GraphQLStoreModelGenerationResult generationResult = new GraphQLStoreModelGenerator().generate(input, Identity.getAnonymousIdentity());
        Assert.assertTrue("expected no skipped types for this schema:\n" + generationResult.skippedTypes, generationResult.skippedTypes.isEmpty());

        dataServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"employees\":["
                                + "{\"firstName\":\"Ada\",\"findManager\":{\"__typename\":\"Worker\",\"employeeId\":\"E1\","
                                + "\"key\":{\"key\":{\"employeeNumber\":\"P123\",\"loginId\":\"ada123\"}}}},"
                                + "{\"firstName\":\"Bo\",\"findManager\":{\"__typename\":\"Candidate\",\"applicationId\":\"A1\"}}"
                                + "]}}")));

        String selection = "employees{firstName,findManager->subType(@" + storePackage + "::Worker){employeeId,key{key{employeeNumber,loginId}}}}";
        String query = connectionRuntimeAndQuery(storePackage, dataServer.port(), "",
                storePackage + "::Query.all()\n" +
                "      ->graphFetch(#{" + storePackage + "::Query{" + selection + "}}#)\n" +
                "      ->serialize(#{" + storePackage + "::Query{" + selection + "}}#)");

        SingleExecutionPlan plan = buildPlanForQuery(generationResult.grammar + query, storePackage + "::TestMapping", storePackage + "::TestRuntime");
        String result = executePlan(plan);

        JsonNode employees = new ObjectMapper().readTree(result).at("/values/employees");
        Assert.assertEquals("expected both rows to be present:\n" + result, 2, employees.size());

        JsonNode ada = employees.get(0);
        Assert.assertEquals("Ada", ada.get("firstName").asText());
        Assert.assertTrue("expected Ada's findManager to be a well-formed object:\n" + result, ada.get("findManager").isObject());
        Assert.assertEquals("expected Ada's Worker data to resolve correctly:\n" + result, "E1", ada.get("findManager").get("employeeId").asText());

        JsonNode bo = employees.get(1);
        Assert.assertEquals("Bo", bo.get("firstName").asText());
        Assert.assertTrue("expected Bo's row to still be present (not dropped):\n" + result, bo.has("findManager"));
        Assert.assertTrue("expected Bo's findManager to be null (no Worker-typed manager):\n" + result, bo.get("findManager").isNull());
    }

    private static final String ROOT_POLYMORPHIC_PROPERTY_INTROSPECTION_RESPONSE =
            "{\"data\":{\"__schema\":{" +
            "\"queryType\":{\"name\":\"Query\"}," +
            "\"mutationType\":null," +
            "\"types\":[" +
            "{\"kind\":\"OBJECT\",\"name\":\"Query\",\"fields\":[" +
            "  {\"name\":\"findManager\",\"type\":{\"kind\":\"INTERFACE\",\"name\":\"TestParentObject\"},\"args\":[{\"name\":\"id\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"Int\"}}]}" +
            "]}," +
            "{\"kind\":\"INTERFACE\",\"name\":\"TestParentObject\",\"fields\":[" +
            "  {\"name\":\"id\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}" +
            "],\"possibleTypes\":[{\"kind\":\"OBJECT\",\"name\":\"Worker\"},{\"kind\":\"OBJECT\",\"name\":\"Candidate\"}]}," +
            "{\"kind\":\"OBJECT\",\"name\":\"Worker\",\"fields\":[" +
            "  {\"name\":\"id\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}," +
            "  {\"name\":\"employeeId\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}" +
            "]}," +
            "{\"kind\":\"OBJECT\",\"name\":\"Candidate\",\"fields\":[" +
            "  {\"name\":\"id\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}," +
            "  {\"name\":\"applicationId\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}" +
            "]}" +
            "]" +
            "}}}";

    @Test
    public void testPolymorphicQueryRootFieldWithProxyQpGeneratesAndCompilesSuccessfully() throws Exception
    {
        introspectionServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(ROOT_POLYMORPHIC_PROPERTY_INTROSPECTION_RESPONSE)));

        String storePackage = "test::graphql::e2e::rootpoly";
        GraphQLStoreModelGenerationInput input = new GraphQLStoreModelGenerationInput();
        input.url = "http://127.0.0.1:" + introspectionServer.port() + "/graphql";
        input.storePath = storePackage + "::TestStore";
        input.targetPackage = storePackage;
        input.targetBindingPath = storePackage;
        input.targetMappingPath = storePackage + "::TestMapping";

        GraphQLStoreModelGenerationResult generationResult = new GraphQLStoreModelGenerator().generate(input, Identity.getAnonymousIdentity());
        Assert.assertTrue("expected no skipped types for this schema:\n" + generationResult.skippedTypes, generationResult.skippedTypes.isEmpty());

        dataServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"findManagerProxy\":{\"__typename\":\"Worker\",\"id\":\"m1\"}}}")));

        String selection = "findManager(5){id}";
        String query = connectionRuntimeAndQuery(storePackage, dataServer.port(), "",
                storePackage + "::Query.all()\n" +
                "      ->graphFetch(#{" + storePackage + "::Query{" + selection + "}}#)\n" +
                "      ->serialize(#{" + storePackage + "::Query{" + selection + "}}#)");

        SingleExecutionPlan plan = buildPlanForQuery(generationResult.grammar + query, storePackage + "::TestMapping", storePackage + "::TestRuntime");
        String result = executePlan(plan);

        Assert.assertTrue("expected the common TestParentObject field 'id' to resolve for a polymorphic, argument-taking Query-root field queried with no subType() branch:\n" + result, result.contains("m1"));
    }

    @Test
    public void testPolymorphicQueryRootFieldWithProxyQpAndSubTypeNarrowingResolvesCorrectly() throws Exception
    {
        introspectionServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(ROOT_POLYMORPHIC_PROPERTY_INTROSPECTION_RESPONSE)));

        String storePackage = "test::graphql::e2e::rootpolysubtype";
        GraphQLStoreModelGenerationInput input = new GraphQLStoreModelGenerationInput();
        input.url = "http://127.0.0.1:" + introspectionServer.port() + "/graphql";
        input.storePath = storePackage + "::TestStore";
        input.targetPackage = storePackage;
        input.targetBindingPath = storePackage;
        input.targetMappingPath = storePackage + "::TestMapping";

        GraphQLStoreModelGenerationResult generationResult = new GraphQLStoreModelGenerator().generate(input, Identity.getAnonymousIdentity());
        Assert.assertTrue("expected no skipped types for this schema:\n" + generationResult.skippedTypes, generationResult.skippedTypes.isEmpty());

        dataServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"findManagerProxy\":{\"__typename\":\"Worker\",\"id\":\"m1\",\"employeeId\":\"E1\"}}}")));

        String selection = "findManager(5)->subType(@" + storePackage + "::Worker){id,employeeId}";
        String query = connectionRuntimeAndQuery(storePackage, dataServer.port(), "",
                storePackage + "::Query.all()\n" +
                "      ->graphFetch(#{" + storePackage + "::Query{" + selection + "}}#)\n" +
                "      ->serialize(#{" + storePackage + "::Query{" + selection + "}}#)");

        SingleExecutionPlan plan = buildPlanForQuery(generationResult.grammar + query, storePackage + "::TestMapping", storePackage + "::TestRuntime");
        String result = executePlan(plan);

        Assert.assertTrue("expected the common TestParentObject field 'id' to resolve:\n" + result, result.contains("m1"));
        Assert.assertTrue("expected the Worker-only field 'employeeId' to resolve via subType() narrowing on an argument-taking Query-root field:\n" + result, result.contains("E1"));
    }

    @Test
    public void testUnrequestedPolymorphicRootFieldIsOmittedEntirelyFromTheRequest() throws Exception
    {
        String schemaWithSiblingScalarField =
                "{\"data\":{\"__schema\":{" +
                "\"queryType\":{\"name\":\"Query\"}," +
                "\"mutationType\":null," +
                "\"types\":[" +
                "{\"kind\":\"OBJECT\",\"name\":\"Query\",\"fields\":[" +
                "  {\"name\":\"simpleField\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}," +
                "  {\"name\":\"findManager\",\"type\":{\"kind\":\"INTERFACE\",\"name\":\"TestParentObject\"},\"args\":[{\"name\":\"id\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"Int\"}}]}" +
                "]}," +
                "{\"kind\":\"INTERFACE\",\"name\":\"TestParentObject\",\"fields\":[" +
                "  {\"name\":\"id\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}" +
                "],\"possibleTypes\":[{\"kind\":\"OBJECT\",\"name\":\"Worker\"}]}," +
                "{\"kind\":\"OBJECT\",\"name\":\"Worker\",\"fields\":[" +
                "  {\"name\":\"id\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}," +
                "  {\"name\":\"employeeId\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}" +
                "]}" +
                "]" +
                "}}}";

        introspectionServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(schemaWithSiblingScalarField)));

        String storePackage = "test::graphql::e2e::exploreUnrequestedPoly";
        GraphQLStoreModelGenerationInput input = new GraphQLStoreModelGenerationInput();
        input.url = "http://127.0.0.1:" + introspectionServer.port() + "/graphql";
        input.storePath = storePackage + "::TestStore";
        input.targetPackage = storePackage;
        input.targetBindingPath = storePackage;
        input.targetMappingPath = storePackage + "::TestMapping";

        GraphQLStoreModelGenerationResult generationResult = new GraphQLStoreModelGenerator().generate(input, Identity.getAnonymousIdentity());
        Assert.assertTrue("expected no skipped types for this schema:\n" + generationResult.skippedTypes, generationResult.skippedTypes.isEmpty());

        dataServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"simpleField\":\"hello\"}}")));

        String query = connectionRuntimeAndQuery(storePackage, dataServer.port(), "",
                storePackage + "::Query.all()\n" +
                "      ->graphFetch(#{" + storePackage + "::Query{simpleField}}#)\n" +
                "      ->serialize(#{" + storePackage + "::Query{simpleField}}#)");

        SingleExecutionPlan plan = buildPlanForQuery(generationResult.grammar + query, storePackage + "::TestMapping", storePackage + "::TestRuntime");
        GraphQLExecutionNode graphQLNode = findGraphQLExecutionNode(plan.rootExecutionNode);
        Assert.assertEquals("expected findManagerProxy to be entirely absent, not rendered with an empty or placeholder selection:\n" + graphQLNode.query,
                "query {\n  simpleField\n}", graphQLNode.query);

        String result = executePlan(plan);
        Assert.assertTrue("expected the real simpleField value to come back even though findManagerProxy was never fetched:\n" + result, result.contains("hello"));
    }

    @Test
    public void testUnrequestedNestedPolymorphicPropertyIsOmittedEntirelyFromTheRequest() throws Exception
    {
        String schemaWithNestedPolymorphicProperty =
                "{\"data\":{\"__schema\":{" +
                "\"queryType\":{\"name\":\"Query\"}," +
                "\"mutationType\":null," +
                "\"types\":[" +
                "{\"kind\":\"OBJECT\",\"name\":\"Query\",\"fields\":[" +
                "  {\"name\":\"fetchManager\",\"type\":{\"kind\":\"OBJECT\",\"name\":\"Manager\"},\"args\":[{\"name\":\"id\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"Int\"}}]}" +
                "]}," +
                "{\"kind\":\"OBJECT\",\"name\":\"Manager\",\"fields\":[" +
                "  {\"name\":\"firstName\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}," +
                "  {\"name\":\"assignedTo\",\"type\":{\"kind\":\"INTERFACE\",\"name\":\"StaffMember\"}}" +
                "]}," +
                "{\"kind\":\"INTERFACE\",\"name\":\"StaffMember\",\"fields\":[" +
                "  {\"name\":\"id\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}" +
                "],\"possibleTypes\":[{\"kind\":\"OBJECT\",\"name\":\"Worker\"}]}," +
                "{\"kind\":\"OBJECT\",\"name\":\"Worker\",\"fields\":[" +
                "  {\"name\":\"id\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}," +
                "  {\"name\":\"employeeId\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}" +
                "]}" +
                "]" +
                "}}}";

        introspectionServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(schemaWithNestedPolymorphicProperty)));

        String storePackage = "test::graphql::e2e::exploreNestedUnrequestedPoly";
        GraphQLStoreModelGenerationInput input = new GraphQLStoreModelGenerationInput();
        input.url = "http://127.0.0.1:" + introspectionServer.port() + "/graphql";
        input.storePath = storePackage + "::TestStore";
        input.targetPackage = storePackage;
        input.targetBindingPath = storePackage;
        input.targetMappingPath = storePackage + "::TestMapping";

        GraphQLStoreModelGenerationResult generationResult = new GraphQLStoreModelGenerator().generate(input, Identity.getAnonymousIdentity());
        Assert.assertTrue("expected no skipped types for this schema:\n" + generationResult.skippedTypes, generationResult.skippedTypes.isEmpty());

        dataServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"fetchManagerProxy\":{\"firstName\":\"Dana\"}}}")));

        String selection = "fetchManager(5){firstName}";
        String query = connectionRuntimeAndQuery(storePackage, dataServer.port(), "",
                storePackage + "::Query.all()\n" +
                "      ->graphFetch(#{" + storePackage + "::Query{" + selection + "}}#)\n" +
                "      ->serialize(#{" + storePackage + "::Query{" + selection + "}}#)");

        SingleExecutionPlan plan = buildPlanForQuery(generationResult.grammar + query, storePackage + "::TestMapping", storePackage + "::TestRuntime");
        GraphQLExecutionNode graphQLNode = findGraphQLExecutionNode(plan.rootExecutionNode);
        Assert.assertEquals("expected assignedToProxy to be entirely absent from fetchManagerProxy's own selection:\n" + graphQLNode.query,
                "query($id: Int!) {\n  fetchManagerProxy: fetchManager(id: $id) {\n    firstName\n  }\n}", graphQLNode.query);

        String result = executePlan(plan);
        Assert.assertTrue("expected the real firstName value to come back even though assignedTo was never fetched:\n" + result, result.contains("Dana"));
    }

    // Empirically answers whether the shared graphFetch router's constraint-widening can cascade: an
    // unrequested polymorphic Query-root property (`manager`) whose OWN possible subtype (`RegularManager`)
    // independently has a further unrequested polymorphic property (`delegate`). If the router widens
    // `manager` to a flat, childless leaf (no further recursive widening into a freshly-injected node's own
    // constraints), `delegate` never enters the tree at all and this reduces to the single-level case. If it
    // does recurse, this proves buildFieldForGroup's own recursion (via getSubTreeFields) correctly drops
    // the whole chain rather than leaving a dangling, now also-empty `managerProxy` behind. Either way, the
    // end result must be the same: nothing from `manager`/`delegate` appears in the request.
    @Test
    public void testUnrequestedPolymorphicPropertyWithItsOwnNestedUnrequestedPolymorphicPropertyIsFullyOmitted() throws Exception
    {
        String schemaWithPotentialCascade =
                "{\"data\":{\"__schema\":{" +
                "\"queryType\":{\"name\":\"Query\"}," +
                "\"mutationType\":null," +
                "\"types\":[" +
                "{\"kind\":\"OBJECT\",\"name\":\"Query\",\"fields\":[" +
                "  {\"name\":\"simpleField\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}," +
                "  {\"name\":\"manager\",\"type\":{\"kind\":\"INTERFACE\",\"name\":\"ManagerInterface\"}}" +
                "]}," +
                "{\"kind\":\"INTERFACE\",\"name\":\"ManagerInterface\",\"fields\":[" +
                "  {\"name\":\"id\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}" +
                "],\"possibleTypes\":[{\"kind\":\"OBJECT\",\"name\":\"RegularManager\"}]}," +
                "{\"kind\":\"OBJECT\",\"name\":\"RegularManager\",\"fields\":[" +
                "  {\"name\":\"id\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}," +
                "  {\"name\":\"delegate\",\"type\":{\"kind\":\"INTERFACE\",\"name\":\"StaffMember\"}}" +
                "]}," +
                "{\"kind\":\"INTERFACE\",\"name\":\"StaffMember\",\"fields\":[" +
                "  {\"name\":\"id\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}" +
                "],\"possibleTypes\":[{\"kind\":\"OBJECT\",\"name\":\"Worker\"}]}," +
                "{\"kind\":\"OBJECT\",\"name\":\"Worker\",\"fields\":[" +
                "  {\"name\":\"id\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}," +
                "  {\"name\":\"employeeId\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}" +
                "]}" +
                "]" +
                "}}}";

        introspectionServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(schemaWithPotentialCascade)));

        String storePackage = "test::graphql::e2e::exploreCascadingUnrequestedPoly";
        GraphQLStoreModelGenerationInput input = new GraphQLStoreModelGenerationInput();
        input.url = "http://127.0.0.1:" + introspectionServer.port() + "/graphql";
        input.storePath = storePackage + "::TestStore";
        input.targetPackage = storePackage;
        input.targetBindingPath = storePackage;
        input.targetMappingPath = storePackage + "::TestMapping";

        GraphQLStoreModelGenerationResult generationResult = new GraphQLStoreModelGenerator().generate(input, Identity.getAnonymousIdentity());
        Assert.assertTrue("expected no skipped types for this schema:\n" + generationResult.skippedTypes, generationResult.skippedTypes.isEmpty());

        dataServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"simpleField\":\"hello\"}}")));

        String query = connectionRuntimeAndQuery(storePackage, dataServer.port(), "",
                storePackage + "::Query.all()\n" +
                "      ->graphFetch(#{" + storePackage + "::Query{simpleField}}#)\n" +
                "      ->serialize(#{" + storePackage + "::Query{simpleField}}#)");

        SingleExecutionPlan plan = buildPlanForQuery(generationResult.grammar + query, storePackage + "::TestMapping", storePackage + "::TestRuntime");
        GraphQLExecutionNode graphQLNode = findGraphQLExecutionNode(plan.rootExecutionNode);
        Assert.assertEquals("expected manager/delegate to be fully absent regardless of whether the router's widening cascades into freshly-injected nodes:\n" + graphQLNode.query,
                "query {\n  simpleField\n}", graphQLNode.query);

        String result = executePlan(plan);
        Assert.assertTrue("expected the real simpleField value to come back:\n" + result, result.contains("hello"));
    }

    // Same drop behavior must apply to a GraphQL UNION-kind unrequested property, not just INTERFACE - both
    // map to the same generated Pure Class representation, so buildFieldForGroup's instanceOf(Class) check
    // should cover both uniformly.
    @Test
    public void testUnrequestedUnionTypedRootFieldIsOmittedEntirelyFromTheRequest() throws Exception
    {
        String schemaWithUnionField =
                "{\"data\":{\"__schema\":{" +
                "\"queryType\":{\"name\":\"Query\"}," +
                "\"mutationType\":null," +
                "\"types\":[" +
                "{\"kind\":\"OBJECT\",\"name\":\"Query\",\"fields\":[" +
                "  {\"name\":\"simpleField\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}," +
                "  {\"name\":\"pick\",\"type\":{\"kind\":\"UNION\",\"name\":\"PickResult\"}}" +
                "]}," +
                "{\"kind\":\"UNION\",\"name\":\"PickResult\",\"possibleTypes\":[{\"kind\":\"OBJECT\",\"name\":\"Worker\"},{\"kind\":\"OBJECT\",\"name\":\"Contractor\"}]}," +
                "{\"kind\":\"OBJECT\",\"name\":\"Worker\",\"fields\":[" +
                "  {\"name\":\"id\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}," +
                "  {\"name\":\"employeeId\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}" +
                "]}," +
                "{\"kind\":\"OBJECT\",\"name\":\"Contractor\",\"fields\":[" +
                "  {\"name\":\"id\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}," +
                "  {\"name\":\"contractCode\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}" +
                "]}" +
                "]" +
                "}}}";

        introspectionServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(schemaWithUnionField)));

        String storePackage = "test::graphql::e2e::exploreUnrequestedUnion";
        GraphQLStoreModelGenerationInput input = new GraphQLStoreModelGenerationInput();
        input.url = "http://127.0.0.1:" + introspectionServer.port() + "/graphql";
        input.storePath = storePackage + "::TestStore";
        input.targetPackage = storePackage;
        input.targetBindingPath = storePackage;
        input.targetMappingPath = storePackage + "::TestMapping";

        GraphQLStoreModelGenerationResult generationResult = new GraphQLStoreModelGenerator().generate(input, Identity.getAnonymousIdentity());
        Assert.assertTrue("expected no skipped types for this schema:\n" + generationResult.skippedTypes, generationResult.skippedTypes.isEmpty());

        dataServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"simpleField\":\"hello\"}}")));

        String query = connectionRuntimeAndQuery(storePackage, dataServer.port(), "",
                storePackage + "::Query.all()\n" +
                "      ->graphFetch(#{" + storePackage + "::Query{simpleField}}#)\n" +
                "      ->serialize(#{" + storePackage + "::Query{simpleField}}#)");

        SingleExecutionPlan plan = buildPlanForQuery(generationResult.grammar + query, storePackage + "::TestMapping", storePackage + "::TestRuntime");
        GraphQLExecutionNode graphQLNode = findGraphQLExecutionNode(plan.rootExecutionNode);
        Assert.assertEquals("expected pickProxy (UNION-typed) to be entirely absent, same as an INTERFACE-typed one:\n" + graphQLNode.query,
                "query {\n  simpleField\n}", graphQLNode.query);

        String result = executePlan(plan);
        Assert.assertTrue("expected the real simpleField value to come back:\n" + result, result.contains("hello"));
    }

    @Test
    public void testOptionalUnsuppliedDynamicVariableArgumentIsDeclaredButOmittedFromVariablesPayload() throws Exception
    {
        String schemaWithOptionalArgument =
                "{\"data\":{\"__schema\":{" +
                "\"queryType\":{\"name\":\"Query\"}," +
                "\"mutationType\":null," +
                "\"types\":[" +
                "{\"kind\":\"OBJECT\",\"name\":\"Query\",\"fields\":[" +
                "  {\"name\":\"greet\",\"type\":{\"kind\":\"OBJECT\",\"name\":\"GreetResult\"},\"args\":[{\"name\":\"name\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}]}" +
                "]}," +
                "{\"kind\":\"OBJECT\",\"name\":\"GreetResult\",\"fields\":[" +
                "  {\"name\":\"message\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}" +
                "]}" +
                "]" +
                "}}}";

        introspectionServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(schemaWithOptionalArgument)));

        String storePackage = "test::graphql::e2e::exploreUnsuppliedOptionalVariable";
        GraphQLStoreModelGenerationInput input = new GraphQLStoreModelGenerationInput();
        input.url = "http://127.0.0.1:" + introspectionServer.port() + "/graphql";
        input.storePath = storePackage + "::TestStore";
        input.targetPackage = storePackage;
        input.targetBindingPath = storePackage;
        input.targetMappingPath = storePackage + "::TestMapping";

        GraphQLStoreModelGenerationResult generationResult = new GraphQLStoreModelGenerator().generate(input, Identity.getAnonymousIdentity());
        Assert.assertTrue("expected no skipped types for this schema:\n" + generationResult.skippedTypes, generationResult.skippedTypes.isEmpty());

        dataServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"greetProxy\":{\"message\":\"hi\"}}}")));

        String selection = "greet($key){message}";
        String query = connectionRuntimeAndQuery(storePackage, dataServer.port(), "key: String[0..1]",
                storePackage + "::Query.all()\n" +
                "      ->graphFetch(#{" + storePackage + "::Query{" + selection + "}}#)\n" +
                "      ->serialize(#{" + storePackage + "::Query{" + selection + "}}#)");

        SingleExecutionPlan plan = buildPlanForQuery(generationResult.grammar + query, storePackage + "::TestMapping", storePackage + "::TestRuntime");
        GraphQLExecutionNode graphQLNode = findGraphQLExecutionNode(plan.rootExecutionNode);
        Assert.assertTrue("expected $key to be declared nullable (no '!') since its Pure type is String[0..1], and referenced in the query text:\n" + graphQLNode.query,
                graphQLNode.query.contains("query($key: String)") && graphQLNode.query.contains("greet(name: $key)"));

        // No "key" entry at all - simulates the caller never supplying this optional parameter.
        String result = executePlan(plan, java.util.Collections.emptyMap());
        Assert.assertTrue("expected the request to still succeed, not throw, despite the unresolved variable:\n" + result, result.contains("hi"));
        dataServer.verify(postRequestedFor(urlEqualTo("/graphql"))
                .withRequestBody(containing("\"variables\":{}")));
    }

    // Regression guard for the toVariableTypeReferenceForParameter fix above: a REQUIRED ([1]) dynamic
    // variable must still render non-null - confirms the fix derives nullability from the variable's own
    // multiplicity rather than always rendering nullable.
    @Test
    public void testRequiredDynamicVariableArgumentStillRendersNonNull() throws Exception
    {
        String schemaWithRequiredArgument =
                "{\"data\":{\"__schema\":{" +
                "\"queryType\":{\"name\":\"Query\"}," +
                "\"mutationType\":null," +
                "\"types\":[" +
                "{\"kind\":\"OBJECT\",\"name\":\"Query\",\"fields\":[" +
                "  {\"name\":\"greet\",\"type\":{\"kind\":\"OBJECT\",\"name\":\"GreetResult\"},\"args\":[{\"name\":\"name\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}]}" +
                "]}," +
                "{\"kind\":\"OBJECT\",\"name\":\"GreetResult\",\"fields\":[" +
                "  {\"name\":\"message\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}" +
                "]}" +
                "]" +
                "}}}";

        introspectionServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(schemaWithRequiredArgument)));

        String storePackage = "test::graphql::e2e::exploreRequiredDynamicVariable";
        GraphQLStoreModelGenerationInput input = new GraphQLStoreModelGenerationInput();
        input.url = "http://127.0.0.1:" + introspectionServer.port() + "/graphql";
        input.storePath = storePackage + "::TestStore";
        input.targetPackage = storePackage;
        input.targetBindingPath = storePackage;
        input.targetMappingPath = storePackage + "::TestMapping";

        GraphQLStoreModelGenerationResult generationResult = new GraphQLStoreModelGenerator().generate(input, Identity.getAnonymousIdentity());
        Assert.assertTrue("expected no skipped types for this schema:\n" + generationResult.skippedTypes, generationResult.skippedTypes.isEmpty());

        String selection = "greet($key){message}";
        String query = connectionRuntimeAndQuery(storePackage, dataServer.port(), "key: String[1]",
                storePackage + "::Query.all()\n" +
                "      ->graphFetch(#{" + storePackage + "::Query{" + selection + "}}#)\n" +
                "      ->serialize(#{" + storePackage + "::Query{" + selection + "}}#)");

        SingleExecutionPlan plan = buildPlanForQuery(generationResult.grammar + query, storePackage + "::TestMapping", storePackage + "::TestRuntime");
        GraphQLExecutionNode graphQLNode = findGraphQLExecutionNode(plan.rootExecutionNode);
        Assert.assertTrue("expected $key to still be declared non-null since its Pure type is String[1]:\n" + graphQLNode.query,
                graphQLNode.query.contains("query($key: String!)") && graphQLNode.query.contains("greet(name: $key)"));
    }

    @Test
    public void testDirectProxyPropertyAccessInternalizesCorrectly() throws Exception
    {
        String schemaWithScalarReturningArgument =
                "{\"data\":{\"__schema\":{" +
                "\"queryType\":{\"name\":\"Query\"}," +
                "\"mutationType\":null," +
                "\"types\":[" +
                "{\"kind\":\"OBJECT\",\"name\":\"Query\",\"fields\":[" +
                "  {\"name\":\"greet\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"},\"args\":[{\"name\":\"name\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}]}" +
                "]}" +
                "]" +
                "}}}";

        introspectionServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(schemaWithScalarReturningArgument)));

        String storePackage = "test::graphql::e2e::scalarProxyDirect";
        GraphQLStoreModelGenerationInput input = new GraphQLStoreModelGenerationInput();
        input.url = "http://127.0.0.1:" + introspectionServer.port() + "/graphql";
        input.storePath = storePackage + "::TestStore";
        input.targetPackage = storePackage;
        input.targetBindingPath = storePackage;
        input.targetMappingPath = storePackage + "::TestMapping";

        GraphQLStoreModelGenerationResult generationResult = new GraphQLStoreModelGenerator().generate(input, Identity.getAnonymousIdentity());

        dataServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"greetProxy\":\"hi\"}}")));

        // Query the plain delegate property DIRECTLY, bypassing the greet(...) QP entirely - isolates
        // internalize from the separate, known QP-reconstruction bug covered below.
        String selection = "greetProxy";
        String query = connectionRuntimeAndQuery(storePackage, dataServer.port(), "",
                storePackage + "::Query.all()\n" +
                "      ->graphFetch(#{" + storePackage + "::Query{" + selection + "}}#)\n" +
                "      ->serialize(#{" + storePackage + "::Query{" + selection + "}}#)");

        SingleExecutionPlan plan = buildPlanForQuery(generationResult.grammar + query, storePackage + "::TestMapping", storePackage + "::TestRuntime");
        String result = executePlan(plan);
        Assert.assertEquals("{\"builder\":{\"_type\":\"json\"},\"values\":{\"greetProxy\":\"hi\"}}", result);
    }

    // Known, documented limitation- root cause
    // confirmed: buildInMemoryPropertyNodeForGraphQL's shouldBuildNode gate skips building a dedicated node
    // for a scalar-returning QP (isPrimitiveValueProperty), so the delegate field (greetProxy) backing it
    // never gets populated on the in-memory graph-fetch wrapper, even though internalize itself is fine
    // (see testDirectProxyPropertyAccessInternalizesCorrectly above).
    @Test
    public void testBareScalarReturningQualifiedPropertyCallRendersMalformedKeyAndNullValue() throws Exception
    {
        String schemaWithScalarReturningArgument =
                "{\"data\":{\"__schema\":{" +
                "\"queryType\":{\"name\":\"Query\"}," +
                "\"mutationType\":null," +
                "\"types\":[" +
                "{\"kind\":\"OBJECT\",\"name\":\"Query\",\"fields\":[" +
                "  {\"name\":\"greet\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"},\"args\":[{\"name\":\"name\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}]}" +
                "]}" +
                "]" +
                "}}}";

        introspectionServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(schemaWithScalarReturningArgument)));

        String storePackage = "test::graphql::e2e::scalarQpFullOutput";
        GraphQLStoreModelGenerationInput input = new GraphQLStoreModelGenerationInput();
        input.url = "http://127.0.0.1:" + introspectionServer.port() + "/graphql";
        input.storePath = storePackage + "::TestStore";
        input.targetPackage = storePackage;
        input.targetBindingPath = storePackage;
        input.targetMappingPath = storePackage + "::TestMapping";

        GraphQLStoreModelGenerationResult generationResult = new GraphQLStoreModelGenerator().generate(input, Identity.getAnonymousIdentity());

        dataServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"greetProxy\":\"hi\"}}")));

        String selection = "greet($key)";
        String query = connectionRuntimeAndQuery(storePackage, dataServer.port(), "key: String[1]",
                storePackage + "::Query.all()\n" +
                "      ->graphFetch(#{" + storePackage + "::Query{" + selection + "}}#)\n" +
                "      ->serialize(#{" + storePackage + "::Query{" + selection + "}}#)");

        SingleExecutionPlan plan = buildPlanForQuery(generationResult.grammar + query, storePackage + "::TestMapping", storePackage + "::TestRuntime");
        GraphQLExecutionNode graphQLNode = findGraphQLExecutionNode(plan.rootExecutionNode);
        Assert.assertTrue("expected the wire request to correctly alias the field to the backing Proxy property name:\n" + graphQLNode.query,
                graphQLNode.query.contains("greetProxy: greet(name: $key)"));

        String result = executePlan(plan, Maps.mutable.with("key", "someone"));
        Assert.assertEquals("{\"builder\":{\"_type\":\"json\"},\"values\":{\"greet($key)\":null}}", result);
    }

    // Same malformed output key as the scalar case above (cosmetic only here - the value is unaffected),
    // confirming the key issue is general to any unaliased QP call, not specific to scalar return types.
    @Test
    public void testBareObjectReturningQualifiedPropertyCallRendersMalformedKeyButCorrectValue() throws Exception
    {
        String schemaWithOptionalArgument =
                "{\"data\":{\"__schema\":{" +
                "\"queryType\":{\"name\":\"Query\"}," +
                "\"mutationType\":null," +
                "\"types\":[" +
                "{\"kind\":\"OBJECT\",\"name\":\"Query\",\"fields\":[" +
                "  {\"name\":\"greet\",\"type\":{\"kind\":\"OBJECT\",\"name\":\"GreetResult\"},\"args\":[{\"name\":\"name\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}]}" +
                "]}," +
                "{\"kind\":\"OBJECT\",\"name\":\"GreetResult\",\"fields\":[" +
                "  {\"name\":\"message\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}" +
                "]}" +
                "]" +
                "}}}";

        introspectionServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(schemaWithOptionalArgument)));

        String storePackage = "test::graphql::e2e::objectQpFullOutput";
        GraphQLStoreModelGenerationInput input = new GraphQLStoreModelGenerationInput();
        input.url = "http://127.0.0.1:" + introspectionServer.port() + "/graphql";
        input.storePath = storePackage + "::TestStore";
        input.targetPackage = storePackage;
        input.targetBindingPath = storePackage;
        input.targetMappingPath = storePackage + "::TestMapping";

        GraphQLStoreModelGenerationResult generationResult = new GraphQLStoreModelGenerator().generate(input, Identity.getAnonymousIdentity());

        dataServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"greetProxy\":{\"message\":\"hi\"}}}")));

        String selection = "greet($key){message}";
        String query = connectionRuntimeAndQuery(storePackage, dataServer.port(), "key: String[1]",
                storePackage + "::Query.all()\n" +
                "      ->graphFetch(#{" + storePackage + "::Query{" + selection + "}}#)\n" +
                "      ->serialize(#{" + storePackage + "::Query{" + selection + "}}#)");

        SingleExecutionPlan plan = buildPlanForQuery(generationResult.grammar + query, storePackage + "::TestMapping", storePackage + "::TestRuntime");
        String result = executePlan(plan, Maps.mutable.with("key", "someone"));
        Assert.assertEquals("{\"builder\":{\"_type\":\"json\"},\"values\":{\"greet($key)\":{\"message\":\"hi\"}}}", result);
    }

    @Test
    public void testHandAuthoredOverloadOfGeneratedProxyQpRendersEachSignatureIndependently() throws Exception
    {
        String schemaWithOptionalArgument =
                "{\"data\":{\"__schema\":{" +
                "\"queryType\":{\"name\":\"Query\"}," +
                "\"mutationType\":null," +
                "\"types\":[" +
                "{\"kind\":\"OBJECT\",\"name\":\"Query\",\"fields\":[" +
                "  {\"name\":\"greet\",\"type\":{\"kind\":\"OBJECT\",\"name\":\"GreetResult\"},\"args\":[" +
                "    {\"name\":\"name\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}," +
                "    {\"name\":\"loud\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"Boolean\"}}" +
                "  ]}" +
                "]}," +
                "{\"kind\":\"OBJECT\",\"name\":\"GreetResult\",\"fields\":[" +
                "  {\"name\":\"message\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}" +
                "]}" +
                "]" +
                "}}}";

        introspectionServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(schemaWithOptionalArgument)));

        String storePackage = "test::graphql::e2e::overloadedProxyQp";
        GraphQLStoreModelGenerationInput input = new GraphQLStoreModelGenerationInput();
        input.url = "http://127.0.0.1:" + introspectionServer.port() + "/graphql";
        input.storePath = storePackage + "::TestStore";
        input.targetPackage = storePackage;
        input.targetBindingPath = storePackage;
        input.targetMappingPath = storePackage + "::TestMapping";

        GraphQLStoreModelGenerationResult generationResult = new GraphQLStoreModelGenerator().generate(input, Identity.getAnonymousIdentity());

        // Hand-splice a second, one-argument "shorthand" overload of the generated two-argument "greet" QP,
        // omitting the nullable "loud" argument entirely (valid per validateTypeNameAndQualifiedPropertiesAgainstTrackedSchema,
        // which only requires non-null schema arguments to be covered) and delegating to the same proxy.
        String generatedQpLine = "greet(name: String[0..1], loud: Boolean[0..1]) { $this.greetProxy }: " + storePackage + "::GreetResult[0..1];";
        String handAuthoredOverload = "greet(name: String[0..1]) { $this.greetProxy }: " + storePackage + "::GreetResult[0..1];";
        Assert.assertTrue("expected to find the generated QP line to splice the overload after:\n" + generationResult.grammar,
                generationResult.grammar.contains(generatedQpLine));
        String grammarWithOverload = generationResult.grammar.replace(generatedQpLine, generatedQpLine + "\n  " + handAuthoredOverload);

        dataServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"greetProxy\":{\"message\":\"hi\"}}}")));

        // Call site 1: the hand-authored one-argument shorthand overload - renders with only its own
        // parameter, with no "loud" argument at all (not even a dropped/omitted one - its own declared
        // signature never had it).
        String oneArgSelection = "greet($key){message}";
        String oneArgQuery = connectionRuntimeAndQuery(storePackage, dataServer.port(), "key: String[1]",
                storePackage + "::Query.all()\n" +
                "      ->graphFetch(#{" + storePackage + "::Query{" + oneArgSelection + "}}#)\n" +
                "      ->serialize(#{" + storePackage + "::Query{" + oneArgSelection + "}}#)");
        SingleExecutionPlan oneArgPlan = buildPlanForQuery(grammarWithOverload + oneArgQuery, storePackage + "::TestMapping", storePackage + "::TestRuntime");
        GraphQLExecutionNode oneArgNode = findGraphQLExecutionNode(oneArgPlan.rootExecutionNode);
        Assert.assertTrue("expected the one-argument overload's own call to render with only its own parameter:\n" + oneArgNode.query,
                oneArgNode.query.contains("greet(name: $key)") && !oneArgNode.query.contains("loud"));

        // Call site 2: the original, generated two-argument overload - renders with its own, different
        // parameter list, resolved independently of call site 1 (no shared, name-keyed lookup conflates them).
        String twoArgSelection = "greet($key, true){message}";
        String twoArgQuery = connectionRuntimeAndQuery(storePackage, dataServer.port(), "key: String[1]",
                storePackage + "::Query.all()\n" +
                "      ->graphFetch(#{" + storePackage + "::Query{" + twoArgSelection + "}}#)\n" +
                "      ->serialize(#{" + storePackage + "::Query{" + twoArgSelection + "}}#)");
        SingleExecutionPlan twoArgPlan = buildPlanForQuery(grammarWithOverload + twoArgQuery, storePackage + "::TestMapping", storePackage + "::TestRuntime");
        GraphQLExecutionNode twoArgNode = findGraphQLExecutionNode(twoArgPlan.rootExecutionNode);
        Assert.assertTrue("expected the two-argument overload's own call to render with both its own parameters:\n" + twoArgNode.query,
                twoArgNode.query.contains("greet(name: $key, loud:"));

        String result = executePlan(twoArgPlan, Maps.mutable.with("key", "someone"));
        Assert.assertTrue("expected the real data to come back for the two-argument overload call:\n" + result, result.contains("hi"));

        // Same two overloads, but requested as TRUE siblings in one graphFetch tree - confirmed reachable
        // (graphFetch grammar allows two overloads of the same name as distinct sibling properties), and
        // there's no single wire field shape to render for both at once, so this must fail loudly at
        // plan-generation time via buildFieldForGroup's defensive assert, not crash on an internal toOne().
        String bothSelection = "greet($key){message}, greet($key, true){message}";
        String bothQuery = connectionRuntimeAndQuery(storePackage, dataServer.port(), "key: String[1]",
                storePackage + "::Query.all()\n" +
                "      ->graphFetch(#{" + storePackage + "::Query{" + bothSelection + "}}#)\n" +
                "      ->serialize(#{" + storePackage + "::Query{" + bothSelection + "}}#)");
        Exception sameLevelException = Assert.assertThrows(Exception.class,
                () -> buildPlanForQuery(grammarWithOverload + bothQuery, storePackage + "::TestMapping", storePackage + "::TestRuntime"));
        Assert.assertTrue("expected a clear error naming the overload conflict:\n" + sameLevelException.getMessage(),
                sameLevelException.getMessage().contains("Two different overloads of qualified property 'greet' were both requested at the same level"));
    }

    private static final String PLAIN_ROOT_POLYMORPHIC_PROPERTY_INTROSPECTION_RESPONSE =
            "{\"data\":{\"__schema\":{" +
            "\"queryType\":{\"name\":\"Query\"}," +
            "\"mutationType\":null," +
            "\"types\":[" +
            "{\"kind\":\"OBJECT\",\"name\":\"Query\",\"fields\":[" +
            "  {\"name\":\"currentWorker\",\"type\":{\"kind\":\"INTERFACE\",\"name\":\"TestParentObject\"}}" +
            "]}," +
            "{\"kind\":\"INTERFACE\",\"name\":\"TestParentObject\",\"fields\":[" +
            "  {\"name\":\"id\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}" +
            "],\"possibleTypes\":[{\"kind\":\"OBJECT\",\"name\":\"Worker\"},{\"kind\":\"OBJECT\",\"name\":\"Candidate\"}]}," +
            "{\"kind\":\"OBJECT\",\"name\":\"Worker\",\"fields\":[" +
            "  {\"name\":\"id\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}," +
            "  {\"name\":\"employeeId\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}" +
            "]}," +
            "{\"kind\":\"OBJECT\",\"name\":\"Candidate\",\"fields\":[" +
            "  {\"name\":\"id\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}," +
            "  {\"name\":\"applicationId\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}" +
            "]}" +
            "]" +
            "}}}";

    @Test
    public void testPlainZeroArgumentQueryRootFieldWithSubTypeNarrowingResolvesCorrectly() throws Exception
    {
        introspectionServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(PLAIN_ROOT_POLYMORPHIC_PROPERTY_INTROSPECTION_RESPONSE)));

        String storePackage = "test::graphql::e2e::rootpolyplain";
        GraphQLStoreModelGenerationInput input = new GraphQLStoreModelGenerationInput();
        input.url = "http://127.0.0.1:" + introspectionServer.port() + "/graphql";
        input.storePath = storePackage + "::TestStore";
        input.targetPackage = storePackage;
        input.targetBindingPath = storePackage;
        input.targetMappingPath = storePackage + "::TestMapping";

        GraphQLStoreModelGenerationResult generationResult = new GraphQLStoreModelGenerator().generate(input, Identity.getAnonymousIdentity());
        Assert.assertTrue("expected no skipped types for this schema:\n" + generationResult.skippedTypes, generationResult.skippedTypes.isEmpty());

        dataServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"currentWorker\":{\"__typename\":\"Worker\",\"id\":\"m1\",\"employeeId\":\"E1\"}}}")));

        String selection = "currentWorker->subType(@" + storePackage + "::Worker){employeeId}";
        String query = connectionRuntimeAndQuery(storePackage, dataServer.port(), "",
                storePackage + "::Query.all()\n" +
                "      ->graphFetch(#{" + storePackage + "::Query{" + selection + "}}#)\n" +
                "      ->serialize(#{" + storePackage + "::Query{" + selection + "}}#)");

        SingleExecutionPlan plan = buildPlanForQuery(generationResult.grammar + query, storePackage + "::TestMapping", storePackage + "::TestRuntime");
        String result = executePlan(plan);

        Assert.assertTrue("expected the Worker-only field 'employeeId' to resolve via subType() narrowing on a plain, zero-argument Query-root field:\n" + result, result.contains("E1"));
    }

    private static SingleExecutionPlan buildPlanForQuery(String grammar, String mapping, String runtime)
    {
        PureModelContextData contextData = PureGrammarParser.newInstance().parseModel(grammar);
        PureModel pureModel = Compiler.compile(contextData, null, Identity.getAnonymousIdentity().getName());

        MutableList<Root_meta_pure_extension_Extension> extensions = Lists.mutable.ofAll(
                core_external_store_graphql_java_platform_binding_graphQLStoreLegendJavaPlatformBindingExtension.Root_meta_external_store_graphQL_executionPlan_platformBinding_legendJava_graphQLStoreExtensionsWithLegendJavaPlatformBinding_ExternalFormatLegendJavaPlatformBindingDescriptor_MANY__Extension_MANY_(
                        Lists.mutable.with(
                                core_external_format_json_java_platform_binding_legendJavaPlatformBinding_descriptor.Root_meta_external_format_json_executionPlan_platformBinding_legendJava_jsonSchemaJavaBindingDescriptor__ExternalFormatLegendJavaPlatformBindingDescriptor_1_(pureModel.getExecutionSupport())
                        ),
                        pureModel.getExecutionSupport()
                )
        );

        Function queryFunctionExpressions = ListIterate.detect(contextData.getElementsOfType(Function.class), f -> "query__Any_1_".equals(f.name));

        return PlanGenerator.generateExecutionPlan(
                HelperValueSpecificationBuilder.buildLambda(((LambdaFunction) queryFunctionExpressions.body.get(0)).body, ((LambdaFunction) queryFunctionExpressions.body.get(0)).parameters, pureModel.getContext()),
                pureModel.getMapping(mapping),
                pureModel.getRuntime(runtime),
                null,
                pureModel,
                "vX_X_X",
                PlanPlatform.JAVA,
                null,
                extensions,
                LegendPlanTransformers.transformers
        );
    }

    // Temporary debug helper for tracing generated Java shapes - not part of the final design.
    private static String executePlan(SingleExecutionPlan plan)
    {
        return executePlan(plan, Maps.mutable.empty());
    }

    private static String executePlan(SingleExecutionPlan plan, Map<String, ?> params)
    {
        SingleExecutionPlan singleExecutionPlan = plan.getSingleExecutionPlan(params);
        Map<String, Result> vars = Maps.mutable.ofInitialCapacity(params.size());
        params.forEach((key, value) -> vars.put(key, new org.finos.legend.engine.plan.execution.result.ConstantResult(value)));
        Identity identity = Identity.makeIdentity(Lists.mutable.with(new KerberosProfile(LocalCredentials.INSTANCE)));
        PlanExecutor planExecutor = PlanExecutor.newPlanExecutor(new GraphQLStoreExecutorBuilder().build(), InMemory.build());
        JsonStreamingResult result = (JsonStreamingResult) planExecutor.execute(singleExecutionPlan, vars, (String) null, identity, null);
        return result.flush(new JsonStreamToJsonDefaultSerializer(result));
    }
}
