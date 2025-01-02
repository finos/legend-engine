// Copyright 2024 Goldman Sachs
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
//

package org.finos.legend.engine.pure.ide.interpreted.debug;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.CountDownLatch;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.serialization.runtime.IncrementalCompiler;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.transaction.framework.ThreadLocalTransactionContext;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;

public class DebugState
{
    private final CountDownLatch latch;

    private final MutableStack<CoreInstance> functionExpressionCallStack;

    private final FunctionExecutionInterpretedWithDebugSupport debugSupport;
    private final MutableList<Pair<String, CoreInstance>> variables;
    private final String variablesTypeAndMultiplicity;
    private volatile boolean abort;

    public DebugState(FunctionExecutionInterpretedWithDebugSupport debugSupport, VariableContext variableContext, MutableStack<CoreInstance> functionExpressionCallStack)
    {
        this.debugSupport = debugSupport;
        this.latch = new CountDownLatch(1);
        this.functionExpressionCallStack = functionExpressionCallStack;
        this.variables = computeVariables(variableContext);
        this.variablesTypeAndMultiplicity = computeVariablesTypeAndMultiplicity(this.debugSupport, this.variables);
        this.debugSupport.setDebugState(this);
    }

    public void release()
    {
        this.debugSupport.setDebugState(null);
        this.latch.countDown();
    }

    public void debug()
    {
        try
        {
            this.latch.await();
        }
        catch (Exception e)
        {
            // todo?
        }
    }

    public void abort()
    {
        this.abort = true;
        this.release();
    }

    public boolean aborted()
    {
        return this.abort;
    }

    public String getSummary()
    {
        PureExecutionException debugLocation = new PureExecutionException(this.functionExpressionCallStack.isEmpty() ? null : this.functionExpressionCallStack.peek().getSourceInformation(), "debug location", functionExpressionCallStack);
        StringBuilder appendable = new StringBuilder();
        debugLocation.printPureStackTrace(appendable, "", this.debugSupport.getProcessorSupport());
        return "Variables: " + this.variablesTypeAndMultiplicity + "\n\n" + appendable;
    }

    public String evaluate(String command)
    {
        Source inMemoryCodeBlock = this.debugSupport.getPureRuntime().createInMemoryCodeBlock("{" + variablesTypeAndMultiplicity + "|\n" + command + "\n}");

        IncrementalCompiler incrementalCompiler = this.debugSupport.getPureRuntime().getIncrementalCompiler();
        IncrementalCompiler.IncrementalCompilerTransaction transaction = incrementalCompiler.newTransaction(false);
        try (ThreadLocalTransactionContext ignore = transaction.openInCurrentThread())
        {
            incrementalCompiler.compileInCurrentTransaction(inMemoryCodeBlock);
        }

        ListIterable<CoreInstance> newInstances = inMemoryCodeBlock.getNewInstances();

        CoreInstance result = this.debugSupport.startRaw(newInstances.get(0), Lists.fixedSize.of());
        CoreInstance lambda = Instance.getValueForMetaPropertyToOneResolved(result, M3Properties.values, this.debugSupport.getProcessorSupport());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        this.debugSupport.start(lambda, variables.collect(Pair::getTwo), out, this.debugSupport.newOutputWriter());
        return out.toString();
    }

    private static String computeVariablesTypeAndMultiplicity(FunctionExecutionInterpretedWithDebugSupport debugSupport, MutableList<Pair<String, CoreInstance>> variables)
    {
        return variables.collect(x -> x.getOne() + ":" + computeVariableTypeAndMultiplicity(debugSupport, x.getTwo())).makeString(", ");
    }

    private static MutableList<Pair<String, CoreInstance>> computeVariables(VariableContext variableContext)
    {
        return variableContext.getVariableNames()
                .asLazy()
                .collect(x -> Tuples.pair(x, variableContext.getValue(x))).select(x -> x.getTwo() != null)
                .toList();
    }

    private static String computeVariableTypeAndMultiplicity(FunctionExecutionInterpretedWithDebugSupport debugSupport, CoreInstance coreInstance)
    {
        // todo the GenericType.print has a bug with type arguments, and functions get printed wrong!
        // ie. meta::pure::metamodel::function::ConcreteFunctionDefinition<<X> {meta::pure::metamodel::function::Function<{->X[o]}>[1]->X[o]}>
        String type;
        ProcessorSupport processorSupport = debugSupport.getProcessorSupport();
        if (processorSupport.type_subTypeOf(coreInstance.getValueForMetaPropertyToOne(M3Properties.genericType).getValueForMetaPropertyToOne(M3Properties.rawType), debugSupport.getPureRuntime().getCoreInstance(M3Paths.Function)))
        {
            type = "Function<Any>";
        }
        else
        {
            type = GenericType.print(coreInstance.getValueForMetaPropertyToOne(M3Properties.genericType), true, processorSupport);
        }
        String multiplicity = Multiplicity.print(coreInstance.getValueForMetaPropertyToOne(M3Properties.multiplicity));
        return type + multiplicity;
    }
}
