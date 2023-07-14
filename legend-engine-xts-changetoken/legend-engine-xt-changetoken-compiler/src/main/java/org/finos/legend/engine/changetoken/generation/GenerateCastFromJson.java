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

package org.finos.legend.engine.changetoken.generation;

import org.finos.legend.engine.external.language.java.generation.GenerateJavaProject;
import org.finos.legend.pure.generated.Root_meta_external_language_java_metamodel_project_Project;
import org.finos.legend.pure.generated.core_pure_changetoken_cast_generation;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;

public class GenerateCastFromJson extends GenerateJavaProject
{
    private final String jsonString;
    private final String outputClassName;

    public GenerateCastFromJson(String outputDirectory, String jsonString, String outputClassName)
    {
        super(outputDirectory);
        this.jsonString = jsonString;
        this.outputClassName = outputClassName;
    }

    @Override
    protected Root_meta_external_language_java_metamodel_project_Project doExecute(CompiledExecutionSupport executionSupport)
    {
        return core_pure_changetoken_cast_generation.Root_meta_pure_changetoken_cast_generation_generateCastFromJson_String_1__String_1__Project_1_(jsonString, outputClassName, executionSupport);
    }
}
