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

package org.finos.legend.engine.language.jarService.generation.deployment;

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
import org.finos.legend.engine.protocol.jarService.deployment.*;
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

public class JarServiceDeploymentManager implements  DeploymentManager<JarServiceArtifact, JarServiceDeploymentResult, JarServiceDeploymentConfiguration>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(JarServiceDeploymentManager.class);

    public static ObjectMapper mapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    public boolean canDeploy(FunctionActivatorArtifact element)
    {
        return element instanceof JarServiceArtifact;
    }

    public List<JarServiceDeploymentConfiguration> selectConfig(List<FunctionActivatorDeploymentConfiguration> availableConfigs)
    {
        return Lists.mutable.withAll(availableConfigs).selectInstancesOf(JarServiceDeploymentConfiguration.class);
    }

    public JarServiceDeploymentResult deploy(Identity identity, JarServiceArtifact artifact)
    {
        JarServiceDeploymentResult result = new JarServiceDeploymentResult();
        return result;
    }

    public JarServiceDeploymentResult deploy(Identity identity, JarServiceArtifact artifact, List<JarServiceDeploymentConfiguration> availableRuntimeConfigurations)
    {
        JarServiceDeploymentResult result = new JarServiceDeploymentResult();
        return result;
    }
}
