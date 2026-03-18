// Copyright 2026 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNative;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.Native;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.TypeProcessor;

public class Reduce extends AbstractNative implements Native
{
    public Reduce()
    {
        super("reduce_Relation_1___Window_1__T_1__Function_1__Function_1__U_m_");
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        ProcessorSupport processorSupport = processorContext.getSupport();

        String type = TypeProcessor.typeToJavaObjectWithMul(Instance.getValueForMetaPropertyToOneResolved(functionExpression, M3Properties.genericType, processorSupport), Instance.getValueForMetaPropertyToOneResolved(functionExpression, M3Properties.multiplicity, processorSupport), processorSupport);

        StringBuilder result = new StringBuilder();
        result.append('(');
        result.append(type);
        result.append(')');
        result.append("(Object) org.finos.legend.pure.runtime.java.extension.external.relation.compiled.RelationNativeImplementation.reduce(");
        result.append(transformedParams.get(0)).append(", ");
        org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.ExtendWindowFunc.processWindow(result, transformedParams.get(1));
        result.append(", ").append(transformedParams.get(2)); // row
        result.append(", ").append(transformedParams.get(3)); // map
        result.append(", ").append(transformedParams.get(4)); // reduce
        result.append(", es)");
        return result.toString();
    }
}
