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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
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

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = PackageableElementPtr.class, name = "packageableElementPtr"),
        @JsonSubTypes.Type(value = HackedClass.class, name = "hackedClass"),
        @JsonSubTypes.Type(value = EnumValue.class, name = "enumValue"),
        @JsonSubTypes.Type(value = Variable.class, name = "var"),
        @JsonSubTypes.Type(value = Lambda.class, name = "lambda"),
        @JsonSubTypes.Type(value = Path.class, name = "path"),
        @JsonSubTypes.Type(value = AppliedFunction.class, name = "func"),
        @JsonSubTypes.Type(value = AppliedProperty.class, name = "property"),
        @JsonSubTypes.Type(value = Collection.class, name = "collection"),
        @JsonSubTypes.Type(value = CInteger.class, name = "integer"),
        @JsonSubTypes.Type(value = CDecimal.class, name = "decimal"),
        @JsonSubTypes.Type(value = CString.class, name = "string"),
        @JsonSubTypes.Type(value = CBoolean.class, name = "boolean"),
        @JsonSubTypes.Type(value = CFloat.class, name = "float"),
        @JsonSubTypes.Type(value = CDateTime.class, name = "dateTime"),
        @JsonSubTypes.Type(value = CStrictDate.class, name = "strictDate"),
        @JsonSubTypes.Type(value = CStrictTime.class, name = "strictTime"),
        @JsonSubTypes.Type(value = CLatestDate.class, name = "latestDate"),

        @JsonSubTypes.Type(value = AggregateValue.class, name = "aggregateValue"),
        @JsonSubTypes.Type(value = Pair.class, name = "pair"),
        @JsonSubTypes.Type(value = RuntimeInstance.class, name = "runtimeInstance"),
        @JsonSubTypes.Type(value = ExecutionContextInstance.class, name = "executionContextInstance"),
        @JsonSubTypes.Type(value = PureList.class, name = "listInstance"),
        @JsonSubTypes.Type(value = RootGraphFetchTree.class, name = "rootGraphFetchTree"),
        @JsonSubTypes.Type(value = PropertyGraphFetchTree.class, name = "propertyGraphFetchTree"),
        @JsonSubTypes.Type(value = SerializationConfig.class, name = "alloySerializationConfig"),
        @JsonSubTypes.Type(value = UnitType.class, name = "unitType"),
        @JsonSubTypes.Type(value = UnitInstance.class, name = "unitInstance"),
        @JsonSubTypes.Type(value = KeyExpression.class, name = "keyExpression"),
        @JsonSubTypes.Type(value = PrimitiveType.class, name = "primitiveType"),

        // TDS (to be moved)?
        @JsonSubTypes.Type(value = TDSAggregateValue.class, name = "tdsAggregateValue"),
        @JsonSubTypes.Type(value = TDSColumnInformation.class, name = "tdsColumnInformation"),
        @JsonSubTypes.Type(value = TDSSortInformation.class, name = "tdsSortInformation"),
        @JsonSubTypes.Type(value = TdsOlapRank.class, name = "tdsOlapRank"),
        @JsonSubTypes.Type(value = TdsOlapAggregation.class, name = "tdsOlapAggregation"),

        // TO BE DELETED
        @JsonSubTypes.Type(value = MappingInstance.class, name = "mappingInstance"),
        @JsonSubTypes.Type(value = AppliedQualifiedProperty.class, name = "qualifiedProperty"),
        @JsonSubTypes.Type(value = HackedUnit.class, name = "hackedUnit"),
        @JsonSubTypes.Type(value = Whatever.class, name = "whatever"),
        @JsonSubTypes.Type(value = UnknownAppliedFunction.class, name = "unknownFunc"),
        @JsonSubTypes.Type(value = Class.class, name = "class"),
        @JsonSubTypes.Type(value = Enum.class, name = "enum")
})
// NOTE: due to plan generator producing duplicated _type field, we need to enable this
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class ValueSpecification
{
    public SourceInformation sourceInformation;

    public abstract <T> T accept(ValueSpecificationVisitor<T> visitor);
}
