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
import org.finos.legend.engine.mcp.protocol.v20251125.result.Result;

import java.util.Objects;

public class ResultResponse extends Response
{
    @JsonProperty
    private final Object id;

    @JsonProperty
    private final Result result;

    public ResultResponse(final Object id, final Result result)
    {
        this.id = Objects.requireNonNull(id);
        this.result = Objects.requireNonNull(result);
    }

    public Object getId()
    {
        return this.id;
    }

    public Result getResult()
    {
        return this.result;
    }
}
