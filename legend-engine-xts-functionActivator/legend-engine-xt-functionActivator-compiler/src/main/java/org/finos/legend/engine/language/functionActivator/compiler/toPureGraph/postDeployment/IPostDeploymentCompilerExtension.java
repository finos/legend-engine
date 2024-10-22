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

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.protocol.functionActivator.metamodel.PostDeploymentAction;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_postDeploymentAction_PostDeploymentAction;

import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;

public interface IPostDeploymentCompilerExtension extends CompilerExtension
{
    static List<IPostDeploymentCompilerExtension> getExtensions()
    {
        return Lists.mutable.withAll(ServiceLoader.load(IPostDeploymentCompilerExtension.class));
    }

    static Root_meta_external_function_activator_postDeploymentAction_PostDeploymentAction process(PostDeploymentAction postDeploymentAction, List<Function2<PostDeploymentAction, CompileContext, Root_meta_external_function_activator_postDeploymentAction_PostDeploymentAction>> processors, CompileContext context)
    {
        return process(postDeploymentAction, processors, context, "Post Deployment Compiler Extension");
    }

    static <T, U> U process(T item, List<Function2<T, CompileContext, U>> processors, CompileContext context, String type)
    {
        return ListIterate
                .collect(processors, processor -> processor.value(item, context))
                .select(Objects::nonNull)
                .getFirstOptional()
                .orElseThrow(() -> new EngineException("Unsupported " + type + " type '" + item.getClass() + "'", EngineErrorType.COMPILATION));
    }

    default List<Function2<PostDeploymentAction, CompileContext, Root_meta_external_function_activator_postDeploymentAction_PostDeploymentAction>> getExtraPostDeploymentActionProcessors()
    {
        return FastList.newList();
    }
}
