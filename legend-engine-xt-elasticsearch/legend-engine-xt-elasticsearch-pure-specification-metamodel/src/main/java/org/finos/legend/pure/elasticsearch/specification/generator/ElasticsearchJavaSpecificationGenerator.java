//  Copyright 2023 Goldman Sachs
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

import java.lang.reflect.Method;
import org.finos.legend.engine.external.language.java.generation.GenerateJavaProject;
import org.finos.legend.pure.generated.Root_meta_external_language_java_metamodel_project_Project;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;

public class ElasticsearchJavaSpecificationGenerator extends GenerateJavaProject
{
    public static void main(String[] args)
    {
        new ElasticsearchJavaSpecificationGenerator(args[0], args[1], args[2]).execute();
    }

    private final String fromPurePackage;
    private final String toJavaPackage;

    protected ElasticsearchJavaSpecificationGenerator(String fromPurePackage, String toJavaPackage, String outputDirectory)
    {
        super(outputDirectory);
        this.fromPurePackage = fromPurePackage;
        this.toJavaPackage = toJavaPackage;
    }

    @Override
    protected Root_meta_external_language_java_metamodel_project_Project doExecute(CompiledExecutionSupport executionSupport)
    {
        try
        {
            Class<?> generatorClass = Class.forName("org.finos.legend.pure.generated.core_elasticsearch_specification_metamodel_specification_generator");
            Method generatorMethod = generatorClass.getMethod("Root_meta_external_store_elasticsearch_specification_metamodel_generateProtocolClasses_String_1__String_1__Project_1_", String.class, String.class, ExecutionSupport.class);
            return (Root_meta_external_language_java_metamodel_project_Project) generatorMethod.invoke(null, this.fromPurePackage, this.toJavaPackage, executionSupport);
        }
        catch (ReflectiveOperationException e)
        {
            throw new RuntimeException(e);
        }
    }
}
