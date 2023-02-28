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

package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.aggregationAware.AggregationAwarePropertyMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.xStore.XStorePropertyMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.mapping.PurePropertyMapping;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = PurePropertyMapping.class, name = "purePropertyMapping"),
        @JsonSubTypes.Type(value = XStorePropertyMapping.class, name = "xStorePropertyMapping"),
        @JsonSubTypes.Type(value = AggregationAwarePropertyMapping.class, name = "AggregationAwarePropertyMapping"),
})
public abstract class PropertyMapping
{
    public PropertyPointer property;
    public String source;
    public String target;
    public LocalMappingPropertyInfo localMappingProperty;
    public SourceInformation sourceInformation;

    public <T> T accept(PropertyMappingVisitor<T> visitor)
    {
        return visitor.visit(this);
    }

    ;
}
