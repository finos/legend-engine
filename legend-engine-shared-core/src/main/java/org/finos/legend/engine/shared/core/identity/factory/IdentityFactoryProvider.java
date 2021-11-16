// Copyright 2021 Goldman Sachs
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

import java.util.Iterator;
import java.util.ServiceLoader;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;

public class IdentityFactoryProvider
{
    private IdentityFactoryProvider()
    {
        // singleton
    }

    private static DefaultIdentityFactory DEFAULT = new DefaultIdentityFactory();

    public static synchronized IdentityFactory getInstance()
    {
        MutableList<IdentityFactory> identityFactories = locateIdentityFactories();
        if (identityFactories.isEmpty())
        {
            return DEFAULT;
        }
        if (identityFactories.size() == 1)
        {
            return identityFactories.get(0);
        }
        MutableList<String> names = identityFactories.collect(c -> c.getClass().getCanonicalName());
        String message = String.format("Found too many identity factories in classpath. Expected 1 but found %d. [%s]", identityFactories.size(), names);
        throw new RuntimeException(message);
    }

    private static MutableList<IdentityFactory> locateIdentityFactories()
    {
        Iterator<IdentityFactory> iterator = ServiceLoader.load(IdentityFactory.class).iterator();
        MutableList<IdentityFactory> factories = Lists.mutable.empty();
        while (iterator.hasNext())
        {
            factories.add(iterator.next());
        }
        return factories;
    }
}