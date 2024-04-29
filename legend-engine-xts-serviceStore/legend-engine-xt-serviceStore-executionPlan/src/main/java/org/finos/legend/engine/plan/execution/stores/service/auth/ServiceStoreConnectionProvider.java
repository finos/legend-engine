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

package org.finos.legend.engine.plan.execution.stores.service.auth;

import io.opentracing.Span;
import io.opentracing.util.GlobalTracer;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.authentication.credentialprovider.CredentialProviderProvider;
import org.finos.legend.connection.legacy.ConnectionProvider;
import org.finos.legend.connection.legacy.ConnectionSpecification;
import org.finos.legend.engine.plan.execution.stores.service.IServiceStoreExecutionExtension;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.AuthenticationSchemeRequirement;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.SingleAuthenticationSchemeRequirement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.AuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model.SecurityScheme;
import org.finos.legend.engine.shared.core.function.Function5;
import org.finos.legend.engine.shared.core.identity.Credential;
import org.finos.legend.engine.shared.core.identity.Identity;

import java.util.List;
import java.util.Objects;

public class ServiceStoreConnectionProvider extends ConnectionProvider<HttpConnectionBuilder>
{
    public ServiceStoreConnectionProvider(CredentialProviderProvider credentialProviderProvider)
    {
        super(credentialProviderProvider);
    }

    public HttpConnectionBuilder makeConnection(ConnectionSpecification connectionSpecification, AuthenticationSpecification authenticationSpecification, Identity identity) throws Exception
    {
        if (!(connectionSpecification instanceof ServiceStoreConnectionSpecification && authenticationSpecification instanceof ServiceStoreAuthenticationSpecification))
        {
            throw new IllegalStateException("Invalid ConnectionSpecification/AuthenticationSpecification. Please reach out to dev team");
        }

        ServiceStoreConnectionSpecification serviceStoreConnectionSpecification = (ServiceStoreConnectionSpecification) connectionSpecification;
        ServiceStoreAuthenticationSpecification serviceStoreAuthenticationSpecification = (ServiceStoreAuthenticationSpecification) authenticationSpecification;

        for (AuthenticationSchemeRequirement requirement : serviceStoreAuthenticationSpecification.authenticationSchemeRequirements)
        {
            try
            {
                SingleAuthenticationSchemeRequirement authenticationSchemeRequirement = (SingleAuthenticationSchemeRequirement) requirement;
                SecurityScheme securityScheme = authenticationSchemeRequirement.securityScheme;
                AuthenticationSpecification authSpecification = authenticationSchemeRequirement.authenticationSpecification;

                HttpClientBuilder clientBuilder = HttpClients.custom();
                RequestBuilder requestBuilder = makeRequestUtil(serviceStoreConnectionSpecification);
                HttpConnectionBuilder httpConnectionBuilder = new HttpConnectionBuilder(clientBuilder, requestBuilder);
                Credential credential = null;
                if (authSpecification != null)
                {
                    credential = makeCredential(authSpecification, identity);
                }
                configureAuthentication(httpConnectionBuilder, securityScheme, credential, identity);
                return httpConnectionBuilder;
            }
            catch (Exception e)
            {
                Span span = GlobalTracer.get().activeSpan();
                if (span != null)
                {
                    span.log("Unable to obtain/use service store credential : " + e.getMessage());
                }
            }
        }

        return new HttpConnectionBuilder(HttpClients.custom(), makeRequestUtil(serviceStoreConnectionSpecification));

    }

    public static RequestBuilder makeRequestUtil(ServiceStoreConnectionSpecification serviceStoreConnectionSpecification) throws Exception
    {
        RequestBuilder builder = null;
        switch (serviceStoreConnectionSpecification.httpMethod)
        {
            case "GET":
                builder = RequestBuilder.get(serviceStoreConnectionSpecification.uri);
                break;
            case "POST":
                builder = RequestBuilder.post(serviceStoreConnectionSpecification.uri).setEntity(serviceStoreConnectionSpecification.requestBodyDescription);
                break;
            default:
                throw new UnsupportedOperationException("The HTTP method " + serviceStoreConnectionSpecification.httpMethod + " is not supported");
        }
        serviceStoreConnectionSpecification.headers.forEach(builder::addHeader);

        return builder;
    }

    private void configureAuthentication(HttpConnectionBuilder httpConnectionBuilder, SecurityScheme scheme, Credential credential, Identity identity)
    {
        List<IServiceStoreExecutionExtension> extensions = IServiceStoreExecutionExtension.getExtensions();
        List<Function<Credential,String>> credentialConsumers = ListIterate.flatCollect(extensions, ext -> ext.getExtraCredentialConsumers());
        List<Function5<SecurityScheme, Credential, HttpConnectionBuilder, Identity, CredentialProviderProvider, Boolean>> processors = ListIterate.flatCollect(extensions, ext -> ext.getExtraSecuritySchemeProcessors(credentialConsumers));

        ListIterate
                .collect(processors, processor -> processor.value(scheme, credential, httpConnectionBuilder, identity, this.credentialProviderProvider))
                .select(Objects::nonNull)
                .getFirstOptional()
                .orElseThrow(() -> new RuntimeException("SecurityScheme/AuthenticationSpecification not supported"));
    }

}
