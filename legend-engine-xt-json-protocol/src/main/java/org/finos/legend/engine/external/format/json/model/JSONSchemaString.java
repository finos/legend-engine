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

package org.finos.legend.engine.external.format.json.model;

import org.finos.legend.engine.external.format.json.visitor.JSONSchemaVisitor;

public class JSONSchemaString extends JSONSchema
{
    public final Integer minLength;
    public final Integer maxLength;
    public final String pattern;
    public final String format;

    protected JSONSchemaString(JSONSchemaString.JSONSchemaStringBuilder b)
    {
        super(b);
        this.minLength = b.minLength;
        this.maxLength = b.maxLength;
        this.pattern = b.pattern;
        this.format = b.format;
    }

    public static class JSONSchemaStringBuilder extends JSONSchemaBuilder<JSONSchemaString.JSONSchemaStringBuilder>
    {
        private Integer minLength;
        private Integer maxLength;
        private String pattern;
        private String format;

        public JSONSchemaString build()
        {
            return new JSONSchemaString(this);
        }

        public JSONSchemaStringBuilder minLength(Integer minLength)
        {
            this.minLength = minLength;
            return this;
        }

        public JSONSchemaStringBuilder maxLength(Integer maxLength)
        {
            this.maxLength = maxLength;
            return this;
        }

        public JSONSchemaStringBuilder pattern(String pattern)
        {
            this.pattern = pattern;
            return this;
        }

        public JSONSchemaStringBuilder format(String format)
        {
            this.format = format;
            return this;
        }
    }

    public <T> T accept(JSONSchemaVisitor<T> visitor)
    {
        return visitor.visit(this);
    }

    public static JSONSchemaString.JSONSchemaStringBuilder builder()
    {
        return new JSONSchemaString.JSONSchemaStringBuilder();
    }
}
