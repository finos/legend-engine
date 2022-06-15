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

package org.finos.legend.engine.external.format.json.fromModel;

import org.finos.legend.engine.external.format.json.JSONSchemaSpecificationExtension;
import org.finos.legend.engine.external.format.json.JSONSchemaSpecificationExtensionLoader;
import org.finos.legend.engine.external.format.json.model.JSONSchema;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.Map;

public class JSONSchemaComposer
{
    public static final String DEFAULT_COMPOSER = "http://json-schema.org/draft-07/schema#"; // DraftV7 will be considered as the default schema specification for composing
    final Map<String, JSONSchemaSpecificationExtension> extensions = JSONSchemaSpecificationExtensionLoader.extensions();
    private final JSONSchemaSpecificationExtension inferredSchema;
    private final JSONSchema protocolModel;


    public JSONSchemaComposer(JSONSchema protocolModel)
    {
        JSONSchemaSpecificationExtension userSchemaSpec = extensions.get(DEFAULT_COMPOSER);
        this.protocolModel = protocolModel;

        if (this.protocolModel.schema != null)
        {
            String schema = this.protocolModel.schema;
            userSchemaSpec = extensions.get(schema);
            if (userSchemaSpec == null)
            {
                throw new EngineException("Cannot find JSON Schema specification for " + schema);
            }
        }

        inferredSchema = userSchemaSpec;
    }

    public String compose()
    {
        return inferredSchema.compose(protocolModel);
    }

}
