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

package org.finos.legend.engine.language.hostedService.generation.deployment;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.functionActivator.deployment.DeploymentManager;
import org.finos.legend.engine.protocol.functionActivator.deployment.FunctionActivatorArtifact;
import org.finos.legend.engine.protocol.functionActivator.deployment.FunctionActivatorDeploymentConfiguration;
import org.finos.legend.engine.protocol.hostedService.deployment.*;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.LegendKerberosCredential;
import org.finos.legend.engine.shared.core.kerberos.HttpClientBuilder;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_hostedService_HostedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.PrivilegedExceptionAction;
import java.util.List;

public class HostedServiceDeploymentManager implements  DeploymentManager<HostedServiceArtifact, HostedServiceDeploymentResult, HostedServiceDeploymentConfiguration, HostedServiceDeploymentDetails, Root_meta_external_function_activator_hostedService_HostedService>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(HostedServiceDeploymentManager.class);

    public static ObjectMapper mapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    public boolean canDeploy(FunctionActivatorArtifact element)
    {
        return element instanceof HostedServiceArtifact;
    }

    public List<HostedServiceDeploymentConfiguration> selectConfig(List<FunctionActivatorDeploymentConfiguration> availableConfigs)
    {
        return Lists.mutable.withAll(availableConfigs).selectInstancesOf(HostedServiceDeploymentConfiguration.class);
    }

    public HostedServiceDeploymentResult deploy(Identity identity, HostedServiceArtifact artifact)
    {
        return doDeploy(identity, (HostedServiceDeploymentConfiguration) artifact.deploymentConfiguration, artifact);
    }

    public HostedServiceDeploymentResult deploy(Identity identity, HostedServiceArtifact artifact, List<HostedServiceDeploymentConfiguration> availableRuntimeConfigurations)
    {
        HostedServiceDeploymentConfiguration deployConf;
        MutableList<HostedServiceDeploymentConfiguration> c = Lists.mutable.withAll(availableRuntimeConfigurations);
        deployConf = getDeploymentConfiguration(artifact, availableRuntimeConfigurations);
        if (artifact.version == null)
        {
            artifact.deploymentConfiguration = deployConf;
        }

        return doDeploy(identity, deployConf, artifact);
    }

    public HostedServiceDeploymentResult doDeploy(Identity identity, HostedServiceDeploymentConfiguration deployConf, HostedServiceArtifact artifact)
    {
        try
        {
            HttpPost request = new HttpPost(new URIBuilder()
                    .setScheme("http")
                    .setHost(deployConf.domain)
                    //.setPort(deployConf.port)
                    .setPath(deployConf.path)
                    .build());
            StringEntity stringEntity = new StringEntity(mapper.writeValueAsString(artifact));
            stringEntity.setContentType("application/json");
            request.setEntity(stringEntity);
            CloseableHttpClient httpclient = (CloseableHttpClient) HttpClientBuilder.getHttpClient(new BasicCookieStore());
            Subject subject = identity.getCredential(LegendKerberosCredential.class).get().getSubject();
            return Subject.doAs(subject, (PrivilegedExceptionAction<HostedServiceDeploymentResult>) () ->
            {
                HostedServiceDeploymentResult result = new HostedServiceDeploymentResult();
                HttpResponse response = httpclient.execute(request);
                if (response.getStatusLine().getStatusCode() != 200)
                {
                    result.error = EntityUtils.toString(response.getEntity());
                }
                else
                {
                    result = mapper.readValue(EntityUtils.toString(response.getEntity()), HostedServiceDeploymentResult.class);
                }
                return result;
            });
            //LOGGER.info("Done deploying hosted service");
        }
        catch (Exception e)
        {
            LOGGER.error(new LogInfo(Identity.getAnonymousIdentity().getName(), "HOSTED_SERVICE_DEPLOYMENT_FAILURE", "Error deploying hosted service" + e).toString());
            throw new EngineException(e.getMessage());

        }
    }

    @Override
    public HostedServiceDeploymentDetails getActivatorDetails(Identity identity, HostedServiceDeploymentConfiguration runtimeConfig, Root_meta_external_function_activator_hostedService_HostedService hostedService)
    {
        try
        {
            HttpGet request = new HttpGet(new URIBuilder()
                    .setScheme("http")
                    .setHost(runtimeConfig.domain)
                    .setPath(runtimeConfig.serviceDetails + "/" + URLEncoder.encode(hostedService._pattern(), StandardCharsets.UTF_8.toString()))
                    .build());
            CloseableHttpClient httpclient = (CloseableHttpClient) HttpClientBuilder.getHttpClient(new BasicCookieStore());
            Subject subject = identity.getCredential(LegendKerberosCredential.class).get().getSubject();
            return Subject.doAs(subject, (PrivilegedExceptionAction<HostedServiceDeploymentDetails>) () ->
            {
                HostedServiceDeploymentDetails data = new HostedServiceDeploymentDetails();
                HttpResponse response = httpclient.execute(request);
                if (response.getStatusLine().getStatusCode() != 200)
                {
                    data.errorMessage = "Failed to get Active Activator Data. Error Response: " +  EntityUtils.toString(response.getEntity());
                }
                else
                {
                    data = mapper.readValue(EntityUtils.toString(response.getEntity()), HostedServiceDeploymentDetails.class);
                }
                return data;
            });
        }
        catch (Exception e)
        {
            LOGGER.error(new LogInfo(Identity.getAnonymousIdentity().getName(), "HOSTED_SERVICE_DETAILS_FAILURE", "Error fetching active hosted service details. Exception: " + e.getMessage()).toString());
            throw new EngineException("Error fetching active hosted service data. Exception received: " + e.getMessage());
        }
    }

    public String buildDeployStub(HostedServiceDeploymentConfiguration config, HostedServiceArtifact artifact)
    {
        //change to UI
        return "http://" + config.domain + ":" + config.port + config.path + ((HostedServiceContent)artifact.content).pattern;
    }
    
    public HostedServiceDeploymentConfiguration getDeploymentConfiguration(HostedServiceArtifact artifact, List<HostedServiceDeploymentConfiguration> availableRuntimeConfigurations)
    {
        MutableList<HostedServiceDeploymentConfiguration> c = Lists.mutable.withAll(availableRuntimeConfigurations);
        if (artifact.version == null)
        {
            return c.select(conf -> conf.destination.equals(HostedServiceDestination.Sandbox)).getFirst();
        }
        else
        {
            return (HostedServiceDeploymentConfiguration) artifact.deploymentConfiguration;
        }
    }
}
