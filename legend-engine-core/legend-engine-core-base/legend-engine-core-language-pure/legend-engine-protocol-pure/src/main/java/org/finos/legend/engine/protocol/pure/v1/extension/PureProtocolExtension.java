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

import com.fasterxml.jackson.databind.JsonDeserializer;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.shared.core.extension.LegendLanguageExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public interface PureProtocolExtension extends LegendLanguageExtension
{
    @Override
    default String type()
    {
        return "Protocol";
    }

    default List<Function0<List<ProtocolSubTypeInfo<?>>>> getExtraProtocolSubTypeInfoCollectors()
    {
        return Collections.emptyList();
    }

    default Map<Class<? extends PackageableElement>, String> getExtraProtocolToClassifierPathMap()
    {
        return Collections.emptyMap();
    }

    default Map<String, Class> getExtraClassInstanceTypeMappings()
    {
        return Maps.mutable.empty();
    }

    default List<ProtocolConverter<?>> getProtocolConverters()
    {
        return Lists.fixedSize.empty();
    }

    default MutableList<Pair<Class, JsonDeserializer>> getExtraDeserializer()
    {
        return Lists.fixedSize.empty();
    }
}
