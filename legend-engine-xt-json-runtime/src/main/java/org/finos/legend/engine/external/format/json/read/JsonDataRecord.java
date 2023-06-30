// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.external.format.json.read;

public class JsonDataRecord
{
    private final long number;
    private final String record;

    public JsonDataRecord(long number, String record)
    {
        this.number = number;
        this.record = record;
    }

    public long getNumber()
    {
        return number;
    }

    public String getRecord()
    {
        return record;
    }

    public String typePath$()
    {
        return "meta::external::format::json::executionPlan::model::JsonDataRecord";
    }

    public String typeName$()
    {
        return "JsonDataRecord";
    }
}
