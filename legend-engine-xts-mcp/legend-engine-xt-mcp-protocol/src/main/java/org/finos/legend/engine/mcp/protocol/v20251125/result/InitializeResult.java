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
import org.finos.legend.engine.mcp.protocol.v20251125.Constants;
import org.finos.legend.engine.mcp.protocol.v20251125.implementation.Implementation;

import java.util.Map;
import java.util.Objects;

public class InitializeResult extends Result
{
    @JsonProperty
    private final Map<String, Object> _meta;

    @JsonProperty
    private final Map<String, Object> capabilities; // Can be typed more strongly when more capabilities are defined

    @JsonProperty
    private final String instructions;

    @JsonProperty
    private final String protocolVersion = Constants.PROTOCOL_VERSION;

    @JsonProperty
    private final Implementation serverInfo;

    public InitializeResult(final Map<String, Object> _meta, final Map<String, Object> capabilities, final String instructions, final Implementation serverInfo)
    {
        this._meta = _meta;
        this.capabilities = Objects.requireNonNull(capabilities);
        this.instructions = instructions;
        this.serverInfo = Objects.requireNonNull(serverInfo);
    }

    public Map<String, Object> get_meta()
    {
        return this._meta;
    }

    public Map<String, Object> getCapabilities()
    {
        return this.capabilities;
    }

    public String getInstructions()
    {
        return this.instructions;
    }

    public String getProtocolVersion()
    {
        return this.protocolVersion;
    }

    public Implementation getServerInfo()
    {
        return this.serverInfo;
    }
}
