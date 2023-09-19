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
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

public class ConnectionFactory
{
    private final EnvironmentConfiguration environmentConfiguration;
    private final Map<CredentialBuilder.Key, CredentialBuilder> credentialBuildersIndex = new LinkedHashMap<>();
    private final Map<ConnectionBuilder.Key, ConnectionBuilder> connectionBuildersIndex = new LinkedHashMap<>();
    private final Map<String, StoreInstance> storeInstancesIndex;

    private ConnectionFactory(EnvironmentConfiguration environmentConfiguration, List<CredentialBuilder> credentialBuilders, List<ConnectionBuilder> connectionBuilders, Map<String, StoreInstance> storeInstancesIndex)
    {
        this.environmentConfiguration = environmentConfiguration;
        for (ConnectionBuilder<?, ?, ?> builder : connectionBuilders)
        {
            this.connectionBuildersIndex.put(new ConnectionBuilder.Key(builder.getConnectionSpecificationType(), builder.getCredentialType()), builder);
        }
        for (CredentialBuilder<?, ?, ?> builder : credentialBuilders)
        {
            this.credentialBuildersIndex.put(new CredentialBuilder.Key(builder.getAuthenticationConfigurationType(), builder.getInputCredentialType(), builder.getOutputCredentialType()), builder);
        }
        this.storeInstancesIndex = storeInstancesIndex;
    }

    public void registerStoreInstance(StoreInstance storeInstance)
    {
        if (this.storeInstancesIndex.containsKey(storeInstance.getIdentifier()))
        {
            throw new RuntimeException(String.format("Can't register store instance: found multiple store instances with identifier '%s'", storeInstance.getIdentifier()));
        }
        this.storeInstancesIndex.put(storeInstance.getIdentifier(), storeInstance);
    }

    private StoreInstance findStoreInstance(String identifier)
    {
        return Objects.requireNonNull(this.storeInstancesIndex.get(identifier), String.format("Can't find store instance with identifier '%s'", identifier));
    }

    public Authenticator getAuthenticator(Identity identity, String storeInstanceIdentifier, AuthenticationConfiguration authenticationConfiguration)
    {
        return this.getAuthenticator(identity, this.findStoreInstance(storeInstanceIdentifier), authenticationConfiguration);
    }

    public Authenticator getAuthenticator(Identity identity, StoreInstance storeInstance, AuthenticationConfiguration authenticationConfiguration)
    {
        AuthenticationMechanism authenticationMechanism = environmentConfiguration.findAuthenticationMechanismForConfiguration(authenticationConfiguration);
        String authenticationMechanismLabel = authenticationMechanism != null ? ("authentication mechanism '" + authenticationMechanism.getLabel() + "'") : ("authentication mechanism with configuration '" + authenticationConfiguration.getClass().getSimpleName() + "'");
        if (!storeInstance.getAuthenticationConfigurationTypes().contains(authenticationConfiguration.getClass()))
        {
            throw new RuntimeException(String.format("Can't get authenticator: %s is not supported by store '%s'. Supported mechanism(s):\n%s",
                    authenticationMechanismLabel,
                    storeInstance.getIdentifier(),
                    storeInstance.getAuthenticationMechanisms().stream().map(mechanism -> "- " + mechanism.getLabel() + " (config: " + mechanism.getAuthenticationConfigurationType().getSimpleName() + ")").collect(Collectors.joining("\n")))
            );
        }
        AuthenticationFlowResolver.ResolutionResult result = AuthenticationFlowResolver.run(this.credentialBuildersIndex, this.connectionBuildersIndex, identity, authenticationConfiguration, storeInstance.getConnectionSpecification());
        if (result == null)
        {
            throw new RuntimeException(String.format("Can't get authenticator: no authentication flow for store '%s' can be resolved for the specified identity using %s (authentication configuration: %s, connection specification: %s)",
                    storeInstance.getIdentifier(),
                    authenticationMechanismLabel,
                    authenticationConfiguration.getClass().getSimpleName(),
                    storeInstance.getConnectionSpecification().getClass().getSimpleName())
            );
        }
        return new Authenticator(identity, storeInstance, authenticationConfiguration, result.sourceCredentialType, result.flow, connectionBuildersIndex.get(new ConnectionBuilder.Key(storeInstance.getConnectionSpecification().getClass(), result.targetCredentialType)));
    }

    public Authenticator getAuthenticator(Identity identity, String storeInstanceIdentifier)
    {
        return this.getAuthenticator(identity, this.findStoreInstance(storeInstanceIdentifier));
    }

    public Authenticator getAuthenticator(Identity identity, StoreInstance storeInstance)
    {
        List<AuthenticationConfiguration> authenticationConfigurations = ListIterate.collect(storeInstance.getAuthenticationMechanisms(), AuthenticationMechanism::generateConfiguration).select(Objects::nonNull);
        Authenticator authenticator = null;
        for (AuthenticationConfiguration authenticationConfiguration : authenticationConfigurations)
        {
            AuthenticationFlowResolver.ResolutionResult result = AuthenticationFlowResolver.run(this.credentialBuildersIndex, this.connectionBuildersIndex, identity, authenticationConfiguration, storeInstance.getConnectionSpecification());
            if (result != null)
            {
                authenticator = new Authenticator(identity, storeInstance, authenticationConfiguration, result.sourceCredentialType, result.flow, connectionBuildersIndex.get(new ConnectionBuilder.Key(storeInstance.getConnectionSpecification().getClass(), result.targetCredentialType)));
                break;
            }
        }
        if (authenticator == null)
        {
            throw new RuntimeException(String.format("Can't get authenticator: no authentication flow for store '%s' can be resolved for the specified identity using auto-generated authentication configuration. Try specifying an authentication mechanism by providing a configuration of one of the following types:\n%s",
                    storeInstance.getIdentifier(),
                    ListIterate.select(storeInstance.getAuthenticationMechanisms(), mechanism -> mechanism.generateConfiguration() == null).collect(mechanism -> "- " + mechanism.getAuthenticationConfigurationType().getSimpleName() + " (mechanism: " + mechanism.getLabel() + ")").makeString("\n")
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
         * - We want to differentiate credential and credential-type nodes because we want to account for (short-circuit) case where
         * no resolution is needed: some credentials that belong to the identity is enough to build the connection (e.g. Kerberos).
         * We want to be very explicit about this case, we don't want this behavior to be generic for all types of credentials; for example,
         * just because an identity comes with a username-password credential, does not mean this credential is appropriate to be used to
         * connect to a database which supports username-password authentication mechanism, unless this intention is explicitly stated.
         * <p>
         * With this setup, we can use a basic graph search algorithm (e.g. BFS) to resolve the shortest path to build a connection
         */
        private AuthenticationFlowResolver(Map<CredentialBuilder.Key, CredentialBuilder> credentialBuildersIndex, Map<ConnectionBuilder.Key, ConnectionBuilder> connectionBuildersIndex, Identity identity, AuthenticationConfiguration authenticationConfiguration, ConnectionSpecification connectionSpecification)
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
                        if (!(builder instanceof CredentialExtractor))
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

        public static String createCredentialBuilderKey(String inputCredentialType, String outputCredentialType)
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
        public static ResolutionResult run(Map<CredentialBuilder.Key, CredentialBuilder> credentialBuildersIndex, Map<ConnectionBuilder.Key, ConnectionBuilder> connectionBuildersIndex, Identity identity, AuthenticationConfiguration authenticationConfiguration, ConnectionSpecification connectionSpecification)
        {
            // using BFS algo to search for the shortest (non-cyclic) path
            AuthenticationFlowResolver state = new AuthenticationFlowResolver(credentialBuildersIndex, connectionBuildersIndex, identity, authenticationConfiguration, connectionSpecification);
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
        return this.getConnection(this.getAuthenticator(identity, storeInstance, authenticationConfiguration));
    }

    public <T> T getConnection(Identity identity, String storeInstanceIdentifier, AuthenticationConfiguration authenticationConfiguration) throws Exception
    {
        return this.getConnection(this.getAuthenticator(identity, storeInstanceIdentifier, authenticationConfiguration));
    }

    public <T> T getConnection(Identity identity, StoreInstance storeInstance) throws Exception
    {
        return this.getConnection(this.getAuthenticator(identity, storeInstance));
    }

    public <T> T getConnection(Identity identity, String storeInstanceIdentifier) throws Exception
    {
        return this.getConnection(this.getAuthenticator(identity, storeInstanceIdentifier));
    }

    public <T> T getConnection(Authenticator authenticator) throws Exception
    {
        Credential credential = authenticator.makeCredential(this.environmentConfiguration);
        ConnectionBuilder<T, Credential, ConnectionSpecification> flow = (ConnectionBuilder<T, Credential, ConnectionSpecification>) authenticator.getConnectionBuilder();
        return flow.getConnection(credential, authenticator.getStoreInstance().getConnectionSpecification(), authenticator.getStoreInstance());
    }

    public static class Builder
    {
        private final EnvironmentConfiguration environmentConfiguration;
        private CredentialBuilderProvider credentialBuilderProvider;
        private ConnectionBuilderProvider connectionBuilderProvider;
        private final List<CredentialBuilder<?, ?, ?>> credentialBuilders = Lists.mutable.empty();
        private final List<ConnectionBuilder<?, ?, ?>> connectionBuilders = Lists.mutable.empty();
        private final Map<String, StoreInstance> storeInstancesIndex = new HashMap<>();

        public Builder(EnvironmentConfiguration environmentConfiguration)
        {
            this.environmentConfiguration = environmentConfiguration;
        }

        public Builder withCredentialBuilderProvider(CredentialBuilderProvider provider)
        {
            this.credentialBuilderProvider = provider;
            return this;
        }

        public Builder withConnectionBuilderProvider(ConnectionBuilderProvider provider)
        {
            this.connectionBuilderProvider = provider;
            return this;
        }

        public Builder withCredentialBuilders(List<CredentialBuilder<?, ?, ?>> credentialBuilders)
        {
            this.credentialBuilders.addAll(credentialBuilders);
            return this;
        }

        public Builder withCredentialBuilder(CredentialBuilder<?, ?, ?> credentialBuilder)
        {
            this.credentialBuilders.add(credentialBuilder);
            return this;
        }

        public Builder withConnectionBuilders(List<ConnectionBuilder<?, ?, ?>> connectionBuilders)
        {
            this.connectionBuilders.addAll(connectionBuilders);
            return this;
        }

        public Builder withConnectionBuilder(ConnectionBuilder<?, ?, ?> connectionBuilder)
        {
            this.connectionBuilders.add(connectionBuilder);
            return this;
        }

        public Builder withStoreInstances(List<StoreInstance> storeInstances)
        {
            storeInstances.forEach(this::registerStoreInstance);
            return this;
        }

        public Builder withStoreInstance(StoreInstance storeInstance)
        {
            this.registerStoreInstance(storeInstance);
            return this;
        }

        private void registerStoreInstance(StoreInstance storeInstance)
        {
            if (this.storeInstancesIndex.containsKey(storeInstance.getIdentifier()))
            {
                throw new RuntimeException(String.format("Can't register store instance: found multiple store instances with identifier '%s'", storeInstance.getIdentifier()));
            }
            this.storeInstancesIndex.put(storeInstance.getIdentifier(), storeInstance);
        }

        public ConnectionFactory build()
        {
            List<CredentialBuilder> credentialBuilders = this.credentialBuilderProvider != null ? this.credentialBuilderProvider.getBuilders() : Lists.mutable.empty();
            credentialBuilders.addAll(this.credentialBuilders);
            List<ConnectionBuilder> connectionBuilders = this.connectionBuilderProvider != null ? this.connectionBuilderProvider.getBuilders() : Lists.mutable.empty();
            connectionBuilders.addAll(this.connectionBuilders);

            for (ConnectionManager connectionManager : ServiceLoader.load(ConnectionManager.class))
            {
                connectionManager.initialize();
            }

            return new ConnectionFactory(
                    this.environmentConfiguration,
                    credentialBuilders,
                    connectionBuilders,
                    this.storeInstancesIndex
            );
        }
    }
}
