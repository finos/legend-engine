package org.finos.legend.engine.language.pure.org.finos.legend.engine.language.pure.dsl.service.test.executionoption;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.language.pure.dsl.service.grammar.from.IServiceParserExtension;
import org.finos.legend.engine.language.pure.dsl.service.grammar.from.executionoption.ExecutionOptionSpecificationSourceCode;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.ServiceParserGrammar;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.executionOption.ExecutionOption;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Lexer;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser;

import java.util.List;
import java.util.function.Function;

public class DummyServiceParserExtension implements IServiceParserExtension
{
    @Override
    public List<Function<ExecutionOptionSpecificationSourceCode, ExecutionOption>> getExtraExecutionOptionParsers()
    {
        return Lists.mutable.of(
                DummyServiceParserExtension::parseDummyExecOption
        );
    }

    private static ExecutionOption parseDummyExecOption(ExecutionOptionSpecificationSourceCode code)
    {
        switch (code.getType())
        {
            case "failingDummyExecOption":
                M3Parser m3Parser = new M3Parser(new CommonTokenStream(new M3Lexer(CharStreams.fromString(code.getCode()))));
                SourceInformation sourceInformation = code.getWalkerSourceInformation().getSourceInformation(m3Parser.getCurrentToken());
                throw new EngineException("Error to check source info reported correctly from subparser", sourceInformation, EngineErrorType.PARSER);
            case "dummyExecOptionWithParam":
                return new DummyExecOptionWithParameters(code.getCode(), code.getSourceInformation());
            case "dummyExecOption":
                return new DummyExecOption(code.getSourceInformation());
            default:
                return null;
        }
    }
}
