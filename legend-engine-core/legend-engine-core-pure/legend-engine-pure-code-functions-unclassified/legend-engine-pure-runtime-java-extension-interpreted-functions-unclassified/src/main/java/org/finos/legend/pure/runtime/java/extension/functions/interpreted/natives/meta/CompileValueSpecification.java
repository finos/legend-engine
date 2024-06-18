// Copyright 2020 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.meta;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;
import org.finos.legend.pure.m3.serialization.runtime.IncrementalCompiler.IncrementalCompilerTransaction;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.transaction.framework.ThreadLocalTransactionContext;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.meta.source.SourceInformation;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

public class CompileValueSpecification extends NativeFunction
{
    private static final String COMPILATION_RESULT_CLASS = "meta::pure::functions::meta::CompilationResult";
    private static final String COMPILATION_FAILURE_CLASS = "meta::pure::functions::meta::CompilationFailure";

    private final PureRuntime runtime;

    public CompileValueSpecification(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
        this.runtime = functionExecution.getRuntime();
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport)
    {
        ListIterable<? extends CoreInstance> codeBlocks = Instance.getValueForMetaPropertyToManyResolved(params.get(0), M3Properties.values, processorSupport);
        MutableList<CoreInstance> results = FastList.newList(codeBlocks.size());

        for (CoreInstance codeBlock : codeBlocks)
        {
            String code = codeBlock.getName();

            CoreInstance functionInstance = null;
            CoreInstance expression = null;
            String failureMessage = null;
            org.finos.legend.pure.m4.coreinstance.SourceInformation failureSourceInfo = null;
            IncrementalCompilerTransaction transaction = this.runtime.getIncrementalCompiler().newTransaction(false);
            try (ThreadLocalTransactionContext ignore = transaction.openInCurrentThread())
            {
                Source source = this.runtime.createInMemoryCodeBlock(code);
                this.runtime.getIncrementalCompiler().compileInCurrentTransaction(source);
                functionInstance = source.getNewInstances().getFirst();
                expression = functionInstance.getValueForMetaPropertyToOne(M3Properties.expressionSequence);
            }
            catch (PureCompilationException | PureParserException ex)
            {
                failureMessage = ex.getOriginatingPureException().getInfo();
                failureSourceInfo = ex.getOriginatingPureException().getSourceInformation();
            }
            finally
            {
                transaction.rollback();
            }

            //Now build a result - THIS MUST BE DONE LAST, otherwise it will be rolled back along with the changes above
            CoreInstance result = this.runtime.getModelRepository().newEphemeralAnonymousCoreInstance(null, processorSupport.package_getByUserPath(COMPILATION_RESULT_CLASS));

            if (failureMessage != null)
            {
                CoreInstance failure = this.runtime.getModelRepository().newEphemeralAnonymousCoreInstance(null, processorSupport.package_getByUserPath(COMPILATION_FAILURE_CLASS));
                CoreInstance message = this.runtime.getModelRepository().newStringCoreInstance(failureMessage);
                Instance.addValueToProperty(failure, "message", message, processorSupport);

                if (failureSourceInfo != null)
                {
                    CoreInstance sourceInfoCoreInstance = SourceInformation.createSourceInfoCoreInstanceWithoutSourceId(this.runtime.getModelRepository(), processorSupport, failureSourceInfo);
                    Instance.addValueToProperty(failure, "sourceInformation", sourceInfoCoreInstance, processorSupport);
                }

                Instance.addValueToProperty(result, "failure", failure, processorSupport);
            }
            else if (expression != null)
            {
                Instance.addValueToProperty(result, "result", ValueSpecificationBootstrap.wrapValueSpecification(expression, false, processorSupport), processorSupport);
            }

            results.add(result);
        }

        return ValueSpecificationBootstrap.wrapValueSpecification(results, true, processorSupport);
    }
}
