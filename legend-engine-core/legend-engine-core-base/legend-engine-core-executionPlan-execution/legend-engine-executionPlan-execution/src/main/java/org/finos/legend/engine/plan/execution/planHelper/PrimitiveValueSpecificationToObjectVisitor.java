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

package org.finos.legend.engine.plan.execution.planHelper;

import org.eclipse.collections.impl.utility.ListIterate;
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
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.PureList;

import java.io.ByteArrayInputStream;

public class PrimitiveValueSpecificationToObjectVisitor implements ValueSpecificationVisitor<Object>
{
    @Override
    public Object visit(ValueSpecification valueSpecification)
    {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public Object visit(PackageableElementPtr packageableElementPtr)
    {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public Object visit(Whatever whatever)
    {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public Object visit(CString cString)
    {
        return cString.value;
    }

    @Override
    public Object visit(CDateTime cDateTime)
    {
        return cDateTime.value;
    }

    @Override
    public Object visit(CLatestDate cLatestDate)
    {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public Object visit(CStrictDate cStrictDate)
    {
        return cStrictDate.value;
    }

    @Override
    public Object visit(CStrictTime cStrictTime)
    {
        return cStrictTime.value;
    }

    @Override
    public Object visit(Class aClass)
    {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public Object visit(CBoolean cBoolean)
    {
        return cBoolean.value;
    }

    @Override
    public Object visit(UnknownAppliedFunction unknownAppliedFunction)
    {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public Object visit(Enum anEnum)
    {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public Object visit(EnumValue enumValue)
    {
        return enumValue.value;
    }

    @Override
    public Object visit(ClassInstance iv)
    {
        switch (iv.type)
        {
            case "listInstance":
                return ListIterate.collect(((PureList) iv.value).values, x -> x.accept(this));
            case "path":
                return iv.value;                
            default:
                throw new UnsupportedOperationException("Unsupported ClassInstance type: " + iv.type);
        }
    }

    @Override
    public Object visit(CInteger cInteger)
    {
        return cInteger.value;
    }

    @Override
    public Object visit(CDecimal cDecimal)
    {
        return cDecimal.value;
    }

    @Override
    public Object visit(CByteArray cByteArray)
    {
        return new ByteArrayInputStream(cByteArray.value);
    }

    @Override
    public Object visit(LambdaFunction lambda)
    {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public Object visit(Variable variable)
    {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public Object visit(CFloat cFloat)
    {
        return cFloat.value;
    }

    @Override
    public Object visit(MappingInstance mappingInstance)
    {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public Object visit(GenericTypeInstance genericTypeInstance)
    {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public Object visit(Collection collection)
    {
        return ListIterate.collect(collection.values, v -> v.accept(this));
    }

    @Override
    public Object visit(AppliedFunction appliedFunction)
    {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public Object visit(AppliedQualifiedProperty appliedQualifiedProperty)
    {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public Object visit(AppliedProperty appliedProperty)
    {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public Object visit(HackedUnit hackedUnit)
    {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public Object visit(UnitInstance unitInstance)
    {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public Object visit(UnitType unitType)
    {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public Object visit(KeyExpression keyExpression)
    {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public Object visit(PrimitiveType primitiveType)
    {
        throw new UnsupportedOperationException("Unsupported value specification type");
    }
}
