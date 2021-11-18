package org.finos.legend.engine.language.pure.dsl.service.grammar.from;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.dsl.service.grammar.from.executionoption.ExecutionOptionSpecificationSourceCode;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtension;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.executionOption.ExecutionOption;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.function.Function;

public interface IServiceParserExtension extends PureGrammarParserExtension
{
    static List<IServiceParserExtension> getExtensions()
    {
        return Lists.mutable.withAll(ServiceLoader.load(IServiceParserExtension.class));
    }

    default List<Function<ExecutionOptionSpecificationSourceCode, ExecutionOption>> getExtraExecutionOptionParsers()
    {
        return Collections.emptyList();
    }

    static ExecutionOption process(ExecutionOptionSpecificationSourceCode code, List<Function<ExecutionOptionSpecificationSourceCode, ExecutionOption>> parsers) {
        return ListIterate
                .collect(parsers, parser -> parser.apply(code))
                .select(Objects::nonNull)
                .getFirstOptional()
                .orElseThrow(() -> new EngineException("Unsupported Execution Option type '" + code.getType() + "'", code.getSourceInformation(), EngineErrorType.PARSER));
    }
}
