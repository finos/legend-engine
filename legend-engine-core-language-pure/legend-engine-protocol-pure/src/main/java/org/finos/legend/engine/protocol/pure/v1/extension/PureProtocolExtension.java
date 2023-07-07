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

import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public interface PureProtocolExtension
{
    @Deprecated
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

}
