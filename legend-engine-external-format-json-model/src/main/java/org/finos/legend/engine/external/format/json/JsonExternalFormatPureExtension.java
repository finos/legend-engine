package org.finos.legend.engine.external.format.json;

import org.finos.legend.engine.external.shared.format.model.ExternalFormatPureExtension;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_ExternalFormatExtension;
import org.finos.legend.pure.generated.core_external_format_json_extension;
import org.finos.legend.pure.m3.execution.ExecutionSupport;

public class JsonExternalFormatPureExtension implements ExternalFormatPureExtension
{
    public static final String CONTENT_TYPE = "application/json";

    @Override
    public String getContentType()
    {
        return CONTENT_TYPE;
    }

    @Override
    public Root_meta_external_shared_format_ExternalFormatExtension getPureExtension(ExecutionSupport executionSupport)
    {
        return core_external_format_json_extension.Root_meta_external_format_json_jsonFormatExtension__ExternalFormatExtension_1_(executionSupport);
    }
}
