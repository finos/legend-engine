package org.finos.legend.engine.language.pure.dsl.persistence.compiler.toPureGraph;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.PersistencePipe;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Service;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_PersistencePipe_Impl;

import java.util.Collections;

public class PersistenceCompilerExtension implements CompilerExtension {
    @Override
    public Iterable<? extends Processor<?>> getExtraProcessors() {
        return Collections.singletonList(Processor.newProcessor(
                PersistencePipe.class,
                Lists.fixedSize.with(Service.class),
                (persistencePipe, context) ->
                        new Root_meta_pure_persistence_metamodel_PersistencePipe_Impl("")
                                ._documentation(persistencePipe.documentation)
                                ._ownersAddAll(Lists.immutable.ofAll(persistencePipe.owners))
                                ._trigger(HelperPersistenceBuilder.buildEventType(persistencePipe.trigger))
                                ._reader(HelperPersistenceBuilder.buildReader(persistencePipe.reader, context))
                                ._persister(HelperPersistenceBuilder.buildPersister(persistencePipe.persister, context))
        ));
    }
}
