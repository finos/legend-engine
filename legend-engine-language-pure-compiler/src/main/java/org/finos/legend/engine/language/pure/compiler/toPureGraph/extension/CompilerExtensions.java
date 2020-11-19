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
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ProcessingContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.FunctionExpressionBuilderRegistrationInfo;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.FunctionHandlerDispatchBuilderInfo;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.FunctionHandlerRegistrationInfo;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.Handlers;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.AssociationMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.ClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.InputData;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.executionContext.ExecutionContext;
import org.finos.legend.engine.shared.core.function.Function4;
import org.finos.legend.engine.shared.core.function.Procedure3;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.AssociationImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.EmbeddedSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.runtime.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

public class CompilerExtensions
{
    private static final Logger LOGGER = LoggerFactory.getLogger("Alloy Execution Server");

    @SuppressWarnings("unchecked")
    private static final ImmutableSet<Class<? extends PackageableElement>> FORBIDDEN_PROCESSOR_CLASSES = Sets.immutable.with(
            org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement.class,
            org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.PackageableConnection.class,
            org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Association.class,
            org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Class.class,
            org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Enumeration.class,
            org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Function.class,
            org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Measure.class,
            org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Profile.class,
            org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Unit.class,
            org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.PackageableRuntime.class,
            org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.SectionIndex.class,
            org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.Store.class
    );

    private final ImmutableList<CompilerExtension> extensions;
    private final MapIterable<Class<? extends PackageableElement>, Processor<?>> extraProcessors;
    private final ImmutableList<Function3<ClassMapping, Mapping, CompileContext, Pair<SetImplementation, RichIterable<EmbeddedSetImplementation>>>> extraClassMappingFirstPassProcessors;
    private final ImmutableList<Procedure3<ClassMapping, Mapping, CompileContext>> extraClassMappingSecondPassProcessors;
    private final ImmutableList<Function3<AssociationMapping, Mapping, CompileContext, AssociationImplementation>> extraAssociationMappingProcessors;
    private final ImmutableList<Function2<org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection, CompileContext, Connection>> extraConnectionValueProcessors;
    private final ImmutableList<Procedure2<InputData, CompileContext>> extraMappingTestInputDataProcessors;
    private final ImmutableList<Function<Handlers, List<FunctionHandlerDispatchBuilderInfo>>> extraFunctionHandlerDispatchBuilderInfoCollectors;
    private final ImmutableList<Function<Handlers, List<FunctionExpressionBuilderRegistrationInfo>>> extraFunctionExpressionBuilderRegistrationInfoCollectors;
    private final ImmutableList<Function<Handlers, List<FunctionHandlerRegistrationInfo>>> extraFunctionHandlerRegistrationInfoCollectors;
    private final ImmutableList<Function4<org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification, CompileContext, List<String>, ProcessingContext, ValueSpecification>> extraValueSpecificationProcessors;
    private final ImmutableList<Procedure2<PackageableElement, MutableMap<String, String>>> extraStoreStatBuilders;
    private final ImmutableList<Function2<ExecutionContext, CompileContext, org.finos.legend.pure.m3.coreinstance.meta.pure.runtime.ExecutionContext>> extraExecutionContextProcessors;
    private final ImmutableList<Procedure<Procedure2<String, List<String>>>> extraElementForPathToElementRegisters;
    private final ImmutableList<Procedure3<SetImplementation, Set<String>, CompileContext>> extraSetImplementationSourceScanners;
    private final ImmutableList<Procedure2<PureModel, PureModelContextData>> extraPostValidators;

    private CompilerExtensions(Iterable<? extends CompilerExtension> extensions)
    {
        this.extensions = Lists.immutable.withAll(extensions);
        this.extraProcessors = indexProcessors(this.extensions);
        this.extraClassMappingFirstPassProcessors = this.extensions.flatCollect(CompilerExtension::getExtraClassMappingFirstPassProcessors);
        this.extraClassMappingSecondPassProcessors = this.extensions.flatCollect(CompilerExtension::getExtraClassMappingSecondPassProcessors);
        this.extraAssociationMappingProcessors = this.extensions.flatCollect(CompilerExtension::getExtraAssociationMappingProcessors);
        this.extraConnectionValueProcessors = this.extensions.flatCollect(CompilerExtension::getExtraConnectionValueProcessors);
        this.extraMappingTestInputDataProcessors = this.extensions.flatCollect(CompilerExtension::getExtraMappingTestInputDataProcessors);
        this.extraFunctionHandlerDispatchBuilderInfoCollectors = this.extensions.flatCollect(CompilerExtension::getExtraFunctionHandlerDispatchBuilderInfoCollectors);
        this.extraFunctionExpressionBuilderRegistrationInfoCollectors = this.extensions.flatCollect(CompilerExtension::getExtraFunctionExpressionBuilderRegistrationInfoCollectors);
        this.extraFunctionHandlerRegistrationInfoCollectors = this.extensions.flatCollect(CompilerExtension::getExtraFunctionHandlerRegistrationInfoCollectors);
        this.extraValueSpecificationProcessors = this.extensions.flatCollect(CompilerExtension::getExtraValueSpecificationProcessors);
        this.extraStoreStatBuilders = this.extensions.flatCollect(CompilerExtension::getExtraStoreStatBuilders);
        this.extraExecutionContextProcessors = this.extensions.flatCollect(CompilerExtension::getExtraExecutionContextProcessors);
        this.extraElementForPathToElementRegisters = this.extensions.flatCollect(CompilerExtension::getExtraElementForPathToElementRegisters);
        this.extraSetImplementationSourceScanners = this.extensions.flatCollect(CompilerExtension::getExtraSetImplementationSourceScanners);
        this.extraPostValidators = this.extensions.flatCollect(CompilerExtension::getExtraPostValidators);
    }

    public List<CompilerExtension> getExtensions()
    {
        return this.extensions.castToList();
    }

    public Iterable<? extends Processor<?>> getExtraProcessors()
    {
        return this.extraProcessors.valuesView();
    }

    public Processor<?> getExtraProcessorOrThrow(PackageableElement element)
    {
        Processor<?> processor = getExtraProcessor(element);
        if (processor == null)
        {
            throw new UnsupportedOperationException("No extra processor available for element " + element.getPath() + " of type " + element.getClass().getName());
        }
        return processor;
    }

    public Processor<?> getExtraProcessor(PackageableElement element)
    {
        return getExtraProcessor(element.getClass());
    }

    public Processor<?> getExtraProcessorOrThrow(java.lang.Class<? extends PackageableElement> cls)
    {
        Processor<?> processor = getExtraProcessor(cls);
        if (processor == null)
        {
            throw new UnsupportedOperationException("No extra processor available for type " + cls.getName());
        }
        return processor;
    }

    public Processor<?> getExtraProcessor(java.lang.Class<? extends PackageableElement> cls)
    {
        return this.extraProcessors.isEmpty() ? null : getExtraProcessor_recursive(cls);
    }

    private Processor<?> getExtraProcessor_recursive(java.lang.Class<?> cls)
    {
        Processor<?> processor = this.extraProcessors.get(cls);
        if (processor != null)
        {
            return processor;
        }
        if (FORBIDDEN_PROCESSOR_CLASSES.contains(cls))
        {
            return null;
        }
        // We can ignore interfaces in this search, since PackageableElement is itself a class (not an interface)
        java.lang.Class<?> superClass = cls.getSuperclass();
        return (superClass == null) ? null : getExtraProcessor_recursive(superClass);
    }

    public List<Function3<ClassMapping, Mapping, CompileContext, Pair<SetImplementation, RichIterable<EmbeddedSetImplementation>>>> getExtraClassMappingFirstPassProcessors()
    {
        return this.extraClassMappingFirstPassProcessors.castToList();
    }

    public List<Procedure3<ClassMapping, Mapping, CompileContext>> getExtraClassMappingSecondPassProcessors()
    {
        return this.extraClassMappingSecondPassProcessors.castToList();
    }

    public List<Function3<AssociationMapping, Mapping, CompileContext, AssociationImplementation>> getExtraAssociationMappingProcessors()
    {
        return this.extraAssociationMappingProcessors.castToList();
    }

    public List<Function2<org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection, CompileContext, Connection>> getExtraConnectionValueProcessors()
    {
        return this.extraConnectionValueProcessors.castToList();
    }

    public List<Procedure2<InputData, CompileContext>> getExtraMappingTestInputDataProcessors()
    {
        return this.extraMappingTestInputDataProcessors.castToList();
    }

    public List<Function<Handlers, List<FunctionHandlerDispatchBuilderInfo>>> getExtraFunctionHandlerDispatchBuilderInfoCollectors()
    {
        return this.extraFunctionHandlerDispatchBuilderInfoCollectors.castToList();
    }

    public List<Function<Handlers, List<FunctionExpressionBuilderRegistrationInfo>>> getExtraFunctionExpressionBuilderRegistrationInfoCollectors()
    {
        return this.extraFunctionExpressionBuilderRegistrationInfoCollectors.castToList();
    }

    public List<Function<Handlers, List<FunctionHandlerRegistrationInfo>>> getExtraFunctionHandlerRegistrationInfoCollectors()
    {
        return this.extraFunctionHandlerRegistrationInfoCollectors.castToList();
    }

    public List<Function4<org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification, CompileContext, List<String>, ProcessingContext, ValueSpecification>> getExtraValueSpecificationProcessors()
    {
        return this.extraValueSpecificationProcessors.castToList();
    }

    public List<Procedure2<PackageableElement, MutableMap<String, String>>> getExtraStoreStatBuilders()
    {
        return this.extraStoreStatBuilders.castToList();
    }

    public List<Function2<ExecutionContext, CompileContext, org.finos.legend.pure.m3.coreinstance.meta.pure.runtime.ExecutionContext>> getExtraExecutionContextProcessors()
    {
        return this.extraExecutionContextProcessors.castToList();
    }

    public List<Procedure<Procedure2<String, List<String>>>> getExtraElementForPathToElementRegisters()
    {
        return this.extraElementForPathToElementRegisters.castToList();
    }

    public List<Procedure3<SetImplementation, Set<String>, CompileContext>> getExtraSetImplementationSourceScanners()
    {
        return this.extraSetImplementationSourceScanners.castToList();
    }

    public List<Procedure2<PureModel, PureModelContextData>> getExtraPostValidators()
    {
        return this.extraPostValidators.castToList();
    }

    public static CompilerExtensions fromExtensions(Iterable<? extends CompilerExtension> extensions)
    {
        return new CompilerExtensions(extensions);
    }

    public static CompilerExtensions fromAvailableExtensions()
    {
        return fromExtensions(ServiceLoader.load(CompilerExtension.class));
    }

    public static void logAvailableExtensions()
    {
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug(LazyIterate.collect(ServiceLoader.load(CompilerExtension.class), extension -> "- " + extension.getClass().getSimpleName()).makeString("Compiler extension(s) loaded:\n", "\n", ""));
        }
    }

    private static MutableMap<java.lang.Class<? extends PackageableElement>, Processor<?>> indexProcessors(Iterable<? extends CompilerExtension> extensions)
    {
        MutableMap<java.lang.Class<? extends PackageableElement>, Processor<?>> index = Maps.mutable.empty();
        for (Processor<?> processor : LazyIterate.flatCollect(extensions, CompilerExtension::getExtraProcessors))
        {
            java.lang.Class<? extends PackageableElement> processorClass = processor.getElementClass();
            if (FORBIDDEN_PROCESSOR_CLASSES.contains(processorClass))
            {
                throw new IllegalArgumentException("Processor not allowed for class: " + processorClass.getName());
            }
            if (index.put(processorClass, processor) != null)
            {
                throw new IllegalArgumentException("Conflicting processors for class: " + processorClass.getName());
            }
        }
        return index;
    }
}
