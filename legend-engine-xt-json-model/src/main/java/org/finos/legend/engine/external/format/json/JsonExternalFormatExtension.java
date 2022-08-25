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

package org.finos.legend.engine.external.format.json;

import org.finos.legend.engine.external.format.json.compile.JsonSchemaCompiler;
import org.finos.legend.engine.external.format.json.fromModel.ModelToJsonSchemaConfiguration;
import org.finos.legend.engine.external.format.json.toModel.JsonSchemaToModelConfiguration;
import org.finos.legend.engine.external.shared.format.model.compile.ExternalSchemaCompileContext;
import org.finos.legend.engine.external.shared.format.model.transformation.fromModel.ExternalFormatSchemaGenerationExtension;
import org.finos.legend.engine.external.shared.format.model.transformation.toModel.ExternalFormatModelGenerationExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.pure.generated.Root_meta_external_format_json_metamodel_JsonSchema;
import org.finos.legend.pure.generated.Root_meta_external_format_json_transformation_fromPure_ModelToJsonSchemaConfiguration_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_json_transformation_toPure_JsonSchemaToModelConfiguration_Impl;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_ExternalFormatContract;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_transformation_fromPure_ModelToSchemaConfiguration;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_transformation_toPure_SchemaToModelConfiguration;
import org.finos.legend.pure.generated.core_external_format_json_externalFormatContract;

public class JsonExternalFormatExtension implements ExternalFormatSchemaGenerationExtension<Root_meta_external_format_json_metamodel_JsonSchema, ModelToJsonSchemaConfiguration>, ExternalFormatModelGenerationExtension<Root_meta_external_format_json_metamodel_JsonSchema, JsonSchemaToModelConfiguration>
{
    private static final Root_meta_external_shared_format_ExternalFormatContract<Root_meta_external_format_json_metamodel_JsonSchema> jsonSchemaContract = (Root_meta_external_shared_format_ExternalFormatContract<Root_meta_external_format_json_metamodel_JsonSchema>) core_external_format_json_externalFormatContract.Root_meta_external_format_json_contract_jsonSchemaFormatContract__ExternalFormatContract_1_(PureModel.CORE_PURE_MODEL.getExecutionSupport());
    public static final String TYPE = jsonSchemaContract._id();

    @Override
    public Root_meta_external_shared_format_ExternalFormatContract<Root_meta_external_format_json_metamodel_JsonSchema> getExternalFormatContract()
    {
        return jsonSchemaContract;
    }

    @Override
    public Root_meta_external_format_json_metamodel_JsonSchema compileSchema(ExternalSchemaCompileContext context)
    {
        return new JsonSchemaCompiler(context).compile();
    }

    @Override
    public String metamodelToText(Root_meta_external_format_json_metamodel_JsonSchema schemaDetail, PureModel pureModel)
    {
        return schemaDetail._content();
    }

    @Override
    public Root_meta_external_shared_format_transformation_toPure_SchemaToModelConfiguration compileSchemaToModelConfiguration(JsonSchemaToModelConfiguration configuration, PureModel pureModel)
    {
        return new Root_meta_external_format_json_transformation_toPure_JsonSchemaToModelConfiguration_Impl("", null, pureModel.getClass("meta::external::format::json::transformation::toPure::JsonSchemaToModelConfiguration"))
                ._sourceSchemaId(configuration.sourceSchemaId)
                ._targetPackage(configuration.targetPackage);
    }

    @Override
    public Root_meta_external_shared_format_transformation_fromPure_ModelToSchemaConfiguration compileModelToSchemaConfiguration(ModelToJsonSchemaConfiguration configuration, PureModel pureModel)
    {
        return new Root_meta_external_format_json_transformation_fromPure_ModelToJsonSchemaConfiguration_Impl("", null, pureModel.getClass("meta::external::format::json::transformation::fromPure::ModelToJsonSchemaConfiguration"))
                ._targetSchemaSet(configuration.targetSchemaSet);
    }
}
