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
import org.finos.legend.engine.ide.helpers.response.Candidate;
import org.finos.legend.engine.ide.helpers.response.IDEPureUnresolvedIdentifierExceptionResponse;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class IDEPureUnmatchedFunctionExceptionResponse extends IDEPureUnresolvedIdentifierExceptionResponse
{
    private final boolean PureUnmatchedFunctionException = true;
    ListIterable<Candidate> candidatesWithPackageImported;

    @Override
    public void addJsonKeyValues(MutableMap<String, Object> jsonMap)
    {
        super.addJsonKeyValues(jsonMap);
        jsonMap.put("PureUnmatchedFunctionException", this.PureUnmatchedFunctionException);
    }

    @Override
    public String toJSONString()
    {
        JSONObject object = new JSONObject(this.buildJsonKeyMaps());
        object.put("candidateName", this.candidateName);

        JSONArray jsonArrayWithPackageNotImported = new JSONArray();
        object.put("candidatesWithPackageNotImported", jsonArrayWithPackageNotImported);
        for (Candidate candidate : this.candidates)
        {
            jsonArrayWithPackageNotImported.add(candidate.toJSONObject());
        }

        JSONArray jsonArrayWithPackageImported = new JSONArray();
        object.put("candidatesWithPackageImported", jsonArrayWithPackageImported);
        for (Candidate candidate : this.candidatesWithPackageImported)
        {
            jsonArrayWithPackageImported.add(candidate.toJSONObject());
        }

        return object.toJSONString();
    }
}

