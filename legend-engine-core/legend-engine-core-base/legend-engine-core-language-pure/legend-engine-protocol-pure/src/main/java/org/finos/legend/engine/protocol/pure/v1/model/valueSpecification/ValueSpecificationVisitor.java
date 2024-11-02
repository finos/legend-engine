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

package org.finos.legend.engine.protocol.pure.v1.model.valueSpecification;

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

public interface ValueSpecificationVisitor<T>
{
    T visit(ValueSpecification valueSpecification);

    T visit(PackageableElementPtr packageableElementPtr);

    T visit(ClassInstance iv);

    T visit(CString cString);

    T visit(CDateTime cDateTime);

    T visit(CLatestDate cLatestDate);

    T visit(CStrictDate cStrictDate);

    T visit(CStrictTime cStrictTime);

    T visit(CBoolean cBoolean);

    T visit(EnumValue enumValue);

    T visit(CInteger cInteger);

    T visit(CDecimal cDecimal);

    T visit(CByteArray cByteArray);

    T visit(Lambda lambda);

    T visit(Variable variable);

    T visit(CFloat cFloat);

    T visit(GenericTypeInstance genericTypeInstance);

    T visit(Collection collection);

    T visit(AppliedFunction appliedFunction);

    T visit(AppliedProperty appliedProperty);

    T visit(UnitInstance unitInstance);

    T visit(KeyExpression keyExpression);

    T visit(HackedUnit hackedUnit);

    T visit(AppliedQualifiedProperty appliedQualifiedProperty);

    T visit(UnitType unitType);

    T visit(PrimitiveType primitiveType);

    T visit(Whatever whatever);

    T visit(MappingInstance mappingInstance);

    T visit(Class aClass);

    T visit(UnknownAppliedFunction unknownAppliedFunction);

    T visit(Enum anEnum);
}
