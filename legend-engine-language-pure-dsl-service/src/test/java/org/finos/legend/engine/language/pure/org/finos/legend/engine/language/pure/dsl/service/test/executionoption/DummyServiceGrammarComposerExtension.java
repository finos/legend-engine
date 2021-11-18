package org.finos.legend.engine.language.pure.org.finos.legend.engine.language.pure.dsl.service.test.executionoption;

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.language.pure.dsl.service.grammar.to.IServiceGrammarComposerExtension;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.protocol.pure.v1.model.executionOption.ExecutionOption;

import java.util.List;

public class DummyServiceGrammarComposerExtension implements IServiceGrammarComposerExtension
{
    @Override
    public List<Function2<ExecutionOption, PureGrammarComposerContext, String>> getExtraExecutionOptionComposers()
    {
        return Lists.mutable.of(
                (execOpt, context) ->{
                    if (execOpt instanceof DummyExecOption)
                    {
                        return context.getIndentationString() + "dummyExecOption";
                    }
                    else if(execOpt instanceof DummyExecOptionWithParameters)
                    {
                        return context.getIndentationString() + ((DummyExecOptionWithParameters) execOpt).code.trim();
                    }
                    else
                    {
                        return null;
                    }
                }
        );
    }
}
