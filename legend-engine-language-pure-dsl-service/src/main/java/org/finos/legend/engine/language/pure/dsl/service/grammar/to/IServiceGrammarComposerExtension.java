package org.finos.legend.engine.language.pure.dsl.service.grammar.to;

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.language.pure.grammar.to.extension.PureGrammarComposerExtension;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.executionOption.ExecutionOption;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.List;
import java.util.Objects;

public interface IServiceGrammarComposerExtension extends PureGrammarComposerExtension
{
    static List<IServiceGrammarComposerExtension> getExtensions(PureGrammarComposerContext context)
    {
        return ListIterate.selectInstancesOf(context.extensions, IServiceGrammarComposerExtension.class);
    }

    static String process(ExecutionOption executionOption, List<Function2<ExecutionOption, PureGrammarComposerContext, String>> processors, PureGrammarComposerContext context) {
        return ListIterate
                .collect(processors, processor -> processor.value(executionOption, context))
                .select(Objects::nonNull)
                .getFirstOptional()
                .orElseThrow(() -> new EngineException("Unsupported Execution Option type '" + executionOption.getClass() + "'", executionOption.sourceInformation, EngineErrorType.PARSER));
    }


    default List<Function2<ExecutionOption, PureGrammarComposerContext, String>> getExtraExecutionOptionComposers()
    {
        return FastList.newList();
    }
}
