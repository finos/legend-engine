// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.connection.manager;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.slf4j.Logger;

import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;

public class ConnectionManagerExtensionLoader
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ConnectionManagerExtensionLoader.class);
    private static final AtomicReference<List<ConnectionManagerExtension>> EXTENSIONS = new AtomicReference<>();

    public static List<ConnectionManagerExtension> extensions()
    {
        return EXTENSIONS.updateAndGet(extensions ->
        {
            if (extensions == null)
            {
                MutableList<ConnectionManagerExtension> result = Lists.mutable.empty();
                for (ConnectionManagerExtension extension : ServiceLoader.load(ConnectionManagerExtension.class))
                {
                    try
                    {
                        result.add(extension);
                    }
                    catch (Throwable throwable)
                    {
                        LOGGER.error("Failed to load ConnectionManagerExtension '" + extension.getClass().getSimpleName() + "'");
                    }
                }
                return result;
            }
            return extensions;
        });
    }
}
