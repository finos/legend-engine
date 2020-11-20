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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElementVisitor;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_PackageableElement_Impl;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TestCompilerExtensions
{
    @Test
    public void testSortExtraProcessors_Empty()
    {
        CompilerExtensions extensions = CompilerExtensions.fromExtensions();
        Assert.assertEquals(Lists.mutable.empty(), extensions.sortExtraProcessors());
        Assert.assertEquals(Lists.mutable.empty(), extensions.sortExtraProcessors(Lists.immutable.empty()));
        assertSortingWithUnknownProcessorThrows(extensions, TestElement1.class);
    }

    @Test
    public void testSortExtraProcessors_NoPrerequisites()
    {
        Processor<?> processor1 = Processor.newProcessor(TestElement1.class, this::stubFirstPass);
        Processor<?> processor2 = Processor.newProcessor(TestElement2.class, this::stubFirstPass);

        CompilerExtensions extensions = CompilerExtensions.fromExtensions(newExtension(processor1, processor2));
        List<Processor<?>> sortedAll = extensions.sortExtraProcessors();
        Assert.assertEquals(2, sortedAll.size());
        Assert.assertEquals(Sets.immutable.with(processor1, processor2), Sets.immutable.withAll(sortedAll));

        Assert.assertEquals(Lists.immutable.with(processor1), extensions.sortExtraProcessors(Lists.immutable.with(processor1)));
        Assert.assertEquals(Lists.immutable.with(processor2), extensions.sortExtraProcessors(Lists.immutable.with(processor2)));

        assertSortingWithUnknownProcessorThrows(extensions, TestElement1.class);
        assertSortingWithUnknownProcessorThrows(extensions, TestElement2.class);
        assertSortingWithUnknownProcessorThrows(extensions, TestElement3.class);
    }

    @Test
    public void testSortExtraProcessors_WithPrerequisites_Simple()
    {
        Processor<?> processor1 = Processor.newProcessor(TestElement1.class, this::stubFirstPass);
        Processor<?> processor2 = Processor.newProcessor(TestElement2.class, Collections.singletonList(TestElement1.class), this::stubFirstPass);

        CompilerExtensions extensions = CompilerExtensions.fromExtensions(newExtension(processor1, processor2));
        Assert.assertEquals(Lists.immutable.with(processor1, processor2), extensions.sortExtraProcessors());
        Assert.assertEquals(Lists.immutable.with(processor1, processor2), extensions.sortExtraProcessors(Lists.immutable.with(processor2, processor1)));
        Assert.assertEquals(Lists.immutable.with(processor1), extensions.sortExtraProcessors(Lists.immutable.with(processor1)));
        Assert.assertEquals(Lists.immutable.with(processor2), extensions.sortExtraProcessors(Lists.immutable.with(processor2)));

        assertSortingWithUnknownProcessorThrows(extensions, TestElement3.class);
    }

    @Test
    public void testSortExtraProcessors_WithPrerequisites_Complex()
    {
        Processor<?> processor1 = Processor.newProcessor(TestElement1.class, Collections.singletonList(TestElement345.class), this::stubFirstPass);
        Processor<?> processor2 = Processor.newProcessor(TestElement2.class, Arrays.asList(TestElement1.class, TestElement3.class), this::stubFirstPass);
        Processor<?> processor3 = Processor.newProcessor(TestElement3.class, this::stubFirstPass);
        Processor<?> processor4 = Processor.newProcessor(TestElement4.class, Collections.singletonList(TestElement3.class), this::stubFirstPass);
        Processor<?> processor5 = Processor.newProcessor(TestElement5.class, Collections.singletonList(TestElement4.class), this::stubFirstPass);

        ImmutableList<Processor<?>> processors = Lists.immutable.with(processor1, processor2, processor3, processor4, processor5);
        CompilerExtensions extensions = CompilerExtensions.fromExtensions(newExtension(processors));

        for (Processor<?> processor : processors)
        {
            Assert.assertEquals(Lists.immutable.with(processor), extensions.sortExtraProcessors(Lists.immutable.with(processor)));
        }

        Assert.assertEquals(Lists.immutable.with(processor3, processor4, processor5, processor1, processor2), extensions.sortExtraProcessors());

        assertSortingWithUnknownProcessorThrows(extensions, TestElement5.class);
    }

    @Test
    public void testSortExtraProcessors_WithPrerequisitesLoop()
    {
        Processor<?> processor1 = Processor.newProcessor(TestElement1.class, Collections.singletonList(TestElement2.class), this::stubFirstPass);
        Processor<?> processor2 = Processor.newProcessor(TestElement2.class, Arrays.asList(TestElement1.class, TestElement3.class), this::stubFirstPass);
        Processor<?> processor3 = Processor.newProcessor(TestElement3.class, this::stubFirstPass);

        CompilerExtensions extensions = CompilerExtensions.fromExtensions(newExtension(processor1, processor2, processor3));

        Assert.assertEquals(Lists.immutable.with(processor1), extensions.sortExtraProcessors(Lists.immutable.with(processor1)));
        Assert.assertEquals(Lists.immutable.with(processor2), extensions.sortExtraProcessors(Lists.immutable.with(processor2)));
        Assert.assertEquals(Lists.immutable.with(processor3, processor1), extensions.sortExtraProcessors(Lists.immutable.with(processor1, processor3)));
        Assert.assertEquals(Lists.immutable.with(processor3, processor2), extensions.sortExtraProcessors(Lists.immutable.with(processor2, processor3)));
        assertSortingWithUnknownProcessorThrows(extensions, TestElement3.class);

        String possibleMessage1 = "Could not consistently order the following processors: " + processor1 + ", " + processor2;
        String possibleMessage2 = "Could not consistently order the following processors: " + processor2 + ", " + processor1;
        EngineException e1 = Assert.assertThrows(EngineException.class, extensions::sortExtraProcessors);
        if (!possibleMessage1.equals(e1.getMessage()) && !possibleMessage2.equals(e1.getMessage()))
        {
            Assert.assertEquals(possibleMessage1, e1.getMessage());
        }

        EngineException e2 = Assert.assertThrows(EngineException.class, () -> extensions.sortExtraProcessors(Lists.immutable.with(processor1, processor2)));
        if (!possibleMessage1.equals(e2.getMessage()) && !possibleMessage2.equals(e2.getMessage()))
        {
            Assert.assertEquals(possibleMessage1, e2.getMessage());
        }
    }

    @Test
    public void testSortExtraProcessors_WithPrerequisitesLoop_Complex()
    {
        Processor<?> processor1 = Processor.newProcessor(TestElement1.class, Collections.singletonList(TestElement345.class), this::stubFirstPass);
        Processor<?> processor2 = Processor.newProcessor(TestElement2.class, Arrays.asList(TestElement1.class, TestElement3.class), this::stubFirstPass);
        Processor<?> processor3 = Processor.newProcessor(TestElement3.class, this::stubFirstPass);
        Processor<?> processor4 = Processor.newProcessor(TestElement4.class, Collections.singletonList(TestElement2.class), this::stubFirstPass);
        Processor<?> processor5 = Processor.newProcessor(TestElement5.class, Collections.singletonList(TestElement12.class), this::stubFirstPass);

        ImmutableList<Processor<?>> processors = Lists.immutable.with(processor1, processor2, processor3, processor4, processor5);
        CompilerExtensions extensions = CompilerExtensions.fromExtensions(newExtension(processors));

        for (Processor<?> processor : processors)
        {
            Assert.assertEquals(Lists.immutable.with(processor), extensions.sortExtraProcessors(Lists.immutable.with(processor)));
        }
        Assert.assertEquals(Lists.immutable.with(processor3, processor1), extensions.sortExtraProcessors(Lists.immutable.with(processor1, processor3)));
        Assert.assertEquals(Lists.immutable.with(processor3, processor2), extensions.sortExtraProcessors(Lists.immutable.with(processor2, processor3)));
        Assert.assertEquals(Lists.immutable.with(processor3, processor1, processor2), extensions.sortExtraProcessors(Lists.immutable.with(processor1, processor2, processor3)));
        assertSortingWithUnknownProcessorThrows(extensions, TestElement4.class);

        String prefix = "Could not consistently order the following processors: ";
        EngineException e1 = Assert.assertThrows(EngineException.class, extensions::sortExtraProcessors);
        String e1Message = e1.getMessage();
        Assert.assertTrue(e1Message.startsWith(prefix));
        Assert.assertEquals(
                Sets.mutable.with(processor1.toString(), processor2.toString(), processor4.toString(), processor5.toString()),
                Sets.mutable.with(e1Message.substring(prefix.length()).split(", ")));

        EngineException e2 = Assert.assertThrows(EngineException.class, () -> extensions.sortExtraProcessors(Lists.immutable.with(processor1, processor5)));
        String e2Message = e2.getMessage();
        Assert.assertTrue(e2Message.startsWith(prefix));
        Assert.assertEquals(
                Sets.mutable.with(processor1.toString(), processor5.toString()),
                Sets.mutable.with(e2Message.substring(prefix.length()).split(", ")));
    }

    private void assertSortingWithUnknownProcessorThrows(CompilerExtensions extensions, Class<? extends TestElement> processorClass)
    {
        Processor<?> unknownProcessor = Processor.newProcessor(processorClass, this::stubFirstPass);
        MutableList<Processor<?>> processors = Lists.mutable.with(unknownProcessor);
        IllegalArgumentException e1 = Assert.assertThrows(IllegalArgumentException.class, () -> extensions.sortExtraProcessors(processors));
        Assert.assertEquals("Unknown processor: " + unknownProcessor, e1.getMessage());

        processors.addAllIterable(extensions.getExtraProcessors());
        IllegalArgumentException e2 = Assert.assertThrows(IllegalArgumentException.class, () -> extensions.sortExtraProcessors(processors));
        Assert.assertEquals("Unknown processor: " + unknownProcessor, e2.getMessage());
    }

    private org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement stubFirstPass(PackageableElement element, CompileContext context)
    {
        return new Root_meta_pure_metamodel_PackageableElement_Impl("");
    }

    private static CompilerExtension newExtension(Processor<?>... processors)
    {
        return newExtension(Arrays.asList(processors));
    }

    private static CompilerExtension newExtension(Iterable<? extends Processor<?>> processors)
    {
        return () -> processors;
    }

    private static class TestElement extends PackageableElement
    {
        @Override
        public <T> T accept(PackageableElementVisitor<T> visitor)
        {
            return visitor.visit(this);
        }
    }

    private static class TestElement12 extends TestElement
    {
    }

    private static class TestElement345 extends TestElement
    {
    }

    private static class TestElement1 extends TestElement12
    {
    }

    private static class TestElement2 extends TestElement12
    {
    }

    private static class TestElement3 extends TestElement345
    {
    }

    private static class TestElement4 extends TestElement345
    {
    }
    private static class TestElement5 extends TestElement345
    {
    }
}
