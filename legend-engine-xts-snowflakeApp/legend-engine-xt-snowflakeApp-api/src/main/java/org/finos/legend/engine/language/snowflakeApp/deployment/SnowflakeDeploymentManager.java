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

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.functionActivator.deployment.DeploymentManager;
import org.finos.legend.engine.language.pure.dsl.generation.extension.Artifact;
import org.finos.legend.engine.language.pure.dsl.generation.extension.ArtifactGenerationExtension;
import org.finos.legend.engine.language.snowflakeApp.api.SnowflakeAppDeploymentTool;
import org.finos.legend.engine.protocol.snowflakeApp.metamodel.SnowflakeDeploymentConfiguration;
import org.finos.legend.engine.protocol.snowflakeApp.metamodel.SnowflakeDeploymentResult;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_snowflakeApp_SnowflakeApp;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_authentication_SnowflakePublicAuthenticationStrategy;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_specification_SnowflakeDatasourceSpecification;
import org.pac4j.core.profile.CommonProfile;

import java.util.List;


public class SnowflakeDeploymentManager implements DeploymentManager<SnowflakeAppArtifact, SnowflakeDeploymentResult, SnowflakeDeploymentConfiguration>
{
    private SnowflakeAppDeploymentTool snowflakeAppDeploymentTool;

    public SnowflakeDeploymentManager(SnowflakeAppDeploymentTool deploymentTool)
    {
        this.snowflakeAppDeploymentTool = deploymentTool;
    }

    @Override
    public SnowflakeDeploymentResult deploy(MutableList<CommonProfile> profiles, SnowflakeAppArtifact artifact)
    {
        return new SnowflakeDeploymentResult(true);
    }

    @Override
    public SnowflakeDeploymentResult deploy(MutableList<CommonProfile> profiles, SnowflakeAppArtifact artifact, List<SnowflakeDeploymentConfiguration> availableRuntimeConfigurations)
    {
        return null;
    }

    @Override
    public boolean canDeploy(SnowflakeAppArtifact artifact)
    {
        return true;
    }

    public SnowflakeAppDeploymentTool getSnowflakeAppDeploymentTool()
    {
        return snowflakeAppDeploymentTool;
    }


    public SnowflakeDeploymentResult fakeDeploy(Root_meta_pure_alloy_connections_alloy_specification_SnowflakeDatasourceSpecification datasourceSpecification, Root_meta_pure_alloy_connections_alloy_authentication_SnowflakePublicAuthenticationStrategy authenticationStrategy, String applicationName)
    {
        try
        {
            this.snowflakeAppDeploymentTool.deploy(datasourceSpecification, authenticationStrategy, applicationName);
            return new SnowflakeDeploymentResult(true);
        }
        catch (Exception e)
        {
            return new SnowflakeDeploymentResult(Lists.mutable.with(e.getMessage()));
        }
    }

}
