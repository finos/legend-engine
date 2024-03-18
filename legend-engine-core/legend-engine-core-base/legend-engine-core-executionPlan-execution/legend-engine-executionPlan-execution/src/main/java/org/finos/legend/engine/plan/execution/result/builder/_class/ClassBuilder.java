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

package org.finos.legend.engine.plan.execution.result.builder._class;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.plan.execution.nodes.helpers.ExecutionNodeClassResultHelper;
import org.finos.legend.engine.plan.execution.result.builder.Builder;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;

import java.util.List;

public class ClassBuilder extends Builder
{
    @JsonProperty(value = "class")
    public String _class;
    public String mapping;
    public List<ClassMappingInfo> classMappings;

    public ClassBuilder(ExecutionNode node)
    {
        this._type = "classBuilder";
        this._class = ExecutionNodeClassResultHelper.getClassNameFromClassResult(node);
        this.mapping = ExecutionNodeClassResultHelper.getMappingFromClassResult(node);
        this.classMappings = Lists.mutable.ofAll(ExecutionNodeClassResultHelper.getClassMappingInfoFromClassResult(node));
    }

    public List<Pair<String, String>> propertyTypes()
    {
        List<Pair<String, String>> propertyTypes = FastList.newList();
        List<String> columns = FastList.newList();

        for (ClassMappingInfo classInfo : this.classMappings)
        {
            for (PropertyInfo propInfo : classInfo.properties)
            {
                if (!columns.contains(propInfo.property))
                {
                    columns.add(propInfo.property);
                    propertyTypes.add(Tuples.pair(propInfo.property, propInfo.type));
                }
            }
        }
        return propertyTypes;
    }
}
