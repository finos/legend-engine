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

package org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ProcessingContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.builder.CompositeFunctionExpressionBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.builder.FunctionExpressionBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.builder.MultiHandlerFunctionExpressionBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.builder.RequiredInferenceSimilarSignatureFunctionExpressionBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.inference.Dispatch;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.inference.MostCommonMultiplicity;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.inference.MostCommonType;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.inference.ParametersInference;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.inference.ReturnInference;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.inference.TypeAndMultiplicity;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.Variable;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.AppliedFunction;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.ClassInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Collection;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.AggregateValue;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.TDSAggregateValue;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.TdsOlapAggregation;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.TdsOlapRank;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_generics_GenericType_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_valuespecification_VariableExpression_Impl;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.SimpleFunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecificationAccessor;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.finos.legend.pure.generated.platform_pure_basics_meta_elementToPath.Root_meta_pure_functions_meta_elementToPath_PackageableElement_1__String_1_;

public class Handlers
{
    private static final String PACKAGE_SEPARATOR = org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.DEFAULT_PATH_SEPARATOR;
    private static final String META_PACKAGE_NAME = "meta";

    private Set<String> registeredMetaPackages = Sets.mutable.empty();

    private static Collection toCollection(org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification vs)
    {
        if (vs instanceof Collection)
        {
            return (Collection) vs;
        }
        return new Collection(Lists.mutable.with(vs));
    }

    private static void updateTwoParamsLambda(Object lambda, GenericType newGenericType, org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity m)
    {
        if (lambda instanceof Lambda)
        {
            Variable variable = ((Lambda) lambda).parameters.get(0);
            variable._class = PackageableElement.getUserPathForPackageableElement(newGenericType._rawType());
            variable.multiplicity = m;

            Variable variable2 = ((Lambda) lambda).parameters.get(1);
            variable2._class = PackageableElement.getUserPathForPackageableElement(newGenericType._rawType());
            variable2.multiplicity = m;
        }
    }

    private static void updateTwoParamsLambdaDiffTypes(Object lambda, GenericType newGenericType, GenericType newGenericType2, org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity m, org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity m2)
    {
        if (lambda instanceof Lambda)
        {
            Variable variable = ((Lambda) lambda).parameters.get(0);
            variable._class = PackageableElement.getUserPathForPackageableElement(newGenericType._rawType());
            variable.multiplicity = m;

            Variable variable2 = ((Lambda) lambda).parameters.get(1);
            variable2._class = PackageableElement.getUserPathForPackageableElement(newGenericType2._rawType());
            variable2.multiplicity = m2;
        }
    }


    private static void updateSimpleLambda(Object lambda, GenericType newGenericType, org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity m)
    {
        if (lambda instanceof Lambda)
        {
            Variable variable = ((Lambda) lambda).parameters.get(0);
            variable._class = PackageableElement.getUserPathForPackageableElement(newGenericType._rawType());
            variable.multiplicity = m;
        }
    }

    private static void updateLambdaCollection(List<org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification> parameters, GenericType gt, org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity mul, int offset)
    {
        toCollection(parameters.get(offset)).values.forEach(l -> updateSimpleLambda(l, gt, mul));
    }

    private static void updateLambdaWithCol(GenericType gt2, org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification l)
    {
        if (l instanceof AppliedFunction)
        {
            updateSimpleLambda(((AppliedFunction) l).parameters.get(0), gt2, new org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity(1, 1));
        }
    }

    private static void updateTDSRowLambda(List<Variable> vars)
    {
        Variable variable = vars.get(0);
        variable._class = "meta::pure::tds::TDSRow";
        variable.multiplicity = new org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity(1, 1);
        Variable variable2 = vars.get(1);
        variable2._class = "meta::pure::tds::TDSRow";
        variable2.multiplicity = new org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity(1, 1);
    }

    public static void aggInference(Object obj, GenericType gt, int mapOffset, int aggOffset, CompileContext cc, MutableList<String> ov, ProcessingContext pc)
    {
        Lambda aggFirstLambda = null;
        Lambda aggSecondLambda = null;
        obj = obj instanceof ClassInstance ? ((ClassInstance) obj).value : obj;
        if (obj instanceof AppliedFunction)
        {
            aggFirstLambda = ((Lambda) ((AppliedFunction) obj).parameters.get(mapOffset));
            aggSecondLambda = ((Lambda) ((AppliedFunction) obj).parameters.get(aggOffset));
        }
        else if (obj instanceof AggregateValue)
        {
            aggFirstLambda = ((AggregateValue) obj).mapFn;
            aggSecondLambda = ((AggregateValue) obj).aggregateFn;
        }
        else if (obj instanceof TDSAggregateValue)
        {
            aggFirstLambda = ((TDSAggregateValue) obj).mapFn;
            aggSecondLambda = ((TDSAggregateValue) obj).aggregateFn;
        }
        if (aggFirstLambda != null && aggSecondLambda != null)
        {
            updateSimpleLambda(aggFirstLambda, gt, org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity.PURE_ONE);
            ValueSpecification processLambda = aggFirstLambda.accept(new ValueSpecificationBuilder(cc, ov, pc));
            updateSimpleLambda(aggSecondLambda, funcReturnType(processLambda, cc.pureModel), new org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity());
        }
    }

    private static void aggInferenceAll(List<org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification> parameters, GenericType gt, int mapOffset, int aggOffset, MutableList<String> ov, CompileContext cc, ProcessingContext pc)
    {
        if (parameters.get(2) instanceof Collection)
        {
            ((Collection) parameters.get(2)).values.forEach(a -> aggInference(a, gt, mapOffset, aggOffset, cc, ov, pc));
        }
        else
        {
            aggInference(parameters.get(2), gt, mapOffset, aggOffset, cc, ov, pc);
        }
    }

    public static final ParametersInference ExtendInference = (parameters, ov, cc, pc) ->
    {
        toCollection(parameters.get(1)).values.forEach(l -> updateLambdaWithCol(cc.pureModel.getGenericType("meta::pure::tds::TDSRow"), l));
        return parameters.stream().map(p -> p.accept(new ValueSpecificationBuilder(cc, ov, pc))).collect(Collectors.toList());
    };

    public static final ParametersInference LambdaCollectionInference = (parameters, ov, cc, pc) ->
    {
        ValueSpecification firstProcessedParameter = parameters.get(0).accept(new ValueSpecificationBuilder(cc, ov, pc));
        updateLambdaCollection(parameters, firstProcessedParameter._genericType(), new org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity(1, 1), 1);
        return Stream.concat(Stream.of(firstProcessedParameter), parameters.stream().skip(1).map(p -> p.accept(new ValueSpecificationBuilder(cc, ov, pc)))).collect(Collectors.toList());
    };

    public static final ParametersInference TDSContainsInference = (parameters, ov, cc, pc) ->
    {
        ValueSpecification firstProcessedParameter = parameters.get(0).accept(new ValueSpecificationBuilder(cc, ov, pc));
        updateLambdaCollection(parameters, firstProcessedParameter._genericType(), new org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity(1, 1), 1);
        updateTDSRowLambda(((Lambda) parameters.get(4)).parameters);
        return Stream.concat(Stream.of(firstProcessedParameter), parameters.stream().skip(1).map(p -> p.accept(new ValueSpecificationBuilder(cc, ov, pc)))).collect(Collectors.toList());
    };

    public static final ParametersInference EvalInference = (parameters, ov, cc, pc) ->
    {
        ValueSpecification secondProcessedParameter = parameters.get(1).accept(new ValueSpecificationBuilder(cc, ov, pc));
        updateLambdaCollection(parameters, secondProcessedParameter._genericType(), new org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity(1, 1), 0);
        ValueSpecification firstProcessedParameter = parameters.get(0).accept(new ValueSpecificationBuilder(cc, ov, pc));
        return Stream.concat(Stream.of(firstProcessedParameter, secondProcessedParameter), parameters.stream().skip(2).map(p -> p.accept(new ValueSpecificationBuilder(cc, ov, pc)))).collect(Collectors.toList());
    };

    public static final ParametersInference LambdaColCollectionInference = (parameters, ov, cc, pc) ->
    {
        ValueSpecification firstProcessedParameter = parameters.get(0).accept(new ValueSpecificationBuilder(cc, ov, pc));
        GenericType gt = firstProcessedParameter._genericType();
        final GenericType gt2 = gt._rawType()._name().equals("TabularDataSet") || gt._rawType()._name().equals("TableTDS") ? cc.pureModel.getGenericType("meta::pure::tds::TDSRow") : gt;
        toCollection(parameters.get(1)).values.forEach(l -> updateLambdaWithCol(gt2, l));
        return Stream.concat(Stream.of(firstProcessedParameter), parameters.stream().skip(1).map(p -> p.accept(new ValueSpecificationBuilder(cc, ov, pc)))).collect(Collectors.toList());
    };

    public static final ParametersInference LambdaInference = (parameters, ov, cc, pc) ->
    {
        List<ValueSpecification> firstPassProcessed = parameters.stream().map(p -> p instanceof Lambda ? null : p.accept(new ValueSpecificationBuilder(cc, ov, pc))).collect(Collectors.toList());
        updateSimpleLambda(parameters.get(1), firstPassProcessed.get(0)._genericType(), new org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity(1, 1));
        return ListIterate.zip(firstPassProcessed, parameters).collect(p -> p.getOne() != null ? p.getOne() : p.getTwo().accept(new ValueSpecificationBuilder(cc, ov, pc)));
    };

    public static final ParametersInference TwoParameterLambdaInference = (parameters, ov, cc, pc) ->
    {
        List<ValueSpecification> firstPassProcessed = parameters.stream().map(p -> p instanceof Lambda ? null : p.accept(new ValueSpecificationBuilder(cc, ov, pc))).collect(Collectors.toList());
        updateTwoParamsLambda(parameters.get(1), firstPassProcessed.get(0)._genericType(), new org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity(1, 1));
        return ListIterate.zip(firstPassProcessed, parameters).collect(p -> p.getOne() != null ? p.getOne() : p.getTwo().accept(new ValueSpecificationBuilder(cc, ov, pc)));
    };

    public static final ParametersInference TwoParameterLambdaInferenceDiffTypes = (parameters, ov, cc, pc) ->
    {
        List<ValueSpecification> firstPassProcessed = parameters.stream().map(p -> p instanceof Lambda ? null : p.accept(new ValueSpecificationBuilder(cc, ov, pc))).collect(Collectors.toList());

        Multiplicity mul = firstPassProcessed.get(2)._multiplicity();
        org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity m2 = new org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity();
        m2.lowerBound = mul._lowerBound()._value().intValue();
        if (mul._upperBound()._value() != null)
        {
            m2.setUpperBound(mul._upperBound()._value().intValue());
        }

        updateTwoParamsLambdaDiffTypes(parameters.get(1), firstPassProcessed.get(0)._genericType(), firstPassProcessed.get(2)._genericType(), new org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity(1, 1), m2);
        return ListIterate.zip(firstPassProcessed, parameters).collect(p -> p.getOne() != null ? p.getOne() : p.getTwo().accept(new ValueSpecificationBuilder(cc, ov, pc)));
    };

    public static final ParametersInference TDSFilterInference = (parameters, ov, cc, pc) ->
    {
        ValueSpecification firstProcessedParameter = parameters.get(0).accept(new ValueSpecificationBuilder(cc, ov, pc));
        GenericType gt = firstProcessedParameter._genericType();
        if ("TabularDataSet".equals(gt._rawType()._name()))
        {
            updateSimpleLambda(parameters.get(1), cc.pureModel.getGenericType("meta::pure::tds::TDSRow"), new org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity(1, 1));
            return Stream.concat(Stream.of(firstProcessedParameter), parameters.stream().skip(1).map(p -> p.accept(new ValueSpecificationBuilder(cc, ov, pc)))).collect(Collectors.toList());
        }
        else
        {
            List<ValueSpecification> firstPassProcessed = parameters.stream().skip(1).map(p -> p instanceof Lambda ? null : p.accept(new ValueSpecificationBuilder(cc, ov, pc))).collect(Collectors.toList());
            updateSimpleLambda(parameters.get(1), parameters.size() != 0 && parameters.get(0) instanceof Lambda ? firstPassProcessed.get(0)._genericType() : firstProcessedParameter._genericType(), new org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity(1, 1));
            return ListIterate.zip(LazyIterate.concatenate(FastList.newListWith(firstProcessedParameter), firstPassProcessed).toList(), parameters).collect(p -> p.getOne() != null ? p.getOne() : p.getTwo().accept(new ValueSpecificationBuilder(cc, ov, pc)));
        }
    };

    public static final ParametersInference JoinInference = (parameters, ov, cc, pc) ->
    {
        updateTDSRowLambda(((Lambda) parameters.get(3)).parameters);
        return parameters.stream().map(p -> p.accept(new ValueSpecificationBuilder(cc, ov, pc))).collect(Collectors.toList());
    };

    public static final ParametersInference TDSAggInference = (parameters, ov, cc, pc) ->
    {
        aggInferenceAll(parameters, cc.pureModel.getGenericType("meta::pure::tds::TDSRow"), 1, 2, ov, cc, pc);
        return parameters.stream().map(p -> p.accept(new ValueSpecificationBuilder(cc, ov, pc))).collect(Collectors.toList());
    };

    public static final ParametersInference TDSOLAPInference = (parameters, ov, cc, pc) ->
    {
        parameters.forEach(parameter ->
        {
            Object param = parameter instanceof ClassInstance ? ((ClassInstance) parameter).value : parameter;
            if (param instanceof TdsOlapRank)
            {
                updateSimpleLambda(((TdsOlapRank) param).function, cc.pureModel.getGenericType("meta::pure::metamodel::type::Any"), new org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity());
            }
            else if (param instanceof TdsOlapAggregation)
            {
                updateSimpleLambda(((TdsOlapAggregation) param).function, cc.pureModel.getGenericType("Number"), new org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity());
            }
            if (parameter instanceof Lambda)
            {
                updateSimpleLambda(parameter, cc.pureModel.getGenericType("meta::pure::tds::TDSRow"), new org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity());
            }
        });

        return parameters.stream().map(p -> p.accept(new ValueSpecificationBuilder(cc, ov, pc))).collect(Collectors.toList());
    };

    public static final ParametersInference OLAPFuncTDSInference = (parameters, ov, cc, pc) ->
    {
        updateSimpleLambda(parameters.get(0), cc.pureModel.getGenericType("meta::pure::tds::TDSRow"), new org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity());
        return parameters.stream().map(p -> p.accept(new ValueSpecificationBuilder(cc, ov, pc))).collect(Collectors.toList());
    };

    public static final ParametersInference OLAPFuncNumInference = (parameters, ov, cc, pc) ->
    {
        updateSimpleLambda(parameters.get(1), cc.pureModel.getGenericType("Number"), new org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity());
        return parameters.stream().map(p -> p.accept(new ValueSpecificationBuilder(cc, ov, pc))).collect(Collectors.toList());
    };

    public static final ParametersInference LambdaAndAggInference = (parameters, ov, cc, pc) ->
    {
        // Main Lambda
        ValueSpecification firstProcessedParameter = parameters.get(0).accept(new ValueSpecificationBuilder(cc, ov, pc));
        GenericType gt = firstProcessedParameter._genericType();
        updateLambdaCollection(parameters, gt, new org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity(1, 1), 1);
        aggInferenceAll(parameters, gt, 0, 1, ov, cc, pc);
        return Stream.concat(Stream.of(firstProcessedParameter), parameters.stream().skip(1).map(p -> p.accept(new ValueSpecificationBuilder(cc, ov, pc)))).collect(Collectors.toList());
    };

    private final Map<String, FunctionExpressionBuilder> map = UnifiedMap.newMap();
    private final Map<String, Dispatch> dispatchMap;
    private final PureModel pureModel;
    private static final ImmutableSet<String> NUMBER = Sets.immutable.with("Integer", "Number", "Float");
    private static final ImmutableSet<String> DATE = Sets.immutable.with("Date", "StrictDate", "DateTime");
    private static final String Nil = "Nil";

    /**
     * NOTE: we only need to pass in the model and not compile context, because the handler's sole job is to register function
     * handlers, it should not be context-aware. We may revise that decision but to assert that fact, we will pass just the Pure
     * model here instead
     */
    public Handlers(PureModel pureModel)
    {
        this.pureModel = pureModel;
        this.dispatchMap = buildDispatch();
        registerMathInequalities();
        registerMaxMin();
        registerAlgebra();
        registerOlapMath();
        registerAggregations();
        registerStdDeviations();
        registerTrigo();
        registerStrings();
        registerDates();
        registerTDS();
        registerJson();
        registerRuntimeHelper();
        registerAsserts();
        registerUnitFunctions();

        register(grp(LambdaInference, h("meta::pure::functions::collection::sortBy_T_m__Function_$0_1$__T_m_", false, ps -> res(ps.get(0)._genericType(), ps.get(0)._multiplicity()), ps -> true)));

        register(grp(LambdaInference,
                // meta::pure::functions::collection::map<T,V|m>(value:T[m], func:Function<{T[1]->V[1]}>[1]):V[m];
                h("meta::pure::functions::collection::map_T_m__Function_1__V_m_", true, ps -> res(funcReturnType(ps.get(1)), ps.get(0)._multiplicity()), ps -> isOne(funcType(ps.get(1)._genericType())._returnMultiplicity())),
                // meta::pure::functions::collection::map<T,V>(value:T[0..1], func:Function<{T[1]->V[0..1]}>[1]):V[0..1];
                h("meta::pure::functions::collection::map_T_$0_1$__Function_1__V_$0_1$_", true, ps -> res(funcReturnType(ps.get(1)), "zeroOne"), ps -> matchZeroOne(ps.get(0)._multiplicity()) && matchZeroOne(funcType(ps.get(1)._genericType())._returnMultiplicity())),
                // meta::pure::functions::collection::map<T,V>(value:T[*], func:Function<{T[1]->V[*]}>[1]):V[*];
                h("meta::pure::functions::collection::map_T_MANY__Function_1__V_MANY_", true, ps -> res(funcReturnType(ps.get(1)), "zeroMany"), ps -> true)));

        register(m(
                        // meta::pure::tds::filter(tds:TabularDataSet[1], f:Function<{TDSRow[1]->Boolean[1]}>[1]):TabularDataSet[1]
                        grp(TDSFilterInference, h("meta::pure::tds::filter_TabularDataSet_1__Function_1__TabularDataSet_1_", false, ps -> res("meta::pure::tds::TabularDataSet", "one"), ps -> typeOne(ps.get(0), "TabularDataSet"))),
                        // meta::pure::functions::collection::filter<T>(value:T[*], func:Function<{T[1]->Boolean[1]}>[1]):T[*];
                        grp(LambdaInference, h("meta::pure::functions::collection::filter_T_MANY__Function_1__T_MANY_", true, ps -> res(ps.get(0)._genericType(), "zeroMany"), ps -> true))
                )
        );

        register(m(
                        // meta::pure::tds::project<T>(set:T[*], paths:Path<T,Any|*>[*]):TabularDataSet[1]
                        m(h("meta::pure::tds::project_T_MANY__Path_MANY__TabularDataSet_1_", false, ps -> res("meta::pure::tds::TabularDataSet", "one"), ps -> ps.size() == 2 && typeMany(ps.get(1), "Path"))),
                        // meta::pure::tds::project<K>(set:K[*], functions:Function<{K[1]->Any[*]}>[*], ids:String[*]):TabularDataSet[1]
                        grp(LambdaCollectionInference, h("meta::pure::tds::project_K_MANY__Function_MANY__String_MANY__TabularDataSet_1_", false, ps -> res("meta::pure::tds::TabularDataSet", "one"), ps -> ps.size() == 3)),
                        grp(LambdaColCollectionInference,
                                //meta::pure::tds::project(tds:TabularDataSet[1], columnFunctions:ColumnSpecification<TDSRow>[*]):TabularDataSet[1]
                                h("meta::pure::tds::project_TabularDataSet_1__ColumnSpecification_MANY__TabularDataSet_1_", false, ps -> res("meta::pure::tds::TabularDataSet", "one"), ps -> typeOne(ps.get(0), "TabularDataSet")),
                                // meta::pure::tds::project<T>(set:T[*], columnSpecifications:ColumnSpecification<T>[*]):TabularDataSet[1]
                                h("meta::pure::tds::project_T_MANY__ColumnSpecification_MANY__TabularDataSet_1_", false, ps -> res("meta::pure::tds::TabularDataSet", "one"), ps -> true)
                        )
                )
        );

        register(m(
                        // meta::pure::tds::groupBy<T,U>(tds:TabularDataSet[1], columns:String[*], aggValues:meta::pure::tds::AggregateValue<T,U>[*]):TabularDataSet[1]
                        grp(TDSAggInference, h("meta::pure::tds::groupBy_TabularDataSet_1__String_MANY__AggregateValue_MANY__TabularDataSet_1_", false, ps -> res("meta::pure::tds::TabularDataSet", "one"), ps -> typeOne(ps.get(0), "TabularDataSet"))),
                        // meta::pure::functions::collection::groupBy<K,V,U>(set:K[*], functions:meta::pure::metamodel::function::Function<{K[1]->Any[*]}>[*], aggValues:meta::pure::functions::collection::AggregateValue<K,V,U>[*], ids:String[*]):TabularDataSet[1]
                        grp(LambdaAndAggInference, h("meta::pure::tds::groupBy_K_MANY__Function_MANY__AggregateValue_MANY__String_MANY__TabularDataSet_1_", false, ps -> res("meta::pure::tds::TabularDataSet", "one"), ps -> true))
                )
        );

        // meta::pure::tds::extend(tds:TabularDataSet[1], newColumnFunctions:BasicColumnSpecification<TDSRow >[*]):TabularDataSet[1]
        register(grp(ExtendInference, h("meta::pure::tds::extend_TabularDataSet_1__BasicColumnSpecification_MANY__TabularDataSet_1_", false, ps -> res("meta::pure::tds::TabularDataSet", "one"), ps -> true)));

        register(grp(LambdaInference, h("meta::pure::functions::collection::exists_T_MANY__Function_1__Boolean_1_", true, ps -> res("Boolean", "one"), ps -> true)));

        register(grp(LambdaInference, h("meta::pure::functions::collection::forAll_T_MANY__Function_1__Boolean_1_", true, ps -> res("Boolean", "one"), ps -> true)));

        register(grp(LambdaAndAggInference, h("meta::pure::tds::groupByWithWindowSubset_K_MANY__Function_MANY__AggregateValue_MANY__String_MANY__String_MANY__String_MANY__TabularDataSet_1_", false, ps -> res("meta::pure::tds::TabularDataSet", "one"), ps -> true)));

        register(m(
                        // meta::pure::functions::lang::eval<T,V|m,n>(func:Function<{T[n]->V[m]}>[1], param:T[n]):V[m];
                        grp(EvalInference, h("meta::pure::functions::lang::eval_Function_1__T_n__V_m_", true, ps -> res(funcReturnType(ps.get(0)), funcReturnMul(ps.get(0))), ps -> ps.size() == 2)),
                        // meta::pure::functions::lang::eval<V|m>(func:Function<{->V[m]}>[1]):V[m];
                        m(h("meta::pure::functions::lang::eval_Function_1__V_m_", true, ps -> res(funcReturnType(ps.get(0)), funcReturnMul(ps.get(0))), ps -> ps.size() == 1))
                )
        );

        // Inference in the context of the parent
        register(m(m(h("meta::pure::tds::agg_String_1__FunctionDefinition_1__FunctionDefinition_1__AggregateValue_1_", false, ps -> res("meta::pure::tds::AggregateValue", "one"), ps -> typeOne(ps.get(0), "String"))),
                m(h("meta::pure::functions::collection::agg_FunctionDefinition_1__FunctionDefinition_1__AggregateValue_1_", false, ps -> res("meta::pure::functions::collection::AggregateValue", "one"), ps -> true))));


        register(m(m(h("meta::pure::tds::col_Function_1__String_1__String_1__BasicColumnSpecification_1_", false, ps -> res(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, this.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))
                                ._rawType(this.pureModel.getType("meta::pure::tds::BasicColumnSpecification"))
                                ._typeArguments(Lists.fixedSize.of(((FunctionType) ps.get(0)._genericType()._typeArguments().getFirst()._rawType())._parameters().getFirst()._genericType())),
                        "one"), ps -> ps.size() == 3)),
                m(h("meta::pure::tds::col_Function_1__String_1__BasicColumnSpecification_1_", false, ps -> res(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, this.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))
                                ._rawType(this.pureModel.getType("meta::pure::tds::BasicColumnSpecification"))
                                ._typeArguments(Lists.fixedSize.of(((FunctionType) ps.get(0)._genericType()._typeArguments().getFirst()._rawType())._parameters().getFirst()._genericType())),
                        "one"), ps -> true))));
        // ----------------------------


        register(h("meta::pure::functions::collection::isEmpty_Any_$0_1$__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> matchZeroOne(ps.get(0)._multiplicity())),
                h("meta::pure::functions::collection::isEmpty_Any_MANY__Boolean_1_", true, ps -> res("Boolean", "one"), ps -> true));
        register(h("meta::pure::functions::collection::isNotEmpty_Any_$0_1$__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> matchZeroOne(ps.get(0)._multiplicity())),
                h("meta::pure::functions::collection::isNotEmpty_Any_MANY__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> true));

        register("meta::pure::functions::boolean::eq_Any_1__Any_1__Boolean_1_", true, ps -> res("Boolean", "one"));
        register("meta::pure::functions::boolean::equal_Any_MANY__Any_MANY__Boolean_1_", true, ps -> res("Boolean", "one"));
        register("meta::pure::functions::boolean::is_Any_1__Any_1__Boolean_1_", true, ps -> res("Boolean", "one"));
        register("meta::pure::functions::boolean::equalJsonStrings_String_1__String_1__Boolean_1_", true, ps -> res("Boolean", "one"));

        register("meta::pure::functions::constraints::warn_Boolean_1__String_1__Boolean_1_", false, ps -> res("Boolean", "one"));

        register("meta::pure::functions::lang::subType_Any_m__T_1__T_m_", false, ps -> res(ps.get(1)._genericType(), ps.get(0)._multiplicity()));
        register(h("meta::pure::functions::lang::whenSubType_Any_1__T_1__T_$0_1$_", false, ps -> res(ps.get(1)._genericType(), "zeroOne"), ps -> isOne(ps.get(0)._multiplicity())),
                h("meta::pure::functions::lang::whenSubType_Any_$0_1$__T_1__T_$0_1$_", false, ps -> res(ps.get(1)._genericType(), "zeroOne"), ps -> isZeroOne(ps.get(0)._multiplicity())),
                h("meta::pure::functions::lang::whenSubType_Any_MANY__T_1__T_MANY_", false, ps -> res(ps.get(1)._genericType(), "zeroMany"), ps -> true));
        register("meta::pure::functions::lang::orElse_T_$0_1$__T_1__T_1_", false, ps -> res(ps.get(0)._genericType(), "one"));

        register(h("meta::pure::functions::string::contains_String_1__String_1__Boolean_1_", true, ps -> res("Boolean", "one"), ps -> typeOne(ps.get(0), "String") && typeOne(ps.get(1), "String")),
                h("meta::pure::functions::string::contains_String_$0_1$__String_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeZeroOne(ps.get(0), "String") && typeOne(ps.get(1), "String")),
                h("meta::pure::functions::collection::contains_Any_MANY__Any_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> true));

        register("meta::pure::functions::collection::containsAny_Any_MANY__Any_MANY__Boolean_1_", false, ps -> res("Boolean", "one"));

        register("meta::pure::functions::collection::objectReferenceIn_Any_1__String_MANY__Boolean_1_", false, ps -> res("Boolean", "one"));

        register(h("meta::pure::functions::collection::in_Any_1__Any_MANY__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> isOne(ps.get(0)._multiplicity())),
                h("meta::pure::functions::collection::in_Any_$0_1$__Any_MANY__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> isZeroOne(ps.get(0)._multiplicity())));

        register(h("meta::pure::tds::take_TabularDataSet_1__Integer_1__TabularDataSet_1_", false, ps -> res("meta::pure::tds::TabularDataSet", "one"), ps -> typeOne(ps.get(0), "TabularDataSet")),
                h("meta::pure::functions::collection::take_T_MANY__Integer_1__T_MANY_", true, ps -> res(ps.get(0)._genericType(), "zeroMany"), ps -> true));

        register("meta::pure::functions::lang::if_Boolean_1__Function_1__Function_1__T_m_", true, ps -> res(MostCommonType.mostCommon(Lists.fixedSize.of(funcReturnType(ps.get(1)), funcReturnType(ps.get(2))), this.pureModel), MostCommonMultiplicity.mostCommon(Lists.fixedSize.of(funcReturnMul(ps.get(1)), funcReturnMul(ps.get(2))), this.pureModel)));

        register(m(m(h("meta::pure::functions::boolean::and_Boolean_1__Boolean_1__Boolean_1_", true, ps -> res("Boolean", "one"), ps -> ps.size() == 2)),
                m(h("meta::pure::functions::collection::and_Boolean_MANY__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> true))));

        register(m(m(h("meta::pure::functions::boolean::or_Boolean_1__Boolean_1__Boolean_1_", true, ps -> res("Boolean", "one"), ps -> ps.size() == 2)),
                m(h("meta::pure::functions::collection::or_Boolean_$1_MANY$__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> true))));

        register("meta::pure::tds::tdsRows_TabularDataSet_1__TDSRow_MANY_", false, ps -> res("meta::pure::tds::TDSRow", "zeroMany"));
        register("meta::pure::functions::boolean::not_Boolean_1__Boolean_1_", true, ps -> res("Boolean", "one"));

        register("meta::pure::functions::boolean::isTrue_Boolean_$0_1$__Boolean_1_", false, ps -> res("Boolean", "one"));
        register("meta::pure::functions::boolean::isFalse_Boolean_$0_1$__Boolean_1_", false, ps -> res("Boolean", "one"));

        register("meta::pure::functions::collection::size_Any_MANY__Integer_1_", true, ps -> res("Integer", "one"));


        register("meta::pure::functions::multiplicity::toOne_T_MANY__T_1_", true, ps -> res(ps.get(0)._genericType(), "one"));

        register(m(
                m(h("meta::pure::functions::string::indexOf_String_1__String_1__Integer_1_", true, ps -> res("Integer", "one"), ps -> ps.size() == 2 && typeOne(ps.get(0), "String") && typeOne(ps.get(1), "String")),
                        h("meta::pure::functions::collection::indexOf_T_MANY__T_1__Integer_1_", true, ps -> res("Integer", "one"), ps -> ps.size() == 2)),
                m(h("meta::pure::functions::string::indexOf_String_1__String_1__Integer_1__Integer_1_", true, ps -> res("Integer", "one"), p -> true))));


        register(m(m(m(h("meta::pure::functions::collection::removeDuplicates_T_MANY__T_MANY_", false, ps -> res(ps.get(0)._genericType(), "zeroMany"), ps -> ps.size() == 1)),
                        // meta::pure::functions::collection::removeDuplicates<T>(col:T[*], eql:Function<{T[1],T[1]->Boolean[1]}>[1]):T[*]
                        grp(TwoParameterLambdaInference, h("meta::pure::functions::collection::removeDuplicates_T_MANY__Function_1__T_MANY_", false, ps -> res(ps.get(0)._genericType(), "zeroMany"), p -> p.size() == 2))),
                grp(TwoParameterLambdaInference, h("meta::pure::functions::collection::removeDuplicates_T_MANY__Function_$0_1$__Function_$0_1$__T_MANY_", true, ps -> res(ps.get(0)._genericType(), "zeroMany"), p -> p.size() == 3))));

        register(h("meta::pure::tds::concatenate_TabularDataSet_1__TabularDataSet_1__TabularDataSet_1_", false, ps -> res("meta::pure::tds::TabularDataSet", "one"), ps -> "TabularDataSet".equals(ps.get(0)._genericType()._rawType()._name())),
                h("meta::pure::functions::collection::concatenate_T_MANY__T_MANY__T_MANY_", true, ps -> res(ps.get(0)._genericType(), "zeroMany"), ps -> true));


        register("meta::pure::functions::collection::first_T_MANY__T_$0_1$_", true, ps -> res(ps.get(0)._genericType(), "zeroOne"));
        register("meta::pure::functions::collection::last_T_MANY__T_$0_1$_", true, ps -> res(ps.get(0)._genericType(), "zeroOne"));
        register("meta::pure::functions::meta::enumName_Enumeration_1__String_1_", true, ps -> res("String", "one"));
        register("meta::pure::functions::lang::extractEnumValue_Enumeration_1__String_1__T_1_", true, ps -> res(ps.get(0)._genericType()._typeArguments().getFirst(), "one"));
        register("meta::pure::functions::meta::enumValues_Enumeration_1__T_MANY_", true, ps -> res(ps.get(0)._genericType()._typeArguments().getFirst(), "zeroMany"));

        register(h("meta::pure::tds::drop_TabularDataSet_1__Integer_1__TabularDataSet_1_", false, ps -> res("meta::pure::tds::TabularDataSet", "one"), ps -> "TabularDataSet".equals(ps.get(0)._genericType()._rawType()._name())),
                h("meta::pure::functions::collection::drop_T_MANY__Integer_1__T_MANY_", true, ps -> res(ps.get(0)._genericType(), "zeroMany"), ps -> true));

        register("meta::pure::functions::multiplicity::toOneMany_T_MANY__T_$1_MANY$_", true, ps -> res(ps.get(0)._genericType(), "oneMany"));
        register("meta::pure::functions::lang::letFunction_String_1__T_m__T_m_", true, ps -> res(ps.get(1)._genericType(), ps.get(1)._multiplicity()));
        register("meta::pure::functions::lang::new_Class_1__String_1__KeyExpression_MANY__T_1_", true, ps -> res(ps.get(0)._genericType()._typeArguments().getFirst(), "one"));
        register("meta::pure::functions::collection::count_Any_MANY__Integer_1_", false, ps -> res("Integer", "one"));

        register(m(m(h("meta::pure::functions::collection::getAll_Class_1__T_MANY_", true, ps -> res(ps.get(0)._genericType()._typeArguments().getFirst(), "zeroMany"), ps -> ps.size() == 1)),
                m(h("meta::pure::functions::collection::getAll_Class_1__Date_1__T_MANY_", true, ps -> res(ps.get(0)._genericType()._typeArguments().getFirst(), "zeroMany"), ps -> ps.size() == 2)),
                m(h("meta::pure::functions::collection::getAll_Class_1__Date_1__Date_1__T_MANY_", true, ps -> res(ps.get(0)._genericType()._typeArguments().getFirst(), "zeroMany"), ps -> ps.size() == 3))));
        register(h("meta::pure::functions::collection::getAllVersions_Class_1__T_MANY_", true, ps -> res(ps.get(0)._genericType()._typeArguments().getFirst(), "zeroMany"), ps -> ps.size() == 1));
        register("meta::pure::functions::collection::getAllVersionsInRange_Class_1__Date_1__Date_1__T_MANY_", true, ps -> res(ps.get(0)._genericType()._typeArguments().getFirst(), "zeroMany"));
        register("meta::pure::functions::collection::getAllForEachDate_Class_1__Date_MANY__T_MANY_", false, ps -> res(ps.get(0)._genericType()._typeArguments().getFirst(), "zeroMany"));

        register(h("meta::pure::tds::distinct_TabularDataSet_1__TabularDataSet_1_", false, ps -> res("meta::pure::tds::TabularDataSet", "one"), ps -> "TabularDataSet".equals(ps.get(0)._genericType()._rawType()._name())),
                h("meta::pure::functions::collection::distinct_T_MANY__T_MANY_", false, ps -> res(ps.get(0)._genericType(), "zeroMany"), ps -> true));


        register(m(
                        m(h("meta::pure::functions::collection::isDistinct_T_MANY__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 1)),
                        m(h("meta::pure::functions::collection::isDistinct_T_MANY__RootGraphFetchTree_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 2))
                )
        );

        register("meta::pure::functions::collection::isEqual_T_1__T_1__RootGraphFetchTree_1__Boolean_1_", false, ps -> res("Boolean", "one"));

        register(m(m(h("meta::pure::functions::collection::uniqueValueOnly_T_MANY__T_$0_1$__T_$0_1$_", false, ps -> res(ps.get(0)._genericType(), "zeroOne"), ps -> ps.size() == 2)),
                m(h("meta::pure::functions::collection::uniqueValueOnly_T_MANY__T_$0_1$_", false, ps -> res(ps.get(0)._genericType(), "zeroOne"), ps -> true))));

        register(h("meta::pure::tds::limit_TabularDataSet_1__Integer_1__TabularDataSet_1_", false, ps -> res("meta::pure::tds::TabularDataSet", "one"), ps -> typeOne(ps.get(0), "TabularDataSet") && typeOne(ps.get(1), "Integer")),
                h("meta::pure::tds::limit_TabularDataSet_1__Integer_$0_1$__TabularDataSet_1_", false, ps -> res("meta::pure::tds::TabularDataSet", "one"), ps -> typeOne(ps.get(0), "TabularDataSet") && typeZeroOne(ps.get(1), "Integer")),
                h("meta::pure::functions::collection::limit_T_MANY__Integer_1__T_MANY_", false, ps -> res(ps.get(0)._genericType(), "zeroMany"), ps -> true));

        register(h("meta::pure::tds::slice_TabularDataSet_1__Integer_1__Integer_1__TabularDataSet_1_", false, ps -> res("meta::pure::tds::TabularDataSet", "one"), ps -> typeOne(ps.get(0), "TabularDataSet")),
                h("meta::pure::functions::collection::slice_T_MANY__Integer_1__Integer_1__T_MANY_", true, ps -> res(ps.get(0)._genericType(), "zeroMany"), ps -> true));

        register("meta::pure::functions::lang::cast_Any_m__T_1__T_m_", true, ps -> res(ps.get(1)._genericType(), ps.get(0)._multiplicity()));

        register("meta::pure::functions::collection::at_T_MANY__Integer_1__T_1_", true, ps -> res(ps.get(0)._genericType(), "one"));

        register(m(
                        m(h("meta::pure::graphFetch::execution::graphFetch_T_MANY__RootGraphFetchTree_1__T_MANY_", false, ps -> res(ps.get(0)._genericType(), "zeroMany"), ps -> ps.size() == 2)),
                        m(h("meta::pure::graphFetch::execution::graphFetch_T_MANY__RootGraphFetchTree_1__Integer_1__T_MANY_", false, ps -> res(ps.get(0)._genericType(), "zeroMany"), ps -> ps.size() == 3))
                )
        );
        register("meta::pure::graphFetch::execution::graphFetchChecked_T_MANY__RootGraphFetchTree_1__Checked_MANY_", false, ps -> res(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, this.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))._rawType(this.pureModel.getType("meta::pure::dataQuality::Checked"))._typeArgumentsAdd(ps.get(0)._genericType()), "zeroMany"));
        register("meta::pure::graphFetch::execution::graphFetchUnexpanded_T_MANY__RootGraphFetchTree_1__T_MANY_", false, ps -> res(ps.get(0)._genericType(), "zeroMany"));
        register("meta::pure::graphFetch::execution::graphFetchCheckedUnexpanded_T_MANY__RootGraphFetchTree_1__Checked_MANY_", false, ps -> res(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, this.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))._rawType(this.pureModel.getType("meta::pure::dataQuality::Checked"))._typeArgumentsAdd(ps.get(0)._genericType()), "zeroMany"));
        register(m(
                        m(h("meta::pure::graphFetch::execution::serialize_Checked_MANY__RootGraphFetchTree_1__String_1_", false, ps -> res("String", "one"), ps -> ps.size() == 2 && "Checked".equals(ps.get(0)._genericType()._rawType()._name()))),
                        m(h("meta::pure::graphFetch::execution::serialize_T_MANY__RootGraphFetchTree_1__String_1_", false, ps -> res("String", "one"), ps -> ps.size() == 2)),
                        m(h("meta::pure::graphFetch::execution::serialize_Checked_MANY__RootGraphFetchTree_1__AlloySerializationConfig_1__String_1_", false, ps -> res("String", "one"), ps -> ps.size() == 3 && "Checked".equals(ps.get(0)._genericType()._rawType()._name()) && "AlloySerializationConfig".equals(ps.get(2)._genericType()._rawType()._name()))),
                        m(h("meta::pure::graphFetch::execution::serialize_T_MANY__RootGraphFetchTree_1__AlloySerializationConfig_1__String_1_", false, ps -> res("String", "one"), ps -> ps.size() == 3))
                )
        );

        register(h("meta::pure::graphFetch::calculateSourceTree_RootGraphFetchTree_1__Mapping_1__Extension_MANY__RootGraphFetchTree_1_", false, ps -> res("meta::pure::graphFetch::RootGraphFetchTree", "one"), ps -> true));
        register("meta::pure::functions::lang::match_Any_MANY__Function_$1_MANY$__T_m_", true, ps -> res(funcReturnType(ps.get(1)), funcReturnMul(ps.get(1))));
        register("meta::pure::functions::meta::instanceOf_Any_1__Type_1__Boolean_1_", true, ps -> res("Boolean", "one"));
        register("meta::pure::functions::collection::union_T_MANY__T_MANY__T_MANY_", false, ps -> res(ps.get(0)._genericType(), "zeroMany"));
        register("meta::pure::functions::collection::reverse_T_m__T_m_", true, ps -> res(ps.get(0)._genericType(), ps.get(0)._multiplicity()));
        register(m(m(h("meta::pure::functions::date::add_StrictDate_1__Duration_1__StrictDate_1_", false, ps -> res("StrictDate", "one"), ps -> typeOne(ps.get(1), "Duration") && typeOne(ps.get(0), "StrictDate"))),
                m(h("meta::pure::functions::collection::add_T_MANY__T_1__T_$1_MANY$_", true, ps -> res(ps.get(0)._genericType(), "oneMany"), ps -> true))));

        register(m(m(h("meta::pure::functions::collection::dropAt_T_MANY__Integer_1__T_MANY_", false, ps -> res(ps.get(0)._genericType(), "zeroMany"), ps -> ps.size() == 2)),
                m(h("meta::pure::functions::collection::dropAt_T_MANY__Integer_1__Integer_1__T_MANY_", false, ps -> res(ps.get(0)._genericType(), "zeroMany"), ps -> ps.size() == 3))));

        register("meta::pure::functions::collection::zip_T_MANY__U_MANY__Pair_MANY_", true, ps -> res(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, this.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))
                ._rawType(this.pureModel.getType("meta::pure::functions::collection::Pair"))
                ._typeArguments(Lists.fixedSize.ofAll(ps.stream().map(ValueSpecificationAccessor::_genericType).collect(Collectors.toList()))), "oneMany"));
        register(m(grp(LambdaInference, h("meta::pure::functions::collection::removeDuplicatesBy_T_MANY__Function_1__T_MANY_", false, ps -> res(ps.get(0)._genericType(), "zeroMany"), p -> true))));
        register("meta::pure::functions::collection::containsAll_Any_MANY__Any_MANY__Boolean_1_", false, ps -> res("Boolean", "one"));

        register("meta::pure::functions::meta::id_Any_1__String_1_", true, ps -> res("String", "one"));
        register("meta::pure::functions::meta::typePath_Any_1__String_1_", false, ps -> res("String", "one"));
        register("meta::pure::functions::meta::typeName_Any_1__String_1_", false, ps -> res("String", "one"));

        register("meta::pure::functions::meta::type_Any_MANY__Type_1_", false, ps -> res("meta::pure::metamodel::type::Type", "one"));
        register("meta::pure::functions::lang::compare_T_1__T_1__Integer_1_", true, ps -> res("Integer", "one"));
        // meta::pure::functions::collection::fold<T,V|m>(value:T[*], func:Function<{T[1],V[m]->V[m]}>[1], accumulator:V[m]):V[m], note return type is V and not T
        register(m(grp(TwoParameterLambdaInferenceDiffTypes, h("meta::pure::functions::collection::fold_T_MANY__Function_1__V_m__V_m_", true, ps -> res(ps.get(2)._genericType(), ps.get(2)._multiplicity()), p -> true))));

        register(m(m(h("meta::pure::functions::collection::range_Integer_1__Integer_1__Integer_1__Integer_MANY_", true, ps -> res("Integer", "zeroMany"), ps -> ps.size() == 3)),
                m(h("meta::pure::functions::collection::range_Integer_1__Integer_1__Integer_MANY_", false, ps -> res("Integer", "zeroMany"), ps -> ps.size() == 2))));
        register("meta::pure::functions::collection::tail_T_MANY__T_MANY_", true, ps -> res(ps.get(0)._genericType(), "zeroMany"));
        register("meta::pure::functions::collection::head_T_MANY__T_$0_1$_", false, ps -> res(ps.get(0)._genericType(), "zeroOne"));
        register("meta::pure::functions::collection::oneOf_Boolean_MANY__Boolean_1_", false, ps -> res("Boolean", "one"));
        register("meta::pure::functions::collection::defaultIfEmpty_T_MANY__T_$1_MANY$__T_$1_MANY$_", false, ps -> res(MostCommonType.mostCommon(Lists.fixedSize.of(ps.get(0)._genericType(), ps.get(1)._genericType()), pureModel), "oneMany"));

        register("meta::pure::functions::string::isUUID_String_$0_1$__Boolean_1_", false, ps -> res("Boolean", "one"));
        register("meta::json::schema::mapSchema_String_1__Type_1__DiscriminatorMapping_1_", false, ps -> res("meta::json::schema::DiscriminatorMapping", "one"));
        register("meta::json::schema::discriminateOneOf_Any_1__Any_1__Type_MANY__DiscriminatorMapping_MANY__Boolean_1_", false, ps -> res("Boolean", "one"));

        // getter for execution parameters from execution environment
        register(m(
                m(h("meta::legend::service::get_ExecutionEnvironmentInstance_1__String_1__SingleExecutionParameters_1_", false, ps -> res("meta::legend::service::metamodel::SingleExecutionParameters", "one"), ps -> ps.size() == 2)),
                m(h("meta::legend::service::get_ExecutionEnvironmentInstance_1__String_1__String_1__SingleExecutionParameters_1_", false, ps -> res("meta::legend::service::metamodel::SingleExecutionParameters", "one"), ps -> ps.size() == 3))));


        // Extensions
        CompileContext context = this.pureModel.getContext();
        ListIterate.flatCollect(context.getCompilerExtensions().getExtraFunctionExpressionBuilderRegistrationInfoCollectors(), collector -> collector.valueOf(this)).forEach(this::register);
        ListIterate.flatCollect(context.getCompilerExtensions().getExtraFunctionHandlerRegistrationInfoCollectors(), collector -> collector.valueOf(this)).forEach(this::register);
    }

    private void registerAsserts()
    {
        register(m(m(h("meta::pure::functions::asserts::assert_Boolean_1__Function_1__Boolean_1_", true, ps -> res("Boolean", "one"), ps -> ps.size() == 2 && !typeOne(ps.get(1), "String"))),
                m(h("meta::pure::functions::asserts::assert_Boolean_1__String_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 2 && typeOne(ps.get(1), "String"))),
                m((h("meta::pure::functions::asserts::assert_Boolean_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 1))),
                m(h("meta::pure::functions::asserts::assert_Boolean_1__String_1__Any_MANY__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 3))));

        register(m(m(h("meta::pure::functions::asserts::assertContains_Any_MANY__Any_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 2)),
                m(h("meta::pure::functions::asserts::assertContains_Any_MANY__Any_1__String_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 3 && typeOne(ps.get(2), "String"))),
                m(h("meta::pure::functions::asserts::assertContains_Any_MANY__Any_1__String_1__Any_MANY__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 4)),
                m(h("meta::pure::functions::asserts::assertContains_Any_MANY__Any_1__Function_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 3 && !typeOne(ps.get(2), "String")))));

        register(m(m(h("meta::pure::functions::asserts::assertEmpty_Any_MANY__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 1)),
                m(h("meta::pure::functions::asserts::assertEmpty_Any_MANY__String_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 2 && typeOne(ps.get(1), "String"))),
                m(h("meta::pure::functions::asserts::assertEmpty_Any_MANY__String_1__Any_MANY__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 3)),
                m(h("meta::pure::functions::asserts::assertEmpty_Any_MANY__Function_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 2 && !typeOne(ps.get(1), "String")))));

        register(m(
                m(h("meta::pure::functions::asserts::assertEq_Any_1__Any_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 2)),
                m(h("meta::pure::functions::asserts::assertEq_Any_1__Any_1__String_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 3 && typeOne(ps.get(2), "String"))),
                m(h("meta::pure::functions::asserts::assertEq_Any_1__Any_1__String_1__Any_MANY__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 4))
        ));

        register(m(m(h("meta::pure::functions::asserts::assertEqWithinTolerance_Number_1__Number_1__Number_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 3)),
                m(h("meta::pure::functions::asserts::assertEqWithinTolerance_Number_1__Number_1__Number_1__String_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 4 && typeOne(ps.get(3), "String"))),
                m(h("meta::pure::functions::asserts::assertEqWithinTolerance_Number_1__Number_1__Number_1__String_1__Any_MANY__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 5)),
                m(h("meta::pure::functions::asserts::assertEqWithinTolerance_Number_1__Number_1__Number_1__Function_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 4 && !typeOne(ps.get(3), "String")))));

        register(m(m(h("meta::pure::functions::asserts::assertEquals_Any_MANY__Any_MANY__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 2)),
                m(h("meta::pure::functions::asserts::assertEquals_Any_MANY__Any_MANY__String_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 3 && typeOne(ps.get(2), "String"))),
                m(h("meta::pure::functions::asserts::assertEquals_Any_MANY__Any_MANY__String_1__Any_MANY__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 4)),
                m(h("meta::pure::functions::asserts::assertEquals_Any_MANY__Any_MANY__Function_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 3 && !typeOne(ps.get(2), "String")))));

        register(m(m(h("meta::pure::functions::asserts::assertFalse_Boolean_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 1)),
                m(h("meta::pure::functions::asserts::assertFalse_Boolean_1__String_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 2 && typeOne(ps.get(1), "String"))),
                m(h("meta::pure::functions::asserts::assertFalse_Boolean_1__String_1__Any_MANY__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 3)),
                m(h("meta::pure::functions::asserts::assertFalse_Boolean_1__Function_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 2 && !typeOne(ps.get(1), "String")))));

        register(m(m(h("meta::pure::functions::asserts::assertInstanceOf_Any_1__Type_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 2)),
                m(h("meta::pure::functions::asserts::assertInstanceOf_Any_1__Type_1__String_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 3 && typeOne(ps.get(2), "String"))),
                m(h("meta::pure::functions::asserts::assertInstanceOf_Any_1__Type_1__String_1__Any_MANY__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 4)),
                m(h("meta::pure::functions::asserts::assertInstanceOf_Any_1__Type_1__Function_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 3 && !typeOne(ps.get(2), "String")))));

        register(m(m(h("meta::pure::functions::asserts::assertIs_Any_1__Any_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 2)),
                m(h("meta::pure::functions::asserts::assertIs_Any_1__Any_1__String_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 3 && typeOne(ps.get(2), "String"))),
                m(h("meta::pure::functions::asserts::assertIs_Any_1__Any_1__String_1__Any_MANY__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 4)),
                m(h("meta::pure::functions::asserts::assertIs_Any_1__Any_1__Function_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 3 && !typeOne(ps.get(2), "String")))));

        register(m(m(h("meta::pure::functions::asserts::assertIsNot_Any_1__Any_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 2)),
                m(h("meta::pure::functions::asserts::assertIsNot_Any_1__Any_1__String_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 3 && typeOne(ps.get(2), "String"))),
                m(h("meta::pure::functions::asserts::assertIsNot_Any_1__Any_1__String_1__Any_MANY__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 4)),
                m(h("meta::pure::functions::asserts::assertIsNot_Any_1__Any_1__Function_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 3 && !typeOne(ps.get(2), "String")))));

        register(m(m(h("meta::pure::functions::asserts::assertNotContains_Any_MANY__Any_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 2)),
                m(h("meta::pure::functions::asserts::assertNotContains_Any_MANY__Any_1__String_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 3 && typeOne(ps.get(2), "String"))),
                m(h("meta::pure::functions::asserts::assertNotContains_Any_MANY__Any_1__String_1__Any_MANY__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 4)),
                m(h("meta::pure::functions::asserts::assertNotContains_Any_MANY__Any_1__Function_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 3 && !typeOne(ps.get(2), "String")))));

        register(m(m(h("meta::pure::functions::asserts::assertNotEmpty_Any_MANY__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 1)),
                m(h("meta::pure::functions::asserts::assertNotEmpty_Any_MANY__String_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 2 && typeOne(ps.get(1), "String"))),
                m(h("meta::pure::functions::asserts::assertNotEmpty_Any_MANY__String_1__Any_MANY__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 3)),
                m(h("meta::pure::functions::asserts::assertNotEmpty_Any_MANY__Function_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 2 && !typeOne(ps.get(1), "String")))));

        register(m(m(h("meta::pure::functions::asserts::assertNotEq_Any_1__Any_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 2)),
                m(h("meta::pure::functions::asserts::assertNotEq_Any_1__Any_1__String_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 3 && typeOne(ps.get(2), "String"))),
                m(h("meta::pure::functions::asserts::assertNotEq_Any_1__Any_1__String_1__Any_MANY__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 4)),
                m(h("meta::pure::functions::asserts::assertNotEq_Any_1__Any_1__Function_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 3 && !typeOne(ps.get(2), "String")))));

        register(m(m(h("meta::pure::functions::asserts::assertNotEquals_Any_MANY__Any_MANY__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 2)),
                m(h("meta::pure::functions::asserts::assertNotEquals_Any_MANY__Any_MANY__String_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 3 && typeOne(ps.get(2), "String"))),
                m(h("meta::pure::functions::asserts::assertNotEquals_Any_MANY__Any_MANY__String_1__Any_MANY__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 4)),
                m(h("meta::pure::functions::asserts::assertNotEquals_Any_MANY__Any_MANY__Function_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 3 && !typeOne(ps.get(2), "String")))));

        register(m(m(h("meta::pure::functions::asserts::assertNotSize_Any_MANY__Integer_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 2)),
                m(h("meta::pure::functions::asserts::assertNotSize_Any_MANY__Integer_1__String_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 3 && typeOne(ps.get(2), "String"))),
                m(h("meta::pure::functions::asserts::assertNotSize_Any_MANY__Integer_1__String_1__Any_MANY__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 4)),
                m(h("meta::pure::functions::asserts::assertNotSize_Any_MANY__Integer_1__Function_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 3 && !typeOne(ps.get(2), "String")))));

        register(m(m(h("meta::pure::functions::asserts::assertSameElements_Any_MANY__Any_MANY__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 2)),
                m(h("meta::pure::functions::asserts::assertSameElements_Any_MANY__Any_MANY__String_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 3 && typeOne(ps.get(2), "String"))),
                m(h("meta::pure::functions::asserts::assertSameElements_Any_MANY__Any_MANY__String_1__Any_MANY__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 4)),
                m(h("meta::pure::functions::asserts::assertSameElements_Any_MANY__Any_MANY__Function_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 3 && !typeOne(ps.get(2), "String")))));

        register(m(m(h("meta::pure::functions::asserts::assertSize_Any_MANY__Integer_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 2)),
                m(h("meta::pure::functions::asserts::assertSize_Any_MANY__Integer_1__String_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 3 && typeOne(ps.get(2), "String"))),
                m(h("meta::pure::functions::asserts::assertSize_Any_MANY__Integer_1__String_1__Any_MANY__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 4)),
                m(h("meta::pure::functions::asserts::assertSize_Any_MANY__Integer_1__Function_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 3 && !typeOne(ps.get(2), "String")))));

        register(m(m(h("meta::pure::functions::asserts::fail__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 0)),
                m(h("meta::pure::functions::asserts::fail_String_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 1 && typeOne(ps.get(0), "String"))),
                m(h("meta::pure::functions::asserts::fail_String_1__Any_MANY__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 2)),
                m(h("meta::pure::functions::asserts::fail_Function_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 1 && !typeOne(ps.get(0), "String")))));
    }

    private void registerTDS()
    {
        register(h("meta::pure::tds::renameColumns_TabularDataSet_1__Pair_MANY__TabularDataSet_1_", false, ps -> res("meta::pure::tds::TabularDataSet", "one"), ps -> true));

        register(m(grp(LambdaColCollectionInference, h("meta::pure::tds::projectWithColumnSubset_T_MANY__ColumnSpecification_MANY__String_MANY__TabularDataSet_1_", false, ps -> res("meta::pure::tds::TabularDataSet", "one"), ps -> "ColumnSpecification".equals(ps.get(1)._genericType()._rawType()._name()))),
                grp(LambdaCollectionInference, h("meta::pure::tds::projectWithColumnSubset_T_MANY__Function_MANY__String_MANY__String_MANY__TabularDataSet_1_", false, ps -> res("meta::pure::tds::TabularDataSet", "one"), ps -> true))));

        register("meta::pure::tds::restrict_TabularDataSet_1__String_MANY__TabularDataSet_1_", false, ps -> res("meta::pure::tds::TabularDataSet", "one"));
        register("meta::pure::tds::restrictDistinct_TabularDataSet_1__String_MANY__TabularDataSet_1_", false, ps -> res("meta::pure::tds::TabularDataSet", "one"));

        register("meta::pure::tds::asc_String_1__SortInformation_1_", false, ps -> res("meta::pure::tds::SortInformation", "one"));
        register("meta::pure::tds::desc_String_1__SortInformation_1_", false, ps -> res("meta::pure::tds::SortInformation", "one"));

        register(m(
                m(h("meta::pure::functions::collection::sort_T_m__T_m_", false, ps -> res(ps.get(0)._genericType(), ps.get(0)._multiplicity()), ps -> ps.size() == 1)),
                m(grp(TwoParameterLambdaInference, h("meta::pure::functions::collection::sort_T_m__Function_$0_1$__T_m_", false, ps -> res(ps.get(0)._genericType(), ps.get(0)._multiplicity()), ps -> ps.size() == 2))),
                m(grp(LambdaInference, h("meta::pure::functions::collection::sort_T_m__Function_$0_1$__Function_$0_1$__T_m_", true, ps -> res(ps.get(0)._genericType(), ps.get(0)._multiplicity()), ps -> ps.size() == 3))),
                m(h("meta::pure::tds::sort_TabularDataSet_1__String_1__SortDirection_1__TabularDataSet_1_", false, ps -> res("meta::pure::tds::TabularDataSet", "one"), ps -> ps.size() == 3)),
                m(h("meta::pure::tds::sort_TabularDataSet_1__String_MANY__TabularDataSet_1_", false, ps -> res("meta::pure::tds::TabularDataSet", "one"), ps -> ps.size() == 2 && "String".equals(ps.get(1)._genericType()._rawType()._name())),
                        h("meta::pure::tds::sort_TabularDataSet_1__SortInformation_MANY__TabularDataSet_1_", false, ps -> res("meta::pure::tds::TabularDataSet", "one"), ps -> true))));

        register(m(m(h("meta::pure::mapping::from_T_m__SingleExecutionParameters_1__T_m_", false, ps -> res(ps.get(0)._genericType(), ps.get(0)._multiplicity()), ps -> ps.size() == 2)),
                m(h("meta::pure::mapping::from_TabularDataSet_1__Mapping_1__Runtime_1__TabularDataSet_1_", false, ps -> res("meta::pure::tds::TabularDataSet", "one"), ps -> ps.size() == 3 && "TabularDataSet".equals(ps.get(0)._genericType()._rawType()._name())),
                    h("meta::pure::mapping::from_T_m__Mapping_1__Runtime_1__T_m_", false, ps -> res(ps.get(0)._genericType(), ps.get(0)._multiplicity()), ps -> ps.size() == 3)),
                m(h("meta::pure::mapping::from_TabularDataSet_1__Mapping_1__Runtime_1__ExecutionContext_1__TabularDataSet_1_", false, ps -> res(ps.get(0)._genericType(), ps.get(0)._multiplicity()), ps -> ps.size() == 4))));

        register(m(grp(LambdaCollectionInference, h("meta::pure::tds::tdsContains_T_1__Function_MANY__TabularDataSet_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 3)),
                grp(TDSContainsInference, h("meta::pure::tds::tdsContains_T_1__Function_MANY__String_MANY__TabularDataSet_1__Function_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> true))));

        register(m(m(grp(TDSOLAPInference, h("meta::pure::tds::olapGroupBy_TabularDataSet_1__String_MANY__SortInformation_$0_1$__OlapOperation_1__String_1__TabularDataSet_1_", false, ps -> res("meta::pure::tds::TabularDataSet", "one"), ps -> ps.size() == 5 && "OlapOperation".equals(ps.get(3)._genericType()._rawType()._name()))),
                m(grp(TDSOLAPInference, h("meta::pure::tds::olapGroupBy_TabularDataSet_1__String_MANY__SortInformation_$0_1$__FunctionDefinition_1__String_1__TabularDataSet_1_", false, ps -> res("meta::pure::tds::TabularDataSet", "one"), ps -> ps.size() == 5 && "FunctionDefinition".equals(ps.get(3)._genericType()._rawType()._name())))),
                m(grp(TDSOLAPInference, h("meta::pure::tds::olapGroupBy_TabularDataSet_1__String_MANY__OlapOperation_1__String_1__TabularDataSet_1_", false, ps -> res("meta::pure::tds::TabularDataSet", "one"), ps -> ps.size() == 4 && typeMany(ps.get(1), "String") && "OlapOperation".equals(ps.get(2)._genericType()._rawType()._name())))),
                m(grp(TDSOLAPInference, h("meta::pure::tds::olapGroupBy_TabularDataSet_1__String_MANY__FunctionDefinition_1__String_1__TabularDataSet_1_", false, ps -> res("meta::pure::tds::TabularDataSet", "one"), ps -> ps.size() == 4 && typeMany(ps.get(1), "String") && "FunctionDefinition".equals(ps.get(2)._genericType()._rawType()._name())))),
                m(grp(TDSOLAPInference, h("meta::pure::tds::olapGroupBy_TabularDataSet_1__SortInformation_$0_1$__OlapOperation_1__String_1__TabularDataSet_1_", false, ps -> res("meta::pure::tds::TabularDataSet", "one"), ps -> ps.size() == 4 && "OlapOperation".equals(ps.get(2)._genericType()._rawType()._name())))),
                m(grp(TDSOLAPInference, h("meta::pure::tds::olapGroupBy_TabularDataSet_1__SortInformation_$0_1$__FunctionDefinition_1__String_1__TabularDataSet_1_", false, ps -> res("meta::pure::tds::TabularDataSet", "one"), ps -> ps.size() == 4 && "SortInformation".equals(ps.get(1)._genericType()._rawType()._name()) && "FunctionDefinition".equals(ps.get(2)._genericType()._rawType()._name())))),
                m(grp(TDSOLAPInference, h("meta::pure::tds::olapGroupBy_TabularDataSet_1__OlapOperation_1__String_1__TabularDataSet_1_", false, ps -> res("meta::pure::tds::TabularDataSet", "one"), ps -> ps.size() == 3 && "OlapOperation".equals(ps.get(1)._genericType()._rawType()._name())))),
                m(grp(TDSOLAPInference, h("meta::pure::tds::olapGroupBy_TabularDataSet_1__FunctionDefinition_1__String_1__TabularDataSet_1_", false, ps -> res("meta::pure::tds::TabularDataSet", "one"), ps -> ps.size() == 3 && "FunctionDefinition".equals(ps.get(1)._genericType()._rawType()._name()))))
        )));

        register(
                m(m(grp(OLAPFuncNumInference, h("meta::pure::tds::func_String_1__FunctionDefinition_1__TdsOlapAggregation_1_", false, ps -> res("meta::pure::tds::TdsOlapAggregation", "one"), ps -> ps.size() == 2))),
                        m(grp(OLAPFuncTDSInference, h("meta::pure::tds::func_FunctionDefinition_1__TdsOlapRank_1_", false, ps -> res("meta::pure::tds::TdsOlapRank", "one"), ps -> ps.size() == 1)))));
    }

    private void registerDates()
    {
        register(h("meta::pure::functions::date::dateDiff_Date_1__Date_1__DurationUnit_1__Integer_1_", true, ps -> res("Integer", "one"), ps -> typeOne(ps.get(0), DATE) && typeOne(ps.get(1), DATE)),
                h("meta::pure::functions::date::dateDiff_Date_$0_1$__Date_$0_1$__DurationUnit_1__Integer_$0_1$_", false, ps -> res("Integer", "zeroOne"), ps -> true));

        register(h("meta::pure::functions::date::datePart_Date_1__Date_1_", true, ps -> res("Date", "one"), ps -> typeOne(ps.get(0), DATE)),
                h("meta::pure::functions::date::datePart_Date_$0_1$__Date_$0_1$_", false, ps -> res("Date", "zeroOne"), ps -> typeZeroOne(ps.get(0), DATE)));

        register(h("meta::pure::functions::date::dayOfWeek_Date_1__DayOfWeek_1_", false, ps -> res("meta::pure::functions::date::DayOfWeek", "one"), ps -> typeOne(ps.get(0), DATE)),
                h("meta::pure::functions::date::dayOfWeek_Integer_1__DayOfWeek_1_", false, ps -> res("meta::pure::functions::date::DayOfWeek", "one"), ps -> typeOne(ps.get(0), "Integer")));

        register(h("meta::pure::functions::date::daysOfMonth_Date_1__Integer_MANY_", false, ps -> res("Integer", "zeroMany"), ps -> true));

        register("meta::pure::functions::date::firstDayOfMonth_Date_1__Date_1_", false, ps -> res("Date", "one"));
        register("meta::pure::functions::date::firstDayOfQuarter_Date_1__StrictDate_1_", false, ps -> res("StrictDate", "one"));
        register("meta::pure::functions::date::firstDayOfThisMonth__Date_1_", false, ps -> res("Date", "one"));
        register("meta::pure::functions::date::firstDayOfThisQuarter__StrictDate_1_", false, ps -> res("StrictDate", "one"));
        register("meta::pure::functions::date::firstDayOfThisWeek__Date_1_", false, ps -> res("Date", "one"));
        register("meta::pure::functions::date::firstDayOfThisYear__Date_1_", false, ps -> res("Date", "one"));
        register("meta::pure::functions::date::firstDayOfWeek_Date_1__Date_1_", false, ps -> res("Date", "one"));
        register("meta::pure::functions::date::firstDayOfYear_Date_1__Date_1_", false, ps -> res("Date", "one"));

        register("meta::pure::functions::date::hasYear_Date_1__Boolean_1_", false, ps -> res("Boolean", "one"));

        register(h("meta::pure::functions::date::isAfterDay_Date_1__Date_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeOne(ps.get(0), DATE) && typeOne(ps.get(1), DATE)),
                h("meta::pure::functions::date::isAfterDay_Date_$0_1$__Date_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeZeroOne(ps.get(0), DATE) && typeOne(ps.get(1), DATE)),
                h("meta::pure::functions::date::isAfterDay_Date_1__Date_$0_1$__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeOne(ps.get(0), DATE) && typeZeroOne(ps.get(1), DATE)),
                h("meta::pure::functions::date::isAfterDay_Date_$0_1$__Date_$0_1$__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeZeroOne(ps.get(0), DATE) && typeZeroOne(ps.get(1), DATE)));

        register(h("meta::pure::functions::date::isBeforeDay_Date_1__Date_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeOne(ps.get(0), DATE) && typeOne(ps.get(1), DATE)),
                h("meta::pure::functions::date::isBeforeDay_Date_$0_1$__Date_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeZeroOne(ps.get(0), DATE) && typeOne(ps.get(1), DATE)),
                h("meta::pure::functions::date::isBeforeDay_Date_1__Date_$0_1$__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeOne(ps.get(0), DATE) && typeZeroOne(ps.get(1), DATE)),
                h("meta::pure::functions::date::isBeforeDay_Date_$0_1$__Date_$0_1$__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeZeroOne(ps.get(0), DATE) && typeZeroOne(ps.get(1), DATE)));

        register(h("meta::pure::functions::date::isOnDay_Date_1__Date_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeOne(ps.get(0), DATE) && typeOne(ps.get(1), DATE)),
                h("meta::pure::functions::date::isOnDay_Date_$0_1$__Date_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeZeroOne(ps.get(0), DATE) && typeOne(ps.get(1), DATE)),
                h("meta::pure::functions::date::isOnDay_Date_1__Date_$0_1$__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeOne(ps.get(0), DATE) && typeZeroOne(ps.get(1), DATE)),
                h("meta::pure::functions::date::isOnDay_Date_$0_1$__Date_$0_1$__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeZeroOne(ps.get(0), DATE) && typeZeroOne(ps.get(1), DATE)));

        register(h("meta::pure::functions::date::isOnOrAfterDay_Date_1__Date_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeOne(ps.get(0), DATE) && typeOne(ps.get(1), DATE)),
                h("meta::pure::functions::date::isOnOrAfterDay_Date_$0_1$__Date_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeZeroOne(ps.get(0), DATE) && typeOne(ps.get(1), DATE)),
                h("meta::pure::functions::date::isOnOrAfterDay_Date_1__Date_$0_1$__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeOne(ps.get(0), DATE) && typeZeroOne(ps.get(1), DATE)),
                h("meta::pure::functions::date::isOnOrAfterDay_Date_$0_1$__Date_$0_1$__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeZeroOne(ps.get(0), DATE) && typeZeroOne(ps.get(1), DATE)));

        register(h("meta::pure::functions::date::isOnOrBeforeDay_Date_1__Date_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeOne(ps.get(0), DATE) && typeOne(ps.get(1), DATE)),
                h("meta::pure::functions::date::isOnOrBeforeDay_Date_$0_1$__Date_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeZeroOne(ps.get(0), DATE) && typeOne(ps.get(1), DATE)),
                h("meta::pure::functions::date::isOnOrBeforeDay_Date_1__Date_$0_1$__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeOne(ps.get(0), DATE) && typeZeroOne(ps.get(1), DATE)),
                h("meta::pure::functions::date::isOnOrBeforeDay_Date_$0_1$__Date_$0_1$__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeZeroOne(ps.get(0), DATE) && typeZeroOne(ps.get(1), DATE)));

        register(h("meta::pure::functions::date::monthNumber_Date_1__Integer_1_", true, ps -> res("Integer", "one"), ps -> typeOne(ps.get(0), DATE)),
                h("meta::pure::functions::date::monthNumber_Date_$0_1$__Integer_$0_1$_", false, ps -> res("Integer", "zeroOne"), ps -> typeZeroOne(ps.get(0), DATE)));

        register(h("meta::pure::functions::date::month_Date_1__Month_1_", false, ps -> res("meta::pure::functions::date::Month", "one"), ps -> typeOne(ps.get(0), DATE)),
                h("meta::pure::functions::date::month_Integer_1__Month_1_", false, ps -> res("meta::pure::functions::date::Month", "one"), ps -> typeOne(ps.get(0), "Integer")));

        register(m(m(h("meta::pure::functions::date::mostRecentDayOfWeek_Date_1__DayOfWeek_1__Date_1_", false, ps -> res("Date", "one"), ps -> ps.size() == 2)),
                m(h("meta::pure::functions::date::mostRecentDayOfWeek_DayOfWeek_1__Date_1_", false, ps -> res("Date", "one"), ps -> true))));

        register(m(m(h("meta::pure::functions::date::previousDayOfWeek_Date_1__DayOfWeek_1__Date_1_", false, ps -> res("Date", "one"), ps -> ps.size() == 2)),
                m(h("meta::pure::functions::date::previousDayOfWeek_DayOfWeek_1__Date_1_", false, ps -> res("Date", "one"), ps -> true))));

        register("meta::pure::functions::date::quarterNumber_Date_1__Integer_1_", false, ps -> res("Integer", "one"));

        register(h("meta::pure::functions::date::quarter_Date_1__Quarter_1_", false, ps -> res("meta::pure::functions::date::Quarter", "one"), ps -> typeOne(ps.get(0), DATE)),
                h("meta::pure::functions::date::quarter_Integer_1__Quarter_1_", false, ps -> res("meta::pure::functions::date::Quarter", "one"), ps -> typeOne(ps.get(0), "Integer")));

        register(h("meta::pure::functions::date::weekOfYear_Date_1__Integer_1_", true, ps -> res("Integer", "one"), ps -> isOne(ps.get(0)._multiplicity())),
                h("meta::pure::functions::date::weekOfYear_Date_$0_1$__Integer_$0_1$_", false, ps -> res("Integer", "zeroOne"), ps -> true));

        register(h("meta::pure::functions::date::year_Date_1__Integer_1_", true, ps -> res("Integer", "one"), ps -> typeOne(ps.get(0), DATE)),
                h("meta::pure::functions::date::year_Date_$0_1$__Integer_$0_1$_", false, ps -> res("Integer", "zeroOne"), ps -> typeZeroOne(ps.get(0), DATE)));

        register("meta::pure::functions::date::adjust_Date_1__Integer_1__DurationUnit_1__Date_1_", true, ps -> res("Date", "one"));

        register(m(m(h("meta::pure::functions::date::date_Integer_1__Date_1_", true, ps -> res("Date", "one"), ps -> ps.size() == 1)),
                m(h("meta::pure::functions::date::date_Integer_1__Integer_1__Date_1_", true, ps -> res("Date", "one"), ps -> ps.size() == 2)),
                m(h("meta::pure::functions::date::date_Integer_1__Integer_1__Integer_1__StrictDate_1_", true, ps -> res("StrictDate", "one"), ps -> ps.size() == 3)),
                m(h("meta::pure::functions::date::date_Integer_1__Integer_1__Integer_1__Integer_1__DateTime_1_", true, ps -> res("DateTime", "one"), ps -> ps.size() == 4)),
                m(h("meta::pure::functions::date::date_Integer_1__Integer_1__Integer_1__Integer_1__Integer_1__DateTime_1_", true, ps -> res("DateTime", "one"), ps -> ps.size() == 5)),
                m(h("meta::pure::functions::date::date_Integer_1__Integer_1__Integer_1__Integer_1__Integer_1__Number_1__DateTime_1_", true, ps -> res("DateTime", "one"), ps -> ps.size() == 6))));

        register("meta::pure::functions::date::dayOfMonth_Date_1__Integer_1_", true, ps -> res("Integer", "one"));

        register(m(m(h("meta::pure::functions::date::dayOfWeekNumber_Date_1__Integer_1_", true, ps -> res("Integer", "one"), ps -> ps.size() == 1)),
                m(h("meta::pure::functions::date::dayOfWeekNumber_Date_1__DayOfWeek_1__Integer_1_", false, ps -> res("Integer", "one"), ps -> ps.size() == 2))));

        register(h("meta::pure::functions::date::hasDay_Date_1__Boolean_1_", true, ps -> res("Boolean", "one"), ps -> typeOne(ps.get(0), DATE)));
        register(h("meta::pure::functions::date::hasHour_Date_1__Boolean_1_", true, ps -> res("Boolean", "one"), ps -> typeOne(ps.get(0), DATE)));
        register(h("meta::pure::functions::date::hasMinute_Date_1__Boolean_1_", true, ps -> res("Boolean", "one"), ps -> typeOne(ps.get(0), DATE)));
        register(h("meta::pure::functions::date::hasMonth_Date_1__Boolean_1_", true, ps -> res("Boolean", "one"), ps -> typeOne(ps.get(0), DATE)));
        register(h("meta::pure::functions::date::hasSecond_Date_1__Boolean_1_", true, ps -> res("Boolean", "one"), ps -> typeOne(ps.get(0), DATE)));
        register(h("meta::pure::functions::date::hasSubsecondWithAtLeastPrecision_Date_1__Integer_1__Boolean_1_", true, ps -> res("Boolean", "one"), ps -> typeOne(ps.get(0), DATE) && typeOne(ps.get(1), "Integer")));
        register(h("meta::pure::functions::date::hasSubsecond_Date_1__Boolean_1_", true, ps -> res("Boolean", "one"), ps -> typeOne(ps.get(0), DATE)));

        register("meta::pure::functions::date::hour_Date_1__Integer_1_", true, ps -> res("Integer", "one"));
        register("meta::pure::functions::date::minute_Date_1__Integer_1_", true, ps -> res("Integer", "one"));
        register("meta::pure::functions::date::second_Date_1__Integer_1_", true, ps -> res("Integer", "one"));

        register("meta::pure::functions::date::now__DateTime_1_", true, ps -> res("DateTime", "one"));
        register("meta::pure::functions::date::today__StrictDate_1_", true, ps -> res("StrictDate", "one"));
    }

    private void registerStrings()
    {
        register(h("meta::pure::functions::string::endsWith_String_1__String_1__Boolean_1_", true, ps -> res("Boolean", "one"), ps -> typeOne(ps.get(0), "String")),
                h("meta::pure::functions::string::endsWith_String_$0_1$__String_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeZeroOne(ps.get(0), "String")));
        register("meta::pure::functions::string::equalIgnoreCase_String_1__String_1__Boolean_1_", false, ps -> res("Boolean", "one"));
        register("meta::pure::functions::string::humanize_String_1__String_1_", false, ps -> res("String", "one"));
        register("meta::pure::functions::string::isLowerCase_String_1__Boolean_1_", false, ps -> res("Boolean", "one"));
        register("meta::pure::functions::string::isUpperCase_String_1__Boolean_1_", false, ps -> res("Boolean", "one"));
        register(m(m(h("meta::pure::functions::string::joinStrings_String_MANY__String_1__String_1_", false, ps -> res("String", "one"), ps -> ps.size() == 2)),
                m(h("meta::pure::functions::string::joinStrings_String_MANY__String_1__String_1__String_1__String_1_", true, ps -> res("String", "one"), ps -> ps.size() == 4)),
                m(h("meta::pure::functions::string::joinStrings_String_MANY__String_1_", false, ps -> res("String", "one"), ps -> true))));
        register("meta::pure::functions::string::lastIndexOf_String_1__String_1__Integer_1_", false, ps -> res("Integer", "one"));
        register(m(m(h("meta::pure::functions::string::makeCamelCase_String_1__Boolean_1__String_1_", false, ps -> res("String", "one"), ps -> ps.size() == 2)),
                m(h("meta::pure::functions::string::makeCamelCase_String_1__String_1_", false, ps -> res("String", "one"), ps -> true))));
        register(m(m(h("meta::pure::functions::string::isDigit_String_1__Integer_1__Integer_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 3)),
                m(h("meta::pure::functions::string::isDigit_String_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> true))));
        register(m(m(h("meta::pure::functions::string::isLetter_String_1__Integer_1__Integer_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> ps.size() == 3)),
                m(h("meta::pure::functions::string::isLetter_String_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> true))));
        register(m(m(h("meta::pure::functions::string::makeString_Pair_MANY__String_1_", false, ps -> res("String", "one"), ps -> ps.size() == 1 && typeMany(ps.get(0), "Pair")),
                        h("meta::pure::functions::string::makeString_Any_MANY__String_1_", false, ps -> res("String", "one"), ps -> ps.size() == 1)),
                m(h("meta::pure::functions::string::makeString_Any_MANY__String_1__String_1_", false, ps -> res("String", "one"), ps -> ps.size() == 2)),
                m(h("meta::pure::functions::string::makeString_Any_MANY__String_1__String_1__String_1__String_1_", false, ps -> res("String", "one"), ps -> ps.size() == 4))));
        register("meta::pure::functions::string::splitOnCamelCase_String_1__String_MANY_", false, ps -> res("String", "zeroMany"));
        register(h("meta::pure::functions::string::startsWith_String_1__String_1__Boolean_1_", true, ps -> res("Boolean", "one"), ps -> isOne(ps.get(0)._multiplicity())),
                h("meta::pure::functions::string::startsWith_String_$0_1$__String_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> true));
        register("meta::pure::functions::string::substringAfter_String_1__String_1__String_1_", false, ps -> res("String", "one"));
        register("meta::pure::functions::string::substringBefore_String_1__String_1__String_1_", false, ps -> res("String", "one"));
        register("meta::pure::functions::string::chunk_String_1__Integer_1__String_MANY_", true, ps -> res("String", "zeroMany"));
        register("meta::pure::functions::string::format_String_1__Any_MANY__String_1_", true, ps -> res("String", "one"));
        register("meta::pure::functions::string::length_String_1__Integer_1_", true, ps -> res("Integer", "one"));
        register("meta::pure::functions::string::parseBoolean_String_1__Boolean_1_", true, ps -> res("Boolean", "one"));
        register("meta::pure::functions::string::parseDate_String_1__Date_1_", true, ps -> res("Date", "one"));
        register("meta::pure::functions::string::parseFloat_String_1__Float_1_", true, ps -> res("Float", "one"));
        register("meta::pure::functions::string::parseInteger_String_1__Integer_1_", true, ps -> res("Integer", "one"));
        register("meta::pure::functions::string::parseDecimal_String_1__Decimal_1_", true, ps -> res("Decimal", "one"));
        register("meta::pure::functions::string::replace_String_1__String_1__String_1__String_1_", true, ps -> res("String", "one"));
        register("meta::pure::functions::string::split_String_1__String_1__String_MANY_", true, ps -> res("String", "zeroMany"));
        register(m(m(h("meta::pure::functions::string::substring_String_1__Integer_1__Integer_1__String_1_", true, ps -> res("String", "one"), ps -> ps.size() == 3)),
                m(h("meta::pure::functions::string::substring_String_1__Integer_1__String_1_", true, ps -> res("String", "one"), ps -> true))));
        register("meta::pure::functions::string::toLower_String_1__String_1_", true, ps -> res("String", "one"));
        register("meta::pure::functions::string::toString_Any_1__String_1_", true, ps -> res("String", "one"));
        register("meta::pure::functions::string::toUpper_String_1__String_1_", true, ps -> res("String", "one"));
        register("meta::pure::functions::string::trim_String_1__String_1_", true, ps -> res("String", "one"));
        register("meta::pure::functions::string::matches_String_1__String_1__Boolean_1_", true, ps -> res("Boolean", "one"));
        register("meta::pure::functions::string::isAlphaNumeric_String_1__Boolean_1_", false, ps -> res("Boolean", "one"));
        register("meta::pure::functions::string::isNoLongerThan_String_$0_1$__Integer_1__Boolean_1_", false, ps -> res("Boolean", "one"));
        register("meta::pure::functions::string::isNoShorterThan_String_$0_1$__Integer_1__Boolean_1_", false, pp -> res("Boolean", "one"));

    }

    private void registerTrigo()
    {
        register("meta::pure::functions::math::cos_Number_1__Float_1_", true, ps -> res("Float", "one"));
        register("meta::pure::functions::math::sin_Number_1__Float_1_", true, ps -> res("Float", "one"));
        register("meta::pure::functions::math::tan_Number_1__Float_1_", true, ps -> res("Float", "one"));
        register("meta::pure::functions::math::asin_Number_1__Float_1_", true, ps -> res("Float", "one"));
        register("meta::pure::functions::math::acos_Number_1__Float_1_", true, ps -> res("Float", "one"));
        register("meta::pure::functions::math::atan_Number_1__Float_1_", true, ps -> res("Float", "one"));
        register("meta::pure::functions::math::atan2_Number_1__Number_1__Float_1_", true, ps -> res("Float", "one"));
        register("meta::pure::functions::math::toDegrees_Number_1__Float_1_", false, ps -> res("Float", "one"));
        register("meta::pure::functions::math::toRadians_Number_1__Float_1_", false, ps -> res("Float", "one"));
        register("meta::pure::functions::math::pi__Float_1_", false, ps -> res("Float", "one"));
        register("meta::pure::functions::math::earthRadius__Float_1_", false, ps -> res("Float", "one"));
        register("meta::pure::functions::math::distanceHaversineDegrees_Number_1__Number_1__Number_1__Number_1__Number_1_", false, ps -> res("Number", "one"));
        register("meta::pure::functions::math::distanceHaversineRadians_Number_1__Number_1__Number_1__Number_1__Number_1_", false, ps -> res("Number", "one"));
        register("meta::pure::functions::math::distanceSphericalLawOfCosinesDegrees_Number_1__Number_1__Number_1__Number_1__Number_1_", false, ps -> res("Number", "one"));
        register("meta::pure::functions::math::distanceSphericalLawOfCosinesRadians_Number_1__Number_1__Number_1__Number_1__Number_1_", false, ps -> res("Number", "one"));


    }

    private void registerMathInequalities()
    {
        register(h("meta::pure::functions::boolean::greaterThan_Boolean_1__Boolean_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeOne(ps.get(0), "Boolean") && typeOne(ps.get(1), "Boolean")),
                h("meta::pure::functions::boolean::greaterThan_Boolean_1__Boolean_$0_1$__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeOne(ps.get(0), "Boolean") && typeZeroOne(ps.get(1), "Boolean")),
                h("meta::pure::functions::boolean::greaterThan_Boolean_$0_1$__Boolean_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeZeroOne(ps.get(0), "Boolean") && typeOne(ps.get(1), "Boolean")),
                h("meta::pure::functions::boolean::greaterThan_Boolean_$0_1$__Boolean_$0_1$__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeZeroOne(ps.get(0), "Boolean") && typeZeroOne(ps.get(1), "Boolean")),
                h("meta::pure::functions::boolean::greaterThan_Date_1__Date_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeOne(ps.get(0), DATE) && typeOne(ps.get(1), DATE)),
                h("meta::pure::functions::boolean::greaterThan_Date_1__Date_$0_1$__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeOne(ps.get(0), DATE) && typeZeroOne(ps.get(1), DATE)),
                h("meta::pure::functions::boolean::greaterThan_Date_$0_1$__Date_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeZeroOne(ps.get(0), DATE) && typeOne(ps.get(1), DATE)),
                h("meta::pure::functions::boolean::greaterThan_Date_$0_1$__Date_$0_1$__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeZeroOne(ps.get(0), DATE) && typeZeroOne(ps.get(1), DATE)),
                h("meta::pure::functions::boolean::greaterThan_Number_1__Number_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeOne(ps.get(0), NUMBER) && typeOne(ps.get(1), NUMBER)),
                h("meta::pure::functions::boolean::greaterThan_Number_1__Number_$0_1$__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeOne(ps.get(0), NUMBER) && typeZeroOne(ps.get(1), NUMBER)),
                h("meta::pure::functions::boolean::greaterThan_Number_$0_1$__Number_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeZeroOne(ps.get(0), NUMBER) && typeOne(ps.get(1), NUMBER)),
                h("meta::pure::functions::boolean::greaterThan_Number_$0_1$__Number_$0_1$__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeZeroOne(ps.get(0), NUMBER) && typeZeroOne(ps.get(1), NUMBER)),
                h("meta::pure::functions::boolean::greaterThan_String_1__String_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeOne(ps.get(0), "String") && typeOne(ps.get(1), "String")),
                h("meta::pure::functions::boolean::greaterThan_String_1__String_$0_1$__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeOne(ps.get(0), "String") && typeZeroOne(ps.get(1), "String")),
                h("meta::pure::functions::boolean::greaterThan_String_$0_1$__String_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeZeroOne(ps.get(0), "String") && typeOne(ps.get(1), "String")),
                h("meta::pure::functions::boolean::greaterThan_String_$0_1$__String_$0_1$__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeZeroOne(ps.get(0), "String") && typeZeroOne(ps.get(1), "String")));

        register(h("meta::pure::functions::boolean::greaterThanEqual_Boolean_1__Boolean_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeOne(ps.get(0), "Boolean") && typeOne(ps.get(1), "Boolean")),
                h("meta::pure::functions::boolean::greaterThanEqual_Boolean_1__Boolean_$0_1$__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeOne(ps.get(0), "Boolean") && typeZeroOne(ps.get(1), "Boolean")),
                h("meta::pure::functions::boolean::greaterThanEqual_Boolean_$0_1$__Boolean_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeZeroOne(ps.get(0), "Boolean") && typeOne(ps.get(1), "Boolean")),
                h("meta::pure::functions::boolean::greaterThanEqual_Boolean_$0_1$__Boolean_$0_1$__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeZeroOne(ps.get(0), "Boolean") && typeZeroOne(ps.get(1), "Boolean")),
                h("meta::pure::functions::boolean::greaterThanEqual_Date_1__Date_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeOne(ps.get(0), DATE) && typeOne(ps.get(1), DATE)),
                h("meta::pure::functions::boolean::greaterThanEqual_Date_1__Date_$0_1$__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeOne(ps.get(0), DATE) && typeZeroOne(ps.get(1), DATE)),
                h("meta::pure::functions::boolean::greaterThanEqual_Date_$0_1$__Date_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeZeroOne(ps.get(0), DATE) && typeOne(ps.get(1), DATE)),
                h("meta::pure::functions::boolean::greaterThanEqual_Date_$0_1$__Date_$0_1$__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeZeroOne(ps.get(0), DATE) && typeZeroOne(ps.get(1), DATE)),
                h("meta::pure::functions::boolean::greaterThanEqual_Number_1__Number_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeOne(ps.get(0), NUMBER) && typeOne(ps.get(1), NUMBER)),
                h("meta::pure::functions::boolean::greaterThanEqual_Number_1__Number_$0_1$__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeOne(ps.get(0), NUMBER) && typeZeroOne(ps.get(1), NUMBER)),
                h("meta::pure::functions::boolean::greaterThanEqual_Number_$0_1$__Number_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeZeroOne(ps.get(0), NUMBER) && typeOne(ps.get(1), NUMBER)),
                h("meta::pure::functions::boolean::greaterThanEqual_Number_$0_1$__Number_$0_1$__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeZeroOne(ps.get(0), NUMBER) && typeZeroOne(ps.get(1), NUMBER)),
                h("meta::pure::functions::boolean::greaterThanEqual_String_1__String_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeOne(ps.get(0), "String") && typeOne(ps.get(1), "String")),
                h("meta::pure::functions::boolean::greaterThanEqual_String_1__String_$0_1$__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeOne(ps.get(0), "String") && typeZeroOne(ps.get(1), "String")),
                h("meta::pure::functions::boolean::greaterThanEqual_String_$0_1$__String_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeZeroOne(ps.get(0), "String") && typeOne(ps.get(1), "String")),
                h("meta::pure::functions::boolean::greaterThanEqual_String_$0_1$__String_$0_1$__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeZeroOne(ps.get(0), "String") && typeZeroOne(ps.get(1), "String")));

        register(h("meta::pure::functions::boolean::lessThan_Boolean_1__Boolean_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeOne(ps.get(0), "Boolean") && typeOne(ps.get(1), "Boolean")),
                h("meta::pure::functions::boolean::lessThan_Boolean_1__Boolean_$0_1$__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeOne(ps.get(0), "Boolean") && typeZeroOne(ps.get(1), "Boolean")),
                h("meta::pure::functions::boolean::lessThan_Boolean_$0_1$__Boolean_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeZeroOne(ps.get(0), "Boolean") && typeOne(ps.get(1), "Boolean")),
                h("meta::pure::functions::boolean::lessThan_Boolean_$0_1$__Boolean_$0_1$__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeZeroOne(ps.get(0), "Boolean") && typeZeroOne(ps.get(1), "Boolean")),
                h("meta::pure::functions::boolean::lessThan_Date_1__Date_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeOne(ps.get(0), DATE) && typeOne(ps.get(1), DATE)),
                h("meta::pure::functions::boolean::lessThan_Date_1__Date_$0_1$__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeOne(ps.get(0), DATE) && typeZeroOne(ps.get(1), DATE)),
                h("meta::pure::functions::boolean::lessThan_Date_$0_1$__Date_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeZeroOne(ps.get(0), DATE) && typeOne(ps.get(1), DATE)),
                h("meta::pure::functions::boolean::lessThan_Date_$0_1$__Date_$0_1$__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeZeroOne(ps.get(0), DATE) && typeZeroOne(ps.get(1), DATE)),
                h("meta::pure::functions::boolean::lessThan_Number_1__Number_1__Boolean_1_", true, ps -> res("Boolean", "one"), ps -> typeOne(ps.get(0), NUMBER) && typeOne(ps.get(1), NUMBER)),
                h("meta::pure::functions::boolean::lessThan_Number_1__Number_$0_1$__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeOne(ps.get(0), NUMBER) && typeZeroOne(ps.get(1), NUMBER)),
                h("meta::pure::functions::boolean::lessThan_Number_$0_1$__Number_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeZeroOne(ps.get(0), NUMBER) && typeOne(ps.get(1), NUMBER)),
                h("meta::pure::functions::boolean::lessThan_Number_$0_1$__Number_$0_1$__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeZeroOne(ps.get(0), NUMBER) && typeZeroOne(ps.get(1), NUMBER)),
                h("meta::pure::functions::boolean::lessThan_String_1__String_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeOne(ps.get(0), "String") && typeOne(ps.get(1), "String")),
                h("meta::pure::functions::boolean::lessThan_String_1__String_$0_1$__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeOne(ps.get(0), "String") && typeZeroOne(ps.get(1), "String")),
                h("meta::pure::functions::boolean::lessThan_String_$0_1$__String_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeZeroOne(ps.get(0), "String") && typeOne(ps.get(1), "String")),
                h("meta::pure::functions::boolean::lessThan_String_$0_1$__String_$0_1$__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeZeroOne(ps.get(0), "String") && typeZeroOne(ps.get(1), "String")));

        register(h("meta::pure::functions::boolean::lessThanEqual_Boolean_1__Boolean_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeOne(ps.get(0), "Boolean") && typeOne(ps.get(1), "Boolean")),
                h("meta::pure::functions::boolean::lessThanEqual_Boolean_1__Boolean_$0_1$__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeOne(ps.get(0), "Boolean") && typeZeroOne(ps.get(1), "Boolean")),
                h("meta::pure::functions::boolean::lessThanEqual_Boolean_$0_1$__Boolean_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeZeroOne(ps.get(0), "Boolean") && typeOne(ps.get(1), "Boolean")),
                h("meta::pure::functions::boolean::lessThanEqual_Boolean_$0_1$__Boolean_$0_1$__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeZeroOne(ps.get(0), "Boolean") && typeZeroOne(ps.get(1), "Boolean")),
                h("meta::pure::functions::boolean::lessThanEqual_Date_1__Date_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeOne(ps.get(0), DATE) && typeOne(ps.get(1), DATE)),
                h("meta::pure::functions::boolean::lessThanEqual_Date_1__Date_$0_1$__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeOne(ps.get(0), DATE) && typeZeroOne(ps.get(1), DATE)),
                h("meta::pure::functions::boolean::lessThanEqual_Date_$0_1$__Date_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeZeroOne(ps.get(0), DATE) && typeOne(ps.get(1), DATE)),
                h("meta::pure::functions::boolean::lessThanEqual_Date_$0_1$__Date_$0_1$__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeZeroOne(ps.get(0), DATE) && typeZeroOne(ps.get(1), DATE)),
                h("meta::pure::functions::boolean::lessThanEqual_Number_1__Number_1__Boolean_1_", true, ps -> res("Boolean", "one"), ps -> typeOne(ps.get(0), NUMBER) && typeOne(ps.get(1), NUMBER)),
                h("meta::pure::functions::boolean::lessThanEqual_Number_1__Number_$0_1$__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeOne(ps.get(0), NUMBER) && typeZeroOne(ps.get(1), NUMBER)),
                h("meta::pure::functions::boolean::lessThanEqual_Number_$0_1$__Number_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeZeroOne(ps.get(0), NUMBER) && typeOne(ps.get(1), NUMBER)),
                h("meta::pure::functions::boolean::lessThanEqual_Number_$0_1$__Number_$0_1$__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeZeroOne(ps.get(0), NUMBER) && typeZeroOne(ps.get(1), NUMBER)),
                h("meta::pure::functions::boolean::lessThanEqual_String_1__String_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeOne(ps.get(0), "String") && typeOne(ps.get(1), "String")),
                h("meta::pure::functions::boolean::lessThanEqual_String_1__String_$0_1$__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeOne(ps.get(0), "String") && typeZeroOne(ps.get(1), "String")),
                h("meta::pure::functions::boolean::lessThanEqual_String_$0_1$__String_1__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeZeroOne(ps.get(0), "String") && typeOne(ps.get(1), "String")),
                h("meta::pure::functions::boolean::lessThanEqual_String_$0_1$__String_$0_1$__Boolean_1_", false, ps -> res("Boolean", "one"), ps -> typeZeroOne(ps.get(0), "String") && typeZeroOne(ps.get(1), "String")));

    }

    private void registerMaxMin()
    {
        register(m(
                m(h("meta::pure::functions::math::max_Integer_1__Integer_1__Integer_1_", false, ps -> res("Integer", "one"), ps -> ps.size() == 2 && typeOne(ps.get(0), "Integer") && typeOne(ps.get(1), "Integer")),
                        h("meta::pure::functions::math::max_Float_1__Float_1__Float_1_", false, ps -> res("Float", "one"), ps -> ps.size() == 2 && typeOne(ps.get(0), "Float") && typeOne(ps.get(1), "Float")),
                        h("meta::pure::functions::math::max_Number_1__Number_1__Number_1_", false, ps -> res("Number", "one"), ps -> ps.size() == 2 && typeOne(ps.get(0), "Number") && typeOne(ps.get(1), "Number")),
                        h("meta::pure::functions::date::max_DateTime_1__DateTime_1__DateTime_1_", false, ps -> res("DateTime", "one"), ps -> ps.size() == 2 && typeOne(ps.get(0), "DateTime") && typeOne(ps.get(1), "DateTime")),
                        h("meta::pure::functions::date::max_StrictDate_1__StrictDate_1__StrictDate_1_", false, ps -> res("StrictDate", "one"), ps -> ps.size() == 2 && typeOne(ps.get(0), "StrictDate") && typeOne(ps.get(1), "StrictDate")),
                        h("meta::pure::functions::date::max_Date_1__Date_1__Date_1_", false, ps -> res("Date", "one"), ps -> ps.size() == 2 && typeOne(ps.get(0), "Date") && typeOne(ps.get(1), "Date"))),
                m(h("meta::pure::functions::math::max_Integer_$1_MANY$__Integer_1_", false, ps -> res("Integer", "one"), ps -> typeOneMany(ps.get(0), "Integer")),
                        h("meta::pure::functions::math::max_Integer_MANY__Integer_$0_1$_", false, ps -> res("Integer", "zeroOne"), ps -> typeMany(ps.get(0), "Integer")),
                        h("meta::pure::functions::math::max_Float_$1_MANY$__Float_1_", false, ps -> res("Float", "one"), ps -> typeOneMany(ps.get(0), "Float")),
                        h("meta::pure::functions::math::max_Float_MANY__Float_$0_1$_", false, ps -> res("Float", "zeroOne"), ps -> typeMany(ps.get(0), "Float")),
                        h("meta::pure::functions::math::max_Number_$1_MANY$__Number_1_", false, ps -> res("Number", "one"), ps -> typeOneMany(ps.get(0), "Number")),
                        h("meta::pure::functions::math::max_Number_MANY__Number_$0_1$_", false, ps -> res("Number", "zeroOne"), ps -> typeMany(ps.get(0), "Number")),
                        h("meta::pure::functions::date::max_DateTime_MANY__DateTime_$0_1$_", false, ps -> res("DateTime", "zeroOne"), ps -> typeMany(ps.get(0), "DateTime")),
                        h("meta::pure::functions::date::max_StrictDate_MANY__StrictDate_$0_1$_", false, ps -> res("StrictDate", "zeroOne"), ps -> typeMany(ps.get(0), "StrictDate")),
                        h("meta::pure::functions::date::max_Date_MANY__Date_$0_1$_", false, ps -> res("Date", "zeroOne"), ps -> typeMany(ps.get(0), "Date")),
                        h("meta::pure::functions::collection::max_X_MANY__X_$0_1$_", false, ps -> res(ps.get(0)._genericType(), "zeroOne")))));

        register(m(
                m(h("meta::pure::functions::math::min_Integer_1__Integer_1__Integer_1_", false, ps -> res("Integer", "one"), ps -> ps.size() == 2 && typeOne(ps.get(0), "Integer") && typeOne(ps.get(1), "Integer")),
                        h("meta::pure::functions::math::min_Float_1__Float_1__Float_1_", false, ps -> res("Float", "one"), ps -> ps.size() == 2 && typeOne(ps.get(0), "Float") && typeOne(ps.get(1), "Float")),
                        h("meta::pure::functions::math::min_Number_1__Number_1__Number_1_", false, ps -> res("Number", "one"), ps -> ps.size() == 2 && typeOne(ps.get(0), "Number") && typeOne(ps.get(1), "Number")),
                        h("meta::pure::functions::date::min_DateTime_1__DateTime_1__DateTime_1_", false, ps -> res("DateTime", "one"), ps -> ps.size() == 2 && typeOne(ps.get(0), "DateTime") && typeOne(ps.get(1), "DateTime")),
                        h("meta::pure::functions::date::min_StrictDate_1__StrictDate_1__StrictDate_1_", false, ps -> res("StrictDate", "one"), ps -> ps.size() == 2 && typeOne(ps.get(0), "StrictDate") && typeOne(ps.get(1), "StrictDate")),
                        h("meta::pure::functions::date::min_Date_1__Date_1__Date_1_", false, ps -> res("Date", "one"), ps -> ps.size() == 2 && typeOne(ps.get(0), "Date") && typeOne(ps.get(1), "Date"))),

                m(h("meta::pure::functions::math::min_Integer_$1_MANY$__Integer_1_", false, ps -> res("Integer", "one"), ps -> typeOneMany(ps.get(0), "Integer")),
                        h("meta::pure::functions::math::min_Integer_MANY__Integer_$0_1$_", false, ps -> res("Integer", "zeroOne"), ps -> typeMany(ps.get(0), "Integer")),
                        h("meta::pure::functions::math::min_Float_$1_MANY$__Float_1_", false, ps -> res("Float", "one"), ps -> typeOneMany(ps.get(0), "Float")),
                        h("meta::pure::functions::math::min_Float_MANY__Float_$0_1$_", false, ps -> res("Float", "zeroOne"), ps -> typeMany(ps.get(0), "Float")),
                        h("meta::pure::functions::math::min_Number_$1_MANY$__Number_1_", false, ps -> res("Number", "one"), ps -> typeOneMany(ps.get(0), "Number")),
                        h("meta::pure::functions::math::min_Number_MANY__Number_$0_1$_", false, ps -> res("Number", "zeroOne"), ps -> typeMany(ps.get(0), "Number")),
                        h("meta::pure::functions::date::min_DateTime_MANY__DateTime_$0_1$_", false, ps -> res("DateTime", "zeroOne"), ps -> typeMany(ps.get(0), "DateTime")),
                        h("meta::pure::functions::date::min_StrictDate_MANY__StrictDate_$0_1$_", false, ps -> res("StrictDate", "zeroOne"), ps -> typeMany(ps.get(0), "StrictDate")),
                        h("meta::pure::functions::date::min_Date_MANY__Date_$0_1$_", false, ps -> res("Date", "zeroOne"), ps -> typeMany(ps.get(0), "Date")))));
    }

    private void registerAlgebra()
    {
        register(h("meta::pure::functions::math::minus_Integer_MANY__Integer_1_", true, ps -> res("Integer", "one"), ps -> typeMany(ps.get(0), "Integer")),
                h("meta::pure::functions::math::minus_Float_MANY__Float_1_", true, ps -> res("Float", "one"), ps -> typeMany(ps.get(0), "Float")),
                h("meta::pure::functions::math::minus_Decimal_MANY__Decimal_1_", true, ps -> res("Decimal", "one"), ps -> typeMany(ps.get(0), "Decimal")),
                h("meta::pure::functions::math::minus_Number_MANY__Number_1_", true, ps -> res("Number", "one"), ps -> typeMany(ps.get(0), "Number")));

        register(h("meta::pure::functions::math::times_Integer_MANY__Integer_1_", true, ps -> res("Integer", "one"), ps -> typeMany(ps.get(0), "Integer")),
                h("meta::pure::functions::math::times_Float_MANY__Float_1_", true, ps -> res("Float", "one"), ps -> typeMany(ps.get(0), "Float")),
                h("meta::pure::functions::math::times_Number_MANY__Number_1_", true, ps -> res("Number", "one"), ps -> typeMany(ps.get(0), "Number")),
                h("meta::pure::functions::math::times_Decimal_MANY__Decimal_1_", true, ps -> res("Decimal", "one"), ps -> typeMany(ps.get(0), "Decimal")));


        register("meta::pure::functions::math::divide_Number_1__Number_1__Float_1_", true, ps -> res("Float", "one"));

        register(h("meta::pure::functions::string::plus_String_MANY__String_1_", false, ps -> res("String", "one"), ps -> ps.size() == 1 && typeMany(ps.get(0), "String")),
                h("meta::pure::functions::math::plus_Integer_MANY__Integer_1_", true, ps -> res("Integer", "one"), ps -> ps.size() == 1 && typeMany(ps.get(0), "Integer")),
                h("meta::pure::functions::math::plus_Float_MANY__Float_1_", true, ps -> res("Float", "one"), ps -> ps.size() == 1 && typeMany(ps.get(0), "Float")),
                h("meta::pure::functions::math::plus_Decimal_MANY__Decimal_1_", true, ps -> res("Decimal", "one"), ps -> ps.size() == 1 && typeMany(ps.get(0), "Decimal")),
                h("meta::pure::functions::math::plus_Number_MANY__Number_1_", true, ps -> res("Number", "one"), ps -> ps.size() == 1 && typeMany(ps.get(0), "Number")));

        register(h("meta::pure::functions::math::abs_Float_1__Float_1_", true, ps -> res("Float", "one"), ps -> typeOne(ps.get(0), "Float")),
                h("meta::pure::functions::math::abs_Integer_1__Integer_1_", true, ps -> res("Integer", "one"), ps -> typeOne(ps.get(0), "Integer")),
                h("meta::pure::functions::math::abs_Number_1__Number_1_", true, ps -> res("Number", "one"), ps -> typeOne(ps.get(0), "Number"))
        );

        register("meta::pure::functions::math::ceiling_Number_1__Integer_1_", true, ps -> res("Integer", "one"));
        register("meta::pure::functions::math::exp_Number_1__Float_1_", true, ps -> res("Float", "one"));
        register("meta::pure::functions::math::floor_Number_1__Integer_1_", true, ps -> res("Integer", "one"));
        register("meta::pure::functions::math::log_Number_1__Float_1_", true, ps -> res("Float", "one"));
        register("meta::pure::functions::math::mod_Integer_1__Integer_1__Integer_1_", true, ps -> res("Integer", "one"));
        register("meta::pure::functions::math::pow_Number_1__Number_1__Number_1_", true, ps -> res("Number", "one"));
        register("meta::pure::functions::math::rem_Number_1__Number_1__Number_1_", true, ps -> res("Number", "one"));
        register("meta::pure::functions::math::sqrt_Number_1__Float_1_", true, ps -> res("Float", "one"));

        register(m(m(h("meta::pure::functions::math::round_Float_1__Integer_1__Float_1_", true, ps -> res("Float", "one"), ps -> ps.size() == 2 && typeOne(ps.get(0), "Float"))),
                m(h("meta::pure::functions::math::round_Decimal_1__Integer_1__Decimal_1_", true, ps -> res("Decimal", "one"), ps -> ps.size() == 2 && typeOne(ps.get(0), "Decimal"))),
                m(h("meta::pure::functions::math::round_Number_1__Integer_1_", true, ps -> res("Integer", "one"), ps -> true))));
        register("meta::pure::functions::math::toFloat_Number_1__Float_1_", true, ps -> res("Float", "one"));
        register("meta::pure::functions::math::toDecimal_Number_1__Decimal_1_", true, ps -> res("Decimal", "one"));


    }

    private void registerOlapMath()
    {
        register("meta::pure::functions::math::olap::rank_Any_MANY__Map_1_", false, ps -> res("meta::pure::functions::collection::Map", "one"));
        register("meta::pure::functions::math::olap::averageRank_Any_MANY__Map_1_", false, ps -> res("meta::pure::functions::collection::Map", "one"));
        register("meta::pure::functions::math::olap::denseRank_Any_MANY__Map_1_", false, ps -> res("meta::pure::functions::collection::Map", "one"));
        register("meta::pure::functions::math::olap::rowNumber_Any_MANY__Map_1_", false, ps -> res("meta::pure::functions::collection::Map", "one"));
    }

    private void registerAggregations()
    {
        register(h("meta::pure::functions::math::average_Float_MANY__Float_1_", false, ps -> res("Float", "one"), ps -> typeMany(ps.get(0), "Float")),
                h("meta::pure::functions::math::average_Integer_MANY__Float_1_", false, ps -> res("Float", "one"), ps -> typeMany(ps.get(0), "Integer")),
                h("meta::pure::functions::math::average_Number_MANY__Float_1_", false, ps -> res("Float", "one"), ps -> typeMany(ps.get(0), "Number")));

        register(h("meta::pure::functions::math::mean_Float_MANY__Float_1_", false, ps -> res("Float", "one"), ps -> typeMany(ps.get(0), "Float")),
                h("meta::pure::functions::math::mean_Integer_MANY__Float_1_", false, ps -> res("Float", "one"), ps -> typeMany(ps.get(0), "Integer")),
                h("meta::pure::functions::math::mean_Number_MANY__Float_1_", false, ps -> res("Float", "one"), ps -> typeMany(ps.get(0), "Number")));

        register(h("meta::pure::functions::math::sum_Float_MANY__Float_1_", false, ps -> res("Float", "one"), ps -> typeMany(ps.get(0), "Float")),
                h("meta::pure::functions::math::sum_Integer_MANY__Integer_1_", false, ps -> res("Integer", "one"), ps -> typeMany(ps.get(0), "Integer")),
                h("meta::pure::functions::math::sum_Number_MANY__Number_1_", false, ps -> res("Number", "one"), ps -> typeMany(ps.get(0), "Number")));

        register(m(m(h("meta::pure::functions::math::percentile_Number_MANY__Float_1__Boolean_1__Boolean_1__Number_$0_1$_", false, ps -> res("Number", "zeroOne"), ps -> ps.size() == 4)),
                m(h("meta::pure::functions::math::percentile_Number_MANY__Float_1__Number_$0_1$_", false, ps -> res("Number", "zeroOne"), ps -> true))));
    }

    private void registerStdDeviations()
    {
        register(h("meta::pure::functions::math::stdDevPopulation_Number_$1_MANY$__Number_1_", false, ps -> res("Number", "one"), ps -> typeOneMany(ps.get(0), NUMBER)),
                h("meta::pure::functions::math::stdDevPopulation_Number_MANY__Number_1_", false, ps -> res("Number", "one"), ps -> typeMany(ps.get(0), NUMBER)));

        register(h("meta::pure::functions::math::stdDevSample_Number_$1_MANY$__Number_1_", false, ps -> res("Number", "one"), ps -> typeOneMany(ps.get(0), NUMBER)),
                h("meta::pure::functions::math::stdDevSample_Number_MANY__Number_1_", false, ps -> res("Number", "one"), ps -> typeMany(ps.get(0), NUMBER)));
    }

    private void registerJson()
    {
        register(m(m(h("meta::json::toJSON_Any_MANY__String_1_", false, ps -> res("String", "one"), ps -> ps.size() == 1)),
                m(h("meta::json::toJSON_T_MANY__LambdaFunction_MANY__String_1_", false, ps -> res(ps.get(0)._genericType(), "zeroMany"), ps -> ps.size() == 2 && typeMany(ps.get(1), "LambdaFunction")))));
    }

    private void registerRuntimeHelper()
    {
        register(h("meta::pure::runtime::getRuntimeWithModelConnection_Class_1__Any_MANY__Runtime_1_", false, ps -> res("meta::pure::runtime::Runtime", "one"), ps -> true));
        register(h("meta::pure::runtime::generateGuid__String_1_", true, ps -> res("String", "one"), ps -> true));
        register(h("meta::pure::runtime::currentUserId__String_1_", true, ps -> res("String", "one"), ps -> true));
    }

    private void registerUnitFunctions()
    {
        register(h("meta::pure::executionPlan::engine::java::unitType_Any_1__String_1_", false, ps -> res("String", "one"), ps -> typeOneMany(ps.get(0), "String")));
        register(h("meta::pure::executionPlan::engine::java::unitValue_Any_1__Number_1_", false, ps -> res("Number", "one"), ps -> typeOneMany(ps.get(0), NUMBER)));
        register(h("meta::pure::functions::meta::newUnit_Unit_1__Number_1__Any_1_", true, ps -> res("meta::pure::metamodel::type::Any", "one"), ps -> typeOneMany(ps.get(0), "Any")));
        register(h("meta::pure::executionPlan::engine::java::convert_Any_1__Unit_1__Any_1_", false, ps -> res("meta::pure::metamodel::type::Any", "one"), ps -> typeOneMany(ps.get(0), "Any")));
    }

    public Pair<SimpleFunctionExpression, List<ValueSpecification>> buildFunctionExpression(String functionName, List<org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification> parameters, MutableList<String> openVariables, SourceInformation sourceInformation, CompileContext compileContext, ProcessingContext processingContext)
    {
        FunctionExpressionBuilder builder = compileContext.resolveFunctionBuilder(functionName, this.registeredMetaPackages, this.map, sourceInformation, processingContext);
        return builder.buildFunctionExpression(parameters, openVariables, compileContext, processingContext);
    }

    private void registerMetaPackage(FunctionHandler... handlers)
    {
        for (FunctionHandler handler : handlers)
        {
            org.finos.legend.pure.m3.coreinstance.Package pkg = handler.getFunc() instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement ? ((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement)handler.getFunc())._package() : null;
            if (pkg != null)
            {
                String path = Root_meta_pure_functions_meta_elementToPath_PackageableElement_1__String_1_(pkg, pureModel.getExecutionSupport());
                if (path.startsWith(this.META_PACKAGE_NAME + this.PACKAGE_SEPARATOR))
                {
                    registeredMetaPackages.add(path);
                }
            }
        }
    }

    private void register(String name, boolean isNative, ReturnInference inference)
    {
        register(new FunctionHandler(this.pureModel, name, isNative, inference));
    }

    public void register(FunctionHandler handler)
    {
        FunctionHandler[] handlers = {handler};
        register(handlers);
    }

    public void register(UserDefinedFunctionHandler handler)
    {
        String functionName = handler.getFunctionName();
        boolean functionRegisteredByName = isFunctionRegisteredByName(handler);
        if (!functionRegisteredByName)
        {
            map.put(functionName, new MultiHandlerFunctionExpressionBuilder(this.pureModel, handler));
        }
        else
        {
            Assert.assertFalse(isFunctionRegisteredBySignature(handler, functionRegisteredByName), () -> "Function '" + handler.getFunctionSignature() + "' is already registered");
            FunctionExpressionBuilder builder = map.get(functionName);
            if (builder.supportFunctionHandler(handler))
            {
                builder.addFunctionHandler(handler);
            }
            else
            {
                this.addFunctionHandler(handler, builder);
            }
        }
        map.get(functionName).handlers().forEach(this::mayReplace);
    }

    private void register(FunctionHandler... handlers)
    {
        MultiHandlerFunctionExpressionBuilder handler = new MultiHandlerFunctionExpressionBuilder(this.pureModel, handlers);
        Assert.assertTrue(map.get(handler.getFunctionName()) == null, () -> "Function '" + handler.getFunctionName() + "' is already registered");
        Arrays.stream(handlers).forEach(this.pureModel::loadModelFromFunctionHandler);
        for (FunctionHandler h : handlers)
        {
            mayReplace(h);
        }
        handler.handlers().forEach(x -> registerMetaPackage(x));
        map.put(handler.getFunctionName(), handler);
    }

    private void register(FunctionExpressionBuilder handler)
    {
        Assert.assertTrue(map.get(handler.getFunctionName()) == null, () -> "Function '" + handler.getFunctionName() + "' is already registered");
        for (FunctionHandler h : handler.handlers())
        {
            mayReplace(h);
        }
        handler.handlers().forEach(x -> registerMetaPackage(x));
        map.put(handler.getFunctionName(), handler);
    }

    private void register(FunctionHandlerRegistrationInfo info)
    {
        if (info.coordinates == null || info.coordinates.isEmpty())
        {
            register(info.functionHandler);
        }
        else
        {
            FunctionExpressionBuilder functionExpressionBuilder = Objects.requireNonNull(this.map.get(info.functionHandler.getFunctionName()), "Can't find expression builder for function '" + info.functionHandler.getFunctionName() + "'");
            for (int i = 0; i < info.coordinates.size() - 1; ++i)
            {
                functionExpressionBuilder = functionExpressionBuilder.getFunctionExpressionBuilderAtIndex(info.coordinates.get(i));
            }
            functionExpressionBuilder.insertFunctionHandlerAtIndex(info.coordinates.get(info.coordinates.size() - 1), info.functionHandler);
        }
    }

    private void register(FunctionExpressionBuilderRegistrationInfo info)
    {
        if (info.coordinates == null || info.coordinates.isEmpty())
        {
            register(info.functionExpressionBuilder);
        }
        else
        {
            FunctionExpressionBuilder functionExpressionBuilder = Objects.requireNonNull(this.map.get(info.functionExpressionBuilder.getFunctionName()), "Can't find expression builder for function '" + info.functionExpressionBuilder.getFunctionName() + "'");
            for (int i = 0; i < info.coordinates.size() - 1; ++i)
            {
                functionExpressionBuilder = functionExpressionBuilder.getFunctionExpressionBuilderAtIndex(info.coordinates.get(i));
            }
            functionExpressionBuilder.insertFunctionExpressionBuilderAtIndex(info.coordinates.get(info.coordinates.size() - 1), info.functionExpressionBuilder);
        }
    }

    private void mayReplace(FunctionHandler handler)
    {
        Dispatch di = this.dispatchMap.get(handler.getFullName());
        if (di != null)
        {
            handler.setDispatch(di);
        }
    }

    private TypeAndMultiplicity res(GenericType genericType, Multiplicity mul)
    {
        return new TypeAndMultiplicity(genericType, mul);
    }

    public TypeAndMultiplicity res(GenericType genericType, String mul)
    {
        return new TypeAndMultiplicity(genericType, this.pureModel.getMultiplicity(mul));
    }

    public TypeAndMultiplicity res(String type, String mul)
    {
        return new TypeAndMultiplicity(this.pureModel.getGenericType(type), this.pureModel.getMultiplicity(mul));
    }

    private TypeAndMultiplicity res(String type, Multiplicity mul)
    {
        return new TypeAndMultiplicity(this.pureModel.getGenericType(type), mul);
    }

    private boolean isFunctionRegisteredByName(UserDefinedFunctionHandler handler)
    {
        return map.containsKey(handler.getFunctionName());
    }

    private boolean isFunctionRegisteredBySignature(UserDefinedFunctionHandler handler, Boolean isFunctionNameAlreadyRegistered)
    {
        return isFunctionNameAlreadyRegistered && map.get(handler.getFunctionName()).handlers().stream().anyMatch(val -> val.getFunctionSignature().equals(handler.getFunctionSignature()));
    }

    public void addFunctionHandler(FunctionHandler handler, FunctionExpressionBuilder builder)
    {
        if (builder instanceof MultiHandlerFunctionExpressionBuilder)
        {
            addFunctionHandler(handler, (MultiHandlerFunctionExpressionBuilder) builder);
        }
        else
        {
            addFunctionHandler(handler, (CompositeFunctionExpressionBuilder) builder);
        }
    }

    private void addFunctionHandler(FunctionHandler handler, MultiHandlerFunctionExpressionBuilder multiHandlerFunctionExpressionBuilder)
    {
        MultiHandlerFunctionExpressionBuilder multiHandler = new MultiHandlerFunctionExpressionBuilder(this.pureModel, handler);
        CompositeFunctionExpressionBuilder compositeFunctionExpressionBuilder = new CompositeFunctionExpressionBuilder(new MultiHandlerFunctionExpressionBuilder[] {multiHandlerFunctionExpressionBuilder, multiHandler});
        map.put(handler.getFunctionName(), compositeFunctionExpressionBuilder);
    }

    private void addFunctionHandler(FunctionHandler handler, CompositeFunctionExpressionBuilder compositeFunctionExpressionBuilder)
    {
        compositeFunctionExpressionBuilder.getBuilders().add(new MultiHandlerFunctionExpressionBuilder(this.pureModel, handler));

    }

    // --------------------------------------------- Function expression builder ----------------------------------

    public FunctionHandler h(String name, boolean isNative, ReturnInference returnInference)
    {
        return new FunctionHandler(this.pureModel, name, isNative, returnInference);
    }

    public FunctionHandler h(String name, boolean isNative, ReturnInference returnInference, Dispatch dispatch)
    {
        return new FunctionHandler(this.pureModel, name, isNative, returnInference, dispatch);
    }

    public RequiredInferenceSimilarSignatureFunctionExpressionBuilder grp(ParametersInference parametersInference, FunctionHandler... handlers)
    {
        Arrays.stream(handlers).forEach(this.pureModel::loadModelFromFunctionHandler);
        return new RequiredInferenceSimilarSignatureFunctionExpressionBuilder(parametersInference, handlers, this.pureModel);
    }

    public MultiHandlerFunctionExpressionBuilder m(FunctionHandler... handlers)
    {
        Arrays.stream(handlers).forEach(this.pureModel::loadModelFromFunctionHandler);
        return new MultiHandlerFunctionExpressionBuilder(this.pureModel, handlers);
    }

    public CompositeFunctionExpressionBuilder m(FunctionExpressionBuilder... builders)
    {
        return new CompositeFunctionExpressionBuilder(builders);
    }


    //-------------------------------
    // Functions below to be deleted
    //-------------------------------

    public static GenericType funcReturnType(ValueSpecification vs, PureModel pm)
    {
        return funcType(vs._genericType(), pm)._returnType();
    }

    private GenericType funcReturnType(ValueSpecification vs)
    {
        return funcType(vs._genericType(), this.pureModel)._returnType();
    }

    private Multiplicity funcReturnMul(ValueSpecification vs)
    {
        return funcType(vs._genericType())._returnMultiplicity();
    }

    public boolean typeOne(ValueSpecification vs, String type)
    {
        return typeOne(vs, Sets.immutable.with(type));
    }

    public boolean typeOne(ValueSpecification vs, ImmutableSet<String> type)
    {
        return isNilOrType(vs, type) && isOne(vs._multiplicity());
    }

    public boolean typeZeroOne(ValueSpecification vs, String type)
    {
        return typeZeroOne(vs, Sets.immutable.with(type));
    }

    public boolean typeZeroOne(ValueSpecification vs, ImmutableSet<String> type)
    {
        return isNilOrType(vs, type) && matchZeroOne(vs._multiplicity());
    }

    public boolean typeOneMany(ValueSpecification vs, String type)
    {
        return typeOneMany(vs, Sets.immutable.with(type));
    }

    public boolean typeOneMany(ValueSpecification vs, ImmutableSet<String> type)
    {
        return type.contains(vs._genericType()._rawType()._name()) && isMinimumOne(vs._multiplicity());
    }

    public boolean typeMany(ValueSpecification vs, String type)
    {
        return isNilOrType(vs, type);
    }

    public boolean typeMany(ValueSpecification vs, ImmutableSet<String> type)
    {
        return isNilOrType(vs, type);
    }

    private boolean isNilOrType(ValueSpecification vs, String type)
    {
        return isNilOrType(vs, Sets.immutable.with(type));
    }

    private boolean isNilOrType(ValueSpecification vs, ImmutableSet<String> type)
    {
        String vsType = vs._genericType()._rawType()._name();
        return Nil.equals(vsType) || type.contains(vsType);
    }

    private boolean isZeroOne(Multiplicity mul)
    {
        return mul._multiplicityParameter() == null && (mul._upperBound()._value() != null && mul._lowerBound()._value() == 0L && mul._upperBound()._value() == 1L);
    }

    // ---------------------------------------------------------------------


    //--------------------------
    // Required functions below
    //--------------------------

    private <T> boolean check(T val, Function<T, Boolean> func)
    {
        return func.apply(val);
    }

    private static FunctionType funcType(GenericType gt, PureModel pm)
    {
        if (gt._rawType()._name().equals("Path"))
        {
            RichIterable<? extends GenericType> g = gt._typeArguments();
            Multiplicity m = gt._multiplicityArguments().getFirst();
            return (FunctionType) PureModel.buildFunctionType(FastList.newListWith(new Root_meta_pure_metamodel_valuespecification_VariableExpression_Impl("", null, pm.getClass("meta::pure::metamodel::valuespecification::VariableExpression"))._genericType(g.getFirst())._multiplicity(pm.getMultiplicity("one"))), g.getLast(), m, pm)._rawType();
        }
        return (FunctionType) gt._typeArguments().getFirst()._rawType();
    }

    private FunctionType funcType(GenericType gt)
    {
        return funcType(gt, this.pureModel);
    }

    public boolean isOne(Multiplicity mul)
    {
        return mul._upperBound()._value() != null && mul._lowerBound()._value() == 1L && mul._upperBound()._value() == 1L;
    }

    private boolean matchZeroOne(Multiplicity mul)
    {
        // engine doesn't support Generics at model level ... We assume that a typeParameter is *. The use case is Result<T,m> used in Service tests
        return mul._multiplicityParameter() == null && mul._upperBound()._value() != null && (mul._upperBound()._value() == 0L || mul._upperBound()._value() == 1L);
    }

    private boolean matchOneMany(Multiplicity mul)
    {
        return isMinimumOne(mul);
    }

    private boolean isMinimumOne(Multiplicity mul)
    {
        return mul._lowerBound()._value() >= 1L;
    }

    private Map<String, Dispatch> buildDispatch()
    {
        CompileContext context = this.pureModel.getContext();

        // ------------------------------------------------------------------------------------------------
        // Please do not update the following code manually! Please check with the team when introducing
        // new matchers as this might be complicated by modularization
        // THIS CODE IS GENERATED BY A FUNCTION IN PURE - CONTACT THE CORE TEAM FOR MORE DETAILS
        //-------------------------------------------------------------------------------------------------

        Map<String, Dispatch> map = Maps.mutable.empty();
        map.put("meta::dsb::query::functions::filterReportDates_Any_1__Date_1__Date_1__Function_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 4 && isOne(ps.get(0)._multiplicity()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(1)._genericType()._rawType()._name()) && isOne(ps.get(2)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(2)._genericType()._rawType()._name()) && isOne(ps.get(3)._multiplicity()) && ("Nil".equals(ps.get(3)._genericType()._rawType()._name()) || check(funcType(ps.get(3)._genericType()), (FunctionType ft) -> matchZeroOne(ft._returnMultiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ft._returnType()._rawType()._name()) && check(ft._parameters().toList(), (List<? extends VariableExpression> nps) -> nps.size() == 1 && isOne(nps.get(0)._multiplicity())))));
        map.put("meta::dsb::query::functions::filterReportDates_Any_1__Date_1__Function_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(0)._multiplicity()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(1)._genericType()._rawType()._name()) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || check(funcType(ps.get(2)._genericType()), (FunctionType ft) -> matchZeroOne(ft._returnMultiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ft._returnType()._rawType()._name()) && check(ft._parameters().toList(), (List<? extends VariableExpression> nps) -> nps.size() == 1 && isOne(nps.get(0)._multiplicity())))));
        map.put("meta::pure::executionPlan::engine::java::convert_Any_1__Unit_1__Any_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Unit".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::executionPlan::engine::java::unitType_Any_1__String_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()));
        map.put("meta::pure::executionPlan::engine::java::unitValue_Any_1__Number_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()));
        map.put("meta::json::schema::discriminateOneOf_Any_1__Any_1__Type_MANY__DiscriminatorMapping_MANY__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 4 && isOne(ps.get(0)._multiplicity()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Type", "DataType", "FunctionType", "Class", "Measure", "Unit", "PrimitiveType", "Enumeration", "ClassProjection", "MappingClass").contains(ps.get(2)._genericType()._rawType()._name()) && ("Nil".equals(ps.get(3)._genericType()._rawType()._name()) || "DiscriminatorMapping".equals(ps.get(3)._genericType()._rawType()._name())));
        map.put("meta::json::schema::mapSchema_String_1__Type_1__DiscriminatorMapping_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Type", "DataType", "Measure", "FunctionType", "Class", "PrimitiveType", "Enumeration", "Unit", "ClassProjection", "MappingClass").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::json::toJSON_Any_MANY__String_1_", (List<ValueSpecification> ps) -> ps.size() == 1);
        map.put("meta::json::toJSON_T_MANY__LambdaFunction_MANY__String_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "LambdaFunction".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::asserts::assertContains_Any_MANY__Any_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(1)._multiplicity()));
        map.put("meta::pure::functions::asserts::assertContains_Any_MANY__Any_1__Function_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(1)._multiplicity()) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || check(funcType(ps.get(2)._genericType()), (FunctionType ft) -> isOne(ft._returnMultiplicity()) && ("Nil".equals(ft._returnType()._rawType()._name()) || "String".equals(ft._returnType()._rawType()._name())) && check(ft._parameters().toList(), (List<? extends VariableExpression> nps) -> nps.size() == 0))));
        map.put("meta::pure::functions::asserts::assertContains_Any_MANY__Any_1__String_1__Any_MANY__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 4 && isOne(ps.get(1)._multiplicity()) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "String".equals(ps.get(2)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::asserts::assertContains_Any_MANY__Any_1__String_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(1)._multiplicity()) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "String".equals(ps.get(2)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::asserts::assertEmpty_Any_MANY__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 1);
        map.put("meta::pure::functions::asserts::assertEmpty_Any_MANY__Function_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || check(funcType(ps.get(1)._genericType()), (FunctionType ft) -> isOne(ft._returnMultiplicity()) && ("Nil".equals(ft._returnType()._rawType()._name()) || "String".equals(ft._returnType()._rawType()._name())) && check(ft._parameters().toList(), (List<? extends VariableExpression> nps) -> nps.size() == 0))));
        map.put("meta::pure::functions::asserts::assertEmpty_Any_MANY__String_1__Any_MANY__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::asserts::assertEmpty_Any_MANY__String_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::asserts::assertEqWithinTolerance_Number_1__Number_1__Number_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(1)._genericType()._rawType()._name()) && isOne(ps.get(2)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(2)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::asserts::assertEqWithinTolerance_Number_1__Number_1__Number_1__Function_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 4 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(1)._genericType()._rawType()._name()) && isOne(ps.get(2)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(2)._genericType()._rawType()._name()) && isOne(ps.get(3)._multiplicity()) && ("Nil".equals(ps.get(3)._genericType()._rawType()._name()) || check(funcType(ps.get(3)._genericType()), (FunctionType ft) -> isOne(ft._returnMultiplicity()) && ("Nil".equals(ft._returnType()._rawType()._name()) || "String".equals(ft._returnType()._rawType()._name())) && check(ft._parameters().toList(), (List<? extends VariableExpression> nps) -> nps.size() == 0))));
        map.put("meta::pure::functions::asserts::assertEqWithinTolerance_Number_1__Number_1__Number_1__String_1__Any_MANY__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 5 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(1)._genericType()._rawType()._name()) && isOne(ps.get(2)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(2)._genericType()._rawType()._name()) && isOne(ps.get(3)._multiplicity()) && ("Nil".equals(ps.get(3)._genericType()._rawType()._name()) || "String".equals(ps.get(3)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::asserts::assertEqWithinTolerance_Number_1__Number_1__Number_1__String_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 4 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(1)._genericType()._rawType()._name()) && isOne(ps.get(2)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(2)._genericType()._rawType()._name()) && isOne(ps.get(3)._multiplicity()) && ("Nil".equals(ps.get(3)._genericType()._rawType()._name()) || "String".equals(ps.get(3)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::asserts::assertEq_Any_1__Any_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && isOne(ps.get(1)._multiplicity()));
        map.put("meta::pure::functions::asserts::assertEq_Any_1__Any_1__Function_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(0)._multiplicity()) && isOne(ps.get(1)._multiplicity()) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || check(funcType(ps.get(2)._genericType()), (FunctionType ft) -> isOne(ft._returnMultiplicity()) && ("Nil".equals(ft._returnType()._rawType()._name()) || "String".equals(ft._returnType()._rawType()._name())) && check(ft._parameters().toList(), (List<? extends VariableExpression> nps) -> nps.size() == 0))));
        map.put("meta::pure::functions::asserts::assertEq_Any_1__Any_1__String_1__Any_MANY__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 4 && isOne(ps.get(0)._multiplicity()) && isOne(ps.get(1)._multiplicity()) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "String".equals(ps.get(2)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::asserts::assertEq_Any_1__Any_1__String_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(0)._multiplicity()) && isOne(ps.get(1)._multiplicity()) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "String".equals(ps.get(2)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::asserts::assertEquals_Any_MANY__Any_MANY__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2);
        map.put("meta::pure::functions::asserts::assertEquals_Any_MANY__Any_MANY__Function_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || check(funcType(ps.get(2)._genericType()), (FunctionType ft) -> isOne(ft._returnMultiplicity()) && ("Nil".equals(ft._returnType()._rawType()._name()) || "String".equals(ft._returnType()._rawType()._name())) && check(ft._parameters().toList(), (List<? extends VariableExpression> nps) -> nps.size() == 0))));
        map.put("meta::pure::functions::asserts::assertEquals_Any_MANY__Any_MANY__String_1__Any_MANY__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 4 && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "String".equals(ps.get(2)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::asserts::assertEquals_Any_MANY__Any_MANY__String_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "String".equals(ps.get(2)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::asserts::assertFalse_Boolean_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Boolean".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::asserts::assertFalse_Boolean_1__Function_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Boolean".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || check(funcType(ps.get(1)._genericType()), (FunctionType ft) -> isOne(ft._returnMultiplicity()) && ("Nil".equals(ft._returnType()._rawType()._name()) || "String".equals(ft._returnType()._rawType()._name())) && check(ft._parameters().toList(), (List<? extends VariableExpression> nps) -> nps.size() == 0))));
        map.put("meta::pure::functions::asserts::assertFalse_Boolean_1__String_1__Any_MANY__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Boolean".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::asserts::assertFalse_Boolean_1__String_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Boolean".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::asserts::assertInstanceOf_Any_1__Type_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Type", "Class", "DataType", "Measure", "FunctionType", "MappingClass", "ClassProjection", "PrimitiveType", "Unit", "Enumeration").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::asserts::assertInstanceOf_Any_1__Type_1__Function_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(0)._multiplicity()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Type", "Class", "DataType", "Measure", "FunctionType", "MappingClass", "ClassProjection", "PrimitiveType", "Unit", "Enumeration").contains(ps.get(1)._genericType()._rawType()._name()) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || check(funcType(ps.get(2)._genericType()), (FunctionType ft) -> isOne(ft._returnMultiplicity()) && ("Nil".equals(ft._returnType()._rawType()._name()) || "String".equals(ft._returnType()._rawType()._name())) && check(ft._parameters().toList(), (List<? extends VariableExpression> nps) -> nps.size() == 0))));
        map.put("meta::pure::functions::asserts::assertInstanceOf_Any_1__Type_1__String_1__Any_MANY__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 4 && isOne(ps.get(0)._multiplicity()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Type", "Class", "DataType", "Measure", "FunctionType", "MappingClass", "ClassProjection", "PrimitiveType", "Unit", "Enumeration").contains(ps.get(1)._genericType()._rawType()._name()) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "String".equals(ps.get(2)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::asserts::assertInstanceOf_Any_1__Type_1__String_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(0)._multiplicity()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Type", "Class", "DataType", "Measure", "FunctionType", "MappingClass", "ClassProjection", "PrimitiveType", "Unit", "Enumeration").contains(ps.get(1)._genericType()._rawType()._name()) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "String".equals(ps.get(2)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::asserts::assertIsNot_Any_1__Any_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && isOne(ps.get(1)._multiplicity()));
        map.put("meta::pure::functions::asserts::assertIsNot_Any_1__Any_1__Function_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(0)._multiplicity()) && isOne(ps.get(1)._multiplicity()) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || check(funcType(ps.get(2)._genericType()), (FunctionType ft) -> isOne(ft._returnMultiplicity()) && ("Nil".equals(ft._returnType()._rawType()._name()) || "String".equals(ft._returnType()._rawType()._name())) && check(ft._parameters().toList(), (List<? extends VariableExpression> nps) -> nps.size() == 0))));
        map.put("meta::pure::functions::asserts::assertIsNot_Any_1__Any_1__String_1__Any_MANY__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 4 && isOne(ps.get(0)._multiplicity()) && isOne(ps.get(1)._multiplicity()) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "String".equals(ps.get(2)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::asserts::assertIsNot_Any_1__Any_1__String_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(0)._multiplicity()) && isOne(ps.get(1)._multiplicity()) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "String".equals(ps.get(2)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::asserts::assertIs_Any_1__Any_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && isOne(ps.get(1)._multiplicity()));
        map.put("meta::pure::functions::asserts::assertIs_Any_1__Any_1__Function_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(0)._multiplicity()) && isOne(ps.get(1)._multiplicity()) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || check(funcType(ps.get(2)._genericType()), (FunctionType ft) -> isOne(ft._returnMultiplicity()) && ("Nil".equals(ft._returnType()._rawType()._name()) || "String".equals(ft._returnType()._rawType()._name())) && check(ft._parameters().toList(), (List<? extends VariableExpression> nps) -> nps.size() == 0))));
        map.put("meta::pure::functions::asserts::assertIs_Any_1__Any_1__String_1__Any_MANY__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 4 && isOne(ps.get(0)._multiplicity()) && isOne(ps.get(1)._multiplicity()) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "String".equals(ps.get(2)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::asserts::assertIs_Any_1__Any_1__String_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(0)._multiplicity()) && isOne(ps.get(1)._multiplicity()) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "String".equals(ps.get(2)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::asserts::assertJsonStringsEqual_String_1__String_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::asserts::assertNotContains_Any_MANY__Any_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(1)._multiplicity()));
        map.put("meta::pure::functions::asserts::assertNotContains_Any_MANY__Any_1__Function_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(1)._multiplicity()) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || check(funcType(ps.get(2)._genericType()), (FunctionType ft) -> isOne(ft._returnMultiplicity()) && ("Nil".equals(ft._returnType()._rawType()._name()) || "String".equals(ft._returnType()._rawType()._name())) && check(ft._parameters().toList(), (List<? extends VariableExpression> nps) -> nps.size() == 0))));
        map.put("meta::pure::functions::asserts::assertNotContains_Any_MANY__Any_1__String_1__Any_MANY__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 4 && isOne(ps.get(1)._multiplicity()) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "String".equals(ps.get(2)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::asserts::assertNotContains_Any_MANY__Any_1__String_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(1)._multiplicity()) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "String".equals(ps.get(2)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::asserts::assertNotEmpty_Any_MANY__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 1);
        map.put("meta::pure::functions::asserts::assertNotEmpty_Any_MANY__Function_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || check(funcType(ps.get(1)._genericType()), (FunctionType ft) -> isOne(ft._returnMultiplicity()) && ("Nil".equals(ft._returnType()._rawType()._name()) || "String".equals(ft._returnType()._rawType()._name())) && check(ft._parameters().toList(), (List<? extends VariableExpression> nps) -> nps.size() == 0))));
        map.put("meta::pure::functions::asserts::assertNotEmpty_Any_MANY__String_1__Any_MANY__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::asserts::assertNotEmpty_Any_MANY__String_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::asserts::assertNotEq_Any_1__Any_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && isOne(ps.get(1)._multiplicity()));
        map.put("meta::pure::functions::asserts::assertNotEq_Any_1__Any_1__Function_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(0)._multiplicity()) && isOne(ps.get(1)._multiplicity()) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || check(funcType(ps.get(2)._genericType()), (FunctionType ft) -> isOne(ft._returnMultiplicity()) && ("Nil".equals(ft._returnType()._rawType()._name()) || "String".equals(ft._returnType()._rawType()._name())) && check(ft._parameters().toList(), (List<? extends VariableExpression> nps) -> nps.size() == 0))));
        map.put("meta::pure::functions::asserts::assertNotEq_Any_1__Any_1__String_1__Any_MANY__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 4 && isOne(ps.get(0)._multiplicity()) && isOne(ps.get(1)._multiplicity()) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "String".equals(ps.get(2)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::asserts::assertNotEq_Any_1__Any_1__String_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(0)._multiplicity()) && isOne(ps.get(1)._multiplicity()) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "String".equals(ps.get(2)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::asserts::assertNotEquals_Any_MANY__Any_MANY__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2);
        map.put("meta::pure::functions::asserts::assertNotEquals_Any_MANY__Any_MANY__Function_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || check(funcType(ps.get(2)._genericType()), (FunctionType ft) -> isOne(ft._returnMultiplicity()) && ("Nil".equals(ft._returnType()._rawType()._name()) || "String".equals(ft._returnType()._rawType()._name())) && check(ft._parameters().toList(), (List<? extends VariableExpression> nps) -> nps.size() == 0))));
        map.put("meta::pure::functions::asserts::assertNotEquals_Any_MANY__Any_MANY__String_1__Any_MANY__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 4 && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "String".equals(ps.get(2)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::asserts::assertNotEquals_Any_MANY__Any_MANY__String_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "String".equals(ps.get(2)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::asserts::assertNotSize_Any_MANY__Integer_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Integer".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::asserts::assertNotSize_Any_MANY__Integer_1__Function_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Integer".equals(ps.get(1)._genericType()._rawType()._name())) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || check(funcType(ps.get(2)._genericType()), (FunctionType ft) -> isOne(ft._returnMultiplicity()) && ("Nil".equals(ft._returnType()._rawType()._name()) || "String".equals(ft._returnType()._rawType()._name())) && check(ft._parameters().toList(), (List<? extends VariableExpression> nps) -> nps.size() == 0))));
        map.put("meta::pure::functions::asserts::assertNotSize_Any_MANY__Integer_1__String_1__Any_MANY__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 4 && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Integer".equals(ps.get(1)._genericType()._rawType()._name())) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "String".equals(ps.get(2)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::asserts::assertNotSize_Any_MANY__Integer_1__String_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Integer".equals(ps.get(1)._genericType()._rawType()._name())) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "String".equals(ps.get(2)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::asserts::assertSameElements_Any_MANY__Any_MANY__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2);
        map.put("meta::pure::functions::asserts::assertSameElements_Any_MANY__Any_MANY__Function_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || check(funcType(ps.get(2)._genericType()), (FunctionType ft) -> isOne(ft._returnMultiplicity()) && ("Nil".equals(ft._returnType()._rawType()._name()) || "String".equals(ft._returnType()._rawType()._name())) && check(ft._parameters().toList(), (List<? extends VariableExpression> nps) -> nps.size() == 0))));
        map.put("meta::pure::functions::asserts::assertSameElements_Any_MANY__Any_MANY__String_1__Any_MANY__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 4 && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "String".equals(ps.get(2)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::asserts::assertSameElements_Any_MANY__Any_MANY__String_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "String".equals(ps.get(2)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::asserts::assertSize_Any_MANY__Integer_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Integer".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::asserts::assertSize_Any_MANY__Integer_1__Function_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Integer".equals(ps.get(1)._genericType()._rawType()._name())) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || check(funcType(ps.get(2)._genericType()), (FunctionType ft) -> isOne(ft._returnMultiplicity()) && ("Nil".equals(ft._returnType()._rawType()._name()) || "String".equals(ft._returnType()._rawType()._name())) && check(ft._parameters().toList(), (List<? extends VariableExpression> nps) -> nps.size() == 0))));
        map.put("meta::pure::functions::asserts::assertSize_Any_MANY__Integer_1__String_1__Any_MANY__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 4 && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Integer".equals(ps.get(1)._genericType()._rawType()._name())) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "String".equals(ps.get(2)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::asserts::assertSize_Any_MANY__Integer_1__String_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Integer".equals(ps.get(1)._genericType()._rawType()._name())) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "String".equals(ps.get(2)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::asserts::assert_Boolean_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Boolean".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::asserts::assert_Boolean_1__Function_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Boolean".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || check(funcType(ps.get(1)._genericType()), (FunctionType ft) -> isOne(ft._returnMultiplicity()) && ("Nil".equals(ft._returnType()._rawType()._name()) || "String".equals(ft._returnType()._rawType()._name())) && check(ft._parameters().toList(), (List<? extends VariableExpression> nps) -> nps.size() == 0))));
        map.put("meta::pure::functions::asserts::assert_Boolean_1__String_1__Any_MANY__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Boolean".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::asserts::assert_Boolean_1__String_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Boolean".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::asserts::fail_Function_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || check(funcType(ps.get(0)._genericType()), (FunctionType ft) -> isOne(ft._returnMultiplicity()) && ("Nil".equals(ft._returnType()._rawType()._name()) || "String".equals(ft._returnType()._rawType()._name())) && check(ft._parameters().toList(), (List<? extends VariableExpression> nps) -> nps.size() == 0))));
        map.put("meta::pure::functions::asserts::fail_String_1__Any_MANY__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::asserts::fail_String_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::asserts::fail__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 0);
        map.put("meta::pure::functions::boolean::and_Boolean_1__Boolean_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Boolean".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Boolean".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::boolean::eq_Any_1__Any_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && isOne(ps.get(1)._multiplicity()));
        map.put("meta::pure::functions::boolean::equalJsonStrings_String_1__String_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::boolean::equal_Any_MANY__Any_MANY__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2);
        map.put("meta::pure::functions::boolean::greaterThanEqual_Boolean_$0_1$__Boolean_$0_1$__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && matchZeroOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Boolean".equals(ps.get(0)._genericType()._rawType()._name())) && matchZeroOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Boolean".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::boolean::greaterThanEqual_Boolean_$0_1$__Boolean_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && matchZeroOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Boolean".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Boolean".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::boolean::greaterThanEqual_Boolean_1__Boolean_$0_1$__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Boolean".equals(ps.get(0)._genericType()._rawType()._name())) && matchZeroOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Boolean".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::boolean::greaterThanEqual_Boolean_1__Boolean_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Boolean".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Boolean".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::boolean::greaterThanEqual_Date_$0_1$__Date_$0_1$__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && matchZeroOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()) && matchZeroOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::boolean::greaterThanEqual_Date_$0_1$__Date_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && matchZeroOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::boolean::greaterThanEqual_Date_1__Date_$0_1$__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()) && matchZeroOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::boolean::greaterThanEqual_Date_1__Date_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::boolean::greaterThanEqual_Number_$0_1$__Number_$0_1$__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && matchZeroOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()) && matchZeroOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::boolean::greaterThanEqual_Number_$0_1$__Number_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && matchZeroOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::boolean::greaterThanEqual_Number_1__Number_$0_1$__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()) && matchZeroOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::boolean::greaterThanEqual_Number_1__Number_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::boolean::greaterThanEqual_String_$0_1$__String_$0_1$__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && matchZeroOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())) && matchZeroOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::boolean::greaterThanEqual_String_$0_1$__String_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && matchZeroOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::boolean::greaterThanEqual_String_1__String_$0_1$__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())) && matchZeroOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::boolean::greaterThanEqual_String_1__String_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::boolean::greaterThan_Boolean_$0_1$__Boolean_$0_1$__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && matchZeroOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Boolean".equals(ps.get(0)._genericType()._rawType()._name())) && matchZeroOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Boolean".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::boolean::greaterThan_Boolean_$0_1$__Boolean_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && matchZeroOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Boolean".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Boolean".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::boolean::greaterThan_Boolean_1__Boolean_$0_1$__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Boolean".equals(ps.get(0)._genericType()._rawType()._name())) && matchZeroOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Boolean".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::boolean::greaterThan_Boolean_1__Boolean_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Boolean".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Boolean".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::boolean::greaterThan_Date_$0_1$__Date_$0_1$__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && matchZeroOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()) && matchZeroOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::boolean::greaterThan_Date_$0_1$__Date_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && matchZeroOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::boolean::greaterThan_Date_1__Date_$0_1$__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()) && matchZeroOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::boolean::greaterThan_Date_1__Date_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::boolean::greaterThan_Number_$0_1$__Number_$0_1$__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && matchZeroOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()) && matchZeroOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::boolean::greaterThan_Number_$0_1$__Number_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && matchZeroOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::boolean::greaterThan_Number_1__Number_$0_1$__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()) && matchZeroOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::boolean::greaterThan_Number_1__Number_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::boolean::greaterThan_String_$0_1$__String_$0_1$__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && matchZeroOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())) && matchZeroOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::boolean::greaterThan_String_$0_1$__String_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && matchZeroOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::boolean::greaterThan_String_1__String_$0_1$__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())) && matchZeroOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::boolean::greaterThan_String_1__String_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::boolean::isFalse_Boolean_$0_1$__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && matchZeroOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Boolean".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::boolean::isTrue_Boolean_$0_1$__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && matchZeroOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Boolean".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::boolean::is_Any_1__Any_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && isOne(ps.get(1)._multiplicity()));
        map.put("meta::pure::functions::boolean::lessThanEqual_Boolean_$0_1$__Boolean_$0_1$__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && matchZeroOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Boolean".equals(ps.get(0)._genericType()._rawType()._name())) && matchZeroOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Boolean".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::boolean::lessThanEqual_Boolean_$0_1$__Boolean_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && matchZeroOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Boolean".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Boolean".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::boolean::lessThanEqual_Boolean_1__Boolean_$0_1$__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Boolean".equals(ps.get(0)._genericType()._rawType()._name())) && matchZeroOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Boolean".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::boolean::lessThanEqual_Boolean_1__Boolean_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Boolean".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Boolean".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::boolean::lessThanEqual_Date_$0_1$__Date_$0_1$__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && matchZeroOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()) && matchZeroOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::boolean::lessThanEqual_Date_$0_1$__Date_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && matchZeroOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::boolean::lessThanEqual_Date_1__Date_$0_1$__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()) && matchZeroOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::boolean::lessThanEqual_Date_1__Date_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::boolean::lessThanEqual_Number_$0_1$__Number_$0_1$__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && matchZeroOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()) && matchZeroOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::boolean::lessThanEqual_Number_$0_1$__Number_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && matchZeroOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::boolean::lessThanEqual_Number_1__Number_$0_1$__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()) && matchZeroOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::boolean::lessThanEqual_Number_1__Number_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::boolean::lessThanEqual_String_$0_1$__String_$0_1$__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && matchZeroOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())) && matchZeroOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::boolean::lessThanEqual_String_$0_1$__String_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && matchZeroOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::boolean::lessThanEqual_String_1__String_$0_1$__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())) && matchZeroOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::boolean::lessThanEqual_String_1__String_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::boolean::lessThan_Boolean_$0_1$__Boolean_$0_1$__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && matchZeroOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Boolean".equals(ps.get(0)._genericType()._rawType()._name())) && matchZeroOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Boolean".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::boolean::lessThan_Boolean_$0_1$__Boolean_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && matchZeroOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Boolean".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Boolean".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::boolean::lessThan_Boolean_1__Boolean_$0_1$__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Boolean".equals(ps.get(0)._genericType()._rawType()._name())) && matchZeroOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Boolean".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::boolean::lessThan_Boolean_1__Boolean_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Boolean".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Boolean".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::boolean::lessThan_Date_$0_1$__Date_$0_1$__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && matchZeroOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()) && matchZeroOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::boolean::lessThan_Date_$0_1$__Date_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && matchZeroOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::boolean::lessThan_Date_1__Date_$0_1$__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()) && matchZeroOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::boolean::lessThan_Date_1__Date_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::boolean::lessThan_Number_$0_1$__Number_$0_1$__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && matchZeroOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()) && matchZeroOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::boolean::lessThan_Number_$0_1$__Number_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && matchZeroOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::boolean::lessThan_Number_1__Number_$0_1$__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()) && matchZeroOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::boolean::lessThan_Number_1__Number_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::boolean::lessThan_String_$0_1$__String_$0_1$__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && matchZeroOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())) && matchZeroOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::boolean::lessThan_String_$0_1$__String_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && matchZeroOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::boolean::lessThan_String_1__String_$0_1$__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())) && matchZeroOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::boolean::lessThan_String_1__String_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::boolean::not_Boolean_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Boolean".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::boolean::or_Boolean_1__Boolean_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Boolean".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Boolean".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::collection::add_T_MANY__T_1__T_$1_MANY$_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(1)._multiplicity()));
        map.put("meta::pure::functions::collection::agg_FunctionDefinition_1__FunctionDefinition_1__AggregateValue_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "FunctionDefinition", "QualifiedProperty", "ConcreteFunctionDefinition", "LambdaFunction", "NewPropertyRouteNodeFunctionDefinition").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "FunctionDefinition", "QualifiedProperty", "ConcreteFunctionDefinition", "LambdaFunction", "NewPropertyRouteNodeFunctionDefinition").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::collection::and_Boolean_MANY__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Boolean".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::collection::at_T_MANY__Integer_1__T_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Integer".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::collection::concatenate_T_MANY__T_MANY__T_MANY_", (List<ValueSpecification> ps) -> ps.size() == 2);
        map.put("meta::pure::functions::collection::containsAll_Any_MANY__Any_MANY__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2);
        map.put("meta::pure::functions::collection::containsAny_Any_MANY__Any_MANY__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2);
        map.put("meta::pure::functions::collection::contains_Any_MANY__Any_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(1)._multiplicity()));
        map.put("meta::pure::functions::collection::count_Any_MANY__Integer_1_", (List<ValueSpecification> ps) -> ps.size() == 1);
        map.put("meta::pure::functions::collection::defaultIfEmpty_T_MANY__T_$1_MANY$__T_$1_MANY$_", (List<ValueSpecification> ps) -> ps.size() == 2 && matchOneMany(ps.get(1)._multiplicity()));
        map.put("meta::pure::functions::collection::distinct_T_MANY__T_MANY_", (List<ValueSpecification> ps) -> ps.size() == 1);
        map.put("meta::pure::functions::collection::dropAt_T_MANY__Integer_1__Integer_1__T_MANY_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Integer".equals(ps.get(1)._genericType()._rawType()._name())) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "Integer".equals(ps.get(2)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::collection::dropAt_T_MANY__Integer_1__T_MANY_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Integer".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::collection::drop_T_MANY__Integer_1__T_MANY_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Integer".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::collection::exists_T_MANY__Function_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || check(funcType(ps.get(1)._genericType()), (FunctionType ft) -> isOne(ft._returnMultiplicity()) && ("Nil".equals(ft._returnType()._rawType()._name()) || "Boolean".equals(ft._returnType()._rawType()._name())) && check(ft._parameters().toList(), (List<? extends VariableExpression> nps) -> nps.size() == 1 && isOne(nps.get(0)._multiplicity())))));
        map.put("meta::pure::functions::collection::filter_T_MANY__Function_1__T_MANY_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || check(funcType(ps.get(1)._genericType()), (FunctionType ft) -> isOne(ft._returnMultiplicity()) && ("Nil".equals(ft._returnType()._rawType()._name()) || "Boolean".equals(ft._returnType()._rawType()._name())) && check(ft._parameters().toList(), (List<? extends VariableExpression> nps) -> nps.size() == 1 && isOne(nps.get(0)._multiplicity())))));
        map.put("meta::pure::functions::collection::first_T_MANY__T_$0_1$_", (List<ValueSpecification> ps) -> ps.size() == 1);
        map.put("meta::pure::functions::collection::fold_T_MANY__Function_1__V_m__V_m_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || check(funcType(ps.get(1)._genericType()), (FunctionType ft) -> check(ft._parameters().toList(), (List<? extends VariableExpression> nps) -> nps.size() == 2 && isOne(nps.get(0)._multiplicity())))));
        map.put("meta::pure::functions::collection::forAll_T_MANY__Function_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || check(funcType(ps.get(1)._genericType()), (FunctionType ft) -> isOne(ft._returnMultiplicity()) && ("Nil".equals(ft._returnType()._rawType()._name()) || "Boolean".equals(ft._returnType()._rawType()._name())) && check(ft._parameters().toList(), (List<? extends VariableExpression> nps) -> nps.size() == 1 && isOne(nps.get(0)._multiplicity())))));
        map.put("meta::pure::functions::collection::getAllForEachDate_Class_1__Date_MANY__T_MANY_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Class", "MappingClass", "ClassProjection").contains(ps.get(0)._genericType()._rawType()._name()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::collection::getAllVersionsInRange_Class_1__Date_1__Date_1__T_MANY_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Class", "MappingClass", "ClassProjection").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(1)._genericType()._rawType()._name()) && isOne(ps.get(2)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(2)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::collection::getAllVersions_Class_1__T_MANY_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Class", "MappingClass", "ClassProjection").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::collection::getAll_Class_1__Date_1__Date_1__T_MANY_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Class", "MappingClass", "ClassProjection").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(1)._genericType()._rawType()._name()) && isOne(ps.get(2)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(2)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::collection::getAll_Class_1__Date_1__T_MANY_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Class", "MappingClass", "ClassProjection").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::collection::getAll_Class_1__T_MANY_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Class", "MappingClass", "ClassProjection").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::collection::head_T_MANY__T_$0_1$_", (List<ValueSpecification> ps) -> ps.size() == 1);
        map.put("meta::pure::functions::collection::in_Any_$0_1$__Any_MANY__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && matchZeroOne(ps.get(0)._multiplicity()));
        map.put("meta::pure::functions::collection::in_Any_1__Any_MANY__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()));
        map.put("meta::pure::functions::collection::indexOf_T_MANY__T_1__Integer_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(1)._multiplicity()));
        map.put("meta::pure::functions::collection::isDistinct_T_MANY__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 1);
        map.put("meta::pure::functions::collection::isDistinct_T_MANY__RootGraphFetchTree_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "RootGraphFetchTree", "ExtendedRootGraphFetchTree", "RoutedRootGraphFetchTree", "SerializeTopRootGraphFetchTree").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::collection::isEmpty_Any_$0_1$__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && matchZeroOne(ps.get(0)._multiplicity()));
        map.put("meta::pure::functions::collection::isEmpty_Any_MANY__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 1);
        map.put("meta::pure::functions::collection::isEqual_T_1__T_1__RootGraphFetchTree_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(0)._multiplicity()) && isOne(ps.get(1)._multiplicity()) && isOne(ps.get(2)._multiplicity()) && Sets.immutable.with("Nil", "RootGraphFetchTree", "ExtendedRootGraphFetchTree", "RoutedRootGraphFetchTree", "SerializeTopRootGraphFetchTree").contains(ps.get(2)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::collection::isNotEmpty_Any_$0_1$__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && matchZeroOne(ps.get(0)._multiplicity()));
        map.put("meta::pure::functions::collection::isNotEmpty_Any_MANY__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 1);
        map.put("meta::pure::functions::collection::last_T_MANY__T_$0_1$_", (List<ValueSpecification> ps) -> ps.size() == 1);
        map.put("meta::pure::functions::collection::limit_T_MANY__Integer_1__T_MANY_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Integer".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::collection::map_T_$0_1$__Function_1__V_$0_1$_", (List<ValueSpecification> ps) -> ps.size() == 2 && matchZeroOne(ps.get(0)._multiplicity()) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || check(funcType(ps.get(1)._genericType()), (FunctionType ft) -> matchZeroOne(ft._returnMultiplicity()) && check(ft._parameters().toList(), (List<? extends VariableExpression> nps) -> nps.size() == 1 && isOne(nps.get(0)._multiplicity())))));
        map.put("meta::pure::functions::collection::map_T_MANY__Function_1__V_MANY_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || check(funcType(ps.get(1)._genericType()), (FunctionType ft) -> check(ft._parameters().toList(), (List<? extends VariableExpression> nps) -> nps.size() == 1 && isOne(nps.get(0)._multiplicity())))));
        map.put("meta::pure::functions::collection::map_T_m__Function_1__V_m_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || check(funcType(ps.get(1)._genericType()), (FunctionType ft) -> isOne(ft._returnMultiplicity()) && check(ft._parameters().toList(), (List<? extends VariableExpression> nps) -> nps.size() == 1 && isOne(nps.get(0)._multiplicity())))));
        map.put("meta::pure::functions::collection::objectReferenceIn_Any_1__String_MANY__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::collection::oneOf_Boolean_MANY__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Boolean".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::collection::or_Boolean_$1_MANY$__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && matchOneMany(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Boolean".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::collection::range_Integer_1__Integer_1__Integer_1__Integer_MANY_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Integer".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Integer".equals(ps.get(1)._genericType()._rawType()._name())) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "Integer".equals(ps.get(2)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::collection::range_Integer_1__Integer_1__Integer_MANY_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Integer".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Integer".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::collection::removeDuplicatesBy_T_MANY__Function_1__T_MANY_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || check(funcType(ps.get(1)._genericType()), (FunctionType ft) -> isOne(ft._returnMultiplicity()) && check(ft._parameters().toList(), (List<? extends VariableExpression> nps) -> nps.size() == 1 && isOne(nps.get(0)._multiplicity())))));
        map.put("meta::pure::functions::collection::removeDuplicates_T_MANY__Function_$0_1$__Function_$0_1$__T_MANY_", (List<ValueSpecification> ps) -> ps.size() == 3 && matchZeroOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || check(funcType(ps.get(1)._genericType()), (FunctionType ft) -> isOne(ft._returnMultiplicity()) && check(ft._parameters().toList(), (List<? extends VariableExpression> nps) -> nps.size() == 1 && isOne(nps.get(0)._multiplicity())))) && matchZeroOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || check(funcType(ps.get(2)._genericType()), (FunctionType ft) -> isOne(ft._returnMultiplicity()) && ("Nil".equals(ft._returnType()._rawType()._name()) || "Boolean".equals(ft._returnType()._rawType()._name())) && check(ft._parameters().toList(), (List<? extends VariableExpression> nps) -> nps.size() == 2 && isOne(nps.get(0)._multiplicity()) && isOne(nps.get(1)._multiplicity())))));
        map.put("meta::pure::functions::collection::removeDuplicates_T_MANY__Function_1__T_MANY_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || check(funcType(ps.get(1)._genericType()), (FunctionType ft) -> isOne(ft._returnMultiplicity()) && ("Nil".equals(ft._returnType()._rawType()._name()) || "Boolean".equals(ft._returnType()._rawType()._name())) && check(ft._parameters().toList(), (List<? extends VariableExpression> nps) -> nps.size() == 2 && isOne(nps.get(0)._multiplicity()) && isOne(nps.get(1)._multiplicity())))));
        map.put("meta::pure::functions::collection::removeDuplicates_T_MANY__T_MANY_", (List<ValueSpecification> ps) -> ps.size() == 1);
        map.put("meta::pure::functions::collection::reverse_T_m__T_m_", (List<ValueSpecification> ps) -> ps.size() == 1);
        map.put("meta::pure::functions::collection::size_Any_MANY__Integer_1_", (List<ValueSpecification> ps) -> ps.size() == 1);
        map.put("meta::pure::functions::collection::slice_T_MANY__Integer_1__Integer_1__T_MANY_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Integer".equals(ps.get(1)._genericType()._rawType()._name())) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "Integer".equals(ps.get(2)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::collection::sortBy_T_m__Function_$0_1$__T_m_", (List<ValueSpecification> ps) -> ps.size() == 2 && matchZeroOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || check(funcType(ps.get(1)._genericType()), (FunctionType ft) -> isOne(ft._returnMultiplicity()) && check(ft._parameters().toList(), (List<? extends VariableExpression> nps) -> nps.size() == 1 && isOne(nps.get(0)._multiplicity())))));
        map.put("meta::pure::functions::collection::sort_T_m__Function_$0_1$__Function_$0_1$__T_m_", (List<ValueSpecification> ps) -> ps.size() == 3 && matchZeroOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || check(funcType(ps.get(1)._genericType()), (FunctionType ft) -> isOne(ft._returnMultiplicity()) && check(ft._parameters().toList(), (List<? extends VariableExpression> nps) -> nps.size() == 1 && isOne(nps.get(0)._multiplicity())))) && matchZeroOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || check(funcType(ps.get(2)._genericType()), (FunctionType ft) -> isOne(ft._returnMultiplicity()) && ("Nil".equals(ft._returnType()._rawType()._name()) || "Integer".equals(ft._returnType()._rawType()._name())) && check(ft._parameters().toList(), (List<? extends VariableExpression> nps) -> nps.size() == 2 && isOne(nps.get(0)._multiplicity()) && isOne(nps.get(1)._multiplicity())))));
        map.put("meta::pure::functions::collection::sort_T_m__Function_$0_1$__T_m_", (List<ValueSpecification> ps) -> ps.size() == 2 && matchZeroOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || check(funcType(ps.get(1)._genericType()), (FunctionType ft) -> isOne(ft._returnMultiplicity()) && ("Nil".equals(ft._returnType()._rawType()._name()) || "Integer".equals(ft._returnType()._rawType()._name())) && check(ft._parameters().toList(), (List<? extends VariableExpression> nps) -> nps.size() == 2 && isOne(nps.get(0)._multiplicity()) && isOne(nps.get(1)._multiplicity())))));
        map.put("meta::pure::functions::collection::sort_T_m__T_m_", (List<ValueSpecification> ps) -> ps.size() == 1);
        map.put("meta::pure::functions::collection::tail_T_MANY__T_MANY_", (List<ValueSpecification> ps) -> ps.size() == 1);
        map.put("meta::pure::functions::collection::take_T_MANY__Integer_1__T_MANY_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Integer".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::collection::union_T_MANY__T_MANY__T_MANY_", (List<ValueSpecification> ps) -> ps.size() == 2);
        map.put("meta::pure::functions::collection::uniqueValueOnly_T_MANY__T_$0_1$_", (List<ValueSpecification> ps) -> ps.size() == 1);
        map.put("meta::pure::functions::collection::uniqueValueOnly_T_MANY__T_$0_1$__T_$0_1$_", (List<ValueSpecification> ps) -> ps.size() == 2 && matchZeroOne(ps.get(1)._multiplicity()));
        map.put("meta::pure::functions::collection::zip_T_MANY__U_MANY__Pair_MANY_", (List<ValueSpecification> ps) -> ps.size() == 2);
        map.put("meta::pure::functions::constraints::warn_Boolean_1__String_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Boolean".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::date::add_StrictDate_1__Duration_1__StrictDate_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "StrictDate".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Duration".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::date::adjust_Date_1__Integer_1__DurationUnit_1__Date_1_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Integer".equals(ps.get(1)._genericType()._rawType()._name())) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "DurationUnit".equals(ps.get(2)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::date::dateDiff_Date_$0_1$__Date_$0_1$__DurationUnit_1__Integer_$0_1$_", (List<ValueSpecification> ps) -> ps.size() == 3 && matchZeroOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()) && matchZeroOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(1)._genericType()._rawType()._name()) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "DurationUnit".equals(ps.get(2)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::date::dateDiff_Date_1__Date_1__DurationUnit_1__Integer_1_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(1)._genericType()._rawType()._name()) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "DurationUnit".equals(ps.get(2)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::date::datePart_Date_$0_1$__Date_$0_1$_", (List<ValueSpecification> ps) -> ps.size() == 1 && matchZeroOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::date::datePart_Date_1__Date_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::date::date_Integer_1__Date_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Integer".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::date::date_Integer_1__Integer_1__Date_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Integer".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Integer".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::date::date_Integer_1__Integer_1__Integer_1__Integer_1__DateTime_1_", (List<ValueSpecification> ps) -> ps.size() == 4 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Integer".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Integer".equals(ps.get(1)._genericType()._rawType()._name())) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "Integer".equals(ps.get(2)._genericType()._rawType()._name())) && isOne(ps.get(3)._multiplicity()) && ("Nil".equals(ps.get(3)._genericType()._rawType()._name()) || "Integer".equals(ps.get(3)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::date::date_Integer_1__Integer_1__Integer_1__Integer_1__Integer_1__DateTime_1_", (List<ValueSpecification> ps) -> ps.size() == 5 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Integer".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Integer".equals(ps.get(1)._genericType()._rawType()._name())) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "Integer".equals(ps.get(2)._genericType()._rawType()._name())) && isOne(ps.get(3)._multiplicity()) && ("Nil".equals(ps.get(3)._genericType()._rawType()._name()) || "Integer".equals(ps.get(3)._genericType()._rawType()._name())) && isOne(ps.get(4)._multiplicity()) && ("Nil".equals(ps.get(4)._genericType()._rawType()._name()) || "Integer".equals(ps.get(4)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::date::date_Integer_1__Integer_1__Integer_1__Integer_1__Integer_1__Number_1__DateTime_1_", (List<ValueSpecification> ps) -> ps.size() == 6 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Integer".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Integer".equals(ps.get(1)._genericType()._rawType()._name())) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "Integer".equals(ps.get(2)._genericType()._rawType()._name())) && isOne(ps.get(3)._multiplicity()) && ("Nil".equals(ps.get(3)._genericType()._rawType()._name()) || "Integer".equals(ps.get(3)._genericType()._rawType()._name())) && isOne(ps.get(4)._multiplicity()) && ("Nil".equals(ps.get(4)._genericType()._rawType()._name()) || "Integer".equals(ps.get(4)._genericType()._rawType()._name())) && isOne(ps.get(5)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(5)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::date::date_Integer_1__Integer_1__Integer_1__StrictDate_1_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Integer".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Integer".equals(ps.get(1)._genericType()._rawType()._name())) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "Integer".equals(ps.get(2)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::date::dayOfMonth_Date_1__Integer_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::date::dayOfWeekNumber_Date_1__Integer_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::date::dayOfWeekNumber_Date_1__DayOfWeek_1__Integer_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "DayOfWeek".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::date::dayOfWeek_Date_1__DayOfWeek_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::date::dayOfWeek_Integer_1__DayOfWeek_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Integer".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::date::daysOfMonth_Date_1__Integer_MANY_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::date::firstDayOfMonth_Date_1__Date_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::date::firstDayOfQuarter_Date_1__StrictDate_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::date::firstDayOfThisMonth__Date_1_", (List<ValueSpecification> ps) -> ps.size() == 0);
        map.put("meta::pure::functions::date::firstDayOfThisQuarter__StrictDate_1_", (List<ValueSpecification> ps) -> ps.size() == 0);
        map.put("meta::pure::functions::date::firstDayOfThisWeek__Date_1_", (List<ValueSpecification> ps) -> ps.size() == 0);
        map.put("meta::pure::functions::date::firstDayOfThisYear__Date_1_", (List<ValueSpecification> ps) -> ps.size() == 0);
        map.put("meta::pure::functions::date::firstDayOfWeek_Date_1__Date_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::date::firstDayOfYear_Date_1__Date_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::date::hasDay_Date_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::date::hasHour_Date_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::date::hasMinute_Date_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::date::hasMonth_Date_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::date::hasSecond_Date_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::date::hasSubsecondWithAtLeastPrecision_Date_1__Integer_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Integer".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::date::hasSubsecond_Date_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::date::hasYear_Date_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::date::hour_Date_1__Integer_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::date::isAfterDay_Date_$0_1$__Date_$0_1$__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && matchZeroOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()) && matchZeroOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::date::isAfterDay_Date_$0_1$__Date_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && matchZeroOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::date::isAfterDay_Date_1__Date_$0_1$__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()) && matchZeroOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::date::isAfterDay_Date_1__Date_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::date::isBeforeDay_Date_$0_1$__Date_$0_1$__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && matchZeroOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()) && matchZeroOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::date::isBeforeDay_Date_$0_1$__Date_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && matchZeroOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::date::isBeforeDay_Date_1__Date_$0_1$__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()) && matchZeroOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::date::isBeforeDay_Date_1__Date_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::date::isOnDay_Date_$0_1$__Date_$0_1$__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && matchZeroOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()) && matchZeroOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::date::isOnDay_Date_$0_1$__Date_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && matchZeroOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::date::isOnDay_Date_1__Date_$0_1$__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()) && matchZeroOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::date::isOnDay_Date_1__Date_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::date::isOnOrAfterDay_Date_$0_1$__Date_$0_1$__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && matchZeroOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()) && matchZeroOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::date::isOnOrAfterDay_Date_$0_1$__Date_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && matchZeroOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::date::isOnOrAfterDay_Date_1__Date_$0_1$__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()) && matchZeroOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::date::isOnOrAfterDay_Date_1__Date_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::date::isOnOrBeforeDay_Date_$0_1$__Date_$0_1$__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && matchZeroOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()) && matchZeroOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::date::isOnOrBeforeDay_Date_$0_1$__Date_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && matchZeroOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::date::isOnOrBeforeDay_Date_1__Date_$0_1$__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()) && matchZeroOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::date::isOnOrBeforeDay_Date_1__Date_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::date::max_DateTime_1__DateTime_1__DateTime_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "DateTime".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "DateTime".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::date::max_DateTime_MANY__DateTime_$0_1$_", (List<ValueSpecification> ps) -> ps.size() == 1 && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "DateTime".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::date::max_Date_1__Date_1__Date_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::date::max_Date_MANY__Date_$0_1$_", (List<ValueSpecification> ps) -> ps.size() == 1 && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::date::max_StrictDate_1__StrictDate_1__StrictDate_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "StrictDate".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "StrictDate".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::date::max_StrictDate_MANY__StrictDate_$0_1$_", (List<ValueSpecification> ps) -> ps.size() == 1 && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "StrictDate".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::date::min_DateTime_1__DateTime_1__DateTime_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "DateTime".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "DateTime".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::date::min_DateTime_MANY__DateTime_$0_1$_", (List<ValueSpecification> ps) -> ps.size() == 1 && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "DateTime".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::date::min_Date_1__Date_1__Date_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::date::min_Date_MANY__Date_$0_1$_", (List<ValueSpecification> ps) -> ps.size() == 1 && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::date::min_StrictDate_1__StrictDate_1__StrictDate_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "StrictDate".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "StrictDate".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::date::min_StrictDate_MANY__StrictDate_$0_1$_", (List<ValueSpecification> ps) -> ps.size() == 1 && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "StrictDate".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::date::minute_Date_1__Integer_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::date::monthNumber_Date_$0_1$__Integer_$0_1$_", (List<ValueSpecification> ps) -> ps.size() == 1 && matchZeroOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::date::monthNumber_Date_1__Integer_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::date::month_Date_1__Month_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::date::month_Integer_1__Month_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Integer".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::date::mostRecentDayOfWeek_Date_1__DayOfWeek_1__Date_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "DayOfWeek".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::date::mostRecentDayOfWeek_DayOfWeek_1__Date_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "DayOfWeek".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::date::now__DateTime_1_", (List<ValueSpecification> ps) -> ps.size() == 0);
        map.put("meta::pure::functions::date::previousDayOfWeek_Date_1__DayOfWeek_1__Date_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "DayOfWeek".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::date::previousDayOfWeek_DayOfWeek_1__Date_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "DayOfWeek".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::date::quarterNumber_Date_1__Integer_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::date::quarter_Date_1__Quarter_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::date::quarter_Integer_1__Quarter_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Integer".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::date::second_Date_1__Integer_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::date::today__StrictDate_1_", (List<ValueSpecification> ps) -> ps.size() == 0);
        map.put("meta::pure::functions::date::weekOfYear_Date_$0_1$__Integer_$0_1$_", (List<ValueSpecification> ps) -> ps.size() == 1 && matchZeroOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::date::weekOfYear_Date_1__Integer_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::date::year_Date_$0_1$__Integer_$0_1$_", (List<ValueSpecification> ps) -> ps.size() == 1 && matchZeroOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::date::year_Date_1__Integer_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Date", "StrictDate", "DateTime", "LatestDate").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::lang::cast_Any_m__T_1__T_m_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(1)._multiplicity()));
        map.put("meta::pure::functions::lang::compare_T_1__T_1__Integer_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && isOne(ps.get(1)._multiplicity()));
        map.put("meta::pure::functions::lang::eval_Function_1__T_n__V_m_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || check(funcType(ps.get(0)._genericType()), (FunctionType ft) -> check(ft._parameters().toList(), (List<? extends VariableExpression> nps) -> nps.size() == 1))));
        map.put("meta::pure::functions::lang::eval_Function_1__V_m_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || check(funcType(ps.get(0)._genericType()), (FunctionType ft) -> check(ft._parameters().toList(), (List<? extends VariableExpression> nps) -> nps.size() == 0))));
        map.put("meta::pure::functions::lang::if_Boolean_1__Function_1__Function_1__T_m_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Boolean".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || check(funcType(ps.get(1)._genericType()), (FunctionType ft) -> check(ft._parameters().toList(), (List<? extends VariableExpression> nps) -> nps.size() == 0))) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || check(funcType(ps.get(2)._genericType()), (FunctionType ft) -> check(ft._parameters().toList(), (List<? extends VariableExpression> nps) -> nps.size() == 0))));
        map.put("meta::pure::functions::lang::letFunction_String_1__T_m__T_m_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::lang::match_Any_MANY__Function_$1_MANY$__T_m_", (List<ValueSpecification> ps) -> ps.size() == 2 && matchOneMany(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || check(funcType(ps.get(1)._genericType()), (FunctionType ft) -> check(ft._parameters().toList(), (List<? extends VariableExpression> nps) -> nps.size() == 1))));
        map.put("meta::pure::functions::lang::new_Class_1__String_1__KeyExpression_MANY__T_1_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Class", "MappingClass", "ClassProjection").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "KeyExpression".equals(ps.get(2)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::lang::subType_Any_m__T_1__T_m_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(1)._multiplicity()));
        map.put("meta::pure::functions::math::abs_Float_1__Float_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Float".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::math::abs_Integer_1__Integer_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Integer".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::math::abs_Number_1__Number_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::math::acos_Number_1__Float_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::math::asin_Number_1__Float_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::math::atan2_Number_1__Number_1__Float_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::math::atan_Number_1__Float_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::math::average_Float_MANY__Float_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Float".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::math::average_Integer_MANY__Float_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Integer".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::math::average_Number_MANY__Float_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::math::ceiling_Number_1__Integer_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::math::cos_Number_1__Float_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::math::distanceHaversineDegrees_Number_1__Number_1__Number_1__Number_1__Number_1_", (List<ValueSpecification> ps) -> ps.size() == 4 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(1)._genericType()._rawType()._name()) && isOne(ps.get(2)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(2)._genericType()._rawType()._name()) && isOne(ps.get(3)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(3)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::math::distanceHaversineRadians_Number_1__Number_1__Number_1__Number_1__Number_1_", (List<ValueSpecification> ps) -> ps.size() == 4 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(1)._genericType()._rawType()._name()) && isOne(ps.get(2)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(2)._genericType()._rawType()._name()) && isOne(ps.get(3)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(3)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::math::distanceSphericalLawOfCosinesDegrees_Number_1__Number_1__Number_1__Number_1__Number_1_", (List<ValueSpecification> ps) -> ps.size() == 4 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(1)._genericType()._rawType()._name()) && isOne(ps.get(2)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(2)._genericType()._rawType()._name()) && isOne(ps.get(3)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(3)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::math::distanceSphericalLawOfCosinesRadians_Number_1__Number_1__Number_1__Number_1__Number_1_", (List<ValueSpecification> ps) -> ps.size() == 4 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(1)._genericType()._rawType()._name()) && isOne(ps.get(2)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(2)._genericType()._rawType()._name()) && isOne(ps.get(3)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(3)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::math::divide_Number_1__Number_1__Float_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::math::earthRadius__Float_1_", (List<ValueSpecification> ps) -> ps.size() == 0);
        map.put("meta::pure::functions::math::exp_Number_1__Float_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::math::floor_Number_1__Integer_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::math::log_Number_1__Float_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::math::max_Float_$1_MANY$__Float_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && matchOneMany(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Float".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::math::max_Float_1__Float_1__Float_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Float".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Float".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::math::max_Float_MANY__Float_$0_1$_", (List<ValueSpecification> ps) -> ps.size() == 1 && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Float".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::math::max_Integer_$1_MANY$__Integer_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && matchOneMany(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Integer".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::math::max_Integer_1__Integer_1__Integer_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Integer".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Integer".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::math::max_Integer_MANY__Integer_$0_1$_", (List<ValueSpecification> ps) -> ps.size() == 1 && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Integer".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::math::max_Number_$1_MANY$__Number_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && matchOneMany(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::math::max_Number_1__Number_1__Number_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::math::max_Number_MANY__Number_$0_1$_", (List<ValueSpecification> ps) -> ps.size() == 1 && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::math::mean_Float_MANY__Float_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Float".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::math::mean_Integer_MANY__Float_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Integer".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::math::mean_Number_MANY__Float_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::math::min_Float_$1_MANY$__Float_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && matchOneMany(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Float".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::math::min_Float_1__Float_1__Float_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Float".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Float".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::math::min_Float_MANY__Float_$0_1$_", (List<ValueSpecification> ps) -> ps.size() == 1 && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Float".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::math::min_Integer_$1_MANY$__Integer_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && matchOneMany(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Integer".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::math::min_Integer_1__Integer_1__Integer_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Integer".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Integer".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::math::min_Integer_MANY__Integer_$0_1$_", (List<ValueSpecification> ps) -> ps.size() == 1 && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Integer".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::math::min_Number_$1_MANY$__Number_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && matchOneMany(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::math::min_Number_1__Number_1__Number_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::math::min_Number_MANY__Number_$0_1$_", (List<ValueSpecification> ps) -> ps.size() == 1 && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::math::minus_Decimal_MANY__Decimal_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Decimal".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::math::minus_Float_MANY__Float_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Float".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::math::minus_Integer_MANY__Integer_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Integer".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::math::minus_Number_MANY__Number_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::math::mod_Integer_1__Integer_1__Integer_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Integer".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Integer".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::math::olap::averageRank_Any_MANY__Map_1_", (List<ValueSpecification> ps) -> ps.size() == 1);
        map.put("meta::pure::functions::math::olap::denseRank_Any_MANY__Map_1_", (List<ValueSpecification> ps) -> ps.size() == 1);
        map.put("meta::pure::functions::math::olap::rank_Any_MANY__Map_1_", (List<ValueSpecification> ps) -> ps.size() == 1);
        map.put("meta::pure::functions::math::percentile_Number_MANY__Float_1__Boolean_1__Boolean_1__Number_$0_1$_", (List<ValueSpecification> ps) -> ps.size() == 4 && Sets.immutable.with("Nil", "Number", "Decimal", "Float", "Integer").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Float".equals(ps.get(1)._genericType()._rawType()._name())) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "Boolean".equals(ps.get(2)._genericType()._rawType()._name())) && isOne(ps.get(3)._multiplicity()) && ("Nil".equals(ps.get(3)._genericType()._rawType()._name()) || "Boolean".equals(ps.get(3)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::math::percentile_Number_MANY__Float_1__Number_$0_1$_", (List<ValueSpecification> ps) -> ps.size() == 2 && Sets.immutable.with("Nil", "Number", "Decimal", "Float", "Integer").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Float".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::math::pi__Float_1_", (List<ValueSpecification> ps) -> ps.size() == 0);
        map.put("meta::pure::functions::math::plus_Decimal_MANY__Decimal_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Decimal".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::math::plus_Float_MANY__Float_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Float".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::math::plus_Integer_MANY__Integer_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Integer".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::math::plus_Number_MANY__Number_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::math::pow_Number_1__Number_1__Number_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::math::rem_Number_1__Number_1__Number_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::math::round_Decimal_1__Integer_1__Decimal_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Decimal".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Integer".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::math::round_Float_1__Integer_1__Float_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Float".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Integer".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::math::round_Number_1__Integer_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::math::sin_Number_1__Float_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::math::sqrt_Number_1__Float_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::math::stdDevPopulation_Number_$1_MANY$__Number_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && matchOneMany(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::math::stdDevPopulation_Number_MANY__Number_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::math::stdDevSample_Number_$1_MANY$__Number_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && matchOneMany(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::math::stdDevSample_Number_MANY__Number_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::math::sum_Float_MANY__Float_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Float".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::math::sum_Integer_MANY__Integer_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Integer".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::math::sum_Number_MANY__Number_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::math::tan_Number_1__Float_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::math::times_Decimal_MANY__Decimal_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Decimal".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::math::times_Float_MANY__Float_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Float".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::math::times_Integer_MANY__Integer_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Integer".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::math::times_Number_MANY__Number_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::math::toDecimal_Number_1__Decimal_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::math::toDegrees_Number_1__Float_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::math::toFloat_Number_1__Float_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::math::toRadians_Number_1__Float_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::meta::enumName_Enumeration_1__String_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()));
        map.put("meta::pure::functions::meta::enumValues_Enumeration_1__T_MANY_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()));
        map.put("meta::pure::functions::meta::extractEnumValue_Enumeration_1__String_1__T_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::meta::id_Any_1__String_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()));
        map.put("meta::pure::functions::meta::instanceOf_Any_1__Type_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Type", "Class", "DataType", "Measure", "FunctionType", "MappingClass", "ClassProjection", "PrimitiveType", "Unit", "Enumeration").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::meta::newUnit_Unit_1__Number_1__Any_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Unit".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "Number", "Integer", "Float", "Decimal").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::meta::type_Any_MANY__Type_1_", (List<ValueSpecification> ps) -> ps.size() == 1);
        map.put("meta::pure::functions::meta::typeName_Any_1__String_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()));
        map.put("meta::pure::functions::meta::typePath_Any_1__String_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()));
        map.put("meta::pure::functions::multiplicity::toOneMany_T_MANY__T_$1_MANY$_", (List<ValueSpecification> ps) -> ps.size() == 1);
        map.put("meta::pure::functions::multiplicity::toOne_T_MANY__T_1_", (List<ValueSpecification> ps) -> ps.size() == 1);
        map.put("meta::pure::functions::string::chunk_String_1__Integer_1__String_MANY_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Integer".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::string::contains_String_$0_1$__String_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && matchZeroOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::string::contains_String_1__String_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::string::endsWith_String_$0_1$__String_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && matchZeroOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::string::endsWith_String_1__String_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::string::equalIgnoreCase_String_1__String_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::string::format_String_1__Any_MANY__String_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::string::humanize_String_1__String_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::string::indexOf_String_1__String_1__Integer_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::string::indexOf_String_1__String_1__Integer_1__Integer_1_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "Integer".equals(ps.get(2)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::string::isDigit_String_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::string::isDigit_String_1__Integer_1__Integer_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Integer".equals(ps.get(1)._genericType()._rawType()._name())) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "Integer".equals(ps.get(2)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::string::isLetter_String_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::string::isLetter_String_1__Integer_1__Integer_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Integer".equals(ps.get(1)._genericType()._rawType()._name())) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "Integer".equals(ps.get(2)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::string::isAlphaNumeric_String_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::string::isLowerCase_String_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::string::isNoLongerThan_String_$0_1$__Integer_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && matchZeroOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Integer".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::string::isNoShorterThan_String_$0_1$__Integer_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && matchZeroOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Integer".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::string::isUUID_String_$0_1$__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && matchZeroOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())));

        map.put("meta::pure::functions::string::isUpperCase_String_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::string::joinStrings_String_MANY__String_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::string::joinStrings_String_MANY__String_1__String_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::string::joinStrings_String_MANY__String_1__String_1__String_1__String_1_", (List<ValueSpecification> ps) -> ps.size() == 4 && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "String".equals(ps.get(2)._genericType()._rawType()._name())) && isOne(ps.get(3)._multiplicity()) && ("Nil".equals(ps.get(3)._genericType()._rawType()._name()) || "String".equals(ps.get(3)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::string::lastIndexOf_String_1__String_1__Integer_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::string::length_String_1__Integer_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::string::makeCamelCase_String_1__Boolean_1__String_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Boolean".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::string::makeCamelCase_String_1__String_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::string::makeString_Any_MANY__String_1_", (List<ValueSpecification> ps) -> ps.size() == 1);
        map.put("meta::pure::functions::string::makeString_Any_MANY__String_1__String_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::string::makeString_Any_MANY__String_1__String_1__String_1__String_1_", (List<ValueSpecification> ps) -> ps.size() == 4 && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "String".equals(ps.get(2)._genericType()._rawType()._name())) && isOne(ps.get(3)._multiplicity()) && ("Nil".equals(ps.get(3)._genericType()._rawType()._name()) || "String".equals(ps.get(3)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::string::makeString_Pair_MANY__String_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && Sets.immutable.with("Nil", "Pair", "StringFunctionPair", "OldAliasToNewAlias", "StringFunctionPair", "PureFunctionToRelationalFunctionPair", "DispatchPair").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::functions::string::matches_String_1__String_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::string::parseBoolean_String_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::string::parseDate_String_1__Date_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::string::parseDecimal_String_1__Decimal_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::string::parseFloat_String_1__Float_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::string::parseInteger_String_1__Integer_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::string::plus_String_MANY__String_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::string::replace_String_1__String_1__String_1__String_1_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "String".equals(ps.get(2)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::string::splitOnCamelCase_String_1__String_MANY_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::string::split_String_1__String_1__String_MANY_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::string::startsWith_String_$0_1$__String_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && matchZeroOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::string::startsWith_String_1__String_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::string::substringAfter_String_1__String_1__String_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::string::substringBefore_String_1__String_1__String_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::string::substring_String_1__Integer_1__Integer_1__String_1_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Integer".equals(ps.get(1)._genericType()._rawType()._name())) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "Integer".equals(ps.get(2)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::string::substring_String_1__Integer_1__String_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Integer".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::string::toLower_String_1__String_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::string::toString_Any_1__String_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()));
        map.put("meta::pure::functions::string::toUpper_String_1__String_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::functions::string::trim_String_1__String_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::graphFetch::calculateSourceTree_RootGraphFetchTree_1__Mapping_1__Extension_MANY__RootGraphFetchTree_1_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "RootGraphFetchTree", "ExtendedRootGraphFetchTree", "RoutedRootGraphFetchTree", "SerializeTopRootGraphFetchTree").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Mapping".equals(ps.get(1)._genericType()._rawType()._name())) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "Extension".equals(ps.get(2)._genericType()._rawType()._name())));
        map.put("meta::pure::graphFetch::execution::graphFetchChecked_T_MANY__RootGraphFetchTree_1__Checked_MANY_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "RootGraphFetchTree", "ExtendedRootGraphFetchTree", "RoutedRootGraphFetchTree", "SerializeTopRootGraphFetchTree").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::graphFetch::execution::graphFetch_T_MANY__RootGraphFetchTree_1__Integer_1__T_MANY_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "RootGraphFetchTree", "ExtendedRootGraphFetchTree", "RoutedRootGraphFetchTree", "SerializeTopRootGraphFetchTree").contains(ps.get(1)._genericType()._rawType()._name()) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "Integer".equals(ps.get(2)._genericType()._rawType()._name())));
        map.put("meta::pure::graphFetch::execution::graphFetch_T_MANY__RootGraphFetchTree_1__T_MANY_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "RootGraphFetchTree", "ExtendedRootGraphFetchTree", "RoutedRootGraphFetchTree", "SerializeTopRootGraphFetchTree").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::graphFetch::execution::serialize_Checked_MANY__RootGraphFetchTree_1__AlloySerializationConfig_1__String_1_", (List<ValueSpecification> ps) -> ps.size() == 3 && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Checked".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "RootGraphFetchTree", "ExtendedRootGraphFetchTree", "RoutedRootGraphFetchTree", "SerializeTopRootGraphFetchTree").contains(ps.get(1)._genericType()._rawType()._name()) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "AlloySerializationConfig".equals(ps.get(2)._genericType()._rawType()._name())));
        map.put("meta::pure::graphFetch::execution::serialize_Checked_MANY__RootGraphFetchTree_1__String_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Checked".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "RootGraphFetchTree", "ExtendedRootGraphFetchTree", "RoutedRootGraphFetchTree", "SerializeTopRootGraphFetchTree").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::graphFetch::execution::serialize_T_MANY__RootGraphFetchTree_1__AlloySerializationConfig_1__String_1_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "RootGraphFetchTree", "ExtendedRootGraphFetchTree", "RoutedRootGraphFetchTree", "SerializeTopRootGraphFetchTree").contains(ps.get(1)._genericType()._rawType()._name()) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "AlloySerializationConfig".equals(ps.get(2)._genericType()._rawType()._name())));
        map.put("meta::pure::graphFetch::execution::serialize_T_MANY__RootGraphFetchTree_1__String_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "RootGraphFetchTree", "ExtendedRootGraphFetchTree", "RoutedRootGraphFetchTree", "SerializeTopRootGraphFetchTree").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::mapping::from_T_m__Mapping_1__Runtime_1__T_m_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Mapping".equals(ps.get(1)._genericType()._rawType()._name())) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "Runtime".equals(ps.get(2)._genericType()._rawType()._name())));
        map.put("meta::pure::mapping::from_TabularDataSet_1__Mapping_1__Runtime_1__ExecutionContext_1__TabularDataSet_1_", (List<ValueSpecification> ps) -> ps.size() == 4 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "TabularDataSet", "TabularDataSetImplementation", "TableTDS").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Mapping".equals(ps.get(1)._genericType()._rawType()._name())) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "Runtime".equals(ps.get(2)._genericType()._rawType()._name())) && isOne(ps.get(3)._multiplicity()) && Sets.immutable.with("Nil", "ExecutionContext", "ExtendedExecutionContext", "RelationalExecutionContext", "AuthenticationContext", "AnalyticsExecutionContext", "BatchQueryContext", "LatestBatchQueryContext", "DirtyLatestBatchQueryContext", "VectorBatchQueryContext", "WatermarkExecutionContext", "SpecificWatermarkExecutionContext", "RefinerWatermarkExecutionContext").contains(ps.get(3)._genericType()._rawType()._name()));
        map.put("meta::pure::mapping::from_TabularDataSet_1__Mapping_1__Runtime_1__TabularDataSet_1_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "TabularDataSet", "TabularDataSetImplementation", "TableTDS").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Mapping".equals(ps.get(1)._genericType()._rawType()._name())) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "Runtime".equals(ps.get(2)._genericType()._rawType()._name())));
        map.put("meta::pure::mapping::from_T_m__SingleExecutionParameters_1__T_m_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "SingleExecutionParameters".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::legend::service::get_ExecutionEnvironmentInstance_1__String_1__SingleExecutionParameters_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "ExecutionEnvironmentInstance".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::legend::service::get_ExecutionEnvironmentInstance_1__String_1__String_1__SingleExecutionParameters_1_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "ExecutionEnvironmentInstance".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "String".equals(ps.get(2)._genericType()._rawType()._name())));
        map.put("meta::pure::runtime::getRuntimeWithModelConnection_Class_1__Any_MANY__Runtime_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Class", "MappingClass", "ClassProjection").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::tds::agg_String_1__FunctionDefinition_1__FunctionDefinition_1__AggregateValue_1_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "FunctionDefinition", "QualifiedProperty", "ConcreteFunctionDefinition", "LambdaFunction", "NewPropertyRouteNodeFunctionDefinition").contains(ps.get(1)._genericType()._rawType()._name()) && isOne(ps.get(2)._multiplicity()) && Sets.immutable.with("Nil", "FunctionDefinition", "QualifiedProperty", "ConcreteFunctionDefinition", "LambdaFunction", "NewPropertyRouteNodeFunctionDefinition").contains(ps.get(2)._genericType()._rawType()._name()));
        map.put("meta::pure::tds::asc_String_1__SortInformation_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::tds::col_Function_1__String_1__BasicColumnSpecification_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || check(funcType(ps.get(0)._genericType()), (FunctionType ft) -> check(ft._parameters().toList(), (List<? extends VariableExpression> nps) -> nps.size() == 1 && isOne(nps.get(0)._multiplicity())))) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::tds::col_Function_1__String_1__String_1__BasicColumnSpecification_1_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || check(funcType(ps.get(0)._genericType()), (FunctionType ft) -> check(ft._parameters().toList(), (List<? extends VariableExpression> nps) -> nps.size() == 1 && isOne(nps.get(0)._multiplicity())))) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "String".equals(ps.get(2)._genericType()._rawType()._name())));
        map.put("meta::pure::tds::concatenate_TabularDataSet_1__TabularDataSet_1__TabularDataSet_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "TabularDataSet", "TabularDataSetImplementation", "TableTDS").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "TabularDataSet", "TabularDataSetImplementation", "TableTDS").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::tds::desc_String_1__SortInformation_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())));
        map.put("meta::pure::tds::distinct_TabularDataSet_1__TabularDataSet_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "TabularDataSet", "TabularDataSetImplementation", "TableTDS").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::tds::drop_TabularDataSet_1__Integer_1__TabularDataSet_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "TabularDataSet", "TabularDataSetImplementation", "TableTDS").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Integer".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::tds::extend_TabularDataSet_1__BasicColumnSpecification_MANY__TabularDataSet_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "TabularDataSet", "TabularDataSetImplementation", "TableTDS").contains(ps.get(0)._genericType()._rawType()._name()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "BasicColumnSpecification".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::tds::extensions::columnValueDifference_TabularDataSet_1__TabularDataSet_1__String_$1_MANY$__String_$1_MANY$__String_$1_MANY$__TabularDataSet_1_", (List<ValueSpecification> ps) -> ps.size() == 5 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "TabularDataSet", "TabularDataSetImplementation", "TableTDS").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "TabularDataSet", "TabularDataSetImplementation", "TableTDS").contains(ps.get(1)._genericType()._rawType()._name()) && matchOneMany(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "String".equals(ps.get(2)._genericType()._rawType()._name())) && matchOneMany(ps.get(3)._multiplicity()) && ("Nil".equals(ps.get(3)._genericType()._rawType()._name()) || "String".equals(ps.get(3)._genericType()._rawType()._name())) && matchOneMany(ps.get(4)._multiplicity()) && ("Nil".equals(ps.get(4)._genericType()._rawType()._name()) || "String".equals(ps.get(4)._genericType()._rawType()._name())));
        map.put("meta::pure::tds::extensions::columnValueDifference_TabularDataSet_1__TabularDataSet_1__String_$1_MANY$__String_$1_MANY$__TabularDataSet_1_", (List<ValueSpecification> ps) -> ps.size() == 4 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "TabularDataSet", "TabularDataSetImplementation", "TableTDS").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "TabularDataSet", "TabularDataSetImplementation", "TableTDS").contains(ps.get(1)._genericType()._rawType()._name()) && matchOneMany(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "String".equals(ps.get(2)._genericType()._rawType()._name())) && matchOneMany(ps.get(3)._multiplicity()) && ("Nil".equals(ps.get(3)._genericType()._rawType()._name()) || "String".equals(ps.get(3)._genericType()._rawType()._name())));
        map.put("meta::pure::tds::filter_TabularDataSet_1__Function_1__TabularDataSet_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "TabularDataSet", "TabularDataSetImplementation", "TableTDS").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || check(funcType(ps.get(1)._genericType()), (FunctionType ft) -> isOne(ft._returnMultiplicity()) && ("Nil".equals(ft._returnType()._rawType()._name()) || "Boolean".equals(ft._returnType()._rawType()._name())) && check(ft._parameters().toList(), (List<? extends VariableExpression> nps) -> nps.size() == 1 && isOne(nps.get(0)._multiplicity()) && Sets.immutable.with("TDSRow", "Any").contains(nps.get(0)._genericType()._rawType()._name())))));
        map.put("meta::pure::tds::groupByWithWindowSubset_K_MANY__Function_MANY__AggregateValue_MANY__String_MANY__String_MANY__String_MANY__TabularDataSet_1_", (List<ValueSpecification> ps) -> ps.size() == 6 && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || check(funcType(ps.get(1)._genericType()), (FunctionType ft) -> check(ft._parameters().toList(), (List<? extends VariableExpression> nps) -> nps.size() == 1 && isOne(nps.get(0)._multiplicity())))) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "AggregateValue".equals(ps.get(2)._genericType()._rawType()._name())) && ("Nil".equals(ps.get(3)._genericType()._rawType()._name()) || "String".equals(ps.get(3)._genericType()._rawType()._name())) && ("Nil".equals(ps.get(4)._genericType()._rawType()._name()) || "String".equals(ps.get(4)._genericType()._rawType()._name())) && ("Nil".equals(ps.get(5)._genericType()._rawType()._name()) || "String".equals(ps.get(5)._genericType()._rawType()._name())));
        map.put("meta::pure::tds::groupBy_K_MANY__Function_MANY__AggregateValue_MANY__String_MANY__TabularDataSet_1_", (List<ValueSpecification> ps) -> ps.size() == 4 && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || check(funcType(ps.get(1)._genericType()), (FunctionType ft) -> check(ft._parameters().toList(), (List<? extends VariableExpression> nps) -> nps.size() == 1 && isOne(nps.get(0)._multiplicity())))) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "AggregateValue".equals(ps.get(2)._genericType()._rawType()._name())) && ("Nil".equals(ps.get(3)._genericType()._rawType()._name()) || "String".equals(ps.get(3)._genericType()._rawType()._name())));
        map.put("meta::pure::tds::groupBy_TabularDataSet_1__String_MANY__AggregateValue_MANY__TabularDataSet_1_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "TabularDataSet", "TabularDataSetImplementation", "TableTDS").contains(ps.get(0)._genericType()._rawType()._name()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "AggregateValue".equals(ps.get(2)._genericType()._rawType()._name())));
        map.put("meta::pure::tds::join_TabularDataSet_1__TabularDataSet_1__JoinType_1__Function_1__TabularDataSet_1_", (List<ValueSpecification> ps) -> ps.size() == 4 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "TabularDataSet", "TabularDataSetImplementation", "TableTDS").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "TabularDataSet", "TabularDataSetImplementation", "TableTDS").contains(ps.get(1)._genericType()._rawType()._name()) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "JoinType".equals(ps.get(2)._genericType()._rawType()._name())) && isOne(ps.get(3)._multiplicity()) && ("Nil".equals(ps.get(3)._genericType()._rawType()._name()) || check(funcType(ps.get(3)._genericType()), (FunctionType ft) -> isOne(ft._returnMultiplicity()) && ("Nil".equals(ft._returnType()._rawType()._name()) || "Boolean".equals(ft._returnType()._rawType()._name())) && check(ft._parameters().toList(), (List<? extends VariableExpression> nps) -> nps.size() == 2 && isOne(nps.get(0)._multiplicity()) && Sets.immutable.with("TDSRow", "Any").contains(nps.get(0)._genericType()._rawType()._name()) && isOne(nps.get(1)._multiplicity()) && Sets.immutable.with("TDSRow", "Any").contains(nps.get(1)._genericType()._rawType()._name())))));
        map.put("meta::pure::tds::join_TabularDataSet_1__TabularDataSet_1__JoinType_1__String_$1_MANY$__String_$1_MANY$__TabularDataSet_1_", (List<ValueSpecification> ps) -> ps.size() == 5 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "TabularDataSet", "TabularDataSetImplementation", "TableTDS").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "TabularDataSet", "TabularDataSetImplementation", "TableTDS").contains(ps.get(1)._genericType()._rawType()._name()) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "JoinType".equals(ps.get(2)._genericType()._rawType()._name())) && matchOneMany(ps.get(3)._multiplicity()) && ("Nil".equals(ps.get(3)._genericType()._rawType()._name()) || "String".equals(ps.get(3)._genericType()._rawType()._name())) && matchOneMany(ps.get(4)._multiplicity()) && ("Nil".equals(ps.get(4)._genericType()._rawType()._name()) || "String".equals(ps.get(4)._genericType()._rawType()._name())));
        map.put("meta::pure::tds::join_TabularDataSet_1__TabularDataSet_1__JoinType_1__String_$1_MANY$__TabularDataSet_1_", (List<ValueSpecification> ps) -> ps.size() == 4 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "TabularDataSet", "TabularDataSetImplementation", "TableTDS").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "TabularDataSet", "TabularDataSetImplementation", "TableTDS").contains(ps.get(1)._genericType()._rawType()._name()) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "JoinType".equals(ps.get(2)._genericType()._rawType()._name())) && matchOneMany(ps.get(3)._multiplicity()) && ("Nil".equals(ps.get(3)._genericType()._rawType()._name()) || "String".equals(ps.get(3)._genericType()._rawType()._name())));
        map.put("meta::pure::tds::limit_TabularDataSet_1__Integer_$0_1$__TabularDataSet_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "TabularDataSet", "TabularDataSetImplementation", "TableTDS").contains(ps.get(0)._genericType()._rawType()._name()) && matchZeroOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Integer".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::tds::limit_TabularDataSet_1__Integer_1__TabularDataSet_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "TabularDataSet", "TabularDataSetImplementation", "TableTDS").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Integer".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::tds::olapGroupBy_TabularDataSet_1__FunctionDefinition_1__String_1__TabularDataSet_1_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "TabularDataSet", "TabularDataSetImplementation", "TableTDS").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "FunctionDefinition", "QualifiedProperty", "ConcreteFunctionDefinition", "LambdaFunction", "NewPropertyRouteNodeFunctionDefinition").contains(ps.get(1)._genericType()._rawType()._name()) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "String".equals(ps.get(2)._genericType()._rawType()._name())));
        map.put("meta::pure::tds::olapGroupBy_TabularDataSet_1__OlapOperation_1__String_1__TabularDataSet_1_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "TabularDataSet", "TabularDataSetImplementation", "TableTDS").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "OlapOperation", "OlapAggregation", "OlapRank", "TdsOlapAggregation", "TdsOlapRank").contains(ps.get(1)._genericType()._rawType()._name()) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "String".equals(ps.get(2)._genericType()._rawType()._name())));
        map.put("meta::pure::tds::olapGroupBy_TabularDataSet_1__SortInformation_$0_1$__FunctionDefinition_1__String_1__TabularDataSet_1_", (List<ValueSpecification> ps) -> ps.size() == 4 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "TabularDataSet", "TabularDataSetImplementation", "TableTDS").contains(ps.get(0)._genericType()._rawType()._name()) && matchZeroOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "SortInformation".equals(ps.get(1)._genericType()._rawType()._name())) && isOne(ps.get(2)._multiplicity()) && Sets.immutable.with("Nil", "FunctionDefinition", "QualifiedProperty", "ConcreteFunctionDefinition", "LambdaFunction", "NewPropertyRouteNodeFunctionDefinition").contains(ps.get(2)._genericType()._rawType()._name()) && isOne(ps.get(3)._multiplicity()) && ("Nil".equals(ps.get(3)._genericType()._rawType()._name()) || "String".equals(ps.get(3)._genericType()._rawType()._name())));
        map.put("meta::pure::tds::olapGroupBy_TabularDataSet_1__SortInformation_$0_1$__OlapOperation_1__String_1__TabularDataSet_1_", (List<ValueSpecification> ps) -> ps.size() == 4 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "TabularDataSet", "TabularDataSetImplementation", "TableTDS").contains(ps.get(0)._genericType()._rawType()._name()) && matchZeroOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "SortInformation".equals(ps.get(1)._genericType()._rawType()._name())) && isOne(ps.get(2)._multiplicity()) && Sets.immutable.with("Nil", "OlapOperation", "OlapAggregation", "OlapRank", "TdsOlapAggregation", "TdsOlapRank").contains(ps.get(2)._genericType()._rawType()._name()) && isOne(ps.get(3)._multiplicity()) && ("Nil".equals(ps.get(3)._genericType()._rawType()._name()) || "String".equals(ps.get(3)._genericType()._rawType()._name())));
        map.put("meta::pure::tds::olapGroupBy_TabularDataSet_1__String_MANY__FunctionDefinition_1__String_1__TabularDataSet_1_", (List<ValueSpecification> ps) -> ps.size() == 4 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "TabularDataSet", "TabularDataSetImplementation", "TableTDS").contains(ps.get(0)._genericType()._rawType()._name()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())) && isOne(ps.get(2)._multiplicity()) && Sets.immutable.with("Nil", "FunctionDefinition", "QualifiedProperty", "ConcreteFunctionDefinition", "LambdaFunction", "NewPropertyRouteNodeFunctionDefinition").contains(ps.get(2)._genericType()._rawType()._name()) && isOne(ps.get(3)._multiplicity()) && ("Nil".equals(ps.get(3)._genericType()._rawType()._name()) || "String".equals(ps.get(3)._genericType()._rawType()._name())));
        map.put("meta::pure::tds::olapGroupBy_TabularDataSet_1__String_MANY__OlapOperation_1__String_1__TabularDataSet_1_", (List<ValueSpecification> ps) -> ps.size() == 4 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "TabularDataSet", "TabularDataSetImplementation", "TableTDS").contains(ps.get(0)._genericType()._rawType()._name()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())) && isOne(ps.get(2)._multiplicity()) && Sets.immutable.with("Nil", "OlapOperation", "OlapAggregation", "OlapRank", "TdsOlapAggregation", "TdsOlapRank").contains(ps.get(2)._genericType()._rawType()._name()) && isOne(ps.get(3)._multiplicity()) && ("Nil".equals(ps.get(3)._genericType()._rawType()._name()) || "String".equals(ps.get(3)._genericType()._rawType()._name())));
        map.put("meta::pure::tds::olapGroupBy_TabularDataSet_1__String_MANY__SortInformation_$0_1$__FunctionDefinition_1__String_1__TabularDataSet_1_", (List<ValueSpecification> ps) -> ps.size() == 5 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "TabularDataSet", "TabularDataSetImplementation", "TableTDS").contains(ps.get(0)._genericType()._rawType()._name()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())) && matchZeroOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "SortInformation".equals(ps.get(2)._genericType()._rawType()._name())) && isOne(ps.get(3)._multiplicity()) && Sets.immutable.with("Nil", "FunctionDefinition", "QualifiedProperty", "ConcreteFunctionDefinition", "LambdaFunction", "NewPropertyRouteNodeFunctionDefinition").contains(ps.get(3)._genericType()._rawType()._name()) && isOne(ps.get(4)._multiplicity()) && ("Nil".equals(ps.get(4)._genericType()._rawType()._name()) || "String".equals(ps.get(4)._genericType()._rawType()._name())));
        map.put("meta::pure::tds::olapGroupBy_TabularDataSet_1__String_MANY__SortInformation_$0_1$__OlapOperation_1__String_1__TabularDataSet_1_", (List<ValueSpecification> ps) -> ps.size() == 5 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "TabularDataSet", "TabularDataSetImplementation", "TableTDS").contains(ps.get(0)._genericType()._rawType()._name()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())) && matchZeroOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "SortInformation".equals(ps.get(2)._genericType()._rawType()._name())) && isOne(ps.get(3)._multiplicity()) && Sets.immutable.with("Nil", "OlapOperation", "OlapAggregation", "OlapRank", "TdsOlapAggregation", "TdsOlapRank").contains(ps.get(3)._genericType()._rawType()._name()) && isOne(ps.get(4)._multiplicity()) && ("Nil".equals(ps.get(4)._genericType()._rawType()._name()) || "String".equals(ps.get(4)._genericType()._rawType()._name())));
        map.put("meta::pure::tds::func_FunctionDefinition_1__TdsOlapRank_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "FunctionDefinition", "QualifiedProperty", "ConcreteFunctionDefinition", "LambdaFunction", "NewPropertyRouteNodeFunctionDefinition").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::tds::func_String_1__FunctionDefinition_1__TdsOlapAggregation_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "String".equals(ps.get(0)._genericType()._rawType()._name())) && isOne(ps.get(1)._multiplicity()) && Sets.immutable.with("Nil", "FunctionDefinition", "QualifiedProperty", "ConcreteFunctionDefinition", "LambdaFunction", "NewPropertyRouteNodeFunctionDefinition").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::tds::projectWithColumnSubset_T_MANY__ColumnSpecification_MANY__String_MANY__TabularDataSet_1_", (List<ValueSpecification> ps) -> ps.size() == 3 && Sets.immutable.with("Nil", "ColumnSpecification", "BasicColumnSpecification", "WindowColumnSpecification").contains(ps.get(1)._genericType()._rawType()._name()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "String".equals(ps.get(2)._genericType()._rawType()._name())));
        map.put("meta::pure::tds::projectWithColumnSubset_T_MANY__Function_MANY__String_MANY__String_MANY__TabularDataSet_1_", (List<ValueSpecification> ps) -> ps.size() == 4 && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || check(funcType(ps.get(1)._genericType()), (FunctionType ft) -> check(ft._parameters().toList(), (List<? extends VariableExpression> nps) -> nps.size() == 1 && isOne(nps.get(0)._multiplicity())))) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "String".equals(ps.get(2)._genericType()._rawType()._name())) && ("Nil".equals(ps.get(3)._genericType()._rawType()._name()) || "String".equals(ps.get(3)._genericType()._rawType()._name())));
        map.put("meta::pure::tds::project_K_MANY__Function_MANY__String_MANY__TabularDataSet_1_", (List<ValueSpecification> ps) -> ps.size() == 3 && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || check(funcType(ps.get(1)._genericType()), (FunctionType ft) -> check(ft._parameters().toList(), (List<? extends VariableExpression> nps) -> nps.size() == 1 && isOne(nps.get(0)._multiplicity())))) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "String".equals(ps.get(2)._genericType()._rawType()._name())));
        map.put("meta::pure::tds::project_T_MANY__ColumnSpecification_MANY__TabularDataSet_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && Sets.immutable.with("Nil", "ColumnSpecification", "BasicColumnSpecification", "WindowColumnSpecification").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::tds::project_T_MANY__Path_MANY__TabularDataSet_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Path".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::tds::project_TableTDS_1__ColumnSpecification_MANY__TabularDataSet_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "TableTDS".equals(ps.get(0)._genericType()._rawType()._name())) && Sets.immutable.with("Nil", "ColumnSpecification", "BasicColumnSpecification", "WindowColumnSpecification").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::tds::project_TabularDataSet_1__ColumnSpecification_MANY__TabularDataSet_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "TabularDataSet", "TabularDataSetImplementation", "TableTDS").contains(ps.get(0)._genericType()._rawType()._name()) && Sets.immutable.with("Nil", "ColumnSpecification", "BasicColumnSpecification", "WindowColumnSpecification").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::tds::renameColumns_TabularDataSet_1__Pair_MANY__TabularDataSet_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "TabularDataSet", "TabularDataSetImplementation", "TableTDS").contains(ps.get(0)._genericType()._rawType()._name()) && Sets.immutable.with("Nil", "Pair", "StringFunctionPair", "OldAliasToNewAlias", "StringFunctionPair", "PureFunctionToRelationalFunctionPair", "DispatchPair").contains(ps.get(1)._genericType()._rawType()._name()));
        map.put("meta::pure::tds::restrict_TabularDataSet_1__String_MANY__TabularDataSet_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "TabularDataSet", "TabularDataSetImplementation", "TableTDS").contains(ps.get(0)._genericType()._rawType()._name()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::tds::restrictDistinct_TabularDataSet_1__String_MANY__TabularDataSet_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "TabularDataSet", "TabularDataSetImplementation", "TableTDS").contains(ps.get(0)._genericType()._rawType()._name()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::tds::slice_TabularDataSet_1__Integer_1__Integer_1__TabularDataSet_1_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "TabularDataSet", "TabularDataSetImplementation", "TableTDS").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Integer".equals(ps.get(1)._genericType()._rawType()._name())) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "Integer".equals(ps.get(2)._genericType()._rawType()._name())));
        map.put("meta::pure::tds::sort_TabularDataSet_1__SortInformation_MANY__TabularDataSet_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "TabularDataSet", "TabularDataSetImplementation", "TableTDS").contains(ps.get(0)._genericType()._rawType()._name()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "SortInformation".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::tds::sort_TabularDataSet_1__String_1__SortDirection_1__TabularDataSet_1_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "TabularDataSet", "TabularDataSetImplementation", "TableTDS").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())) && isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "SortDirection".equals(ps.get(2)._genericType()._rawType()._name())));
        map.put("meta::pure::tds::sort_TabularDataSet_1__String_MANY__TabularDataSet_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "TabularDataSet", "TabularDataSetImplementation", "TableTDS").contains(ps.get(0)._genericType()._rawType()._name()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::tds::tableToTDS_Table_1__TableTDS_1_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "Table", "ViewSelectSQLQuery", "VarCrossSetPlaceHolder", "LakeTable", "LakeViewSelectSQLQuery").contains(ps.get(0)._genericType()._rawType()._name()));
        map.put("meta::pure::tds::take_TabularDataSet_1__Integer_1__TabularDataSet_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "TabularDataSet", "TabularDataSetImplementation", "TableTDS").contains(ps.get(0)._genericType()._rawType()._name()) && isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "Integer".equals(ps.get(1)._genericType()._rawType()._name())));
        map.put("meta::pure::tds::tdsContains_T_1__Function_MANY__String_MANY__TabularDataSet_1__Function_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 5 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || check(funcType(ps.get(1)._genericType()), (FunctionType ft) -> matchZeroOne(ft._returnMultiplicity()) && check(ft._parameters().toList(), (List<? extends VariableExpression> nps) -> nps.size() == 1 && isOne(nps.get(0)._multiplicity())))) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "String".equals(ps.get(2)._genericType()._rawType()._name())) && isOne(ps.get(3)._multiplicity()) && Sets.immutable.with("Nil", "TabularDataSet", "TabularDataSetImplementation", "TableTDS").contains(ps.get(3)._genericType()._rawType()._name()) && isOne(ps.get(4)._multiplicity()) && ("Nil".equals(ps.get(4)._genericType()._rawType()._name()) || check(funcType(ps.get(4)._genericType()), (FunctionType ft) -> isOne(ft._returnMultiplicity()) && ("Nil".equals(ft._returnType()._rawType()._name()) || "Boolean".equals(ft._returnType()._rawType()._name())) && check(ft._parameters().toList(), (List<? extends VariableExpression> nps) -> nps.size() == 2 && isOne(nps.get(0)._multiplicity()) && Sets.immutable.with("TDSRow", "Any").contains(nps.get(0)._genericType()._rawType()._name()) && isOne(nps.get(1)._multiplicity()) && Sets.immutable.with("TDSRow", "Any").contains(nps.get(1)._genericType()._rawType()._name())))));
        map.put("meta::pure::tds::tdsContains_T_1__Function_MANY__TabularDataSet_1__Boolean_1_", (List<ValueSpecification> ps) -> ps.size() == 3 && isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || check(funcType(ps.get(1)._genericType()), (FunctionType ft) -> matchZeroOne(ft._returnMultiplicity()) && check(ft._parameters().toList(), (List<? extends VariableExpression> nps) -> nps.size() == 1 && isOne(nps.get(0)._multiplicity())))) && isOne(ps.get(2)._multiplicity()) && Sets.immutable.with("Nil", "TabularDataSet", "TabularDataSetImplementation", "TableTDS").contains(ps.get(2)._genericType()._rawType()._name()));
        map.put("meta::pure::tds::tdsRows_TabularDataSet_1__TDSRow_MANY_", (List<ValueSpecification> ps) -> ps.size() == 1 && isOne(ps.get(0)._multiplicity()) && Sets.immutable.with("Nil", "TabularDataSet", "TabularDataSetImplementation", "TableTDS").contains(ps.get(0)._genericType()._rawType()._name()));

        // ------------------------------------------------------------------------------------------------
        // Please do not update the following code manually! Please check with the team when introducing
        // new matchers as this might be complicated by modularization
        // THIS CODE IS GENERATED BY A FUNCTION IN PURE - CONTACT THE CORE TEAM FOR MORE DETAILS
        //-------------------------------------------------------------------------------------------------

        ListIterate.flatCollect(context.getCompilerExtensions().getExtraFunctionHandlerDispatchBuilderInfoCollectors(), collector -> collector.valueOf(this)).forEach(info -> map.put(info.functionName, info.dispatch));
        return map;
    }
}
