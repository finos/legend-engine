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

package org.finos.legend.engine.language.functionJar.generation.deployment;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.functionActivator.deployment.DeploymentManager;
import org.finos.legend.engine.protocol.functionActivator.deployment.FunctionActivatorArtifact;
import org.finos.legend.engine.protocol.functionActivator.deployment.FunctionActivatorDeploymentConfiguration;
import org.finos.legend.engine.protocol.functionJar.deployment.*;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class FunctionJarDeploymentManager implements  DeploymentManager<FunctionJarArtifact, FunctionJarDeploymentResult, FunctionJarDeploymentConfiguration>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(FunctionJarDeploymentManager.class);

    public static ObjectMapper mapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    public boolean canDeploy(FunctionActivatorArtifact element)
    {
        return element instanceof FunctionJarArtifact;
    }

    public List<FunctionJarDeploymentConfiguration> selectConfig(List<FunctionActivatorDeploymentConfiguration> availableConfigs)
    {
        return Lists.mutable.withAll(availableConfigs).selectInstancesOf(FunctionJarDeploymentConfiguration.class);
    }

    public FunctionJarDeploymentResult deploy(Identity identity, FunctionJarArtifact artifact)
    {
        FunctionJarDeploymentResult result = new FunctionJarDeploymentResult();
        return result;
    }

    public FunctionJarDeploymentResult deploy(Identity identity, FunctionJarArtifact artifact, List<FunctionJarDeploymentConfiguration> availableRuntimeConfigurations)
    {
        FunctionJarDeploymentResult result = new FunctionJarDeploymentResult();
        return result;
    }
}
