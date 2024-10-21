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

package org.finos.legend.engine.functionActivator.postDeployment;

import org.eclipse.collections.api.RichIterable;
import org.finos.legend.engine.protocol.functionActivator.deployment.FunctionActivatorArtifact;
import org.finos.legend.engine.protocol.functionActivator.deployment.FunctionActivatorDeploymentConfiguration;
import org.finos.legend.engine.protocol.functionActivator.deployment.PostDeploymentActionResult;
import org.finos.legend.engine.protocol.functionActivator.postDeployment.ActionContent;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_postDeploymentAction_PostDeploymentAction;

import java.util.List;

public interface PostDeploymentContract
{
    List<ActionContent> generate(RichIterable<? extends Root_meta_external_function_activator_postDeploymentAction_PostDeploymentAction> actions);

    List<PostDeploymentActionResult> processAction(Identity identity, FunctionActivatorArtifact artifact);
}