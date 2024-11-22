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

package org.finos.legend.engine.functionActivator.generation;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.functionActivator.postDeployment.PostDeploymentActionLoader;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.functionActivator.postDeployment.ActionContent;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_FunctionActivator;

import java.util.List;

public abstract class FunctionActivatorGenerator
{
    public static List<ActionContent> generateActions(Root_meta_external_function_activator_FunctionActivator activator, PureModel pureModel)
    {
        List<ActionContent> actionResults = Lists.mutable.empty();
        PostDeploymentActionLoader.generationExtensions().forEach((ex) ->
        {
            actionResults.addAll(ex.generate(activator, pureModel));
        });
        return actionResults;
    }
}
