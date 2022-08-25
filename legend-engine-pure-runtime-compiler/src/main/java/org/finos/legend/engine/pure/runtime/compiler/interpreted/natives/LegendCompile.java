// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.pure.runtime.compiler.interpreted.natives;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.metadata.Metadata;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.core.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.core.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

public class LegendCompile extends NativeFunction
{
    private final ModelRepository repository;
    private final FunctionExecutionInterpreted functionExecution;

    public LegendCompile(FunctionExecutionInterpreted functionExecution, ModelRepository modelRepository)
    {
        this.repository = modelRepository;
        this.functionExecution = functionExecution;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        String code = Instance.getValueForMetaPropertyToOneResolved(params.get(0), M3Properties.values, processorSupport).getName();
        MutableList<PackageableElement> createdElements = org.finos.legend.engine.pure.runtime.compiler.shared.LegendCompile.doCompile(code, new org.finos.legend.engine.pure.runtime.compiler.interpreted.natives.LegendCompile.InterpretedMetadata(processorSupport));
        return ValueSpecificationBootstrap.wrapValueSpecification(createdElements, true, functionExecution.getProcessorSupport());
    }


    private static class InterpretedMetadata implements Metadata
    {
        private ProcessorSupport processorSupport;

        public InterpretedMetadata(ProcessorSupport processorSupport)
        {
            this.processorSupport = processorSupport;
        }

        @Override
        public void startTransaction()
        {

        }

        @Override
        public void commitTransaction()
        {

        }

        @Override
        public void rollbackTransaction()
        {

        }

        @Override
        public CoreInstance getMetadata(String s, String s1)
        {
            if (s1.startsWith("Root::"))
            {
                s1 = s1.substring(6);
            }
            return _Package.getByUserPath(s1, processorSupport);
        }

        @Override
        public MapIterable<String, CoreInstance> getMetadata(String s)
        {
            throw new RuntimeException("Not supported");
        }

        @Override
        public CoreInstance getEnum(String s, String s1)
        {
            throw new RuntimeException("Not supported");
        }
    }
}
