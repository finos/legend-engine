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
