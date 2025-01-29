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

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.json.simple.JSONAware;
import org.json.simple.JSONObject;

import java.util.Map;

public abstract class IDEResponse implements JSONAware
{
    private String text;
    private String compiler;

    @Override
    public String toJSONString()
    {
        return JSONObject.toJSONString(this.buildJsonKeyMaps());
    }

    Map<String, Object> buildJsonKeyMaps()
    {
        MutableMap<String, Object> jsonMap = Maps.mutable.of();
        jsonMap.put("text", this.text);
        jsonMap.put("compiler", this.compiler);
        this.addJsonKeyValues(jsonMap);
        return jsonMap;
    }

    public String getText()
    {
        return text;
    }

    public void appendText(String extraText)
    {
        this.text = null == this.text ? extraText : this.text + extraText;
    }

    abstract void addJsonKeyValues(MutableMap<String, Object> jsonMap);
}
