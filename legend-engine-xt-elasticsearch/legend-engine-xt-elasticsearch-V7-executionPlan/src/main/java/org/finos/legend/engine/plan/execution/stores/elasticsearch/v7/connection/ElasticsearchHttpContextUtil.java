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
//

package org.finos.legend.engine.plan.execution.stores.elasticsearch.v7.connection;

import java.util.List;
import java.util.Optional;
import org.apache.http.client.protocol.HttpClientContext;
import org.finos.legend.authentication.credentialprovider.CredentialBuilder;
import org.finos.legend.authentication.credentialprovider.CredentialProviderProvider;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.AuthenticationSpecification;
import org.finos.legend.engine.shared.core.identity.Credential;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionCategory;

public final class ElasticsearchHttpContextUtil
{
    private ElasticsearchHttpContextUtil()
    {

    }

    public static HttpClientContext authToHttpContext(Identity identity, CredentialProviderProvider credentialProviderProvider, AuthenticationSpecification authenticationSpecification, List<? extends ElasticsearchHttpContextProvider> providers)
    {
        Credential credential = CredentialBuilder.makeCredential(credentialProviderProvider, authenticationSpecification, identity);

        Optional<HttpClientContext> clientContext = providers.stream()
                .map(x -> x.provide(credential))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findAny();

        return clientContext.orElseThrow(() -> new EngineException(String.format("Credential %s not supported to connect to ElasticSearch", credential.getClass().getCanonicalName()), ExceptionCategory.USER_CREDENTIALS_ERROR));
    }
}
