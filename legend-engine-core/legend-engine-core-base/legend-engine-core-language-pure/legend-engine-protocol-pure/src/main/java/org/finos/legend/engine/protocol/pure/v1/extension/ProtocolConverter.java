// Copyright 2024 Goldman Sachs
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
//

package org.finos.legend.engine.protocol.pure.v1.extension;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;

public abstract class ProtocolConverter<T>
{
    private ProtocolConverter()
    {

    }

    public abstract T convert(T value);

    public abstract Class<T> getType();

    public static <T> ProtocolConverter<T> converter(Class<T> type, Function<T, T> func)
    {
        return new DefaultProtocolConverter<>(type, func);
    }

    public static <T> ProtocolConverter<T> merge(ProtocolConverter<T> one, ProtocolConverter<T> two)
    {
        return new MergedProtocolConverter<>(Objects.requireNonNull(one), Objects.requireNonNull(two));
    }

    private static class MergedProtocolConverter<T> extends ProtocolConverter<T>
    {
        private final List<DefaultProtocolConverter<T>> converters = Lists.mutable.empty();

        public MergedProtocolConverter(ProtocolConverter<T> one, ProtocolConverter<T> two)
        {
            if (one instanceof MergedProtocolConverter)
            {
                this.converters.addAll(((MergedProtocolConverter<T>) one).converters);
            }
            else
            {
                this.converters.add((DefaultProtocolConverter<T>) one);
            }

            if (two instanceof MergedProtocolConverter)
            {
                this.converters.addAll(((MergedProtocolConverter<T>) two).converters);
            }
            else
            {
                this.converters.add((DefaultProtocolConverter<T>) two);
            }
        }

        @Override
        public T convert(T value)
        {
            return ListIterate.injectInto(value, this.converters, (v, c) -> c.convert(v));
        }

        @Override
        public Class<T> getType()
        {
            return this.converters.get(0).type;
        }
    }

    private static class DefaultProtocolConverter<T> extends ProtocolConverter<T>
    {
        private final Class<T> type;
        private final Function<T, T> converter;

        public DefaultProtocolConverter(Class<T> type, Function<T, T> converter)
        {
            this.type = type;
            this.converter = converter;
        }

        @Override
        public T convert(T value)
        {
            return this.converter.apply(value);
        }

        @Override
        public Class<T> getType()
        {
            return this.type;
        }
    }
}
