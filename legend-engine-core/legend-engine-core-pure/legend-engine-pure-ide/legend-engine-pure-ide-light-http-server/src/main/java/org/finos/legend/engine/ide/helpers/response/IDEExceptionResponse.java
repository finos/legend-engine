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

package org.finos.legend.engine.ide.helpers.response;

import org.eclipse.collections.api.map.MutableMap;

public class IDEExceptionResponse extends IDEResponse
{
    private final boolean error = true;
    public boolean RO;
    public String source;
    public Integer line;
    public Integer column;

    @Override
    public void addJsonKeyValues(MutableMap<String, Object> jsonMap)
    {
        jsonMap.put("error", this.error);
        jsonMap.put("RO", this.RO);
        jsonMap.put("source", this.source);
        jsonMap.put("line", this.line);
        jsonMap.put("column", this.column);
    }
}
