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

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.protocol.functionActivator.postDeployment.ActionContent;
import org.finos.legend.engine.protocol.functionActivator.postDeployment.PostDeploymentContract;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_FunctionActivator;

import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;

public class PostDeploymentLoader
{
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(PostDeploymentContract.class);
    private static final AtomicReference<MutableList<PostDeploymentContract>> INSTANCE = new AtomicReference<>();

    public static MutableList<PostDeploymentContract> extensions()
    {
        return INSTANCE.updateAndGet(existing ->
        {
            if (existing == null)
            {
                MutableList<PostDeploymentContract> extensions = Lists.mutable.empty();
                for (PostDeploymentContract extension : ServiceLoader.load(PostDeploymentContract.class))
                {
                    try
                    {
                        extensions.add(extension);
                    }
                    catch (Throwable throwable)
                    {
                        LOGGER.error("Failed to load execution extension '" + extension.getClass().getSimpleName() + "'");
                    }
                }
                return extensions;
            }
            return existing;
        });
    }

    public static List<ActionContent> generateActions(Root_meta_external_function_activator_FunctionActivator activator)
    {
        List<ActionContent> actionsContent = Lists.mutable.empty();
        extensions().forEach(e ->
        {
            actionsContent.addAll(e.generate(activator._actions()));
        });

        return actionsContent;
    }
}
