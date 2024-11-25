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

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.Converter;
import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;

public class ProtocolConverter<T> implements Converter<T, T>
{
    private final List<? extends Converter<T, T>> converters;

    public ProtocolConverter(Converter<T, T> converters)
    {
        this(Lists.fixedSize.of(converters));
    }

    public ProtocolConverter(List<? extends Converter<T, T>> converters)
    {
        this.converters = converters;
    }

    @Override
    public JavaType getInputType(TypeFactory typeFactory)
    {
        return this.converters.get(0).getInputType(typeFactory);
    }

    @Override
    public JavaType getOutputType(TypeFactory typeFactory)
    {
        return this.converters.get(0).getOutputType(typeFactory);
    }

    @Override
    public T convert(T value)
    {
        return ListIterate.injectInto(value, this.converters, (t, c) -> c.convert(t));
    }

    public static <T> ProtocolConverter<T> merge(List<ProtocolConverter<T>> converters)
    {
        List<? extends Converter<T, T>> allConverters = converters.stream().flatMap(x -> x.converters.stream()).collect(Collectors.toList());
        return new ProtocolConverter<>(allConverters);
    }
}
