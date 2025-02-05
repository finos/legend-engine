// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.repl.autocomplete;

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

public class ValueSpecificationDefaultVisitor<T> implements ValueSpecificationVisitor<T>
{
    @Override
    public T visit(ValueSpecification valueSpecification)
    {
        return null;
    }

    @Override
    public T visit(PackageableElementPtr packageableElementPtr)
    {
        return null;
    }

    @Override
    public T visit(ClassInstance iv)
    {
        return null;
    }

    @Override
    public T visit(CString cString)
    {
        return null;
    }

    @Override
    public T visit(CDateTime cDateTime)
    {
        return null;
    }

    @Override
    public T visit(CLatestDate cLatestDate)
    {
        return null;
    }

    @Override
    public T visit(CStrictDate cStrictDate)
    {
        return null;
    }

    @Override
    public T visit(CStrictTime cStrictTime)
    {
        return null;
    }

    @Override
    public T visit(CBoolean cBoolean)
    {
        return null;
    }

    @Override
    public T visit(EnumValue enumValue)
    {
        return null;
    }

    @Override
    public T visit(CInteger cInteger)
    {
        return null;
    }

    @Override
    public T visit(CDecimal cDecimal)
    {
        return null;
    }

    @Override
    public T visit(CByteArray cByteArray)
    {
        return null;
    }

    @Override
    public T visit(Lambda lambda)
    {
        return null;
    }

    @Override
    public T visit(Variable variable)
    {
        return null;
    }

    @Override
    public T visit(CFloat cFloat)
    {
        return null;
    }

    @Override
    public T visit(GenericTypeInstance genericTypeInstance)
    {
        return null;
    }

    @Override
    public T visit(Collection collection)
    {
        return null;
    }

    @Override
    public T visit(AppliedFunction appliedFunction)
    {
        return null;
    }

    @Override
    public T visit(AppliedProperty appliedProperty)
    {
        return null;
    }

    @Override
    public T visit(UnitInstance unitInstance)
    {
        return null;
    }

    @Override
    public T visit(KeyExpression keyExpression)
    {
        return null;
    }

    @Override
    public T visit(HackedUnit hackedUnit)
    {
        return null;
    }

    @Override
    public T visit(AppliedQualifiedProperty appliedQualifiedProperty)
    {
        return null;
    }

    @Override
    public T visit(UnitType unitType)
    {
        return null;
    }

    @Override
    public T visit(PrimitiveType primitiveType)
    {
        return null;
    }

    @Override
    public T visit(Whatever whatever)
    {
        return null;
    }

    @Override
    public T visit(MappingInstance mappingInstance)
    {
        return null;
    }

    @Override
    public T visit(Class aClass)
    {
        return null;
    }

    @Override
    public T visit(UnknownAppliedFunction unknownAppliedFunction)
    {
        return null;
    }

    @Override
    public T visit(Enum anEnum)
    {
        return null;
    }
}
