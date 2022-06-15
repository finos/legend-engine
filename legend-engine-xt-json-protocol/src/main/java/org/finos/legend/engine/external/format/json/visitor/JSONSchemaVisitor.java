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

package org.finos.legend.engine.external.format.json.visitor;

import org.finos.legend.engine.external.format.json.model.JSONSchema;
import org.finos.legend.engine.external.format.json.model.JSONSchemaArray;
import org.finos.legend.engine.external.format.json.model.JSONSchemaBoolean;
import org.finos.legend.engine.external.format.json.model.JSONSchemaEmpty;
import org.finos.legend.engine.external.format.json.model.JSONSchemaInteger;
import org.finos.legend.engine.external.format.json.model.JSONSchemaNull;
import org.finos.legend.engine.external.format.json.model.JSONSchemaNumber;
import org.finos.legend.engine.external.format.json.model.JSONSchemaObject;
import org.finos.legend.engine.external.format.json.model.JSONSchemaString;

public interface JSONSchemaVisitor<T>
{
    T visit(JSONSchema schemaObject);

    T visit(JSONSchemaArray schemaObject);

    T visit(JSONSchemaBoolean schemaObject);

    T visit(JSONSchemaEmpty schemaObject);

    T visit(JSONSchemaInteger schemaObject);

    T visit(JSONSchemaNull schemaObject);

    T visit(JSONSchemaNumber schemaObject);

    T visit(JSONSchemaObject schemaObject);

    T visit(JSONSchemaString schemaObject);
}
