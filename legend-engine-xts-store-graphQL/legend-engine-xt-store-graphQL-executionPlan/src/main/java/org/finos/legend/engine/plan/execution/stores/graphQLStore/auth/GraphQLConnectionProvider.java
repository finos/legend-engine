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

package org.finos.legend.engine.plan.execution.stores.graphQLStore.auth;

import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.authentication.credentialprovider.CredentialProviderProvider;
import org.finos.legend.connection.legacy.ConnectionProvider;
import org.finos.legend.connection.legacy.ConnectionSpecification;
import org.finos.legend.engine.plan.execution.stores.graphQLStore.GraphQLHttpConnectionBuilder;
import org.finos.legend.engine.plan.execution.stores.graphQLStore.IGraphQLStoreExecutionExtension;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.AuthenticationSpecification;
import org.finos.legend.engine.shared.core.identity.Credential;
import org.finos.legend.engine.shared.core.identity.Identity;

import java.util.List;

public class GraphQLConnectionProvider extends ConnectionProvider<GraphQLHttpConnectionBuilder>
{
    public GraphQLConnectionProvider(CredentialProviderProvider credentialProviderProvider)
    {
        super(credentialProviderProvider);
    }

    @Override
    public GraphQLHttpConnectionBuilder makeConnection(ConnectionSpecification connectionSpecification, AuthenticationSpecification authenticationSpecification, Identity identity) throws Exception
    {
        if (!(connectionSpecification instanceof GraphQLStoreConnectionSpecification))
        {
            throw new IllegalStateException("Invalid ConnectionSpecification. Please reach out to dev team");
        }
        GraphQLStoreConnectionSpecification graphQLConnectionSpecification = (GraphQLStoreConnectionSpecification) connectionSpecification;

        RequestBuilder requestBuilder = RequestBuilder.post(graphQLConnectionSpecification.uri).setEntity(graphQLConnectionSpecification.body);
        GraphQLHttpConnectionBuilder connectionBuilder = new GraphQLHttpConnectionBuilder(HttpClients.custom(), requestBuilder);

        if (authenticationSpecification != null)
        {
            Credential credential = makeCredential(authenticationSpecification, identity);
            configureAuthentication(authenticationSpecification, credential, connectionBuilder);
        }

        return connectionBuilder;
    }

    private static void configureAuthentication(AuthenticationSpecification authenticationSpecification, Credential credential, GraphQLHttpConnectionBuilder connectionBuilder)
    {
        List<IGraphQLStoreExecutionExtension> extensions = IGraphQLStoreExecutionExtension.getExtensions();
        List<Function3<AuthenticationSpecification, Credential, GraphQLHttpConnectionBuilder, Boolean>> processors = ListIterate.flatCollect(extensions, IGraphQLStoreExecutionExtension::getExtraAuthenticationProcessors);

        for (Function3<AuthenticationSpecification, Credential, GraphQLHttpConnectionBuilder, Boolean> processor : processors)
        {
            if (processor.value(authenticationSpecification, credential, connectionBuilder) != null)
            {
                return;
            }
        }
        throw new RuntimeException("AuthenticationSpecification not supported");
    }
}
