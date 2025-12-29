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

package org.finos.legend.engine.mcp.protocol.v20251125.error;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.Objects;

public class Error
{
    @JsonProperty
    private final int code;

    @JsonProperty
    private final Map<String, Object> data;

    @JsonProperty
    private final String message;

    public Error(final int code, final Map<String, Object> data, final String message)
    {
        this.code = code;
        this.data = data;
        this.message = Objects.requireNonNull(message);
    }

    public int getCode()
    {
        return this.code;
    }

    public Map<String, Object> getData()
    {
        return this.data;
    }

    public String getMessage()
    {
        return this.message;
    }
}

