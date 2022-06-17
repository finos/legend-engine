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

package org.finos.legend.engine.external.format.json.specifications.openapiv3_0_3.visitor;

import org.finos.legend.engine.external.format.json.specifications.openapiv3_0_3.model.OpenAPIv3_0_3Schema;
import org.finos.legend.engine.external.format.json.specifications.openapiv3_0_3.model.OpenAPIv3_0_3SchemaArray;
import org.finos.legend.engine.external.format.json.specifications.openapiv3_0_3.model.OpenAPIv3_0_3SchemaBoolean;
import org.finos.legend.engine.external.format.json.specifications.openapiv3_0_3.model.OpenAPIv3_0_3SchemaFragment;
import org.finos.legend.engine.external.format.json.specifications.openapiv3_0_3.model.OpenAPIv3_0_3SchemaInteger;
import org.finos.legend.engine.external.format.json.specifications.openapiv3_0_3.model.OpenAPIv3_0_3SchemaNull;
import org.finos.legend.engine.external.format.json.specifications.openapiv3_0_3.model.OpenAPIv3_0_3SchemaNumber;
import org.finos.legend.engine.external.format.json.specifications.openapiv3_0_3.model.OpenAPIv3_0_3SchemaObject;
import org.finos.legend.engine.external.format.json.specifications.openapiv3_0_3.model.OpenAPIv3_0_3SchemaString;

public interface OpenAPIv3_0_3SchemaVisitor<T>
{
    T visit(OpenAPIv3_0_3Schema schemaObject);

    T visit(OpenAPIv3_0_3SchemaArray schemaObject);

    T visit(OpenAPIv3_0_3SchemaBoolean schemaObject);

    T visit(OpenAPIv3_0_3SchemaFragment schemaObject);

    T visit(OpenAPIv3_0_3SchemaInteger schemaObject);

    T visit(OpenAPIv3_0_3SchemaNull schemaObject);

    T visit(OpenAPIv3_0_3SchemaNumber schemaObject);

    T visit(OpenAPIv3_0_3SchemaObject schemaObject);

    T visit(OpenAPIv3_0_3SchemaString schemaObject);
}
