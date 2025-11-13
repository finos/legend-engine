// Copyright 2025 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.extension.external.variant.interpreted.natives;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.stack.MutableStack;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.extension.external.variant.VariantInstanceImpl;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;

public class To extends AbstractTo
{
    public To(FunctionExecutionInterpreted exec, ModelRepository repository)
    {
        super(exec, repository);
    }

    @Override
    Iterable<? extends CoreInstance> toCoreInstances(VariantInstanceImpl variantCoreInstance, CoreInstance targetGenericType, MutableStack<CoreInstance> functionExpressionCallStack, ExecutionSupport executionSupport, ProcessorSupport processorSupport)
    {
        return Lists.fixedSize.of(this.toCoreInstance(variantCoreInstance.getJsonNode(), targetGenericType, functionExpressionCallStack, executionSupport, processorSupport));
    }
}
