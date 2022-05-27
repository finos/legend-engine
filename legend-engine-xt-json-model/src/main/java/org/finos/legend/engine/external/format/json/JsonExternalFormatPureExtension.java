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
