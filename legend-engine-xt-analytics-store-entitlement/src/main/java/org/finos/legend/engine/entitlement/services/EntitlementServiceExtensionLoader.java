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

package org.finos.legend.engine.entitlement.services;

import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.LazyIterate;

import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;

public class EntitlementServiceExtensionLoader
{
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Engine Execution Server");
    private static final AtomicReference<List<EntitlementServiceExtension>> INSTANCE = new AtomicReference<>();

    public static void logExtensionList()
    {
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug(LazyIterate.collect(extensions(), extension -> "- " + extension.getClass().getSimpleName()).makeString("EntitlementServiceExtensionLoader loaded:\n", "\n", ""));
        }
    }

    public static List<EntitlementServiceExtension> extensions()
    {
        return INSTANCE.updateAndGet(existing ->
        {
            if (existing == null)
            {
                List<EntitlementServiceExtension> extensions = Lists.mutable.empty();
                for (EntitlementServiceExtension extension : ServiceLoader.load(EntitlementServiceExtension.class))
                {
                    try
                    {
                        extensions.add(extension);
                    }
                    catch (Throwable throwable)
                    {
                        LOGGER.error("Failed to load EntitlementServiceExtension '" + extension.getClass().getSimpleName() + "'");
                        // Needs to be silent ... during the build process
                    }
                }
                return extensions;
            }
            return existing;
        });
    }
}
