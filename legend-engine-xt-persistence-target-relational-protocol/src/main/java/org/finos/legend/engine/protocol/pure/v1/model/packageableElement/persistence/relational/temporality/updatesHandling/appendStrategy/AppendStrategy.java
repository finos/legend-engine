// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.updatesHandling.appendStrategy;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type", defaultImpl = AllowDuplicates.class)
@JsonSubTypes({
    @JsonSubTypes.Type(value = AllowDuplicates.class, name = "allowDuplicates"),
    @JsonSubTypes.Type(value = FailOnDuplicates.class, name = "FailOnDuplicates"),
    @JsonSubTypes.Type(value = FilterDuplicates.class, name = "FilterDuplicates")
})
public abstract class AppendStrategy
{
    public SourceInformation sourceInformation;

    public abstract <T> T accept(AppendStrategyVisitor<T> visitor);
}
