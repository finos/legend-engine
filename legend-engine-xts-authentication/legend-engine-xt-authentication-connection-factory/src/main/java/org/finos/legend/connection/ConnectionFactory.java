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
import org.finos.legend.connection.protocol.AuthenticationConfiguration;
import org.finos.legend.connection.protocol.ConnectionSpecification;
import org.finos.legend.engine.shared.core.identity.Credential;
import org.finos.legend.engine.shared.core.identity.Identity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

public class ConnectionFactory
{
    private final EnvironmentConfiguration environmentConfiguration;
    private final Map<CredentialBuilder.Key, CredentialBuilder> credentialBuildersIndex = new HashMap<>();
    private final Map<ConnectionBuilder.Key, ConnectionBuilder> connectionBuildersIndex = new HashMap<>();
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
        if (!storeInstance.getAuthenticationConfigurationTypes().contains(authenticationConfiguration.getClass()))
        {
            throw new RuntimeException(String.format("Can't get authenticator: authentication configuration of type '%s' is not supported by store '%s'", authenticationConfiguration.getClass().getSimpleName(), storeInstance.getIdentifier()));
        }

        AuthenticationFlowResolver.ResolutionResult result = AuthenticationFlowResolver.run(this.credentialBuildersIndex, this.connectionBuildersIndex, identity, authenticationConfiguration, storeInstance.getConnectionSpecification());
        return new Authenticator(identity, storeInstance, authenticationConfiguration, result.flow, connectionBuildersIndex.get(new ConnectionBuilder.Key(storeInstance.getConnectionSpecification().getClass(), result.sourceCredentialType)));
    }

    public Authenticator getAuthenticator(Identity identity, String storeInstanceIdentifier)
    {
        return this.getAuthenticator(identity, this.findStoreInstance(storeInstanceIdentifier));
    }

    public Authenticator getAuthenticator(Identity identity, StoreInstance storeInstance)
    {
        throw new RuntimeException("Not implemented");
        // TODO: @akphi - implement this and the generic authentication config form
//        return this.getAuthenticator(identity, this.findStoreInstance(storeInstanceIdentifier), authenticationConfiguration);
    }

    private static class AuthenticationFlowResolver
    {
        private final Map<String, CredentialBuilder> credentialBuildersIndex = new HashMap<>();
        private final Set<FlowNode> nodes = new HashSet<>();
        private final Map<String, LinkedHashSet<FlowNode>> edges = new HashMap<>();
        private final FlowNode startNode;
        private final FlowNode endNode;

        private AuthenticationFlowResolver(Map<CredentialBuilder.Key, CredentialBuilder> credentialBuildersIndex, Map<ConnectionBuilder.Key, ConnectionBuilder> connectionBuildersIndex, Identity identity, AuthenticationConfiguration authenticationConfiguration, ConnectionSpecification connectionSpecification)
        {
            this.startNode = new FlowNode(identity);
            identity.getCredentials().forEach(cred -> this.processEdge(this.startNode, new FlowNode(cred.getClass())));
            this.processEdge(this.startNode, new FlowNode(Credential.class)); // add a node for catch-all credential builders
            credentialBuildersIndex.values().stream()
                    .filter(builder -> builder.getAuthenticationConfigurationType().equals(authenticationConfiguration.getClass()))
                    .forEach(builder ->
                    {
                        this.processEdge(new FlowNode(builder.getInputCredentialType()), new FlowNode(builder.getOutputCredentialType()));
                        this.credentialBuildersIndex.put(createCredentialBuilderKey(builder.getInputCredentialType().getSimpleName(), builder.getOutputCredentialType().getSimpleName()), builder);
                    });
            this.endNode = new FlowNode(connectionSpecification);
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

        public static ResolutionResult run(Map<CredentialBuilder.Key, CredentialBuilder> credentialBuildersIndex, Map<ConnectionBuilder.Key, ConnectionBuilder> connectionBuildersIndex, Identity identity, AuthenticationConfiguration authenticationConfiguration, ConnectionSpecification connectionSpecification)
        {
            // try to short-circuit this first
            Set<Class<? extends Credential>> compatibleCredentials = connectionBuildersIndex.values().stream()
                    .filter(builder -> builder.getConnectionSpecificationType().equals(connectionSpecification.getClass()))
                    .map(builder -> (Class<? extends Credential>) builder.getCredentialType()).collect(Collectors.toSet());
            Optional<? extends Class<? extends Credential>> shortCircuitCredentialTypes = identity.getCredentials().stream().map(Credential::getClass).filter(compatibleCredentials::contains).findFirst();
            if (shortCircuitCredentialTypes.isPresent())
            {
                return new ResolutionResult(Lists.mutable.empty(), shortCircuitCredentialTypes.get());
            }

            // if not possible, attempt to resolve using BFS algo
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
                throw new RuntimeException(String.format("Can't resolve connection authentication flow for specified identity (AuthenticationConfiguration=%s, ConnectionSpecification=%s)", authenticationConfiguration.getClass().getSimpleName(), connectionSpecification.getClass().getSimpleName()));
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
                throw new RuntimeException("Can't resolve connection authentication flow for specified identity: invalid non short-circuit flow found!");
            }
            List<CredentialBuilder> flow = new ArrayList<>();
            for (int i = 0; i < nodes.size() - 1; i++)
            {
                flow.add(Objects.requireNonNull(
                        state.credentialBuildersIndex.get(createCredentialBuilderKey(nodes.get(i).credentialType.getSimpleName(), nodes.get(i + 1).credentialType.getSimpleName())),
                        String.format("Can't find credential builder: Input=%s, output=%s", nodes.get(i).credentialType.getSimpleName(), nodes.get(i + 1).credentialType.getSimpleName()
                        )));
            }

            return new ResolutionResult(flow, nodes.get(0).credentialType);
        }

        private static class FlowNode
        {
            private static final String IDENTITY_NODE_NAME = "__identity__";
            public final String id;
            public final Class<? extends Credential> credentialType;

            public FlowNode(Class<? extends Credential> credentialType)
            {
                this.id = credentialType.getSimpleName();
                this.credentialType = credentialType;
            }

            public FlowNode(Identity identity)
            {
                this.id = IDENTITY_NODE_NAME;
                this.credentialType = null;
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

            public ResolutionResult(List<CredentialBuilder> flow, Class<? extends Credential> sourceCredentialType)
            {
                this.flow = flow;
                this.sourceCredentialType = sourceCredentialType;
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
        Optional<ConnectionBuilder<T, Credential, ConnectionSpecification>> compatibleConnectionBuilder = Optional.ofNullable((ConnectionBuilder<T, Credential, ConnectionSpecification>) this.connectionBuildersIndex.get(new ConnectionBuilder.Key(authenticator.getStoreInstance().getConnectionSpecification().getClass(), credential.getClass())));
        ConnectionBuilder<T, Credential, ConnectionSpecification> flow = compatibleConnectionBuilder.orElseThrow(() -> new RuntimeException(String.format("Can't find any compatible connection builders (Store=%s, Credential=%s)",
                credential.getClass().getSimpleName(),
                authenticator.getStoreInstance().getIdentifier()
        )));
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
