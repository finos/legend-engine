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

package org.finos.legend.engine.language.functionActivator.compiler.toPureGraph.postDeployment;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.protocol.functionActivator.metamodel.PostDeploymentAction;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_postDeploymentAction_PostDeploymentAction;

import java.util.List;

public class HelperPostDeploymentCompilerBuilder
{
    public static MutableList<Root_meta_external_function_activator_postDeploymentAction_PostDeploymentAction> resolveDeploymentAction(List<PostDeploymentAction> actions, CompileContext context)
    {
        List<IPostDeploymentCompilerExtension> extensions = IPostDeploymentCompilerExtension.getExtensions();
        return ListIterate.collect(actions, action -> IPostDeploymentCompilerExtension.process(
                action,
                ListIterate.flatCollect(extensions, IPostDeploymentCompilerExtension::getExtraPostDeploymentActionProcessors),
                context));
    }
}
