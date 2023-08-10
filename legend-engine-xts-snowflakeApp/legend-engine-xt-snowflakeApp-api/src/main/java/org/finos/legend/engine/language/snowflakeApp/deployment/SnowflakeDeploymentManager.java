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

package org.finos.legend.engine.language.snowflakeApp.deployment;

import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.functionActivator.deployment.DeploymentManager;
import org.finos.legend.engine.functionActivator.service.FunctionActivatorError;
import org.finos.legend.engine.language.snowflakeApp.api.SnowflakeAppArtifact;
import org.finos.legend.engine.language.snowflakeApp.api.SnowflakeAppDeploymentTool;


public class SnowflakeDeploymentManager implements DeploymentManager<SnowflakeAppArtifact, SnowflakeDeploymentConfiguration, SnowflakeDeploymentResult>
{

    private SnowflakeAppDeploymentTool snowflakeAppDeploymentTool;

    public SnowflakeDeploymentManager(SnowflakeAppDeploymentTool deploymentTool)
    {
        this.snowflakeAppDeploymentTool = deploymentTool;
    }

    public SnowflakeDeploymentResult deploy(SnowflakeAppArtifact snowflakeAppArtifact, SnowflakeDeploymentConfiguration deploymentConfiguration)
    {
        try
        {
            this.snowflakeAppDeploymentTool.deploy(deploymentConfiguration.datasourceSpecification, deploymentConfiguration.authenticationStrategy, deploymentConfiguration.applicationName);
            return new SnowflakeDeploymentResult(true);
        }
        catch (Exception e)
        {
            return new SnowflakeDeploymentResult(Lists.mutable.with(new FunctionActivatorError(e.getMessage())));
        }
    }

    public SnowflakeAppDeploymentTool getSnowflakeAppDeploymentTool()
    {
        return snowflakeAppDeploymentTool;
    }
}
