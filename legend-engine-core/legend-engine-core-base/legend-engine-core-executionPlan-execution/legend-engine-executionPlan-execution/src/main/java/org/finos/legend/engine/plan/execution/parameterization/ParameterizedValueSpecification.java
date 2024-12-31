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

package org.finos.legend.engine.plan.execution.parameterization;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.ParameterValue;
import org.finos.legend.engine.protocol.pure.v1.model.domain.Multiplicity;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecificationVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.Variable;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.AppliedFunction;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.AppliedProperty;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.AppliedQualifiedProperty;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.UnknownAppliedFunction;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.datatype.CBoolean;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.datatype.CByteArray;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.datatype.CDateTime;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.datatype.CDecimal;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.datatype.CFloat;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.datatype.CInteger;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.datatype.CLatestDate;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.datatype.CStrictDate;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.datatype.CStrictTime;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.datatype.CString;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.Class;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.ClassInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Collection;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.Enum;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.datatype.EnumValue;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.packageableElement.GenericTypeInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.HackedUnit;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.KeyExpression;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.MappingInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.packageableElement.PackageableElementPtr;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.PrimitiveType;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.datatype.UnitInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.UnitType;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.Whatever;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ParameterizedValueSpecification
{
    private final List<ParameterValue> parameterValues;  //map of the extracted values
    private final ValueSpecification valueSpecification;
    private final List<Variable> variables;
    private final Map<String, BiFunction<AppliedFunction, String, List<ValueSpecification>>> parameterHandler = parameterHandler();
    private final Map<java.lang.Class, String> primitiveHelper = primitiveTypeHelper();

    public ParameterizedValueSpecification(ValueSpecification valueSpecification, String parameterSeed)
    {
        this.parameterValues = new ArrayList<>();
        this.variables = new ArrayList<>();
        this.valueSpecification = parameterizedValueSpec(valueSpecification, parameterSeed, false, null);
    }


    public List<Variable> getVariables()
    {
        return variables;
    }

    public List<ParameterValue> getParameterValues()
    {
        return parameterValues;
    }

    public ValueSpecification getValueSpecification()
    {
        return valueSpecification;
    }


    private Map<String, BiFunction<AppliedFunction, String, List<ValueSpecification>>> parameterHandler()

    {
        Map<String, BiFunction<AppliedFunction, String, List<ValueSpecification>>> parameterHelper = new HashMap<>();
        parameterHelper.put("filter", (fn, parameterName) -> valueSpecificationListHelper(fn.parameters, parameterName, true, fn.function));
        parameterHelper.put("take", (fn, parameterName) -> valueSpecificationListHelper(fn.parameters, parameterName, true, fn.function));
        parameterHelper.put("getAll", (fn, parameterName) -> valueSpecificationListHelper(fn.parameters, parameterName, true, fn.function));
        parameterHelper.put("limit", (fn, parameterName) -> valueSpecificationListHelper(fn.parameters, parameterName, true, fn.function));

        parameterHelper.put("in", (fn, parameterName) -> valueSpecificationListHelper(fn.parameters, parameterName, false, fn.function));
        parameterHelper.put("col", (fn, parameterName) -> valueSpecificationListHelper(fn.parameters, parameterName, false, fn.function));
        parameterHelper.put("project", (fn, parameterName) -> valueSpecificationListHelper(fn.parameters, parameterName, false, fn.function));
        parameterHelper.put("groupBy", (fn, parameterName) -> valueSpecificationListHelper(fn.parameters, parameterName, false, fn.function));
        parameterHelper.put("serialize", (fn, parameterName) -> valueSpecificationListHelper(fn.parameters, parameterName, false, fn.function));
        parameterHelper.put("graphFetch", (fn, parameterName) -> valueSpecificationListHelper(fn.parameters, parameterName, false, fn.function));
        parameterHelper.put("sort", (fn, parameterName) -> valueSpecificationListHelper(fn.parameters, parameterName, false, fn.function));
        parameterHelper.put("tdsContains", (fn, parameterName) -> valueSpecificationListHelper(fn.parameters, parameterName, false, fn.function));
        parameterHelper.put("restrict", (fn, parameterName) -> valueSpecificationListHelper(fn.parameters, parameterName, false, fn.function));
        parameterHelper.put("join", (fn, parameterName) -> valueSpecificationListHelper(fn.parameters, parameterName, false, fn.function));
        parameterHelper.put("objectReferenceIn", (fn, parameterName) -> valueSpecificationListHelper(fn.parameters, parameterName, false, fn.function));
        parameterHelper.put("restrictDistinct", (fn, parameterName) -> valueSpecificationListHelper(fn.parameters, parameterName, false, fn.function));
        parameterHelper.put("projectWithColumnSubset", (fn, parameterName) -> valueSpecificationListHelper(fn.parameters, parameterName, false, fn.function));
        parameterHelper.put("groupByWithWindowSubset", (fn, parameterName) -> valueSpecificationListHelper(fn.parameters, parameterName, false, fn.function));
        parameterHelper.put("olapGroupBy", (fn, parameterName) -> valueSpecificationListHelper(fn.parameters, parameterName, false, fn.function));


        return parameterHelper;
    }

    public Map<java.lang.Class, String> primitiveTypeHelper()

    {
        Map<java.lang.Class, String> map = new HashMap<>();
        map.put(CString.class, "String");
        map.put(CDateTime.class, "DateTime");
        map.put(CStrictDate.class, "StrictDate");
        map.put(CStrictTime.class, "StrictTime");
        map.put(CBoolean.class, "Boolean");
        map.put(CInteger.class, "Integer");
        map.put(CDecimal.class, "Decimal");
        map.put(CByteArray.class, "ByteArray");
        map.put(CFloat.class, "Float");

        return map;
    }

    private ValueSpecification parameterizedValueSpec(ValueSpecification valueSpecification, String parameter, Boolean inScopeRoot, String callingFunction)
    {
        return valueSpecification.accept(new ValueSpecificationVisitor<ValueSpecification>()
        {
            @Override
            public ValueSpecification visit(ValueSpecification valueSpecification)
            {
                return parameterizedValueSpec(valueSpecification, parameter + "L", true, callingFunction);
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
            }

            @Override
            public ValueSpecification visit(CString cString)
            {
                return primitiveHelper(parameter, cString, "String", inScopeRoot);

            }

            @Override
            public ValueSpecification visit(CDateTime cDateTime)
            {
                return primitiveHelper(parameter, cDateTime, "DateTime", inScopeRoot);

            }

            @Override
            public ValueSpecification visit(CLatestDate cLatestDate)
            {
                return cLatestDate;
            }

            @Override
            public ValueSpecification visit(CStrictDate cStrictDate)
            {
                return primitiveHelper(parameter, cStrictDate, "StrictDate", inScopeRoot);
            }

            @Override
            public ValueSpecification visit(CStrictTime cStrictTime)
            {
                return primitiveHelper(parameter, cStrictTime, "StrictTime", inScopeRoot);
            }

            @Override
            public ValueSpecification visit(CBoolean cBoolean)
            {
                return primitiveHelper(parameter, cBoolean, "Boolean", inScopeRoot);
            }

            @Override
            public ValueSpecification visit(EnumValue enumValue)
            {
                return enumValue;  //TODO: see what we we need here
            }

            @Override
            public ValueSpecification visit(CInteger cInteger)
            {
                return primitiveHelper(parameter, cInteger, "Integer", inScopeRoot);
            }

            @Override
            public ValueSpecification visit(CDecimal cDecimal)
            {
                return primitiveHelper(parameter, cDecimal, "Decimal", inScopeRoot);
            }

            @Override
            public ValueSpecification visit(CByteArray cByteArray)
            {
                return primitiveHelper(parameter, cByteArray, "ByteArray", inScopeRoot);
            }

            @Override
            public ValueSpecification visit(CFloat cFloat)
            {
                return primitiveHelper(parameter, cFloat, "Float", inScopeRoot);
            }

            @Override
            public ValueSpecification visit(Lambda lambda)
            {
                lambda.body = valueSpecificationListHelper(lambda.body, parameter, inScopeRoot, callingFunction);
                return lambda;
            }

            @Override
            public ValueSpecification visit(Variable variable)
            {
                return variable;
            }


            @Override
            public ValueSpecification visit(GenericTypeInstance genericTypeInstance)
            {
                return genericTypeInstance;
            }

            @Override
            public ValueSpecification visit(Collection collection)
            {
                collection.values = valueSpecificationListHelper(collection.values, parameter, inScopeRoot, callingFunction);
                return collection;
            }


            @Override
            public ValueSpecification visit(AppliedFunction appliedFunction)
            {
                appliedFunction.parameters = parameterHandler.getOrDefault(appliedFunction.function, (fn, parameterName) -> valueSpecificationListHelper(fn.parameters, parameterName, inScopeRoot, fn.function)).apply(appliedFunction, parameter);
                return appliedFunction;
            }

            @Override
            public ValueSpecification visit(AppliedProperty appliedProperty)
            {
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


    private ValueSpecification primitiveHelper(String name, ValueSpecification value, String type, Boolean inScopeRoot)
    {
        if (inScopeRoot)
        {
            Variable variable = new Variable(name, type, Multiplicity.PURE_ONE);

            variables.add(variable);
            ParameterValue parameterValue = new ParameterValue();
            parameterValue.name = name;
            parameterValue.value = value;
            parameterValues.add(parameterValue);
            return variable;
        }
        else
        {
            return value;
        }
    }

    private ValueSpecification ManyParam(String name, ValueSpecification specification)
    {
        if (specification instanceof Collection)
        {
            String type = this.primitiveHelper.getOrDefault(((Collection) specification).values.get(0).getClass(), null);
            if (type != null)
            {
                Variable variable = new Variable(name, type, new Multiplicity(0, null));
                variables.add(variable);
                ParameterValue parameterValue = new ParameterValue();
                parameterValue.name = name;
                parameterValue.value = specification;
                parameterValues.add(parameterValue);
                return variable;
            }
            else
            {
                return specification;
            }
        }
        else
        {
            return specification;
        }
    }

    private List<ValueSpecification> valueSpecificationListHelper(List<ValueSpecification> valueSpecificationList, String parameter, Boolean inScopeRoot, String callingFunction)
    {

        return IntStream.range(0, valueSpecificationList.size())
                .mapToObj(index -> parameterizedValueSpec(valueSpecificationList.get(index), parameter + "L" + index, inScopeRoot, callingFunction))
                .collect(Collectors.toList());
    }

}

