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

package org.finos.legend.engine.mcp.protocol.v20251125.result.content;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.Objects;

public class TextContent extends ContentBlock
{
    @JsonProperty
    private final Map<String, Object> _meta;

    @JsonProperty
    private final Annotations annotations;

    @JsonProperty
    private final String text;

    @JsonProperty
    private final String type = "text";

    public TextContent(final Map<String, Object> _meta, final Annotations annotations, final String text)
    {
        this._meta = _meta;
        this.annotations = annotations;
        this.text = Objects.requireNonNull(text);
    }

    public Map<String, Object> get_meta()
    {
        return this._meta;
    }

    public Annotations getAnnotations()
    {
        return this.annotations;
    }

    public String getText()
    {
        return this.text;
    }

    public String getType()
    {
        return this.type;
    }
}
