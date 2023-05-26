// Copyright 2023 Goldman Sachs
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


package org.finos.legend.engine.plan.execution.planCaching;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
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
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.PackageableElementPtr;
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

public class CachableValueSpecification
{
    private HashMap<String, Object> parameterMap;  //map of the extracted values
    private ValueSpecification valueSpecification;

    public CachableValueSpecification(ValueSpecification valueSpecification, String parameterSeed)
    {
        this.valueSpecification = cachableValueSpec(valueSpecification,parameterSeed);
    }

    public HashMap<String, Object> getParameterMap()
    {
        return parameterMap;
    }

    public ValueSpecification getValueSpecification()
    {
        return valueSpecification;
    }

    private ValueSpecification cachableValueSpec(ValueSpecification valueSpecification, String parameter)
    {
        return valueSpecification.accept(new ValueSpecificationVisitor<ValueSpecification>()
        {
            @Override
            public ValueSpecification visit(ValueSpecification valueSpecification)
            {
                return cachableValueSpec(valueSpecification, parameter + "_");
            }

            @Override
            public ValueSpecification visit(PackageableElementPtr packageableElementPtr)
            {
                return packageableElementPtr;
            }

            @Override
            public ValueSpecification visit(ClassInstance iv)
            {
                return iv;

//                switch (iv.type)
 //               {
//                    case "path":
//                        return processClassInstance((Path) iv.value);
//                    case "rootGraphFetchTree":
//                        return processClassInstance((RootGraphFetchTree) iv.value);
//                    case "propertyGraphFetchTree":
//                        return processClassInstance((PropertyGraphFetchTree) iv.value);
//                    case "keyExpression":
//                        return visit((KeyExpression) iv.value);
//                    case "primitiveType":
//                        return visit((PrimitiveType) iv.value);
//                    case "listInstance":
//                        return processClassInstance((PureList) iv.value);
//                    case "aggregateValue":
//                        return processClassInstance((AggregateValue) iv.value);
//                    case "pair":
//                        return processClassInstance((Pair) iv.value);
//                    case "runtimeInstance":
//                        return processClassInstance((RuntimeInstance) iv.value);
//                    case "executionContextInstance":
//                        return processClassInstance((ExecutionContextInstance) iv.value);
//                    case "alloySerializationConfig":
//                        return processClassInstance((SerializationConfig) iv.value);
//                    case "tdsAggregateValue":
//                        return processClassInstance((TDSAggregateValue) iv.value);
//                    case "tdsColumnInformation":
//                        return processClassInstance((TDSColumnInformation) iv.value);
//                    case "tdsSortInformation":
//                        return processClassInstance((TDSSortInformation) iv.value);
//                    case "tdsOlapRank":
//                        return processClassInstance((TdsOlapRank) iv.value);
//                    case "tdsOlapAggregation":
//                        return processClassInstance((TdsOlapAggregation) iv.value);
//                    default:
//                        throw new RuntimeException("/* Unsupported instance value " + iv.type + " */");
  //              }

            }

            @Override
            public ValueSpecification visit(CString cString)
            {
                return primitiveHelper(parameter, cString.value);

            }

            @Override
            public ValueSpecification visit(CDateTime cDateTime)
            {
                return primitiveHelper(parameter, cDateTime.value);

            }

            @Override
            public ValueSpecification visit(CLatestDate cLatestDate)
            {
                return cLatestDate;
            }

            @Override
            public ValueSpecification visit(CStrictDate cStrictDate)
            {
                return primitiveHelper(parameter, cStrictDate.value);
            }

            @Override
            public ValueSpecification visit(CStrictTime cStrictTime)
            {
                return primitiveHelper(parameter, cStrictTime.value);
            }

            @Override
            public ValueSpecification visit(CBoolean cBoolean)
            {
                return primitiveHelper(parameter, cBoolean.value);
            }

            @Override
            public ValueSpecification visit(EnumValue enumValue)
            {
                return enumValue;  //TODO: see what we we need here
            }

            @Override
            public ValueSpecification visit(CInteger cInteger)
            {
                return primitiveHelper(parameter, cInteger.value);
            }

            @Override
            public ValueSpecification visit(CDecimal cDecimal)
            {
                return primitiveHelper(parameter, cDecimal.value);
            }

            @Override
            public ValueSpecification visit(CByteArray cByteArray)
            {
                return primitiveHelper(parameter, cByteArray.value);
            }

            @Override
            public ValueSpecification visit(Lambda lambda)
            {
                lambda.body = valueSpecificationListHelper(lambda.body, parameter);
                return lambda;
            }

            @Override
            public ValueSpecification visit(Variable variable)
            {
                return variable;
            }

            @Override
            public ValueSpecification visit(CFloat cFloat)
            {
                return primitiveHelper(parameter, cFloat.value);
            }

            @Override
            public ValueSpecification visit(GenericTypeInstance genericTypeInstance)
            {
                return genericTypeInstance;
            }

            @Override
            public ValueSpecification visit(Collection collection)
            {
                collection.values = valueSpecificationListHelper(collection.values, parameter);
                return collection;
            }

            @Override
            public ValueSpecification visit(AppliedFunction appliedFunction)
            {
                appliedFunction.parameters = valueSpecificationListHelper(appliedFunction.parameters, parameter);
                return appliedFunction;


            }

            @Override
            public ValueSpecification visit(AppliedProperty appliedProperty)
            {
                appliedProperty.parameters = valueSpecificationListHelper(appliedProperty.parameters, parameter);
                return appliedProperty;
            }

            @Override
            public ValueSpecification visit(UnitInstance unitInstance)
            {
                return unitInstance;
            }

            @Override
            public ValueSpecification visit(KeyExpression keyExpression)
            {
                keyExpression.expression = cachableValueSpec(keyExpression.expression, parameter + "_1");
                keyExpression.key = cachableValueSpec(keyExpression.key, parameter + "_2");
                return keyExpression;
            }

            @Override
            public ValueSpecification visit(HackedUnit hackedUnit)
            {
                return hackedUnit;
            }

            @Override
            public ValueSpecification visit(AppliedQualifiedProperty appliedQualifiedProperty)
            {
                appliedQualifiedProperty.parameters = valueSpecificationListHelper(appliedQualifiedProperty.parameters, parameter);
                return appliedQualifiedProperty;
            }

            @Override
            public ValueSpecification visit(UnitType unitType)
            {
                return unitType;
            }

            @Override
            public ValueSpecification visit(PrimitiveType primitiveType)
            {
                return primitiveType;
            }

            @Override
            public ValueSpecification visit(Whatever whatever)
            {
                return whatever;
            }

            @Override
            public ValueSpecification visit(MappingInstance mappingInstance)
            {
                return mappingInstance;
            }

            @Override
            public ValueSpecification visit(Class aClass)
            {
                return aClass;
            }

            @Override
            public ValueSpecification visit(UnknownAppliedFunction unknownAppliedFunction)
            {
                return unknownAppliedFunction;
            }

            @Override
            public ValueSpecification visit(Enum anEnum)
            {
                return anEnum;
            }


        });


    }

    private Variable primitiveHelper(String name, Object value)
    {
        parameterMap.put(name, value);
        return new Variable(name, null, null);
    }

    private List<ValueSpecification> valueSpecificationListHelper(List<ValueSpecification> valueSpecificationList, String parameter)
    {
        return IntStream.range(0, valueSpecificationList.size())
                .mapToObj(index -> cachableValueSpec(valueSpecificationList.get(index), parameter + "_" + index))
                .collect(Collectors.toList());
    }


}

