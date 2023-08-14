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

package org.finos.legend.engine.language.pure.compiler.toPureGraph;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.inference.MostCommonType;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecificationVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.Variable;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.AppliedFunction;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.AppliedProperty;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.AppliedQualifiedProperty;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.UnknownAppliedFunction;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CBoolean;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CByteArray;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CDateTime;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CDecimal;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CFloat;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CInteger;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CLatestDate;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CStrictDate;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CStrictTime;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CString;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Class;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.ClassInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Collection;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Enum;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.EnumValue;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.GenericTypeInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.HackedUnit;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.KeyExpression;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.MappingInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.PrimitiveType;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.UnitInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.UnitType;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Whatever;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.AggregateValue;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.ExecutionContextInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.Pair;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.PureList;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.RuntimeInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.SerializationConfig;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.TDSAggregateValue;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.TDSColumnInformation;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.TDSSortInformation;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.TdsOlapAggregation;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.TdsOlapRank;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.graph.PropertyGraphFetchTree;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.graph.RootGraphFetchTree;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.path.Path;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.path.PropertyPathElement;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_pure_functions_collection_AggregateValue_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_functions_collection_Pair_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_functions_lang_KeyExpression_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_graphFetch_execution_AlloySerializationConfig;
import org.finos.legend.pure.generated.Root_meta_pure_graphFetch_execution_AlloySerializationConfig_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_path_Path_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_path_PropertyPathElement_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_PrimitiveType_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_generics_GenericType_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_valuespecification_SimpleFunctionExpression_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_valuespecification_VariableExpression_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_runtime_ExecutionContext;
import org.finos.legend.pure.generated.Root_meta_pure_runtime_Runtime;
import org.finos.legend.pure.generated.Root_meta_pure_tds_AggregateValue_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_tds_BasicColumnSpecification_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_tds_SortInformation_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_tds_TdsOlapAggregation_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_tds_TdsOlapRank_Impl;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.SimpleFunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecificationAccessor;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.coreinstance.primitive.date.DateFormat;
import org.finos.legend.pure.m4.coreinstance.primitive.date.LatestDate;
import org.finos.legend.pure.m4.coreinstance.primitive.strictTime.StrictTimeFormat;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.finos.legend.pure.generated.core_pure_corefunctions_metaExtension.Root_meta_pure_functions_meta_functionReturnType_Function_1__GenericType_1_;

public class ValueSpecificationBuilder implements ValueSpecificationVisitor<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification>
{
    private final CompileContext context;
    private final MutableList<String> openVariables;
    private final ProcessingContext processingContext;

    public ValueSpecificationBuilder(CompileContext context, MutableList<String> openVariables, ProcessingContext processingContext)
    {
        this.context = context;
        this.openVariables = openVariables;
        this.processingContext = processingContext;
    }

    @Override
    public ValueSpecification visit(org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification valueSpecification)
    {
        return this.context.getCompilerExtensions().getExtraValueSpecificationProcessors().stream()
                .map(processor -> processor.value(valueSpecification, this.context, this.openVariables, this.processingContext))
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new UnsupportedOperationException("Unsupported value specification type '" + valueSpecification.getClass().getSimpleName() + "'"));
    }

    @Override
    public ValueSpecification visit(org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.PackageableElementPtr packageableElementPtr)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement packageableElement = this.context.resolvePackageableElement(packageableElementPtr.fullPath, packageableElementPtr.sourceInformation);

        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::InstanceValue"))
                ._genericType(packageableElement._classifierGenericType())
                ._multiplicity(this.context.pureModel.getMultiplicity("one"))
                ._values(this.processingContext.peek().equals("Applying new") ? FastList.newList() : FastList.newListWith(packageableElement));
    }

    @Override
    public ValueSpecification visit(Whatever whatever)
    {
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::InstanceValue"))
                ._genericType(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))._rawType(this.context.resolveType(whatever._class, whatever.sourceInformation)))
                ._multiplicity(this.context.pureModel.getMultiplicity(whatever.multiplicity))
                ._values(FastList.newListWith());
    }

    @Override
    public ValueSpecification visit(CString cString)
    {
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::InstanceValue"))
                ._genericType(this.context.pureModel.getGenericType("String"))
                ._multiplicity(this.context.pureModel.getMultiplicity(cString.multiplicity))
                ._values(FastList.newListWith(cString.value));
    }

    @Override
    public ValueSpecification visit(CDateTime cDateTime)
    {
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::InstanceValue"))
                ._genericType(this.context.pureModel.getGenericType("DateTime"))
                ._multiplicity(this.context.pureModel.getMultiplicity(cDateTime.multiplicity))
                ._values(Lists.immutable.of(DateFormat.parseDateTime(cDateTime.value)));
    }

    @Override
    public ValueSpecification visit(CLatestDate cLatestDate)
    {
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::InstanceValue"))
                ._genericType(this.context.pureModel.getGenericType("LatestDate"))
                ._multiplicity(this.context.pureModel.getMultiplicity(cLatestDate.multiplicity))
                ._values(Lists.immutable.of(LatestDate.instance));
    }

    @Override
    public ValueSpecification visit(CStrictDate cStrictDate)
    {
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::InstanceValue"))
                ._genericType(this.context.pureModel.getGenericType("StrictDate"))
                ._multiplicity(this.context.pureModel.getMultiplicity(cStrictDate.multiplicity))
                ._values(Lists.immutable.of(DateFormat.parseStrictDate(cStrictDate.value)));
    }

    @Override
    public ValueSpecification visit(CStrictTime cStrictTime)
    {
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::InstanceValue"))
                ._genericType(this.context.pureModel.getGenericType("StrictTime"))
                ._multiplicity(this.context.pureModel.getMultiplicity(cStrictTime.multiplicity))
                ._values(Lists.immutable.of(StrictTimeFormat.parsePureStrictTime(cStrictTime.value)));
    }

    public ValueSpecification processClassInstance(AggregateValue aggregateValue)
    {
        LambdaFunction l = (LambdaFunction) ((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue) aggregateValue.mapFn.accept(new ValueSpecificationBuilder(this.context, openVariables, processingContext)))._values().getFirst();
        LambdaFunction o = (LambdaFunction) ((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue) aggregateValue.aggregateFn.accept(new ValueSpecificationBuilder(this.context, openVariables, processingContext)))._values().getFirst();
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::InstanceValue"))
                ._genericType(this.context.pureModel.getGenericType("meta::pure::functions::collection::AggregateValue"))
                ._multiplicity(this.context.pureModel.getMultiplicity("one"))
                ._values(Lists.mutable.of(new Root_meta_pure_functions_collection_AggregateValue_Impl("", null, context.pureModel.getClass("meta::pure::functions::collection::AggregateValue"))._mapFn(l)._aggregateFn(o)));
    }

    @Override
    public ValueSpecification visit(Class aClass)
    {
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::InstanceValue"))
                ._genericType(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))._rawType(this.context.pureModel.getType("meta::pure::metamodel::type::Class"))._typeArguments(FastList.newListWith(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))._rawType(this.context.resolveType(aClass.fullPath, aClass.sourceInformation)))))
                ._multiplicity(this.context.pureModel.getMultiplicity("one"))
                ._values(this.processingContext.peek().equals("Applying new") ? FastList.newList() : FastList.newListWith(this.context.resolveType(aClass.fullPath, aClass.sourceInformation))); //Align with pure graph, first instance value of new function used for inference only (i.e only need the typeArgument, values should be empty)
    }

    @Override
    public ValueSpecification visit(CBoolean cBoolean)
    {
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::InstanceValue"))
                ._genericType(this.context.pureModel.getGenericType("Boolean"))
                ._multiplicity(this.context.pureModel.getMultiplicity(cBoolean.multiplicity))
                ._values(FastList.newListWith(cBoolean.value));
    }

    @Override
    public ValueSpecification visit(UnknownAppliedFunction unknownAppliedFunction)
    {
        return new Root_meta_pure_metamodel_valuespecification_SimpleFunctionExpression_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::SimpleFunctionExpression"))
                ._func((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<? extends java.lang.Object>) null)
                ._functionName(null)
                ._genericType(this.context.resolveGenericType(unknownAppliedFunction.returnType, unknownAppliedFunction.sourceInformation))
                ._multiplicity(this.context.pureModel.getMultiplicity(unknownAppliedFunction.returnMultiplicity));
    }

    @Override
    public ValueSpecification visit(Enum anEnum)
    {
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::InstanceValue"))
                ._genericType(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))._rawType(this.context.pureModel.getType("meta::pure::metamodel::type::Enumeration"))._typeArguments(FastList.newListWith(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))._rawType(this.context.resolveType(anEnum.fullPath, anEnum.sourceInformation)))))
                ._multiplicity(this.context.pureModel.getMultiplicity("one"))
                ._values(FastList.newListWith(this.context.resolveType(anEnum.fullPath, anEnum.sourceInformation)));
    }

    @Override
    public ValueSpecification visit(EnumValue enumValue)
    {
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::InstanceValue"))
                ._genericType(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))
                        ._rawType(this.context.resolveEnumeration(enumValue.fullPath, enumValue.sourceInformation)))
                ._multiplicity(this.context.pureModel.getMultiplicity("one"))
                ._values(FastList.newListWith(this.context.resolveEnumValue(enumValue.fullPath, enumValue.value, enumValue.sourceInformation, enumValue.sourceInformation)));
    }

    public ValueSpecification processClassInstance(RuntimeInstance runtimeInstance)
    {
        Root_meta_pure_runtime_Runtime _runtime = HelperRuntimeBuilder.buildPureRuntime(runtimeInstance.runtime, this.context);
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::InstanceValue"))
                ._genericType(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))._rawType(this.context.pureModel.getType("meta::pure::runtime::Runtime")))
                ._multiplicity(this.context.pureModel.getMultiplicity("one"))
                ._values(FastList.newListWith(_runtime));
    }


    @Override
    public ValueSpecification visit(ClassInstance iv)
    {
        Function3<Object, CompileContext, ProcessingContext, ValueSpecification> extension = this.context.getCompilerExtensions().getExtraClassInstanceProcessors().get(iv.type);
        if (extension != null)
        {
            return extension.value(iv.value, context, processingContext);
        }
        switch (iv.type)
        {
            case "path":
                return processClassInstance((Path) iv.value);
            case "rootGraphFetchTree":
                return processClassInstance((RootGraphFetchTree) iv.value);
            case "propertyGraphFetchTree":
                return processClassInstance((PropertyGraphFetchTree) iv.value);
            case "keyExpression":
                return visit((KeyExpression) iv.value);
            case "primitiveType":
                return visit((PrimitiveType) iv.value);
            case "listInstance":
                return processClassInstance((PureList) iv.value);
            case "aggregateValue":
                return processClassInstance((AggregateValue) iv.value);
            case "pair":
                return processClassInstance((Pair) iv.value);
            case "runtimeInstance":
                return processClassInstance((RuntimeInstance) iv.value);
            case "executionContextInstance":
                return processClassInstance((ExecutionContextInstance) iv.value);
            case "alloySerializationConfig":
                return processClassInstance((SerializationConfig) iv.value);
            case "tdsAggregateValue":
                return processClassInstance((TDSAggregateValue) iv.value);
            case "tdsColumnInformation":
                return processClassInstance((TDSColumnInformation) iv.value);
            case "tdsSortInformation":
                return processClassInstance((TDSSortInformation) iv.value);
            case "tdsOlapRank":
                return processClassInstance((TdsOlapRank) iv.value);
            case "tdsOlapAggregation":
                return processClassInstance((TdsOlapAggregation) iv.value);
            default:
                throw new RuntimeException("/* Unsupported instance value " + iv.type + " */");
        }
    }

    public ValueSpecification processClassInstance(Path path)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> cl = this.context.resolveClass(path.startType, path.sourceInformation);
        TypeAndList res = ListIterate.injectInto(new TypeAndList(cl), path.path, (a, b) ->
                {
                    AbstractProperty<?> property = HelperModelBuilder.getAppliedProperty(this.context, (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?>) a.currentClass, Optional.empty(), ((PropertyPathElement) b).property);
                    GenericType genericType = Root_meta_pure_functions_meta_functionReturnType_Function_1__GenericType_1_(property, this.context.pureModel.getExecutionSupport());
                    MutableList<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.path.PathElement> result = a.result;
                    org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.path.PropertyPathElement ppe = new Root_meta_pure_metamodel_path_PropertyPathElement_Impl("", new SourceInformation("X", 0, 0, 0, 0), null);
                    RichIterable<ValueSpecification> params = ListIterate.collect(((PropertyPathElement) b).parameters, p -> p.accept(new ValueSpecificationBuilder(this.context, openVariables, processingContext)));
                    result.add(ppe._property(property)._parameters(params));
                    return new TypeAndList(genericType._rawType(), result);
                }
        );

        // Should be the max of all the properties multiplicities!!!
        Multiplicity mul = ((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.path.PropertyPathElement) res.result.getLast())._property()._multiplicity();
        GenericType genericType = new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))
                ._rawType(this.context.pureModel.getType("meta::pure::metamodel::path::Path"))
                ._typeArguments(FastList.newListWith(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))._rawType(cl),
                        new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))._rawType(res.currentClass)))
                ._multiplicityArguments(FastList.newListWith(mul));

        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::InstanceValue"))
                ._genericType(genericType)
                ._multiplicity(this.context.pureModel.getMultiplicity("one"))
                ._values(
                        FastList.newListWith(
                                new Root_meta_pure_metamodel_path_Path_Impl<>("", new SourceInformation("X", 0, 0, 0, 0), null)
                                        ._classifierGenericType(genericType)
                                        ._start(this.context.resolveGenericType(path.startType, path.sourceInformation))
                                        ._name(path.name)
                                        ._path(res.result)
                        )
                );
    }

    class TypeAndList
    {
        Type currentClass;
        MutableList<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.path.PathElement> result;

        public TypeAndList(Type currentClass)
        {
            this.currentClass = currentClass;
            this.result = Lists.mutable.empty();
        }

        public TypeAndList(Type currentClass, MutableList<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.path.PathElement> result)
        {
            this.currentClass = currentClass;
            this.result = result;
        }
    }

    @Override
    public ValueSpecification visit(CInteger cInteger)
    {
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::InstanceValue"))
                ._genericType(this.context.pureModel.getGenericType("Integer"))
                ._multiplicity(this.context.pureModel.getMultiplicity(cInteger.multiplicity))
                ._values(FastList.newListWith(cInteger.value));
    }

    @Override
    public ValueSpecification visit(CDecimal cDecimal)
    {
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::InstanceValue"))
                ._genericType(this.context.pureModel.getGenericType("Decimal"))
                ._multiplicity(this.context.pureModel.getMultiplicity(cDecimal.multiplicity))
                ._values(FastList.newListWith(cDecimal.value));
    }

    @Override
    public ValueSpecification visit(CByteArray cByteArray)
    {
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::InstanceValue"))
                ._genericType(this.context.pureModel.getGenericType("Byte"))
                ._multiplicity(this.context.pureModel.getMultiplicity("zeroMany"))
                ._values(FastList.newListWith(new ByteArrayInputStream(cByteArray.value)));
    }

    public ValueSpecification processClassInstance(SerializationConfig serializationConfig)
    {
        Root_meta_pure_graphFetch_execution_AlloySerializationConfig config = new Root_meta_pure_graphFetch_execution_AlloySerializationConfig_Impl("", null, context.pureModel.getClass("meta::pure::graphFetch::execution::AlloySerializationConfig"));
        config._includeType(serializationConfig.includeType);
        config._typeKeyName(serializationConfig.typeKeyName);
        config._includeEnumType(serializationConfig.includeEnumType);
        config._dateTimeFormat(serializationConfig.dateTimeFormat);
        config._removePropertiesWithNullValues(serializationConfig.removePropertiesWithNullValues);
        config._removePropertiesWithEmptySets(serializationConfig.removePropertiesWithEmptySets);
        config._fullyQualifiedTypePath(serializationConfig.fullyQualifiedTypePath);
        config._includeObjectReference(serializationConfig.includeObjectReference);
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::InstanceValue"))
                ._genericType(this.context.pureModel.getGenericType("meta::pure::graphFetch::execution::AlloySerializationConfig"))
                ._multiplicity(this.context.pureModel.getMultiplicity("one"))
                ._values(Lists.mutable.of(config));
    }

    @Override
    public ValueSpecification visit(Lambda glambda)
    {
        LambdaFunction lambda = HelperValueSpecificationBuilder.buildLambdaWithContext(glambda.body, glambda.parameters, this.context, processingContext);
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::InstanceValue"))
                ._genericType(lambda._classifierGenericType())
                ._multiplicity(this.context.pureModel.getMultiplicity("one"))
                ._values(Lists.fixedSize.of(lambda));
    }

    public ValueSpecification processClassInstance(ExecutionContextInstance executionContextInstance)
    {
        Root_meta_pure_runtime_ExecutionContext _executionContext = HelperValueSpecificationBuilder.processExecutionContext(executionContextInstance.executionContext, this.context);
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::InstanceValue"))
                ._genericType(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))._rawType(this.context.pureModel.getType("meta::pure::runtime::ExecutionContext")))
                ._multiplicity(this.context.pureModel.getMultiplicity("one"))
                ._values(FastList.newListWith(_executionContext));
    }

    public ValueSpecification processClassInstance(Pair pair)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification f = pair.first.accept(new ValueSpecificationBuilder(this.context, Lists.mutable.empty(), processingContext));
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification s = pair.second.accept(new ValueSpecificationBuilder(this.context, Lists.mutable.empty(), processingContext));
        GenericType gt = new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))._rawType(this.context.pureModel.getType("meta::pure::functions::collection::Pair"))
                ._typeArguments(FastList.newListWith(f._genericType(), s._genericType()));
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::InstanceValue"))
                ._genericType(gt)
                ._multiplicity(this.context.pureModel.getMultiplicity("one"))
                ._values(FastList.newListWith(
                        new Root_meta_pure_functions_collection_Pair_Impl("", null, context.pureModel.getClass("meta::pure::functions::collection::Pair"))
                                ._classifierGenericType(gt)
                                ._first(((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue) f)._values().getFirst())
                                ._second(((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue) s)._values().getFirst())));
    }

    public ValueSpecification processClassInstance(PureList pureList)
    {
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl(" ")
                ._genericType(this.context.pureModel.getGenericType("meta::pure::functions::collection::List"))
                ._multiplicity(this.context.pureModel.getMultiplicity("one"))
                ._values(ListIterate.collect(pureList.values, v -> v.accept(new ValueSpecificationBuilder(this.context, openVariables, processingContext))));
    }

    @Override
    public ValueSpecification visit(Variable variable)
    {
        openVariables.add(variable.name);
        if (variable._class != null && variable.multiplicity != null)
        {
            VariableExpression ve = new Root_meta_pure_metamodel_valuespecification_VariableExpression_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::VariableExpression"))
                    ._name(variable.name);
            ve._genericType(this.context.resolveGenericType(this.context.pureModel.addPrefixToTypeReference(variable._class), variable.sourceInformation));
            ve._multiplicity(this.context.pureModel.getMultiplicity(variable.multiplicity));
            processingContext.addInferredVariables(variable.name, ve);
            return ve;
        }
        else
        {
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification vs = processingContext.getInferredVariable(variable.name);
            if (vs == null)
            {
                throw new EngineException("Can't find variable class for variable '" + variable.name + "' in the graph", variable.sourceInformation, EngineErrorType.COMPILATION);
            }
            return vs;
        }
    }

    @Override
    public ValueSpecification visit(CFloat cFloat)
    {
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::InstanceValue"))
                ._genericType(this.context.pureModel.getGenericType("Float"))
                ._multiplicity(this.context.pureModel.getMultiplicity(cFloat.multiplicity))
                ._values(FastList.newListWith(cFloat.value));
    }

    @Override
    public ValueSpecification visit(MappingInstance mappingInstance)
    {
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::InstanceValue"))
                ._genericType(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))._rawType(this.context.pureModel.getType("meta::pure::mapping::Mapping")))
                ._multiplicity(this.context.pureModel.getMultiplicity("one"))
                ._values(FastList.newListWith(this.context.resolveMapping(mappingInstance.fullPath, mappingInstance.sourceInformation)));
    }

    @Override
    public ValueSpecification visit(GenericTypeInstance genericTypeInstance)
    {
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::InstanceValue"))
                ._genericType(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))._rawType(this.context.resolveType(genericTypeInstance.fullPath, genericTypeInstance.sourceInformation)))
                ._multiplicity(this.context.pureModel.getMultiplicity("one"));
    }

    @Override
    public ValueSpecification visit(Collection collection)
    {
        MutableList<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification> transformed = ListIterate.collect(collection.values, new Function<org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification, ValueSpecification>()
        {
            @Override
            public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification valueOf(org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification expression)
            {
                org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification res = expression.accept(new ValueSpecificationBuilder(context, openVariables, processingContext));
                if (res._multiplicity()._lowerBound()._value() != 1 || res._multiplicity()._upperBound()._value() == null || res._multiplicity()._upperBound()._value() != 1)
                {
                    throw new EngineException("Collection element must have a multiplicity [1] - Context:" + processingContext.getStack() + ", multiplicity:" + org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.print(res._multiplicity()), expression.sourceInformation, EngineErrorType.COMPILATION);
                }
                return res;
            }
        });
        GenericType _genericType = collection.values.isEmpty()
                ? this.context.pureModel.getGenericType(M3Paths.Nil)
                : MostCommonType.mostCommon(transformed.collect(ValueSpecificationAccessor::_genericType).distinct(), this.context.pureModel);
        _genericType._classifierGenericType(this.context.pureModel.getGenericType(M3Paths.GenericType));
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::InstanceValue"))
                ._genericType(_genericType)
                ._multiplicity(this.context.pureModel.getMultiplicity(collection.multiplicity))
                ._values(transformed.collect(valueSpecification ->
                        {
                            if (valueSpecification instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue && ((InstanceValue) valueSpecification)._values().size() == 1)
                            {
                                return ((InstanceValue) valueSpecification)._values().getFirst();
                            }
                            return valueSpecification;
                        }
                ));
    }

    @Override
    public ValueSpecification visit(AppliedFunction appliedFunction)
    {
        processingContext.push("Applying " + appliedFunction.function);
        MilestoningDatePropagationHelper.isValidSource(appliedFunction, processingContext);
        if (appliedFunction.function.equals("letFunction"))
        {
            MutableList<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification> vs = ListIterate.collect(appliedFunction.parameters, expression -> expression.accept(new ValueSpecificationBuilder(this.context, openVariables, processingContext)));
            String letName = ((CString) appliedFunction.parameters.get(0)).value;
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification ve = new Root_meta_pure_metamodel_valuespecification_VariableExpression_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::VariableExpression"))._name(letName);
            ve._genericType(vs.get(1)._genericType());
            ve._multiplicity(vs.get(1)._multiplicity());
            processingContext.addInferredVariables(letName, ve);
        }
        org.eclipse.collections.api.tuple.Pair<SimpleFunctionExpression, List<ValueSpecification>> func = this.context.buildFunctionExpression(this.context.pureModel.buildNameForAppliedFunction(appliedFunction.function), appliedFunction.fControl, appliedFunction.parameters, openVariables, appliedFunction.sourceInformation, processingContext);
        processingContext.pop();
        Assert.assertTrue(func != null, () -> "Can't find a match for function '" + appliedFunction.function + "(?)'", appliedFunction.sourceInformation, EngineErrorType.COMPILATION);
        Assert.assertTrue(func.getOne() != null, () -> "Can't find a match for function '" + appliedFunction.function + "(" + (func.getTwo() == null ? "?" : LazyIterate.collect(func.getTwo(), v -> (v._genericType() == null ? "?" : v._genericType()._rawType()._name()) + org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.print(v._multiplicity())).makeString(",")) + ")'", appliedFunction.sourceInformation, EngineErrorType.COMPILATION);
        SimpleFunctionExpression result = func.getOne();
        result.setSourceInformation(SourceInformationHelper.toM3SourceInformation(appliedFunction.sourceInformation));
        MilestoningDatePropagationHelper.updateMilestoningContextFromValidSources(result, processingContext);

        return result;
    }

    @Override
    public ValueSpecification visit(AppliedQualifiedProperty appliedQualifiedProperty)
    {
        return HelperValueSpecificationBuilder.processProperty(this.context, openVariables, processingContext, appliedQualifiedProperty.parameters, appliedQualifiedProperty.qualifiedProperty, appliedQualifiedProperty.sourceInformation);
    }

    public ValueSpecification processClassInstance(PropertyGraphFetchTree propertyGraphFetchTree)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.graphFetch.GraphFetchTree tree = HelperValueSpecificationBuilder.buildGraphFetchTree(propertyGraphFetchTree, this.context, null, openVariables, processingContext);
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::InstanceValue"))
                ._genericType(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))._rawType(this.context.pureModel.getType("meta::pure::graphFetch::PropertyGraphFetchTree")))
                ._multiplicity(this.context.pureModel.getMultiplicity("one"))
                ._values(FastList.newListWith(tree));
    }

    public ValueSpecification processClassInstance(RootGraphFetchTree rootGraphFetchTree)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.graphFetch.GraphFetchTree tree = HelperValueSpecificationBuilder.buildGraphFetchTree(rootGraphFetchTree, this.context, null, openVariables, processingContext);
        GenericType genericType = new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))
                                      ._rawType(this.context.pureModel.getType("meta::pure::graphFetch::RootGraphFetchTree"))
                                      ._typeArguments(FastList.newListWith(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))
                                      ._rawType(context.resolveClass(rootGraphFetchTree._class, rootGraphFetchTree.sourceInformation))));
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::InstanceValue"))
                ._genericType(genericType)
                ._multiplicity(this.context.pureModel.getMultiplicity("one"))
                ._values(FastList.newListWith(tree));
    }

    @Override
    public ValueSpecification visit(AppliedProperty appliedProperty)
    {
        return HelperValueSpecificationBuilder.processProperty(this.context, openVariables, processingContext, appliedProperty.parameters, appliedProperty.property, appliedProperty.sourceInformation);
    }

    public ValueSpecification processClassInstance(TdsOlapAggregation tdsOlapAggregation)
    {
        LambdaFunction lambda = HelperValueSpecificationBuilder.buildLambdaWithContext(tdsOlapAggregation.function, this.context, processingContext);
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::InstanceValue"))
                ._genericType(this.context.pureModel.getGenericType("meta::pure::tds::TdsOlapAggregation"))
                ._multiplicity(this.context.pureModel.getMultiplicity("one"))
                ._values(Lists.mutable.of(new Root_meta_pure_tds_TdsOlapAggregation_Impl<>("")._func(lambda)._colName(tdsOlapAggregation.columnName)));
    }

    public ValueSpecification processClassInstance(TDSAggregateValue tdsAggregateValue)
    {
        LambdaFunction l = (LambdaFunction) ((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue) tdsAggregateValue.mapFn.accept(new ValueSpecificationBuilder(this.context, openVariables, processingContext)))._values().getFirst();
        LambdaFunction o = (LambdaFunction) ((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue) tdsAggregateValue.aggregateFn.accept(new ValueSpecificationBuilder(this.context, openVariables, processingContext)))._values().getFirst();
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::InstanceValue"))
                ._genericType(this.context.pureModel.getGenericType("meta::pure::tds::AggregateValue"))
                ._multiplicity(this.context.pureModel.getMultiplicity("one"))
                ._values(Lists.mutable.of(new Root_meta_pure_tds_AggregateValue_Impl("", null, context.pureModel.getClass("meta::pure::tds::AggregateValue"))._name(tdsAggregateValue.name)._mapFn(l)._aggregateFn(o)));
    }

    public ValueSpecification processClassInstance(TDSSortInformation tdsSortInformation)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum dirEnum = this.context.pureModel.getEnumValue("meta::pure::tds::SortDirection", tdsSortInformation.direction);
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::InstanceValue"))
                ._genericType(this.context.pureModel.getGenericType("meta::pure::tds::SortInformation"))
                ._multiplicity(this.context.pureModel.getMultiplicity("one"))
                ._values(Lists.mutable.of(new Root_meta_pure_tds_SortInformation_Impl("", null, context.pureModel.getClass("meta::pure::tds::SortInformation"))._column(tdsSortInformation.column)._direction(dirEnum)));
    }

    public ValueSpecification processClassInstance(TDSColumnInformation tdsColumnInformation)
    {
        LambdaFunction l = (LambdaFunction) ((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue) tdsColumnInformation.columnFn.accept(new ValueSpecificationBuilder(this.context, openVariables, processingContext)))._values().getFirst();
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::InstanceValue"))
                ._genericType(this.context.pureModel.getGenericType("meta::pure::tds::BasicColumnSpecification"))
                ._multiplicity(this.context.pureModel.getMultiplicity("one"))
                ._values(Lists.mutable.of(new Root_meta_pure_tds_BasicColumnSpecification_Impl("", null, context.pureModel.getClass("meta::pure::tds::BasicColumnSpecification"))._name(tdsColumnInformation.name)._func(l)));
    }

    public ValueSpecification processClassInstance(TdsOlapRank tdsOlapRank)
    {
        LambdaFunction lambda = HelperValueSpecificationBuilder.buildLambdaWithContext(tdsOlapRank.function, this.context, processingContext);
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::InstanceValue"))
                ._genericType(this.context.pureModel.getGenericType("meta::pure::tds::TdsOlapRank"))
                ._multiplicity(this.context.pureModel.getMultiplicity("one"))
                ._values(Lists.mutable.of(new Root_meta_pure_tds_TdsOlapRank_Impl("", null, context.pureModel.getClass("meta::pure::tds::TdsOlapRank"))._func(lambda)));
    }

    @Override
    public ValueSpecification visit(HackedUnit hackedUnit)
    {
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::InstanceValue"))
                ._genericType(this.context.resolveGenericType(hackedUnit.fullPath, hackedUnit.sourceInformation))
                ._multiplicity(this.context.pureModel.getMultiplicity("one"))
                ._values(FastList.newList());
    }

    @Override
    public ValueSpecification visit(UnitInstance unitInstance)
    {
        FastList<Long> values = FastList.newList();
        if (unitInstance.unitValue != null)
        {
            values.add(unitInstance.unitValue);
        }
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::InstanceValue"))
                ._genericType(this.context.resolveGenericType(unitInstance.unitType, unitInstance.sourceInformation))
                ._multiplicity(this.context.pureModel.getMultiplicity("one"))
                ._values(values);
    }

    @Override
    public ValueSpecification visit(UnitType unitType)
    {
        FastList<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Unit> values = FastList.newList();
        values.add((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Unit) this.context.resolveType(unitType.fullPath, unitType.sourceInformation));
        GenericType unitGenericType = new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))._rawType(this.context.pureModel.getType("meta::pure::metamodel::type::Unit"));
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::InstanceValue"))
                ._genericType(unitGenericType)
                ._multiplicity(this.context.pureModel.getMultiplicity("one"))
                ._values(values);
    }

    @Override
    public ValueSpecification visit(KeyExpression keyExpression)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification key = keyExpression.key.accept(new ValueSpecificationBuilder(this.context, openVariables, processingContext));
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification expression = keyExpression.expression.accept(new ValueSpecificationBuilder(this.context, openVariables, processingContext));
        FastList<org.finos.legend.pure.m3.coreinstance.meta.pure.functions.lang.KeyExpression> values = FastList.newList();
        values.add(new Root_meta_pure_functions_lang_KeyExpression_Impl("", null, context.pureModel.getClass("meta::pure::functions::lang::KeyExpression"))._add(keyExpression.add)._key((InstanceValue) key)._expression(expression));
        GenericType keyExpressionGenericType = new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))._rawType(this.context.pureModel.getType("meta::pure::functions::lang::KeyExpression"));
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::InstanceValue"))
                ._genericType(keyExpressionGenericType)
                ._multiplicity(this.context.pureModel.getMultiplicity("one"))
                ._values(values);
    }

    @Override
    public ValueSpecification visit(PrimitiveType primitiveType)
    {
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::InstanceValue"))
                ._genericType(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))._rawType(this.context.pureModel.getType("meta::pure::metamodel::type::PrimitiveType")))
                ._multiplicity(this.context.pureModel.getMultiplicity("one"))
                ._values(FastList.newListWith(new Root_meta_pure_metamodel_type_PrimitiveType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::PrimitiveType"))
                        ._name(primitiveType.fullPath)));
    }
}
