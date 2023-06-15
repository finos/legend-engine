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

package org.finos.legend.engine.functionActivator.service;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;

import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;

public class FunctionActivatorLoader
{
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");
    private static final AtomicReference<MutableList<FunctionActivatorService>> INSTANCE = new AtomicReference<>();

    public static MutableList<FunctionActivatorService> extensions()
    {
        return INSTANCE.updateAndGet(existing ->
        {
            if (existing == null)
            {
                MutableList<FunctionActivatorService> extensions = Lists.mutable.empty();
                for (FunctionActivatorService extension : ServiceLoader.load(FunctionActivatorService.class))
                {
                    try
                    {
                        extensions.add(extension);
                    }
                    catch (Throwable throwable)
                    {
                        LOGGER.error("Failed to load execution extension '" + extension.getClass().getSimpleName() + "'");
                        // Needs to be silent ... during the build process
                    }
                }
                return extensions;
            }
            return existing;
        });
    }
}
