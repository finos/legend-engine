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

package org.finos.legend.connection.impl;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.ImmutableMap;
import org.finos.legend.connection.StoreInstance;
import org.finos.legend.connection.StoreInstanceProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DefaultStoreInstanceProvider implements StoreInstanceProvider
{
    private final ImmutableMap<String, StoreInstance> storeInstancesIndex;

    private DefaultStoreInstanceProvider(Map<String, StoreInstance> storeInstancesIndex)
    {

        this.storeInstancesIndex = Maps.immutable.withAll(storeInstancesIndex);
    }

    @Override
    public StoreInstance lookup(String identifier)
    {
        return Objects.requireNonNull(this.storeInstancesIndex.get(identifier), String.format("Can't find store instance with identifier '%s'", identifier));
    }

    public static class Builder
    {
        private final Map<String, StoreInstance> storeInstancesIndex = new HashMap<>();

        public Builder()
        {

        }

        public Builder withStoreInstances(List<StoreInstance> storeInstances)
        {
            storeInstances.forEach(this::registerStoreInstance);
            return this;
        }

        public Builder withStoreInstances(StoreInstance... storeInstances)
        {
            Lists.mutable.with(storeInstances).forEach(this::registerStoreInstance);
            return this;
        }

        public Builder withStoreInstance(StoreInstance storeInstance)
        {
            this.registerStoreInstance(storeInstance);
            return this;
        }

        private void registerStoreInstance(StoreInstance storeInstance)
        {
            if (this.storeInstancesIndex.containsKey(storeInstance.getIdentifier()))
            {
                throw new RuntimeException(String.format("Found multiple store instances with identifier '%s'", storeInstance.getIdentifier()));
            }
            this.storeInstancesIndex.put(storeInstance.getIdentifier(), storeInstance);
        }

        public DefaultStoreInstanceProvider build()
        {
            return new DefaultStoreInstanceProvider(this.storeInstancesIndex);
        }
    }
}
