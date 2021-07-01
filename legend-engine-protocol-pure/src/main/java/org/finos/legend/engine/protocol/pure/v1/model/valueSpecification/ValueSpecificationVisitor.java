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
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.AppliedQualifiedProperty;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.UnknownAppliedFunction;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.*;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Class;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Enum;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.graph.PropertyGraphFetchTree;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.graph.RootGraphFetchTree;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.path.Path;

public interface ValueSpecificationVisitor<T>
{
    T visit(ValueSpecification valueSpecification);

    T visit(PackageableElementPtr packageableElementPtr);

    T visit(Whatever whatever);

    T visit(CString cString);

    T visit(CDateTime cDateTime);

    T visit(CLatestDate cLatestDate);

    T visit(CStrictDate cStrictDate);

    T visit(CStrictTime cStrictTime);

    T visit(AggregateValue aggregateValue);

    T visit(Class aClass);

    T visit(CBoolean cBoolean);

    T visit(UnknownAppliedFunction unknownAppliedFunction);

    T visit(Enum anEnum);

    T visit(EnumValue enumValue);

    T visit(RuntimeInstance runtimeInstance);

    T visit(Path path);

    T visit(CInteger cInteger);

    T visit(CDecimal cDecimal);

    T visit(Lambda lambda);

    T visit(ExecutionContextInstance executionContextInstance);

    T visit(Pair pair);

    T visit(PureList pureList);

    T visit(Variable variable);

    T visit(CFloat cFloat);

    T visit(MappingInstance mappingInstance);

    T visit(HackedClass hackedClass);

    T visit(Collection collection);

    T visit(AppliedFunction appliedFunction);

    T visit(AppliedQualifiedProperty appliedQualifiedProperty);

    T visit(PropertyGraphFetchTree propertyGraphFetchTree);

    T visit(RootGraphFetchTree rootGraphFetchTree);

    T visit(SerializationConfig serializationConfig);

    T visit(AppliedProperty appliedProperty);

    T visit(TdsOlapAggregation tdsOlapAggregation);

    T visit(TDSAggregateValue tdsAggregateValue);

    T visit(TDSSortInformation tdsSortInformation);

    T visit(TDSColumnInformation tdsColumnInformation);

    T visit(TdsOlapRank tdsOlapRank);

    T visit(HackedUnit hackedUnit);

    T visit(UnitInstance unitInstance);

    T visit(UnitType unitType);

    T visit(KeyExpression keyExpression);

    T visit(PrimitiveType primitiveType);

}
