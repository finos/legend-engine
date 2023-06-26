// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.dataset.actionindicator;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type", defaultImpl = DeleteIndicatorForGraphFetch.class)
@JsonSubTypes({
    @JsonSubTypes.Type(value = DeleteIndicatorForGraphFetch.class, name = "deleteIndicatorForGraphFetch"),
    @JsonSubTypes.Type(value = DeleteIndicatorForTds.class, name = "deleteIndicatorForTds")
})
public abstract class DeleteIndicator extends ActionIndicatorFields
{
    public List<String> deleteValues;

    @Override
    public <T> T accept(ActionIndicatorFieldsVisitor<T> visitor)
    {
        return visitor.visitDeleteIndicator(this);
    }

    public abstract <T> T accept(DeleteIndicatorVisitor<T> visitor);
}
