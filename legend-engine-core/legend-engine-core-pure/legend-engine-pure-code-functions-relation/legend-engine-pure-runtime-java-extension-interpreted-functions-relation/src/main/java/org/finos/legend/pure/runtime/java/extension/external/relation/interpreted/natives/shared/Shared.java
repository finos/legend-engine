// Copyright 2023 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.shared;

import java.util.Stack;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.stack.MutableStack;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.TestTDS;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;

public abstract class Shared extends NativeFunction
{
    protected final ModelRepository repository;
    protected final FunctionExecutionInterpreted functionExecution;

    public Shared(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
        this.repository = repository;
        this.functionExecution = functionExecution;
    }

    public TestTDS getTDS(ListIterable<? extends CoreInstance> params, int position, ProcessorSupport processorSupport)
    {
        return getTDS(params.get(position).getValueForMetaPropertyToOne("values"), processorSupport);
    }

    public TestTDS getTDS(CoreInstance value, ProcessorSupport processorSupport)
    {
        if (Instance.instanceOf(value, "meta::pure::metamodel::relation::TDSRelationAccessor", processorSupport))
        {
            return getTDS(value.getValueForMetaPropertyToOne("sourceElement"), processorSupport);
        }
        return value instanceof TDSCoreInstance ?
                ((TDSCoreInstance) value).getTDS() :
                new TestTDSInterpreted(TestTDS.readCsv((value.getValueForMetaPropertyToOne("csv")).getName()), repository, processorSupport);
    }

    public RelationType<?> getRelationType(ListIterable<? extends CoreInstance> params, int i)
    {
        return (RelationType<?>) params.get(i).getValueForMetaPropertyToOne("genericType").getValueForMetaPropertyToMany("typeArguments").getFirst().getValueForMetaPropertyToOne("rawType");
    }

    public static CoreInstance getReturnGenericType(Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, MutableStack<CoreInstance> functionExpressionCallStack, ProcessorSupport processorSupport)
    {
        CoreInstance result = functionExpressionCallStack.peek().getValueForMetaPropertyToOne("genericType");

        for (int i = resolvedTypeParameters.size() - 1; i >= 0; i--)
        {
            MutableMap<String, CoreInstance> resolvedTypeParameter = resolvedTypeParameters.elementAt(i);
            MutableMap<String, CoreInstance> resolvedMultiplicityParameter = resolvedMultiplicityParameters.elementAt(i);
            if (resolvedTypeParameter.notEmpty() || resolvedMultiplicityParameter.notEmpty())
            {
                result = GenericType.makeTypeArgumentAsConcreteAsPossible(result, resolvedTypeParameter, resolvedMultiplicityParameter, processorSupport);
                if (GenericType.isGenericTypeFullyConcrete(result, processorSupport))
                {
                    return result;
                }
            }
        }

        return result;
    }
}