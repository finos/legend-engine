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

package org.finos.legend.engine.language.pure.compiler.toPureGraph.extension;

import org.eclipse.collections.api.factory.Sets;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public abstract class Processor<T extends PackageableElement>
{
    public abstract Class<T> getElementClass();

    public Collection<? extends Class<? extends PackageableElement>> getPrerequisiteClasses()
    {
        return Collections.emptyList();
    }

    public Collection<? extends Class<? extends PackageableElement>> getReversePrerequisiteClasses()
    {
        return Collections.emptyList();
    }

    public Set<PackageableElementPointer> getPrerequisiteElements(PackageableElement element, CompileContext context)
    {
        return processPrerequisiteElements(castElement(element), context);
    }

    public final org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement processFirstPass(PackageableElement element, CompileContext context)
    {
        return processElementFirstPass(castElement(element), context);
    }

    public final void processSecondPass(PackageableElement element, CompileContext context)
    {
        processElementSecondPass(castElement(element), context);
    }

    public final void processThirdPass(PackageableElement element, CompileContext context)
    {
        processElementThirdPass(castElement(element), context);
    }

    @Override
    public final boolean equals(Object other)
    {
        return this == other;
    }

    @Override
    public final int hashCode()
    {
        return System.identityHashCode(this);
    }

    @Override
    public String toString()
    {
        return "<Processor class=" + getElementClass().getName() + " @" + Integer.toHexString(hashCode()) + ">";
    }

    protected abstract Set<PackageableElementPointer> processPrerequisiteElements(T element, CompileContext context);

    protected abstract org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement processElementFirstPass(T element, CompileContext context);

    protected void processElementSecondPass(T element, CompileContext context)
    {
        // nothing by default
    }

    protected void processElementThirdPass(T element, CompileContext context)
    {
        // nothing by default
    }

    private T castElement(PackageableElement element)
    {
        return getElementClass().cast(element);
    }

    public static <T extends PackageableElement> Processor<T> newProcessor(Class<T> elementClass, BiFunction<? super T, CompileContext, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement> firstPass)
    {
        return newProcessor(elementClass, null, null, firstPass);
    }

    public static <T extends PackageableElement> Processor<T> newProcessor(Class<T> elementClass, Collection<? extends java.lang.Class<? extends PackageableElement>> prerequisiteClasses, BiFunction<? super T, CompileContext, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement> firstPass)
    {
        return newProcessor(elementClass, prerequisiteClasses, null, firstPass);
    }

    public static <T extends PackageableElement> Processor<T> newProcessor(Class<T> elementClass, Collection<? extends java.lang.Class<? extends PackageableElement>> prerequisiteClasses, Collection<? extends Class<? extends PackageableElement>> reversePrerequisiteClasses, BiFunction<? super T, CompileContext, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement> firstPass)
    {
        Collection<? extends Class<? extends PackageableElement>> resolvedPrerequisiteClasses = (prerequisiteClasses == null) ? Collections.emptyList() : prerequisiteClasses;
        Collection<? extends Class<? extends PackageableElement>> resolvedReversePrerequisiteClasses = (reversePrerequisiteClasses == null) ? Collections.emptyList() : reversePrerequisiteClasses;
        return new Processor<T>()
        {
            @Override
            public Class<T> getElementClass()
            {
                return elementClass;
            }

            @Override
            public Collection<? extends Class<? extends PackageableElement>> getPrerequisiteClasses()
            {
                return resolvedPrerequisiteClasses;
            }

            @Override
            public Collection<? extends Class<? extends PackageableElement>> getReversePrerequisiteClasses()
            {
                return resolvedReversePrerequisiteClasses;
            }

            @Override
            protected Set<PackageableElementPointer> processPrerequisiteElements(T element, CompileContext context)
            {
                return Sets.fixedSize.empty();
            }

            @Override
            protected org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement processElementFirstPass(T element, CompileContext context)
            {
                return firstPass.apply(element, context);
            }
        };
    }

    public static <T extends PackageableElement> Processor<T> newProcessor(Class<T> elementClass,
                                                                           BiFunction<? super T, CompileContext, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement> firstPass,
                                                                           BiConsumer<? super T, CompileContext> secondPass)
    {
        return newProcessor(elementClass, null, firstPass, secondPass);
    }

    public static <T extends PackageableElement> Processor<T> newProcessor(Class<T> elementClass,
                                                                           Collection<? extends Class<? extends PackageableElement>> prerequisiteClasses,
                                                                           BiFunction<? super T, CompileContext, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement> firstPass,
                                                                           BiConsumer<? super T, CompileContext> secondPass)
    {
        return newProcessor(elementClass, prerequisiteClasses, firstPass, secondPass, null, null);
    }

    public static <T extends PackageableElement> Processor<T> newProcessor(Class<T> elementClass,
                                                                           BiFunction<? super T, CompileContext, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement> firstPass,
                                                                           BiConsumer<? super T, CompileContext> secondPass,
                                                                           BiConsumer<? super T, CompileContext> thirdPass)
    {
        return newProcessor(elementClass, null, firstPass, secondPass, thirdPass, null);
    }

    public static <T extends PackageableElement> Processor<T> newProcessor(Class<T> elementClass,
                                                                           Collection<? extends Class<? extends PackageableElement>> prerequisiteClasses,
                                                                           BiFunction<? super T, CompileContext, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement> firstPass,
                                                                           BiConsumer<? super T, CompileContext> secondPass,
                                                                           BiConsumer<? super T, CompileContext> thirdPass)
    {
        return newProcessor(elementClass, prerequisiteClasses, firstPass, secondPass, thirdPass, null);
    }

    public static <T extends PackageableElement> Processor<T> newProcessor(Class<T> elementClass,
                                                                           BiFunction<? super T, CompileContext, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement> firstPass,
                                                                           BiConsumer<? super T, CompileContext> secondPass,
                                                                           BiConsumer<? super T, CompileContext> thirdPass,
                                                                           BiFunction<? super T, CompileContext, Set<PackageableElementPointer>> prerequisiteElementsPass)
    {
        return newProcessor(elementClass, null, firstPass, secondPass, thirdPass, prerequisiteElementsPass);
    }

    public static <T extends PackageableElement> Processor<T> newProcessor(Class<T> elementClass,
                                                                           Collection<? extends Class<? extends PackageableElement>> prerequisiteClasses,
                                                                           BiFunction<? super T, CompileContext, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement> firstPass,
                                                                           BiConsumer<? super T, CompileContext> secondPass,
                                                                           BiConsumer<? super T, CompileContext> thirdPass,
                                                                           BiFunction<? super T, CompileContext, Set<PackageableElementPointer>> prerequisiteElementsPass)
    {
        Collection<? extends Class<? extends PackageableElement>> resolvedPrerequisiteClasses = (prerequisiteClasses == null) ? Collections.emptyList() : prerequisiteClasses;
         return new Processor<T>()
        {
            @Override
            public Class<T> getElementClass()
            {
                return elementClass;
            }

            @Override
            public Collection<? extends Class<? extends PackageableElement>> getPrerequisiteClasses()
            {
                return resolvedPrerequisiteClasses;
            }

            @Override
            protected Set<PackageableElementPointer> processPrerequisiteElements(T element, CompileContext context)
            {
                if (prerequisiteElementsPass != null)
                {
                    return prerequisiteElementsPass.apply(element, context);
                }
                return Sets.fixedSize.empty();
            }

            @Override
            protected org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement processElementFirstPass(T element, CompileContext context)
            {
                return firstPass.apply(element, context);
            }

            @Override
            protected void processElementSecondPass(T element, CompileContext context)
            {
                if (secondPass != null)
                {
                    secondPass.accept(element, context);
                }
            }

            @Override
            protected void processElementThirdPass(T element, CompileContext context)
            {
                if (thirdPass != null)
                {
                    thirdPass.accept(element, context);
                }
            }
        };
    }
}
