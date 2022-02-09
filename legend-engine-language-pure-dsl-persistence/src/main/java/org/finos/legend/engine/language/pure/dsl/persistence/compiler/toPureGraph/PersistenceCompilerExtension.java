package org.finos.legend.engine.language.pure.dsl.persistence.compiler.toPureGraph;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.PersistencePipe;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Service;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_PersistencePipe;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_PersistencePipe_Impl;

import java.util.Collections;

public class PersistenceCompilerExtension implements CompilerExtension
{
    @Override
    public Iterable<? extends Processor<?>> getExtraProcessors()
    {
        return Collections.singletonList(Processor.newProcessor(
                PersistencePipe.class,
                Lists.fixedSize.with(Service.class, Mapping.class),
                (pipe, context) -> new Root_meta_pure_persistence_metamodel_PersistencePipe_Impl("")
                        ._documentation(pipe.documentation)
                        ._ownersAddAll(Lists.immutable.ofAll(pipe.owners))
                        ._trigger(HelperPersistenceBuilder.buildTrigger(pipe.trigger))
                        ._reader(HelperPersistenceBuilder.buildReader(pipe.reader, context)),
                (pipe, context) ->
                {
                    Root_meta_pure_persistence_metamodel_PersistencePipe purePipe = (Root_meta_pure_persistence_metamodel_PersistencePipe) context.pureModel.getOrCreatePackage(pipe._package)._children().detect(c -> pipe.name.equals(c._name()));
                    purePipe._persister(HelperPersistenceBuilder.buildPersister(pipe.persister, context));
                }
        ));
    }
}
