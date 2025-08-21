// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.api.analytics.model.propertyLineage;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type", defaultImpl = PropertyLineageNode.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = RootQuery.class, name = "RootQuery"),
        @JsonSubTypes.Type(value = Query.class, name = "Query"),
        @JsonSubTypes.Type(value = PropertyOwnerNode.class, name = "PropertyOwnerNode"),
        @JsonSubTypes.Type(value = StorePropertyLineageNode.class, name = "StorePropertyLineageNode"),
        @JsonSubTypes.Type(value = DataProductAccessPointOwner.class, name = "DataProductAccessPointOwner"),
        @JsonSubTypes.Type(value = IngestionSpecificationOwner.class, name = "IngestionSpecificationOwner")
})
public class PropertyLineageNode
{
    public String id;
    public String name;
}
