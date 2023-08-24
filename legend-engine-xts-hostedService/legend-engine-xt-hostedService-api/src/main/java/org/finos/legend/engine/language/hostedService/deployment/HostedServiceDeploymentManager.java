//  Copyright 2023 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.language.hostedService.deployment;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.functionActivator.deployment.DeploymentManager;
import org.finos.legend.engine.protocol.functionActivator.metamodel.DeploymentStage;
import org.finos.legend.engine.protocol.hostedService.metamodel.HostedServiceDeploymentConfiguration;
import org.finos.legend.engine.protocol.hostedService.metamodel.HostedServiceDeploymentResult;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.kerberos.HttpClientBuilder;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_hostedService_HostedService;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_hostedService_HostedServiceDeploymentConfiguration;
import org.pac4j.core.profile.CommonProfile;

import javax.security.auth.Subject;
import java.security.Principal;
import java.security.PrivilegedExceptionAction;
import java.util.List;

public class HostedServiceDeploymentManager implements  DeploymentManager<Root_meta_external_function_activator_hostedService_HostedService, HostedServiceArtifact, HostedServiceDeploymentResult, HostedServiceDeploymentConfiguration>
{

    public static ObjectMapper mapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    public boolean canDeploy(Root_meta_external_function_activator_hostedService_HostedService element)
    {
        return element._activationConfiguration() != null;
    }

    public HostedServiceDeploymentResult deploy(MutableList<CommonProfile> profiles, HostedServiceArtifact artifact, Root_meta_external_function_activator_hostedService_HostedService activator)
    {
        return new HostedServiceDeploymentResult();
    }


    public HostedServiceDeploymentResult deploy(MutableList<CommonProfile> profiles, HostedServiceArtifact artifact, Root_meta_external_function_activator_hostedService_HostedService activator, List<HostedServiceDeploymentConfiguration> availableRuntimeConfigurations)
    {
        String host;
        String path;
        int port;

        if (activator._activationConfiguration() == null || activator._activationConfiguration()._stage()._name().equals(DeploymentStage.SANDBOX.name()))
        {
            if (availableRuntimeConfigurations.size() > 0)
            {
                host = availableRuntimeConfigurations.get(0).host;
                path = availableRuntimeConfigurations.get(0).path;
                port = availableRuntimeConfigurations.get(0).port;
            }
            else
            {
                throw new EngineException("No available configuration for sandbox deployment");
            }
            try
            {
                HttpPost request = new HttpPost(new URIBuilder()
                        .setScheme("http")
                        .setHost(host)
                        .setPort(port)
                        .setPath(path)
                        .build());
                StringEntity stringEntity = new StringEntity(mapper.writeValueAsString(artifact));
                stringEntity.setContentType("application/json");
                request.setEntity(stringEntity);
                CloseableHttpClient httpclient = (CloseableHttpClient) HttpClientBuilder.getHttpClient(new BasicCookieStore());
                Subject s = ProfileManagerHelper.extractSubject(profiles);
                Subject.doAs(s, (PrivilegedExceptionAction<HostedServiceDeploymentResult>) () ->
                            {
                                HttpResponse response = httpclient.execute(request);
                                return new HostedServiceDeploymentResult();
                            });
            }
            catch (Exception e)
            {
                throw new EngineException("No available configuration for sandbox deployment");

            }
        }
//        else if (activator._activationConfiguration() != null)
//        {
//            host = ((Root_meta_external_function_activator_hostedService_HostedServiceDeploymentConfiguration)activator._activationConfiguration())._;
//            path = availableRuntimeConfigurations.get(0).path;
//            port = availableRuntimeConfigurations.get(0).port;
//        }
        return new HostedServiceDeploymentResult();
    }


}
