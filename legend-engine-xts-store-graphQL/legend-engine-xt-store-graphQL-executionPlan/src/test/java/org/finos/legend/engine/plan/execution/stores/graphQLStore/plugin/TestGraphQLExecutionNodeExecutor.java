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
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.finos.legend.authentication.credentialprovider.CredentialProvider;
import org.finos.legend.authentication.credentialprovider.CredentialProviderProvider;
import org.finos.legend.authentication.intermediationrule.IntermediationRule;
import org.finos.legend.authentication.vault.CredentialVaultProvider;
import org.finos.legend.engine.plan.execution.graphFetch.GraphFetchExecutionConfiguration;
import org.finos.legend.engine.plan.execution.nodes.ExecutionNodeExecutor;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.result.InputStreamResult;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.protocol.graphQLStore.executionPlan.GraphQLExecutionNode;
import org.finos.legend.engine.protocol.graphQLStore.executionPlan.GraphQLVariableBinding;
import org.finos.legend.engine.protocol.pure.m3.multiplicity.Multiplicity;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.VariableInput;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.AuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.AuthenticationSpecificationVisitor;
import org.finos.legend.engine.shared.core.identity.Credential;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.AnonymousCredential;
import org.finos.legend.engine.shared.core.identity.credential.OAuthCredential;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.core.port.DynamicPortGenerator;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

public class TestGraphQLExecutionNodeExecutor
{
    private WireMockServer wireMockServer;
    private String url;

    @Before
    public void setup()
    {
        int port = DynamicPortGenerator.generatePort();
        this.wireMockServer = new WireMockServer(port);
        this.wireMockServer.start();
        WireMock.configureFor("localhost", port);
        this.url = "http://localhost:" + port + "/graphql";
    }

    @After
    public void teardown()
    {
        this.wireMockServer.stop();
    }

    @Test
    public void testHappyPath() throws IOException
    {
        this.wireMockServer.stubFor(post(urlEqualTo("/graphql"))
                .withRequestBody(equalToJson("{\"query\":\"query { employees { firstName } }\",\"variables\":{}}"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"employees\":[{\"firstName\":\"Ada\"}]}}")));

        GraphQLExecutionNode node = new GraphQLExecutionNode();
        node.url = this.url;
        node.query = "query { employees { firstName } }";

        Result result = execute(node);
        Assert.assertTrue(result instanceof InputStreamResult);
        String data = readAll(((InputStreamResult) result).getInputStream());
        Assert.assertEquals("{\"employees\":[{\"firstName\":\"Ada\"}]}", data);
    }

    @Test
    public void testVariableValueWithEmbeddedQuoteIsSafelyEscaped() throws IOException
    {
        this.wireMockServer.stubFor(post(urlEqualTo("/graphql"))
                .withRequestBody(equalToJson("{\"query\":\"query($department: String!) {\\n  employees(department: $department) {\\n    firstName\\n  }\\n}\",\"variables\":{\"department\":\"Sales\\\" } evil { hack\"}}"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"employees\":[{\"firstName\":\"Ada\"}]}}")));

        GraphQLExecutionNode node = new GraphQLExecutionNode();
        node.url = this.url;
        node.query = "query($department: String!) {\n  employees(department: $department) {\n    firstName\n  }\n}";
        GraphQLVariableBinding binding = new GraphQLVariableBinding();
        binding.name = "department";
        binding.value = "\"Sales\\\" } evil { hack\"";
        node.variables = Collections.singletonList(binding);

        Result result = execute(node);
        Assert.assertTrue(result instanceof InputStreamResult);
        String data = readAll(((InputStreamResult) result).getInputStream());
        Assert.assertEquals("{\"employees\":[{\"firstName\":\"Ada\"}]}", data);
    }

    @Test
    public void testHardFailsOnGraphQLErrors()
    {
        this.wireMockServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":null,\"errors\":[{\"message\":\"Field 'employees' not found\"}]}")));

        GraphQLExecutionNode node = new GraphQLExecutionNode();
        node.url = this.url;
        node.query = "query { employees { firstName } }";

        try
        {
            execute(node);
            Assert.fail("Expected a RuntimeException for a non-empty GraphQL errors array");
        }
        catch (RuntimeException e)
        {
            Assert.assertTrue(e.getMessage().contains("Field 'employees' not found"));
        }
    }

    @Test
    public void testFailsClearlyWhenARequiredVariableHasNoResolvedValue()
    {
        GraphQLExecutionNode node = new GraphQLExecutionNode();
        node.url = this.url;
        node.query = "query($id: String!) { employee(id: $id) { firstName } }";
        VariableInput requiredInput = new VariableInput();
        requiredInput.name = "id";
        requiredInput.type = "String";
        requiredInput.multiplicity = new Multiplicity(1, 1);
        node.requiredVariableInputs = Collections.singletonList(requiredInput);

        try
        {
            execute(node);
            Assert.fail("Expected a clear error for a required variable with no resolved value");
        }
        catch (EngineException e)
        {
            Assert.assertTrue("Unexpected error message: " + e.getMessage(), e.getMessage().contains("unable to resolve required variable 'id'"));
        }
    }

    @Test
    public void testHardFailsOnNon200()
    {
        this.wireMockServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(500).withBody("internal error")));

        GraphQLExecutionNode node = new GraphQLExecutionNode();
        node.url = this.url;
        node.query = "query { employees { firstName } }";

        try
        {
            execute(node);
            Assert.fail("Expected a RuntimeException for a non-200 response");
        }
        catch (RuntimeException e)
        {
            Assert.assertTrue(e.getMessage().contains("500"));
        }
    }

    @Test
    public void testOAuthCredentialSentAsBearerAuthorizationHeader() throws IOException
    {
        this.wireMockServer.stubFor(post(urlEqualTo("/graphql"))
                .withHeader("Authorization", equalTo("Bearer oauth-access-token"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"employees\":[{\"firstName\":\"Ada\"}]}}")));

        GraphQLExecutionNode node = new GraphQLExecutionNode();
        node.url = this.url;
        node.query = "query { employees { firstName } }";
        node.authenticationSpecification = new TestOAuthAuthenticationSpecification();

        Identity identity = new Identity("test", new OAuthCredential("oauth-access-token"));
        CredentialProviderProvider credentialProviderProvider = CredentialProviderProvider.builder().with(new TestOAuthCredentialProvider()).build();

        Result result = execute(node, identity, credentialProviderProvider);
        Assert.assertTrue(result instanceof InputStreamResult);
    }

    @Test
    public void testFetchedOAuthCredentialTokenIsFetchedAndSentAsBearerAuthorizationHeader() throws Exception
    {
        WireMockServer tokenServer = new WireMockServer(DynamicPortGenerator.generatePort());
        tokenServer.start();
        try
        {
            tokenServer.stubFor(post(urlPathEqualTo("/oauth2/token"))
                    .willReturn(aResponse().withStatus(200).withBody("fetched-oauth-token")));

            this.wireMockServer.stubFor(post(urlEqualTo("/graphql"))
                    .withHeader("Authorization", equalTo("Bearer fetched-oauth-token"))
                    .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                            .withBody("{\"data\":{\"employees\":[{\"firstName\":\"Ada\"}]}}")));

            TestFetchedOAuthAuthenticationSpecification spec = new TestFetchedOAuthAuthenticationSpecification("http://localhost:" + tokenServer.port() + "/oauth2/token");

            GraphQLExecutionNode node = new GraphQLExecutionNode();
            node.url = this.url;
            node.query = "query { employees { firstName } }";
            node.authenticationSpecification = spec;

            CredentialVaultProvider vaultProvider = CredentialVaultProvider.builder().build();
            TestTokenFetchingRule rule = new TestTokenFetchingRule(vaultProvider);
            TestFetchedOAuthCredentialProvider provider = new TestFetchedOAuthCredentialProvider(Lists.mutable.with(rule));
            CredentialProviderProvider credentialProviderProvider = CredentialProviderProvider.builder().with(provider).build();

            Result result = execute(node, new Identity("test", new AnonymousCredential()), credentialProviderProvider);
            Assert.assertTrue(result instanceof InputStreamResult);
        }
        finally
        {
            tokenServer.stop();
        }
    }

    @Test
    public void testUnsupportedAuthenticationSpecificationThrowsClearlyInsteadOfSendingUnauthenticated() throws IOException
    {
        this.wireMockServer.stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"employees\":[{\"firstName\":\"Ada\"}]}}")));

        GraphQLExecutionNode node = new GraphQLExecutionNode();
        node.url = this.url;
        node.query = "query { employees { firstName } }";
        node.authenticationSpecification = new TestUnsupportedAuthenticationSpecification();

        Identity identity = new Identity("test", new TestUnsupportedCredential());
        CredentialProviderProvider credentialProviderProvider = CredentialProviderProvider.builder().with(new TestUnsupportedCredentialProvider()).build();

        try
        {
            execute(node, identity, credentialProviderProvider);
            Assert.fail("Expected a RuntimeException for an unsupported AuthenticationSpecification/Credential combination");
        }
        catch (RuntimeException e)
        {
            Assert.assertTrue("Unexpected message: " + e.getMessage(), e.getMessage().contains("AuthenticationSpecification not supported"));
        }
    }

    private static class TestUnsupportedAuthenticationSpecification extends AuthenticationSpecification
    {
        @Override
        public <T> T accept(AuthenticationSpecificationVisitor<T> visitor)
        {
            return visitor.visit((AuthenticationSpecification) this);
        }
    }

    private static class TestUnsupportedCredential implements org.finos.legend.engine.shared.core.identity.Credential
    {
    }

    private static class TestUnsupportedCredentialProvider extends CredentialProvider<TestUnsupportedAuthenticationSpecification, TestUnsupportedCredential>
    {
        @Override
        public TestUnsupportedCredential makeCredential(TestUnsupportedAuthenticationSpecification specification, Identity identity)
        {
            return new TestUnsupportedCredential();
        }
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

    private static class TestFetchedOAuthAuthenticationSpecification extends AuthenticationSpecification
    {
        private final String tokenEndpointUrl;

        private TestFetchedOAuthAuthenticationSpecification(String tokenEndpointUrl)
        {
            this.tokenEndpointUrl = tokenEndpointUrl;
        }

        @Override
        public <T> T accept(AuthenticationSpecificationVisitor<T> visitor)
        {
            return visitor.visit((AuthenticationSpecification) this);
        }
    }

    private static class TestFetchedOAuthCredentialProvider extends CredentialProvider<TestFetchedOAuthAuthenticationSpecification, OAuthCredential>
    {
        private TestFetchedOAuthCredentialProvider(List<IntermediationRule> intermediationRules)
        {
            super(intermediationRules);
        }

        @Override
        public OAuthCredential makeCredential(TestFetchedOAuthAuthenticationSpecification specification, Identity identity) throws Exception
        {
            return (OAuthCredential) super.makeCredential(specification, identity, OAuthCredential.class);
        }
    }

    private static class TestTokenFetchingRule extends IntermediationRule<TestFetchedOAuthAuthenticationSpecification, Credential, OAuthCredential>
    {
        private TestTokenFetchingRule(CredentialVaultProvider credentialVaultProvider)
        {
            super(credentialVaultProvider);
        }

        @Override
        public OAuthCredential makeCredential(TestFetchedOAuthAuthenticationSpecification spec, Credential cred, Identity identity) throws Exception
        {
            HttpPost request = new HttpPost(spec.tokenEndpointUrl);
            try (CloseableHttpClient httpClient = HttpClients.createDefault(); CloseableHttpResponse response = httpClient.execute(request))
            {
                return new OAuthCredential(EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8));
            }
        }
    }

    private static Result execute(GraphQLExecutionNode node)
    {
        return execute(node, Identity.getAnonymousIdentity(), CredentialProviderProvider.builder().build());
    }

    private static Result execute(GraphQLExecutionNode node, Identity identity, CredentialProviderProvider credentialProviderProvider)
    {
        GraphQLStoreExecutionState graphQLStoreExecutionState = new GraphQLStoreExecutionState(new GraphQLStoreState(), credentialProviderProvider);
        ExecutionState executionState = new ExecutionState(Maps.mutable.empty(), Lists.mutable.empty(), Lists.mutable.with(graphQLStoreExecutionState), true, new GraphFetchExecutionConfiguration());
        return node.accept(new ExecutionNodeExecutor(identity, executionState));
    }

    private static String readAll(InputStream inputStream) throws IOException
    {
        byte[] bytes = new byte[8192];
        int read;
        StringBuilder builder = new StringBuilder();
        while ((read = inputStream.read(bytes)) != -1)
        {
            builder.append(new String(bytes, 0, read, StandardCharsets.UTF_8));
        }
        return builder.toString();
    }
}
