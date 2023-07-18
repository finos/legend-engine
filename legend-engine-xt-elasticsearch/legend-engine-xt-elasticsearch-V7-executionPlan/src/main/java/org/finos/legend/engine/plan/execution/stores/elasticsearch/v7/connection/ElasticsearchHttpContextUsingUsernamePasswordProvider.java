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

import java.util.Optional;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.finos.legend.engine.shared.core.identity.Credential;
import org.finos.legend.engine.shared.core.identity.credential.PlaintextUserPasswordCredential;

public class ElasticsearchHttpContextUsingUsernamePasswordProvider implements ElasticsearchHttpContextProvider
{
    @Override
    public Optional<HttpClientContext> provide(Credential credential)
    {
        return Optional.of(credential)
                .filter(PlaintextUserPasswordCredential.class::isInstance)
                .map(PlaintextUserPasswordCredential.class::cast)
                .map(this::createCredentialContext);
    }

    private HttpClientContext createCredentialContext(PlaintextUserPasswordCredential credential)
    {
        UsernamePasswordCredentials httpCredentials = new UsernamePasswordCredentials(credential.getUser(), credential.getPassword());

        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, httpCredentials);

        HttpClientContext httpClientContext = new HttpClientContext();
        httpClientContext.setCredentialsProvider(credentialsProvider);

        return httpClientContext;
    }
}
