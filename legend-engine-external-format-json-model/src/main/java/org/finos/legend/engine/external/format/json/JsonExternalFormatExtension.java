package org.finos.legend.engine.external.format.json;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.ImmutableList;
import org.finos.legend.engine.external.format.json.compile.JsonSchemaCompiler;
import org.finos.legend.engine.external.format.json.fromModel.ModelToJsonSchemaConfiguration;
import org.finos.legend.engine.external.format.json.toModel.JsonSchemaToModelConfiguration;
import org.finos.legend.engine.external.shared.format.model.ExternalFormatExtension;
import org.finos.legend.engine.external.shared.format.model.ExternalSchemaCompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.pure.generated.*;

import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.List;

public class JsonExternalFormatExtension implements ExternalFormatExtension<Root_meta_external_format_json_metamodel_JsonSchema, JsonSchemaToModelConfiguration, ModelToJsonSchemaConfiguration>
{
    private static final String TYPE = "JSON";
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
    public String metamodelToText(Root_meta_external_format_json_metamodel_JsonSchema schemaDetail)
    {
        return schemaDetail._content();
    }

    @Override
    public String getFileExtension() {
        return TYPE.toLowerCase();
    }

    @Override
    public String getFormatLabel()
    {
        return "JSON Schema";
    }

    @Override
    public boolean supportsModelGeneration()
    {
        return true;
    }

    @Override
    public RichIterable<? extends Root_meta_pure_generation_metamodel_GenerationParameter> getModelGenerationProperties(PureModel pureModel)
    {
        return core_external_format_json_binding_jsonSchemaToPure.Root_meta_external_format_json_binding_toPure_describeConfiguration__GenerationParameter_MANY_(pureModel.getExecutionSupport());
    }

    @Override
    public Root_meta_external_shared_format_binding_Binding generateModel(Root_meta_external_shared_format_metamodel_SchemaSet schemaSet, JsonSchemaToModelConfiguration config, PureModel pureModel)
    {

        Root_meta_external_format_json_binding_toPure_JsonSchemaToModelConfiguration configuration =
                core_external_format_json_binding_jsonSchemaToPure.Root_meta_external_format_json_binding_toPure_defaultConfig__JsonSchemaToModelConfiguration_1_(pureModel.getExecutionSupport());
        if(config.sourceSchemaId != null)
        {
            configuration._sourceSchemaId((config.sourceSchemaId));
        }
        if(config.targetBinding != null)
        {
            configuration._targetBinding(config.targetBinding);
        }
        if(config.targetPackage != null)
        {
            configuration._targetPackage(config.targetPackage);
        }
        return IN_DEBUG
                ? core_external_format_json_binding_jsonSchemaToPure.Root_meta_external_format_json_binding_toPure_jsonSchemaToPureWithDebug_SchemaSet_1__JsonSchemaToModelConfiguration_1__Binding_1_(schemaSet, configuration, pureModel.getExecutionSupport())
                : core_external_format_json_binding_jsonSchemaToPure.Root_meta_external_format_json_binding_toPure_jsonSchemaToPure_SchemaSet_1__JsonSchemaToModelConfiguration_1__Binding_1_(schemaSet, configuration, pureModel.getExecutionSupport());
    }

    @Override
    public boolean supportsSchemaGeneration()
    {
        return true;
    }

    @Override
    public RichIterable<? extends Root_meta_pure_generation_metamodel_GenerationParameter> getSchemaGenerationProperties(PureModel pureModel)
    {
        return core_external_format_flatdata_binding_pureToFlatData.Root_meta_external_format_flatdata_binding_fromPure_describeConfiguration__GenerationParameter_MANY_(pureModel.getExecutionSupport());
    }


    @Override
    public Root_meta_external_shared_format_binding_Binding generateSchema(ModelToJsonSchemaConfiguration config, PureModel pureModel)
    {
        Root_meta_external_format_json_binding_fromPure_ModelToJsonSchemaConfiguration configuration = new Root_meta_external_format_json_binding_fromPure_ModelToJsonSchemaConfiguration_Impl("")
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
        ImmutableList<String> versions = PureClientVersions.versionsSince("v1_22_0");
        return versions.collect(v -> "meta::protocols::pure::" + v + "::external::format::json::serializerExtension_String_1__SerializerExtension_1_").toList();
    }
}
