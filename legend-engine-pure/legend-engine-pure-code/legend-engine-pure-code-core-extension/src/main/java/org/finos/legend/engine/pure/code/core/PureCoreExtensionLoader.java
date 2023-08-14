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

package org.finos.legend.engine.pure.code.core;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.LazyIterate;

import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;

public class PureCoreExtensionLoader
{
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(PureCoreExtensionLoader.class);
    private static final AtomicReference<MutableList<PureCoreExtension>> INSTANCE = new AtomicReference<>();

    public static void logExtensionList()
    {
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug(LazyIterate.collect(extensions(), extension -> "- " + extension.getClass().getSimpleName()).makeString("PureCoreExtension extension(s) loaded:\n", "\n", ""));
        }
    }

    public static MutableList<PureCoreExtension> extensions()
    {
        return INSTANCE.updateAndGet(existing ->
        {
            if (existing == null)
            {
                MutableList<PureCoreExtension> extensions = Lists.mutable.empty();
                for (PureCoreExtension extension : ServiceLoader.load(PureCoreExtension.class))
                {
                    try
                    {
                        extensions.add(extension);
                    }
                    catch (Throwable throwable)
                    {
                        LOGGER.error("Failed to load PureCoreExtension extension '" + extension.getClass().getSimpleName() + "'");
                        // Needs to be silent ... during the build process
                    }
                }
                return extensions;
            }
            return existing;
        });
    }
}
