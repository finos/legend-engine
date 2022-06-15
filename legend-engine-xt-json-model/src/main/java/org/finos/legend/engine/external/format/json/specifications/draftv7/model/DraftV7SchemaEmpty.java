//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.external.format.json.specifications.draftv7.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.finos.legend.engine.external.format.json.specifications.draftv7.visitor.DraftV7SchemaVisitor;

@JsonDeserialize(
        builder = DraftV7SchemaEmpty.DraftV7SchemaEmptyBuilder.class
)
public class DraftV7SchemaEmpty extends DraftV7Schema
{
    protected DraftV7SchemaEmpty(DraftV7SchemaEmpty.DraftV7SchemaEmptyBuilder b)
    {
        super(b);
    }

    @JsonPOJOBuilder(
            withPrefix = ""
    )
    public static class DraftV7SchemaEmptyBuilder extends DraftV7SchemaBuilder<DraftV7SchemaEmpty.DraftV7SchemaEmptyBuilder>
    {
        public DraftV7SchemaEmpty build()
        {
            return new DraftV7SchemaEmpty(this);
        }
    }

    public <T> T accept(DraftV7SchemaVisitor<T> visitor)
    {
        return visitor.visit(this);
    }

    public static DraftV7SchemaEmpty.DraftV7SchemaEmptyBuilder builder()
    {
        return new DraftV7SchemaEmpty.DraftV7SchemaEmptyBuilder();
    }
}
