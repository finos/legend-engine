package org.finos.legend.engine.language.pure.org.finos.legend.engine.language.pure.dsl.service.test.executionoption;

import org.eclipse.collections.api.block.function.Function2;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.protocol.pure.v1.model.executionOption.ExecutionOption;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_ExecutionOption;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_tests_DummyExecutionOption_Impl;

import java.util.Collections;
import java.util.List;

public class DummyCompilerExtension implements CompilerExtension
{
    @Override
    public Iterable<? extends Processor<?>> getExtraProcessors()
    {
        return Collections.emptyList();
    }

    @Override
    public List<Function2<ExecutionOption, CompileContext, Root_meta_pure_executionPlan_ExecutionOption>> getExtraExecutionOptionProcessors()
    {
        return Collections.singletonList(this::processExecOption);
    }

    private Root_meta_pure_executionPlan_ExecutionOption processExecOption(ExecutionOption executionOption, CompileContext context)
    {
        if (executionOption instanceof DummyExecOption)
        {
            return new Root_meta_pure_executionPlan_tests_DummyExecutionOption_Impl("");
        }
        else
        {
            return null;
        }
    }
}
