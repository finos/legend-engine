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

package org.finos.legend.pure.elasticsearch.specification.generator;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.finos.legend.engine.external.language.java.generation.GenerateJavaProject;
import org.finos.legend.pure.generated.Root_meta_external_language_java_metamodel_project_Project;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;

public class ElasticsearchGenerateSpecificationClasses extends GenerateJavaProject
{
    public static void main(String[] args) throws Exception
    {
        String json = readConfigFile(args[0]);
        new ElasticsearchGenerateSpecificationClasses(json, args[1]).execute();
    }

    private static String readConfigFile(String file) throws Exception
    {
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(file))
        {
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        }
    }

    private final String configJson;

    protected ElasticsearchGenerateSpecificationClasses(String configJson, String outputDirectory)
    {
        super(outputDirectory);
        this.configJson = configJson;
    }

    @Override
    protected Root_meta_external_language_java_metamodel_project_Project doExecute(CompiledExecutionSupport executionSupport)
    {
        try
        {
            return (Root_meta_external_language_java_metamodel_project_Project) Class.forName("org.finos.legend.pure.generated.core_elasticsearch_specification_metamodel_protocol_generator")
                    .getMethod("Root_meta_external_store_elasticsearch_specification_metamodel_generateElasticsearchSpecificationClasses_String_1__Project_1_", String.class, ExecutionSupport.class)
                    .invoke(null, configJson, executionSupport);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
