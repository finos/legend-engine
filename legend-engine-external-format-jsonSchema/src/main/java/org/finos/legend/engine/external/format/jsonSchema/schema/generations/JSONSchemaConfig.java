// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.external.format.jsonSchema.schema.generations;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.external.shared.format.generations.GenerationConfiguration;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.pure.generated.Root_meta_json_schema_generation_JSONSchemaConfig;
import org.finos.legend.pure.generated.core_json_jsonSchema;
import org.finos.legend.pure.generated.core_pure_corefunctions_metaExtension;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;

import java.util.List;

public class JSONSchemaConfig extends GenerationConfiguration
{
    @JsonProperty("useConstraints")
    public Boolean useConstraints;
    @JsonProperty("includeAllRelatedTypes")
    public Boolean includeAllRelatedTypes;
    @JsonProperty("generateConstraintFunctionSchemas")
    public Boolean generateConstraintFunctionSchemas;
    @JsonProperty(value = "schemaSpecification")
    public String schemaSpecification;



    public Root_meta_json_schema_generation_JSONSchemaConfig process(PureModel pureModel)
    {
        Root_meta_json_schema_generation_JSONSchemaConfig generationConfiguration = core_json_jsonSchema.Root_meta_json_schema_generation_defaultConfig__JSONSchemaConfig_1_(pureModel.getExecutionSupport());
        List<PackageableElement> scopeElements = ListIterate.collect(this.generationScope(), e -> core_pure_corefunctions_metaExtension.Root_meta_pure_functions_meta_pathToElement_String_1__PackageableElement_1_(e, pureModel.getExecutionSupport()));


        if (useConstraints != null)
        {
            generationConfiguration._useConstraints(useConstraints);
        }
        if (includeAllRelatedTypes != null)
        {
            generationConfiguration._includeAllRelatedTypes(includeAllRelatedTypes);
        }

        if (generateConstraintFunctionSchemas != null)
        {
            generationConfiguration._generateConstraintFunctionSchemas(generateConstraintFunctionSchemas);
        }

        if( schemaSpecification != null)
        {

            switch (schemaSpecification)
            {
                case "meta::json::schema::generation::JSONSchemaSpecification.OPEN_API_V3_0_3":
                case "meta::json::schema::generation::JSONSchemaSpecification.OPEN_API_V3_0_3_YAML":
                case "meta::json::schema::generation::JSONSchemaSpecification.JSON_SCHEMA_DRAFT_07":

                    generationConfiguration._schemaSpecification(pureModel.getEnumValue("meta::json::schema::generation::JSONSchemaSpecification", schemaSpecification.split("[.]")[1]));
                    break;
                case "OPEN_API_V3_0_3":
                case "OPEN_API_V3_0_3_YAML":
                case "JSON_SCHEMA_DRAFT_07":
                    generationConfiguration._schemaSpecification(pureModel.getEnumValue("meta::json::schema::generation::JSONSchemaSpecification", schemaSpecification));
                    break;
            }
        }

        generationConfiguration._scopeElements((RichIterable<? extends PackageableElement>) scopeElements);
        return generationConfiguration;
    }
}

