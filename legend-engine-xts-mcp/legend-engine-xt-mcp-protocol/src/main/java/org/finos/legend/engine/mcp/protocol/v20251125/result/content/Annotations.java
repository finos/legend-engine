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

import java.util.List;

public class Annotations
{
    @JsonProperty
    private final List<Role> audience;

    @JsonProperty
    private final String lastModified;

    @JsonProperty
    private final Integer priority;

    public Annotations(final List<Role> audience, final String lastModified, final Integer priority)
    {
        this.audience = audience;
        this.lastModified = lastModified;
        this.priority = priority;
    }

    public List<Role> getAudience()
    {
        return this.audience;
    }

    public String getLastModified()
    {
        return this.lastModified;
    }

    public Integer getPriority()
    {
        return this.priority;
    }

    public enum Role
    {
        assistant,
        user,
    }
}
