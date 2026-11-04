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

package org.finos.legend.engine.plan.generation.stores.graphQLStore.introspection;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.eclipse.collections.api.factory.Maps;
import org.finos.legend.authentication.credentialprovider.CredentialProviderProvider;
import org.finos.legend.engine.plan.execution.stores.graphQLStore.GraphQLHttpConnectionBuilder;
import org.finos.legend.engine.plan.execution.stores.graphQLStore.auth.GraphQLConnectionProvider;
import org.finos.legend.engine.plan.execution.stores.graphQLStore.auth.GraphQLStoreConnectionSpecification;
import org.finos.legend.engine.protocol.graphQLStore.introspection.GraphQLIntrospectionSchema;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.AuthenticationSpecification;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.net.URI;
import java.util.List;
import java.util.Map;

public class GraphQLIntrospectionClient
{
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public GraphQLIntrospectionSchema introspect(String url, AuthenticationSpecification authenticationSpecification, CredentialProviderProvider credentialProviderProvider, Identity identity) throws Exception
    {
        Map<String, Object> body = Maps.mutable.with("query", GraphQLIntrospectionQuery.QUERY);
        StringEntity bodyEntity = new StringEntity(OBJECT_MAPPER.writeValueAsString(body), ContentType.APPLICATION_JSON);
        GraphQLStoreConnectionSpecification connectionSpecification = new GraphQLStoreConnectionSpecification(URI.create(url), bodyEntity);

        GraphQLConnectionProvider connectionProvider = new GraphQLConnectionProvider(credentialProviderProvider);
        GraphQLHttpConnectionBuilder connectionBuilder = connectionProvider.makeConnection(connectionSpecification, authenticationSpecification, identity);

        return connectionBuilder.execute(httpResponse ->
        {
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            String responseBody = httpResponse.getEntity() == null ? "" : EntityUtils.toString(httpResponse.getEntity());
            if (statusCode != 200)
            {
                throw new EngineException("Introspection request to [" + url + "] failed with status " + statusCode + " - " + responseBody);
            }

            GraphQLIntrospectionResponse response = OBJECT_MAPPER.readValue(responseBody, GraphQLIntrospectionResponse.class);
            List<GraphQLIntrospectionResponse.GraphQLIntrospectionError> errors = response.errors;
            if (errors != null && !errors.isEmpty())
            {
                StringBuilder messages = new StringBuilder();
                errors.forEach(e -> messages.append(e.message).append("; "));
                throw new EngineException("Introspection request to [" + url + "] returned errors: " + messages);
            }
            if (response.data == null || response.data.__schema == null)
            {
                throw new EngineException("Introspection request to [" + url + "] returned no __schema data");
            }
            return response.data.__schema;
        });
    }
}
