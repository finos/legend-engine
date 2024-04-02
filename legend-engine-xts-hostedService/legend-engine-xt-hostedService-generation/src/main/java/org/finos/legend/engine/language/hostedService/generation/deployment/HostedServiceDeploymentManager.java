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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import java.security.PrivilegedExceptionAction;
import java.util.List;

public class HostedServiceDeploymentManager implements  DeploymentManager<HostedServiceArtifact, HostedServiceDeploymentResult, HostedServiceDeploymentConfiguration>
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
        if (artifact.version == null)
        {
            deployConf = c.select(conf -> conf.destination.equals(HostedServiceDestination.Sandbox)).getFirst();
        }
        else
        {
            deployConf = (HostedServiceDeploymentConfiguration) artifact.deploymentConfiguration;
        }

        return doDeploy(identity, deployConf, artifact);
    }

    public HostedServiceDeploymentResult doDeploy(Identity identity, HostedServiceDeploymentConfiguration deployConf, HostedServiceArtifact artifact)
    {
        HostedServiceDeploymentResult result = new HostedServiceDeploymentResult();
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
            Subject.doAs(subject, (PrivilegedExceptionAction<String>) () ->
            {
                HttpResponse response = httpclient.execute(request);
                if (response.getStatusLine().getStatusCode() != 200)
                {
                    result.error = EntityUtils.toString(response.getEntity());
                }
                else
                {
                    HostedServiceDeploymentResult responseResult = mapper.readValue(EntityUtils.toString(response.getEntity()), HostedServiceDeploymentResult.class);
                    result.deployed = responseResult.deployed;
                    result.successful = true;
                    result.deploymentLocation = responseResult.deploymentLocation;
                }
                return "done";
            });
            //LOGGER.info("Done deploying hosted service");
        }
        catch (Exception e)
        {
            LOGGER.error("Error deploying hosted service", e);
            throw new EngineException(e.getMessage());

        }
        return result;
    }

    public String buildDeployStub(HostedServiceDeploymentConfiguration config, HostedServiceArtifact artifact)
    {
        //change to UI
        return "http://" + config.domain + ":" + config.port + config.path + ((HostedServiceContent)artifact.content).pattern;
    }
}
