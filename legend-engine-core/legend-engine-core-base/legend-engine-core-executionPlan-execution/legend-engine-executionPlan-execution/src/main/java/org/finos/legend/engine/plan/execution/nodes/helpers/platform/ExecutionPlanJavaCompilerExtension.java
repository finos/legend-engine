// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.nodes.helpers.platform;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.shared.core.extension.LegendPlanExtension;
import org.finos.legend.engine.shared.javaCompiler.ClassPathFilter;
import java.util.Map;

public interface ExecutionPlanJavaCompilerExtension extends LegendPlanExtension
{
    default Map<String, Class<?>> dependencies()
    {
        return Maps.fixedSize.empty();
    }

    ClassPathFilter getExtraClassPathFilter();

    @Override
    default MutableList<String> typeGroup()
    {
        return Lists.mutable.with("Plan", "Generation", "Binding_Java");
    }
    
    @Override
    default String type()
    {
        return "Class_Path_Extension";
    }
}
