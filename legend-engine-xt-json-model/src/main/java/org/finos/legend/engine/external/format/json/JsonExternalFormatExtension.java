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

import org.eclipse.collections.api.list.ImmutableList;
import org.finos.legend.engine.external.format.json.compile.JsonSchemaCompiler;
import org.finos.legend.engine.external.format.json.fromModel.ModelToJsonSchemaConfiguration;
import org.finos.legend.engine.external.format.json.toModel.JsonSchemaToModelConfiguration;
import org.finos.legend.engine.external.shared.format.model.ExternalFormatExtension;
import org.finos.legend.engine.external.shared.format.model.ExternalSchemaCompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.pure.generated.core_external_format_json_binding_validation;
import org.finos.legend.pure.generated.core_external_format_json_binding_jsonSchemaToPure;
import org.finos.legend.pure.generated.core_external_format_json_binding_pureToJsonSchema;
import org.finos.legend.pure.generated.Root_meta_external_format_json_binding_fromPure_ModelToJsonSchemaConfiguration;
import org.finos.legend.pure.generated.Root_meta_external_format_json_binding_fromPure_ModelToJsonSchemaConfiguration_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_json_binding_toPure_JsonSchemaToModelConfiguration;
import org.finos.legend.pure.generated.Root_meta_external_format_json_binding_toPure_JsonSchemaToModelConfiguration_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_json_metamodel_JsonSchema;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_binding_Binding;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_binding_validation_BindingDetail;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_metamodel_SchemaSet;

import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.List;

public class JsonExternalFormatExtension implements ExternalFormatExtension<Root_meta_external_format_json_metamodel_JsonSchema, JsonSchemaToModelConfiguration, ModelToJsonSchemaConfiguration>
{
    public static final String TYPE = "JSON";
    private static final boolean IN_DEBUG = ManagementFactory.getRuntimeMXBean().getInputArguments().toString().contains(":jdwp");

    @Override
    public String getFormat()
    {
        return TYPE;
    }

    @Override
    public List<String> getContentTypes()
    {
        return Collections.singletonList(JsonExternalFormatPureExtension.CONTENT_TYPE);
    }

    @Override
    public Root_meta_external_format_json_metamodel_JsonSchema compileSchema(ExternalSchemaCompileContext context)
    {
        return new JsonSchemaCompiler(context).compile();
    }

    @Override
    public Root_meta_external_shared_format_binding_validation_BindingDetail bindDetails(Root_meta_external_shared_format_binding_Binding binding, CompileContext context)
    {
        return core_external_format_json_binding_validation.Root_meta_external_format_json_binding_validation_bindDetails_Binding_1__BindingDetail_1_(binding, context.getExecutionSupport());
    }

    @Override
    public String metamodelToText(Root_meta_external_format_json_metamodel_JsonSchema schemaDetail, PureModel pureModel)
    {
        return schemaDetail._content();
    }

    @Override
    public Root_meta_external_shared_format_binding_Binding generateModel(Root_meta_external_shared_format_metamodel_SchemaSet schemaSet, JsonSchemaToModelConfiguration config, PureModel pureModel)
    {
        Root_meta_external_format_json_binding_toPure_JsonSchemaToModelConfiguration configuration = new Root_meta_external_format_json_binding_toPure_JsonSchemaToModelConfiguration_Impl("", null, pureModel.getClass("meta::external::format::json::binding::toPure::JsonSchemaToModelConfiguration"))
                ._sourceSchemaId(config.sourceSchemaId)
                ._targetBinding(config.targetBinding)
                ._targetPackage(config.targetPackage);
        return IN_DEBUG
                ? core_external_format_json_binding_jsonSchemaToPure.Root_meta_external_format_json_binding_toPure_jsonSchemaToPureWithDebug_SchemaSet_1__JsonSchemaToModelConfiguration_1__Binding_1_(schemaSet, configuration, pureModel.getExecutionSupport())
                : core_external_format_json_binding_jsonSchemaToPure.Root_meta_external_format_json_binding_toPure_jsonSchemaToPure_SchemaSet_1__JsonSchemaToModelConfiguration_1__Binding_1_(schemaSet, configuration, pureModel.getExecutionSupport());
    }

    @Override
    public Root_meta_external_shared_format_binding_Binding generateSchema(ModelToJsonSchemaConfiguration config, PureModel pureModel)
    {
        Root_meta_external_format_json_binding_fromPure_ModelToJsonSchemaConfiguration configuration = new Root_meta_external_format_json_binding_fromPure_ModelToJsonSchemaConfiguration_Impl("", null, pureModel.getClass("meta::external::format::json::binding::fromPure::ModelToJsonSchemaConfiguration"))
                ._targetBinding(config.targetBinding)
                ._targetSchemaSet(config.targetSchemaSet);

        config.sourceModel.forEach(pe -> configuration._sourceModelAdd(pureModel.getPackageableElement(pe)));

        return IN_DEBUG
                ? core_external_format_json_binding_pureToJsonSchema.Root_meta_external_format_json_binding_fromPure_pureToJsonSchemaWithDebug_ModelToJsonSchemaConfiguration_1__Binding_1_(configuration, pureModel.getExecutionSupport())
                : core_external_format_json_binding_pureToJsonSchema.Root_meta_external_format_json_binding_fromPure_pureToJsonSchema_ModelToJsonSchemaConfiguration_1__Binding_1_(configuration, pureModel.getExecutionSupport());
    }

    @Override
    public List<String> getRegisterablePackageableElementNames()
    {
        ImmutableList<String> versions = PureClientVersions.versionsSince("v1_23_0");
        return versions.collect(v -> "meta::protocols::pure::" + v + "::external::format::json::serializerExtension_String_1__SerializerExtension_1_").toList();
    }
}
