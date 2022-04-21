package org.finos.legend.engine.language.pure.compiler.toPureGraph;


import java.util.List;
import java.util.Objects;

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.function.Function3;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;

public interface IAwsCompilerExtension extends CompilerExtension {

    static <T, U> U process(T item, List<Function2<T, CompileContext, U>> processors, CompileContext context, String type, SourceInformation srcInfo) {
        return ListIterate
                .collect(processors, processor -> processor.value(item, context))
                .select(Objects::nonNull)
                .getFirstOptional()
                .orElseThrow(() -> new EngineException("Unsupported " + type + " type '" + item.getClass() + "'", srcInfo, EngineErrorType.COMPILATION));
    }

    static <T, U, V> U process(T item, List<Function3<T, CompileContext, V, U>> processors, CompileContext context, V parameter, String type, SourceInformation srcInfo) {
        return ListIterate
                .collect(processors, processor -> processor.value(item, context, parameter))
                .select(Objects::nonNull)
                .getFirstOptional()
                .orElseThrow(() -> new EngineException("Unsupported " + type + " type '" + item.getClass() + "'" , srcInfo, EngineErrorType.COMPILATION));
    }
}