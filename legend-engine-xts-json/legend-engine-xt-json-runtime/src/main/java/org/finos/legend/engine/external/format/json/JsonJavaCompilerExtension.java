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

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.external.format.json.read.IJsonDeserializeExecutionNodeSpecifics;
import org.finos.legend.engine.external.format.json.read.IJsonInternalizeExecutionNodeSpecifics;
import org.finos.legend.engine.external.format.json.read.JsonDataReader;
import org.finos.legend.engine.external.format.json.read.JsonDataRecord;
import org.finos.legend.engine.external.format.json.write.IJsonExternalizeExecutionNodeSpecifics;
import org.finos.legend.engine.external.format.json.write.IJsonSerializer;
import org.finos.legend.engine.external.format.json.write.IJsonWriter;
import org.finos.legend.engine.external.shared.ExternalFormatJavaCompilerExtension;
import org.finos.legend.engine.plan.compilation.GeneratePureConfig;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.ExecutionPlanJavaCompilerExtension;
import org.finos.legend.engine.shared.javaCompiler.ClassPathFilter;
import org.finos.legend.engine.shared.javaCompiler.ClassPathFilters;

import java.util.LinkedHashMap;
import java.util.Map;

public class JsonJavaCompilerExtension implements ExecutionPlanJavaCompilerExtension
{
    private static final String PURE_PACKAGE = "meta::external::format::json::executionPlan::platformBinding::legendJava::";
    private static final Map<String, Class<?>> DEPENDENCIES = new LinkedHashMap<>();

    static
    {
        DEPENDENCIES.put(PURE_PACKAGE + "_IJsonDeserializeExecutionNodeSpecifics", IJsonDeserializeExecutionNodeSpecifics.class);
        DEPENDENCIES.put("meta::external::format::json::executionPlan::model::JsonDataRecord", JsonDataRecord.class);
        DEPENDENCIES.put(PURE_PACKAGE + "_IJsonInternalizeExecutionNodeSpecifics", IJsonInternalizeExecutionNodeSpecifics.class);
        DEPENDENCIES.put(PURE_PACKAGE + "JsonDataReader", JsonDataReader.class);
        DEPENDENCIES.put(PURE_PACKAGE + "_IJsonExternalizeExecutionNodeSpecifics", IJsonExternalizeExecutionNodeSpecifics.class);
        DEPENDENCIES.put(PURE_PACKAGE + "IJsonSerializer", IJsonSerializer.class);
        DEPENDENCIES.put(PURE_PACKAGE + "IJsonWriter", IJsonWriter.class);
    }

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("External_Format", "JSON");
    }

    @Override
    public ClassPathFilter getExtraClassPathFilter()
    {
        return ClassPathFilters.fromClasses(DEPENDENCIES.values());
    }

    /**
     * Use to generate content of core_external_format_json/executionPlan/engine.pure (see GeneratePureConfig)
     */
    public static void main(String[] args)
    {
        GeneratePureConfig extension = new GeneratePureConfig("externalFormatJson", ExternalFormatJavaCompilerExtension.class, PURE_PACKAGE);
        DEPENDENCIES.forEach((p, c) ->
        {
            if (!c.isAnnotationPresent(Deprecated.class))
            {
                extension.addClass(p, c);
            }
        });
        System.out.println(extension.generate());
    }
}
