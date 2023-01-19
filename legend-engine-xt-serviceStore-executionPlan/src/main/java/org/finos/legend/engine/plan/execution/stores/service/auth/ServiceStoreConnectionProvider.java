// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.service.auth;

import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.authentication.credentialprovider.CredentialProviderProvider;
import org.finos.legend.engine.connection.ConnectionProvider;
import org.finos.legend.engine.connection.ConnectionSpecification;
import org.finos.legend.engine.plan.execution.stores.service.IServiceStoreExecutionExtension;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.AuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model.SecurityScheme;
import org.finos.legend.engine.shared.core.function.Function5;
import org.finos.legend.engine.shared.core.identity.Credential;
import org.finos.legend.engine.shared.core.identity.Identity;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ServiceStoreConnectionProvider extends ConnectionProvider<Pair<HttpClientBuilder, RequestBuilder>>
{
    private final CredentialProviderProvider credentialProviderProvider;

    public ServiceStoreConnectionProvider(CredentialProviderProvider credentialProviderProvider)
    {
        super(credentialProviderProvider);
        this.credentialProviderProvider = credentialProviderProvider;
    }

    public Pair<HttpClientBuilder, RequestBuilder> makeConnection(ConnectionSpecification connectionSpec, AuthenticationSpecification authenticationSpec, Identity identity) throws Exception
    {
        assert (connectionSpec instanceof ServiceStoreConnectionSpecification);
        ServiceStoreConnectionSpecification serviceStoreConnectionSpecification = (ServiceStoreConnectionSpecification) connectionSpec;

        assert (authenticationSpec instanceof ServiceStoreAuthenticationSpecification);
        ServiceStoreAuthenticationSpecification serviceStoreAuthenticationSpecification = (ServiceStoreAuthenticationSpecification) authenticationSpec;

        Map<String, SecurityScheme> securitySchemeMap = serviceStoreAuthenticationSpecification.securitySchemes;
        Map<String, AuthenticationSpecification> authenticationSpecMap = serviceStoreAuthenticationSpecification.authSpecs;

        for (Map.Entry<String, SecurityScheme> entry : securitySchemeMap.entrySet())
        {
            String securitySchemeId = entry.getKey();
            SecurityScheme securityScheme = entry.getValue();
            AuthenticationSpecification authenticationSpecification = authenticationSpecMap.get(securitySchemeId);

            HttpClientBuilder httpClientBuilder = HttpClients.custom();
            RequestBuilder builder = makeRequestUtil(serviceStoreConnectionSpecification, serviceStoreAuthenticationSpecification, identity);

            validateSecurityScheme(securityScheme, authenticationSpecification);
            Credential cred = processSecurityScheme(securitySchemeId, securityScheme, authenticationSpecification, identity);

            if (cred != null)
            {
                configureAuthentication(builder, httpClientBuilder, entry.getKey(), entry.getValue(), authenticationSpecMap, cred);
                return Tuples.pair(httpClientBuilder, builder);
            }
        }

        return Tuples.pair(HttpClients.custom(), makeRequestUtil(serviceStoreConnectionSpecification, serviceStoreAuthenticationSpecification, identity));
    }

    public static RequestBuilder makeRequestUtil(ServiceStoreConnectionSpecification serviceStoreConnectionSpecification, AuthenticationSpecification authenticationSpec, Identity identity) throws Exception
    {
        RequestBuilder builder = null;
        switch (serviceStoreConnectionSpecification.httpMethod)
        {
            case "GET":
                builder = RequestBuilder.get(serviceStoreConnectionSpecification.uri);
                break;
            case "POST":
                builder = RequestBuilder.post(serviceStoreConnectionSpecification.uri);
                break;
            default:
                throw new UnsupportedOperationException("The HTTP method " + serviceStoreConnectionSpecification.httpMethod + " is not supported");
        }
        serviceStoreConnectionSpecification.headers.forEach(builder::addHeader);

        return builder;
    }

    private Credential processSecurityScheme(String schemeId, SecurityScheme securityScheme, AuthenticationSpecification authenticationSpecification, Identity identity) throws Exception
    {
        return makeCredential(authenticationSpecification, identity);
    }

    private static void configureAuthentication(RequestBuilder builder, HttpClientBuilder httpClientBuilder, String securitySchemeId, SecurityScheme scheme, Map<String, AuthenticationSpecification> authenticationSpecMap, Credential credential) throws IOException
    {
        AuthenticationSpecification authenticationSpecification = authenticationSpecMap.get(securitySchemeId);
        List<Function5<SecurityScheme, AuthenticationSpecification, Credential, RequestBuilder, HttpClientBuilder, Boolean>> processors = ListIterate.flatCollect(IServiceStoreExecutionExtension.getExtensions(), ext -> ext.getExtraSecuritySchemeProcessors());

        ListIterate
                .collect(processors, processor -> processor.value(scheme, authenticationSpecification, credential, builder, httpClientBuilder))
                .select(Objects::nonNull)
                .getFirstOptional()
                .orElseThrow(() -> new RuntimeException(" Error using security scheme " + securitySchemeId));
    }

    private static void validateSecurityScheme(SecurityScheme securityScheme, AuthenticationSpecification authenticationSpecification)
    {
        List<Function2<SecurityScheme, AuthenticationSpecification, Boolean>> processors = ListIterate.flatCollect(IServiceStoreExecutionExtension.getExtensions(), ext -> ext.getExtraSecuritySchemeValidators());

        ListIterate
                .collect(processors, processor -> processor.value(securityScheme, authenticationSpecification))
                .select(Objects::nonNull)
                .getFirstOptional()
                .orElseThrow(() -> new RuntimeException("securityScheme-AuthenticationSpec combination is not supported. Only supported combinations are [Http, UserPassword], [ApiKey, ApiKey]"));

    }

}
