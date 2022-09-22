//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.test.runner.service;

import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecificationVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.Variable;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.AppliedFunction;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.AppliedProperty;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.AppliedQualifiedProperty;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.UnknownAppliedFunction;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.AggregateValue;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CBoolean;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CDateTime;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CDecimal;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CFloat;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CInteger;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CLatestDate;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CStrictDate;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CStrictTime;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CString;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Class;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Collection;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Enum;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.EnumValue;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.ExecutionContextInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.HackedClass;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.HackedUnit;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.KeyExpression;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.MappingInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.PackageableElementPtr;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Pair;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.PrimitiveType;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.PureList;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.RuntimeInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.SerializationConfig;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.TDSAggregateValue;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.TDSColumnInformation;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.TDSSortInformation;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.TdsOlapAggregation;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.TdsOlapRank;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.UnitInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.UnitType;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Whatever;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.graph.PropertyGraphFetchTree;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.graph.RootGraphFetchTree;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.path.Path;

public class ValueSpecificationToResultVisitor implements ValueSpecificationVisitor<ConstantResult>
{
    @Override
    public ConstantResult visit(org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification valueSpecification)
    {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(PackageableElementPtr packageableElementPtr)
    {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(Whatever whatever)
    {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(CString cString)
    {
        return new ConstantResult(cString.values.get(0));
    }

    @Override
    public ConstantResult visit(CDateTime cDateTime)
    {
        return new ConstantResult(cDateTime.values.get(0));
    }

    @Override
    public ConstantResult visit(CLatestDate cLatestDate)
    {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(CStrictDate cStrictDate)
    {
        return new ConstantResult(cStrictDate.values.get(0));
    }

    @Override
    public ConstantResult visit(CStrictTime cStrictTime)
    {
        return new ConstantResult(cStrictTime.values.get(0));
    }

    @Override
    public ConstantResult visit(AggregateValue aggregateValue)
    {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(Class aClass)
    {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(CBoolean cBoolean)
    {
        return new ConstantResult(cBoolean.values.get(0));
    }

    @Override
    public ConstantResult visit(UnknownAppliedFunction unknownAppliedFunction)
    {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(Enum anEnum)
    {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(EnumValue enumValue)
    {
        return new ConstantResult(enumValue.value);
    }

    @Override
    public ConstantResult visit(RuntimeInstance runtimeInstance)
    {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(Path path)
    {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(CInteger cInteger)
    {
        return new ConstantResult(cInteger.values.get(0));
    }

    @Override
    public ConstantResult visit(CDecimal cDecimal)
    {
        return new ConstantResult(cDecimal.values.get(0));
    }

    @Override
    public ConstantResult visit(Lambda lambda)
    {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(ExecutionContextInstance executionContextInstance)
    {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(Pair pair)
    {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(PureList pureList)
    {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(Variable variable)
    {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(CFloat cFloat)
    {
        return new ConstantResult(cFloat.values.get(0));
    }

    @Override
    public ConstantResult visit(MappingInstance mappingInstance)
    {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(HackedClass hackedClass)
    {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(Collection collection)
    {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(AppliedFunction appliedFunction)
    {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(AppliedQualifiedProperty appliedQualifiedProperty)
    {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(PropertyGraphFetchTree propertyGraphFetchTree)
    {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(RootGraphFetchTree rootGraphFetchTree)
    {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(SerializationConfig serializationConfig)
    {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(AppliedProperty appliedProperty)
    {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(TdsOlapAggregation tdsOlapAggregation)
    {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(TDSAggregateValue tdsAggregateValue)
    {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(TDSSortInformation tdsSortInformation)
    {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(TDSColumnInformation tdsColumnInformation)
    {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(TdsOlapRank tdsOlapRank)
    {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(HackedUnit hackedUnit)
    {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(UnitInstance unitInstance)
    {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(UnitType unitType)
    {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(KeyExpression keyExpression)
    {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(PrimitiveType primitiveType)
    {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }
}
