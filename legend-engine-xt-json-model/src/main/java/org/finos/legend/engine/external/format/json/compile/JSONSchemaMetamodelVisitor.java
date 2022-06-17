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

package org.finos.legend.engine.external.format.json.compile;

import org.finos.legend.pure.generated.Root_meta_external_format_json_metamodel_JSONSchema;
import org.finos.legend.pure.generated.Root_meta_external_format_json_metamodel_JSONSchemaArray;
import org.finos.legend.pure.generated.Root_meta_external_format_json_metamodel_JSONSchemaBoolean;
import org.finos.legend.pure.generated.Root_meta_external_format_json_metamodel_JSONSchemaFragment;
import org.finos.legend.pure.generated.Root_meta_external_format_json_metamodel_JSONSchemaInteger;
import org.finos.legend.pure.generated.Root_meta_external_format_json_metamodel_JSONSchemaMultiType;
import org.finos.legend.pure.generated.Root_meta_external_format_json_metamodel_JSONSchemaNull;
import org.finos.legend.pure.generated.Root_meta_external_format_json_metamodel_JSONSchemaNumber;
import org.finos.legend.pure.generated.Root_meta_external_format_json_metamodel_JSONSchemaObject;
import org.finos.legend.pure.generated.Root_meta_external_format_json_metamodel_JSONSchemaString;

public interface JSONSchemaMetamodelVisitor<T>
{
    T visit(Root_meta_external_format_json_metamodel_JSONSchemaArray schemaObject);

    T visit(Root_meta_external_format_json_metamodel_JSONSchemaBoolean schemaObject);

    T visit(Root_meta_external_format_json_metamodel_JSONSchemaFragment schemaObject);

    T visit(Root_meta_external_format_json_metamodel_JSONSchemaMultiType schemaObject);

    T visit(Root_meta_external_format_json_metamodel_JSONSchemaInteger schemaObject);

    T visit(Root_meta_external_format_json_metamodel_JSONSchemaNull schemaObject);

    T visit(Root_meta_external_format_json_metamodel_JSONSchemaNumber schemaObject);

    T visit(Root_meta_external_format_json_metamodel_JSONSchemaObject schemaObject);

    T visit(Root_meta_external_format_json_metamodel_JSONSchemaString schemaObject);

    T visit(Root_meta_external_format_json_metamodel_JSONSchema schemaObject);
}
