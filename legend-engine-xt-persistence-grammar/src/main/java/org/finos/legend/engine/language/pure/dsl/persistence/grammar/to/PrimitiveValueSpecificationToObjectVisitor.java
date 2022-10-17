// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.language.pure.dsl.persistence.grammar.to;

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecificationVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.Variable;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.AppliedFunction;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.AppliedProperty;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.AppliedQualifiedProperty;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.UnknownAppliedFunction;
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

public class PrimitiveValueSpecificationToObjectVisitor implements ValueSpecificationVisitor<Object>
{
    @Override
    public Object visit(org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification valueSpecification)
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
        throw new UnsupportedOperationException("Unsupported value specification type");
    }

    @Override
    public Object visit(ClassInstance iv)
    {
        throw new UnsupportedOperationException("Unsupported value specification type");
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
    public Object visit(Lambda lambda)
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
