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

package org.finos.legend.engine.mcp.protocol.v20251125.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class ErrorResponse extends Response
{
    @JsonProperty
    private final Object id;

    @JsonProperty
    private final org.finos.legend.engine.mcp.protocol.v20251125.error.Error error;

    public ErrorResponse(final Object id, final org.finos.legend.engine.mcp.protocol.v20251125.error.Error error)
    {
        this.id = id;
        this.error = Objects.requireNonNull(error);
    }

    public Object getId()
    {
        return this.id;
    }

    public org.finos.legend.engine.mcp.protocol.v20251125.error.Error getError()
    {
        return this.error;
    }
}
