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

import com.fasterxml.jackson.databind.JsonNode;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.extension.external.variant.VariantInstanceImpl;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;

public class ToMany extends AbstractTo
{
    public ToMany(FunctionExecutionInterpreted exec, ModelRepository repository)
    {
        super(repository);
    }

    @Override
    Iterable<? extends CoreInstance> toCoreInstances(VariantInstanceImpl variantCoreInstance, CoreInstance targetGenericType, MutableStack<CoreInstance> functionExpressionCallStack, ProcessorSupport processorSupport)
    {
        JsonNode jsonNode = variantCoreInstance.getJsonNode();
        if (jsonNode.isArray())
        {
            return Iterate.collect(jsonNode, x -> toCoreInstance(x, targetGenericType, functionExpressionCallStack, processorSupport));
        }
        else
        {
            throw new PureExecutionException(functionExpressionCallStack.peek().getSourceInformation(), "Expect variant that contains an 'ARRAY', but got '" + jsonNode.getNodeType() + "'", functionExpressionCallStack);
        }
    }
}
