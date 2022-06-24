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

package org.finos.legend.engine.language.pure.dsl.persistence.compiler.toPureGraph;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.PackageableConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.Persistence;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.PersistenceContext;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Service;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.Database;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.external.shared.Binding;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_Persistence;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_PersistenceContext;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_PersistenceContext_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_Persistence_Impl;

public class PersistenceCompilerExtension implements CompilerExtension
{
    @Override
    public Iterable<? extends Processor<?>> getExtraProcessors()
    {
        return Lists.fixedSize.of(
                Processor.newProcessor(
                        Persistence.class,
                        Lists.fixedSize.of(Service.class, Mapping.class, Binding.class, PackageableConnection.class, Database.class),
                        (persistence, context) -> new Root_meta_pure_persistence_metamodel_Persistence_Impl("")
                                ._documentation(persistence.documentation)
                                ._trigger(HelperPersistenceBuilder.buildTrigger(persistence.trigger)),
                        (persistence, context) ->
                        {
                            Root_meta_pure_persistence_metamodel_Persistence purePersistence = (Root_meta_pure_persistence_metamodel_Persistence) context.pureModel.getOrCreatePackage(persistence._package)._children().detect(c -> persistence.name.equals(c._name()));
                            purePersistence._service(HelperPersistenceBuilder.buildService(persistence, context));
                            purePersistence._persister(HelperPersistenceBuilder.buildPersister(persistence.persister, context));
                            purePersistence._notifier(HelperPersistenceBuilder.buildNotifier(persistence.notifier, context));
                        }
                ),
                Processor.newProcessor(
                        PersistenceContext.class,
                        Lists.fixedSize.of(Persistence.class, PackageableConnection.class),
                        (persistenceContext, context) -> new Root_meta_pure_persistence_metamodel_PersistenceContext_Impl(""),
                        (persistenceContext, context) ->
                        {
                            Root_meta_pure_persistence_metamodel_PersistenceContext purePersistenceContext = (Root_meta_pure_persistence_metamodel_PersistenceContext) context.pureModel.getOrCreatePackage(persistenceContext._package)._children().detect(c -> persistenceContext.name.equals(c._name()));
                            purePersistenceContext._persistence(HelperPersistenceBuilder.buildPersistence(persistenceContext, context));
                            purePersistenceContext._serviceParameters(ListIterate.collect(persistenceContext.serviceParameters, sp -> HelperPersistenceBuilder.buildServiceParameter(sp, context)));
                            purePersistenceContext._sinkConnection(HelperPersistenceBuilder.buildConnection(persistenceContext.sinkConnection, context));
                        }
                ));
    }
}
