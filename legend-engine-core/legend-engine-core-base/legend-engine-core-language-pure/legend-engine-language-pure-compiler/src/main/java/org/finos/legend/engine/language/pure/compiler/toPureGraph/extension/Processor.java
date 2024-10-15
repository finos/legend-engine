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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;

import java.util.Collection;
import java.util.Collections;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public abstract class Processor<T extends PackageableElement>
{
    public abstract Class<T> getElementClass();

    public Collection<? extends Class<? extends PackageableElement>> getPrerequisiteClasses()
    {
        return Collections.emptyList();
    }

    public RichIterable<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement> getPrerequisiteElements(PackageableElement element, CompileContext context)
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

    protected abstract RichIterable<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement> processPrerequisiteElements(T element, CompileContext context);

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
        return newProcessor(elementClass, null, firstPass);
    }

    public static <T extends PackageableElement> Processor<T> newProcessor(Class<T> elementClass, Collection<? extends java.lang.Class<? extends PackageableElement>> prerequisiteClasses, BiFunction<? super T, CompileContext, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement> firstPass)
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
            protected RichIterable<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement> processPrerequisiteElements(T element, CompileContext context)
            {
                return Lists.fixedSize.empty();
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
        return newProcessor(elementClass, prerequisiteClasses, firstPass, secondPass, null, null, null, null);
    }

    public static <T extends PackageableElement> Processor<T> newProcessor(Class<T> elementClass,
                                                                           BiFunction<? super T, CompileContext, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement> firstPass,
                                                                           BiConsumer<? super T, CompileContext> secondPass,
                                                                           BiConsumer<? super T, CompileContext> thirdPass)
    {
        return newProcessor(elementClass, null, firstPass, secondPass, thirdPass);
    }

    public static <T extends PackageableElement> Processor<T> newProcessor(Class<T> elementClass,
                                                                           Collection<? extends Class<? extends PackageableElement>> prerequisiteClasses,
                                                                           BiFunction<? super T, CompileContext, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement> firstPass,
                                                                           BiConsumer<? super T, CompileContext> secondPass,
                                                                           BiConsumer<? super T, CompileContext> thirdPass)
    {
        return newProcessor(elementClass, prerequisiteClasses, firstPass, secondPass, thirdPass, null, null, null);
    }

    public static <T extends PackageableElement> Processor<T> newProcessor(Class<T> elementClass,
                                                                           BiFunction<? super T, CompileContext, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement> firstPass,
                                                                           BiConsumer<? super T, CompileContext> secondPass,
                                                                           BiConsumer<? super T, CompileContext> thirdPass,
                                                                           BiFunction<? super T, CompileContext, RichIterable<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement>> prerequisiteElementsPass)
    {
        return newProcessor(elementClass, null, firstPass, secondPass, thirdPass, null, null, prerequisiteElementsPass);
    }

    public static <T extends PackageableElement> Processor<T> newProcessor(Class<T> elementClass,
                                                                           Collection<? extends Class<? extends PackageableElement>> prerequisiteClasses,
                                                                           BiFunction<? super T, CompileContext, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement> firstPass,
                                                                           BiConsumer<? super T, CompileContext> secondPass,
                                                                           BiConsumer<? super T, CompileContext> thirdPass,
                                                                           BiFunction<? super T, CompileContext, RichIterable<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement>> prerequisiteElementsPass)
    {
        return newProcessor(elementClass, prerequisiteClasses, firstPass, secondPass, thirdPass, null, null, prerequisiteElementsPass);
    }

    /**
     * @deprecated This will soon be removed as part of Engine compiler cleanup in the next release
     */
    @Deprecated
    public static <T extends PackageableElement> Processor<T> newProcessor(Class<T> elementClass,
                                                                           BiFunction<? super T, CompileContext, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement> firstPass,
                                                                           BiConsumer<? super T, CompileContext> secondPass,
                                                                           BiConsumer<? super T, CompileContext> thirdPass,
                                                                           BiConsumer<? super T, CompileContext> fourthPass)
    {
        return newProcessor(elementClass, null, firstPass, secondPass, thirdPass, fourthPass, null, null);
    }

    /**
     * @deprecated This will soon be removed as part of Engine compiler cleanup in the next release
     */
    @Deprecated
    public static <T extends PackageableElement> Processor<T> newProcessor(Class<T> elementClass,
                                                                           Collection<? extends Class<? extends PackageableElement>> prerequisiteClasses,
                                                                           BiFunction<? super T, CompileContext, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement> firstPass,
                                                                           BiConsumer<? super T, CompileContext> secondPass,
                                                                           BiConsumer<? super T, CompileContext> thirdPass,
                                                                           BiConsumer<? super T, CompileContext> fourthPass)
    {
        return newProcessor(elementClass, prerequisiteClasses, firstPass, secondPass, thirdPass, fourthPass, null, null);
    }

    /**
     * @deprecated This will soon be removed as part of Engine compiler cleanup in the next release
     */
    @Deprecated
    public static <T extends PackageableElement> Processor<T> newProcessor(Class<T> elementClass,
                                                                           BiFunction<? super T, CompileContext, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement> firstPass,
                                                                           BiConsumer<? super T, CompileContext> secondPass,
                                                                           BiConsumer<? super T, CompileContext> thirdPass,
                                                                           BiConsumer<? super T, CompileContext> fourthPass,
                                                                           BiFunction<? super T, CompileContext, RichIterable<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement>> prerequisiteElementsPass)
    {
        return newProcessor(elementClass, null, firstPass, secondPass, thirdPass, fourthPass, null, prerequisiteElementsPass);
    }

    /**
     * @deprecated This will soon be removed as part of Engine compiler cleanup in the next release
     */
    @Deprecated
    public static <T extends PackageableElement> Processor<T> newProcessor(Class<T> elementClass,
                                                                           Collection<? extends Class<? extends PackageableElement>> prerequisiteClasses,
                                                                           BiFunction<? super T, CompileContext, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement> firstPass,
                                                                           BiConsumer<? super T, CompileContext> secondPass,
                                                                           BiConsumer<? super T, CompileContext> thirdPass,
                                                                           BiConsumer<? super T, CompileContext> fourthPass,
                                                                           BiFunction<? super T, CompileContext, RichIterable<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement>> prerequisiteElementsPass)
    {
        return newProcessor(elementClass, prerequisiteClasses, firstPass, secondPass, thirdPass, fourthPass, null, prerequisiteElementsPass);
    }

    /**
     * @deprecated This will soon be removed as part of Engine compiler cleanup in the next release
     */
    @Deprecated
    public static <T extends PackageableElement> Processor<T> newProcessor(Class<T> elementClass,
                                                                           BiFunction<? super T, CompileContext, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement> firstPass,
                                                                           BiConsumer<? super T, CompileContext> secondPass,
                                                                           BiConsumer<? super T, CompileContext> thirdPass,
                                                                           BiConsumer<? super T, CompileContext> fourthPass,
                                                                           BiConsumer<? super T, CompileContext> fifthPass)
    {
        return newProcessor(elementClass, null, firstPass, secondPass, thirdPass, fourthPass, fifthPass, null);
    }

    /**
     * @deprecated This will soon be removed as part of Engine compiler cleanup in the next release
     */
    @Deprecated
    public static <T extends PackageableElement> Processor<T> newProcessor(Class<T> elementClass,
                                                                           Collection<? extends Class<? extends PackageableElement>> prerequisiteClasses,
                                                                           BiFunction<? super T, CompileContext, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement> firstPass,
                                                                           BiConsumer<? super T, CompileContext> secondPass,
                                                                           BiConsumer<? super T, CompileContext> thirdPass,
                                                                           BiConsumer<? super T, CompileContext> fourthPass,
                                                                           BiConsumer<? super T, CompileContext> fifthPass)
    {
        return newProcessor(elementClass, prerequisiteClasses, firstPass, secondPass, thirdPass, fourthPass, fifthPass, null);
    }

    /**
     * @deprecated This will soon be removed as part of Engine compiler cleanup in the next release
     */
    @Deprecated
    public static <T extends PackageableElement> Processor<T> newProcessor(Class<T> elementClass,
                                                                           BiFunction<? super T, CompileContext, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement> firstPass,
                                                                           BiConsumer<? super T, CompileContext> secondPass,
                                                                           BiConsumer<? super T, CompileContext> thirdPass,
                                                                           BiConsumer<? super T, CompileContext> fourthPass,
                                                                           BiConsumer<? super T, CompileContext> fifthPass,
                                                                           BiFunction<? super T, CompileContext, RichIterable<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement>> prerequisiteElementsPass)
    {
        return newProcessor(elementClass, null, firstPass, secondPass, thirdPass, fourthPass, fifthPass, prerequisiteElementsPass);
    }

    /**
     * @deprecated This will soon be removed as part of Engine compiler cleanup in the next release
     */
    @Deprecated
    public static <T extends PackageableElement> Processor<T> newProcessor(Class<T> elementClass,
                                                                           Collection<? extends Class<? extends PackageableElement>> prerequisiteClasses,
                                                                           BiFunction<? super T, CompileContext, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement> firstPass,
                                                                           BiConsumer<? super T, CompileContext> secondPass,
                                                                           BiConsumer<? super T, CompileContext> thirdPass,
                                                                           BiConsumer<? super T, CompileContext> fourthPass,
                                                                           BiConsumer<? super T, CompileContext> fifthPass,
                                                                           BiFunction<? super T, CompileContext, RichIterable<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement>> prerequisiteElementsPass)
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
            protected RichIterable<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement> processPrerequisiteElements(T element, CompileContext context)
            {
                if (prerequisiteElementsPass != null)
                {
                    return prerequisiteElementsPass.apply(element, context);
                }
                return Lists.fixedSize.empty();
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

                if (fourthPass != null)
                {
                    fourthPass.accept(element, context);
                }

                if (fifthPass != null)
                {
                    fifthPass.accept(element, context);
                }
            }
        };
    }
}
