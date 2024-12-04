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

package org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.stack.MutableStack;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.shared.Shared;
import org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.shared.TDSCoreInstance;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.TestTDS;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

public class Write extends Shared
{
    public Write(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
        super(functionExecution, repository);
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, MutableStack<CoreInstance> functionExpressionCallStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        CoreInstance returnGenericType = getReturnGenericType(resolvedTypeParameters, resolvedMultiplicityParameters, functionExpressionCallStack, processorSupport);
        TestTDS sourceTds = getTDS(params, 0, processorSupport);
        CoreInstance coreInstance = params.get(1).getValueForMetaPropertyToOne("values");
        if (!Instance.instanceOf(coreInstance, "meta::pure::metamodel::relation::TDSRelationAccessor", processorSupport))
        {
            throw new RuntimeException("Only source element of type meta::pure::metamodel::relation::TDSRelationAccessor is supported");
        }
        TestTDS targetTds = getTDS(coreInstance, processorSupport);
        TDSCoreInstance resultTds = new TDSCoreInstance(targetTds.concatenate(sourceTds), returnGenericType, repository, processorSupport);
        Instance.setValueForProperty(coreInstance, "sourceElement", resultTds, processorSupport);
        return ValueSpecificationBootstrap.wrapValueSpecification(this.repository.newIntegerCoreInstance(sourceTds.getRowCount()), true, processorSupport);
    }
}
