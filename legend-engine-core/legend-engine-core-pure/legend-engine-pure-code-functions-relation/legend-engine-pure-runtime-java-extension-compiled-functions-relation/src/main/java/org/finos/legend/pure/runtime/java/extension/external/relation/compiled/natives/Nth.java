// Copyright 2024 Goldman Sachs
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
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNative;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.Native;

import static org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.ExtendWindowFunc.processWindow;

public class Nth extends AbstractNative implements Native
{
    public Nth()
    {
        super("nth_Relation_1___Window_1__T_1__Integer_1__T_$0_1$_");
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        StringBuilder result = new StringBuilder("org.finos.legend.pure.runtime.java.extension.external.relation.compiled.RelationNativeImplementation.nth(");
        result.append(transformedParams.get(0));
        result.append(", ");
        processWindow(result, transformedParams.get(1));
        result.append(",");
        result.append(transformedParams.get(2));
        result.append(",");
        result.append(transformedParams.get(3));
        result.append(",es)");
        return result.toString();
    }
}






//
//
//
//
//        extends AbstractNativeFunctionGeneric
//{
//    public Nth()
//    {
//        super("org.finos.legend.pure.runtime.java.extension.external.relation.compiled.RelationNativeImplementation.nth", new Class[]{Relation.class, Frame.class, Integer.class, ExecutionSupport.class}, false, true, false, "nth_Relation_1__Frame_1__Integer_1__T_$0_1$_");
//    }
//}
//
//
