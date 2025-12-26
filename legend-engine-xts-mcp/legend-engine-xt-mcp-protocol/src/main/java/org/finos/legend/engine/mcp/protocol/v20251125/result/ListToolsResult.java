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
import org.finos.legend.engine.mcp.protocol.v20251125.tool.Tool;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ListToolsResult extends Result
{
    @JsonProperty
    private final Map<String, Object> _meta;

    @JsonProperty
    private final String nextCursor;

    @JsonProperty
    private final List<Tool> tools;

    public ListToolsResult(final Map<String, Object> _meta, final String nextCursor, final List<Tool> tools)
    {
        this._meta = _meta;
        this.nextCursor = nextCursor;
        this.tools = Objects.requireNonNull(tools);
    }

    public Map<String, Object> get_meta()
    {
        return this._meta;
    }

    public String getNextCursor()
    {
        return this.nextCursor;
    }

    public List<Tool> getTools()
    {
        return this.tools;
    }
}
