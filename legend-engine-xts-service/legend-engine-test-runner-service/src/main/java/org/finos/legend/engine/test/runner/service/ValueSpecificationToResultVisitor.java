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
import org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecification;
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
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.ClassInstance;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.Collection;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.Enum;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.EnumValue;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.GenericTypeInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.HackedUnit;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.KeyExpression;
import org.finos.legend.engine.protocol.pure.m3.function.Lambda;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.MappingInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.packageableElement.PackageableElementPtr;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.PrimitiveType;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.UnitInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.UnitType;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.Whatever;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.PureList;

public class ValueSpecificationToResultVisitor implements ValueSpecificationVisitor<ConstantResult>
{
    @Override
    public ConstantResult visit(ValueSpecification valueSpecification)
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
        return new ConstantResult(cString.value);
    }

    @Override
    public ConstantResult visit(CDateTime cDateTime)
    {
        return new ConstantResult(cDateTime.value);
    }

    @Override
    public ConstantResult visit(CLatestDate cLatestDate)
    {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(CStrictDate cStrictDate)
    {
        return new ConstantResult(cStrictDate.value);
    }

    @Override
    public ConstantResult visit(CStrictTime cStrictTime)
    {
        return new ConstantResult(cStrictTime.value);
    }

    @Override
    public ConstantResult visit(Class aClass)
    {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(CBoolean cBoolean)
    {
        return new ConstantResult(cBoolean.value);
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
    public ConstantResult visit(ClassInstance iv)
    {
        System.out.println(((CString)((PureList)iv.value).values.get(1)).value);
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(CInteger cInteger)
    {
        return new ConstantResult(cInteger.value);
    }

    @Override
    public ConstantResult visit(CDecimal cDecimal)
    {
        return new ConstantResult(cDecimal.value);
    }

    @Override
    public ConstantResult visit(CByteArray cByteArray)
    {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(Lambda lambda)
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
        return new ConstantResult(cFloat.value);
    }

    @Override
    public ConstantResult visit(MappingInstance mappingInstance)
    {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public ConstantResult visit(GenericTypeInstance genericTypeInstance)
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
    public ConstantResult visit(AppliedProperty appliedProperty)
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
