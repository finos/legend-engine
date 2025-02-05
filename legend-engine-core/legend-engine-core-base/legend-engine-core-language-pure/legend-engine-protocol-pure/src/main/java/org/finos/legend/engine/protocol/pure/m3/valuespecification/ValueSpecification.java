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

package org.finos.legend.engine.protocol.pure.m3.valuespecification;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ClassInstanceWrapper;
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
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.Enum;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.EnumValue;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.GenericTypeInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.HackedClass;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.HackedUnit;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.KeyExpression;
import org.finos.legend.engine.protocol.pure.m3.function.Lambda;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.MappingInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.packageableElement.PackageableElementPtr;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.PrimitiveType;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.UnitInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.UnitType;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.Whatever;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type")
@JsonSubTypes({
        // Collection
        @JsonSubTypes.Type(value = Collection.class, name = "collection"),
        // Applied Function
        @JsonSubTypes.Type(value = AppliedFunction.class, name = "func"),
        @JsonSubTypes.Type(value = AppliedProperty.class, name = "property"),
        // Packageable Element Ptr
        @JsonSubTypes.Type(value = PackageableElementPtr.class, name = "packageableElementPtr"),
        @JsonSubTypes.Type(value = GenericTypeInstance.class, name = "genericTypeInstance"),
        // Variable
        @JsonSubTypes.Type(value = Variable.class, name = "var"),
        // Core Class Instances (could be moved to classInstance)
        @JsonSubTypes.Type(value = Lambda.class, name = "lambda"),
        @JsonSubTypes.Type(value = KeyExpression.class, name = "keyExpression"), // Used for new
        // Data Type - Enumeration
        @JsonSubTypes.Type(value = EnumValue.class, name = "enumValue"),
        // Data Type - Unit
        @JsonSubTypes.Type(value = UnitInstance.class, name = "unitInstance"),
        // Data Type - Primitives
        @JsonSubTypes.Type(value = CInteger.class, name = "integer"),
        @JsonSubTypes.Type(value = CDecimal.class, name = "decimal"),
        @JsonSubTypes.Type(value = CString.class, name = "string"),
        @JsonSubTypes.Type(value = CBoolean.class, name = "boolean"),
        @JsonSubTypes.Type(value = CFloat.class, name = "float"),
        @JsonSubTypes.Type(value = CDateTime.class, name = "dateTime"),
        @JsonSubTypes.Type(value = CStrictDate.class, name = "strictDate"),
        @JsonSubTypes.Type(value = CStrictTime.class, name = "strictTime"),
        @JsonSubTypes.Type(value = CLatestDate.class, name = "latestDate"),
        @JsonSubTypes.Type(value = CByteArray.class, name = "byteArray"),
        // Class Instance
        @JsonSubTypes.Type(value = ClassInstance.class, name = "classInstance"),


        // Instances understood by the protocol ----------------------------------------------------------
        // !!! ValueSpecification SHOULD NOT BE EXTENDED anymore (The Classes below are here only for backward compatibility) !!!
        // !!! Please now use ClassInstance and its extension mechanism !!!
        // Move to VS extension
        @JsonSubTypes.Type(value = ClassInstanceWrapper.class, name = "path"),
        @JsonSubTypes.Type(value = ClassInstanceWrapper.class, name = "rootGraphFetchTree"),
        // Below not used in the grammar parser (No instantiation in the project)
        // Move to functions and deprecate
        @JsonSubTypes.Type(value = ClassInstanceWrapper.class, name = "listInstance"),  // Used for anonymous collection of collection
        @JsonSubTypes.Type(value = ClassInstanceWrapper.class, name = "pair"),
        @JsonSubTypes.Type(value = ClassInstanceWrapper.class, name = "aggregateValue"),
        @JsonSubTypes.Type(value = ClassInstanceWrapper.class, name = "tdsAggregateValue"),
        @JsonSubTypes.Type(value = ClassInstanceWrapper.class, name = "tdsColumnInformation"),
        @JsonSubTypes.Type(value = ClassInstanceWrapper.class, name = "tdsSortInformation"),
        @JsonSubTypes.Type(value = ClassInstanceWrapper.class, name = "tdsOlapRank"),
        @JsonSubTypes.Type(value = ClassInstanceWrapper.class, name = "tdsOlapAggregation"),
        // Move to VS extension
        @JsonSubTypes.Type(value = ClassInstanceWrapper.class, name = "runtimeInstance"),
        @JsonSubTypes.Type(value = ClassInstanceWrapper.class, name = "executionContextInstance"),
        @JsonSubTypes.Type(value = ClassInstanceWrapper.class, name = "alloySerializationConfig"),
        //  ---------------------------------------------------------- Instances understood by the protocol

        // TO BE DELETED
        @JsonSubTypes.Type(value = AppliedQualifiedProperty.class, name = "qualifiedProperty"),     // Should not be coming to the system
        @JsonSubTypes.Type(value = Whatever.class, name = "whatever"),                              // Should not be coming to the system
        @JsonSubTypes.Type(value = UnknownAppliedFunction.class, name = "unknownFunc"),             // Should not be coming to the system
        @JsonSubTypes.Type(value = MappingInstance.class, name = "mappingInstance"),                // Go to PackageableElementPtr
        @JsonSubTypes.Type(value = PrimitiveType.class, name = "primitiveType"),                    // Go to PackageableElementPtr
        @JsonSubTypes.Type(value = UnitType.class, name = "unitType"),                              // Go to PackageableElementPtr
        @JsonSubTypes.Type(value = Class.class, name = "class"),                                    // Go to PackageableElementPtr
        @JsonSubTypes.Type(value = Enum.class, name = "enum"),                                      // Go to PackageableElementPtr
        @JsonSubTypes.Type(value = HackedClass.class, name = "hackedClass"),                        // Go to GenericTypeInstance
        @JsonSubTypes.Type(value = HackedUnit.class, name = "hackedUnit")                           // Go to GenericTypeInstance
})

// NOTE: due to plan generator producing duplicated _type field, we need to enable this
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class ValueSpecification
{
    public SourceInformation sourceInformation;

    public abstract <T> T accept(ValueSpecificationVisitor<T> visitor);
}
