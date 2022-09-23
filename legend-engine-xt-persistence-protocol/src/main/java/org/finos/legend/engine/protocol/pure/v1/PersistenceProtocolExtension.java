// Copyright 2022 Goldman Sachs
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
import org.finos.legend.engine.protocol.pure.v1.extension.ProtocolSubTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.extension.PureProtocolExtension;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.Persistence;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.PersistenceContext;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.context.PersistencePlatform;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.context.DefaultPersistencePlatform;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.test.PersistenceTest;
import org.finos.legend.engine.protocol.pure.v1.model.test.AtomicTest;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.trigger.CronTrigger;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.trigger.ManualTrigger;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.trigger.Trigger;

import java.util.List;
import java.util.Map;

public class PersistenceProtocolExtension implements PureProtocolExtension
{
    public static final String PERSISTENCE_CLASSIFIER_PATH = "meta::pure::persistence::metamodel::Persistence";
    public static final String PERSISTENCE_CONTEXT_CLASSIFIER_PATH = "meta::pure::persistence::metamodel::PersistenceContext";

    @Override
    public List<Function0<List<ProtocolSubTypeInfo<?>>>> getExtraProtocolSubTypeInfoCollectors()
    {
        return Lists.fixedSize.of(() -> Lists.fixedSize.of(
                // Packageable element
                ProtocolSubTypeInfo.newBuilder(PackageableElement.class)
                        .withSubtype(Persistence.class, "persistence")
                        .withSubtype(PersistenceContext.class, "persistenceContext")
                        .build(),

                // Persistence platform
                ProtocolSubTypeInfo.newBuilder(PersistencePlatform.class)
                        .withSubtype(DefaultPersistencePlatform.class, "default")
                        .build(),

                // Trigger
                ProtocolSubTypeInfo.newBuilder(Trigger.class)
                        .withSubtype(ManualTrigger.class, "manualTrigger")
                        .withSubtype(CronTrigger.class, "cronTrigger")
                        .build(),

                // Persistence test
                ProtocolSubTypeInfo.newBuilder(AtomicTest.class)
                        .withSubtype(PersistenceTest.class, "test")
                        .build()
        ));
    }

    @Override
    public Map<Class<? extends PackageableElement>, String> getExtraProtocolToClassifierPathMap()
    {
        return Maps.mutable.with(
                Persistence.class, PERSISTENCE_CLASSIFIER_PATH,
                PersistenceContext.class, PERSISTENCE_CONTEXT_CLASSIFIER_PATH
        );
    }
}
