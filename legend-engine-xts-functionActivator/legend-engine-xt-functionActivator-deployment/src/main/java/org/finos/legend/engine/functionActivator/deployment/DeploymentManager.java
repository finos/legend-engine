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

package org.finos.legend.engine.functionActivator.deployment;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.functionActivator.postDeployment.PostDeploymentActionLoader;
import org.finos.legend.engine.protocol.functionActivator.deployment.DeploymentResult;
import org.finos.legend.engine.protocol.functionActivator.deployment.FunctionActivatorArtifact;
import org.finos.legend.engine.protocol.functionActivator.deployment.FunctionActivatorDeploymentConfiguration;
import org.finos.legend.engine.protocol.functionActivator.deployment.FunctionActivatorDeploymentDetails;
import org.finos.legend.engine.protocol.functionActivator.deployment.PostDeploymentActionResult;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_FunctionActivator;

import java.util.List;

public interface DeploymentManager<U extends FunctionActivatorArtifact, V extends DeploymentResult, W extends FunctionActivatorDeploymentConfiguration, X extends FunctionActivatorDeploymentDetails, Y extends Root_meta_external_function_activator_FunctionActivator>
{

    public List<W> selectConfig(List<FunctionActivatorDeploymentConfiguration> availableConfigs);

    public V deploy(Identity identity, U artifact);

    public V deploy(Identity identity, U artifact, List<W> availableRuntimeConfigurations);

    public X getActivatorDetails(Identity identity, W deploymentConfig, Y activator);

    public boolean canDeploy(FunctionActivatorArtifact activatorArtifact);

    public default List<PostDeploymentActionResult> deployActions(Identity identity, U artifact)
    {
        List<PostDeploymentActionResult> actionResults = Lists.mutable.empty();
        PostDeploymentActionLoader.extensions().forEach((ex) ->
        {
            if (ex.canDeploy(artifact))
            {
                actionResults.addAll(ex.processAction(identity, artifact));
            }
        });
        return actionResults;
    }
}
