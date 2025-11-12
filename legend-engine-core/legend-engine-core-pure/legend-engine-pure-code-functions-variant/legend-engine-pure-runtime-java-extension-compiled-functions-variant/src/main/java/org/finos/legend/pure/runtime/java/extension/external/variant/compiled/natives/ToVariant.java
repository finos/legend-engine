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

package org.finos.legend.pure.runtime.java.extension.external.variant.compiled.natives;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.List;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNativeFunctionGeneric;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.CompiledSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap;
import org.finos.legend.pure.runtime.java.extension.external.variant.VariantInstanceImpl;

public class ToVariant extends AbstractNativeFunctionGeneric
{
    public ToVariant()
    {
        super(ToVariant.class.getCanonicalName() + ".toVariant", new Class[]{Object.class, ExecutionSupport.class}, false, true, false, "toVariant_Any_MANY__Variant_1_");
    }

    public static VariantInstanceImpl toVariant(Object object, ExecutionSupport es)
    {
        JsonNode node;

        if (CompiledSupport.isEmpty(object))
        {
            node = VariantInstanceImpl.OBJECT_MAPPER.nullNode();
        }
        else
        {
            node = toJson(object);
        }
        return VariantInstanceImpl.newVariant(node, ((CompiledExecutionSupport) es).getProcessorSupport());
    }

    private static JsonNode toJson(Object value)
    {
        if (value == null)
        {
            return VariantInstanceImpl.OBJECT_MAPPER.nullNode();
        }
        if (value instanceof VariantInstanceImpl)
        {
            return ((VariantInstanceImpl) value).getJsonNode();
        }
        else if (CompiledSupport.safeSize(value) > 1)
        {
            ArrayNode node = VariantInstanceImpl.OBJECT_MAPPER.createArrayNode();
            CompiledSupport.toPureCollection(value).asLazy().collect(ToVariant::toJson).forEach(node::add);
            return node;
        }
        else if (value instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.List)
        {
            ArrayNode node = VariantInstanceImpl.OBJECT_MAPPER.createArrayNode();
            ((List<?>) value)._values().asLazy().collect(ToVariant::toJson).forEach(node::add);
            return node;
        }
        else if (value instanceof PureMap)
        {
            ObjectNode node = VariantInstanceImpl.OBJECT_MAPPER.createObjectNode();
            ((MutableMap<String, Object>) ((PureMap) value).getMap())
                    .keyValuesView()
                    .collect(x -> Tuples.pair(x.getOne(), toJson(x.getTwo())))
                    .toSortedListBy(Pair::getOne)
                    .forEach(x -> node.set(x.getOne(), x.getTwo()));
            return node;
        }
        else // primitive
        {
            return VariantInstanceImpl.OBJECT_MAPPER.valueToTree(value);
        }
    }
}
