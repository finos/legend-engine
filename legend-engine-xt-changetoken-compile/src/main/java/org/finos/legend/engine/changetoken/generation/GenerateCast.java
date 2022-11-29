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

package org.finos.legend.engine.changetoken.generation;

import org.finos.legend.engine.external.language.java.generation.GenerateJavaProject;
import org.finos.legend.pure.generated.Root_meta_external_language_java_metamodel_project_Project;
import org.finos.legend.pure.generated.core_pure_changetoken_cast_generation;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;

public class GenerateCast extends GenerateJavaProject
{
    public static void main(String[] args)
    {
        new GenerateCast(args[0], args[1], args[2]).execute();
    }

    private final String versionsFuncName;
    private final String outputClassName;

    protected GenerateCast(String outputDirectory, String versionsFuncName, String outputClassName)
    {
        super(outputDirectory);
        this.versionsFuncName = versionsFuncName;
        this.outputClassName = outputClassName;
    }

    @Override
    protected Root_meta_external_language_java_metamodel_project_Project doExecute(CompiledExecutionSupport executionSupport)
    {
        return core_pure_changetoken_cast_generation.Root_meta_pure_changetoken_cast_generation_generateCast_String_1__String_1__Project_1_(versionsFuncName, outputClassName, executionSupport);
    }
}
