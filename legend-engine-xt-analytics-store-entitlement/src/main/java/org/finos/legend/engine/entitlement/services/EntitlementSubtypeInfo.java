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

import com.fasterxml.jackson.databind.module.SimpleModule;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;

import java.util.List;

public class EntitlementSubtypeInfo<T>
{
    private final Class<T> superType;
    private final Class<? extends T> defaultSubType;
    private final MutableList<Pair<Class<? extends T>, String>> subTypes;

    private EntitlementSubtypeInfo(EntitlementSubtypeInfo.Builder<T> builder)
    {
        this.superType = builder.superType;
        this.defaultSubType = builder.defaultSubType;
        this.subTypes = builder.subTypes;
    }

    public Class<T> getSuperType()
    {
        return this.superType;
    }

    public Class<? extends T> getDefaultSubType()
    {
        return this.defaultSubType;
    }

    public List<Pair<Class<? extends T>, String>> getSubTypes()
    {
        return this.subTypes.asUnmodifiable();
    }

    public SimpleModule registerDefaultSubType()
    {
        if (this.defaultSubType == null)
        {
            throw new RuntimeException("Default sub type has not been set");
        }
        return new SimpleModule().addAbstractTypeMapping(this.superType, this.defaultSubType);
    }

    public static <T> EntitlementSubtypeInfo.Builder<T> newBuilder(Class<T> superType)
    {
        return EntitlementSubtypeInfo.Builder.newInstance(superType);
    }

    public static class Builder<T>
    {
        private final Class<T> superType;
        private Class<? extends T> defaultSubType;
        private final MutableList<Pair<Class<? extends T>, String>> subTypes = Lists.mutable.empty();

        private Builder(Class<T> superType)
        {
            this.superType = superType;
        }

        public static <T> EntitlementSubtypeInfo.Builder<T> newInstance(Class<T> superType)
        {
            return new EntitlementSubtypeInfo.Builder<>(superType);
        }

        public EntitlementSubtypeInfo<T> build()
        {
            return new EntitlementSubtypeInfo<>(this);
        }

        public EntitlementSubtypeInfo.Builder<T> withDefaultSubType(Class<? extends T> subType)
        {
            this.defaultSubType = subType;
            return this;
        }

        public EntitlementSubtypeInfo.Builder<T> withSubtypes(Iterable<? extends Pair<Class<? extends T>, String>> subTypes)
        {
            this.subTypes.addAllIterable(subTypes);
            return this;
        }

        public EntitlementSubtypeInfo.Builder<T> withSubtype(Class<? extends T> type, String name)
        {
            this.subTypes.add(Tuples.pair(type, name));
            return this;
        }
    }
}
