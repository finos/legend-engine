// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.protocol.pure.v1.extension;

import com.fasterxml.jackson.databind.module.SimpleModule;
import org.eclipse.collections.api.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

public class ProtocolSubTypeInfo<T>
{
    private final Class<T> superType;
    private final Class<? extends T> defaultSubType;
    private final List<Pair<Class<? extends T>, String>> subTypes;

    private ProtocolSubTypeInfo(ProtocolSubTypeInfo.Builder<T> builder)
    {
        this.superType = builder.superType;
        this.defaultSubType = builder.defaultSubType;
        this.subTypes = builder.subTypes;
    }

    public Class<T> getSuperType()
    {
        return superType;
    }

    public Class<? extends T> getDefaultSubType()
    {
        return defaultSubType;
    }

    public List<Pair<Class<? extends T>, String>> getSubTypes()
    {
        return subTypes;
    }

    public SimpleModule registerDefaultSubType()
    {
        if (this.defaultSubType == null)
        {
            throw new RuntimeException("Default sub type has not been set");
        }
        return new SimpleModule().addAbstractTypeMapping(this.superType, this.defaultSubType);
    }

    public static class Builder<T>
    {
        private final Class<T> superType;
        private Class<? extends T> defaultSubType;
        private List<Pair<Class<? extends T>, String>> subTypes = new ArrayList<>();

        private Builder(Class<T> superType)
        {
            this.superType = superType;
        }

        public static <T> Builder<T> newInstance(Class<T> superType)
        {
            return new Builder<>(superType);
        }

        public ProtocolSubTypeInfo<T> build()
        {
            return new ProtocolSubTypeInfo<>(this);
        }

        public Builder<T> withDefaultSubType(Class<? extends T> subType)
        {
            this.defaultSubType = subType;
            return this;
        }

        public Builder<T> withSubtypes(List<Pair<Class<? extends T>, String>> subTypes)
        {
            this.subTypes = subTypes;
            return this;
        }
    }
}
