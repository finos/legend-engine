// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.ide.api.execution.function.manager;

import java.util.Map;

public class ExecutionRequest
{
    private final Map<String, String[]> requestParams;
    private boolean disableStreaming = false;

    public ExecutionRequest(Map<String, String[]> requestParams)
    {
        this(requestParams, false);
    }

    public ExecutionRequest(Map<String, String[]> requestParams, boolean disableStreaming)
    {
        this.requestParams = requestParams;
        this.disableStreaming = disableStreaming;
    }

    public Map<String, String[]> getRequestParams()
    {
        return this.requestParams;
    }

    public boolean isStreamingDisabled()
    {
        return this.disableStreaming;
    }

    String getRequestParamToOne(String key)
    {
        String[] vals = this.requestParams.get(key);
        if (vals == null)
        {
            return null;
        }
        else if (vals.length == 1)
        {
            return vals[0];
        }
        else
        {
            throw new IllegalArgumentException("Parameter '" + key + "' must have at most one value");
        }
    }
}
