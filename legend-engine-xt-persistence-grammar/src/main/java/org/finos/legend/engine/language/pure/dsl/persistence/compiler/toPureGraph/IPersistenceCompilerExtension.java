package org.finos.legend.engine.language.pure.dsl.persistence.compiler.toPureGraph;

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.PersistencePlatform;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_PersistencePlatform;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;

public interface IPersistenceCompilerExtension extends CompilerExtension
{
    static List<IPersistenceCompilerExtension> getExtensions()
    {
        return Lists.mutable.withAll(ServiceLoader.load(IPersistenceCompilerExtension.class));
    }

    static Root_meta_pure_persistence_metamodel_PersistencePlatform process(PersistencePlatform persistencePlatform, List<Function2<PersistencePlatform, CompileContext, Root_meta_pure_persistence_metamodel_PersistencePlatform>> processors, CompileContext context)
    {
        return process(persistencePlatform, processors, context, "Persistence Platform", persistencePlatform.sourceInformation);
    }

    static <T, U> U process(T item, List<Function2<T, CompileContext, U>> processors, CompileContext context, String type, SourceInformation srcInfo)
    {
        return ListIterate
                .collect(processors, processor -> processor.value(item, context))
                .select(Objects::nonNull)
                .getFirstOptional()
                .orElseThrow(() -> new EngineException("Unsupported " + type + " type '" + item.getClass() + "'", srcInfo, EngineErrorType.COMPILATION));
    }

    default List<Function2<PersistencePlatform, CompileContext, Root_meta_pure_persistence_metamodel_PersistencePlatform>> getExtraPersistencePlatformProcessors()
    {
        return Collections.emptyList();
    }
}
