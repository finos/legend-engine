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

package org.finos.legend.engine.plan.execution.nodes.helpers;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.plan.execution.result.builder._class.ClassMappingInfo;
import org.finos.legend.engine.plan.execution.result.builder._class.PropertyInfo;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.result.ClassResultType;

import java.util.List;
import java.util.Map;

public class ExecutionNodeClassResultHelper
{
    @JsonIgnore
    public static boolean isClassResult(ExecutionNode executionNode)
    {
        return executionNode.resultType instanceof ClassResultType;
    }

    @JsonIgnore
    public static String getMappingFromClassResult(ExecutionNode executionNode)
    {
        return ((ClassResultType) executionNode.resultType).setImplementations.get(0).mapping;
    }

    @JsonIgnore
    public static String getClassNameFromClassResult(ExecutionNode executionNode)
    {
        return ((ClassResultType) executionNode.resultType)._class;
    }

    @JsonIgnore
    public static MutableList<? extends ClassMappingInfo> getClassMappingInfoFromClassResult(ExecutionNode executionNode)
    {
        ClassResultType crt = (ClassResultType) executionNode.resultType;
        return ListIterate.collect(crt.setImplementations, s ->
        {
            ClassMappingInfo info = new ClassMappingInfo(s._class, s.id);
            info.properties = s.propertyMappings != null ?
                    ListIterate.collect(s.propertyMappings, p -> new PropertyInfo(p.property, p.type)) :
                    FastList.newList();
            return info;
        }).toList();
    }

    @JsonIgnore
    public static boolean isClassPropertyEnum(ExecutionNode executionNode, String setImplementId, String name)
    {
        ClassResultType classResultType = (ClassResultType) executionNode.resultType;
        return !ListIterate.select(ListIterate.select(classResultType.setImplementations, s -> s.id.equals(setImplementId)).getFirst().propertyMappings, p -> p.property.equals(name)).getFirst().enumMapping.isEmpty();
    }

    @JsonIgnore
    public static Function<Object, Object> getClassEnumTransformer(ExecutionNode executionNode, String setImplementId, String name)
    {
        ClassResultType classResultType = (ClassResultType) executionNode.resultType;
        Map<String, List<String>> enumMapping = ListIterate.select(ListIterate.select(classResultType.setImplementations, s -> s.id.equals(setImplementId)).getFirst().propertyMappings, p -> p.property.equals(name)).getFirst().enumMapping;
        return ExecutionNodeResultHelper.buildReverseEnumFunc(executionNode, enumMapping);
    }
}
