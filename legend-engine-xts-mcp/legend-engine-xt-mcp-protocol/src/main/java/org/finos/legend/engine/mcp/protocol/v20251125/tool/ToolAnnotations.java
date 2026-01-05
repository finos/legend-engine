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

package org.finos.legend.engine.mcp.protocol.v20251125.tool;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ToolAnnotations
{
    @JsonProperty
    private final Boolean destructiveHint;

    @JsonProperty
    private final Boolean idempotentHint;

    @JsonProperty
    private final Boolean openWorldHint;

    @JsonProperty
    private final Boolean readOnlyHint;

    @JsonProperty
    private final String title;

    public ToolAnnotations(final Boolean destructiveHint, final Boolean idempotentHint, final Boolean openWorldHint, final Boolean readOnlyHint, final String title)
    {
        this.destructiveHint = destructiveHint;
        this.idempotentHint = idempotentHint;
        this.openWorldHint = openWorldHint;
        this.readOnlyHint = readOnlyHint;
        this.title = title;
    }

    public Boolean getDestructiveHint()
    {
        return this.destructiveHint;
    }

    public Boolean getIdempotentHint()
    {
        return this.idempotentHint;
    }

    public Boolean getOpenWorldHint()
    {
        return this.openWorldHint;
    }

    public Boolean getReadOnlyHint()
    {
        return this.readOnlyHint;
    }

    public String getTitle()
    {
        return this.title;
    }
}
