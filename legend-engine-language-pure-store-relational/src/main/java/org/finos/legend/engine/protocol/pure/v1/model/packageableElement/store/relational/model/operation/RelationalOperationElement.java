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

package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.operation;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = TableAliasColumn.class, name = "column"),
        @JsonSubTypes.Type(value = ElementWithJoins.class, name = "elemtWithJoins"),
        @JsonSubTypes.Type(value = DynaFunc.class, name = "dynaFunc"),
        @JsonSubTypes.Type(value = Literal.class, name = "literal"),
        @JsonSubTypes.Type(value = LiteralList.class, name = "literalList")
})
public abstract class RelationalOperationElement
{
    public SourceInformation sourceInformation;
}
