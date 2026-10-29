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

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.finos.legend.authentication.credentialprovider.CredentialProvider;
import org.finos.legend.authentication.credentialprovider.CredentialProviderProvider;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.AuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.AuthenticationSpecificationVisitor;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.OAuthCredential;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.core.port.DynamicPortGenerator;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

public class TestGraphQLStoreModelGenerator
{
    private WireMockServer wireMockServer;
    private String url;

    @Before
    public void setup()
    {
        int port = DynamicPortGenerator.generatePort();
        wireMockServer = new WireMockServer(port);
        wireMockServer.start();
        WireMock.configureFor("localhost", port);
        url = "http://127.0.0.1:" + port + "/graphql";
    }

    @After
    public void teardown()
    {
        wireMockServer.stop();
    }

    private static final String INTROSPECTION_RESPONSE =
            "{\"data\":{\"__schema\":{" +
            "\"queryType\":{\"name\":\"Query\"}," +
            "\"mutationType\":null," +
            "\"types\":[" +
            "{\"kind\":\"OBJECT\",\"name\":\"Query\",\"fields\":[" +
            "  {\"name\":\"employees\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"LIST\",\"ofType\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"OBJECT\",\"name\":\"Employee\"}}}}}," +
            "  {\"name\":\"departments\",\"type\":{\"kind\":\"LIST\",\"ofType\":{\"kind\":\"OBJECT\",\"name\":\"Department\"}}}," +
            "  {\"name\":\"employee\",\"type\":{\"kind\":\"OBJECT\",\"name\":\"Employee2\"}}" +
            "]}," +
            "{\"kind\":\"OBJECT\",\"name\":\"Employee\",\"fields\":[" +
            "  {\"name\":\"firstName\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}," +
            "  {\"name\":\"lastName\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}," +
            "  {\"name\":\"manager\",\"type\":{\"kind\":\"OBJECT\",\"name\":\"Employee\"}}," +
            "  {\"name\":\"department\",\"type\":{\"kind\":\"OBJECT\",\"name\":\"Department\"}}," +
            "  {\"name\":\"address\",\"type\":{\"kind\":\"OBJECT\",\"name\":\"Address\"}}" +
            "]}," +
            "{\"kind\":\"OBJECT\",\"name\":\"Employee2\",\"fields\":[" +
            "  {\"name\":\"id\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}" +
            "]}," +
            "{\"kind\":\"OBJECT\",\"name\":\"Department\",\"fields\":[" +
            "  {\"name\":\"name\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}" +
            "]}," +
            "{\"kind\":\"OBJECT\",\"name\":\"Address\",\"fields\":[" +
            "  {\"name\":\"street\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}," +
            "  {\"name\":\"country\",\"type\":{\"kind\":\"OBJECT\",\"name\":\"Country\"}}" +
            "]}," +
            "{\"kind\":\"OBJECT\",\"name\":\"Country\",\"fields\":[" +
            "  {\"name\":\"name\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}" +
            "]}" +
            "]" +
            "}}}";

    @Test
    public void testGeneratesClassBindingAndMappingFromLiveIntrospection() throws Exception
    {
        wireMockServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(INTROSPECTION_RESPONSE)));

        GraphQLStoreModelGenerationInput input = new GraphQLStoreModelGenerationInput();
        input.url = url;
        input.storePath = "test::graphql::gen::TestStore";
        input.targetPackage = "test::graphql::gen";
        input.targetBindingPath = "test::graphql::gen";
        input.targetMappingPath = "test::graphql::gen::TestMapping";

        GraphQLStoreModelGenerationResult result = new GraphQLStoreModelGenerator().generate(input, Identity.getAnonymousIdentity());

        Assert.assertTrue("expected no skipped fields for this schema:\n" + result.skippedTypes, result.skippedTypes.isEmpty());

        String grammar = result.grammar;
        Assert.assertTrue("expected a generated Query wrapper Class:\n" + grammar, grammar.contains("Class test::graphql::gen::Query"));
        Assert.assertTrue("expected a generated Employee Class:\n" + grammar, grammar.contains("Class test::graphql::gen::Employee"));
        Assert.assertTrue("expected a generated Department Class:\n" + grammar, grammar.contains("Class test::graphql::gen::Department"));
        Assert.assertTrue("expected a generated Employee2 Class (no longer skipped):\n" + grammar, grammar.contains("Class test::graphql::gen::Employee2"));

        Assert.assertTrue("expected a single shared generated Binding:\n" + grammar, grammar.contains("Binding test::graphql::gen::GeneratedBinding"));
        Assert.assertEquals("expected exactly one Binding declaration, not one per entity:\n" + grammar, 1, countOccurrences(grammar, "Binding test::graphql::gen::GeneratedBinding"));
        Assert.assertEquals("expected exactly one class mapping (the Query wrapper) referencing the shared binding:\n" + grammar,
                1, countOccurrences(grammar, "~binding test::graphql::gen::GeneratedBinding"));
        Assert.assertEquals("expected exactly one Department Class declaration, not a duplicate from Employee's own schema:\n" + grammar,
                1, countOccurrences(grammar, "Class test::graphql::gen::Department"));
        Assert.assertTrue("expected Employee.department property:\n" + grammar, grammar.contains("department"));

        Assert.assertTrue("expected the Query wrapper's own root mapping (* prefix):\n" + grammar, grammar.contains("*test::graphql::gen::Query: GraphQL"));
        Assert.assertTrue("expected the Query wrapper to use ~typeName:\n" + grammar, grammar.contains("~typeName [test::graphql::gen::TestStore] 'Query'"));
        Assert.assertTrue("expected Query's own plain employees property:\n" + grammar, grammar.contains("employees: test::graphql::gen::Employee[1..*]"));
        Assert.assertTrue("expected Query's own plain departments property:\n" + grammar, grammar.contains("departments: test::graphql::gen::Department[*]"));
        Assert.assertTrue("expected Query's own plain employee property (no longer skipped):\n" + grammar, grammar.contains("employee: test::graphql::gen::Employee2[0..1]"));

        Assert.assertTrue("expected a generated GraphQLStore declaration:\n" + grammar, grammar.contains("GraphQLStore test::graphql::gen::TestStore"));
        Assert.assertTrue("expected the GraphQLStore to embed an explicit schema{query:...} block:\n" + grammar, grammar.contains("query : Query"));
        Assert.assertTrue("expected the embedded schema text to declare the Employee type:\n" + grammar, grammar.contains("type Employee"));

        Assert.assertTrue("expected a generated Address Class:\n" + grammar, grammar.contains("Class test::graphql::gen::Address"));
        Assert.assertTrue("expected a generated Country Class:\n" + grammar, grammar.contains("Class test::graphql::gen::Country"));
        Assert.assertFalse("expected no standalone class mapping for folded-only Address:\n" + grammar, grammar.contains("test::graphql::gen::Address: GraphQL"));
        Assert.assertFalse("expected no standalone class mapping for folded-only Country:\n" + grammar, grammar.contains("test::graphql::gen::Country: GraphQL"));

        PureModelContextData reparsed = PureGrammarParser.newInstance().parseModel(grammar);
        Compiler.compile(reparsed, null, Identity.getAnonymousIdentity().getName());

        Assert.assertTrue("expected Employee.manager to resolve back to Employee itself (self-cycle), not be omitted:\n" + grammar, grammar.contains("manager"));
    }

    private static final String ENUM_INTROSPECTION_RESPONSE =
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

    @Test
    public void testGeneratesRealPureEnumAndSupportsEnumTypedArgument() throws Exception
    {
        wireMockServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(ENUM_INTROSPECTION_RESPONSE)));

        GraphQLStoreModelGenerationInput input = new GraphQLStoreModelGenerationInput();
        input.url = url;
        input.storePath = "test::graphql::gen::enumtest::TestStore";
        input.targetPackage = "test::graphql::gen::enumtest";
        input.targetBindingPath = "test::graphql::gen::enumtest";
        input.targetMappingPath = "test::graphql::gen::enumtest::TestMapping";

        GraphQLStoreModelGenerationResult result = new GraphQLStoreModelGenerator().generate(input, Identity.getAnonymousIdentity());

        Assert.assertTrue("expected no skipped fields - Enum return types and Enum arguments are both supported now:\n" + result.skippedTypes, result.skippedTypes.isEmpty());

        String grammar = result.grammar;
        Assert.assertTrue("expected a real generated Enum declaration, not Employee.status falling back to String:\n" + grammar,
                grammar.contains("Enum test::graphql::gen::enumtest::Status"));
        Assert.assertTrue("expected both enum values generated:\n" + grammar, grammar.contains("ACTIVE") && grammar.contains("INACTIVE"));
        Assert.assertEquals("expected the Status Enum declared exactly once, not duplicated between the plain field and the QP argument:\n" + grammar,
                1, countOccurrences(grammar, "Enum test::graphql::gen::enumtest::Status"));

        Assert.assertTrue("expected Employee.status typed as the real Enum, not String:\n" + grammar,
                grammar.contains("status: test::graphql::gen::enumtest::Status[0..1]") || grammar.contains("status : test::graphql::gen::enumtest::Status[0..1]"));

        Assert.assertTrue("expected the generated QP to declare its argument using the real Enum type:\n" + grammar,
                grammar.contains("employeesByStatus(status: test::graphql::gen::enumtest::Status[0..1])"));

        PureModelContextData reparsed = PureGrammarParser.newInstance().parseModel(grammar);
        Compiler.compile(reparsed, null, Identity.getAnonymousIdentity().getName());
    }

    private static final String SCHEMA_COMPLETENESS_INTROSPECTION_RESPONSE =
            "{\"data\":{\"__schema\":{" +
            "\"queryType\":{\"name\":\"Query\"}," +
            "\"mutationType\":null," +
            "\"types\":[" +
            "{\"kind\":\"OBJECT\",\"name\":\"Query\",\"fields\":[" +
            "  {\"name\":\"employeesByStatus\",\"type\":{\"kind\":\"LIST\",\"ofType\":{\"kind\":\"OBJECT\",\"name\":\"Employee\"}},\"args\":[{\"name\":\"status\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"ENUM\",\"name\":\"Status\"}}}]}," +
            "  {\"name\":\"employeesByFilter\",\"type\":{\"kind\":\"LIST\",\"ofType\":{\"kind\":\"OBJECT\",\"name\":\"Employee\"}},\"args\":[{\"name\":\"filter\",\"type\":{\"kind\":\"INPUT_OBJECT\",\"name\":\"EmployeeFilter\"}}]}" +
            "]}," +
            "{\"kind\":\"OBJECT\",\"name\":\"Employee\",\"fields\":[" +
            "  {\"name\":\"firstName\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}," +
            "  {\"name\":\"status\",\"type\":{\"kind\":\"ENUM\",\"name\":\"Status\"}}," +
            "  {\"name\":\"assignedTo\",\"type\":{\"kind\":\"INTERFACE\",\"name\":\"StaffMember\"}}" +
            "]}," +
            "{\"kind\":\"ENUM\",\"name\":\"Status\",\"enumValues\":[{\"name\":\"ACTIVE\"},{\"name\":\"INACTIVE\"}]}," +
            "{\"kind\":\"INTERFACE\",\"name\":\"StaffMember\",\"fields\":[]," +
            "\"possibleTypes\":[{\"kind\":\"OBJECT\",\"name\":\"Worker\"}]}," +
            "{\"kind\":\"OBJECT\",\"name\":\"Worker\",\"interfaces\":[{\"kind\":\"INTERFACE\",\"name\":\"StaffMember\"}],\"fields\":[" +
            "  {\"name\":\"employeeId\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}" +
            "]}," +
            "{\"kind\":\"INPUT_OBJECT\",\"name\":\"EmployeeFilter\",\"inputFields\":[" +
            "  {\"name\":\"status\",\"type\":{\"kind\":\"ENUM\",\"name\":\"Status\"}}" +
            "]}" +
            "]" +
            "}}}";

    @Test
    public void testComposedSchemaIncludesInterfaceEnumAndInputObjectDefinitions() throws Exception
    {
        wireMockServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(SCHEMA_COMPLETENESS_INTROSPECTION_RESPONSE)));

        GraphQLStoreModelGenerationInput input = new GraphQLStoreModelGenerationInput();
        input.url = url;
        input.storePath = "test::graphql::gen::schemacompleteness::TestStore";
        input.targetPackage = "test::graphql::gen::schemacompleteness";
        input.targetBindingPath = "test::graphql::gen::schemacompleteness";
        input.targetMappingPath = "test::graphql::gen::schemacompleteness::TestMapping";

        GraphQLStoreModelGenerationResult result = new GraphQLStoreModelGenerator().generate(input, Identity.getAnonymousIdentity());
        Assert.assertTrue("expected no skipped fields:\n" + result.skippedTypes, result.skippedTypes.isEmpty());

        String grammar = result.grammar;
        Assert.assertTrue("expected the Status enum definition in the composed schema, not just referenced by name:\n" + grammar,
                grammar.contains("enum Status {\n  ACTIVE\n  INACTIVE\n}"));
        Assert.assertTrue("expected a bare, brace-less zero-field StaffMember interface definition:\n" + grammar,
                grammar.contains("interface StaffMember"));
        Assert.assertFalse("expected no empty braces on the zero-field interface - fieldsDefinition must be omitted, not empty, when there are no fields:\n" + grammar,
                grammar.contains("interface StaffMember {"));
        Assert.assertTrue("expected Worker's `implements StaffMember` clause to survive composition:\n" + grammar,
                grammar.contains("type Worker implements StaffMember {"));
        Assert.assertTrue("expected the EmployeeFilter input-object definition in the composed schema:\n" + grammar,
                grammar.contains("input EmployeeFilter {"));

        PureModelContextData reparsed = PureGrammarParser.newInstance().parseModel(grammar);
        Compiler.compile(reparsed, null, Identity.getAnonymousIdentity().getName());
    }

    private static final String MIXED_ARGUMENT_NULLABILITY_INTROSPECTION_RESPONSE =
            "{\"data\":{\"__schema\":{" +
            "\"queryType\":{\"name\":\"Query\"}," +
            "\"mutationType\":null," +
            "\"types\":[" +
            "{\"kind\":\"OBJECT\",\"name\":\"Query\",\"fields\":[" +
            "  {\"name\":\"employees\",\"type\":{\"kind\":\"LIST\",\"ofType\":{\"kind\":\"OBJECT\",\"name\":\"Employee\"}},\"args\":[" +
            "    {\"name\":\"requiredArg\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}," +
            "    {\"name\":\"nullableArg\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"Int\"}}," +
            "    {\"name\":\"defaultedArg\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"Int\"}},\"defaultValue\":\"100\"}" +
            "  ]}" +
            "]}," +
            "{\"kind\":\"OBJECT\",\"name\":\"Employee\",\"fields\":[" +
            "  {\"name\":\"firstName\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}" +
            "]}" +
            "]" +
            "}}}";

    @Test
    public void testArgumentMultiplicityReflectsNullabilityAndDefaultValue() throws Exception
    {
        wireMockServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(MIXED_ARGUMENT_NULLABILITY_INTROSPECTION_RESPONSE)));

        GraphQLStoreModelGenerationInput input = new GraphQLStoreModelGenerationInput();
        input.url = url;
        input.storePath = "test::graphql::gen::argnullability::TestStore";
        input.targetPackage = "test::graphql::gen::argnullability";
        input.targetBindingPath = "test::graphql::gen::argnullability";
        input.targetMappingPath = "test::graphql::gen::argnullability::TestMapping";

        GraphQLStoreModelGenerationResult result = new GraphQLStoreModelGenerator().generate(input, Identity.getAnonymousIdentity());
        Assert.assertTrue("expected no skipped fields:\n" + result.skippedTypes, result.skippedTypes.isEmpty());

        String grammar = result.grammar;
        Assert.assertTrue("expected a NON_NULL argument with no default to stay required:\n" + grammar,
                grammar.contains("requiredArg: String[1]"));
        Assert.assertTrue("expected a nullable argument to become optional:\n" + grammar,
                grammar.contains("nullableArg: Integer[0..1]"));
        Assert.assertTrue("expected a NON_NULL argument with a captured defaultValue to become optional too:\n" + grammar,
                grammar.contains("defaultedArg: Integer[0..1]"));

        PureModelContextData reparsed = PureGrammarParser.newInstance().parseModel(grammar);
        Compiler.compile(reparsed, null, Identity.getAnonymousIdentity().getName());
    }

    private static final String REQUIRED_LIST_ARGUMENT_INTROSPECTION_RESPONSE =
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

    @Test
    public void testStampsRequiredArgumentsTaggedValueOnlyForNonNullListArguments() throws Exception
    {
        wireMockServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(REQUIRED_LIST_ARGUMENT_INTROSPECTION_RESPONSE)));

        GraphQLStoreModelGenerationInput input = new GraphQLStoreModelGenerationInput();
        input.url = url;
        input.storePath = "test::graphql::gen::requiredlist::TestStore";
        input.targetPackage = "test::graphql::gen::requiredlist";
        input.targetBindingPath = "test::graphql::gen::requiredlist";
        input.targetMappingPath = "test::graphql::gen::requiredlist::TestMapping";

        GraphQLStoreModelGenerationResult result = new GraphQLStoreModelGenerator().generate(input, Identity.getAnonymousIdentity());
        Assert.assertTrue("expected no skipped fields:\n" + result.skippedTypes, result.skippedTypes.isEmpty());

        String grammar = result.grammar;
        Assert.assertTrue("expected a tagged value naming the required list argument right before the QP that declares it:\n" + grammar,
                grammar.contains("{meta::external::store::graphQL::generation::RequiredArgumentsProfile.requiredArguments = 'requiredTags'} employeesByTags("));
        Assert.assertFalse("expected no tagged value for the optional list argument:\n" + grammar,
                grammar.contains("'optionalTags'"));

        PureModelContextData reparsed = PureGrammarParser.newInstance().parseModel(grammar);
        Compiler.compile(reparsed, null, Identity.getAnonymousIdentity().getName());
    }

    private static final String INPUT_OBJECT_ARGUMENT_INTROSPECTION_RESPONSE =
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

    @Test
    public void testInputObjectTypedArgumentGeneratesStandaloneClassAndListTypedParameter() throws Exception
    {
        wireMockServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(INPUT_OBJECT_ARGUMENT_INTROSPECTION_RESPONSE)));

        GraphQLStoreModelGenerationInput input = new GraphQLStoreModelGenerationInput();
        input.url = url;
        input.storePath = "test::graphql::gen::inputobject::TestStore";
        input.targetPackage = "test::graphql::gen::inputobject";
        input.targetBindingPath = "test::graphql::gen::inputobject";
        input.targetMappingPath = "test::graphql::gen::inputobject::TestMapping";

        GraphQLStoreModelGenerationResult result = new GraphQLStoreModelGenerator().generate(input, Identity.getAnonymousIdentity());
        Assert.assertTrue("expected no skipped fields:\n" + result.skippedTypes, result.skippedTypes.isEmpty());

        String grammar = result.grammar;
        Assert.assertTrue("expected a standalone input-object Class:\n" + grammar,
                grammar.contains("Class test::graphql::gen::inputobject::FilterKey\n{\n  key: String[1];\n  value: String[1];\n}"));
        Assert.assertTrue("expected the QP's own argument typed as a list of the input-object Class:\n" + grammar,
                grammar.contains("filters: test::graphql::gen::inputobject::FilterKey[*]"));

        PureModelContextData reparsed = PureGrammarParser.newInstance().parseModel(grammar);
        Compiler.compile(reparsed, null, Identity.getAnonymousIdentity().getName());
    }

    private static final String SAME_TYPE_PLAIN_AND_QUALIFIED_INTROSPECTION_RESPONSE =
            "{\"data\":{\"__schema\":{" +
            "\"queryType\":{\"name\":\"Query\"}," +
            "\"mutationType\":null," +
            "\"types\":[" +
            "{\"kind\":\"OBJECT\",\"name\":\"Query\",\"fields\":[" +
            "  {\"name\":\"employees\",\"type\":{\"kind\":\"LIST\",\"ofType\":{\"kind\":\"OBJECT\",\"name\":\"Employee\"}}}," +
            "  {\"name\":\"employee\",\"type\":{\"kind\":\"OBJECT\",\"name\":\"Employee\"},\"args\":[{\"name\":\"id\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"Int\"}}]}" +
            "]}," +
            "{\"kind\":\"OBJECT\",\"name\":\"Employee\",\"fields\":[" +
            "  {\"name\":\"firstName\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}" +
            "]}" +
            "]" +
            "}}}";

    @Test
    public void testPlainAndQualifiedPropertyTargetingSameEntityTypeBothGenerateCorrectly() throws Exception
    {
        wireMockServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(SAME_TYPE_PLAIN_AND_QUALIFIED_INTROSPECTION_RESPONSE)));

        GraphQLStoreModelGenerationInput input = new GraphQLStoreModelGenerationInput();
        input.url = url;
        input.storePath = "test::graphql::gen::sametype::TestStore";
        input.targetPackage = "test::graphql::gen::sametype";
        input.targetBindingPath = "test::graphql::gen::sametype";
        input.targetMappingPath = "test::graphql::gen::sametype::TestMapping";

        GraphQLStoreModelGenerationResult result = new GraphQLStoreModelGenerator().generate(input, Identity.getAnonymousIdentity());
        Assert.assertTrue("expected no skipped fields for this schema:\n" + result.skippedTypes, result.skippedTypes.isEmpty());

        String grammar = result.grammar;
        Assert.assertTrue("expected the plain employees property:\n" + grammar, grammar.contains("employees: test::graphql::gen::sametype::Employee[*]"));
        Assert.assertTrue("expected the dedicated proxy property for the employee QP:\n" + grammar, grammar.contains("employeeProxy: test::graphql::gen::sametype::Employee[0..1]"));
        Assert.assertTrue("expected the employee qualified property navigating its own proxy:\n" + grammar,
                grammar.contains("employee(id: Integer[0..1]) { $this.employeeProxy }: test::graphql::gen::sametype::Employee[0..1]"));
        Assert.assertEquals("expected exactly one Employee Class declaration:\n" + grammar, 1, countOccurrences(grammar, "Class test::graphql::gen::sametype::Employee"));

        PureModelContextData reparsed = PureGrammarParser.newInstance().parseModel(grammar);
        Compiler.compile(reparsed, null, Identity.getAnonymousIdentity().getName());
    }

    private static final String PROXY_NAME_COLLISION_INTROSPECTION_RESPONSE =
            "{\"data\":{\"__schema\":{" +
            "\"queryType\":{\"name\":\"Query\"}," +
            "\"mutationType\":null," +
            "\"types\":[" +
            "{\"kind\":\"OBJECT\",\"name\":\"Query\",\"fields\":[" +
            "  {\"name\":\"employee\",\"type\":{\"kind\":\"OBJECT\",\"name\":\"Employee\"},\"args\":[{\"name\":\"id\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"Int\"}}]}," +
            "  {\"name\":\"employeeProxy\",\"type\":{\"kind\":\"OBJECT\",\"name\":\"Employee\"}}" +
            "]}," +
            "{\"kind\":\"OBJECT\",\"name\":\"Employee\",\"fields\":[" +
            "  {\"name\":\"firstName\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}" +
            "]}" +
            "]" +
            "}}}";

    @Test
    public void testProxyNameCollisionWithRealFieldIsAvoided() throws Exception
    {
        wireMockServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(PROXY_NAME_COLLISION_INTROSPECTION_RESPONSE)));

        GraphQLStoreModelGenerationInput input = new GraphQLStoreModelGenerationInput();
        input.url = url;
        input.storePath = "test::graphql::gen::proxycollision::TestStore";
        input.targetPackage = "test::graphql::gen::proxycollision";
        input.targetBindingPath = "test::graphql::gen::proxycollision";
        input.targetMappingPath = "test::graphql::gen::proxycollision::TestMapping";

        GraphQLStoreModelGenerationResult result = new GraphQLStoreModelGenerator().generate(input, Identity.getAnonymousIdentity());
        Assert.assertTrue("expected no skipped fields for this schema:\n" + result.skippedTypes, result.skippedTypes.isEmpty());

        String grammar = result.grammar;
        Assert.assertTrue("expected the real employeeProxy field as a plain property:\n" + grammar, grammar.contains("employeeProxy: test::graphql::gen::proxycollision::Employee[0..1]"));
        Assert.assertTrue("expected the employee QP's own proxy to be renamed to employeeProxy2:\n" + grammar, grammar.contains("employeeProxy2: test::graphql::gen::proxycollision::Employee[0..1]"));
        Assert.assertTrue("expected the employee QP to navigate its renamed proxy:\n" + grammar, grammar.contains("employee(id: Integer[0..1]) { $this.employeeProxy2 }: test::graphql::gen::proxycollision::Employee[0..1]"));

        PureModelContextData reparsed = PureGrammarParser.newInstance().parseModel(grammar);
        Compiler.compile(reparsed, null, Identity.getAnonymousIdentity().getName());
    }

    private static final String ALL_FIELDS_UNSUPPORTED_INTROSPECTION_RESPONSE =
            "{\"data\":{\"__schema\":{" +
            "\"queryType\":{\"name\":\"Query\"}," +
            "\"mutationType\":null," +
            "\"types\":[" +
            "{\"kind\":\"OBJECT\",\"name\":\"Query\",\"fields\":[" +
            "  {\"name\":\"employeesByFilter\",\"type\":{\"kind\":\"LIST\",\"ofType\":{\"kind\":\"OBJECT\",\"name\":\"Employee\"}},\"args\":[{\"name\":\"filter\",\"type\":{\"kind\":\"OBJECT\",\"name\":\"StatusFilter\"}}]}" +
            "]}," +
            "{\"kind\":\"OBJECT\",\"name\":\"Employee\",\"fields\":[" +
            "  {\"name\":\"firstName\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}" +
            "]}" +
            "]" +
            "}}}";

    @Test
    public void testEverySingleFieldUnsupportedProducesEmptyGrammar() throws Exception
    {
        wireMockServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(ALL_FIELDS_UNSUPPORTED_INTROSPECTION_RESPONSE)));

        GraphQLStoreModelGenerationInput input = new GraphQLStoreModelGenerationInput();
        input.url = url;
        input.storePath = "test::graphql::gen::allskipped::TestStore";
        input.targetPackage = "test::graphql::gen::allskipped";
        input.targetBindingPath = "test::graphql::gen::allskipped";
        input.targetMappingPath = "test::graphql::gen::allskipped::TestMapping";

        GraphQLStoreModelGenerationResult result = new GraphQLStoreModelGenerator().generate(input, Identity.getAnonymousIdentity());

        Assert.assertEquals("expected a completely empty grammar when every field is unsupported", "", result.grammar);
        Assert.assertFalse("expected a reported skip reason", result.skippedTypes.isEmpty());
        Assert.assertTrue("expected the skip reason to name the unsupported argument:\n" + result.skippedTypes,
                result.skippedTypes.stream().anyMatch(s -> s.contains("employeesByFilter")));
    }

    // A non-conformant type name can reach an unescaped Pure property-type reference and corrupt the
    // composed grammar text if not rejected first.
    private static final String HOSTILE_INTROSPECTION_RESPONSE =
            "{\"data\":{\"__schema\":{" +
            "\"queryType\":{\"name\":\"Query\"}," +
            "\"mutationType\":null," +
            "\"types\":[" +
            "{\"kind\":\"OBJECT\",\"name\":\"Query\",\"fields\":[" +
            "  {\"name\":\"employees\",\"type\":{\"kind\":\"LIST\",\"ofType\":{\"kind\":\"OBJECT\",\"name\":\"Employee\"}}}" +
            "]}," +
            "{\"kind\":\"OBJECT\",\"name\":\"Employee\",\"fields\":[" +
            "  {\"name\":\"firstName\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}," +
            "  {\"name\":\"address\",\"type\":{\"kind\":\"OBJECT\",\"name\":\"Weird\\\"Name\\nWith}Braces{AndNewline\"}}" +
            "]}," +
            "{\"kind\":\"OBJECT\",\"name\":\"Weird\\\"Name\\nWith}Braces{AndNewline\",\"fields\":[" +
            "  {\"name\":\"street\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}" +
            "]}" +
            "]" +
            "}}}";

    @Test
    public void testHostileTypeNameDoesNotCorruptGeneratedGrammar() throws Exception
    {
        wireMockServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(HOSTILE_INTROSPECTION_RESPONSE)));

        GraphQLStoreModelGenerationInput input = new GraphQLStoreModelGenerationInput();
        input.url = url;
        input.storePath = "test::graphql::gen::TestStore";
        input.targetPackage = "test::graphql::gen";
        input.targetBindingPath = "test::graphql::gen";
        input.targetMappingPath = "test::graphql::gen::TestMapping";

        GraphQLStoreModelGenerationResult result = new GraphQLStoreModelGenerator().generate(input, Identity.getAnonymousIdentity());

        String grammar = result.grammar;
        Assert.assertTrue("expected a generated Employee Class:\n" + grammar, grammar.contains("Class test::graphql::gen::Employee"));
        Assert.assertTrue("expected Employee's valid firstName field to still be generated:\n" + grammar, grammar.contains("firstName"));

        String classAndMappingSection = grammar.substring(grammar.indexOf("###Pure"));
        Assert.assertFalse("hostile type name must not be referenced in any generated Class/Mapping:\n" + classAndMappingSection,
                classAndMappingSection.contains("With}Braces{AndNewline"));
        Assert.assertFalse("the field referencing the hostile type must be dropped, not just its type:\n" + classAndMappingSection,
                classAndMappingSection.contains("address"));
        Assert.assertTrue("expected a warning naming the omitted field:\n" + result.skippedTypes,
                result.skippedTypes.stream().anyMatch(s -> s.contains("Employee.address") && s.contains("not a valid GraphQL identifier")));

        PureModelContextData reparsed = PureGrammarParser.newInstance().parseModel(grammar);
        Compiler.compile(reparsed, null, Identity.getAnonymousIdentity().getName());
    }

    private static final String NESTED_POLYMORPHIC_PROPERTY_INTROSPECTION_RESPONSE =
            "{\"data\":{\"__schema\":{" +
            "\"queryType\":{\"name\":\"Query\"}," +
            "\"mutationType\":null," +
            "\"types\":[" +
            "{\"kind\":\"OBJECT\",\"name\":\"Query\",\"fields\":[" +
            "  {\"name\":\"employees\",\"type\":{\"kind\":\"LIST\",\"ofType\":{\"kind\":\"OBJECT\",\"name\":\"Employee\"}}}" +
            "]}," +
            "{\"kind\":\"OBJECT\",\"name\":\"Employee\",\"fields\":[" +
            "  {\"name\":\"firstName\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}," +
            "  {\"name\":\"manager\",\"type\":{\"kind\":\"INTERFACE\",\"name\":\"TestParentObject\"}}" +
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

    private static final String TWO_POLYMORPHIC_PROPERTIES_SHARING_SUBTYPES_INTROSPECTION_RESPONSE =
            "{\"data\":{\"__schema\":{" +
            "\"queryType\":{\"name\":\"Query\"}," +
            "\"mutationType\":null," +
            "\"types\":[" +
            "{\"kind\":\"OBJECT\",\"name\":\"Query\",\"fields\":[" +
            "  {\"name\":\"employees\",\"type\":{\"kind\":\"LIST\",\"ofType\":{\"kind\":\"OBJECT\",\"name\":\"Employee\"}}}," +
            "  {\"name\":\"departments\",\"type\":{\"kind\":\"LIST\",\"ofType\":{\"kind\":\"OBJECT\",\"name\":\"Department\"}}}" +
            "]}," +
            "{\"kind\":\"OBJECT\",\"name\":\"Employee\",\"fields\":[" +
            "  {\"name\":\"firstName\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}," +
            "  {\"name\":\"manager\",\"type\":{\"kind\":\"INTERFACE\",\"name\":\"TestParentObject\"}}" +
            "]}," +
            "{\"kind\":\"OBJECT\",\"name\":\"Department\",\"fields\":[" +
            "  {\"name\":\"name\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}," +
            "  {\"name\":\"lead\",\"type\":{\"kind\":\"INTERFACE\",\"name\":\"TestParentObject\"}}" +
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

    // Two different owning classes (Employee.manager, Department.lead) both reference the SAME interface
    // (TestParentObject, same possibleTypes) - exercises GraphQLStoreModelGenerator's seenSubtypeNames dedup logic
    // (generate()'s possibleSubtypes/seenSubtypeNames set): each subtype must get exactly ONE minimal root
    // ClassMapping despite being reachable from two places.
    @Test
    public void testSharedSubtypeAcrossTwoPolymorphicPropertiesGetsOneMappingEach() throws Exception
    {
        wireMockServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(TWO_POLYMORPHIC_PROPERTIES_SHARING_SUBTYPES_INTROSPECTION_RESPONSE)));

        GraphQLStoreModelGenerationInput input = new GraphQLStoreModelGenerationInput();
        input.url = url;
        input.storePath = "test::graphql::gen::shared::TestStore";
        input.targetPackage = "test::graphql::gen::shared";
        input.targetBindingPath = "test::graphql::gen::shared";
        input.targetMappingPath = "test::graphql::gen::shared::TestMapping";

        GraphQLStoreModelGenerationResult result = new GraphQLStoreModelGenerator().generate(input, Identity.getAnonymousIdentity());
        Assert.assertTrue("expected no skipped fields for this schema:\n" + result.skippedTypes, result.skippedTypes.isEmpty());

        String grammar = result.grammar;
        Assert.assertEquals("expected exactly one root ClassMapping for Worker despite being referenced by two polymorphic properties:\n" + grammar,
                1, countOccurrences(grammar, "*test::graphql::gen::shared::Worker: GraphQL"));
        Assert.assertEquals("expected exactly one root ClassMapping for Candidate despite being referenced by two polymorphic properties:\n" + grammar,
                1, countOccurrences(grammar, "*test::graphql::gen::shared::Candidate: GraphQL"));

        PureModelContextData reparsed = PureGrammarParser.newInstance().parseModel(grammar);
        Compiler.compile(reparsed, null, Identity.getAnonymousIdentity().getName());
    }

    private static final String ZERO_FIELD_INTERFACE_INTROSPECTION_RESPONSE =
            "{\"data\":{\"__schema\":{" +
            "\"queryType\":{\"name\":\"Query\"}," +
            "\"mutationType\":null," +
            "\"types\":[" +
            "{\"kind\":\"OBJECT\",\"name\":\"Query\",\"fields\":[" +
            "  {\"name\":\"employees\",\"type\":{\"kind\":\"LIST\",\"ofType\":{\"kind\":\"OBJECT\",\"name\":\"Employee\"}}}" +
            "]}," +
            "{\"kind\":\"OBJECT\",\"name\":\"Employee\",\"fields\":[" +
            "  {\"name\":\"firstName\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}," +
            "  {\"name\":\"member\",\"type\":{\"kind\":\"INTERFACE\",\"name\":\"StaffMember\"}}" +
            "]}," +
            "{\"kind\":\"INTERFACE\",\"name\":\"StaffMember\",\"fields\":[]," +
            "\"possibleTypes\":[{\"kind\":\"OBJECT\",\"name\":\"Worker\"},{\"kind\":\"OBJECT\",\"name\":\"Requisition\"}]}," +
            "{\"kind\":\"OBJECT\",\"name\":\"Worker\",\"fields\":[" +
            "  {\"name\":\"employeeId\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}," +
            "  {\"name\":\"startDate\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}" +
            "]}," +
            "{\"kind\":\"OBJECT\",\"name\":\"Requisition\",\"fields\":[" +
            "  {\"name\":\"requisitionId\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}," +
            "  {\"name\":\"openSince\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}" +
            "]}" +
            "]" +
            "}}}";

    @Test
    public void testGeneratesRealPureInheritanceForZeroFieldBaseInterface() throws Exception
    {
        wireMockServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(ZERO_FIELD_INTERFACE_INTROSPECTION_RESPONSE)));

        GraphQLStoreModelGenerationInput input = new GraphQLStoreModelGenerationInput();
        input.url = url;
        input.storePath = "test::graphql::gen::zerofield::TestStore";
        input.targetPackage = "test::graphql::gen::zerofield";
        input.targetBindingPath = "test::graphql::gen::zerofield";
        input.targetMappingPath = "test::graphql::gen::zerofield::TestMapping";

        GraphQLStoreModelGenerationResult result = new GraphQLStoreModelGenerator().generate(input, Identity.getAnonymousIdentity());
        Assert.assertTrue("expected no skipped fields for a base interface with zero own fields:\n" + result.skippedTypes, result.skippedTypes.isEmpty());

        String grammar = result.grammar;
        Assert.assertTrue("expected a generated StaffMember base Class even with no own fields:\n" + grammar,
                grammar.contains("Class test::graphql::gen::zerofield::StaffMember"));
        Assert.assertTrue("expected a generated Worker Class extending StaffMember:\n" + grammar,
                grammar.contains("test::graphql::gen::zerofield::Worker extends test::graphql::gen::zerofield::StaffMember"));
        Assert.assertTrue("expected a generated Requisition Class extending StaffMember:\n" + grammar,
                grammar.contains("test::graphql::gen::zerofield::Requisition extends test::graphql::gen::zerofield::StaffMember"));

        PureModelContextData reparsed = PureGrammarParser.newInstance().parseModel(grammar);
        Compiler.compile(reparsed, null, Identity.getAnonymousIdentity().getName());
    }

    @Test
    public void testGeneratesRealPureInheritanceForNestedInterfaceTypedProperty() throws Exception
    {
        wireMockServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(NESTED_POLYMORPHIC_PROPERTY_INTROSPECTION_RESPONSE)));

        GraphQLStoreModelGenerationInput input = new GraphQLStoreModelGenerationInput();
        input.url = url;
        input.storePath = "test::graphql::gen::nested::TestStore";
        input.targetPackage = "test::graphql::gen::nested";
        input.targetBindingPath = "test::graphql::gen::nested";
        input.targetMappingPath = "test::graphql::gen::nested::TestMapping";

        GraphQLStoreModelGenerationResult result = new GraphQLStoreModelGenerator().generate(input, Identity.getAnonymousIdentity());

        Assert.assertTrue("expected no skipped fields for this schema:\n" + result.skippedTypes, result.skippedTypes.isEmpty());

        String grammar = result.grammar;
        Assert.assertTrue("expected a generated TestParentObject Class:\n" + grammar, grammar.contains("Class test::graphql::gen::nested::TestParentObject"));
        Assert.assertTrue("expected a generated Worker Class extending TestParentObject:\n" + grammar,
                grammar.contains("test::graphql::gen::nested::Worker extends test::graphql::gen::nested::TestParentObject"));
        Assert.assertTrue("expected a generated Candidate Class extending TestParentObject:\n" + grammar,
                grammar.contains("test::graphql::gen::nested::Candidate extends test::graphql::gen::nested::TestParentObject"));
        Assert.assertTrue("expected Employee.manager typed as TestParentObject:\n" + grammar,
                grammar.contains("manager: test::graphql::gen::nested::TestParentObject[0..1]"));

        Assert.assertFalse("expected no standalone GraphQL class mapping for TestParentObject - it's folded/embedded:\n" + grammar,
                grammar.contains("test::graphql::gen::nested::TestParentObject: GraphQL"));
        // Worker/Candidate each need a minimal, otherwise-unused root GraphQL ClassMapping - confirmed
        // empirically that the generic router requires some discoverable mapping for any class referenced in
        // a routed expression, including the target of a query's own property->subType(@X){...} branch -
        // this is NOT an embedding, Worker/Candidate are never fetched through it, it exists purely to
        // satisfy that routing requirement.
        Assert.assertTrue("expected a minimal GraphQL class mapping for Worker:\n" + grammar,
                grammar.contains("*test::graphql::gen::nested::Worker: GraphQL"));
        Assert.assertTrue("expected a minimal GraphQL class mapping for Candidate:\n" + grammar,
                grammar.contains("*test::graphql::gen::nested::Candidate: GraphQL"));

        PureModelContextData reparsed = PureGrammarParser.newInstance().parseModel(grammar);
        Compiler.compile(reparsed, null, Identity.getAnonymousIdentity().getName());
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
            "  {\"name\":\"manager\",\"type\":{\"kind\":\"INTERFACE\",\"name\":\"TestParentObject\"}}" +
            "]}," +
            "{\"kind\":\"INTERFACE\",\"name\":\"TestParentObject\",\"fields\":[" +
            "  {\"name\":\"id\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}" +
            "],\"possibleTypes\":[{\"kind\":\"OBJECT\",\"name\":\"Candidate\"},{\"kind\":\"OBJECT\",\"name\":\"SeniorWorker\"},{\"kind\":\"OBJECT\",\"name\":\"RegularWorker\"}]}," +
            "{\"kind\":\"INTERFACE\",\"name\":\"WorkerBase\",\"interfaces\":[{\"kind\":\"INTERFACE\",\"name\":\"TestParentObject\"}],\"fields\":[" +
            "  {\"name\":\"id\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}," +
            "  {\"name\":\"employeeId\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}" +
            "],\"possibleTypes\":[{\"kind\":\"OBJECT\",\"name\":\"SeniorWorker\"},{\"kind\":\"OBJECT\",\"name\":\"RegularWorker\"}]}," +
            "{\"kind\":\"OBJECT\",\"name\":\"SeniorWorker\",\"interfaces\":[{\"kind\":\"INTERFACE\",\"name\":\"WorkerBase\"},{\"kind\":\"INTERFACE\",\"name\":\"TestParentObject\"}],\"fields\":[" +
            "  {\"name\":\"id\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}," +
            "  {\"name\":\"employeeId\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}," +
            "  {\"name\":\"bonus\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}" +
            "]}," +
            "{\"kind\":\"OBJECT\",\"name\":\"RegularWorker\",\"interfaces\":[{\"kind\":\"INTERFACE\",\"name\":\"WorkerBase\"},{\"kind\":\"INTERFACE\",\"name\":\"TestParentObject\"}],\"fields\":[" +
            "  {\"name\":\"id\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}," +
            "  {\"name\":\"employeeId\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}," +
            "  {\"name\":\"shiftPattern\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}" +
            "]}," +
            "{\"kind\":\"OBJECT\",\"name\":\"Candidate\",\"interfaces\":[{\"kind\":\"INTERFACE\",\"name\":\"TestParentObject\"}],\"fields\":[" +
            "  {\"name\":\"id\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}," +
            "  {\"name\":\"applicationId\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}" +
            "]}" +
            "]" +
            "}}}";

    @Test
    public void testGeneratesMultiLevelPureInheritanceForIntermediateGraphQLInterface() throws Exception
    {
        wireMockServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(MULTI_LEVEL_INTERFACE_HIERARCHY_INTROSPECTION_RESPONSE)));

        GraphQLStoreModelGenerationInput input = new GraphQLStoreModelGenerationInput();
        input.url = url;
        input.storePath = "test::graphql::gen::multilevel::TestStore";
        input.targetPackage = "test::graphql::gen::multilevel";
        input.targetBindingPath = "test::graphql::gen::multilevel";
        input.targetMappingPath = "test::graphql::gen::multilevel::TestMapping";

        GraphQLStoreModelGenerationResult result = new GraphQLStoreModelGenerator().generate(input, Identity.getAnonymousIdentity());
        Assert.assertTrue("expected no skipped fields for this schema:\n" + result.skippedTypes, result.skippedTypes.isEmpty());

        String grammar = result.grammar;
        Assert.assertTrue("expected a generated TestParentObject Class:\n" + grammar, grammar.contains("Class test::graphql::gen::multilevel::TestParentObject"));
        Assert.assertTrue("expected a generated WorkerBase Class extending TestParentObject directly:\n" + grammar,
                grammar.contains("test::graphql::gen::multilevel::WorkerBase extends test::graphql::gen::multilevel::TestParentObject"));
        Assert.assertTrue("expected SeniorWorker to extend the intermediate WorkerBase, not TestParentObject directly:\n" + grammar,
                grammar.contains("test::graphql::gen::multilevel::SeniorWorker extends test::graphql::gen::multilevel::WorkerBase"));
        Assert.assertTrue("expected RegularWorker to extend the intermediate WorkerBase, not TestParentObject directly:\n" + grammar,
                grammar.contains("test::graphql::gen::multilevel::RegularWorker extends test::graphql::gen::multilevel::WorkerBase"));
        Assert.assertTrue("expected the flat, single-level Candidate to still extend TestParentObject directly (regression check):\n" + grammar,
                grammar.contains("test::graphql::gen::multilevel::Candidate extends test::graphql::gen::multilevel::TestParentObject"));

        int seniorWorkerDeclStart = grammar.indexOf("test::graphql::gen::multilevel::SeniorWorker extends");
        Assert.assertFalse("expected SeniorWorker to declare its own field only, not WorkerBase's inherited employeeId:\n" + grammar,
                grammar.substring(seniorWorkerDeclStart, seniorWorkerDeclStart + 200).contains("employeeId"));
        int workerBaseDeclStart = grammar.indexOf("test::graphql::gen::multilevel::WorkerBase extends");
        Assert.assertTrue("expected WorkerBase to declare its own employeeId field:\n" + grammar,
                grammar.substring(workerBaseDeclStart, workerBaseDeclStart + 200).contains("employeeId"));

        PureModelContextData reparsed = PureGrammarParser.newInstance().parseModel(grammar);
        Compiler.compile(reparsed, null, Identity.getAnonymousIdentity().getName());
    }

    private static final String ROOT_POLYMORPHIC_PROPERTY_INTROSPECTION_RESPONSE =
            "{\"data\":{\"__schema\":{" +
            "\"queryType\":{\"name\":\"Query\"}," +
            "\"mutationType\":null," +
            "\"types\":[" +
            "{\"kind\":\"OBJECT\",\"name\":\"Query\",\"fields\":[" +
            "  {\"name\":\"currentWorker\",\"type\":{\"kind\":\"INTERFACE\",\"name\":\"TestParentObject\"}}," +
            "  {\"name\":\"executeQuery\",\"type\":{\"kind\":\"INTERFACE\",\"name\":\"TestParentObject\"},\"args\":[{\"name\":\"id\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"Int\"}}]}" +
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
    public void testGeneratesRealPureInheritanceAndClassMappingsForPolymorphicQueryRootFields() throws Exception
    {
        wireMockServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(ROOT_POLYMORPHIC_PROPERTY_INTROSPECTION_RESPONSE)));

        GraphQLStoreModelGenerationInput input = new GraphQLStoreModelGenerationInput();
        input.url = url;
        input.storePath = "test::graphql::gen::rootpoly::TestStore";
        input.targetPackage = "test::graphql::gen::rootpoly";
        input.targetBindingPath = "test::graphql::gen::rootpoly";
        input.targetMappingPath = "test::graphql::gen::rootpoly::TestMapping";

        GraphQLStoreModelGenerationResult result = new GraphQLStoreModelGenerator().generate(input, Identity.getAnonymousIdentity());
        Assert.assertTrue("expected no skipped fields - a Query-root field returning an interface is now supported:\n" + result.skippedTypes, result.skippedTypes.isEmpty());

        String grammar = result.grammar;
        Assert.assertTrue("expected the plain polymorphic root field typed as the interface, not String:\n" + grammar,
                grammar.contains("currentWorker: test::graphql::gen::rootpoly::TestParentObject[0..1]"));
        Assert.assertTrue("expected the argument-taking polymorphic root field's proxy typed as the interface, not String:\n" + grammar,
                grammar.contains("executeQueryProxy: test::graphql::gen::rootpoly::TestParentObject[0..1]"));
        Assert.assertTrue("expected the executeQuery QP itself typed as the interface:\n" + grammar,
                grammar.contains("executeQuery(id: Integer[0..1]) { $this.executeQueryProxy }: test::graphql::gen::rootpoly::TestParentObject[0..1];"));
        Assert.assertTrue("expected a minimal GraphQL class mapping for Worker (needed for ->subType(@Worker) routing):\n" + grammar,
                grammar.contains("*test::graphql::gen::rootpoly::Worker: GraphQL"));
        Assert.assertTrue("expected a minimal GraphQL class mapping for Candidate:\n" + grammar,
                grammar.contains("*test::graphql::gen::rootpoly::Candidate: GraphQL"));

        PureModelContextData reparsed = PureGrammarParser.newInstance().parseModel(grammar);
        Compiler.compile(reparsed, null, Identity.getAnonymousIdentity().getName());
    }

    // OrphanType is never reachable from Query (nothing references it), so SchemaToModelGenerator's
    // JSON-schema-driven generation never produces a Pure Class for it or for its field's interface's
    // possible-types - but findPolymorphicPropertyPossibleTypes scans every OBJECT type in the whole
    // introspected schema, reachable or not, so WidgetA/WidgetB would be found as possible-types
    // regardless. Exercises GraphQLStoreModelGenerator's generatedClassNames filter (#5 fix).
    private static final String UNREACHABLE_POLYMORPHIC_PROPERTY_INTROSPECTION_RESPONSE =
            "{\"data\":{\"__schema\":{" +
            "\"queryType\":{\"name\":\"Query\"}," +
            "\"mutationType\":null," +
            "\"types\":[" +
            "{\"kind\":\"OBJECT\",\"name\":\"Query\",\"fields\":[" +
            "  {\"name\":\"employees\",\"type\":{\"kind\":\"LIST\",\"ofType\":{\"kind\":\"OBJECT\",\"name\":\"Employee\"}}}" +
            "]}," +
            "{\"kind\":\"OBJECT\",\"name\":\"Employee\",\"fields\":[" +
            "  {\"name\":\"firstName\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}" +
            "]}," +
            "{\"kind\":\"OBJECT\",\"name\":\"OrphanType\",\"fields\":[" +
            "  {\"name\":\"widget\",\"type\":{\"kind\":\"INTERFACE\",\"name\":\"WidgetInterface\"}}" +
            "]}," +
            "{\"kind\":\"INTERFACE\",\"name\":\"WidgetInterface\",\"fields\":[]," +
            "\"possibleTypes\":[{\"kind\":\"OBJECT\",\"name\":\"WidgetA\"},{\"kind\":\"OBJECT\",\"name\":\"WidgetB\"}]}," +
            "{\"kind\":\"OBJECT\",\"name\":\"WidgetA\",\"fields\":[" +
            "  {\"name\":\"id\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}" +
            "]}," +
            "{\"kind\":\"OBJECT\",\"name\":\"WidgetB\",\"fields\":[" +
            "  {\"name\":\"id\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}" +
            "]}" +
            "]" +
            "}}}";

    @Test
    public void testSkipsClassMappingForPolymorphicPossibleTypeUnreachableFromQuery() throws Exception
    {
        wireMockServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(UNREACHABLE_POLYMORPHIC_PROPERTY_INTROSPECTION_RESPONSE)));

        GraphQLStoreModelGenerationInput input = new GraphQLStoreModelGenerationInput();
        input.url = url;
        input.storePath = "test::graphql::gen::orphan::TestStore";
        input.targetPackage = "test::graphql::gen::orphan";
        input.targetBindingPath = "test::graphql::gen::orphan";
        input.targetMappingPath = "test::graphql::gen::orphan::TestMapping";

        GraphQLStoreModelGenerationResult result = new GraphQLStoreModelGenerator().generate(input, Identity.getAnonymousIdentity());

        String grammar = result.grammar;
        Assert.assertFalse("expected no class mapping for WidgetA, since it was never actually generated:\n" + grammar,
                grammar.contains("WidgetA: GraphQL"));
        Assert.assertFalse("expected no class mapping for WidgetB, since it was never actually generated:\n" + grammar,
                grammar.contains("WidgetB: GraphQL"));

        PureModelContextData reparsed = PureGrammarParser.newInstance().parseModel(grammar);
        Compiler.compile(reparsed, null, Identity.getAnonymousIdentity().getName());
    }

    @Test
    public void testCredentialProviderProviderInjectedAtConstructionIsActuallyUsedForIntrospection() throws Exception
    {
        wireMockServer.stubFor(post(urlEqualTo("/graphql"))
                .withHeader("Authorization", equalTo("Bearer oauth-access-token"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(INTROSPECTION_RESPONSE)));

        GraphQLStoreModelGenerationInput input = new GraphQLStoreModelGenerationInput();
        input.url = url;
        input.authenticationSpecification = new TestOAuthAuthenticationSpecification();
        input.storePath = "test::graphql::gen::auth::TestStore";
        input.targetPackage = "test::graphql::gen::auth";
        input.targetBindingPath = "test::graphql::gen::auth";
        input.targetMappingPath = "test::graphql::gen::auth::TestMapping";

        Identity identity = new Identity("test", new OAuthCredential("oauth-access-token"));
        CredentialProviderProvider credentialProviderProvider = CredentialProviderProvider.builder().with(new TestOAuthCredentialProvider()).build();

        GraphQLStoreModelGenerationResult result = new GraphQLStoreModelGenerator(credentialProviderProvider).generate(input, identity);

        Assert.assertTrue("expected a generated Query wrapper Class:\n" + result.grammar, result.grammar.contains("Class test::graphql::gen::auth::Query"));
        wireMockServer.verify(com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor(urlEqualTo("/graphql"))
                .withHeader("Authorization", equalTo("Bearer oauth-access-token")));
    }

    @Test
    public void testNonOkIntrospectionResponseThrowsEngineExceptionWithStatusAndResponseBody()
    {
        wireMockServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(500).withBody("upstream error detail")));

        GraphQLStoreModelGenerationInput input = new GraphQLStoreModelGenerationInput();
        input.url = url;
        input.storePath = "test::graphql::gen::introspectionerror::TestStore";
        input.targetPackage = "test::graphql::gen::introspectionerror";
        input.targetBindingPath = "test::graphql::gen::introspectionerror";
        input.targetMappingPath = "test::graphql::gen::introspectionerror::TestMapping";

        EngineException exception = Assert.assertThrows(EngineException.class, () -> new GraphQLStoreModelGenerator().generate(input, Identity.getAnonymousIdentity()));
        Assert.assertTrue("expected a clear error mentioning the status code:\n" + exception.getMessage(), exception.getMessage().contains("500"));
        Assert.assertTrue("expected the upstream response body in the error, matching ServiceStore/Elasticsearch's own precedent of surfacing it inline:\n" + exception.getMessage(),
                exception.getMessage().contains("upstream error detail"));
    }

    @Test
    public void testNonHttpIntrospectionUrlSchemeRejectedBeforeAnyRequestIsMade()
    {
        GraphQLStoreModelGenerationInput input = new GraphQLStoreModelGenerationInput();
        input.url = "file:///etc/passwd";
        input.storePath = "test::graphql::gen::badurl::TestStore";
        input.targetPackage = "test::graphql::gen::badurl";
        input.targetBindingPath = "test::graphql::gen::badurl";
        input.targetMappingPath = "test::graphql::gen::badurl::TestMapping";

        EngineException exception = Assert.assertThrows(EngineException.class, () -> new GraphQLStoreModelGenerator().generate(input, Identity.getAnonymousIdentity()));
        Assert.assertTrue("expected a clear error about the unsupported scheme:\n" + exception.getMessage(),
                exception.getMessage().contains("http or https scheme"));
    }

    @Test
    public void testMalformedIntrospectionUrlRejectedBeforeAnyRequestIsMade()
    {
        GraphQLStoreModelGenerationInput input = new GraphQLStoreModelGenerationInput();
        input.url = "not a url";
        input.storePath = "test::graphql::gen::malformedurl::TestStore";
        input.targetPackage = "test::graphql::gen::malformedurl";
        input.targetBindingPath = "test::graphql::gen::malformedurl";
        input.targetMappingPath = "test::graphql::gen::malformedurl::TestMapping";

        EngineException exception = Assert.assertThrows(EngineException.class, () -> new GraphQLStoreModelGenerator().generate(input, Identity.getAnonymousIdentity()));
        Assert.assertTrue("expected a clear error about the malformed URL:\n" + exception.getMessage(),
                exception.getMessage().contains("not a valid URL"));
    }

    @Test
    public void testRenamedRootOperationTypeIsUsedAsWrapperClassNameInsteadOfHardcodedQuery() throws Exception
    {
        String introspectionResponseWithRenamedRootType =
                "{\"data\":{\"__schema\":{" +
                "\"queryType\":{\"name\":\"RootQuery\"}," +
                "\"mutationType\":null," +
                "\"types\":[" +
                "{\"kind\":\"OBJECT\",\"name\":\"RootQuery\",\"fields\":[" +
                "  {\"name\":\"employees\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"LIST\",\"ofType\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"OBJECT\",\"name\":\"Employee\"}}}}}" +
                "]}," +
                "{\"kind\":\"OBJECT\",\"name\":\"Employee\",\"fields\":[" +
                "  {\"name\":\"firstName\",\"type\":{\"kind\":\"NON_NULL\",\"ofType\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}}" +
                "]}" +
                "]" +
                "}}}";

        wireMockServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(introspectionResponseWithRenamedRootType)));

        GraphQLStoreModelGenerationInput input = new GraphQLStoreModelGenerationInput();
        input.url = url;
        input.storePath = "test::graphql::gen::renamedroot::TestStore";
        input.targetPackage = "test::graphql::gen::renamedroot";
        input.targetBindingPath = "test::graphql::gen::renamedroot";
        input.targetMappingPath = "test::graphql::gen::renamedroot::TestMapping";

        GraphQLStoreModelGenerationResult result = new GraphQLStoreModelGenerator().generate(input, Identity.getAnonymousIdentity());

        String grammar = result.grammar;
        Assert.assertTrue("expected the wrapper Class to be named after the schema's own root operation type, not hardcoded 'Query':\n" + grammar,
                grammar.contains("Class test::graphql::gen::renamedroot::RootQuery"));
        Assert.assertFalse("did not expect a hardcoded 'Query' wrapper Class when the root type is actually named 'RootQuery':\n" + grammar,
                grammar.contains("Class test::graphql::gen::renamedroot::Query"));

        PureModelContextData reparsed = PureGrammarParser.newInstance().parseModel(grammar);
        Compiler.compile(reparsed, null, Identity.getAnonymousIdentity().getName());
    }

    private static int countOccurrences(String text, String substring)
    {
        int count = 0;
        Matcher matcher = Pattern.compile(Pattern.quote(substring)).matcher(text);
        while (matcher.find())
        {
            count++;
        }
        return count;
    }

    private static class TestOAuthAuthenticationSpecification extends AuthenticationSpecification
    {
        @Override
        public <T> T accept(AuthenticationSpecificationVisitor<T> visitor)
        {
            return visitor.visit((AuthenticationSpecification) this);
        }
    }

    private static class TestOAuthCredentialProvider extends CredentialProvider<TestOAuthAuthenticationSpecification, OAuthCredential>
    {
        @Override
        public OAuthCredential makeCredential(TestOAuthAuthenticationSpecification specification, Identity identity) throws Exception
        {
            return (OAuthCredential) super.makeCredential(specification, identity, OAuthCredential.class);
        }
    }
}
