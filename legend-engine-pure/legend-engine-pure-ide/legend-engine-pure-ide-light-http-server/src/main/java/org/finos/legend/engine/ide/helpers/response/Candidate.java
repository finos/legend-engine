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

import org.json.simple.JSONObject;

public class Candidate
{
    public String sourceID;
    public Integer line;
    public Integer column;
    public String foundName;
    public String type;
    public String fileToBeModified;
    public Integer lineToBeModified;
    public Integer columnToBeModified;
    public Boolean add;
    public String messageToBeModified;

    public JSONObject toJSONObject()
    {
        JSONObject object = new JSONObject();
        object.put("sourceID", this.sourceID);
        object.put("line", this.line);
        object.put("column", this.column);
        object.put("foundName", this.foundName);
        object.put("type", this.type);
        object.put("fileToBeModified", this.fileToBeModified);
        object.put("lineToBeModified", this.lineToBeModified);
        object.put("columnToBeModified", this.columnToBeModified);
        object.put("add", this.add);
        object.put("messageToBeModified", this.messageToBeModified);
        return object;
    }
}

