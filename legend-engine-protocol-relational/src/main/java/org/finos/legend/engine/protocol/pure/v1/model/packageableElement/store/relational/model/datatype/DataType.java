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

package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Char.class, name = "Char"),
        @JsonSubTypes.Type(value = VarChar.class, name = "Varchar"),
        @JsonSubTypes.Type(value = Numeric.class, name = "Numeric"),
        @JsonSubTypes.Type(value = Decimal.class, name = "Decimal"),
        @JsonSubTypes.Type(value = Float.class, name = "Float"),
        @JsonSubTypes.Type(value = Double.class, name = "Double"),
        @JsonSubTypes.Type(value = Real.class, name = "Real"),
        @JsonSubTypes.Type(value = Integer.class, name = "Integer"),
        @JsonSubTypes.Type(value = BigInt.class, name = "BigInt"),
        @JsonSubTypes.Type(value = SmallInt.class, name = "SmallInt"),
        @JsonSubTypes.Type(value = TinyInt.class, name = "TinyInt"),
        @JsonSubTypes.Type(value = Date.class, name = "Date"),
        @JsonSubTypes.Type(value = Timestamp.class, name = "Timestamp"),
        @JsonSubTypes.Type(value = Bit.class, name = "Bit"),
        @JsonSubTypes.Type(value = Varbinary.class, name = "Varbinary"),
        @JsonSubTypes.Type(value = Binary.class, name = "Binary"),
        @JsonSubTypes.Type(value = Other.class, name = "Other"),
        @JsonSubTypes.Type(value = Other.class, name = "Array")
})
public abstract class DataType
{
}

