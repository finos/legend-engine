// Copyright 2023 Goldman Sachs
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

package org.finos.legend.connection;

import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.connection.protocol.AuthenticationConfiguration;
import org.finos.legend.connection.protocol.AuthenticationMechanism;
import org.finos.legend.connection.protocol.ConnectionSpecification;
import org.finos.legend.engine.shared.core.identity.Credential;
import org.finos.legend.engine.shared.core.identity.Identity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ConnectionFactory
{
    private final LegendEnvironment environment;
    private final StoreInstanceProvider storeInstanceProvider;
    private final Map<CredentialBuilder.Key, CredentialBuilder> credentialBuildersIndex = new LinkedHashMap<>();
    private final Map<ConnectionBuilder.Key, ConnectionBuilder> connectionBuildersIndex = new LinkedHashMap<>();

    private ConnectionFactory(LegendEnvironment environment, StoreInstanceProvider storeInstanceProvider, List<CredentialBuilder> credentialBuilders, List<ConnectionBuilder> connectionBuilders)
    {
        this.environment = environment;
        this.storeInstanceProvider = storeInstanceProvider;
        for (ConnectionBuilder<?, ?, ?> builder : connectionBuilders)
        {
            this.connectionBuildersIndex.put(new ConnectionBuilder.Key(builder.getConnectionSpecificationType(), builder.getCredentialType()), builder);
        }
        for (CredentialBuilder<?, ?, ?> builder : credentialBuilders)
        {
            this.credentialBuildersIndex.put(new CredentialBuilder.Key(builder.getAuthenticationConfigurationType(), builder.getInputCredentialType(), builder.getOutputCredentialType()), builder);
        }
    }

    public LegendEnvironment getEnvironment()
    {
        return environment;
    }

    public Authenticator getAuthenticator(Identity identity, String storeInstanceIdentifier, AuthenticationMechanism authenticationMechanism)
    {
        return this.getAuthenticator(identity, this.storeInstanceProvider.lookup(storeInstanceIdentifier), authenticationMechanism);
    }

    public Authenticator getAuthenticator(Identity identity, StoreInstance storeInstance, AuthenticationMechanism authenticationMechanism)
    {
        AuthenticationMechanismConfiguration authenticationMechanismConfiguration = Objects.requireNonNull(storeInstance.getAuthenticationMechanismConfiguration(authenticationMechanism), String.format("Store '%s' does not support authentication mechanism '%s'. Supported mechanism(s):\n%s",
                storeInstance.getIdentifier(),
                authenticationMechanism.getLabel(),
                ListIterate.collect(storeInstance.getAuthenticationMechanisms(), mechanism -> "- " + mechanism.getLabel()).makeString("\n")
        ));
        Function0<AuthenticationConfiguration> generator = authenticationMechanismConfiguration.getDefaultAuthenticationConfigurationGenerator();
        AuthenticationConfiguration authenticationConfiguration = Objects.requireNonNull(generator != null ? generator.get() : null, String.format("Can't auto-generate authentication configuration for store '%s' with authentication mechanism '%s'. Please provide a configuration of one of the following type(s):\n%s",
                storeInstance.getIdentifier(),
                authenticationMechanism.getLabel(),
                authenticationMechanismConfiguration.getAuthenticationConfigurationTypes().collect(configType -> "- " + configType.getSimpleName()).makeString("\n")
        ));
        return this.getAuthenticator(identity, storeInstance, authenticationMechanism, authenticationConfiguration);
    }

    public Authenticator getAuthenticator(Identity identity, String storeInstanceIdentifier, AuthenticationConfiguration authenticationConfiguration)
    {
        return this.getAuthenticator(identity, this.storeInstanceProvider.lookup(storeInstanceIdentifier), authenticationConfiguration);
    }

    public Authenticator getAuthenticator(Identity identity, StoreInstance storeInstance, AuthenticationConfiguration authenticationConfiguration)
    {
        AuthenticationMechanism authenticationMechanism = Objects.requireNonNull(storeInstance.getAuthenticationMechanism(authenticationConfiguration.getClass()), String.format("Store '%s' does not accept authentication configuration type '%s'. Supported configuration type(s):\n%s",
                storeInstance.getIdentifier(),
                authenticationConfiguration.getClass().getSimpleName(),
                ListIterate.collect(storeInstance.getAuthenticationConfigurationTypes(), configType -> "- " + configType.getSimpleName()).makeString("\n")
        ));
        return this.getAuthenticator(identity, storeInstance, authenticationMechanism, authenticationConfiguration);
    }

    private Authenticator getAuthenticator(Identity identity, StoreInstance storeInstance, AuthenticationMechanism authenticationMechanism, AuthenticationConfiguration authenticationConfiguration)
    {
        AuthenticationFlowResolver.ResolutionResult result = AuthenticationFlowResolver.run(this.credentialBuildersIndex, this.connectionBuildersIndex, identity, authenticationMechanism, authenticationConfiguration, storeInstance.getConnectionSpecification());
        if (result == null)
        {
            throw new RuntimeException(String.format("No authentication flow for store '%s' can be resolved for the specified identity (authentication configuration: %s, connection specification: %s)",
                    storeInstance.getIdentifier(),
                    authenticationConfiguration.getClass().getSimpleName(),
                    storeInstance.getConnectionSpecification().getClass().getSimpleName()
            ));
        }
        return new Authenticator(storeInstance, authenticationMechanism, authenticationConfiguration, result.sourceCredentialType, result.targetCredentialType, result.flow, connectionBuildersIndex.get(new ConnectionBuilder.Key(storeInstance.getConnectionSpecification().getClass(), result.targetCredentialType)), this.environment);
    }

    public Authenticator getAuthenticator(Identity identity, String storeInstanceIdentifier)
    {
        return this.getAuthenticator(identity, this.storeInstanceProvider.lookup(storeInstanceIdentifier));
    }

    public Authenticator getAuthenticator(Identity identity, StoreInstance storeInstance)
    {
        Authenticator authenticator = null;
        for (AuthenticationMechanism authenticationMechanism : storeInstance.getAuthenticationMechanisms())
        {
            AuthenticationMechanismConfiguration authenticationMechanismConfiguration = storeInstance.getAuthenticationMechanismConfiguration(authenticationMechanism);
            Function0<AuthenticationConfiguration> generator = authenticationMechanismConfiguration.getDefaultAuthenticationConfigurationGenerator();
            AuthenticationConfiguration authenticationConfiguration = generator != null ? generator.get() : null;
            if (authenticationConfiguration != null)
            {
                AuthenticationFlowResolver.ResolutionResult result = AuthenticationFlowResolver.run(this.credentialBuildersIndex, this.connectionBuildersIndex, identity, authenticationMechanism, authenticationConfiguration, storeInstance.getConnectionSpecification());
                if (result != null)
                {
                    authenticator = new Authenticator(storeInstance, authenticationMechanism, authenticationConfiguration, result.sourceCredentialType, result.targetCredentialType, result.flow, connectionBuildersIndex.get(new ConnectionBuilder.Key(storeInstance.getConnectionSpecification().getClass(), result.targetCredentialType)), this.environment);
                    break;
                }
            }
        }
        if (authenticator == null)
        {
            throw new RuntimeException(String.format("No authentication flow for store '%s' can be resolved for the specified identity. Try specifying an authentication mechanism or authentication configuration. Supported configuration type(s):\n%s",
                    storeInstance.getIdentifier(),
                    ListIterate.collect(storeInstance.getAuthenticationConfigurationTypes(), configType -> "- " + configType.getSimpleName() + " (" + storeInstance.getAuthenticationMechanism(configType).getLabel() + ")").makeString("\n")
            ));
        }
        return authenticator;
    }

    private static class AuthenticationFlowResolver
    {
        private final Map<String, CredentialBuilder> credentialBuildersIndex = new HashMap<>();
        private final Set<FlowNode> nodes = new HashSet<>();
        private final Map<String, LinkedHashSet<FlowNode>> edges = new HashMap<>();
        private final FlowNode startNode;
        private final FlowNode endNode;

        /**
         * This constructor sets up the authentication flow (directed non-cyclic) graph to help with flow resolution
         * <p>
         * The start node is the identity and the end node is the connection
         * The immediately adjacent nodes to the start node are credential nodes
         * The remaining nodes are credential-type nodes
         * <p>
         * The edges coming out from the start node correspond to credentials that the identity comes with
         * The edges going to end node correspond to available connection builders
         * The remaining edges correspond to available credential builders
         * <p>
         * NOTE:
         * - Since some credential builders do not require a specific input credential type, we added a generic `Credential` node
         * to Identity (start node)
         * - We want to differentiate credential and credential-type nodes because we want to account for (short-circuit) cases where
         * no resolution is needed: some credentials that belong to the identity is enough to build the connection (e.g. Kerberos).
         * We want to be very explicit about this case, we don't want this behavior to be generic for all types of credentials; for example,
         * just because an identity comes with a username-password credential, does not mean this credential is appropriate to be used to
         * connect to a database which supports username-password authentication mechanism, unless this intention is explicitly stated.
         * <p>
         * With this setup, we can use a basic graph search algorithm (e.g. BFS) to resolve the shortest path to build a connection
         */
        private AuthenticationFlowResolver(Map<CredentialBuilder.Key, CredentialBuilder> credentialBuildersIndex, Map<ConnectionBuilder.Key, ConnectionBuilder> connectionBuildersIndex, Identity identity, AuthenticationConfiguration authenticationConfiguration, AuthenticationMechanism authenticationMechanism, ConnectionSpecification connectionSpecification)
        {
            // add start node (i.e. identity node)
            this.startNode = new FlowNode(identity);
            // add identity's credential nodes
            identity.getCredentials().forEach(cred -> this.processEdge(this.startNode, new FlowNode(identity, cred.getClass())));
            // add special `Credential` node for catch-all credential builders
            this.processEdge(this.startNode, new FlowNode(identity, Credential.class));
            // process credential builders
            credentialBuildersIndex.values().stream()
                    .filter(builder -> builder.getAuthenticationConfigurationType().equals(authenticationConfiguration.getClass()))
                    .forEach(builder ->
                    {
                        if (!(builder.getInputCredentialType().equals(builder.getOutputCredentialType())))
                        {
                            this.processEdge(new FlowNode(builder.getInputCredentialType()), new FlowNode(builder.getOutputCredentialType()));
                        }
                        this.processEdge(new FlowNode(identity, builder.getInputCredentialType()), new FlowNode(builder.getOutputCredentialType()));
                        this.credentialBuildersIndex.put(createCredentialBuilderKey(builder.getInputCredentialType().getSimpleName(), builder.getOutputCredentialType().getSimpleName()), builder);
                    });
            // add end node (i.e. connection node)
            this.endNode = new FlowNode(connectionSpecification);
            // process connection builders
            connectionBuildersIndex.values().stream()
                    .filter(builder -> builder.getConnectionSpecificationType().equals(connectionSpecification.getClass()))
                    .forEach(builder -> this.processEdge(new FlowNode(builder.getCredentialType()), this.endNode));
        }

        static String createCredentialBuilderKey(String inputCredentialType, String outputCredentialType)
        {
            return inputCredentialType + "__" + outputCredentialType;
        }

        private void processEdge(FlowNode node, FlowNode adjacentNode)
        {
            this.nodes.add(node);
            this.nodes.add(adjacentNode);
            if (this.edges.get(node.id) != null)
            {
                Set<FlowNode> adjacentNodes = this.edges.get(node.id);
                adjacentNodes.add(adjacentNode);
            }
            else
            {
                LinkedHashSet<FlowNode> adjacentNodes = new LinkedHashSet<>();
                adjacentNodes.add(adjacentNode);
                this.edges.put(node.id, adjacentNodes);
            }
        }

        /**
         * Resolves the authentication flow in order to build a connection for a specified identity
         */
        public static ResolutionResult run(Map<CredentialBuilder.Key, CredentialBuilder> credentialBuildersIndex, Map<ConnectionBuilder.Key, ConnectionBuilder> connectionBuildersIndex, Identity identity, AuthenticationMechanism authenticationMechanism, AuthenticationConfiguration authenticationConfiguration, ConnectionSpecification connectionSpecification)
        {
            // using BFS algo to search for the shortest (non-cyclic) path
            AuthenticationFlowResolver state = new AuthenticationFlowResolver(credentialBuildersIndex, connectionBuildersIndex, identity, authenticationConfiguration, authenticationMechanism, connectionSpecification);
            boolean found = false;

            Set<FlowNode> visitedNodes = new HashSet<>(); // Create a set to keep track of visited vertices
            Deque<FlowNode> queue = new ArrayDeque<>();
            queue.add(state.startNode);

            Map<String, Integer> distances = new HashMap<>();
            Map<String, FlowNode> previousNodes = new HashMap<>();
            state.nodes.forEach(node -> distances.put(node.id, Integer.MAX_VALUE));
            distances.put(state.startNode.id, 0);

            while (!queue.isEmpty())
            {
                if (found)
                {
                    break;
                }

                FlowNode node = queue.removeFirst();
                visitedNodes.add(node);

                if (state.edges.get(node.id) != null)
                {
                    for (FlowNode adjNode : state.edges.get(node.id))
                    {
                        if (!visitedNodes.contains(adjNode))
                        {
                            distances.put(adjNode.id, distances.get(node.id) + 1);
                            previousNodes.put(adjNode.id, node);
                            queue.addLast(adjNode);

                            if (adjNode.equals(state.endNode))
                            {
                                found = true;
                                break;
                            }
                        }
                    }
                }
            }

            if (!found)
            {
                return null;
            }

            // resolve the path
            LinkedList<FlowNode> nodes = new LinkedList<>();
            FlowNode currentNode = previousNodes.get(connectionSpecification.getClass().getSimpleName());
            while (!state.startNode.equals(currentNode))
            {
                nodes.addFirst(currentNode);
                currentNode = previousNodes.get(currentNode.id);
            }

            if (nodes.size() < 2)
            {
                throw new IllegalStateException("Can't resolve connection authentication flow for specified identity: invalid flow state found!");
            }
            List<CredentialBuilder> flow = new ArrayList<>();
            for (int i = 0; i < nodes.size() - 1; i++)
            {
                flow.add(Objects.requireNonNull(
                        state.credentialBuildersIndex.get(createCredentialBuilderKey(nodes.get(i).credentialType.getSimpleName(), nodes.get(i + 1).credentialType.getSimpleName())),
                        String.format("Can't find a matching credential builder (input: %s, output: %s)", nodes.get(i).credentialType.getSimpleName(), nodes.get(i + 1).credentialType.getSimpleName()
                        )));
            }

            return new ResolutionResult(flow, nodes.get(0).credentialType, nodes.get(nodes.size() - 1).credentialType);
        }

        private static class FlowNode
        {
            private static final String IDENTITY_NODE_ID = "__identity__";
            private static final String IDENTITY_CREDENTIAL_NODE_ID_PREFIX = "identity__";
            public final String id;
            public final Class<? extends Credential> credentialType;

            public FlowNode(Identity identity)
            {
                this.id = IDENTITY_NODE_ID;
                this.credentialType = null;
            }

            public FlowNode(Identity identity, Class<? extends Credential> credentialType)
            {
                this.id = IDENTITY_CREDENTIAL_NODE_ID_PREFIX + credentialType.getSimpleName();
                this.credentialType = credentialType;
            }

            public FlowNode(Class<? extends Credential> credentialType)
            {
                this.id = credentialType.getSimpleName();
                this.credentialType = credentialType;
            }

            public FlowNode(ConnectionSpecification connectionSpecification)
            {
                this.id = connectionSpecification.getClass().getSimpleName();
                this.credentialType = null;
            }

            @Override
            public boolean equals(Object o)
            {
                if (this == o)
                {
                    return true;
                }
                if (o == null || getClass() != o.getClass())
                {
                    return false;
                }
                FlowNode that = (FlowNode) o;
                return this.id.equals(that.id);
            }

            @Override
            public int hashCode()
            {
                return Objects.hash(this.id);
            }
        }

        private static class ResolutionResult
        {
            public final List<CredentialBuilder> flow;
            public final Class<? extends Credential> sourceCredentialType;
            public final Class<? extends Credential> targetCredentialType;

            public ResolutionResult(List<CredentialBuilder> flow, Class<? extends Credential> sourceCredentialType, Class<? extends Credential> targetCredentialType)
            {
                this.flow = flow;
                this.sourceCredentialType = sourceCredentialType;
                this.targetCredentialType = targetCredentialType;
            }
        }
    }

    public <T> T getConnection(Identity identity, StoreInstance storeInstance, AuthenticationConfiguration authenticationConfiguration) throws Exception
    {
        return this.getConnection(identity, this.getAuthenticator(identity, storeInstance, authenticationConfiguration));
    }

    public <T> T getConnection(Identity identity, String storeInstanceIdentifier, AuthenticationConfiguration authenticationConfiguration) throws Exception
    {
        return this.getConnection(identity, this.getAuthenticator(identity, storeInstanceIdentifier, authenticationConfiguration));
    }

    public <T> T getConnection(Identity identity, StoreInstance storeInstance) throws Exception
    {
        return this.getConnection(identity, this.getAuthenticator(identity, storeInstance));
    }

    public <T> T getConnection(Identity identity, String storeInstanceIdentifier) throws Exception
    {
        return this.getConnection(identity, this.getAuthenticator(identity, storeInstanceIdentifier));
    }

    public <T> T getConnection(Identity identity, Authenticator authenticator) throws Exception
    {
        ConnectionBuilder<T, Credential, ConnectionSpecification> flow = (ConnectionBuilder<T, Credential, ConnectionSpecification>) authenticator.getConnectionBuilder();
        return flow.getConnection(authenticator.getStoreInstance().getConnectionSpecification(), flow.getAuthenticatorCompatible(authenticator), identity);
    }

    public static class Builder
    {
        private final LegendEnvironment environment;
        private final StoreInstanceProvider storeInstanceProvider;
        private final List<CredentialBuilder> credentialBuilders = Lists.mutable.empty();
        private final List<ConnectionBuilder> connectionBuilders = Lists.mutable.empty();

        public Builder(LegendEnvironment environment, StoreInstanceProvider storeInstanceProvider)
        {
            this.environment = environment;
            this.storeInstanceProvider = storeInstanceProvider;
        }

        public Builder withCredentialBuilders(List<CredentialBuilder> credentialBuilders)
        {
            this.credentialBuilders.addAll(credentialBuilders);
            return this;
        }

        public Builder withCredentialBuilders(CredentialBuilder... credentialBuilders)
        {
            this.credentialBuilders.addAll(Lists.mutable.with(credentialBuilders));
            return this;
        }

        public Builder withCredentialBuilder(CredentialBuilder credentialBuilder)
        {
            this.credentialBuilders.add(credentialBuilder);
            return this;
        }

        public Builder withConnectionBuilders(List<ConnectionBuilder> connectionBuilders)
        {
            this.connectionBuilders.addAll(connectionBuilders);
            return this;
        }

        public Builder withConnectionBuilders(ConnectionBuilder... connectionBuilders)
        {
            this.connectionBuilders.addAll(Lists.mutable.with(connectionBuilders));
            return this;
        }

        public Builder withConnectionBuilder(ConnectionBuilder connectionBuilder)
        {
            this.connectionBuilders.add(connectionBuilder);
            return this;
        }

        public ConnectionFactory build()
        {
            for (ConnectionBuilder connectionBuilder : connectionBuilders)
            {
                ConnectionManager connectionManager = connectionBuilder.getConnectionManager();
                if (connectionManager != null)
                {
                    connectionManager.initialize(environment);
                }
            }

            return new ConnectionFactory(
                    this.environment,
                    this.storeInstanceProvider,
                    this.credentialBuilders,
                    this.connectionBuilders
            );
        }
    }
}
