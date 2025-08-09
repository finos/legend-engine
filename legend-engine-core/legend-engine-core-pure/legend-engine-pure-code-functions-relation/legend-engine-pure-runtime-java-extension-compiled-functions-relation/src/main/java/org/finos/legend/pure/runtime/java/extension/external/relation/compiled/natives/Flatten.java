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

package org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNative;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.Native;

public class Flatten extends AbstractNative implements Native
{
    public Flatten()
    {
        super("flatten_T_MANY__ColSpec_1__Relation_1_");
    }

    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        ListIterable<? extends CoreInstance> parameterValues = functionExpression.getValueForMetaPropertyToMany(M3Properties.parametersValues);

        CoreInstance rawType = Instance.getValueForMetaPropertyToOneResolved(parameterValues.get(0), M3Properties.genericType, M3Properties.rawType, processorContext.getSupport());
        String type = PackageableElement.getUserPathForPackageableElement(rawType, "::");

        StringBuilder result = new StringBuilder("org.finos.legend.pure.runtime.java.extension.external.relation.compiled.RelationNativeImplementation.flatten");
        result.append('(');
        result.append(transformedParams.get(0));
        result.append(",");
        result.append(transformedParams.get(1)).append("._name()");
        result.append(",");
        result.append("\"").append(type).append("\"");
        result.append(",es)\n");
        return result.toString();
    }
}
