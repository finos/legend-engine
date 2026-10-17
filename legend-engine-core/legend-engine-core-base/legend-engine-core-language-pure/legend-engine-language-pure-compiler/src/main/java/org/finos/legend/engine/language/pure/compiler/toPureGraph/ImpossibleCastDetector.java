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

package org.finos.legend.engine.language.pure.compiler.toPureGraph;

import java.util.ArrayDeque;
import java.util.Deque;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.defect.Defect;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.defect.DefectSeverityLevel;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Generalization;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.PrimitiveType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.SimpleFunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

/**
 * Reports a cast that can never succeed: no instance can be both of the declared type of the value and of the type
 * it is cast to.
 *
 * <p>A cast never converts its value. A class can extend only classes, a primitive only primitives, and an
 * enumeration nothing, so a primitive or an enumeration can be cast only within its own hierarchy, while two classes
 * can be cast to one another only when some class extends both. Extended primitives are compared by the primitive
 * they extend, as their values are values of that primitive.
 */
final class ImpossibleCastDetector
{
    private static final String CAST_NAME = "cast_Any_m__T_1__T_m_";
    private static final String CAST_FUNCTION = "meta::pure::functions::lang::" + CAST_NAME;
    private static final String DEFECT_TYPE_ID = "PureImpossibleCast";

    private ImpossibleCastDetector()
    {
    }

    static void check(SimpleFunctionExpression expression, SourceInformation sourceInformation, PureModel pureModel)
    {
        if (!(expression._func() instanceof PackageableElement) || !CAST_NAME.equals(((PackageableElement) expression._func())._name()))
        {
            return;
        }
        if (!CAST_FUNCTION.equals(HelperModelBuilder.getElementFullPath((PackageableElement) expression._func(), pureModel.getExecutionSupport())))
        {
            return;
        }
        ListIterable<? extends ValueSpecification> parameters = Lists.mutable.withAll(expression._parametersValues());
        if (parameters.size() != 2)
        {
            return;
        }
        GenericType source = parameters.get(0)._genericType();
        GenericType target = parameters.get(1)._genericType();
        if ((source == null) || (target == null) || (source._rawType() == null) || (target._rawType() == null))
        {
            return;
        }
        ProcessorSupport processorSupport = pureModel.getExecutionSupport().getProcessorSupport();
        if (!canShareInstance(source._rawType(), target._rawType(), pureModel, processorSupport))
        {
            String message = "Cast will always fail: " + print(source, processorSupport) + " cannot be cast to " + print(target, processorSupport);
            pureModel.addDefects(Lists.fixedSize.with(new Defect(DefectSeverityLevel.ERROR, sourceInformation, message, DEFECT_TYPE_ID)));
        }
    }

    private static boolean canShareInstance(Type source, Type target, PureModel pureModel, ProcessorSupport processorSupport)
    {
        if (!isCheckable(source) || !isCheckable(target))
        {
            return true;
        }
        Type from = basePrimitive(source, processorSupport);
        Type to = basePrimitive(target, processorSupport);
        if ((from == to) || subTypeOf(from, to, processorSupport) || subTypeOf(to, from, processorSupport))
        {
            return true;
        }
        Type enumType = pureModel.getType(M3Paths.Enum);
        if (((from instanceof Enumeration) && subTypeOf(enumType, to, processorSupport)) || ((to instanceof Enumeration) && subTypeOf(enumType, from, processorSupport)))
        {
            return true;
        }
        return (from instanceof Class) && (to instanceof Class) && haveCommonSubclass(from, to, pureModel, processorSupport);
    }

    private static boolean isCheckable(Type type)
    {
        return (type instanceof Class) || (type instanceof PrimitiveType) || (type instanceof Enumeration);
    }

    private static Type basePrimitive(Type type, ProcessorSupport processorSupport)
    {
        return (type instanceof PrimitiveType) ? (Type) org.finos.legend.pure.m3.navigation.type.Type.findPrimitiveTypeFromExtendedPrimitiveType(type, processorSupport) : type;
    }

    private static boolean haveCommonSubclass(Type first, Type second, PureModel pureModel, ProcessorSupport processorSupport)
    {
        MutableSet<Type> visited = Sets.mutable.empty();
        Deque<Type> pending = new ArrayDeque<>();
        pending.add(first);
        while (!pending.isEmpty())
        {
            for (Generalization specialization : pending.poll()._specializations())
            {
                Type specific = specialization._specific();
                if (visited.add(specific))
                {
                    if (subTypeOf(specific, second, processorSupport))
                    {
                        return true;
                    }
                    pending.add(specific);
                }
            }
        }
        return pureModel.getModelClasses().anySatisfy(type -> (type instanceof Class) && subTypeOf(type, first, processorSupport) && subTypeOf(type, second, processorSupport));
    }

    private static boolean subTypeOf(CoreInstance type, CoreInstance possibleSuperType, ProcessorSupport processorSupport)
    {
        return org.finos.legend.pure.m3.navigation.type.Type.subTypeOf(type, possibleSuperType, processorSupport);
    }

    private static String print(GenericType genericType, ProcessorSupport processorSupport)
    {
        return org.finos.legend.pure.m3.navigation.generictype.GenericType.print(genericType, true, processorSupport);
    }
}
