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
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;

public class PostDeploymentActionLoader
{
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(PostDeploymentActionLoader.class);
    private static final AtomicReference<MutableList<PostDeploymentActionDeploymentContract>> INSTANCE = new AtomicReference<>();
    private static final AtomicReference<MutableList<PostDeploymentGeneration>> GENERATION_INSTANCE = new AtomicReference<>();

    public static MutableList<PostDeploymentActionDeploymentContract> extensions()
    {
        return INSTANCE.updateAndGet(existing ->
        {
            if (existing == null)
            {
                MutableList<PostDeploymentActionDeploymentContract> extensions = Lists.mutable.empty();
                for (PostDeploymentActionDeploymentContract extension : ServiceLoader.load(PostDeploymentActionDeploymentContract.class))
                {
                    try
                    {
                        extensions.add(extension);
                    }
                    catch (Throwable throwable)
                    {
                        LOGGER.error("Failed to load deployment execution extension '" + extension.getClass().getSimpleName() + "'");
                    }
                }
                return extensions;
            }
            return existing;
        });
    }

    public static MutableList<PostDeploymentGeneration> generationExtensions()
    {
        return GENERATION_INSTANCE.updateAndGet(existing ->
        {
            if (existing == null)
            {
                MutableList<PostDeploymentGeneration> generationExtensions = Lists.mutable.empty();
                for (PostDeploymentGeneration extension : ServiceLoader.load(PostDeploymentGeneration.class))
                {
                    try
                    {
                        generationExtensions.add(extension);
                    }
                    catch (Throwable throwable)
                    {
                        LOGGER.error("Failed to load generation extension '" + extension.getClass().getSimpleName() + "'");
                    }
                }
                return generationExtensions;
            }
            return existing;
        });
    }
}
