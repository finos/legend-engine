package org.finos.legend.engine.external.format.json.compile;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.external.shared.format.model.ExternalSchemaCompileContext;
import org.finos.legend.pure.generated.*;

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
        core_json_fromJSONSchema.Root_meta_json_schema_fromSchema_JSONSchemaToPure_SchemaInput_MANY__PackageableElement_MANY_(Lists.mutable.with(schemaInput), context.getPureModel().getExecutionSupport());

        return new Root_meta_external_format_json_metamodel_JsonSchema_Impl("")
                ._content(content);
    }
}
