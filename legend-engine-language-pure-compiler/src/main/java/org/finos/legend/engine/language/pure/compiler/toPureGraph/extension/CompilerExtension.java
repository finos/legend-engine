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
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.tuple.Pair;
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
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.executionContext.ExecutionContext;
import org.finos.legend.engine.shared.core.function.Function4;
import org.finos.legend.engine.shared.core.function.Procedure3;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.AssociationImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.EmbeddedSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.runtime.Connection;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public interface CompilerExtension
{
    /**
     * NOTE: This processor is the only one that returns the element as we will use the result to determine if an element is supported
     */
    default List<Function2<PackageableElement, CompileContext, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement>> getExtraPackageableElementFirstPassProcessors()
    {
        return new ArrayList<>();
    }

    default List<Procedure2<PackageableElement, CompileContext>> getExtraPackageableElementSecondPassProcessors()
    {
        return new ArrayList<>();
    }

    default List<Procedure2<PackageableElement, CompileContext>> getExtraPackageableElementThirdPassProcessors()
    {
        return new ArrayList<>();
    }

    default List<Procedure2<PackageableElement, CompileContext>> getExtraPackageableElementFourthPassProcessors()
    {
        return new ArrayList<>();
    }

    default List<Procedure2<PackageableElement, CompileContext>> getExtraPackageableElementFifthPassProcessors()
    {
        return new ArrayList<>();
    }

    default List<Function4<ValueSpecification, CompileContext, List<String>, ProcessingContext, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification>> getExtraValueSpecificationProcessors()
    {
        return new ArrayList<>();
    }

    default List<Function3<ClassMapping, Mapping, CompileContext, Pair<SetImplementation, RichIterable<EmbeddedSetImplementation>>>> getExtraClassMappingFirstPassProcessors()
    {
        return new ArrayList<>();
    }

    default List<Procedure3<ClassMapping, Mapping, CompileContext>> getExtraClassMappingSecondPassProcessors()
    {
        return new ArrayList<>();
    }

    default List<Function3<AssociationMapping, Mapping, CompileContext, AssociationImplementation>> getExtraAssociationMappingProcessors()
    {
        return new ArrayList<>();
    }

    default List<Function2<org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection, CompileContext, Connection>> getExtraConnectionValueProcessors()
    {
        return new ArrayList<>();
    }

    default List<Procedure2<InputData, CompileContext>> getExtraMappingTestInputDataProcessors()
    {
        return new ArrayList<>();
    }

    default List<Function<Handlers, List<FunctionHandlerDispatchBuilderInfo>>> getExtraFunctionHandlerDispatchBuilderInfoCollectors()
    {
        return new ArrayList<>();
    }

    default List<Function<Handlers, List<FunctionExpressionBuilderRegistrationInfo>>> getExtraFunctionExpressionBuilderRegistrationInfoCollectors()
    {
        return new ArrayList<>();
    }

    default List<Function<Handlers, List<FunctionHandlerRegistrationInfo>>> getExtraFunctionHandlerRegistrationInfoCollectors()
    {
        return new ArrayList<>();
    }

    /**
     * FIXME: to be removed
     */
    @FunctionalInterface
    interface Procedure8<T1, T2, T3, T4, T5, T6, T7, T8>
    {
        void value(T1 var1, T2 var2, T3 var3, T4 var4, T5 var5, T6 var6, T7 var7, T8 var8);
    }

    /**
     * FIXME: to be removed
     */
    @Deprecated
    default List<Procedure8<Type, ValueSpecification, CompileContext, ProcessingContext, List<ValueSpecification>, String, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType, MutableList<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification>>> DEPRECATED_getExtraInferredTypeProcessors()
    {
        return new ArrayList<>();
    }

    // MISC

    default List<Procedure2<PackageableElement, MutableMap<String, String>>> getExtraStoreStatBuilders()
    {
        return new ArrayList<>();
    }

    default List<Function2<ExecutionContext, CompileContext, org.finos.legend.pure.m3.coreinstance.meta.pure.runtime.ExecutionContext>> getExtraExecutionContextProcessors()
    {
        return new ArrayList<>();
    }

    default List<Procedure3<SetImplementation, Set<String>, CompileContext>> getExtraSetImplementationSourceScanners()
    {
        return new ArrayList<>();
    }

    default List<Procedure2<PureModel, PureModelContextData>> getExtraPostValidators()
    {
        return new ArrayList<>();
    }

    /**
     * Needed to register paths for `pathToElement` to work on old graph
     */
    default List<Procedure<Procedure2<String, List<String>>>> getExtraElementForPathToElementRegisters()
    {
        return new ArrayList<>();
    }
}
