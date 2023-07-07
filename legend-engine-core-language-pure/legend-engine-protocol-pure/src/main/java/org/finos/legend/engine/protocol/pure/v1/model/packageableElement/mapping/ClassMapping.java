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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.aggregationAware.AggregationAwareClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.mapping.PureInstanceClassMapping;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = OperationClassMapping.class, name = "operation"),
        @JsonSubTypes.Type(value = PureInstanceClassMapping.class, name = "pureInstance"),
        @JsonSubTypes.Type(value = AggregationAwareClassMapping.class, name = "aggregationAware"),
        @JsonSubTypes.Type(value = MergeOperationClassMapping.class, name = "mergeOperation"),

})
public abstract class ClassMapping
{
    public String id;
    public MappingClass mappingClass;
    @JsonProperty(value = "class")
    public String _class;
    public String extendsClassMappingId;
    public boolean root = true;
    public SourceInformation sourceInformation;
    public SourceInformation classSourceInformation;

    public abstract <T> T accept(ClassMappingVisitor<T> visitor);
}
