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
//

package org.finos.legend.pure.runtime.java.extension.external.variant.interpreted.natives;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Stack;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.*;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.extension.external.variant.VariantInstanceImpl;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.MapCoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

public class ToVariant extends NativeFunction
{
    public ToVariant(FunctionExecutionInterpreted exec, ModelRepository repository)
    {

    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, MutableStack<CoreInstance> functionExpressionCallStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        ListIterable<? extends CoreInstance> values = Instance.getValueForMetaPropertyToManyResolved(params.get(0), M3Properties.values, processorSupport);

        VariantInstanceImpl variantInstance;

        if (values.isEmpty())
        {
            variantInstance = VariantInstanceImpl.newVariant(VariantInstanceImpl.OBJECT_MAPPER.nullNode(), functionExpressionCallStack.peek().getSourceInformation(), processorSupport);
        }
        else if (values.size() == 1)
        {
            JsonNode json = this.coreInstanceToJson(values.get(0), processorSupport);
            variantInstance = VariantInstanceImpl.newVariant(json, functionExpressionCallStack.peek().getSourceInformation(), processorSupport);
        }
        else
        {
            ArrayNode arrayNode = VariantInstanceImpl.OBJECT_MAPPER.createArrayNode();
            values.forEach(x -> arrayNode.add(this.coreInstanceToJson(x, processorSupport)));
            variantInstance = VariantInstanceImpl.newVariant(arrayNode, functionExpressionCallStack.peek().getSourceInformation(), processorSupport);
        }

        return ValueSpecificationBootstrap.wrapValueSpecification(variantInstance, true, processorSupport);
    }

    private JsonNode coreInstanceToJson(CoreInstance valueCoreInstance, ProcessorSupport processorSupport)
    {
        if (valueCoreInstance == null)
        {
            return VariantInstanceImpl.OBJECT_MAPPER.nullNode();
        }
        else if (valueCoreInstance instanceof VariantInstanceImpl)
        {
            return ((VariantInstanceImpl) valueCoreInstance).getJsonNode();
        }
        else if (Instance.instanceOf(valueCoreInstance, M3Paths.List, processorSupport))
        {
            ListIterable<? extends CoreInstance> values = Instance.getValueForMetaPropertyToManyResolved(valueCoreInstance, M3Properties.values, processorSupport);
            ArrayNode arrayNode = VariantInstanceImpl.OBJECT_MAPPER.createArrayNode();
            values.forEach(x -> arrayNode.add(this.coreInstanceToJson(x, processorSupport)));
            return arrayNode;
        }
        else if (Instance.instanceOf(valueCoreInstance, M3Paths.Map, processorSupport))
        {
            ObjectNode objectNode = VariantInstanceImpl.OBJECT_MAPPER.createObjectNode();

            MapCoreInstance map = (MapCoreInstance) valueCoreInstance;
            map.getMap().keyValuesView()
                    .collect(x -> Tuples.pair(PrimitiveUtilities.getStringValue(x.getOne()), this.coreInstanceToJson(x.getTwo(), processorSupport)))
                    .toSortedListBy(Pair::getOne)
                    .forEach(x -> objectNode.set(x.getOne(), x.getTwo()));

            return objectNode;
        }
        else
        {
            if (Instance.instanceOf(valueCoreInstance, M3Paths.Integer, processorSupport))
            {
                return VariantInstanceImpl.OBJECT_MAPPER.getNodeFactory().numberNode(PrimitiveUtilities.getIntegerValue(valueCoreInstance).longValue());
            }
            if (Instance.instanceOf(valueCoreInstance, M3Paths.Float, processorSupport))
            {
                return VariantInstanceImpl.OBJECT_MAPPER.getNodeFactory().numberNode(PrimitiveUtilities.getFloatValue(valueCoreInstance));
            }
            if (Instance.instanceOf(valueCoreInstance, M3Paths.Boolean, processorSupport))
            {
                return VariantInstanceImpl.OBJECT_MAPPER.getNodeFactory().booleanNode(PrimitiveUtilities.getBooleanValue(valueCoreInstance));
            }
            if (Instance.instanceOf(valueCoreInstance, M3Paths.String, processorSupport))
            {
                return VariantInstanceImpl.OBJECT_MAPPER.getNodeFactory().textNode(PrimitiveUtilities.getStringValue(valueCoreInstance));
            }
            if (Instance.instanceOf(valueCoreInstance, M3Paths.Date, processorSupport))
            {
                return VariantInstanceImpl.OBJECT_MAPPER.getNodeFactory().textNode(PrimitiveUtilities.getDateValue(valueCoreInstance).toString());
            }
        }

        throw new UnsupportedOperationException(processorSupport.getClassifier(valueCoreInstance) + " - not supported!");
    }
}
