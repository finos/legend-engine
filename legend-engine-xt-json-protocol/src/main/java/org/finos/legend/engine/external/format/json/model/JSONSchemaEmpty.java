//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.external.format.json.model;

import org.finos.legend.engine.external.format.json.visitor.JSONSchemaVisitor;

public class JSONSchemaEmpty extends JSONSchema
{
    protected JSONSchemaEmpty(JSONSchemaEmpty.JSONSchemaEmptyBuilder b)
    {
        super(b);
    }

    public static class JSONSchemaEmptyBuilder extends JSONSchema.JSONSchemaBuilder<JSONSchemaEmpty.JSONSchemaEmptyBuilder>
    {
        public JSONSchemaEmpty build()
        {
            return new JSONSchemaEmpty(this);
        }
    }

    public <T> T accept(JSONSchemaVisitor<T> visitor)
    {
        return visitor.visit(this);
    }

    public static JSONSchemaEmpty.JSONSchemaEmptyBuilder builder()
    {
        return new JSONSchemaEmpty.JSONSchemaEmptyBuilder();
    }
}
