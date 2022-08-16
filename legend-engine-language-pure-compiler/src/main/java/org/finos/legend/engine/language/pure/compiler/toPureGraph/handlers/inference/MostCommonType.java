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

package org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.inference;

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;

public class MostCommonType
{
    public static GenericType mostCommon(MutableList<GenericType> sourceTypes, PureModel pureModel)
    {
        // Need generics
        return (GenericType) org.finos.legend.pure.m3.navigation.generictype.GenericType.findBestCommonGenericType(sourceTypes.collect(s -> s), true, false, pureModel.getExecutionSupport().getProcessorSupport());
//        Type nil = pureModel.getType(M3Paths.Nil);
//        MutableList<Type> types = sourceTypes.select(t->!t.equals(nil));
//        if (types.isEmpty())
//        {
//            return pureModel.getGenericType(M3Paths.Nil);
//        }
//        else
//        {
//            Type res = types.size() == 1? types.get(0):types.take(types.size()-1).injectInto(types.get(types.size()-1), (a,b) -> mostCommon(a,b, pureModel));
//            return new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))._rawType(res);
//        }
    }


//    public static Type mostCommon(Type a, Type b, PureModel pureModel)
//    {
//        if (a == b)
//        {
//            return a;
//        }
//        ListIterable<CoreInstance> la = C3Linearization.getTypeGeneralizationLinearization(a, pureModel.getExecutionSupport().getProcessorSupport()).toReversed();
//        ListIterable<CoreInstance> lb = C3Linearization.getTypeGeneralizationLinearization(b, pureModel.getExecutionSupport().getProcessorSupport()).toReversed();
//        int min = Math.min(la.size(), lb.size());
//        for (int i=0; i < min; i++)
//        {
//            if (la.get(i) != lb.get(i))
//            {
//                return (Type)la.get(i-1);
//            }
//        }
//        return (Type)la.get(min-1);
//    }
}
