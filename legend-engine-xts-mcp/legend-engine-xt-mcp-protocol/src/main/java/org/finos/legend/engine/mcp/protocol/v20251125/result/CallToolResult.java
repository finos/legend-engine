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

package org.finos.legend.engine.mcp.protocol.v20251125.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.finos.legend.engine.mcp.protocol.v20251125.result.content.ContentBlock;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CallToolResult extends Result
{
    @JsonProperty
    private final Map<String, Object> _meta;

    @JsonProperty
    private final List<ContentBlock> content;

    @JsonProperty
    private final Boolean isError;

    @JsonProperty
    private final Map<String, Object> structuredContent;

    public CallToolResult(final Map<String, Object> _meta, final List<ContentBlock> content, final Boolean isError, final Map<String, Object> structuredContent)
    {
        this._meta = _meta;
        this.content = Objects.requireNonNull(content);
        this.isError = isError;
        this.structuredContent = structuredContent;
    }

    public Map<String, Object> get_meta()
    {
        return this._meta;
    }

    public List<ContentBlock> getContent()
    {
        return this.content;
    }

    public Boolean getIsError()
    {
        return this.isError;
    }

    public Map<String, Object> getStructuredContent()
    {
        return this.structuredContent;
    }
}
