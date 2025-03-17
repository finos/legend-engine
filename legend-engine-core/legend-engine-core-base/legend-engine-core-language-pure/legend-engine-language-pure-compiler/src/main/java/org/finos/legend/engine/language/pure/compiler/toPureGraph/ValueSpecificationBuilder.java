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
import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.Handlers;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.inference.MostCommonType;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecificationVisitor;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.Variable;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.AppliedFunction;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.AppliedProperty;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.AppliedQualifiedProperty;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.UnknownAppliedFunction;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.primitive.CBoolean;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.primitive.CByteArray;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.primitive.CDateTime;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.primitive.CDecimal;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.primitive.CFloat;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.primitive.CInteger;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.primitive.CLatestDate;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.primitive.CStrictDate;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.primitive.CStrictTime;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.primitive.CString;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.Class;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.classInstance.ClassInstance;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.Collection;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.Enum;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.EnumValue;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.GenericTypeInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.HackedUnit;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.KeyExpression;
import org.finos.legend.engine.protocol.pure.m3.function.LambdaFunction;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.MappingInstance;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.PackageableElementPtr;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.PrimitiveType;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.UnitInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.UnitType;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.Whatever;
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
import org.finos.legend.engine.protocol.pure.dsl.graph.valuespecification.constant.classInstance.PropertyGraphFetchTree;
import org.finos.legend.engine.protocol.pure.dsl.graph.valuespecification.constant.classInstance.RootGraphFetchTree;
import org.finos.legend.engine.protocol.pure.dsl.path.valuespecification.constant.classInstance.Path;
import org.finos.legend.engine.protocol.pure.dsl.path.valuespecification.constant.classInstance.PropertyPathElement;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.classInstance.relation.ColSpec;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.classInstance.relation.ColSpecArray;
import org.finos.legend.engine.protocol.pure.dsl.store.valuespecification.constant.classInstance.RelationStoreAccessor;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_core_runtime_Runtime;
import org.finos.legend.pure.generated.Root_meta_pure_functions_collection_AggregateValue_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_functions_collection_Pair_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_function_KeyExpression_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_graphFetch_execution_AlloySerializationConfig;
import org.finos.legend.pure.generated.Root_meta_pure_graphFetch_execution_AlloySerializationConfig_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_path_Path_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_path_PropertyPathElement_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_relation_AggColSpecArray_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_relation_AggColSpec_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_relation_ColSpecArray_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_relation_FuncColSpecArray_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_relation_FuncColSpec_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_PrimitiveType_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_generics_GenericType_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_valuespecification_SimpleFunctionExpression_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_valuespecification_VariableExpression_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_runtime_ExecutionContext;
import org.finos.legend.pure.generated.Root_meta_pure_tds_AggregateValue_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_tds_BasicColumnSpecification_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_tds_SortInformation_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_tds_TdsOlapAggregation_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_tds_TdsOlapRank_Impl;
import org.finos.legend.pure.generated.core_pure_corefunctions_metaExtension;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.AggColSpec;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.Column;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.FuncColSpec;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Unit;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.SimpleFunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecificationAccessor;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.relation._Column;
import org.finos.legend.pure.m3.navigation.relation._RelationType;
import org.finos.legend.pure.m4.coreinstance.primitive.date.DateFormat;
import org.finos.legend.pure.m4.coreinstance.primitive.date.LatestDate;
import org.finos.legend.pure.m4.coreinstance.primitive.strictTime.StrictTimeFormat;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ValueSpecificationBuilder implements ValueSpecificationVisitor<ValueSpecification>
{
    private final CompileContext context;
    private final MutableList<String> openVariables;
    private final ProcessingContext processingContext;

    public CompileContext getContext()
    {
        return context;
    }

    public MutableList<String> getOpenVariables()
    {
        return openVariables;
    }

    public ProcessingContext getProcessingContext()
    {
        return processingContext;
    }

    public ValueSpecificationBuilder(CompileContext context, MutableList<String> openVariables, ProcessingContext processingContext)
    {
        this.context = context;
        this.openVariables = openVariables;
        this.processingContext = processingContext;
    }

    @Override
    public ValueSpecification visit(org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecification valueSpecification)
    {
        return this.context.getCompilerExtensions().getExtraValueSpecificationProcessors().stream()
                .map(processor -> processor.value(valueSpecification, this.context, this.openVariables, this.processingContext))
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new UnsupportedOperationException("Unsupported value specification type '" + valueSpecification.getClass().getSimpleName() + "'"));
    }

    @Override
    public ValueSpecification visit(PackageableElementPtr packageableElementPtr)
    {
        if (packageableElementPtr.fullPath.contains("~"))
        {
            // for backward compatibility, since some protocol versions use PackageableElementPtr for units
            return visit(new UnitType(packageableElementPtr.fullPath, packageableElementPtr.sourceInformation));
        }
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement packageableElement = this.context.resolvePackageableElement(packageableElementPtr.fullPath, packageableElementPtr.sourceInformation);

        MutableList<InstanceValue> values = this.context.getCompilerExtensions().getExtraValueSpecificationBuilderForFuncExpr().collect(x -> x.value(packageableElement, this.context, this.processingContext), Lists.mutable.empty());
        values.removeIf(Objects::isNull);
        switch (values.size())
        {
            case 0:
            {
                return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", SourceInformationHelper.toM3SourceInformation(packageableElementPtr.sourceInformation), this.context.pureModel.getClass(M3Paths.InstanceValue))
                        ._genericType(packageableElement._classifierGenericType())
                        ._multiplicity(this.context.pureModel.getMultiplicity("one"))
                        ._values(Lists.mutable.with(packageableElement));
            }
            case 1:
            {
                return values.get(0);
            }
            default:
            {
                throw new EngineException("More than one handler found for the Packageable Element '" + packageableElementPtr.fullPath + "'", packageableElementPtr.sourceInformation, EngineErrorType.COMPILATION);
            }
        }
    }

    @Override
    public ValueSpecification visit(Whatever whatever)
    {
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", SourceInformationHelper.toM3SourceInformation(whatever.sourceInformation), this.context.pureModel.getClass(M3Paths.InstanceValue))
                ._genericType(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, this.context.pureModel.getClass(M3Paths.GenericType))._rawType(this.context.resolveType(whatever._class, whatever.sourceInformation)))
                ._multiplicity(this.context.pureModel.getMultiplicity(whatever.multiplicity))
                ._values(Lists.immutable.empty());
    }

    @Override
    public ValueSpecification visit(CString cString)
    {
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", SourceInformationHelper.toM3SourceInformation(cString.sourceInformation), this.context.pureModel.getClass(M3Paths.InstanceValue))
                ._genericType(this.context.pureModel.getGenericType("String"))
                ._multiplicity(this.context.pureModel.getMultiplicity(cString.multiplicity))
                ._values(Lists.immutable.with(cString.value));
    }

    @Override
    public ValueSpecification visit(CDateTime cDateTime)
    {
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", SourceInformationHelper.toM3SourceInformation(cDateTime.sourceInformation), this.context.pureModel.getClass(M3Paths.InstanceValue))
                ._genericType(this.context.pureModel.getGenericType("DateTime"))
                ._multiplicity(this.context.pureModel.getMultiplicity(cDateTime.multiplicity))
                ._values(Lists.immutable.of(DateFormat.parsePureDate(cDateTime.value)));
    }

    @Override
    public ValueSpecification visit(CLatestDate cLatestDate)
    {
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", SourceInformationHelper.toM3SourceInformation(cLatestDate.sourceInformation), this.context.pureModel.getClass(M3Paths.InstanceValue))
                ._genericType(this.context.pureModel.getGenericType("LatestDate"))
                ._multiplicity(this.context.pureModel.getMultiplicity(cLatestDate.multiplicity))
                ._values(Lists.immutable.of(LatestDate.instance));
    }

    @Override
    public ValueSpecification visit(CStrictDate cStrictDate)
    {
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", SourceInformationHelper.toM3SourceInformation(cStrictDate.sourceInformation), this.context.pureModel.getClass(M3Paths.InstanceValue))
                ._genericType(this.context.pureModel.getGenericType("StrictDate"))
                ._multiplicity(this.context.pureModel.getMultiplicity(cStrictDate.multiplicity))
                ._values(Lists.immutable.of(DateFormat.parseStrictDate(cStrictDate.value)));
    }

    @Override
    public ValueSpecification visit(CStrictTime cStrictTime)
    {
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", SourceInformationHelper.toM3SourceInformation(cStrictTime.sourceInformation), this.context.pureModel.getClass(M3Paths.InstanceValue))
                ._genericType(this.context.pureModel.getGenericType("StrictTime"))
                ._multiplicity(this.context.pureModel.getMultiplicity(cStrictTime.multiplicity))
                ._values(Lists.immutable.of(StrictTimeFormat.parsePureStrictTime(cStrictTime.value)));
    }

    public ValueSpecification processClassInstance(AggregateValue aggregateValue)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction<?> l = (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction<?>) ((InstanceValue) aggregateValue.mapFn.accept(this))._values().getFirst();
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction<?> o = (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction<?>) ((InstanceValue) aggregateValue.aggregateFn.accept(this))._values().getFirst();
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", SourceInformationHelper.toM3SourceInformation(aggregateValue.sourceInformation), this.context.pureModel.getClass(M3Paths.InstanceValue))
                ._genericType(this.context.pureModel.getGenericType("meta::pure::functions::collection::AggregateValue"))
                ._multiplicity(this.context.pureModel.getMultiplicity("one"))
                ._values(Lists.immutable.with(new Root_meta_pure_functions_collection_AggregateValue_Impl("", SourceInformationHelper.toM3SourceInformation(aggregateValue.sourceInformation), this.context.pureModel.getClass("meta::pure::functions::collection::AggregateValue"))._mapFn(l)._aggregateFn(o)));
    }

    @Override
    public ValueSpecification visit(Class aClass)
    {
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", SourceInformationHelper.toM3SourceInformation(aClass.sourceInformation), this.context.pureModel.getClass(M3Paths.InstanceValue))
                ._genericType(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, this.context.pureModel.getClass(M3Paths.GenericType))._rawType(this.context.pureModel.getType(M3Paths.Class))._typeArguments(Lists.immutable.with(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, this.context.pureModel.getClass(M3Paths.GenericType))._rawType(this.context.resolveType(aClass.fullPath, aClass.sourceInformation)))))
                ._multiplicity(this.context.pureModel.getMultiplicity("one"))
                ._values(this.processingContext.peek().equals("Applying new") ? Lists.immutable.empty() : Lists.immutable.with(this.context.resolveType(aClass.fullPath, aClass.sourceInformation))); //Align with pure graph, first instance value of new function used for inference only (i.e only need the typeArgument, values should be empty)
    }

    @Override
    public ValueSpecification visit(CBoolean cBoolean)
    {
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", SourceInformationHelper.toM3SourceInformation(cBoolean.sourceInformation), this.context.pureModel.getClass(M3Paths.InstanceValue))
                ._genericType(this.context.pureModel.getGenericType("Boolean"))
                ._multiplicity(this.context.pureModel.getMultiplicity(cBoolean.multiplicity))
                ._values(Lists.immutable.with(cBoolean.value));
    }

    @Override
    public ValueSpecification visit(UnknownAppliedFunction unknownAppliedFunction)
    {
        return new Root_meta_pure_metamodel_valuespecification_SimpleFunctionExpression_Impl("", SourceInformationHelper.toM3SourceInformation(unknownAppliedFunction.sourceInformation), this.context.pureModel.getClass(M3Paths.SimpleFunctionExpression))
                ._func((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<? extends java.lang.Object>) null)
                ._functionName(null)
                ._genericType(this.context.resolveGenericType(unknownAppliedFunction.returnType, unknownAppliedFunction.sourceInformation))
                ._multiplicity(this.context.pureModel.getMultiplicity(unknownAppliedFunction.returnMultiplicity));
    }

    @Override
    public ValueSpecification visit(Enum anEnum)
    {
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", SourceInformationHelper.toM3SourceInformation(anEnum.sourceInformation), this.context.pureModel.getClass(M3Paths.InstanceValue))
                ._genericType(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, this.context.pureModel.getClass(M3Paths.GenericType))._rawType(this.context.pureModel.getType(M3Paths.Enumeration))._typeArguments(Lists.immutable.with(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, this.context.pureModel.getClass(M3Paths.GenericType))._rawType(this.context.resolveType(anEnum.fullPath, anEnum.sourceInformation)))))
                ._multiplicity(this.context.pureModel.getMultiplicity("one"))
                ._values(Lists.immutable.with(this.context.resolveType(anEnum.fullPath, anEnum.sourceInformation)));
    }

    @Override
    public ValueSpecification visit(EnumValue enumValue)
    {
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", SourceInformationHelper.toM3SourceInformation(enumValue.sourceInformation), this.context.pureModel.getClass(M3Paths.InstanceValue))
                ._genericType(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, this.context.pureModel.getClass(M3Paths.GenericType))
                        ._rawType(this.context.resolveEnumeration(enumValue.fullPath, enumValue.sourceInformation)))
                ._multiplicity(this.context.pureModel.getMultiplicity("one"))
                ._values(Lists.immutable.with(this.context.resolveEnumValue(enumValue.fullPath, enumValue.value, enumValue.sourceInformation, enumValue.sourceInformation)));
    }

    public ValueSpecification processClassInstance(RuntimeInstance runtimeInstance)
    {
        Root_meta_core_runtime_Runtime _runtime = HelperRuntimeBuilder.buildPureRuntime(runtimeInstance.runtime, this.context);
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", SourceInformationHelper.toM3SourceInformation(runtimeInstance.sourceInformation), this.context.pureModel.getClass(M3Paths.InstanceValue))
                ._genericType(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, this.context.pureModel.getClass(M3Paths.GenericType))._rawType(this.context.pureModel.getType("meta::core::runtime::Runtime")))
                ._multiplicity(this.context.pureModel.getMultiplicity("one"))
                ._values(Lists.immutable.with(_runtime));
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
            {
                return processClassInstance((Path) iv.value);
            }
            case "rootGraphFetchTree":
            {
                return processClassInstance((RootGraphFetchTree) iv.value);
            }
            case ">":
            {
                return processRelationStoreAccessor((RelationStoreAccessor) iv.value);
            }
            case "colSpec":
            {
                return proccessColSpec((ColSpec) iv.value);
            }
            case "colSpecArray":
            {
                return proccessColSpecArray((ColSpecArray) iv.value);
            }
            case "propertyGraphFetchTree":
            {
                return processClassInstance((PropertyGraphFetchTree) iv.value);
            }
            case "keyExpression":
            {
                return visit((KeyExpression) iv.value);
            }
            case "primitiveType":
            {
                return visit((PrimitiveType) iv.value);
            }
            case "listInstance":
            {
                return processClassInstance((PureList) iv.value);
            }
            case "aggregateValue":
            {
                return processClassInstance((AggregateValue) iv.value);
            }
            case "pair":
            {
                return processClassInstance((Pair) iv.value);
            }
            case "runtimeInstance":
            {
                return processClassInstance((RuntimeInstance) iv.value);
            }
            case "executionContextInstance":
            {
                return processClassInstance((ExecutionContextInstance) iv.value);
            }
            case "alloySerializationConfig":
            {
                return processClassInstance((SerializationConfig) iv.value);
            }
            case "tdsAggregateValue":
            {
                return processClassInstance((TDSAggregateValue) iv.value);
            }
            case "tdsColumnInformation":
            {
                return processClassInstance((TDSColumnInformation) iv.value);
            }
            case "tdsSortInformation":
            {
                return processClassInstance((TDSSortInformation) iv.value);
            }
            case "tdsOlapRank":
            {
                return processClassInstance((TdsOlapRank) iv.value);
            }
            case "tdsOlapAggregation":
            {
                return processClassInstance((TdsOlapAggregation) iv.value);
            }
            default:
            {
                throw new RuntimeException("/* Unsupported instance value " + iv.type + " */");
            }
        }
    }

    private ValueSpecification proccessColSpecArray(ColSpecArray value)
    {
        ProcessorSupport processorSupport = this.context.pureModel.getExecutionSupport().getProcessorSupport();

        MutableList<ValueSpecification> cols = ListIterate.collect(value.colSpecs, this::proccessColSpec);
        RichIterable<?> processedValues = cols.flatCollect(v -> ((InstanceValue) v)._values());
        Object resO = processedValues.getFirst();

        String className = resO instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.ColSpec ?
                "meta::pure::metamodel::relation::ColSpecArray" :
                resO instanceof AggColSpec ?
                        "meta::pure::metamodel::relation::AggColSpecArray" :
                        "meta::pure::metamodel::relation::FuncColSpecArray";

        GenericType inferredType = new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, this.context.pureModel.getClass(M3Paths.GenericType))
                ._rawType(
                        _RelationType.build(
                                cols.collect(c ->
                                {
                                    Column<?, ?> theCol = ((RelationType<?>) c._genericType()._typeArguments().getLast()._rawType())._columns().getFirst();
                                    return _Column.getColumnInstance(theCol._name(), false, _Column.getColumnType(theCol), _Column.getColumnMultiplicity(theCol), null, processorSupport);
                                }),
                                null,
                                processorSupport
                        )
                );

        Object valueToInsert = null;
        GenericType colSpecGT = null;
        if (resO instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.ColSpec)
        {
            colSpecGT = buildColArrayGenType(className, Lists.mutable.with(inferredType));
            valueToInsert = new Root_meta_pure_metamodel_relation_ColSpecArray_Impl<>("")
                    ._classifierGenericType(colSpecGT)
                    ._names(processedValues.collect(c -> ((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.ColSpec<?>) c)._name()));
        }
        else if (resO instanceof FuncColSpec)
        {
            colSpecGT = buildColArrayGenType(className,
                    Lists.mutable.with(
                            MostCommonType.mostCommon(processedValues.collect(x -> ((FuncColSpec<?, ?>) x)._function()._classifierGenericType()).toList(), this.context.pureModel)._typeArguments().getFirst(),
                            inferredType
                    )
            );
            valueToInsert = new Root_meta_pure_metamodel_relation_FuncColSpecArray_Impl<>("")
                    ._classifierGenericType(colSpecGT)
                    ._funcSpecs(processedValues.collect(x -> (FuncColSpec<?, ?>) x));
        }
        else if (resO instanceof AggColSpec)
        {
            colSpecGT = buildColArrayGenType(className,
                    Lists.mutable.with(
                            MostCommonType.mostCommon(processedValues.collect(x -> ((AggColSpec<?, ?, ?>) x)._map()._classifierGenericType()).toList(), this.context.pureModel)._typeArguments().getFirst(),
                            MostCommonType.mostCommon(processedValues.collect(x -> ((AggColSpec<?, ?, ?>) x)._reduce()._classifierGenericType()).toList(), this.context.pureModel)._typeArguments().getFirst(),
                            inferredType
                    )
            );
            valueToInsert = new Root_meta_pure_metamodel_relation_AggColSpecArray_Impl<>("")
                    ._classifierGenericType(colSpecGT)
                    ._aggSpecs(processedValues.collect(x -> (AggColSpec<?, ?, ?>) x));
        }
        else
        {
            throw new RuntimeException("Not Possible: " + resO.getClass());
        }

        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", null, this.context.pureModel.getClass(M3Paths.InstanceValue))
                ._multiplicity(context.pureModel.getMultiplicity("one"))
                ._genericType(colSpecGT)
                ._values(Lists.mutable.with(
                                valueToInsert
                        )
                );
    }

    private GenericType buildColArrayGenType(String className, MutableList<GenericType> args)
    {
        return new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, this.context.pureModel.getClass(M3Paths.GenericType))
                ._rawType(context.pureModel.getClass(className))
                ._typeArguments(args);
    }

    private ValueSpecification proccessColSpec(ColSpec colSpec)
    {
        ProcessorSupport processorSupport = this.context.pureModel.getExecutionSupport().getProcessorSupport();
        if (colSpec.function1 == null)
        {
            GenericType gt = null;
            if (colSpec.genericType != null)
            {
                gt = this.context.newGenericType(colSpec.genericType);
                gt.setSourceInformation(SourceInformationHelper.toM3SourceInformation(colSpec.sourceInformation));
            }
            return Handlers.wrapInstanceValue(Handlers.buildColSpec(colSpec.name, gt, (Multiplicity) org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.newMultiplicity(0, 1, processorSupport), this.context.pureModel, this.context.pureModel.getExecutionSupport().getProcessorSupport()), this.context.pureModel);
        }
        else if (colSpec.function2 == null)
        {
            InstanceValue funcVS = (InstanceValue) colSpec.function1.accept(this);
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition<?> func = (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition<?>) funcVS._values().getFirst();

            FunctionType func1Type = (FunctionType) org.finos.legend.pure.m3.navigation.function.Function.computeFunctionType(func, this.context.pureModel.getExecutionSupport().getProcessorSupport());

            GenericType colSpecGT = new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, this.context.pureModel.getClass(M3Paths.GenericType))
                    ._rawType(context.pureModel.getClass("meta::pure::metamodel::relation::FuncColSpec"))
                    ._typeArguments(
                            Lists.immutable.with(
                                    new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, this.context.pureModel.getClass(M3Paths.GenericType))._rawType(func1Type),
                                    new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, this.context.pureModel.getClass(M3Paths.GenericType))
                                            ._rawType(
                                                    _RelationType.build(
                                                            Lists.immutable.with(_Column.getColumnInstance(colSpec.name, false, Handlers.funcReturnType(funcVS, this.context.pureModel), Handlers.funcReturnMul(funcVS, this.context.pureModel), null, processorSupport)),
                                                            null,
                                                            processorSupport
                                                    )
                                            )
                            )
                    );

            return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", null, this.context.pureModel.getClass(M3Paths.InstanceValue))
                    ._multiplicity(context.pureModel.getMultiplicity("one"))
                    ._genericType(colSpecGT)
                    ._values(Lists.immutable.with(
                                    new Root_meta_pure_metamodel_relation_FuncColSpec_Impl<>("", null, this.context.pureModel.getClass("meta::pure::metamodel::relation::FuncColSpec"))
                                            ._classifierGenericType(colSpecGT)
                                            ._name(colSpec.name)
                                            ._function(func)
                            )
                    );
        }
        else
        {
            InstanceValue funcVS = (InstanceValue) colSpec.function1.accept(this);
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition<?> func1 = (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition<?>) funcVS._values().getFirst();

            InstanceValue func2VS = (InstanceValue) colSpec.function2.accept(this);
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition<?> func2 = (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition<?>) func2VS._values().getFirst();

            FunctionType func1Type = (FunctionType) org.finos.legend.pure.m3.navigation.function.Function.computeFunctionType(func1, this.context.pureModel.getExecutionSupport().getProcessorSupport());
            FunctionType func2Type = (FunctionType) org.finos.legend.pure.m3.navigation.function.Function.computeFunctionType(func2, this.context.pureModel.getExecutionSupport().getProcessorSupport());

            GenericType aggColSpecGT = new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, this.context.pureModel.getClass(M3Paths.GenericType))
                    ._rawType(context.pureModel.getClass("meta::pure::metamodel::relation::AggColSpec"))
                    ._typeArguments(
                            Lists.immutable.with(
                                    new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, this.context.pureModel.getClass(M3Paths.GenericType))._rawType(func1Type),
                                    new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, this.context.pureModel.getClass(M3Paths.GenericType))._rawType(func2Type),
                                    new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, this.context.pureModel.getClass(M3Paths.GenericType))
                                            ._rawType(
                                                    _RelationType.build(
                                                            Lists.immutable.with(_Column.getColumnInstance(colSpec.name, false, Handlers.funcReturnType(func2VS, this.context.pureModel), Handlers.funcReturnMul(func2VS, this.context.pureModel), null, processorSupport)),
                                                            null,
                                                            processorSupport
                                                    )
                                            )
                            )
                    );

            return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", null, this.context.pureModel.getClass(M3Paths.InstanceValue))
                    ._multiplicity(context.pureModel.getMultiplicity("one"))
                    ._genericType(aggColSpecGT)
                    ._values(Lists.immutable.with(
                                    new Root_meta_pure_metamodel_relation_AggColSpec_Impl<>("", null, this.context.pureModel.getClass("meta::pure::metamodel::relation::AggColSpec"))
                                            ._classifierGenericType(aggColSpecGT)
                                            ._name(colSpec.name)
                                            ._map(func1)
                                            ._reduce(func2)
                            )
                    );
        }
    }

    private ValueSpecification processRelationStoreAccessor(RelationStoreAccessor value)
    {
        String element = value.path.get(0);
        Store store = this.context.pureModel.getStore(element, value.sourceInformation);
        return this.context.getCompilerExtensions().getExtraRelationStoreAccessorProcessors().stream()
                .map(processor -> processor.value(value, store, this.context, this.processingContext))
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new UnsupportedOperationException("Unsupported store '" + value.path.get(0) + "' (type:" + store._classifierGenericType()._rawType().getName() + ") for RelationStoreAccessor '" + Iterate.makeString(value.path, ".") + "'"));
    }

    public ValueSpecification processClassInstance(Path path)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> cl = this.context.resolveClass(path.startType, path.sourceInformation);
        TypeAndList res = ListIterate.injectInto(new TypeAndList(cl), path.path, (a, b) ->
                {
                    AbstractProperty<?> property = HelperModelBuilder.getAppliedProperty(this.context, (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?>) a.currentClass, Optional.empty(), ((PropertyPathElement) b).property);
                    GenericType genericType = core_pure_corefunctions_metaExtension.Root_meta_pure_functions_meta_functionReturnType_Function_1__GenericType_1_(property, this.context.pureModel.getExecutionSupport());
                    MutableList<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.path.PathElement> result = a.result;
                    org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.path.PropertyPathElement ppe = new Root_meta_pure_metamodel_path_PropertyPathElement_Impl("", SourceInformationHelper.toM3SourceInformation(b.sourceInformation), null);
                    RichIterable<ValueSpecification> params = ListIterate.collect(((PropertyPathElement) b).parameters, p -> p.accept(this));
                    result.add(ppe._property(property)._parameters(params));
                    return new TypeAndList(genericType._rawType(), result);
                }
        );

        // Should be the max of all the properties multiplicities!!!
        Multiplicity mul = ((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.path.PropertyPathElement) res.result.getLast())._property()._multiplicity();
        GenericType genericType = new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, this.context.pureModel.getClass(M3Paths.GenericType))
                ._rawType(this.context.pureModel.getType("meta::pure::metamodel::path::Path"))
                ._typeArguments(Lists.immutable.with(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, this.context.pureModel.getClass(M3Paths.GenericType))._rawType(cl),
                        new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, this.context.pureModel.getClass(M3Paths.GenericType))._rawType(res.currentClass)))
                ._multiplicityArguments(Lists.immutable.with(mul));

        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", SourceInformationHelper.toM3SourceInformation(path.sourceInformation), this.context.pureModel.getClass(M3Paths.InstanceValue))
                ._genericType(genericType)
                ._multiplicity(this.context.pureModel.getMultiplicity("one"))
                ._values(
                        Lists.immutable.with(
                                new Root_meta_pure_metamodel_path_Path_Impl<>("", SourceInformationHelper.toM3SourceInformation(path.sourceInformation), null)
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
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", SourceInformationHelper.toM3SourceInformation(cInteger.sourceInformation), this.context.pureModel.getClass(M3Paths.InstanceValue))
                ._genericType(this.context.pureModel.getGenericType("Integer"))
                ._multiplicity(this.context.pureModel.getMultiplicity(cInteger.multiplicity))
                ._values(Lists.immutable.with(cInteger.value));
    }

    @Override
    public ValueSpecification visit(CDecimal cDecimal)
    {
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", SourceInformationHelper.toM3SourceInformation(cDecimal.sourceInformation), this.context.pureModel.getClass(M3Paths.InstanceValue))
                ._genericType(this.context.pureModel.getGenericType("Decimal"))
                ._multiplicity(this.context.pureModel.getMultiplicity(cDecimal.multiplicity))
                ._values(Lists.immutable.with(cDecimal.value));
    }

    @Override
    public ValueSpecification visit(CByteArray cByteArray)
    {
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", SourceInformationHelper.toM3SourceInformation(cByteArray.sourceInformation), this.context.pureModel.getClass(M3Paths.InstanceValue))
                ._genericType(this.context.pureModel.getGenericType("Byte"))
                ._multiplicity(this.context.pureModel.getMultiplicity("zeroMany"))
                ._values(Lists.immutable.with(new ByteArrayInputStream(cByteArray.value)));
    }

    public ValueSpecification processClassInstance(SerializationConfig serializationConfig)
    {
        Root_meta_pure_graphFetch_execution_AlloySerializationConfig config = new Root_meta_pure_graphFetch_execution_AlloySerializationConfig_Impl("", SourceInformationHelper.toM3SourceInformation(serializationConfig.sourceInformation), this.context.pureModel.getClass("meta::pure::graphFetch::execution::AlloySerializationConfig"));
        config._includeType(serializationConfig.includeType);
        config._typeKeyName(serializationConfig.typeKeyName);
        config._includeEnumType(serializationConfig.includeEnumType);
        config._dateTimeFormat(serializationConfig.dateTimeFormat);
        config._removePropertiesWithNullValues(serializationConfig.removePropertiesWithNullValues);
        config._removePropertiesWithEmptySets(serializationConfig.removePropertiesWithEmptySets);
        config._fullyQualifiedTypePath(serializationConfig.fullyQualifiedTypePath);
        config._includeObjectReference(serializationConfig.includeObjectReference);
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", SourceInformationHelper.toM3SourceInformation(serializationConfig.sourceInformation), this.context.pureModel.getClass(M3Paths.InstanceValue))
                ._genericType(this.context.pureModel.getGenericType("meta::pure::graphFetch::execution::AlloySerializationConfig"))
                ._multiplicity(this.context.pureModel.getMultiplicity("one"))
                ._values(Lists.immutable.of(config));
    }

    @Override
    public ValueSpecification visit(LambdaFunction glambda)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction<?> lambda = HelperValueSpecificationBuilder.buildLambdaWithContext(glambda.body, glambda.parameters, this.context, processingContext);
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", SourceInformationHelper.toM3SourceInformation(glambda.sourceInformation), this.context.pureModel.getClass(M3Paths.InstanceValue))
                ._genericType(lambda._classifierGenericType())
                ._multiplicity(this.context.pureModel.getMultiplicity("one"))
                ._values(Lists.fixedSize.of(lambda));
    }

    public ValueSpecification processClassInstance(ExecutionContextInstance executionContextInstance)
    {
        Root_meta_pure_runtime_ExecutionContext _executionContext = HelperValueSpecificationBuilder.processExecutionContext(executionContextInstance.executionContext, this.context);
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", SourceInformationHelper.toM3SourceInformation(executionContextInstance.sourceInformation), this.context.pureModel.getClass(M3Paths.InstanceValue))
                ._genericType(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, this.context.pureModel.getClass(M3Paths.GenericType))._rawType(this.context.pureModel.getType("meta::pure::runtime::ExecutionContext")))
                ._multiplicity(this.context.pureModel.getMultiplicity("one"))
                ._values(Lists.immutable.with(_executionContext));
    }

    public ValueSpecification processClassInstance(Pair pair)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification f = pair.first.accept(this);
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification s = pair.second.accept(this);
        GenericType gt = new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, this.context.pureModel.getClass(M3Paths.GenericType))._rawType(this.context.pureModel.getType(M3Paths.Pair))
                ._typeArguments(Lists.immutable.with(f._genericType(), s._genericType()));
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", SourceInformationHelper.toM3SourceInformation(pair.sourceInformation), this.context.pureModel.getClass(M3Paths.InstanceValue))
                ._genericType(gt)
                ._multiplicity(this.context.pureModel.getMultiplicity("one"))
                ._values(Lists.immutable.with(
                        new Root_meta_pure_functions_collection_Pair_Impl<>("", SourceInformationHelper.toM3SourceInformation(pair.sourceInformation), this.context.pureModel.getClass(M3Paths.Pair))
                                ._classifierGenericType(gt)
                                ._first(((InstanceValue) f)._values().getFirst())
                                ._second(((InstanceValue) s)._values().getFirst())));
    }

    public ValueSpecification processClassInstance(PureList pureList)
    {
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl(" ", SourceInformationHelper.toM3SourceInformation(pureList.sourceInformation), null)
                ._genericType(this.context.pureModel.getGenericType(M3Paths.List))
                ._multiplicity(this.context.pureModel.getMultiplicity("one"))
                ._values(ListIterate.collect(pureList.values, v -> v.accept(this)));
    }

    @Override
    public ValueSpecification visit(Variable variable)
    {
        openVariables.add(variable.name);
        if (variable.genericType != null && variable.multiplicity != null)
        {
            VariableExpression ve = new Root_meta_pure_metamodel_valuespecification_VariableExpression_Impl("", SourceInformationHelper.toM3SourceInformation(variable.sourceInformation), this.context.pureModel.getClass(M3Paths.VariableExpression))
                    ._name(variable.name);
            GenericType genericType = context.newGenericType(variable.genericType);
            ve._genericType(genericType);
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
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", SourceInformationHelper.toM3SourceInformation(cFloat.sourceInformation), this.context.pureModel.getClass(M3Paths.InstanceValue))
                ._genericType(this.context.pureModel.getGenericType("Float"))
                ._multiplicity(this.context.pureModel.getMultiplicity(cFloat.multiplicity))
                ._values(Lists.immutable.with(cFloat.value));
    }

    @Override
    public ValueSpecification visit(MappingInstance mappingInstance)
    {
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", SourceInformationHelper.toM3SourceInformation(mappingInstance.sourceInformation), this.context.pureModel.getClass(M3Paths.InstanceValue))
                ._genericType(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, this.context.pureModel.getClass(M3Paths.GenericType))._rawType(this.context.pureModel.getType("meta::pure::mapping::Mapping")))
                ._multiplicity(this.context.pureModel.getMultiplicity("one"))
                ._values(Lists.immutable.with(this.context.resolveMapping(mappingInstance.fullPath, mappingInstance.sourceInformation)));
    }

    @Override
    public ValueSpecification visit(GenericTypeInstance genericTypeInstance)
    {
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", null, this.context.pureModel.getClass(M3Paths.InstanceValue))
                ._genericType(context.newGenericType(genericTypeInstance.genericType))
                ._multiplicity(this.context.pureModel.getMultiplicity("one"));
    }

    @Override
    public ValueSpecification visit(Collection collection)
    {
        MutableList<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification> transformed = ListIterate.collect(collection.values, expression ->
        {
            ValueSpecification res = expression.accept(this);
            if (res._multiplicity()._lowerBound()._value() != 1 || res._multiplicity()._upperBound()._value() == null || res._multiplicity()._upperBound()._value() != 1)
            {
                throw new EngineException("Collection element must have a multiplicity [1] - Context:" + processingContext.getStack() + ", multiplicity:" + org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.print(res._multiplicity()), expression.sourceInformation, EngineErrorType.COMPILATION);
            }
            return res;
        });
        GenericType _genericType = collection.values.isEmpty()
                ? this.context.pureModel.getGenericType(M3Paths.Nil)
                : MostCommonType.mostCommon(transformed.collect(ValueSpecificationAccessor::_genericType).distinct(), this.context.pureModel);
        _genericType._classifierGenericType(this.context.pureModel.getGenericType(M3Paths.GenericType));
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", null, this.context.pureModel.getClass(M3Paths.InstanceValue))
                ._genericType(_genericType)
                ._multiplicity(this.context.pureModel.getMultiplicity(collection.multiplicity))
                ._values(transformed.collect(valueSpecification ->
                        {
                            if (valueSpecification instanceof InstanceValue && ((InstanceValue) valueSpecification)._values().size() == 1)
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
            MutableList<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification> vs = ListIterate.collect(appliedFunction.parameters, expression -> expression.accept(this));
            String letName = ((CString) appliedFunction.parameters.get(0)).value;
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification ve = new Root_meta_pure_metamodel_valuespecification_VariableExpression_Impl("", SourceInformationHelper.toM3SourceInformation(appliedFunction.sourceInformation), this.context.pureModel.getClass(M3Paths.VariableExpression))._name(letName);
            ve._genericType(vs.get(1)._genericType());
            ve._multiplicity(vs.get(1)._multiplicity());
            processingContext.addInferredVariables(letName, ve);
        }

        org.eclipse.collections.api.tuple.Pair<SimpleFunctionExpression, List<ValueSpecification>> func = this.context.buildFunctionExpression(this.context.pureModel.buildNameForAppliedFunction(appliedFunction.function), appliedFunction.fControl, appliedFunction.parameters, appliedFunction.sourceInformation, this);
        processingContext.pop();
        Assert.assertTrue(func != null, () -> "Can't find a match for function '" + appliedFunction.function + "(?)'", appliedFunction.sourceInformation, EngineErrorType.COMPILATION);
        Assert.assertTrue(func.getOne() != null, () -> "Can't find a match for function '" + appliedFunction.function + "(" + (func.getTwo() == null ? "?" : LazyIterate.collect(func.getTwo(), v -> (v._genericType() == null ? "?" : org.finos.legend.pure.m3.navigation.generictype.GenericType.print(v._genericType(), context.pureModel.getExecutionSupport().getProcessorSupport())) + org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.print(v._multiplicity())).makeString(",")) + ")'", appliedFunction.sourceInformation, EngineErrorType.COMPILATION);
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
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", SourceInformationHelper.toM3SourceInformation(propertyGraphFetchTree.sourceInformation), this.context.pureModel.getClass(M3Paths.InstanceValue))
                ._genericType(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, this.context.pureModel.getClass(M3Paths.GenericType))._rawType(this.context.pureModel.getType("meta::pure::graphFetch::PropertyGraphFetchTree")))
                ._multiplicity(this.context.pureModel.getMultiplicity("one"))
                ._values(Lists.immutable.with(tree));
    }

    public ValueSpecification processClassInstance(RootGraphFetchTree rootGraphFetchTree)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.graphFetch.GraphFetchTree tree = HelperValueSpecificationBuilder.buildGraphFetchTree(rootGraphFetchTree, this.context, null, openVariables, processingContext);
        GenericType genericType = new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, this.context.pureModel.getClass(M3Paths.GenericType))
                ._rawType(this.context.pureModel.getType("meta::pure::graphFetch::RootGraphFetchTree"))
                ._typeArguments(Lists.immutable.with(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, this.context.pureModel.getClass(M3Paths.GenericType))
                        ._rawType(context.resolveClass(rootGraphFetchTree._class, rootGraphFetchTree.sourceInformation))));
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", SourceInformationHelper.toM3SourceInformation(rootGraphFetchTree.sourceInformation), this.context.pureModel.getClass(M3Paths.InstanceValue))
                ._genericType(genericType)
                ._multiplicity(this.context.pureModel.getMultiplicity("one"))
                ._values(Lists.immutable.with(tree));
    }

    @Override
    public ValueSpecification visit(AppliedProperty appliedProperty)
    {
        return HelperValueSpecificationBuilder.processProperty(this.context, openVariables, processingContext, appliedProperty.parameters, appliedProperty.property, appliedProperty.sourceInformation);
    }

    public ValueSpecification processClassInstance(TdsOlapAggregation tdsOlapAggregation)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction<?> lambda = HelperValueSpecificationBuilder.buildLambdaWithContext(tdsOlapAggregation.function, this.context, processingContext);
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", SourceInformationHelper.toM3SourceInformation(tdsOlapAggregation.sourceInformation), this.context.pureModel.getClass(M3Paths.InstanceValue))
                ._genericType(this.context.pureModel.getGenericType("meta::pure::tds::TdsOlapAggregation"))
                ._multiplicity(this.context.pureModel.getMultiplicity("one"))
                ._values(Lists.immutable.of(new Root_meta_pure_tds_TdsOlapAggregation_Impl<>("")._func(lambda)._colName(tdsOlapAggregation.columnName)
                        ._classifierGenericType(this.context.pureModel.getGenericType("meta::pure::tds::TdsOlapAggregation")._typeArguments(FastList.newListWith(
                                ((FunctionType) lambda._classifierGenericType()._typeArguments().getFirst()._rawType())._parameters().getFirst()._genericType()
                        )))));
    }

    public ValueSpecification processClassInstance(TDSAggregateValue tdsAggregateValue)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction<?> l = (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction<?>) ((InstanceValue) tdsAggregateValue.mapFn.accept(this))._values().getFirst();
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction<?> o = (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction<?>) ((InstanceValue) tdsAggregateValue.aggregateFn.accept(this))._values().getFirst();

        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", SourceInformationHelper.toM3SourceInformation(tdsAggregateValue.sourceInformation), this.context.pureModel.getClass(M3Paths.InstanceValue))
                ._genericType(this.context.pureModel.getGenericType("meta::pure::tds::AggregateValue"))
                ._multiplicity(this.context.pureModel.getMultiplicity("one"))
                ._values(Lists.immutable.of(new Root_meta_pure_tds_AggregateValue_Impl("", SourceInformationHelper.toM3SourceInformation(tdsAggregateValue.sourceInformation), this.context.pureModel.getClass("meta::pure::tds::AggregateValue"))
                        ._classifierGenericType(this.context.pureModel.getGenericType("meta::pure::tds::AggregateValue")._typeArguments(FastList.newListWith(
                                ((FunctionType) l._classifierGenericType()._typeArguments().getFirst()._rawType())._returnType(),
                                ((FunctionType) o._classifierGenericType()._typeArguments().getFirst()._rawType())._returnType()
                        )))
                        ._name(tdsAggregateValue.name)
                        ._mapFn(l)
                        ._aggregateFn(o)));
    }

    public ValueSpecification processClassInstance(TDSSortInformation tdsSortInformation)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum dirEnum = this.context.pureModel.getEnumValue("meta::pure::tds::SortDirection", tdsSortInformation.direction);
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", SourceInformationHelper.toM3SourceInformation(tdsSortInformation.sourceInformation), this.context.pureModel.getClass(M3Paths.InstanceValue))
                ._genericType(this.context.pureModel.getGenericType("meta::pure::tds::SortInformation"))
                ._multiplicity(this.context.pureModel.getMultiplicity("one"))
                ._values(Lists.immutable.of(new Root_meta_pure_tds_SortInformation_Impl("", SourceInformationHelper.toM3SourceInformation(tdsSortInformation.sourceInformation), this.context.pureModel.getClass("meta::pure::tds::SortInformation"))._column(tdsSortInformation.column)._direction(dirEnum)));
    }

    public ValueSpecification processClassInstance(TDSColumnInformation tdsColumnInformation)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction<?> l = (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction<?>) ((InstanceValue) tdsColumnInformation.columnFn.accept(this))._values().getFirst();
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", SourceInformationHelper.toM3SourceInformation(tdsColumnInformation.sourceInformation), this.context.pureModel.getClass(M3Paths.InstanceValue))
                ._genericType(this.context.pureModel.getGenericType("meta::pure::tds::BasicColumnSpecification"))
                ._multiplicity(this.context.pureModel.getMultiplicity("one"))
                ._values(Lists.immutable.of(new Root_meta_pure_tds_BasicColumnSpecification_Impl("", SourceInformationHelper.toM3SourceInformation(tdsColumnInformation.sourceInformation), this.context.pureModel.getClass("meta::pure::tds::BasicColumnSpecification"))._name(tdsColumnInformation.name)._func(l)));
    }

    public ValueSpecification processClassInstance(TdsOlapRank tdsOlapRank)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction<?> lambda = HelperValueSpecificationBuilder.buildLambdaWithContext(tdsOlapRank.function, this.context, processingContext);
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", SourceInformationHelper.toM3SourceInformation(tdsOlapRank.sourceInformation), this.context.pureModel.getClass(M3Paths.InstanceValue))
                ._genericType(this.context.pureModel.getGenericType("meta::pure::tds::TdsOlapRank"))
                ._multiplicity(this.context.pureModel.getMultiplicity("one"))
                ._values(Lists.immutable.of(new Root_meta_pure_tds_TdsOlapRank_Impl("", SourceInformationHelper.toM3SourceInformation(tdsOlapRank.sourceInformation), this.context.pureModel.getClass("meta::pure::tds::TdsOlapRank"))
                        ._func(lambda)
                        ._classifierGenericType(this.context.pureModel.getGenericType("meta::pure::tds::TdsOlapRank")._typeArguments(FastList.newListWith(
                                ((FunctionType) lambda._classifierGenericType()._typeArguments().getFirst()._rawType())._parameters().getFirst()._genericType()
                        )))));
    }

    @Override
    public ValueSpecification visit(HackedUnit hackedUnit)
    {
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", SourceInformationHelper.toM3SourceInformation(hackedUnit.sourceInformation), this.context.pureModel.getClass(M3Paths.InstanceValue))
                ._genericType(this.context.resolveGenericType(hackedUnit.fullPath, hackedUnit.sourceInformation))
                ._multiplicity(this.context.pureModel.getMultiplicity("one"))
                ._values(Lists.immutable.empty());
    }

    @Override
    public ValueSpecification visit(UnitInstance unitInstance)
    {
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", SourceInformationHelper.toM3SourceInformation(unitInstance.sourceInformation), this.context.pureModel.getClass(M3Paths.InstanceValue))
                ._genericType(this.context.resolveGenericType(unitInstance.unitType, unitInstance.sourceInformation))
                ._multiplicity(this.context.pureModel.getMultiplicity("one"))
                ._values((unitInstance.unitValue == null) ? Lists.immutable.empty() : Lists.immutable.with(unitInstance.unitValue));
    }

    @Override
    public ValueSpecification visit(UnitType unitType)
    {
        GenericType unitGenericType = new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, this.context.pureModel.getClass(M3Paths.GenericType))._rawType(this.context.pureModel.getType(M3Paths.Unit));
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", SourceInformationHelper.toM3SourceInformation(unitType.sourceInformation), this.context.pureModel.getClass(M3Paths.InstanceValue))
                ._genericType(unitGenericType)
                ._multiplicity(this.context.pureModel.getMultiplicity("one"))
                ._values(Lists.immutable.with((Unit) this.context.resolveType(unitType.fullPath, unitType.sourceInformation)));
    }

    @Override
    public ValueSpecification visit(KeyExpression keyExpression)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification key = keyExpression.key.accept(this);
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification expression = keyExpression.expression.accept(this);
        GenericType keyExpressionGenericType = new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, this.context.pureModel.getClass(M3Paths.GenericType))._rawType(this.context.pureModel.getType(M3Paths.KeyExpression));
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", SourceInformationHelper.toM3SourceInformation(keyExpression.sourceInformation), this.context.pureModel.getClass(M3Paths.InstanceValue))
                ._genericType(keyExpressionGenericType)
                ._multiplicity(this.context.pureModel.getMultiplicity("one"))
                ._values(Lists.immutable.with(new Root_meta_pure_metamodel_function_KeyExpression_Impl("", SourceInformationHelper.toM3SourceInformation(keyExpression.sourceInformation), this.context.pureModel.getClass(M3Paths.KeyExpression))._add(keyExpression.add)._key((InstanceValue) key)._expression(expression)));
    }

    @Override
    public ValueSpecification visit(PrimitiveType primitiveType)
    {
        return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", SourceInformationHelper.toM3SourceInformation(primitiveType.sourceInformation), this.context.pureModel.getClass(M3Paths.InstanceValue))
                ._genericType(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, this.context.pureModel.getClass(M3Paths.GenericType))._rawType(this.context.pureModel.getType(M3Paths.PrimitiveType)))
                ._multiplicity(this.context.pureModel.getMultiplicity("one"))
                ._values(Lists.immutable.with(new Root_meta_pure_metamodel_type_PrimitiveType_Impl("", SourceInformationHelper.toM3SourceInformation(primitiveType.sourceInformation), this.context.pureModel.getClass(M3Paths.PrimitiveType))
                        ._name(primitiveType.fullPath)));
    }
}
