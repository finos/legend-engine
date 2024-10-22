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

package org.finos.legend.engine.language.functionActivator.grammar.postDeployment.to;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.to.extension.PureGrammarComposerExtension;
import org.finos.legend.engine.protocol.functionActivator.metamodel.PostDeploymentAction;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;

public interface IPostDeploymentActionGrammarComposerExtension extends PureGrammarComposerExtension
{
    static List<IPostDeploymentActionGrammarComposerExtension> getExtensions()
    {
        return Lists.mutable.withAll(ServiceLoader.load(IPostDeploymentActionGrammarComposerExtension.class));
    }

    static String process(PostDeploymentAction postDeploymentAction, List<Function<PostDeploymentAction, String>> processors)
    {
        return process(postDeploymentAction, processors, "Post Deployment Action");
    }

    static <T> String process(T item, List<Function<T, String>> processors, String type)
    {
        return ListIterate
                .collect(processors, processor -> processor.valueOf(item))
                .select(Objects::nonNull)
                .getFirstOptional()
                .orElseThrow(() -> new EngineException("Unsupported " + type + " type '" + item.getClass() + "'", EngineErrorType.PARSER));
    }

    default List<Function<PostDeploymentAction, String>> getExtraPostDeploymentActionComposer()
    {
        return FastList.newList();
    }
}
