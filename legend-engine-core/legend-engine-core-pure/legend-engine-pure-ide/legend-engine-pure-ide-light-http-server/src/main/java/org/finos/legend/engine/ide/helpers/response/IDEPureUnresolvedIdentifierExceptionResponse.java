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

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class IDEPureUnresolvedIdentifierExceptionResponse extends IDEExceptionResponse
{
    String candidateName;
    ListIterable<Candidate> candidates;

    @Override
    public void addJsonKeyValues(MutableMap<String, Object> jsonMap)
    {
        super.addJsonKeyValues(jsonMap);
        jsonMap.put("candidateName", this.candidateName);
    }

    @Override
    public String toJSONString()
    {
        JSONObject object = new JSONObject(this.buildJsonKeyMaps());
        object.put("candidateName", this.candidateName);
        JSONArray jsonArray = new JSONArray();
        object.put("candidates", jsonArray);

        for (Candidate candidate : this.candidates)
        {
            jsonArray.add(candidate.toJSONObject());
        }
        return object.toJSONString();
    }
}
