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
        builder = DraftV7SchemaString.DraftV7SchemaStringBuilder.class
)
public class DraftV7SchemaString extends DraftV7Schema
{
    public final Integer minLength;
    public final Integer maxLength;
    public final String pattern;
    public final String format;

    protected DraftV7SchemaString(DraftV7SchemaString.DraftV7SchemaStringBuilder b)
    {
        super(b);
        this.minLength = b.minLength;
        this.maxLength = b.maxLength;
        this.pattern = b.pattern;
        this.format = b.format;
    }

    @JsonPOJOBuilder(
            withPrefix = ""
    )
    public static class DraftV7SchemaStringBuilder extends DraftV7SchemaBuilder<DraftV7SchemaString.DraftV7SchemaStringBuilder>
    {
        private Integer minLength;
        private Integer maxLength;
        private String pattern;
        private String format;

        public DraftV7SchemaString build()
        {
            return new DraftV7SchemaString(this);
        }

        public DraftV7SchemaStringBuilder minLength(Integer minLength)
        {
            this.minLength = minLength;
            return this;
        }

        public DraftV7SchemaStringBuilder maxLength(Integer maxLength)
        {
            this.maxLength = maxLength;
            return this;
        }

        public DraftV7SchemaStringBuilder pattern(String pattern)
        {
            this.pattern = pattern;
            return this;
        }

        public DraftV7SchemaStringBuilder format(String format)
        {
            this.format = format;
            return this;
        }
    }

    public <T> T accept(DraftV7SchemaVisitor<T> visitor)
    {
        return visitor.visit(this);
    }

    public static DraftV7SchemaString.DraftV7SchemaStringBuilder builder()
    {
        return new DraftV7SchemaString.DraftV7SchemaStringBuilder();
    }
}
