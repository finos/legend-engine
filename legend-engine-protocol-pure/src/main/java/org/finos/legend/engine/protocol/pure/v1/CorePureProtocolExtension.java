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

package org.finos.legend.engine.protocol.pure.v1;

import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.protocol.pure.v1.extension.ProtocolSubTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.extension.PureProtocolExtension;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.PackageableConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Association;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Class;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Enumeration;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Function;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Measure;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Profile;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Unit;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.PackageableRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.EngineRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.LegacyRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.Runtime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.RuntimePointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.SectionIndex;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.text.Text;

import java.util.List;
import java.util.Map;

public class CorePureProtocolExtension implements PureProtocolExtension
{
    @Override
    public List<Function0<List<ProtocolSubTypeInfo<?>>>> getExtraProtocolSubTypeInfoCollectors()
    {
        return Lists.mutable.with(() -> Lists.mutable.with(
                ProtocolSubTypeInfo.Builder
                        .newInstance(PackageableElement.class)
                        .withSubtypes(FastList.newListWith(
                                Tuples.pair(SectionIndex.class, "sectionIndex"),
                                // Domain
                                Tuples.pair(Profile.class, "profile"),
                                Tuples.pair(Enumeration.class, "Enumeration"),
                                Tuples.pair(Class.class, "class"),
                                Tuples.pair(Association.class, "association"),
                                Tuples.pair(Function.class, "function"),
                                Tuples.pair(Measure.class, "measure"),
                                Tuples.pair(Unit.class, "unit"),
                                Tuples.pair(Text.class, "text")
                        ))
                        .build(),
                // Runtime
                ProtocolSubTypeInfo.Builder
                        .newInstance(Runtime.class)
                        .withDefaultSubType(LegacyRuntime.class)
                        .withSubtypes(FastList.newListWith(
                                Tuples.pair(LegacyRuntime.class, "legacyRuntime"),
                                Tuples.pair(EngineRuntime.class, "engineRuntime"),
                                Tuples.pair(RuntimePointer.class, "runtimePointer")
                        ))
                        .build()
        ));
    }

    @Override
    public Map<java.lang.Class<? extends PackageableElement>, String> getExtraProtocolToClassifierPathMap()
    {
        return  Maps.mutable.<java.lang.Class<? extends PackageableElement>, String>ofInitialCapacity(11)
                 .withKeyValue(Association.class, "meta::pure::metamodel::relationship::Association")
                .withKeyValue(Class.class, "meta::pure::metamodel::type::Class")
                .withKeyValue(Enumeration.class, "meta::pure::metamodel::type::Enumeration")
                .withKeyValue(Mapping.class, "meta::pure::mapping::Mapping")
                .withKeyValue(Function.class, "meta::pure::metamodel::function::ConcreteFunctionDefinition")
                .withKeyValue(Measure.class, "meta::pure::metamodel::type::Measure")
                .withKeyValue(PackageableConnection.class, "meta::pure::runtime::PackageableConnection")
                .withKeyValue(PackageableRuntime.class, "meta::pure::runtime::PackageableRuntime")
                .withKeyValue(Profile.class, "meta::pure::metamodel::extension::Profile")
                .withKeyValue(SectionIndex.class, "meta::pure::metamodel::section::SectionIndex")
                .withKeyValue(Unit.class, "meta::pure::metamodel::type::Unit")
                .withKeyValue(Text.class, "meta::pure::metamodel::text::Text");
    }

}
