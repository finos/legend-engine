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

package org.finos.legend.engine.shared.core.identity.factory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

final class AuthenticationSourceProviderLoader
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationSourceProviderLoader.class);

    static final List<AuthenticationSourceProvider> PROVIDERS = load(Thread.currentThread().getContextClassLoader());

    private AuthenticationSourceProviderLoader()
    {
    }

    static List<AuthenticationSourceProvider> load(ClassLoader classLoader)
    {
        List<AuthenticationSourceProvider> providers = new ArrayList<>();
        Iterator<AuthenticationSourceProvider> iterator = ServiceLoader.load(AuthenticationSourceProvider.class, classLoader).iterator();
        while (true)
        {
            try
            {
                if (!iterator.hasNext())
                {
                    return Collections.unmodifiableList(providers);
                }
                providers.add(iterator.next());
            }
            catch (ServiceConfigurationError | LinkageError e)
            {
                LOGGER.warn("Skipping an {} that failed to load", AuthenticationSourceProvider.class.getSimpleName(), e);
            }
        }
    }
}
