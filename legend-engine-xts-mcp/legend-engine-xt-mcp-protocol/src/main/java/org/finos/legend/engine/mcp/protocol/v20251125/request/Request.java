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

package org.finos.legend.engine.mcp.protocol.v20251125.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.finos.legend.engine.mcp.protocol.v20251125.JSONRPC2Message;

import java.util.Map;
import java.util.Objects;

public class Request extends JSONRPC2Message
{
    @JsonProperty
    private final Object id;

    @JsonProperty
    private final String method;

    @JsonProperty
    private final Map<String, Object> params;

    @JsonCreator
    public Request(@JsonProperty("id") final Object id, @JsonProperty("method") final String method, @JsonProperty("params") final Map<String, Object> params)
    {
        this.id = Objects.requireNonNull(id);
        this.method = Objects.requireNonNull(method);
        this.params = params;
    }

    public Object getId()
    {
        return this.id;
    }

    public String getMethod()
    {
        return this.method;
    }

    public Map<String, Object> getParams()
    {
        return this.params;
    }
}
