package org.finos.legend.engine.language.pure.dsl.persistence.compiler.toPureGraph;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.PackageableConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.Persistence;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Service;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_Persistence;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_Persistence_Impl;

import java.util.Collections;

public class PersistenceCompilerExtension implements CompilerExtension
{
    @Override
    public Iterable<? extends Processor<?>> getExtraProcessors()
    {
        return Collections.singletonList(Processor.newProcessor(
                Persistence.class,
                Lists.fixedSize.with(Service.class, Mapping.class, PackageableConnection.class),
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
        ));
    }
}
