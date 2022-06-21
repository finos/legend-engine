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

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.external.shared.format.model.ExternalSchemaCompileContext;
import org.finos.legend.pure.generated.Root_meta_json_schema_fromSchema_SchemaInput;
import org.finos.legend.pure.generated.Root_meta_json_schema_fromSchema_SchemaInput_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_json_metamodel_JsonSchema;
import org.finos.legend.pure.generated.Root_meta_external_format_json_metamodel_JsonSchema_Impl;
import org.finos.legend.pure.generated.core_external_format_json_fromJSONSchema;

public class JsonSchemaCompiler
{
    private final ExternalSchemaCompileContext context;

    public JsonSchemaCompiler(ExternalSchemaCompileContext context)
    {
        this.context = context;
    }

    public Root_meta_external_format_json_metamodel_JsonSchema compile()
    {
        String content = context.getContent();
        String location = context.getLocation();

        // validation step
        Root_meta_json_schema_fromSchema_SchemaInput schemaInput =
                new Root_meta_json_schema_fromSchema_SchemaInput_Impl("")
                        ._fileName(location)
                        ._schema(content);
        core_external_format_json_fromJSONSchema.Root_meta_json_schema_fromSchema_JSONSchemaToPure_SchemaInput_MANY__PackageableElement_MANY_(Lists.mutable.with(schemaInput), context.getPureModel().getExecutionSupport());

        return new Root_meta_external_format_json_metamodel_JsonSchema_Impl("")
                ._content(content);
    }
}
